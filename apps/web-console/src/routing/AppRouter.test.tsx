import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { appRoutes } from './AppRouter';

function renderRoute(path: string): void {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });

  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe('role-based routing', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('opens the customer workspace', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ data: [] }))));
    renderRoute('/customer/accounts');

    expect(await screen.findByRole('heading', { name: /accounts/i })).toBeVisible();
    expect(screen.getByRole('navigation', { name: /customer workspace/i })).toBeVisible();
  });

  it('opens the operations workspace', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ data: [] }))));
    renderRoute('/ops/ledger');

    expect(await screen.findByRole('heading', { name: /ledger explorer/i })).toBeVisible();
    expect(screen.getByRole('navigation', { name: /operations workspace/i })).toBeVisible();
  });

  it('shows a recoverable not-found page', () => {
    renderRoute('/not-a-real-route');

    expect(screen.getByRole('heading', { name: /page not found/i })).toBeVisible();
    expect(screen.getByRole('link', { name: /operations overview/i })).toHaveAttribute('href', '/ops/overview');
  });
});
