import {authenticatedFetch, refreshSession} from "./auth-client.js";

const sessionId = Number(window.location.pathname.split("/").filter(Boolean).at(-1));
let session = null;
let answers = [];
let currentIndex = 0;
let questionStartedAt = Date.now();
let timerHandle = null;

const loading = document.querySelector("#room-loading");
const content = document.querySelector("#room-content");
const errorBox = document.querySelector("#room-error");
const composer = document.querySelector("#answer-composer");
const evaluationPanel = document.querySelector("#evaluation-panel");
const completionPanel = document.querySelector("#completion-panel");
const evaluationOverlay = document.querySelector("#evaluation-overlay");

boot();

async function boot() {
    if (!Number.isSafeInteger(sessionId) || sessionId < 1) {
        showFatalError();
        return;
    }
    try {
        await refreshSession();
        const [sessionResponse, answerResponse] = await Promise.all([
            authenticatedFetch(`/api/v1/interviews/${sessionId}`),
            authenticatedFetch(`/api/v1/interviews/${sessionId}/answers`)
        ]);
        if (!sessionResponse.ok) throw await apiError(sessionResponse);
        if (!answerResponse.ok) throw await apiError(answerResponse);
        session = await sessionResponse.json();
        answers = await answerResponse.json();
        const unanswered = session.questions.findIndex(question => !answerFor(question.id));
        currentIndex = unanswered >= 0 ? unanswered : 0;
        renderRoom();
        loading.hidden = true;
        content.hidden = false;
    } catch (error) {
        if (error?.status === 401) {
            window.location.replace("/login");
            return;
        }
        showFatalError();
    }
}

function renderRoom() {
    document.title = `${session.targetRole} interview — InterviewPilot`;
    text("#rail-role", session.targetRole);
    text("#rail-meta", `${label(session.interviewType)} · ${label(session.difficulty)} · ${session.totalQuestions} questions`);
    renderQuestionMap();
    updateProgress();
    renderCurrentQuestion();
}

function renderQuestionMap() {
    const map = document.querySelector("#question-map");
    map.replaceChildren();
    session.questions.forEach((question, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "question-map-item";
        button.classList.toggle("active", index === currentIndex);
        button.classList.toggle("answered", Boolean(answerFor(question.id)));
        const number = document.createElement("span");
        number.textContent = String(question.sequenceNumber).padStart(2, "0");
        const copy = document.createElement("div");
        const title = document.createElement("strong");
        title.textContent = `Question ${question.sequenceNumber}`;
        const type = document.createElement("small");
        type.textContent = label(question.questionType);
        copy.append(title, type);
        const state = document.createElement("b");
        state.textContent = answerFor(question.id) ? "✓" : "";
        button.append(number, copy, state);
        button.addEventListener("click", () => selectQuestion(index));
        map.append(button);
    });
}

function renderCurrentQuestion() {
    stopTimer();
    completionPanel.hidden = true;
    const question = session.questions[currentIndex];
    const answer = answerFor(question.id);
    text("#question-sequence", `Question ${question.sequenceNumber} of ${session.totalQuestions}`);
    text("#question-kind", `${label(question.questionType)} · ${label(question.difficulty)}`);
    text("#question-text", question.questionText);
    renderTopics(question.expectedTopics || []);
    document.querySelector("#answer-error").textContent = "";
    if (answer) {
        composer.hidden = true;
        renderEvaluation(answer.evaluation);
    } else {
        evaluationPanel.hidden = true;
        composer.hidden = false;
        const input = document.querySelector("#answer-text");
        input.value = "";
        updateAnswerCount();
        startTimer(false);
        input.focus();
    }
    renderQuestionMap();
}

function renderTopics(topics) {
    const target = document.querySelector("#expected-topics");
    target.replaceChildren();
    topics.forEach(topic => {
        const tag = document.createElement("span");
        tag.textContent = topic;
        target.append(tag);
    });
}

document.querySelector("#answer-text")?.addEventListener("input", updateAnswerCount);
function updateAnswerCount() { text("#answer-count", `${document.querySelector("#answer-text").value.length} / 10000`); }

