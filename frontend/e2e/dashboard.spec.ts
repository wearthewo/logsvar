import { expect, test } from '@playwright/test'

test('operator can sign in and submit an event', async ({ page }) => {
  const browserErrors: string[] = []
  page.on('pageerror', (error) => browserErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') browserErrors.push(message.text())
  })

  await page.goto('/')
  await page.getByRole('button', { name: 'Sign in with Keycloak' }).click()
  await page.getByLabel('Username or email').fill('demo')
  await page.getByLabel('Password', { exact: true }).fill('demo')
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByRole('heading', { name: 'System overview' })).toBeVisible()
  await page.getByRole('link', { name: 'Event simulator' }).click()
  await page.getByRole('button', { name: 'Submit event' }).click()
  await expect(page.getByText('Event accepted')).toBeVisible()
  await expect(page.locator('.vite-error-overlay')).toHaveCount(0)
  expect(browserErrors).toEqual([])
})
