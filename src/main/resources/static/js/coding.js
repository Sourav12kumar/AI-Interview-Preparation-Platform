import {authenticatedFetch, logout, refreshSession} from "./auth-client.js";

let problems = [];
let submissions = [];
let searchQuery = "";
let difficultyFilter = "ALL";
let tagFilter = "ALL";

const loading = document.querySelector("#coding-loading");
const content = document.querySelector("#coding-content");
const errorBox = document.querySelector("#coding-error");

boot();

async function boot() {
    try {
        await refreshSession();
        const [problemResponse, submissionResponse] = await Promise.all([
            authenticatedFetch("/api/v1/coding/problems"),
            authenticatedFetch("/api/v1/coding/submissions")
        ]);
        if (!problemResponse.ok) throw await apiError(problemResponse);
        if (!submissionResponse.ok) throw await apiError(submissionResponse);
        problems = await problemResponse.json();
        submissions = await submissionResponse.json();
        renderStats();
        renderTagOptions();
        renderProblems();
        renderSubmissions();
        configureContinueLink();
        loading.hidden = true;
        content.hidden = false;
    } catch (error) {
        if (error?.status === 401) {
            window.location.replace("/login");
            return;
        }
        loading.hidden = true;
        errorBox.hidden = false;
    }
}

function renderStats() {
    const accepted = submissions.filter(submission => submission.verdict === "ACCEPTED");
    const solved = new Set(accepted.map(submission => submission.problemId));
    const rate = submissions.length ? Math.round((accepted.length / submissions.length) * 100) : null;
    text("#problem-total", problems.length);
    text("#submission-total", submissions.length);
    text("#solved-total", solved.size);
    text("#acceptance-rate", rate == null ? "—" : `${rate}%`);
}

function renderTagOptions() {
    const select = document.querySelector("#tag-filter");
    const tags = [...new Set(problems.flatMap(problem => Array.isArray(problem.tags) ? problem.tags : []))]
        .sort((a, b) => a.localeCompare(b));
    tags.forEach(tag => {
        const option = document.createElement("option");
        option.value = tag;
        option.textContent = label(tag);
        select.append(option);
    });
}

