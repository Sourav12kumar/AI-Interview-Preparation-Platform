let accessToken = null;
let currentUser = null;

function rememberSession(payload) {
    accessToken = payload.accessToken;
    currentUser = payload.user;
    return payload;
}

export function getCurrentUser() {
    return currentUser;
}

export async function authenticate(mode, body) {
    const response = await fetch(`/api/v1/auth/browser/${mode}`, {
        method: "POST",
        credentials: "same-origin",
        headers: {"Content-Type": "application/json", "Accept": "application/json"},
        body: JSON.stringify(body)
    });
    if (!response.ok) throw await apiError(response);
    return rememberSession(await response.json());
}

export async function refreshSession() {
    const response = await fetch("/api/v1/auth/browser/refresh", {
        method: "POST",
        credentials: "same-origin",
        headers: {"Accept": "application/json"}
    });
    if (!response.ok) throw await apiError(response);
    return rememberSession(await response.json());
}

export async function authenticatedFetch(url, options = {}) {
    if (!accessToken) await refreshSession();
    const headers = new Headers(options.headers || {});
    headers.set("Authorization", `Bearer ${accessToken}`);
    headers.set("Accept", "application/json");
    let response = await fetch(url, {...options, headers, credentials: "same-origin"});
    if (response.status === 401) {
        await refreshSession();
        headers.set("Authorization", `Bearer ${accessToken}`);
        response = await fetch(url, {...options, headers, credentials: "same-origin"});
    }
    return response;
}

export async function logout() {
    await fetch("/api/v1/auth/browser/logout", {method: "POST", credentials: "same-origin"});
    accessToken = null;
    currentUser = null;
}

async function apiError(response) {
    let body = {};
    try { body = await response.json(); } catch (ignored) { /* empty response */ }
    return {status: response.status, message: body.message || "The request could not be completed.", fieldErrors: body.fieldErrors || {}};
}
