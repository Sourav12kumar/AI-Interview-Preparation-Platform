import {authenticatedFetch, logout, refreshSession} from "./auth-client.js";

const fields = ["headline", "phone", "location", "educationLevel", "institution", "graduationYear", "yearsOfExperience", "targetRole", "bio"];
let companies = [];
let skills = [];
let profileExists = false;

const loading = document.querySelector("#profile-loading");
const content = document.querySelector("#profile-content");
const errorBox = document.querySelector("#profile-error");
const profileForm = document.querySelector("#profile-form");
const skillDialog = document.querySelector("#skill-dialog");
const skillForm = document.querySelector("#skill-form");
const deleteDialog = document.querySelector("#delete-profile-dialog");

boot();

async function boot() {
    try {
        await refreshSession();
        const response = await authenticatedFetch("/api/v1/profile");
        if (response.ok) {
            const profile = await response.json();
            profileExists = true;
            populateProfile(profile);
            skills = profile.skills || [];
        } else if (response.status === 404) {
            skills = await loadSkills();
        } else {
            throw await apiError(response);
        }
        renderCompanies();
        renderSkills();
        updateCompletion();
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

async function loadSkills() {
    const response = await authenticatedFetch("/api/v1/profile/skills");
    if (!response.ok) throw await apiError(response);
    return response.json();
}

function populateProfile(profile) {
    fields.forEach(name => {
        const input = document.querySelector(`#${name}`);
        input.value = profile[name] ?? "";
    });
    companies = [...(profile.targetCompanies || [])];
    updateBioCount();
}

profileForm?.addEventListener("submit", async event => {
    event.preventDefault();
    clearProfileErrors();
    if (!profileForm.checkValidity()) {
        profileForm.reportValidity();
        return;
    }
    const saveButton = document.querySelector("#save-profile");
    const status = document.querySelector("#save-status");
    saveButton.disabled = true;
    status.textContent = "Saving…";
    try {
        const response = await authenticatedFetch("/api/v1/profile", {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(profilePayload())
        });
        if (!response.ok) throw await apiError(response);
        const saved = await response.json();
        profileExists = true;
        companies = [...(saved.targetCompanies || [])];
        renderCompanies();
        updateCompletion();
        status.textContent = "Saved just now";
        showToast("Profile saved successfully.");
    } catch (error) {
        applyProfileErrors(error.fieldErrors || {});
        status.textContent = "Save failed";
        showToast(error.message || "Profile could not be saved.", true);
    } finally {
        saveButton.disabled = false;
    }
});

function profilePayload() {
    const payload = {};
    fields.forEach(name => {
        const value = document.querySelector(`#${name}`).value.trim();
        payload[name] = value === "" ? null : value;
    });
    payload.graduationYear = numberOrNull(payload.graduationYear);
    payload.yearsOfExperience = numberOrNull(payload.yearsOfExperience);
    payload.targetCompanies = companies;
    return payload;
}

function numberOrNull(value) {
    return value == null ? null : Number(value);
}

document.querySelector("#add-company")?.addEventListener("click", addCompany);
document.querySelector("#company-input")?.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        addCompany();
    }
});

function addCompany() {
    const input = document.querySelector("#company-input");
    const name = input.value.trim();
    if (!name) return;
    if (companies.length >= 20) {
        showToast("You can add up to 20 target companies.", true);
        return;
    }
    if (companies.some(company => company.toLocaleLowerCase() === name.toLocaleLowerCase())) {
        showToast("That company is already in your list.", true);
        return;
    }
    companies.push(name);
    input.value = "";
    renderCompanies();
    updateCompletion();
}

function renderCompanies() {
    const target = document.querySelector("#company-tags");
    target.replaceChildren();
    if (!companies.length) {
        const empty = document.createElement("span");
        empty.className = "empty-tag";
        empty.textContent = "No target companies added";
        target.append(empty);
    } else {
        companies.forEach((company, index) => {
            const tag = document.createElement("span");
            tag.className = "company-tag";
            tag.append(document.createTextNode(company));
            const remove = document.createElement("button");
            remove.type = "button";
            remove.setAttribute("aria-label", `Remove ${company}`);
            remove.textContent = "×";
            remove.addEventListener("click", () => {
                companies.splice(index, 1);
                renderCompanies();
                updateCompletion();
            });
            tag.append(remove);
            target.append(tag);
        });
    }
    document.querySelector("#company-count").textContent = `${companies.length} / 20`;
}

