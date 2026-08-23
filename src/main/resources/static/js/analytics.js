import {authenticatedFetch, getCurrentUser, logout, refreshSession} from "./auth-client.js";

const SVG_NS = "http://www.w3.org/2000/svg";
let dashboard = null;
let reports = [];
let activeReportId = null;
let toastTimer;

const loading = document.querySelector("#analytics-loading");
const content = document.querySelector("#analytics-content");
const errorBox = document.querySelector("#analytics-error");
const overlay = document.querySelector("#analytics-overlay");
const fromInput = document.querySelector("#analytics-from");
const toInput = document.querySelector("#analytics-to");

boot();

async function boot() {
    try {
        await refreshSession();
        const user = getCurrentUser();
        document.querySelector("#analytics-admin-link").hidden = !user?.roles?.includes("ROLE_ADMIN");
        setPreset(30);
        const [dashboardResponse, reportsResponse] = await Promise.all([
            fetchDashboard(),
            authenticatedFetch("/api/v1/analytics/reports")
        ]);
        if (!dashboardResponse.ok) throw await apiError(dashboardResponse);
        if (!reportsResponse.ok) throw await apiError(reportsResponse);
        dashboard = await dashboardResponse.json();
        reports = await reportsResponse.json();
        renderDashboard();
        renderReports();
        loading.hidden = true;
        content.hidden = false;
    } catch (error) {
        if (error?.status === 401) {
            window.location.replace("/login");
            return;
        }
        showAnalyticsError(error.message);
    }
}

async function reloadDashboard() {
    errorBox.hidden = true;
    loading.hidden = false;
    content.hidden = true;
    try {
        const response = await fetchDashboard();
        if (!response.ok) throw await apiError(response);
        dashboard = await response.json();
        renderDashboard();
        loading.hidden = true;
        content.hidden = false;
    } catch (error) {
        showAnalyticsError(error.message);
    }
}

function fetchDashboard() {
    const params = new URLSearchParams({from: fromInput.value, to: toInput.value});
    return authenticatedFetch(`/api/v1/analytics/dashboard?${params}`);
}

function renderDashboard() {
    const overview = dashboard.overview;
    text("#analytics-interview-average", score(overview.averageInterviewScore));
    text("#analytics-interview-count", `${overview.interviewsCompleted} completed`);
    text("#analytics-coding-average", score(overview.averageCodingScore));
    text("#analytics-coding-count", `${overview.codingSubmissions} submission${overview.codingSubmissions === 1 ? "" : "s"}`);
    text("#analytics-problems", overview.codingProblemsAttempted);
    text("#analytics-acceptance", percentage(overview.codingAcceptanceRate));
    text("#analytics-accepted-count", `${overview.acceptedSubmissions} accepted`);
    fromInput.value = dashboard.period.from;
    toInput.value = dashboard.period.to;
    renderTrendChart(dashboard.trends || []);
    renderTopicList("#strongest-topic-list", dashboard.strongestTopics, "Complete practice to discover your strongest topics.");
    renderTopicList("#improvement-topic-list", dashboard.improvementTopics, "Complete practice to receive a focused improvement signal.");
}

