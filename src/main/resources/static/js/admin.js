import {authenticatedFetch, getCurrentUser, logout, refreshSession} from "./auth-client.js";

let overview = null;
let users = [];
let problems = [];
let auditEvents = [];
let currentUser = null;
let pendingAction = null;
let toastTimer;

const loading = document.querySelector("#admin-loading");
const content = document.querySelector("#admin-content");
const errorBox = document.querySelector("#admin-error");
const accessDenied = document.querySelector("#admin-access-denied");
const problemDialog = document.querySelector("#problem-dialog");
const confirmDialog = document.querySelector("#admin-confirm-dialog");
const overlay = document.querySelector("#admin-overlay");

boot();

async function boot() {
    try {
        await refreshSession();
        currentUser = getCurrentUser();
        if (!currentUser?.roles?.includes("ROLE_ADMIN")) {
            loading.hidden = true;
            accessDenied.hidden = false;
            return;
        }
        const [overviewResponse, usersResponse, problemsResponse, auditResponse] = await Promise.all([
            authenticatedFetch("/api/v1/admin/overview"),
            authenticatedFetch("/api/v1/admin/users?limit=100"),
            authenticatedFetch("/api/v1/admin/coding/problems"),
            authenticatedFetch("/api/v1/admin/audit-events?limit=100")
        ]);
        for (const response of [overviewResponse, usersResponse, problemsResponse, auditResponse]) {
            if (!response.ok) throw await apiError(response);
        }
        overview = await overviewResponse.json();
        users = await usersResponse.json();
        problems = await problemsResponse.json();
        auditEvents = await auditResponse.json();
        renderAll();
        loading.hidden = true;
        content.hidden = false;
    } catch (error) {
        if (error?.status === 401) {
            window.location.replace("/login");
            return;
        }
        if (error?.status === 403) {
            loading.hidden = true;
            accessDenied.hidden = false;
            return;
        }
        showAdminError(error.message);
    }
}

function renderAll() {
    renderOverview();
    renderUsers();
    renderProblems();
    renderAuditEvents();
}

function renderOverview() {
    text("#admin-total-users", overview.totalUsers);
    text("#admin-active-users", `${overview.activeUsers} active`);
    text("#admin-suspended-users", overview.suspendedUsers);
    text("#admin-active-problems", overview.activeCodingProblems);
    text("#admin-total-problems", `${overview.totalCodingProblems} total`);
    text("#admin-activity-total", Number(overview.codingSubmissions) + Number(overview.interviewSessions));
}

async function loadOverview() {
    const response = await authenticatedFetch("/api/v1/admin/overview");
    if (!response.ok) throw await apiError(response);
    overview = await response.json();
    renderOverview();
}

async function filterUsers(event) {
    event?.preventDefault();
    const params = new URLSearchParams({limit: "100"});
    const query = document.querySelector("#admin-user-search").value.trim();
    const status = document.querySelector("#admin-user-status").value;
    if (query) params.set("q", query);
    if (status !== "ALL") params.set("status", status);
    try {
        const response = await authenticatedFetch(`/api/v1/admin/users?${params}`);
        if (!response.ok) throw await apiError(response);
        users = await response.json();
        renderUsers();
    } catch (error) {
        showToast(error.message || "Users could not be loaded.", true);
    }
}

function renderUsers() {
    const target = document.querySelector("#admin-user-list");
    target.replaceChildren();
    if (!users.length) {
        target.append(emptyState("No users match this filter", "Try another name, email, or account status."));
        return;
    }
    users.forEach(user => target.append(userRow(user)));
}

