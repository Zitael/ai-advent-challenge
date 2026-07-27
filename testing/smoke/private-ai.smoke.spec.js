const { test, expect } = require('@playwright/test');
const baseURL = process.env.SMOKE_BASE_URL || 'http://localhost:8080';
const apiKey = process.env.PRIVATE_AI_API_KEY || 'change-me';

test.beforeEach(async ({ page }, testInfo) => {
  await page.goto(baseURL);
  await page.screenshot({ path: `testing/smoke/screenshots/${testInfo.title.replace(/[^a-z0-9]+/gi, '-')}-01-open.png`, fullPage: true });
});

test('UI opens and health status is resolved', async ({ page }) => {
  await expect(page.locator('h1')).toHaveText('Private AI');
  await expect(page.locator('#status')).not.toHaveText('Проверка...');
  await page.screenshot({ path: 'testing/smoke/screenshots/health-02-result.png', fullPage: true });
});

test('settings are saved in browser storage', async ({ page }) => {
  await page.locator('#apiKey').fill(apiKey);
  await page.locator('#sessionId').fill('smoke-settings');
  await page.locator('#saveSettingsButton').click();
  await page.reload();
  await expect(page.locator('#sessionId')).toHaveValue('smoke-settings');
  await page.screenshot({ path: 'testing/smoke/screenshots/settings-02-reloaded.png', fullPage: true });
});

test('empty API key is rejected by UI', async ({ page }) => {
  await page.locator('#messageInput').fill('hello');
  await page.locator('#sendButton').click();
  await expect(page.locator('#messages')).toContainText('Сначала укажи API key.');
  await page.screenshot({ path: 'testing/smoke/screenshots/missing-key-02-error.png', fullPage: true });
});

test('invalid API key produces visible error', async ({ page }) => {
  await page.locator('#apiKey').fill('wrong-key');
  await page.locator('#sessionId').fill('smoke-invalid-key');
  await page.locator('#messageInput').fill('hello');
  await page.locator('#sendButton').click();
  await expect(page.locator('#messages')).toContainText('Ошибка:');
  await page.screenshot({ path: 'testing/smoke/screenshots/invalid-key-02-error.png', fullPage: true });
});

test('session clear validates required settings', async ({ page }) => {
  await page.locator('#apiKey').fill('');
  await page.locator('#sessionId').fill('');
  await page.locator('#clearButton').click();
  await expect(page.locator('#messages')).toContainText('Для очистки нужны API key и Session ID.');
  await page.screenshot({ path: 'testing/smoke/screenshots/clear-02-validation.png', fullPage: true });
});
