import {authenticate} from "./auth-client.js";

const form = document.querySelector("#auth-form");
const alertBox = document.querySelector("#form-alert");

form?.addEventListener("submit", async event => {
    event.preventDefault();
    clearErrors();
    const mode = form.dataset.mode;
    const values = Object.fromEntries(new FormData(form).entries());
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    if (mode === "register" && values.password !== values.confirmPassword) {
        showFieldError("confirmPassword", "Passwords do not match");
        return;
    }
    delete values.confirmPassword;
    const button = form.querySelector("button[type='submit']");
    button.disabled = true;
    try {
        await authenticate(mode, values);
        window.location.replace("/dashboard");
    } catch (error) {
        Object.entries(error.fieldErrors || {}).forEach(([field, message]) => showFieldError(field, message));
        alertBox.textContent = error.message || "Authentication failed. Please try again.";
        alertBox.hidden = false;
    } finally {
        button.disabled = false;
    }
});

function clearErrors() {
    alertBox.hidden = true;
    document.querySelectorAll(".field-error").forEach(node => node.textContent = "");
    document.querySelectorAll("input.invalid").forEach(node => node.classList.remove("invalid"));
}

function showFieldError(field, message) {
    const input = document.querySelector(`#${field}`);
    const target = document.querySelector(`[data-error-for='${field}']`);
    input?.classList.add("invalid");
    if (target) target.textContent = message;
}
