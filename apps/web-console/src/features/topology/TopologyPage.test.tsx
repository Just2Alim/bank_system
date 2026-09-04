import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { TopologyPage } from './TopologyPage';

const fixture = {
  data: {
    generatedAt: '2026-09-04T10:00:00Z',
    nodes: [
      {
        id: 'orda-ledger',
        label: 'Orda Ledger',
        group: 'ORDA',
        kind: 'LEDGER',
        health: 'UP',
        throughputPerSecond: 42.5,
        p95LatencyMs: 18,
        errorRatePercent: 0.1,
      },
      {
        id: 'npp-msmp',
        label: 'MSMP',
        group: 'NPP',
        kind: 'PAYMENT_RAIL',
        health: 'DEGRADED',
        throughputPerSecond: 25,
        p95LatencyMs: 80,
        errorRatePercent: 1.2,
      },
    ],
    edges: [
      {
        id: 'route-1',
        source: 'orda-ledger',
        target: 'npp-msmp',
        active: true,
        eventCount: 12,
      },
    ],
  },
};

describe('TopologyPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('renders live topology data with a semantic service list', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(fixture), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    );

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <TopologyPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByRole('heading', { name: /live banking topology/i })).toBeVisible();
    expect(await screen.findByRole('button', { name: /orda ledger/i })).toHaveTextContent('42.5 tx/s');
    expect(screen.getByRole('button', { name: /msmp/i })).toHaveTextContent('Degraded');
    expect(screen.getByText(/1 active route/i)).toBeVisible();
  });
});
