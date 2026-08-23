import AxeBuilder from "@axe-core/playwright";
import {expect, test} from "@playwright/test";

const wcagTags = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];
const pageErrors = new WeakMap();

test.beforeEach(async ({page}) => {
    const errors = [];
    pageErrors.set(page, errors);
    page.on("pageerror", error => errors.push(error.message));
});

test.afterEach(async ({page}) => {
    expect(pageErrors.get(page), "The browser page emitted uncaught JavaScript errors").toEqual([]);
});

async function expectAccessible(page) {
    const scan = await new AxeBuilder({page}).withTags(wcagTags).analyze();
    expect(scan.violations, JSON.stringify(scan.violations, null, 2)).toEqual([]);
}

test.describe("release smoke tests", () => {
    test("public pages meet the automated WCAG baseline", async ({page}) => {
        for (const path of ["/", "/login", "/register"]) {
            await page.goto(path);
            await expect(page.locator("main")).toBeVisible();
            await expectAccessible(page);
        }
    });

    test("a protected workspace sends an anonymous browser to login", async ({page}) => {
        await page.goto("/dashboard");
        await expect(page).toHaveURL(/\/login$/);
        await expect(page.getByRole("heading", {name: "Log in to your workspace."})).toBeVisible();
    });

    test("candidate can register, save a profile, log out, and log in again", async ({page}) => {
        const runId = `${Date.now()}-${test.info().retry}`;
        const email = `release-${runId}@example.com`;
        const password = "ReleaseGate!2026";

        await page.goto("/register");
        await page.getByLabel("Full name").fill("Release Candidate");
        await page.getByLabel("Email address").fill(email);
        await page.getByLabel("Password", {exact: true}).fill(password);
        await page.getByLabel("Confirm password").fill(password);
        await page.getByRole("button", {name: "Create account"}).click();

        await expect(page).toHaveURL(/\/dashboard$/);
        await expect(page.getByRole("heading", {name: /Good to see you, Release/})).toBeVisible();
        await expect(page.locator("#dashboard-content")).toBeVisible();
        await expectAccessible(page);

        await page.getByRole("link", {name: /Candidate profile/}).click();
        await expect(page).toHaveURL(/\/profile$/);
        await expect(page.locator("#profile-content")).toBeVisible();
        await page.getByLabel("Professional headline").fill("Java platform engineer");
        await page.getByLabel("Target role").fill("Senior Backend Engineer");
        await page.getByRole("button", {name: "Save profile"}).click();
        await expect(page.locator("#save-status")).toContainText("Saved just now");
        await expectAccessible(page);

        await page.getByRole("button", {name: "Log out"}).click();
        await expect(page).toHaveURL(/\/login$/);

        await page.getByLabel("Email address").fill(email);
        await page.getByLabel("Password").fill(password);
        await page.getByRole("button", {name: "Log in"}).click();
        await expect(page).toHaveURL(/\/dashboard$/);
        await expect(page.locator("#dashboard-content")).toBeVisible();

        await page.goto("/profile");
        await expect(page.locator("#profile-content")).toBeVisible();
        await expect(page.getByLabel("Professional headline")).toHaveValue("Java platform engineer");
        await expect(page.getByLabel("Target role")).toHaveValue("Senior Backend Engineer");
    });
});