function userRow(user) {
    const row = document.createElement("article");
    row.className = "admin-user-row";
    const avatar = document.createElement("span");
    avatar.className = "admin-user-avatar";
    avatar.textContent = user.fullName?.trim()?.charAt(0)?.toUpperCase() || "U";
    const identity = document.createElement("div");
    identity.className = "admin-user-identity";
    const name = document.createElement("strong");
    name.textContent = user.fullName;
    const email = document.createElement("span");
    email.textContent = user.email;
    identity.append(name, email);
    const roles = document.createElement("div");
    roles.className = "admin-role-list";
    (user.roles || []).forEach(value => {
        const role = document.createElement("span");
        role.textContent = value === "ROLE_ADMIN" ? "Admin" : "User";
        roles.append(role);
    });
    const status = document.createElement("span");
    status.className = `account-status account-status-${user.status.toLowerCase()}`;
    status.textContent = label(user.status);
    const activity = document.createElement("span");
    activity.className = "admin-user-date";
    activity.textContent = user.lastLoginAt ? `Last login ${formatDateTime(user.lastLoginAt)}` : `Joined ${formatDateTime(user.createdAt)}`;
    const actions = document.createElement("div");
    actions.className = "admin-row-actions";
    const isSelf = user.id === currentUser.id;
    if (user.status === "ACTIVE" || user.status === "SUSPENDED" || user.status === "PENDING") {
        const nextStatus = user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE";
        const button = document.createElement("button");
        button.type = "button";
        button.className = nextStatus === "SUSPENDED" ? "admin-suspend-button" : "admin-activate-button";
        button.textContent = nextStatus === "SUSPENDED" ? "Suspend" : "Activate";
        button.disabled = isSelf;
        button.title = isSelf ? "Administrators cannot change their own status" : "";
        button.addEventListener("click", () => confirmUserStatus(user, nextStatus));
        actions.append(button);
    } else {
        const unavailable = document.createElement("span");
        unavailable.textContent = "No action";
        actions.append(unavailable);
    }
    row.append(avatar, identity, roles, status, activity, actions);
    return row;
}

function confirmUserStatus(user, status) {
    const suspending = status === "SUSPENDED";
    text("#admin-confirm-title", `${suspending ? "Suspend" : "Activate"} ${user.fullName}?`);
    text("#admin-confirm-copy", suspending
        ? "This immediately revokes active refresh sessions and blocks future authenticated access. The change is audited."
        : "This restores authenticated access to the account. The change is audited.");
    text("#confirm-admin-action", suspending ? "Suspend user" : "Activate user");
    document.querySelector("#confirm-admin-action").className = suspending ? "button danger-button" : "button button-primary";
    pendingAction = () => changeUserStatus(user.id, status);
    confirmDialog.showModal();
}

async function changeUserStatus(userId, status) {
    setOverlayTitle(`Changing account status to ${label(status)}…`);
    overlay.hidden = false;
    try {
        const response = await authenticatedFetch(`/api/v1/admin/users/${userId}/status`, {
            method: "PATCH",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({status})
        });
        if (!response.ok) throw await apiError(response);
        const updated = await response.json();
        users = users.map(user => user.id === updated.id ? updated : user);
        renderUsers();
        await Promise.allSettled([loadOverview(), refreshAudit()]);
        showToast(`User status changed to ${label(status)}.`);
    } catch (error) {
        showToast(error.message || "The account status could not be changed.", true);
    } finally {
        overlay.hidden = true;
    }
}

function renderProblems() {
    const target = document.querySelector("#admin-problem-list");
    target.replaceChildren();
    if (!problems.length) {
        target.append(emptyState("No coding problems", "Add the first problem to the candidate catalogue."));
        return;
    }
    problems.forEach(problem => target.append(problemRow(problem)));
}

