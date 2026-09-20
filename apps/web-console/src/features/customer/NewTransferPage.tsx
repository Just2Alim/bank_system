import SendRoundedIcon from '@mui/icons-material/SendRounded';
import {
  Alert,
  Button,
  Card,
  CardContent,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';

import { AsyncState } from '@/components/AsyncState';
import { PageHeader } from '@/components/PageHeader';
import { getApi, postCommand, toErrorMessage } from '@/lib/api';
import { accountsSchema, commandReceiptSchema } from '@/lib/schemas';

export function NewTransferPage() {
  const queryClient = useQueryClient();
  const [sourceAccountId, setSourceAccountId] = useState('');
  const [destinationAccountId, setDestinationAccountId] = useState('');
  const [amount, setAmount] = useState('');
  const [purpose, setPurpose] = useState('');
  const accounts = useQuery({
    queryKey: ['customer', 'accounts'],
    queryFn: () => getApi('/api/v1/me/accounts', accountsSchema),
  });
  const [commandKey] = useState(() => `console-${crypto.randomUUID()}`);
  const transfer = useMutation({
    mutationFn: () =>
      postCommand(
        '/api/v1/transfers',
        { sourceAccountId, destinationAccountId, amount, purpose },
        commandReceiptSchema,
        commandKey,
      ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['customer', 'accounts'] }),
        queryClient.invalidateQueries({ queryKey: ['customer', 'transactions'] }),
      ]);
    },
  });
  const canSubmit = sourceAccountId !== '' && destinationAccountId !== '' &&
    sourceAccountId !== destinationAccountId && /^\d+(?:\.\d{1,2})?$/.test(amount) &&
    Number(amount) > 0 && purpose.trim().length > 0;

  return (
    <>
      <PageHeader
        eyebrow="Open banking sandbox"
        title="New test transfer"
        description="In demo mode this posts an idempotent transfer and updates both balances. BCC mode is deliberately read-only until its subscribed payment approval contract is verified."
      />
      <AsyncState
        loading={accounts.isPending}
        label="Loading accounts"
        error={accounts.isError ? toErrorMessage(accounts.error) : null}
        onRetry={() => void accounts.refetch()}
      >
        <Card variant="outlined" sx={{ maxWidth: 720 }}>
          <CardContent>
            <Stack spacing={2} component="form" onSubmit={(event) => { event.preventDefault(); transfer.mutate(); }}>
              <TextField select label="From account" value={sourceAccountId} onChange={(event) => setSourceAccountId(event.target.value)}>
                {(accounts.data ?? []).map((account) => (
                  <MenuItem key={account.id} value={account.id}>{account.displayName} · {account.availableBalance} {account.currency}</MenuItem>
                ))}
              </TextField>
              <TextField select label="To account" value={destinationAccountId} onChange={(event) => setDestinationAccountId(event.target.value)}>
                {(accounts.data ?? []).filter((account) => account.id !== sourceAccountId).map((account) => (
                  <MenuItem key={account.id} value={account.id}>{account.displayName} · {account.accountNumberMasked}</MenuItem>
                ))}
              </TextField>
              <TextField label="Amount" value={amount} onChange={(event) => setAmount(event.target.value)} inputMode="decimal" />
              <TextField label="Purpose" value={purpose} onChange={(event) => setPurpose(event.target.value)} multiline minRows={2} />
              {transfer.isError ? <Alert severity="error">{toErrorMessage(transfer.error)}</Alert> : null}
              {transfer.data === undefined ? null : (
                <Alert severity="success">
                  Transfer {transfer.data.resourceId} — {transfer.data.status}
                </Alert>
              )}
              <Button type="submit" disabled={!canSubmit || transfer.isPending} variant="contained" startIcon={<SendRoundedIcon />}>
                {transfer.isPending ? 'Sending…' : 'Send transfer'}
              </Button>
              <Typography variant="caption" color="text.secondary">Idempotency key: {commandKey}</Typography>
            </Stack>
          </CardContent>
        </Card>
      </AsyncState>
    </>
  );
}
