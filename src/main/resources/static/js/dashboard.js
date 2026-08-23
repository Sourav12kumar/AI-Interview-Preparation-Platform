import {authenticatedFetch, getCurrentUser, logout, refreshSession} from "./auth-client.js";

const loading = document.querySelector("#dashboard-loading");
const content = document.querySelector("#dashboard-content");
const errorBox = document.querySelector("#dashboard-error");

boot();

async function boot() {
    try {
        await refreshSession();
        renderUser(getCurrentUser());
        const response = await authenticatedFetch("/api/v1/analytics/dashboard");
        if (!response.ok) throw new Error("Dashboard request failed");
        renderDashboard(await response.json());
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

function renderUser(user) {
    const firstName = user.fullName.trim().split(/\s+/)[0];
    document.querySelector("#user-name").textContent = firstName;
    document.querySelector("#profile-name").textContent = user.fullName;
    document.querySelector("#user-initial").textContent = firstName.charAt(0).toUpperCase();
    document.querySelector("#profile-role").textContent = user.roles.includes("ROLE_ADMIN") ? "Administrator" : "Candidate";
    document.querySelector("#dashboard-admin-link").hidden = !user.roles.includes("ROLE_ADMIN");
}

function renderDashboard(data) {
    const overview = data.overview;
    text("#interview-score", score(overview.averageInterviewScore));
    text("#coding-score", score(overview.averageCodingScore));
    text("#interview-count", overview.interviewsCompleted);
    text("#acceptance-rate", percentage(overview.codingAcceptanceRate));
    text("#submission-count", `${overview.codingSubmissions} coding submission${overview.codingSubmissions === 1 ? "" : "s"}`);
    renderTopics("#strong-topics", data.strongestTopics, "No performance data yet");
    renderTopics("#improvement-topics", data.improvementTopics, "Complete a practice session to begin");
}

function renderTopics(selector, topics, emptyText) {
    const target = document.querySelector(selector);
    target.replaceChildren();
    if (!topics.length) {
        const item = document.createElement("span");
        item.textContent = emptyText;
        target.append(item);
        return;
    }
    topics.slice(0, 5).forEach(topic => {
        const item = document.createElement("span");
        item.textContent = `${topic.topic} · ${Math.round(topic.averageScore)}%`;
        target.append(item);
    });
}

function score(value) { return value == null ? "—" : `${Math.round(value)}`; }
function percentage(value) { return value == null ? "—" : `${Math.round(value)}%`; }
function text(selector, value) { document.querySelector(selector).textContent = value; }

document.querySelector("#logout-button")?.addEventListener("click", async () => {
    await logout();
    window.location.replace("/login");
});
document.querySelector("#menu-button")?.addEventListener("click", () => {
    document.querySelector(".sidebar")?.classList.toggle("open");
});