function renderTrendChart(trends) {
    const target = document.querySelector("#trend-chart");
    target.replaceChildren();
    if (!trends.length) {
        const empty = document.createElement("div");
        empty.className = "chart-empty";
        const title = document.createElement("strong");
        title.textContent = "No activity in this period";
        const copy = document.createElement("span");
        copy.textContent = "Complete an interview or submit code to create a trend.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }

    const width = 800;
    const height = 260;
    const left = 48;
    const right = 20;
    const top = 18;
    const bottom = 38;
    const plotWidth = width - left - right;
    const plotHeight = height - top - bottom;
    const svg = svgElement("svg", {viewBox: `0 0 ${width} ${height}`, role: "img", "aria-label": "Daily average interview and coding scores"});

    [0, 50, 100].forEach(value => {
        const y = top + plotHeight - (value / 100) * plotHeight;
        svg.append(svgElement("line", {x1: left, y1: y, x2: width - right, y2: y, class: "chart-grid-line"}));
        const labelNode = svgElement("text", {x: left - 10, y: y + 4, class: "chart-axis-label", "text-anchor": "end"});
        labelNode.textContent = String(value);
        svg.append(labelNode);
    });

    const point = (index, value) => ({
        x: trends.length === 1 ? left + plotWidth / 2 : left + (index / (trends.length - 1)) * plotWidth,
        y: top + plotHeight - (clampScore(value) / 100) * plotHeight
    });
    renderSeries(svg, trends, "averageInterviewScore", "chart-interview-line", "chart-interview-point", point);
    renderSeries(svg, trends, "averageCodingScore", "chart-coding-line", "chart-coding-point", point);

    const firstLabel = svgElement("text", {x: left, y: height - 10, class: "chart-date-label", "text-anchor": "start"});
    firstLabel.textContent = formatShortDate(trends[0].date);
    const lastLabel = svgElement("text", {x: width - right, y: height - 10, class: "chart-date-label", "text-anchor": "end"});
    lastLabel.textContent = formatShortDate(trends[trends.length - 1].date);
    svg.append(firstLabel, lastLabel);
    target.append(svg);
}

function renderSeries(svg, trends, field, lineClass, pointClass, position) {
    const values = trends.map((trend, index) => trend[field] == null ? null : {...position(index, trend[field]), value: trend[field]});
    let segment = [];
    const flush = () => {
        if (segment.length > 1) {
            svg.append(svgElement("polyline", {points: segment.map(item => `${item.x},${item.y}`).join(" "), class: lineClass}));
        }
        segment.forEach(item => {
            const circle = svgElement("circle", {cx: item.x, cy: item.y, r: 4, class: pointClass});
            const title = svgElement("title", {});
            title.textContent = `${Math.round(Number(item.value))}`;
            circle.append(title);
            svg.append(circle);
        });
        segment = [];
    };
    values.forEach(item => {
        if (item == null) flush();
        else segment.push(item);
    });
    flush();
}

function renderTopicList(selector, topics, emptyText) {
    const target = document.querySelector(selector);
    target.replaceChildren();
    if (!topics?.length) {
        const empty = document.createElement("div");
        empty.className = "topic-performance-empty";
        empty.textContent = emptyText;
        target.append(empty);
        return;
    }
    topics.forEach(topic => {
        const row = document.createElement("article");
        const head = document.createElement("div");
        const identity = document.createElement("span");
        const name = document.createElement("strong");
        name.textContent = topic.topic;
        const source = document.createElement("small");
        source.textContent = `${label(topic.source)} · ${topic.attempts} attempt${topic.attempts === 1 ? "" : "s"}`;
        identity.append(name, source);
        const value = document.createElement("strong");
        value.textContent = `${Math.round(clampScore(topic.averageScore))}`;
        head.append(identity, value);
        const progress = document.createElement("progress");
        progress.max = 100;
        progress.value = clampScore(topic.averageScore);
        progress.textContent = `${Math.round(progress.value)}%`;
        row.append(head, progress);
        target.append(row);
    });
}

function renderReports() {
    const target = document.querySelector("#report-list");
    target.replaceChildren();
    if (!reports.length) {
        const empty = document.createElement("div");
        empty.className = "report-list-empty";
        const title = document.createElement("strong");
        title.textContent = "No saved reports";
        const copy = document.createElement("span");
        copy.textContent = "Generate a Gemini coaching report after completing practice.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    reports.forEach(report => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "report-list-item";
        if (report.id === activeReportId) button.classList.add("active");
        button.addEventListener("click", () => loadReport(report.id));
        const mark = document.createElement("span");
        mark.textContent = "✦";
        const identity = document.createElement("span");
        const period = document.createElement("strong");
        period.textContent = `${formatShortDate(report.periodStart)} – ${formatShortDate(report.periodEnd)}`;
        const meta = document.createElement("small");
        meta.textContent = `${report.interviewsCompleted} interviews · ${report.codingProblemsAttempted} problems`;
        identity.append(period, meta);
        const date = document.createElement("time");
        date.dateTime = report.generatedAt;
        date.textContent = formatShortDate(report.generatedAt);
        button.append(mark, identity, date);
        target.append(button);
    });
}

