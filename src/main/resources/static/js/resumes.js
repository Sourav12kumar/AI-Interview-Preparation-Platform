import {authenticatedFetch, logout, refreshSession} from "./auth-client.js";

const MAX_FILE_BYTES = 5 * 1024 * 1024;
const ALLOWED_EXTENSIONS = new Set(["pdf", "docx", "txt"]);

let resumes = [];
let selectedResumeId = null;
let selectedFile = null;
let profileTargetRole = "";
let toastTimer;

const loading = document.querySelector("#resumes-loading");
const content = document.querySelector("#resumes-content");
const errorBox = document.querySelector("#resumes-error");
const fileInput = document.querySelector("#resume-file");
const dropZone = document.querySelector("#resume-drop-zone");
const uploadButton = document.querySelector("#upload-resume");
const analysisForm = document.querySelector("#analysis-form");
const deleteDialog = document.querySelector("#delete-resume-dialog");
const overlay = document.querySelector("#resume-overlay");

boot();

async function boot() {
    try {
        await refreshSession();
        const [resumeResponse, profileResponse] = await Promise.all([
            authenticatedFetch("/api/v1/resumes"),
            authenticatedFetch("/api/v1/profile")
        ]);
        if (!resumeResponse.ok) throw await apiError(resumeResponse);
        resumes = await resumeResponse.json();
        if (profileResponse.ok) {
            const profile = await profileResponse.json();
            profileTargetRole = profile.targetRole || "";
        }
        renderWorkspace();
        if (resumes.length) selectResume(resumes[0].id);
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

function renderWorkspace() {
    renderStats();
    renderResumeList();
}

function renderStats() {
    const completed = resumes.filter(resume => resume.analysisStatus === "COMPLETED");
    const scores = completed.map(resume => Number(resume.atsScore)).filter(Number.isFinite);
    const latestAnalyzed = completed.find(resume => resume.targetRole);
    text("#resume-total", resumes.length);
    text("#resume-completed", completed.length);
    text("#resume-best-score", scores.length ? `${Math.round(Math.max(...scores))}` : "—");
    text("#resume-latest-role", latestAnalyzed?.targetRole || "Not set");
}

function renderResumeList() {
    const target = document.querySelector("#resume-list");
    target.replaceChildren();
    if (!resumes.length) {
        const empty = document.createElement("div");
        empty.className = "resume-list-empty";
        const title = document.createElement("strong");
        title.textContent = "No resumes uploaded";
        const copy = document.createElement("span");
        copy.textContent = "Your private resume history will appear here.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    resumes.forEach(resume => target.append(resumeListItem(resume)));
}

function resumeListItem(resume) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "resume-list-item";
    if (resume.id === selectedResumeId) button.classList.add("active");
    button.addEventListener("click", () => selectResume(resume.id));

    const type = document.createElement("span");
    type.className = "resume-list-type";
    type.textContent = extension(resume.originalFilename).toUpperCase();
    const identity = document.createElement("span");
    identity.className = "resume-list-identity";
    const name = document.createElement("strong");
    name.textContent = resume.originalFilename;
    const meta = document.createElement("small");
    meta.textContent = `${formatBytes(resume.fileSizeBytes)} · ${formatDate(resume.createdAt)}`;
    identity.append(name, meta);
    const status = document.createElement("span");
    status.className = `resume-status resume-status-${resume.analysisStatus.toLowerCase()}`;
    status.textContent = resume.analysisStatus === "COMPLETED" ? `${Math.round(Number(resume.atsScore))}` : label(resume.analysisStatus);
    button.append(type, identity, status);
    return button;
}

function selectResume(id) {
    const resume = resumes.find(item => item.id === id);
    if (!resume) return;
    selectedResumeId = id;
    renderResumeList();
    document.querySelector("#resume-empty-detail").hidden = true;
    document.querySelector("#resume-detail").hidden = false;
    text("#selected-file-type", extension(resume.originalFilename).toUpperCase());
    text("#selected-file-name", resume.originalFilename);
    text("#selected-file-meta", `${formatBytes(resume.fileSizeBytes)} · uploaded ${formatDate(resume.createdAt)}`);
    text("#analysis-status-label", statusMessage(resume.analysisStatus));
    const roleInput = document.querySelector("#analysis-target-role");
    roleInput.value = resume.targetRole || profileTargetRole;
    document.querySelector("#job-description").value = "";
    updateJobDescriptionCount();
    clearAnalysisErrors();
    renderAnalysis(resume);
}

function renderAnalysis(resume) {
    const result = document.querySelector("#resume-result");
    if (resume.analysisStatus !== "COMPLETED") {
        result.hidden = true;
        if (resume.analysisStatus === "FAILED" && resume.failureReason) {
            showAlert("#analysis-alert", resume.failureReason);
        }
        text("#analyze-resume", resume.analysisStatus === "FAILED" ? "Try analysis again" : "Analyze with Gemini");
        return;
    }

    document.querySelector("#analysis-alert").hidden = true;
    text("#analyze-resume", "Analyze again");
    const score = clampScore(resume.atsScore);
    text("#ats-score", Math.round(score));
    const progress = document.querySelector("#ats-progress");
    progress.value = score;
    progress.textContent = `${Math.round(score)}%`;
    text("#ats-verdict", verdict(score));
    text("#ats-summary", resume.summary || "Gemini completed the role-specific resume analysis.");
    text("#analysis-model", `${resume.aiModel || "Gemini"} · ${formatDate(resume.analyzedAt)}`);
    renderStringList("#resume-strengths", resume.strengths, "No strengths were returned.");
    renderStringList("#resume-weaknesses", resume.weaknesses, "No weaknesses were returned.");
    renderKeywords(resume.missingKeywords);
    renderSuggestions(resume.suggestions);
    result.hidden = false;
}

function renderStringList(selector, values, fallback) {
    const target = document.querySelector(selector);
    target.replaceChildren();
    const items = Array.isArray(values) && values.length ? values : [fallback];
    items.forEach(value => {
        const item = document.createElement("li");
        item.textContent = value;
        target.append(item);
    });
}

function renderKeywords(values) {
    const target = document.querySelector("#missing-keywords");
    target.replaceChildren();
    const keywords = Array.isArray(values) ? values : [];
    if (!keywords.length) {
        const empty = document.createElement("span");
        empty.className = "keyword-empty";
        empty.textContent = "No important keyword gaps were identified.";
        target.append(empty);
        return;
    }
    keywords.forEach(value => {
        const keyword = document.createElement("span");
        keyword.textContent = value;
        target.append(keyword);
    });
}

function renderSuggestions(values) {
    const target = document.querySelector("#resume-suggestions");
    target.replaceChildren();
    const suggestions = Array.isArray(values) && values.length ? values : ["Review the summary and make one evidence-based improvement."];
    suggestions.forEach((value, index) => {
        const item = document.createElement("li");
        const number = document.createElement("span");
        number.textContent = String(index + 1).padStart(2, "0");
        const copy = document.createElement("p");
        copy.textContent = value;
        item.append(number, copy);
        target.append(item);
    });
}

function chooseFile(file) {
    clearUploadError();
    const validation = validateFile(file);
    if (validation) {
        selectedFile = null;
        uploadButton.disabled = true;
        showAlert("#upload-alert", validation);
        resetUploadLabel();
        return;
    }
    selectedFile = file;
    text("#upload-title", file.name);
    text("#upload-copy", `${formatBytes(file.size)} · ready for secure upload`);
    uploadButton.disabled = false;
}

function validateFile(file) {
    if (!file) return "Choose a resume file before uploading.";
    const fileExtension = extension(file.name);
    if (!ALLOWED_EXTENSIONS.has(fileExtension)) return "Only PDF, DOCX, and TXT resumes are supported.";
    if (file.size === 0) return "The selected file is empty.";
    if (file.size > MAX_FILE_BYTES) return "Resume file must not exceed 5 MB.";
    return "";
}

async function uploadResume() {
    const validation = validateFile(selectedFile);
    if (validation) {
        showAlert("#upload-alert", validation);
        return;
    }
    setOverlay("Secure upload", "Uploading and extracting resume text…", "The file is validated before it is added to your private history.");
    overlay.hidden = false;
    uploadButton.disabled = true;
    try {
        const formData = new FormData();
        formData.append("file", selectedFile, selectedFile.name);
        const response = await authenticatedFetch("/api/v1/resumes", {method: "POST", body: formData});
        if (!response.ok) throw await apiError(response);
        const uploaded = await response.json();
        resumes.unshift(uploaded);
        selectedFile = null;
        fileInput.value = "";
        resetUploadLabel();
        renderWorkspace();
        selectResume(uploaded.id);
        showToast("Resume uploaded securely. Configure the target and run the analysis.");
    } catch (error) {
        showAlert("#upload-alert", error.message || "The resume could not be uploaded.");
    } finally {
        overlay.hidden = true;
        uploadButton.disabled = selectedFile == null;
    }
}

async function analyzeResume(event) {
    event.preventDefault();
    clearAnalysisErrors();
    const resume = currentResume();
    if (!resume) return;
    const targetRole = document.querySelector("#analysis-target-role").value.trim();
    const jobDescription = document.querySelector("#job-description").value.trim();
    if (!targetRole) {
        showFieldError("targetRole", "Enter the role you want this resume to target.");
        return;
    }
    if (targetRole.length > 120 || jobDescription.length > 20000) return;

    setOverlay("Gemini ATS analysis", "Comparing your resume with the target role…", "Reviewing evidence, keywords, strengths, weaknesses, and practical improvements.");
    overlay.hidden = false;
    const submit = document.querySelector("#analyze-resume");
    submit.disabled = true;
    try {
        const response = await authenticatedFetch(`/api/v1/resumes/${resume.id}/analysis`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({targetRole, jobDescription: jobDescription || null})
        });
        if (!response.ok) throw await apiError(response);
        const analyzed = await response.json();
        replaceResume(analyzed);
        renderWorkspace();
        selectResume(analyzed.id);
        document.querySelector("#resume-result").scrollIntoView({behavior: "smooth", block: "start"});
        showToast("Gemini analysis completed.");
    } catch (error) {
        if (error.fieldErrors) applyAnalysisErrors(error.fieldErrors);
        showAlert("#analysis-alert", error.message || "Gemini could not analyze this resume.");
        await refreshSelectedResume();
    } finally {
        overlay.hidden = true;
        submit.disabled = false;
    }
}

async function refreshSelectedResume() {
    const resume = currentResume();
    if (!resume) return;
    try {
        const response = await authenticatedFetch(`/api/v1/resumes/${resume.id}`);
        if (!response.ok) return;
        const refreshed = await response.json();
        replaceResume(refreshed);
        renderWorkspace();
        selectResume(refreshed.id);
    } catch (ignored) {
        // The original API error remains visible.
    }
}

async function deleteResume() {
    const resume = currentResume();
    if (!resume) return;
    const confirmButton = document.querySelector("#confirm-delete-resume");
    confirmButton.disabled = true;
    try {
        const response = await authenticatedFetch(`/api/v1/resumes/${resume.id}`, {method: "DELETE"});
        if (!response.ok) throw await apiError(response);
        resumes = resumes.filter(item => item.id !== resume.id);
        selectedResumeId = null;
        deleteDialog.close();
        renderWorkspace();
        if (resumes.length) {
            selectResume(resumes[0].id);
        } else {
            document.querySelector("#resume-detail").hidden = true;
            document.querySelector("#resume-empty-detail").hidden = false;
        }
        showToast("Resume and analysis deleted.");
    } catch (error) {
        deleteDialog.close();
        showToast(error.message || "The resume could not be deleted.", true);
    } finally {
        confirmButton.disabled = false;
    }
}

function replaceResume(updated) {
    resumes = resumes.map(resume => resume.id === updated.id ? updated : resume);
}

function currentResume() {
    return resumes.find(resume => resume.id === selectedResumeId);
}

function applyAnalysisErrors(errors) {
    Object.entries(errors).forEach(([field, message]) => showFieldError(field, message));
}

function showFieldError(field, message) {
    const target = document.querySelector(`[data-analysis-error="${field}"]`);
    if (target) target.textContent = message;
}

function clearAnalysisErrors() {
    document.querySelectorAll("[data-analysis-error]").forEach(target => target.textContent = "");
    document.querySelector("#analysis-alert").hidden = true;
}

function clearUploadError() {
    document.querySelector("#upload-alert").hidden = true;
}

function resetUploadLabel() {
    text("#upload-title", "Choose or drop a file");
    text("#upload-copy", "PDF, DOCX, or TXT · maximum 5 MB");
}

function setOverlay(labelText, titleText, copyText) {
    text("#resume-overlay-label", labelText);
    text("#resume-overlay-title", titleText);
    text("#resume-overlay-copy", copyText);
}

function showAlert(selector, message) {
    const alert = document.querySelector(selector);
    alert.textContent = message;
    alert.hidden = false;
}

function showToast(message, isError = false) {
    const toast = document.querySelector("#resume-toast");
    clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.toggle("toast-error", isError);
    toast.hidden = false;
    toastTimer = setTimeout(() => toast.hidden = true, 3600);
}

function updateJobDescriptionCount() {
    const value = document.querySelector("#job-description").value.length;
    text("#job-description-count", `${value} / 20000`);
}

function statusMessage(status) {
    if (status === "COMPLETED") return "Analysis complete";
    if (status === "FAILED") return "Previous analysis failed";
    if (status === "PROCESSING") return "Analysis processing";
    return "Ready to analyze";
}

function verdict(score) {
    if (score >= 85) return "Strong match for this target";
    if (score >= 70) return "Competitive foundation with clear opportunities";
    if (score >= 50) return "Relevant experience needs stronger positioning";
    return "A focused rewrite can improve role alignment";
}

function clampScore(value) {
    const score = Number(value);
    return Number.isFinite(score) ? Math.min(100, Math.max(0, score)) : 0;
}

function extension(filename) {
    const parts = String(filename || "file").split(".");
    return parts.length > 1 ? parts.pop().toLowerCase() : "file";
}

function formatBytes(value) {
    const bytes = Number(value);
    if (!Number.isFinite(bytes) || bytes < 1024) return `${Math.max(0, bytes || 0)} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(value) {
    if (!value) return "Not analyzed";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "Unknown date";
    return new Intl.DateTimeFormat(undefined, {day: "numeric", month: "short", year: "numeric"}).format(date);
}

function label(value) {
    return String(value || "").toLowerCase().replaceAll("_", " ").replace(/\b\w/g, letter => letter.toUpperCase());
}

function text(selector, value) {
    const target = document.querySelector(selector);
    if (target) target.textContent = String(value);
}

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}

fileInput.addEventListener("change", () => chooseFile(fileInput.files?.[0]));
dropZone.addEventListener("dragover", event => {
    event.preventDefault();
    dropZone.classList.add("dragging");
});
dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragging"));
dropZone.addEventListener("drop", event => {
    event.preventDefault();
    dropZone.classList.remove("dragging");
    chooseFile(event.dataTransfer?.files?.[0]);
});
uploadButton.addEventListener("click", uploadResume);
document.querySelector("#header-upload-button").addEventListener("click", () => fileInput.click());
analysisForm.addEventListener("submit", analyzeResume);
document.querySelector("#job-description").addEventListener("input", updateJobDescriptionCount);
document.querySelector("#open-delete-resume").addEventListener("click", () => deleteDialog.showModal());
document.querySelector("#cancel-delete-resume").addEventListener("click", () => deleteDialog.close());
document.querySelector("#confirm-delete-resume").addEventListener("click", deleteResume);
document.querySelector("#menu-button").addEventListener("click", () => document.querySelector(".sidebar").classList.toggle("open"));
document.querySelector("#logout-button").addEventListener("click", async () => {
    await logout();
    window.location.replace("/login");
});
