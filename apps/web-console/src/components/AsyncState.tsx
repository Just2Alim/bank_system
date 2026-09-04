import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import { Alert, Box, Button, Skeleton, Stack, Typography } from '@mui/material';
import type { PropsWithChildren, ReactNode } from 'react';

type AsyncStateProps = PropsWithChildren<{
  loading?: boolean;
  label?: string;
  error?: string | null;
  onRetry?: () => void;
  empty?: boolean;
  emptyTitle?: string;
  emptyBody?: string;
  emptyAction?: ReactNode;
}>;

export function AsyncState({
  loading = false,
  label = 'Loading data',
  error = null,
  onRetry,
  empty = false,
  emptyTitle = 'Nothing to show',
  emptyBody = 'Data will appear here when it becomes available.',
  emptyAction,
  children,
}: AsyncStateProps) {
  if (loading) {
    return (
      <Stack role="status" aria-label={label} spacing={1.25} sx={{ py: 2 }}>
        <Skeleton variant="rounded" height={68} />
        <Skeleton variant="rounded" height={68} />
        <Skeleton variant="rounded" height={68} width="72%" />
      </Stack>
    );
  }

  if (error !== null) {
    return (
      <Alert
        severity="error"
        role="alert"
        icon={<ErrorOutlineRoundedIcon />}
        action={
          onRetry === undefined ? undefined : (
            <Button color="inherit" size="small" startIcon={<RefreshRoundedIcon />} onClick={onRetry}>
              Try again
            </Button>
          )
        }
        sx={{ alignItems: 'center' }}
      >
        {error}
      </Alert>
    );
  }

  if (empty) {
    return (
      <Box sx={{ py: 6, px: 2, textAlign: 'center' }}>
        <InboxOutlinedIcon color="disabled" sx={{ fontSize: 40, mb: 1 }} aria-hidden="true" />
        <Typography variant="h3" gutterBottom>
          {emptyTitle}
        </Typography>
        <Typography color="text.secondary" sx={{ maxWidth: 480, mx: 'auto', mb: emptyAction ? 2 : 0 }}>
          {emptyBody}
        </Typography>
        {emptyAction}
      </Box>
    );
  }

  return children;
}