function renderProblems() {
    const target = document.querySelector("#problem-grid");
    target.replaceChildren();
    const visible = problems.filter(problem => {
        const searchable = `${problem.title} ${problem.slug} ${(problem.tags || []).join(" ")}`.toLowerCase();
        return (!searchQuery || searchable.includes(searchQuery))
            && (difficultyFilter === "ALL" || problem.difficulty === difficultyFilter)
            && (tagFilter === "ALL" || (problem.tags || []).includes(tagFilter));
    });
    text("#problem-count-label", `${visible.length} problem${visible.length === 1 ? "" : "s"}`);
    if (!visible.length) {
        const empty = document.createElement("div");
        empty.className = "problem-empty";
        const title = document.createElement("strong");
        title.textContent = "No problems match these filters";
        const copy = document.createElement("span");
        copy.textContent = "Try another title, level, or topic.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    visible.forEach((problem, index) => target.append(problemCard(problem, index)));
}

function problemCard(problem, index) {
    const attempts = submissions.filter(submission => submission.problemId === problem.id);
    const accepted = attempts.some(submission => submission.verdict === "ACCEPTED");
    const card = document.createElement("article");
    card.className = "problem-card";

    const head = document.createElement("div");
    head.className = "problem-card-head";
    const number = document.createElement("span");
    number.textContent = String(index + 1).padStart(2, "0");
    const difficulty = document.createElement("span");
    difficulty.className = `difficulty-badge difficulty-${problem.difficulty.toLowerCase()}`;
    difficulty.textContent = label(problem.difficulty);
    head.append(number, difficulty);

    const title = document.createElement("h3");
    title.textContent = problem.title;
    const tags = document.createElement("div");
    tags.className = "problem-tags";
    (problem.tags || []).forEach(value => {
        const tag = document.createElement("span");
        tag.textContent = label(value);
        tags.append(tag);
    });

    const foot = document.createElement("div");
    foot.className = "problem-card-foot";
    const state = document.createElement("span");
    state.className = accepted ? "problem-solved" : "problem-attempts";
    state.textContent = accepted ? "✓ Solved" : attempts.length ? `${attempts.length} attempt${attempts.length === 1 ? "" : "s"}` : "Not attempted";
    const action = document.createElement("a");
    action.href = `/coding/${problem.id}`;
    action.textContent = accepted ? "Practice again →" : "Solve problem →";
    foot.append(state, action);
    card.append(head, title, tags, foot);
    return card;
}

function renderSubmissions() {
    const target = document.querySelector("#submission-list");
    target.replaceChildren();
    if (!submissions.length) {
        const empty = document.createElement("div");
        empty.className = "submission-empty";
        const title = document.createElement("strong");
        title.textContent = "No submissions yet";
        const copy = document.createElement("span");
        copy.textContent = "Choose a problem and send your first solution to the runner.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    submissions.slice(0, 12).forEach(submission => target.append(submissionRow(submission)));
}

function submissionRow(submission) {
    const row = document.createElement("article");
    row.className = "submission-row";
    const verdict = document.createElement("span");
    verdict.className = `submission-verdict verdict-${submission.verdict.toLowerCase().replaceAll("_", "-")}`;
    verdict.textContent = verdictMark(submission.verdict);
    const identity = document.createElement("div");
    identity.className = "submission-identity";
    const title = document.createElement("strong");
    title.textContent = submission.problemTitle;
    const meta = document.createElement("span");
    meta.textContent = `${languageLabel(submission.language)} · ${formatDateTime(submission.submittedAt)}`;
    identity.append(title, meta);
    const status = document.createElement("span");
    status.className = `verdict-label verdict-text-${submission.verdict.toLowerCase().replaceAll("_", "-")}`;
    status.textContent = label(submission.verdict);
    const tests = document.createElement("span");
    tests.className = "submission-tests";
    tests.textContent = `${submission.passedTestCases}/${submission.totalTestCases} tests`;
    const score = document.createElement("strong");
    score.className = "submission-score-small";
    score.textContent = `${Math.round(Number(submission.score) || 0)}`;
    const action = document.createElement("a");
    action.className = "button button-ghost submission-review";
    action.href = `/coding/${submission.problemId}?submission=${submission.id}`;
    action.textContent = "Review";
    row.append(verdict, identity, status, tests, score, action);
    return row;
}

function configureContinueLink() {
    if (!problems.length) return;
    const solved = new Set(submissions.filter(item => item.verdict === "ACCEPTED").map(item => item.problemId));
    const next = problems.find(problem => !solved.has(problem.id)) || problems[0];
    const link = document.querySelector("#continue-coding");
    link.href = `/coding/${next.id}`;
    link.textContent = solved.size ? "Continue practice" : "Start first problem";
}

function verdictMark(verdict) {
    if (verdict === "ACCEPTED") return "✓";
    if (verdict === "TIME_LIMIT_EXCEEDED") return "⌛";
    if (verdict === "COMPILE_ERROR") return "!";
    return "×";
}

function languageLabel(language) {
    return {JAVA: "Java", PYTHON: "Python", JAVASCRIPT: "JavaScript", CPP: "C++"}[language] || label(language);
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

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed."};
}

document.querySelector("#problem-search").addEventListener("input", event => {
    searchQuery = event.target.value.trim().toLowerCase();
    renderProblems();
});
document.querySelector("#difficulty-filter").addEventListener("change", event => {
    difficultyFilter = event.target.value;
    renderProblems();
});
document.querySelector("#tag-filter").addEventListener("change", event => {
    tagFilter = event.target.value;
    renderProblems();
});
document.querySelector("#menu-button").addEventListener("click", () => document.querySelector(".sidebar").classList.toggle("open"));
document.querySelector("#logout-button").addEventListener("click", async () => {
    await logout();
    window.location.replace("/login");
});