document.querySelector("#submit-answer")?.addEventListener("click", submitAnswer);
async function submitAnswer() {
    const question = session.questions[currentIndex];
    const input = document.querySelector("#answer-text");
    const answerText = input.value.trim();
    if (!answerText) {
        document.querySelector("#answer-error").textContent = "Write an answer before requesting evaluation.";
        input.focus();
        return;
    }
    stopTimer();
    evaluationOverlay.hidden = false;
    try {
        const response = await authenticatedFetch(`/api/v1/interviews/${sessionId}/answers`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                questionId: question.id,
                answerText,
                responseTimeSeconds: Math.min(7200, Math.max(0, Math.round((Date.now() - questionStartedAt) / 1000)))
            })
        });
        if (!response.ok) throw await apiError(response);
        answers.push(await response.json());
        const sessionResponse = await authenticatedFetch(`/api/v1/interviews/${sessionId}`);
        if (sessionResponse.ok) session = await sessionResponse.json();
        evaluationOverlay.hidden = true;
        composer.hidden = true;
        renderEvaluation(answerFor(question.id).evaluation);
        updateProgress();
        renderQuestionMap();
    } catch (error) {
        evaluationOverlay.hidden = true;
        document.querySelector("#answer-error").textContent = error.message || "Your answer could not be evaluated.";
        startTimer();
    }
}

function renderEvaluation(evaluation) {
    evaluationPanel.hidden = false;
    text("#overall-score", Math.round(Number(evaluation.overallScore)));
    text("#evaluation-model", `Evaluated by ${evaluation.aiModel}`);
    renderScores(evaluation);
    renderList("#strength-list", evaluation.strengths, "No strength details returned.");
    renderList("#improvement-list", evaluation.improvements, "No improvement details returned.");
    text("#ideal-answer-text", evaluation.idealAnswer);
    const hasUnanswered = session.questions.some(question => !answerFor(question.id));
    text("#next-question", hasUnanswered ? "Next question →" : "Complete session →");
}

function renderScores(evaluation) {
    const target = document.querySelector("#score-breakdown");
    target.replaceChildren();
    [
        ["Technical", evaluation.technicalScore],
        ["Relevance", evaluation.relevanceScore],
        ["Clarity", evaluation.clarityScore],
        ["Confidence", evaluation.confidenceScore]
    ].forEach(([name, value]) => {
        const row = document.createElement("div");
        const labelNode = document.createElement("span");
        labelNode.textContent = name;
        const progress = document.createElement("progress");
        progress.max = 100;
        progress.value = Number(value);
        progress.textContent = `${value}%`;
        const score = document.createElement("strong");
        score.textContent = `${Math.round(Number(value))}`;
        row.append(labelNode, progress, score);
        target.append(row);
    });
}

function renderList(selector, values, emptyText) {
    const list = document.querySelector(selector);
    list.replaceChildren();
    (values?.length ? values : [emptyText]).forEach(value => {
        const item = document.createElement("li");
        item.textContent = value;
        list.append(item);
    });
}

document.querySelector("#next-question")?.addEventListener("click", () => {
    const nextUnanswered = session.questions.findIndex((question, index) => index > currentIndex && !answerFor(question.id));
    const anyUnanswered = session.questions.findIndex(question => !answerFor(question.id));
    if (nextUnanswered >= 0) selectQuestion(nextUnanswered);
    else if (anyUnanswered >= 0) selectQuestion(anyUnanswered);
    else showCompletion();
});
document.querySelector("#skip-question")?.addEventListener("click", () => selectQuestion((currentIndex + 1) % session.questions.length));
document.querySelector("#review-answers")?.addEventListener("click", () => selectQuestion(0));

function selectQuestion(index) {
    currentIndex = index;
    renderCurrentQuestion();
}

function showCompletion() {
    stopTimer();
    composer.hidden = true;
    evaluationPanel.hidden = true;
    completionPanel.hidden = false;
    text("#final-score", session.overallScore == null ? "—" : Math.round(Number(session.overallScore)));
    text("#room-status", "Session complete");
}

function updateProgress() {
    const completed = answers.length;
    const percentage = session.totalQuestions ? Math.round((completed / session.totalQuestions) * 100) : 0;
    const progress = document.querySelector("#room-progress-bar");
    progress.value = percentage;
    progress.textContent = `${percentage}%`;
    text("#room-progress-text", `${completed} / ${session.totalQuestions}`);
    text("#room-status", session.status === "COMPLETED" ? "Session complete" : "Interview in progress");
}

function startTimer(reset = true) {
    if (reset) questionStartedAt = Date.now();
    updateTimer();
    timerHandle = window.setInterval(updateTimer, 1000);
}
function stopTimer() { if (timerHandle) window.clearInterval(timerHandle); timerHandle = null; }
function updateTimer() {
    const seconds = Math.min(7200, Math.floor((Date.now() - questionStartedAt) / 1000));
    text("#answer-timer", `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`);
}

function answerFor(questionId) { return answers.find(answer => answer.questionId === questionId); }
function label(value) { return value.split("_").map(part => part.charAt(0) + part.slice(1).toLowerCase()).join(" "); }
function text(selector, value) { document.querySelector(selector).textContent = value; }
function showFatalError() { loading.hidden = true; errorBox.hidden = false; }
async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* response had no JSON body */ }
    return {status: response.status, message: body.message || "The request could not be completed."};
}
