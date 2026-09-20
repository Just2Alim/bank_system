import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, expect, test, vi } from 'vitest';

import { NewTransferPage } from './NewTransferPage';

afterEach(() => {
  vi.unstubAllGlobals();
});

test('submits an idempotent demo transfer and shows its receipt', async () => {
  const user = userEvent.setup();
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(new Response(JSON.stringify({ data: [
      { id: 'a', displayName: 'Main', accountNumberMasked: 'KZ12••••0001', type: 'CURRENT', currency: 'KZT', bookBalance: '1000.00', availableBalance: '1000.00', status: 'ACTIVE', provider: 'DEMO' },
      { id: 'b', displayName: 'Savings', accountNumberMasked: 'KZ34••••0002', type: 'SAVINGS', currency: 'KZT', bookBalance: '500.00', availableBalance: '500.00', status: 'ACTIVE', provider: 'DEMO' },
    ] }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    .mockResolvedValueOnce(new Response(JSON.stringify({ data: {
      commandId: 'console-key-12345678', resourceId: 'tx-1', status: 'COMPLETED', bookedAt: '2026-09-21T00:00:00Z',
    } }), { status: 201, headers: { 'Content-Type': 'application/json' } }))
    .mockResolvedValue(new Response(JSON.stringify({ data: [
      { id: 'a', displayName: 'Main', accountNumberMasked: 'KZ12••••0001', type: 'CURRENT', currency: 'KZT', bookBalance: '874.50', availableBalance: '874.50', status: 'ACTIVE', provider: 'DEMO' },
      { id: 'b', displayName: 'Savings', accountNumberMasked: 'KZ34••••0002', type: 'SAVINGS', currency: 'KZT', bookBalance: '625.50', availableBalance: '625.50', status: 'ACTIVE', provider: 'DEMO' },
    ] }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
  vi.stubGlobal('fetch', fetchMock);

  render(
    <MemoryRouter>
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <NewTransferPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );

  await user.click(await screen.findByLabelText('From account'));
  await user.click(await screen.findByRole('option', { name: /Main/ }));
  await user.click(screen.getByLabelText('To account'));
  await user.click(await screen.findByRole('option', { name: /Savings/ }));
  await user.type(screen.getByLabelText('Amount'), '125.50');
  await user.type(screen.getByLabelText('Purpose'), 'Console test');
  await user.click(screen.getByRole('button', { name: 'Send transfer' }));

  expect(await screen.findByText(/tx-1/)).toBeInTheDocument();
  expect(fetchMock).toHaveBeenCalledWith('/api/v1/transfers', expect.objectContaining({ method: 'POST' }));
});
