import CreditCardRoundedIcon from '@mui/icons-material/CreditCardRounded';
import { Box, Card, CardContent, Divider, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import { AsyncState } from '@/components/AsyncState';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { getApi, toErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { cardsSchema } from '@/lib/schemas';

export function CardsPage() {
  const cards = useQuery({
    queryKey: ['customer', 'cards'],
    queryFn: () => getApi('/api/v1/me/cards', cardsSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Customer banking"
        title="Cards"
        description="Simulated cards are processed by bank-isolated card-processing services. No real PANs or networks are used."
      />
      <AsyncState
        loading={cards.isPending}
        label="Loading cards"
        error={cards.isError ? toErrorMessage(cards.error) : null}
        onRetry={() => void cards.refetch()}
        empty={cards.data?.length === 0}
        emptyTitle="No cards"
        emptyBody="Issued simulated cards will appear here."
      >
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 320px), 1fr))', gap: 2 }}>
          {cards.data?.map((card) => (
            <Card key={card.id}>
              <CardContent>
                <Stack sx={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <CreditCardRoundedIcon color="primary" sx={{ fontSize: 34 }} aria-hidden="true" />
                  <StatusBadge status={card.status} />
                </Stack>
                <Typography variant="h2" sx={{ mt: 2 }}>
                  {card.displayName}
                </Typography>
                <Typography sx={{ mt: 1, fontFamily: 'ui-monospace, monospace', letterSpacing: '0.06em' }}>
                  {card.maskedPan}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {card.scheme} · expires {formatDateTime(card.expiresAt)}
                </Typography>
                <Divider sx={{ my: 2 }} />
                <Typography variant="caption" color="text.secondary">
                  Active authorization holds
                </Typography>
                <Typography sx={{ fontWeight: 740 }}>
                  {formatMoney(card.activeHoldAmount, card.currency)}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Box>
      </AsyncState>
    </>
  );
}