function problemRow(problem) {
    const row = document.createElement("article");
    row.className = "admin-problem-row";
    const status = document.createElement("span");
    status.className = problem.active ? "problem-publish-status active" : "problem-publish-status inactive";
    status.textContent = problem.active ? "Live" : "Off";
    const identity = document.createElement("div");
    identity.className = "admin-problem-identity";
    const title = document.createElement("strong");
    title.textContent = problem.title;
    const slug = document.createElement("span");
    slug.textContent = problem.slug;
    identity.append(title, slug);
    const difficulty = document.createElement("span");
    difficulty.className = `difficulty-badge difficulty-${problem.difficulty.toLowerCase()}`;
    difficulty.textContent = label(problem.difficulty);
    const meta = document.createElement("span");
    meta.className = "admin-problem-meta";
    meta.textContent = `${Array.isArray(problem.tags) ? problem.tags.length : 0} tags · ${Array.isArray(problem.testCases) ? problem.testCases.length : 0} hidden tests`;
    const updated = document.createElement("span");
    updated.className = "admin-problem-date";
    updated.textContent = `Updated ${formatDateTime(problem.updatedAt)}`;
    const actions = document.createElement("div");
    actions.className = "admin-row-actions";
    const edit = document.createElement("button");
    edit.type = "button";
    edit.textContent = "Edit";
    edit.addEventListener("click", () => openProblemDialog(problem));
    actions.append(edit);
    if (problem.active) {
        const deactivate = document.createElement("button");
        deactivate.type = "button";
        deactivate.className = "admin-suspend-button";
        deactivate.textContent = "Deactivate";
        deactivate.addEventListener("click", () => confirmProblemDeactivation(problem));
        actions.append(deactivate);
    }
    row.append(status, identity, difficulty, meta, updated, actions);
    return row;
}

function openProblemDialog(problem = null) {
    clearProblemForm();
    text("#problem-dialog-title", problem ? "Edit coding problem" : "Add coding problem");
    text("#save-problem", problem ? "Save changes" : "Create problem");
    if (problem) {
        document.querySelector("#admin-problem-id").value = problem.id;
        document.querySelector("#admin-problem-title").value = problem.title;
        document.querySelector("#admin-problem-slug").value = problem.slug;
        document.querySelector("#admin-problem-description").value = problem.description;
        document.querySelector("#admin-problem-difficulty").value = problem.difficulty;
        document.querySelector("#admin-problem-active").checked = Boolean(problem.active);
        document.querySelector("#admin-problem-tags").value = Array.isArray(problem.tags) ? problem.tags.join(", ") : "";
        document.querySelector("#starter-java").value = problem.starterCode?.JAVA || "";
        document.querySelector("#starter-python").value = problem.starterCode?.PYTHON || "";
        document.querySelector("#starter-javascript").value = problem.starterCode?.JAVASCRIPT || "";
        document.querySelector("#starter-cpp").value = problem.starterCode?.CPP || "";
        document.querySelector("#admin-test-cases").value = JSON.stringify(problem.testCases || [], null, 2);
    }
    problemDialog.showModal();
}

function clearProblemForm() {
    document.querySelector("#problem-form").reset();
    document.querySelector("#admin-problem-id").value = "";
    document.querySelector("#admin-problem-active").checked = true;
    document.querySelectorAll("[data-problem-error]").forEach(target => target.textContent = "");
    document.querySelector("#problem-form-alert").hidden = true;
}

function collectProblemRequest() {
    const title = document.querySelector("#admin-problem-title").value.trim();
    const slug = document.querySelector("#admin-problem-slug").value.trim();
    const description = document.querySelector("#admin-problem-description").value.trim();
    const tags = [...new Set(document.querySelector("#admin-problem-tags").value.split(",").map(value => value.trim()).filter(Boolean))];
    const starterCode = {};
    const starters = {JAVA: "#starter-java", PYTHON: "#starter-python", JAVASCRIPT: "#starter-javascript", CPP: "#starter-cpp"};
    Object.entries(starters).forEach(([language, selector]) => {
        const value = document.querySelector(selector).value;
        if (value.trim()) starterCode[language] = value;
    });
    let testCases;
    try { testCases = JSON.parse(document.querySelector("#admin-test-cases").value); }
    catch (ignored) { showProblemError("testCases", "Enter a valid JSON array."); return null; }

    let valid = true;
    if (!title) { showProblemError("title", "Title is required."); valid = false; }
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) { showProblemError("slug", "Use lowercase letters, numbers, and single hyphens."); valid = false; }
    if (!description) { showProblemError("description", "Description is required."); valid = false; }
    if (tags.length < 1 || tags.length > 20 || tags.some(tag => tag.length > 60)) { showProblemError("tags", "Use 1–20 tags, each up to 60 characters."); valid = false; }
    if (!Object.keys(starterCode).length) { showProblemError("starterCode", "Add starter code for at least one supported language."); valid = false; }
    if (!Array.isArray(testCases) || testCases.length < 1 || testCases.length > 100
        || testCases.some(item => !item || typeof item !== "object" || !("input" in item) || !("expected" in item))) {
        showProblemError("testCases", "Use 1–100 objects containing input and expected values.");
        valid = false;
    }
    if (!valid) return null;
    return {title, slug, description, difficulty: document.querySelector("#admin-problem-difficulty").value, starterCode, testCases, tags, active: document.querySelector("#admin-problem-active").checked};
}

