import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import { Button, Card, CardContent, Divider, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';

import { AsyncState } from '@/components/AsyncState';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { getApi, toErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { transactionDetailSchema } from '@/lib/schemas';

export function TransactionDetailsPage() {
  const { transactionId = '' } = useParams();
  const transaction = useQuery({
    queryKey: ['customer', 'transaction', transactionId],
    queryFn: () => getApi(`/api/v1/transactions/${encodeURIComponent(transactionId)}`, transactionDetailSchema),
    enabled: transactionId.length > 0,
  });

  return (
    <>
      <PageHeader
        eyebrow="Customer banking"
        title="Transaction details"
        description="Authoritative bank-side status and references for this simulated transaction."
        actions={
          <Button component={Link} to="/customer/payments" startIcon={<ArrowBackRoundedIcon />}>
            Back to payments
          </Button>
        }
      />
      <AsyncState
        loading={transaction.isPending}
        label="Loading transaction details"
        error={transaction.isError ? toErrorMessage(transaction.error) : null}
        onRetry={() => void transaction.refetch()}
      >
        {transaction.data === undefined ? null : (
          <Card sx={{ maxWidth: 760 }}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center" gap={2}>
                <div>
                  <Typography variant="h2">{transaction.data.description}</Typography>
                  <Typography color="text.secondary">{transaction.data.counterparty}</Typography>
                </div>
                <StatusBadge status={transaction.data.status} size="medium" />
              </Stack>
              <Typography sx={{ my: 3, fontSize: '2rem', fontWeight: 780 }}>
                {formatMoney(transaction.data.amount, transaction.data.currency)}
              </Typography>
              <Divider />
              <Stack component="dl" spacing={1.25} sx={{ mt: 2, '& dt': { color: 'text.secondary' }, '& dd': { m: 0, fontWeight: 650 } }}>
                {[
                  ['Transaction ID', transaction.data.id],
                  ['Correlation ID', transaction.data.correlationId],
                  ['Reference', transaction.data.reference],
                  ['Rail', transaction.data.rail],
                  ['Created', formatDateTime(transaction.data.createdAt)],
                  ['Booking date', formatDateTime(transaction.data.bookingDate)],
                  ['Value date', formatDateTime(transaction.data.valueDate)],
                  ['Rejection reason', transaction.data.rejectionReason ?? 'Not applicable'],
                ].map(([label, value]) => (
                  <Stack key={label} direction={{ xs: 'column', sm: 'row' }} component="div" justifyContent="space-between" gap={0.5}>
                    <Typography component="dt" variant="body2">{label}</Typography>
                    <Typography component="dd" variant="body2" sx={{ overflowWrap: 'anywhere' }}>{value}</Typography>
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>
        )}
      </AsyncState>
    </>
  );
}
