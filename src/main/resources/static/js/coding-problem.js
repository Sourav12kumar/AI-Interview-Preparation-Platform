import {authenticatedFetch, refreshSession} from "./auth-client.js";

const LANGUAGE_NAMES = {JAVA: "Java", PYTHON: "Python", JAVASCRIPT: "JavaScript", CPP: "C++"};
const FILE_NAMES = {JAVA: "Solution.java", PYTHON: "solution.py", JAVASCRIPT: "solution.js", CPP: "solution.cpp"};

let problem = null;
let submissions = [];
let activeLanguage = "JAVA";
let drafts = {};
const dirtyLanguages = new Set();
let toastTimer;

const problemId = parseProblemId();
const loading = document.querySelector("#problem-loading");
const content = document.querySelector("#problem-content");
const errorBox = document.querySelector("#problem-error");
const sourceCode = document.querySelector("#source-code");
const languageSelect = document.querySelector("#language-select");
const resultPanel = document.querySelector("#code-result");
const runnerOverlay = document.querySelector("#runner-overlay");
const resetDialog = document.querySelector("#reset-code-dialog");

boot();

async function boot() {
    if (!problemId) {
        showLoadError();
        return;
    }
    try {
        await refreshSession();
        const [problemResponse, submissionResponse] = await Promise.all([
            authenticatedFetch(`/api/v1/coding/problems/${problemId}`),
            authenticatedFetch("/api/v1/coding/submissions")
        ]);
        if (!problemResponse.ok) throw await apiError(problemResponse);
        if (!submissionResponse.ok) throw await apiError(submissionResponse);
        problem = await problemResponse.json();
        submissions = (await submissionResponse.json()).filter(item => item.problemId === problem.id);
        initializeEditor();
        renderProblem();
        renderSubmissionHistory();
        loading.hidden = true;
        content.hidden = false;
        const requestedSubmission = Number(new URLSearchParams(window.location.search).get("submission"));
        if (Number.isSafeInteger(requestedSubmission) && requestedSubmission > 0) {
            await reviewSubmission(requestedSubmission);
        }
    } catch (error) {
        if (error?.status === 401) {
            window.location.replace("/login");
            return;
        }
        showLoadError();
    }
}

function initializeEditor() {
    const starters = problem.starterCode && typeof problem.starterCode === "object" ? problem.starterCode : {};
    drafts = {...starters};
    const available = Object.keys(LANGUAGE_NAMES).filter(language => typeof starters[language] === "string");
    activeLanguage = available.includes("JAVA") ? "JAVA" : (available[0] || "JAVA");
    [...languageSelect.options].forEach(option => option.disabled = !available.includes(option.value));
    languageSelect.value = activeLanguage;
    loadEditorLanguage(activeLanguage);
}

function renderProblem() {
    text("#problem-title", problem.title);
    text("#problem-description", problem.description);
    const difficulty = document.querySelector("#problem-difficulty");
    difficulty.textContent = label(problem.difficulty);
    difficulty.className = `difficulty-badge difficulty-${problem.difficulty.toLowerCase()}`;
    const tags = document.querySelector("#problem-tags");
    tags.replaceChildren();
    (problem.tags || []).forEach(value => {
        const tag = document.createElement("span");
        tag.textContent = label(value);
        tags.append(tag);
    });
}

function changeLanguage(nextLanguage) {
    drafts[activeLanguage] = sourceCode.value;
    activeLanguage = nextLanguage;
    loadEditorLanguage(activeLanguage);
}

function loadEditorLanguage(language) {
    sourceCode.value = typeof drafts[language] === "string" ? drafts[language] : "";
    text("#editor-file-name", FILE_NAMES[language] || "solution.txt");
    updateSourceCount();
    clearSourceError();
}

function updateSourceCount() {
    text("#source-count", `${sourceCode.value.length} / 100000`);
}