async function saveProblem(event) {
    event.preventDefault();
    document.querySelectorAll("[data-problem-error]").forEach(target => target.textContent = "");
    document.querySelector("#problem-form-alert").hidden = true;
    const request = collectProblemRequest();
    if (!request) return;
    const id = Number(document.querySelector("#admin-problem-id").value);
    const editing = Number.isSafeInteger(id) && id > 0;
    const saveButton = document.querySelector("#save-problem");
    saveButton.disabled = true;
    try {
        const response = await authenticatedFetch(editing ? `/api/v1/admin/coding/problems/${id}` : "/api/v1/admin/coding/problems", {
            method: editing ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(request)
        });
        if (!response.ok) throw await apiError(response);
        const saved = await response.json();
        if (editing) problems = problems.map(problem => problem.id === saved.id ? saved : problem);
        else problems.unshift(saved);
        problemDialog.close();
        renderProblems();
        await Promise.allSettled([loadOverview(), refreshAudit()]);
        showToast(editing ? "Coding problem updated." : "Coding problem created.");
    } catch (error) {
        if (error.fieldErrors) Object.entries(error.fieldErrors).forEach(([field, message]) => showProblemError(field, message));
        const alert = document.querySelector("#problem-form-alert");
        alert.textContent = error.message || "The coding problem could not be saved.";
        alert.hidden = false;
    } finally {
        saveButton.disabled = false;
    }
}

function showProblemError(field, message) {
    const target = document.querySelector(`[data-problem-error="${field}"]`);
    if (target) target.textContent = message;
}

function confirmProblemDeactivation(problem) {
    text("#admin-confirm-title", `Deactivate ${problem.title}?`);
    text("#admin-confirm-copy", "Candidates will no longer see this problem. Existing submissions remain available and the change is audited.");
    text("#confirm-admin-action", "Deactivate problem");
    document.querySelector("#confirm-admin-action").className = "button danger-button";
    pendingAction = () => deactivateProblem(problem.id);
    confirmDialog.showModal();
}

async function deactivateProblem(problemId) {
    setOverlayTitle("Deactivating coding problem…");
    overlay.hidden = false;
    try {
        const response = await authenticatedFetch(`/api/v1/admin/coding/problems/${problemId}`, {method: "DELETE"});
        if (!response.ok) throw await apiError(response);
        problems = problems.map(problem => problem.id === problemId ? {...problem, active: false} : problem);
        renderProblems();
        await Promise.allSettled([loadOverview(), refreshAudit()]);
        showToast("Coding problem deactivated.");
    } catch (error) {
        showToast(error.message || "The problem could not be deactivated.", true);
    } finally {
        overlay.hidden = true;
    }
}

async function refreshAudit() {
    const response = await authenticatedFetch("/api/v1/admin/audit-events?limit=100");
    if (!response.ok) throw await apiError(response);
    auditEvents = await response.json();
    renderAuditEvents();
}

function renderAuditEvents() {
    const target = document.querySelector("#audit-list");
    target.replaceChildren();
    if (!auditEvents.length) {
        target.append(emptyState("No audit events", "Privileged user and problem changes will appear here."));
        return;
    }
    auditEvents.forEach(event => target.append(auditRow(event)));
}

