import { expect, test } from '@playwright/test'

/**
 * End-to-end of the commercial flow's UI path with a mocked backend (the same
 * Playwright route-mocking approach as home.spec.ts): authenticate, enter an
 * organization, land on the operational Inicio, and reach the approval and
 * document surfaces that close the commercial cycle — all gated by permission.
 *
 * The data-level rules (submit only viable, approve locks the version, audit,
 * PDF without internal costs) are pinned by backend tests; this spec covers the
 * authenticated navigation through the cycle's screens.
 */

const ACCESS = {
  role: 'APROBADOR',
  permissions: ['PROYECTOS_READ', 'PRESUPUESTOS_READ', 'APROBACIONES_READ', 'DOCUMENTOS_READ'],
}

async function mockBackend(page: import('@playwright/test').Page) {
  // Only the endpoints the flow depends on are mocked. Other list endpoints are
  // left unmocked: with no backend they fail fast and the surfaces render their
  // empty/loading states (same approach as home.spec.ts).
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 200,
      headers: { 'content-type': 'application/json', 'set-cookie': 'forze_refresh=test; HttpOnly; Path=/api/auth; SameSite=Lax' },
      body: JSON.stringify({ accessToken: 'access-1', tokenType: 'Bearer' }),
    }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({ status: 200, headers: { 'content-type': 'application/json' }, body: JSON.stringify({ accessToken: 'access-2', tokenType: 'Bearer' }) }),
  )
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 200, headers: { 'content-type': 'application/json' }, body: JSON.stringify({ id: '00000000-0000-0000-0000-000000000001', username: 'aprobador', email: null }) }),
  )
  await page.route('**/api/auth/logout', (route) => route.fulfill({ status: 204 }))
  await page.route('**/api/organizations', (route) =>
    route.fulfill({ status: 200, headers: { 'content-type': 'application/json' }, body: JSON.stringify([{ id: '11111111-1111-1111-1111-111111111111', name: 'Constructora Demo' }]) }),
  )
  await page.route('**/api/me/access', (route) =>
    route.fulfill({ status: 200, headers: { 'content-type': 'application/json' }, body: JSON.stringify(ACCESS) }),
  )
}

test('commercial flow: login, enter org, reach approval and document surfaces', async ({ page }) => {
  await mockBackend(page)

  // Authenticate.
  await page.goto('/login')
  await page.getByLabel('Usuario').fill('aprobador')
  await page.getByLabel('Contrasena').fill('change-me')
  await page.getByRole('button', { name: /entrar/i }).click()

  // Enter the organization (no active org yet after login).
  await page.getByRole('button', { name: /Ingresar a Organización/i }).click()

  // Operational Inicio dashboard.
  await expect(page.getByRole('button', { name: 'Inicio' })).toBeVisible()
  await expect(page.getByText(/Resumen operativo/i)).toBeVisible()

  // Reach the approval surface (close-the-cycle screen), permission-gated.
  await page.getByRole('button', { name: /Flujo Aprobación/i }).click()
  await expect(page.getByRole('heading', { name: /Flujo de Aprobaciones/i })).toBeVisible()

  // Reach the document surface (client PDFs).
  await page.getByRole('button', { name: /Documentos PDF/i }).click()
  await expect(page.getByRole('heading', { name: /Reportes y Documentos PDF/i })).toBeVisible()

  // Close the session.
  await page.getByRole('button', { name: /salir/i }).click()
  await expect(page).toHaveURL(/\/login$/)
})