async function submitSolution() {
    clearSourceError();
    const code = sourceCode.value;
    if (!code.trim()) {
        document.querySelector("#source-error").textContent = "Write a solution before submitting.";
        sourceCode.focus();
        return;
    }
    if (code.length > 100000) {
        document.querySelector("#source-error").textContent = "Source code must not exceed 100,000 characters.";
        return;
    }
    drafts[activeLanguage] = code;
    runnerOverlay.hidden = false;
    const submitButton = document.querySelector("#submit-code");
    submitButton.disabled = true;
    try {
        const response = await authenticatedFetch(`/api/v1/coding/problems/${problem.id}/submissions`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({language: activeLanguage, sourceCode: code})
        });
        if (!response.ok) throw await apiError(response);
        const submission = await response.json();
        submissions.unshift(submission);
        dirtyLanguages.delete(activeLanguage);
        renderResult(submission);
        renderSubmissionHistory();
        resultPanel.scrollIntoView({behavior: "smooth", block: "center"});
        showToast(submission.verdict === "ACCEPTED" ? "Solution accepted." : "Submission completed. Review the verdict and try again.", submission.verdict !== "ACCEPTED");
    } catch (error) {
        if (error.fieldErrors?.sourceCode) document.querySelector("#source-error").textContent = error.fieldErrors.sourceCode;
        showToast(error.message || "The code runner could not complete this submission.", true);
    } finally {
        runnerOverlay.hidden = true;
        submitButton.disabled = false;
    }
}

function renderResult(submission) {
    const accepted = submission.verdict === "ACCEPTED";
    const mark = document.querySelector("#verdict-mark");
    mark.textContent = verdictMark(submission.verdict);
    mark.className = `verdict-mark verdict-mark-${submission.verdict.toLowerCase().replaceAll("_", "-")}`;
    text("#verdict-title", label(submission.verdict));
    text("#verdict-message", submission.message || verdictMessage(submission.verdict));
    text("#submission-score", Math.round(clampScore(submission.score)));
    text("#tests-passed", `${submission.passedTestCases} / ${submission.totalTestCases}`);
    text("#execution-time", submission.executionTimeMs == null ? "—" : `${submission.executionTimeMs} ms`);
    text("#memory-used", formatMemory(submission.memoryUsedKb));
    text("#result-language", LANGUAGE_NAMES[submission.language] || label(submission.language));
    resultPanel.classList.toggle("result-accepted", accepted);
    resultPanel.classList.toggle("result-failed", !accepted);
    resultPanel.hidden = false;
}

