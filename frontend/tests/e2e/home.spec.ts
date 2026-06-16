import { expect, test } from '@playwright/test'

test('loads the initial FORZE route', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('link', { name: /FORZE/i })).toBeVisible()
  await expect(page.getByText('Base tecnica inicializada')).toBeVisible()
})

test('logs in, refreshes after reload, and logs out', async ({ page }) => {
  let meRequests = 0

  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        'content-type': 'application/json',
        'set-cookie': 'forze_refresh=test; HttpOnly; Path=/api/auth; SameSite=Lax',
      },
      body: JSON.stringify({ accessToken: 'access-1', tokenType: 'Bearer' }),
    })
  })

  await page.route('**/api/auth/refresh', async (route) => {
    await route.fulfill({
      status: 200,
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ accessToken: 'access-2', tokenType: 'Bearer' }),
    })
  })

  await page.route('**/api/auth/me', async (route) => {
    meRequests += 1
    const authorization = route.request().headers().authorization
    if (meRequests > 1 && !authorization) {
      await route.fulfill({
        status: 401,
        headers: { 'content-type': 'application/problem+json' },
        body: JSON.stringify({ status: 401, detail: 'Authentication is required.' }),
      })
      return
    }

    await route.fulfill({
      status: 200,
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ id: '00000000-0000-0000-0000-000000000001', username: 'admin', email: null }),
    })
  })

  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({ status: 204 })
  })

  await page.goto('/login')
  await page.getByLabel('Usuario').fill('admin')
  await page.getByLabel('Contrasena').fill('change-me')
  await page.getByRole('button', { name: /entrar/i }).click()

  await expect(page.getByText('Sesion autenticada')).toBeVisible()
  await page.reload()
  await expect(page.getByText('Refresh automatico activo')).toBeVisible()

  await page.getByRole('button', { name: /salir/i }).click()
  await expect(page).toHaveURL(/\/login$/)
})
