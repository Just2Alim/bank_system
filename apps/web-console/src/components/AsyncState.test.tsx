import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { AsyncState } from './AsyncState';

describe('AsyncState', () => {
  it('announces a loading state', () => {
    render(<AsyncState loading label="Loading accounts" />);

    expect(screen.getByRole('status')).toHaveAccessibleName('Loading accounts');
  });

  it('renders an actionable error state', async () => {
    const retry = vi.fn();
    render(<AsyncState error="The service is unavailable." onRetry={retry} />);

    expect(screen.getByRole('alert')).toHaveTextContent('The service is unavailable.');
    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    expect(retry).toHaveBeenCalledOnce();
  });

  it('renders a descriptive empty state', () => {
    render(<AsyncState empty emptyTitle="No transactions" emptyBody="New activity will appear here." />);

    expect(screen.getByText('No transactions')).toBeVisible();
    expect(screen.getByText('New activity will appear here.')).toBeVisible();
  });
});
