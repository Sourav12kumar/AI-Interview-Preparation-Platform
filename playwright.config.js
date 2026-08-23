import {defineConfig, devices} from "@playwright/test";

export default defineConfig({
    testDir: "./tests/e2e",
    fullyParallel: false,
    forbidOnly: Boolean(process.env.CI),
    retries: process.env.CI ? 1 : 0,
    workers: 1,
    reporter: process.env.CI
        ? [["line"], ["html", {open: "never", outputFolder: "playwright-report"}]]
        : "list",
    use: {
        baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:8080",
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
        video: "retain-on-failure"
    },
    projects: [
        {name: "chromium", use: {...devices["Desktop Chrome"]}}
    ],
    outputDir: "test-results"
});
