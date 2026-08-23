import {authenticatedFetch, logout, refreshSession} from "./auth-client.js";

let sessions = [];
let activeFilter = "ALL";
let profileTargetRole = "";
let toastTimer;

const loading = document.querySelector("#interviews-loading");
const content = document.querySelector("#interviews-content");
const errorBox = document.querySelector("#interviews-error");
const dialog = document.querySelector("#interview-dialog");
const form = document.querySelector("#interview-form");
const overlay = document.querySelector("#generation-overlay");

boot();

async function boot() {
    try {
        await refreshSession();
        const [sessionResponse, profileResponse] = await Promise.all([
            authenticatedFetch("/api/v1/interviews"),
            authenticatedFetch("/api/v1/profile")
        ]);
        if (!sessionResponse.ok) throw await apiError(sessionResponse);
        sessions = await sessionResponse.json();
        if (profileResponse.ok) {
            const profile = await profileResponse.json();
            profileTargetRole = profile.targetRole || "";
        }
        renderStats();
        renderSessions();
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
    const completed = sessions.filter(session => session.status === "COMPLETED");
    const active = sessions.filter(session => session.status === "CREATED" || session.status === "IN_PROGRESS");
    const scores = completed.map(session => Number(session.overallScore)).filter(Number.isFinite);
    text("#total-sessions", sessions.length);
    text("#completed-sessions", completed.length);
    text("#active-sessions", active.length);
    text("#best-score", scores.length ? `${Math.round(Math.max(...scores))}` : "—");
}

function renderSessions() {
    const target = document.querySelector("#session-list");
    target.replaceChildren();
    const visible = sessions.filter(session => activeFilter === "ALL" || session.status === activeFilter);
    if (!visible.length) {
        const empty = document.createElement("div");
        empty.className = "session-empty";
        const mark = document.createElement("span");
        mark.textContent = "◎";
        const title = document.createElement("strong");
        title.textContent = sessions.length ? "No sessions match this filter" : "Your first practice session starts here";
        const copy = document.createElement("p");
        copy.textContent = sessions.length ? "Choose another filter to view your interview history." : "Configure a focused Gemini interview and receive feedback after every answer.";
        const start = document.createElement("button");
        start.type = "button";
        start.className = "button button-primary";
        start.textContent = "Create interview";
        start.addEventListener("click", openDialog);
        empty.append(mark, title, copy, start);
        target.append(empty);
        return;
    }
    visible.forEach(session => target.append(sessionRow(session)));
}

function sessionRow(session) {
    const row = document.createElement("article");
    row.className = "session-row";
    const icon = document.createElement("span");
    icon.className = `session-type-icon type-${session.interviewType.toLowerCase()}`;
    icon.textContent = typeInitial(session.interviewType);
    const identity = document.createElement("div");
    identity.className = "session-identity";
    const title = document.createElement("strong");
    title.textContent = session.targetRole;
    const meta = document.createElement("span");
    meta.textContent = `${label(session.interviewType)} · ${label(session.difficulty)} · ${session.totalQuestions} question${session.totalQuestions === 1 ? "" : "s"}`;
    identity.append(title, meta);
    const date = document.createElement("time");
    date.dateTime = session.createdAt;
    date.textContent = formatDate(session.createdAt);
    const status = document.createElement("span");
    status.className = `session-status status-${session.status.toLowerCase()}`;
    status.textContent = label(session.status);
    const score = document.createElement("span");
    score.className = "session-score";
    score.textContent = session.overallScore == null ? "—" : `${Math.round(Number(session.overallScore))}`;
    const action = document.createElement("a");
    action.className = "button button-ghost session-action";
    action.href = `/interviews/${session.id}`;
    action.textContent = session.status === "COMPLETED" ? "Review" : session.status === "IN_PROGRESS" ? "Continue" : "Start";
    row.append(icon, identity, date, status, score, action);
    return row;
}

document.querySelectorAll("[data-filter]").forEach(button => button.addEventListener("click", () => {
    activeFilter = button.dataset.filter;
    document.querySelectorAll("[data-filter]").forEach(item => item.classList.toggle("active", item === button));
    renderSessions();
}));

document.querySelector("#new-interview-button")?.addEventListener("click", openDialog);
document.querySelector("#close-interview-dialog")?.addEventListener("click", () => dialog.close());
document.querySelector("#cancel-interview")?.addEventListener("click", () => dialog.close());
function openDialog() {
    document.querySelector("#interview-form-alert").hidden = true;
    document.querySelector("#interview-target-role").value = profileTargetRole;
    dialog.showModal();
}

const countInput = document.querySelector("#question-count");
countInput?.addEventListener("input", () => text("#question-count-output", countInput.value));

form?.addEventListener("submit", async event => {
    event.preventDefault();
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    const alertBox = document.querySelector("#interview-form-alert");
    alertBox.hidden = true;
    const targetRole = document.querySelector("#interview-target-role").value.trim();
    const payload = {
        interviewType: form.querySelector("input[name='interviewType']:checked").value,
        difficulty: form.querySelector("input[name='difficulty']:checked").value,
        questionCount: Number(countInput.value),
        targetRole: targetRole || null
    };
    dialog.close();
    overlay.hidden = false;
    try {
        const response = await authenticatedFetch("/api/v1/interviews", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw await apiError(response);
        const session = await response.json();
        window.location.assign(`/interviews/${session.id}`);
    } catch (error) {
        overlay.hidden = true;
        dialog.showModal();
        alertBox.textContent = error.message || "Interview questions could not be generated.";
        alertBox.hidden = false;
    }
});

function typeInitial(type) { return ({TECHNICAL: "T", HR: "H", BEHAVIORAL: "B", MIXED: "M"})[type] || "I"; }
function label(value) { return value.split("_").map(part => part.charAt(0) + part.slice(1).toLowerCase()).join(" "); }
function formatDate(value) { return new Intl.DateTimeFormat(undefined, {day: "2-digit", month: "short", year: "numeric"}).format(new Date(value)); }
function text(selector, value) { document.querySelector(selector).textContent = value; }
async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* response had no JSON body */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}
function showToast(message, error = false) {
    const toast = document.querySelector("#interview-toast");
    toast.textContent = message;
    toast.classList.toggle("toast-error", error);
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.hidden = true, 3500);
}

document.querySelector("#logout-button")?.addEventListener("click", async () => {
    await logout();
    window.location.replace("/login");
});
document.querySelector("#menu-button")?.addEventListener("click", () => document.querySelector(".sidebar")?.classList.toggle("open"));