async function loadReport(reportId) {
    try {
        const response = await authenticatedFetch(`/api/v1/analytics/reports/${reportId}`);
        if (!response.ok) throw await apiError(response);
        const report = await response.json();
        activeReportId = report.id;
        renderReports();
        renderReportDetail(report);
    } catch (error) {
        showToast(error.message || "The coaching report could not be loaded.", true);
    }
}

function renderReportDetail(report) {
    const target = document.querySelector("#report-detail");
    target.replaceChildren();
    const head = document.createElement("div");
    head.className = "report-detail-head";
    const heading = document.createElement("div");
    const eyebrow = document.createElement("p");
    eyebrow.className = "eyebrow";
    eyebrow.textContent = "Gemini coaching report";
    const title = document.createElement("h3");
    title.textContent = `${formatLongDate(report.periodStart)} – ${formatLongDate(report.periodEnd)}`;
    heading.append(eyebrow, title);
    const model = document.createElement("span");
    model.textContent = report.aiModel || "Gemini";
    head.append(heading, model);

    const summary = document.createElement("p");
    summary.className = "report-summary";
    summary.textContent = report.summary || "Your coaching report is ready.";
    const metrics = document.createElement("div");
    metrics.className = "report-metrics";
    metrics.append(reportMetric("Interview avg", score(report.averageInterviewScore)), reportMetric("Coding avg", score(report.averageCodingScore)), reportMetric("Interviews", report.interviewsCompleted), reportMetric("Problems", report.codingProblemsAttempted));
    const recommendationTitle = document.createElement("h4");
    recommendationTitle.textContent = "Recommended next steps";
    const recommendations = document.createElement("ol");
    recommendations.className = "report-recommendations";
    (report.recommendations || []).forEach((value, index) => {
        const item = document.createElement("li");
        const number = document.createElement("span");
        number.textContent = String(index + 1).padStart(2, "0");
        const copy = document.createElement("p");
        copy.textContent = value;
        item.append(number, copy);
        recommendations.append(item);
    });
    const foot = document.createElement("small");
    foot.className = "report-generated-at";
    foot.textContent = `Generated ${formatDateTime(report.generatedAt)} · ${report.promptVersion || "current prompt"}`;
    target.append(head, summary, metrics, recommendationTitle, recommendations, foot);
}

function reportMetric(name, value) {
    const metric = document.createElement("div");
    const labelNode = document.createElement("span");
    labelNode.textContent = name;
    const valueNode = document.createElement("strong");
    valueNode.textContent = String(value);
    metric.append(labelNode, valueNode);
    return metric;
}

async function generateReport() {
    overlay.hidden = false;
    setReportButtonsDisabled(true);
    try {
        const response = await authenticatedFetch("/api/v1/analytics/reports", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({periodStart: fromInput.value, periodEnd: toInput.value})
        });
        if (!response.ok) throw await apiError(response);
        const report = await response.json();
        const summariesResponse = await authenticatedFetch("/api/v1/analytics/reports");
        if (summariesResponse.ok) reports = await summariesResponse.json();
        activeReportId = report.id;
        renderReports();
        renderReportDetail(report);
        document.querySelector("#report-detail").scrollIntoView({behavior: "smooth", block: "center"});
        showToast("Gemini coaching report saved.");
    } catch (error) {
        showToast(error.message || "The report could not be generated.", true);
    } finally {
        overlay.hidden = true;
        setReportButtonsDisabled(false);
    }
}

