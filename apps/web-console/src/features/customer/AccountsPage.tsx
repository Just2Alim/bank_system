import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import { Alert, Box, Button, Card, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import { AsyncState } from '@/components/AsyncState';
import { PageHeader } from '@/components/PageHeader';
import { RouterLinkBehavior } from '@/components/RouterLinks';
import { StatusBadge } from '@/components/StatusBadge';
import { getApi, toErrorMessage } from '@/lib/api';
import { formatMoney } from '@/lib/money';
import { accountsSchema, integrationStatusSchema } from '@/lib/schemas';

export function AccountsPage() {
  const accounts = useQuery({
    queryKey: ['customer', 'accounts'],
    queryFn: () => getApi('/api/v1/me/accounts', accountsSchema),
  });
  const integration = useQuery({
    queryKey: ['integration', 'status'],
    queryFn: () => getApi('/api/v1/integration/status', integrationStatusSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Customer banking"
        title="Accounts"
        description="Book balances come from the bank ledger. Available balances also account for active authorization holds."
        actions={
          <Button component={RouterLinkBehavior} to="/customer/transfers/new" variant="contained" endIcon={<ArrowForwardRoundedIcon />}>
            Make a transfer
          </Button>
        }
      />
      {integration.data === undefined ? null : (
        <Alert
          severity={integration.data.provider === 'BCC_SANDBOX' ? 'info' : 'warning'}
          sx={{ mb: 2 }}
          action={<Chip size="small" label={`${integration.data.provider} · ${integration.data.writeMode}`} />}
        >
          {integration.data.detail}
        </Alert>
      )}
      <AsyncState
        loading={accounts.isPending}
        label="Loading accounts"
        error={accounts.isError ? toErrorMessage(accounts.error) : null}
        onRetry={() => void accounts.refetch()}
        empty={accounts.data?.length === 0}
        emptyTitle="No accounts"
        emptyBody="Your active simulated accounts will appear here."
      >
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 300px), 1fr))',
            gap: 2,
          }}
        >
          {accounts.data?.map((account) => (
            <Card key={account.id}>
              <CardContent>
                <Stack sx={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', gap: 1 }}>
                  <Box>
                    <Typography variant="h2">{account.displayName}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                      {account.accountNumberMasked} · {account.type}
                    </Typography>
                  </Box>
                  <StatusBadge status={account.status} />
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 3 }}>
                  Available balance
                </Typography>
                <Typography sx={{ fontSize: '1.8rem', fontWeight: 780, fontVariantNumeric: 'tabular-nums' }}>
                  {formatMoney(account.availableBalance, account.currency)}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.75 }}>
                  Book balance {formatMoney(account.bookBalance, account.currency)}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Box>
      </AsyncState>
    </>
  );
}