document.querySelector("#open-skill-dialog")?.addEventListener("click", () => openSkillDialog());
document.querySelector("#close-skill-dialog")?.addEventListener("click", () => skillDialog.close());
document.querySelector("#cancel-skill")?.addEventListener("click", () => skillDialog.close());

function openSkillDialog(skill = null) {
    clearSkillErrors();
    skillForm.reset();
    document.querySelector("#skill-years").value = "0";
    document.querySelector("#skill-id").value = skill?.id ?? "";
    document.querySelector("#skill-name").value = skill?.name ?? "";
    document.querySelector("#skill-category").value = skill?.category ?? "";
    document.querySelector("#skill-proficiency").value = skill?.proficiency ?? "BEGINNER";
    document.querySelector("#skill-years").value = skill?.yearsUsed ?? "0";
    document.querySelector("#skill-name").disabled = Boolean(skill);
    document.querySelector("#skill-category").disabled = Boolean(skill);
    document.querySelector("#skill-dialog-title").textContent = skill ? `Update ${skill.name}` : "Add a skill";
    skillDialog.showModal();
}

skillForm?.addEventListener("submit", async event => {
    event.preventDefault();
    clearSkillErrors();
    if (!skillForm.checkValidity()) {
        skillForm.reportValidity();
        return;
    }
    const skillId = document.querySelector("#skill-id").value;
    const editing = skillId !== "";
    const payload = {
        proficiency: document.querySelector("#skill-proficiency").value,
        yearsUsed: Number(document.querySelector("#skill-years").value)
    };
    if (!editing) {
        payload.name = document.querySelector("#skill-name").value.trim();
        payload.category = document.querySelector("#skill-category").value.trim();
    }
    const saveButton = document.querySelector("#save-skill");
    saveButton.disabled = true;
    try {
        const response = await authenticatedFetch(
            editing ? `/api/v1/profile/skills/${skillId}` : "/api/v1/profile/skills",
            {method: editing ? "PUT" : "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(payload)}
        );
        if (!response.ok) throw await apiError(response);
        const saved = await response.json();
        if (editing) {
            const index = skills.findIndex(skill => skill.id === saved.id);
            if (index >= 0) skills[index] = saved;
        } else {
            skills.push(saved);
        }
        skills.sort((left, right) => left.name.localeCompare(right.name));
        renderSkills();
        updateCompletion();
        skillDialog.close();
        showToast(editing ? "Skill updated." : "Skill added.");
    } catch (error) {
        Object.entries(error.fieldErrors || {}).forEach(([field, message]) => {
            const target = document.querySelector(`[data-skill-error='${field}']`);
            if (target) target.textContent = message;
        });
        showToast(error.message || "Skill could not be saved.", true);
    } finally {
        saveButton.disabled = false;
    }
});

function renderSkills() {
    const target = document.querySelector("#skills-list");
    target.replaceChildren();
    if (!skills.length) {
        const empty = document.createElement("div");
        empty.className = "skills-empty";
        const title = document.createElement("strong");
        title.textContent = "No skills added yet";
        const copy = document.createElement("span");
        copy.textContent = "Add your first language, framework, tool, or domain skill.";
        empty.append(title, copy);
        target.append(empty);
        return;
    }
    skills.forEach(skill => {
        const row = document.createElement("article");
        row.className = "skill-row";
        const badge = document.createElement("span");
        badge.className = "skill-badge";
        badge.textContent = skill.name.charAt(0).toUpperCase();
        const identity = document.createElement("div");
        identity.className = "skill-identity";
        const name = document.createElement("strong");
        name.textContent = skill.name;
        const category = document.createElement("small");
        category.textContent = skill.category;
        identity.append(name, category);
        const level = document.createElement("span");
        level.className = `proficiency proficiency-${skill.proficiency.toLowerCase()}`;
        level.textContent = titleCase(skill.proficiency);
        const years = document.createElement("span");
        years.className = "skill-years";
        years.textContent = `${skill.yearsUsed} ${Number(skill.yearsUsed) === 1 ? "year" : "years"}`;
        const actions = document.createElement("div");
        actions.className = "skill-actions";
        const edit = document.createElement("button");
        edit.type = "button";
        edit.textContent = "Edit";
        edit.addEventListener("click", () => openSkillDialog(skill));
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = "remove-skill";
        remove.textContent = "Remove";
        remove.addEventListener("click", () => removeSkill(skill));
        actions.append(edit, remove);
        row.append(badge, identity, level, years, actions);
        target.append(row);
    });
}

async function removeSkill(skill) {
    if (!window.confirm(`Remove ${skill.name} from your profile?`)) return;
    const response = await authenticatedFetch(`/api/v1/profile/skills/${skill.id}`, {method: "DELETE"});
    if (!response.ok) {
        const error = await apiError(response);
        showToast(error.message, true);
        return;
    }
    skills = skills.filter(item => item.id !== skill.id);
    renderSkills();
    updateCompletion();
    showToast("Skill removed.");
}

document.querySelector("#open-delete-dialog")?.addEventListener("click", () => {
    if (!profileExists) {
        showToast(skills.length ? "Save a profile first, or remove skills individually." : "There is no profile data to delete.", true);
        return;
    }
    deleteDialog.showModal();
});
document.querySelector("#cancel-delete")?.addEventListener("click", () => deleteDialog.close());
document.querySelector("#confirm-delete")?.addEventListener("click", async () => {
    const button = document.querySelector("#confirm-delete");
    button.disabled = true;
    try {
        const response = await authenticatedFetch("/api/v1/profile", {method: "DELETE"});
        if (!response.ok) throw await apiError(response);
        profileExists = false;
        companies = [];
        skills = [];
        profileForm.reset();
        updateBioCount();
        renderCompanies();
        renderSkills();
        updateCompletion();
        deleteDialog.close();
        showToast("Profile data deleted.");
    } catch (error) {
        showToast(error.message || "Profile could not be deleted.", true);
    } finally {
        button.disabled = false;
    }
});

function updateCompletion() {
    const values = fields.map(name => document.querySelector(`#${name}`).value.trim());
    const completed = values.filter(Boolean).length + (companies.length ? 1 : 0) + (skills.length ? 1 : 0);
    const percentage = Math.round((completed / (fields.length + 2)) * 100);
    document.querySelector("#completion-value").textContent = `${percentage}%`;
    const ring = document.querySelector(".completion-ring");
    [...ring.classList]
        .filter(name => /^completion-\d+$/.test(name))
        .forEach(name => ring.classList.remove(name));
    ring.classList.add(`completion-${Math.round(percentage / 10) * 10}`);
    document.querySelector("#completion-message").textContent = percentage >= 80 ? "Strong practice context" : percentage >= 45 ? "Good progress—keep going" : "Add more detail for better practice";
}

fields.forEach(name => document.querySelector(`#${name}`)?.addEventListener("input", updateCompletion));
document.querySelector("#bio")?.addEventListener("input", updateBioCount);
function updateBioCount() { document.querySelector("#bio-count").textContent = `${document.querySelector("#bio").value.length} / 5000`; }

function clearProfileErrors() {
    document.querySelectorAll("[data-error-for]").forEach(node => node.textContent = "");
}
function applyProfileErrors(errors) {
    Object.entries(errors).forEach(([field, message]) => {
        const normalized = field.startsWith("targetCompanies") ? "targetCompanies" : field;
        const target = document.querySelector(`[data-error-for='${normalized}']`);
        if (target) target.textContent = message;
    });
}
function clearSkillErrors() { document.querySelectorAll("[data-skill-error]").forEach(node => node.textContent = ""); }

let toastTimer;
function showToast(message, error = false) {
    const toast = document.querySelector("#profile-toast");
    toast.textContent = message;
    toast.classList.toggle("toast-error", error);
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.hidden = true, 3500);
}

function titleCase(value) { return value.charAt(0) + value.slice(1).toLowerCase(); }
async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* response had no JSON body */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}

document.querySelector("#logout-button")?.addEventListener("click", async () => {
    await logout();
    window.location.replace("/login");
});
document.querySelector("#menu-button")?.addEventListener("click", () => document.querySelector(".sidebar")?.classList.toggle("open"));