function setReportButtonsDisabled(disabled) {
    ["#generate-report-side", "#generate-report-header", "#generate-report-inline"].forEach(selector => document.querySelector(selector).disabled = disabled);
}

function applyCustomPeriod(event) {
    event.preventDefault();
    document.querySelectorAll("[data-days]").forEach(button => button.classList.remove("active"));
    const validation = validatePeriod(fromInput.value, toInput.value);
    if (validation) {
        showToast(validation, true);
        return;
    }
    reloadDashboard();
}

function setPreset(days) {
    const to = new Date();
    const from = new Date(to);
    from.setUTCDate(from.getUTCDate() - (days - 1));
    toInput.value = isoDate(to);
    fromInput.value = isoDate(from);
}

function validatePeriod(from, to) {
    if (!from || !to) return "Choose both period dates.";
    const start = Date.parse(`${from}T00:00:00Z`);
    const end = Date.parse(`${to}T00:00:00Z`);
    if (!Number.isFinite(start) || !Number.isFinite(end)) return "Choose valid period dates.";
    if (start > end) return "Period start must not be after its end.";
    if ((end - start) / 86400000 >= 366) return "Analytics period must not exceed 366 days.";
    if (to > isoDate(new Date())) return "Analytics period must not end in the future.";
    return "";
}

function svgElement(name, attributes) {
    const node = document.createElementNS(SVG_NS, name);
    Object.entries(attributes).forEach(([key, value]) => node.setAttribute(key, String(value)));
    return node;
}

function showAnalyticsError(message) {
    loading.hidden = true;
    content.hidden = true;
    errorBox.hidden = false;
    text("#analytics-error-message", message || "Please refresh or choose another period.");
}

function showToast(message, isError = false) {
    const toast = document.querySelector("#analytics-toast");
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("toast-error", isError);
    toast.hidden = false;
    toastTimer = setTimeout(() => toast.hidden = true, 3800);
}

function score(value) { return value == null ? "—" : `${Math.round(clampScore(value))}`; }
function percentage(value) { return value == null ? "—" : `${Math.round(clampScore(value))}%`; }
function clampScore(value) { const parsed = Number(value); return Number.isFinite(parsed) ? Math.min(100, Math.max(0, parsed)) : 0; }
function isoDate(date) { return date.toISOString().slice(0, 10); }
function formatShortDate(value) { const date = new Date(String(value).length === 10 ? `${value}T00:00:00Z` : value); return Number.isNaN(date.getTime()) ? "Unknown" : new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short"}).format(date); }
function formatLongDate(value) { const date = new Date(`${value}T00:00:00Z`); return Number.isNaN(date.getTime()) ? "Unknown" : new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short", year: "numeric"}).format(date); }
function formatDateTime(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? "Unknown date" : new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short", year: "numeric", hour: "numeric", minute: "2-digit"}).format(date); }
function label(value) { return String(value || "").toLowerCase().replaceAll("_", " ").replace(/\b\w/g, letter => letter.toUpperCase()); }
function text(selector, value) { const target = document.querySelector(selector); if (target) target.textContent = String(value); }

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}

document.querySelectorAll("[data-days]").forEach(button => button.addEventListener("click", () => {
    document.querySelectorAll("[data-days]").forEach(item => item.classList.toggle("active", item === button));
    setPreset(Number(button.dataset.days));
    reloadDashboard();
}));
document.querySelector("#analytics-period-form").addEventListener("submit", applyCustomPeriod);
["#generate-report-side", "#generate-report-header", "#generate-report-inline"].forEach(selector => document.querySelector(selector).addEventListener("click", generateReport));
document.querySelector("#menu-button").addEventListener("click", () => document.querySelector(".sidebar").classList.toggle("open"));
document.querySelector("#logout-button").addEventListener("click", async () => { await logout(); window.location.replace("/login"); });