function auditRow(event) {
    const row = document.createElement("article");
    row.className = "audit-row";
    const outcome = document.createElement("span");
    outcome.className = `audit-outcome audit-${event.outcome.toLowerCase()}`;
    outcome.textContent = event.outcome === "SUCCESS" ? "✓" : "!";
    const identity = document.createElement("div");
    identity.className = "audit-identity";
    const action = document.createElement("strong");
    action.textContent = label(event.action);
    const target = document.createElement("span");
    target.textContent = `${label(event.targetType)} · ${event.targetId || "platform"}`;
    identity.append(action, target);
    const actor = document.createElement("span");
    actor.textContent = event.actorUserId == null ? "System" : `User #${event.actorUserId}`;
    const correlation = document.createElement("code");
    correlation.textContent = event.correlationId || "No correlation ID";
    const metadata = document.createElement("span");
    metadata.className = "audit-metadata";
    metadata.textContent = formatMetadata(event.metadata);
    const time = document.createElement("time");
    time.dateTime = event.createdAt;
    time.textContent = formatDateTime(event.createdAt);
    row.append(outcome, identity, actor, correlation, metadata, time);
    return row;
}

function formatMetadata(value) {
    if (!value || typeof value !== "object" || !Object.keys(value).length) return "No metadata";
    return Object.entries(value).map(([key, item]) => `${label(key)}: ${String(item)}`).join(" · ");
}

function switchTab(name) {
    document.querySelectorAll("[data-admin-tab]").forEach(button => button.classList.toggle("active", button.dataset.adminTab === name));
    document.querySelectorAll("[data-admin-panel]").forEach(panel => panel.hidden = panel.dataset.adminPanel !== name);
}

function emptyState(titleText, copyText) {
    const empty = document.createElement("div");
    empty.className = "admin-empty";
    const title = document.createElement("strong");
    title.textContent = titleText;
    const copy = document.createElement("span");
    copy.textContent = copyText;
    empty.append(title, copy);
    return empty;
}

function setOverlayTitle(value) { text("#admin-overlay-title", value); }
function showAdminError(message) { loading.hidden = true; content.hidden = true; errorBox.hidden = false; text("#admin-error-message", message || "Please refresh and try again."); }
function showToast(message, isError = false) { const toast = document.querySelector("#admin-toast"); clearTimeout(toastTimer); toast.textContent = message; toast.classList.toggle("toast-error", isError); toast.hidden = false; toastTimer = setTimeout(() => toast.hidden = true, 3800); }
function formatDateTime(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? "Unknown date" : new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit"}).format(date); }
function label(value) { return String(value || "").toLowerCase().replaceAll("_", " ").replace(/\b\w/g, letter => letter.toUpperCase()); }
function text(selector, value) { const target = document.querySelector(selector); if (target) target.textContent = String(value); }

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}

document.querySelectorAll("[data-admin-tab]").forEach(button => button.addEventListener("click", () => switchTab(button.dataset.adminTab)));
document.querySelector("#user-filter-form").addEventListener("submit", filterUsers);
["#open-create-problem", "#open-create-problem-inline"].forEach(selector => document.querySelector(selector).addEventListener("click", () => openProblemDialog()));
document.querySelector("#problem-form").addEventListener("submit", saveProblem);
document.querySelector("#close-problem-dialog").addEventListener("click", () => problemDialog.close());
document.querySelector("#cancel-problem").addEventListener("click", () => problemDialog.close());
document.querySelector("#cancel-admin-action").addEventListener("click", () => { pendingAction = null; confirmDialog.close(); });
document.querySelector("#confirm-admin-action").addEventListener("click", async () => { const action = pendingAction; pendingAction = null; confirmDialog.close(); if (action) await action(); });
document.querySelector("#refresh-audit").addEventListener("click", async () => { try { await refreshAudit(); showToast("Audit trail refreshed."); } catch (error) { showToast(error.message, true); } });
document.querySelector("#menu-button").addEventListener("click", () => document.querySelector(".sidebar").classList.toggle("open"));
document.querySelector("#logout-button").addEventListener("click", async () => { await logout(); window.location.replace("/login"); });
