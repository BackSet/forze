import { expect, test } from '@playwright/test'

test('loads the initial FORZE route', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('link', { name: /FORZE/i })).toBeVisible()
  await expect(page.getByText('Base tecnica inicializada')).toBeVisible()
})