function renderSubmissionHistory() {
    const target = document.querySelector("#problem-submission-list");
    target.replaceChildren();
    if (!submissions.length) {
        const empty = document.createElement("div");
        empty.className = "submission-empty";
        const title = document.createElement("strong");
        title.textContent = "No attempts for this problem";
        const copy = document.createElement("span");
        copy.textContent = "Your runner verdicts will appear here.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    submissions.forEach(submission => target.append(problemSubmissionRow(submission)));
}

function problemSubmissionRow(submission) {
    const row = document.createElement("button");
    row.type = "button";
    row.className = "problem-submission-row";
    row.addEventListener("click", () => reviewSubmission(submission.id));
    const mark = document.createElement("span");
    mark.className = `submission-verdict verdict-${submission.verdict.toLowerCase().replaceAll("_", "-")}`;
    mark.textContent = verdictMark(submission.verdict);
    const identity = document.createElement("span");
    identity.className = "problem-submission-identity";
    const verdict = document.createElement("strong");
    verdict.textContent = label(submission.verdict);
    const time = document.createElement("small");
    time.textContent = formatDateTime(submission.submittedAt);
    identity.append(verdict, time);
    const language = document.createElement("span");
    language.textContent = LANGUAGE_NAMES[submission.language] || label(submission.language);
    const tests = document.createElement("span");
    tests.textContent = `${submission.passedTestCases}/${submission.totalTestCases} tests`;
    const score = document.createElement("strong");
    score.textContent = `${Math.round(clampScore(submission.score))}`;
    const action = document.createElement("span");
    action.className = "review-code-label";
    action.textContent = "Review code →";
    row.append(mark, identity, language, tests, score, action);
    return row;
}

async function reviewSubmission(submissionId) {
    const row = submissions.find(item => item.id === submissionId);
    if (!row) return;
    try {
        const response = await authenticatedFetch(`/api/v1/coding/submissions/${submissionId}`);
        if (!response.ok) throw await apiError(response);
        const submission = await response.json();
        if (submission.problemId !== problem.id) return;
        drafts[activeLanguage] = sourceCode.value;
        activeLanguage = submission.language;
        languageSelect.value = activeLanguage;
        drafts[activeLanguage] = submission.sourceCode;
        dirtyLanguages.delete(activeLanguage);
        loadEditorLanguage(activeLanguage);
        renderResult(submission);
        document.querySelector(".code-editor-panel").scrollIntoView({behavior: "smooth", block: "start"});
        showToast("Loaded this submission for review. Editing creates a new attempt.");
    } catch (error) {
        showToast(error.message || "The submission could not be loaded.", true);
    }
}

function resetStarterCode() {
    const starter = problem.starterCode?.[activeLanguage];
    drafts[activeLanguage] = typeof starter === "string" ? starter : "";
    dirtyLanguages.delete(activeLanguage);
    loadEditorLanguage(activeLanguage);
    resultPanel.hidden = true;
    resetDialog.close();
    showToast("Starter code restored for this language.");
}

function insertIndent(event) {
    if (event.key !== "Tab") return;
    event.preventDefault();
    const start = sourceCode.selectionStart;
    const end = sourceCode.selectionEnd;
    sourceCode.setRangeText("    ", start, end, "end");
    drafts[activeLanguage] = sourceCode.value;
    dirtyLanguages.add(activeLanguage);
    updateSourceCount();
}

function showLoadError() {
    loading.hidden = true;
    errorBox.hidden = false;
}

function clearSourceError() {
    document.querySelector("#source-error").textContent = "";
}

function verdictMark(verdict) {
    if (verdict === "ACCEPTED") return "✓";
    if (verdict === "TIME_LIMIT_EXCEEDED") return "⌛";
    if (verdict === "COMPILE_ERROR") return "!";
    return "×";
}

function verdictMessage(verdict) {
    const messages = {
        ACCEPTED: "All hidden tests passed.",
        WRONG_ANSWER: "At least one hidden test produced an incorrect result.",
        COMPILE_ERROR: "The runner could not compile this solution.",
        RUNTIME_ERROR: "The solution stopped with a runtime error.",
        TIME_LIMIT_EXCEEDED: "The solution exceeded the execution time limit."
    };
    return messages[verdict] || "The runner completed this submission.";
}

function clampScore(value) {
    const score = Number(value);
    return Number.isFinite(score) ? Math.min(100, Math.max(0, score)) : 0;
}

function formatMemory(value) {
    const kb = Number(value);
    if (!Number.isFinite(kb)) return "—";
    return kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`;
}

function formatDateTime(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "Unknown date";
    return new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short", hour: "numeric", minute: "2-digit"}).format(date);
}

function label(value) {
    return String(value || "").toLowerCase().replaceAll("_", " ").replaceAll("-", " ").replace(/\b\w/g, letter => letter.toUpperCase());
}

function text(selector, value) {
    const target = document.querySelector(selector);
    if (target) target.textContent = String(value);
}

function parseProblemId() {
    const match = window.location.pathname.match(/^\/coding\/(\d+)\/?$/);
    if (!match) return null;
    const value = Number(match[1]);
    return Number.isSafeInteger(value) && value > 0 ? value : null;
}

function showToast(message, isError = false) {
    const toast = document.querySelector("#coding-toast");
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("toast-error", isError);
    toast.hidden = false;
    toastTimer = setTimeout(() => toast.hidden = true, 3600);
}

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}

languageSelect.addEventListener("change", event => changeLanguage(event.target.value));
sourceCode.addEventListener("input", () => {
    drafts[activeLanguage] = sourceCode.value;
    dirtyLanguages.add(activeLanguage);
    updateSourceCount();
    clearSourceError();
});
sourceCode.addEventListener("keydown", insertIndent);
document.querySelector("#submit-code").addEventListener("click", submitSolution);
document.querySelector("#reset-code").addEventListener("click", () => resetDialog.showModal());
document.querySelector("#cancel-reset-code").addEventListener("click", () => resetDialog.close());
document.querySelector("#confirm-reset-code").addEventListener("click", resetStarterCode);
window.addEventListener("beforeunload", event => {
    if (!dirtyLanguages.size) return;
    event.preventDefault();
    event.returnValue = "";
});
