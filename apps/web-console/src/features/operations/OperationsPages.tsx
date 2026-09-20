import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import {
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import { AsyncState } from '@/components/AsyncState';
import { MetricCard } from '@/components/MetricCard';
import { PageHeader } from '@/components/PageHeader';
import { RouterLinkBehavior } from '@/components/RouterLinks';
import { StatusBadge } from '@/components/StatusBadge';
import { getApi, toErrorMessage } from '@/lib/api';
import { formatDateTime, formatDuration } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import {
  cardProcessingSchema,
  clearingSchema,
  isoMessagesSchema,
  kafkaEventsSchema,
  ledgerSchema,
  overviewSchema,
  settlementSchema,
  simulationSchema,
  traceSchema,
} from '@/lib/schemas';

export function OperationsOverviewPage() {
  const overview = useQuery({
    queryKey: ['operations', 'overview'],
    queryFn: () => getApi('/api/v1/ops/overview', overviewSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Operations overview"
        description="A simulator-wide view of transaction flow, service health, and settlement liquidity."
      />
      <AsyncState
        loading={overview.isPending}
        label="Loading operations overview"
        error={overview.isError ? toErrorMessage(overview.error) : null}
        onRetry={() => void overview.refetch()}
      >
        {overview.data === undefined ? null : (
          <Stack spacing={2}>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 2 }}>
              <MetricCard
                label="Transactions per second"
                value={overview.data.metrics.transactionsPerSecond.value.toFixed(1)}
                deltaPercent={overview.data.metrics.transactionsPerSecond.deltaPercent}
              />
              <MetricCard
                label="Successful payments"
                value={overview.data.metrics.successfulPayments.value.toLocaleString('en-KZ')}
                deltaPercent={overview.data.metrics.successfulPayments.deltaPercent}
              />
              <MetricCard
                label="Interbank transfers"
                value={overview.data.metrics.interbankTransfers.value.toLocaleString('en-KZ')}
                deltaPercent={overview.data.metrics.interbankTransfers.deltaPercent}
              />
              <MetricCard
                label="Card authorizations"
                value={overview.data.metrics.cardAuthorizations.value.toLocaleString('en-KZ')}
                deltaPercent={overview.data.metrics.cardAuthorizations.deltaPercent}
              />
            </Box>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h2">Service health</Typography>
                <Stack spacing={1.25} sx={{ mt: 2 }}>
                  {overview.data.serviceHealth.map((service) => (
                    <Stack key={service.id} sx={{ flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', gap: 1 }}>
                      <Box>
                        <Typography sx={{ fontWeight: 700 }}>{service.name}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {service.bank ?? 'National platform'} · p95 {service.p95LatencyMs.toFixed(0)} ms · errors{' '}
                          {service.errorRatePercent.toFixed(2)}%
                        </Typography>
                      </Box>
                      <StatusBadge status={service.status} />
                    </Stack>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          </Stack>
        )}
      </AsyncState>
    </>
  );
}

export function TransactionTracePage() {
  const trace = useQuery({
    queryKey: ['operations', 'trace'],
    queryFn: () => getApi('/api/v1/ops/transactions/search/trace', traceSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Transaction trace"
        description="Follow one payment across bank, national payment platform, ledger, outbox, and settlement stages."
      />
      <AsyncState
        loading={trace.isPending}
        label="Loading transaction trace"
        error={trace.isError ? toErrorMessage(trace.error) : null}
        onRetry={() => void trace.refetch()}
      >
        {trace.data === undefined ? null : (
          <Stack spacing={1.5}>
            {trace.data.stages.map((stage) => (
              <Card key={stage.id} variant="outlined">
                <CardContent>
                  <Stack sx={{ flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', gap: 1 }}>
                    <Box>
                      <Typography sx={{ fontWeight: 750 }}>{stage.name}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {stage.service} · {formatDateTime(stage.startedAt)} · {formatDuration(stage.durationMs ?? 0)}
                      </Typography>
                    </Box>
                    <StatusBadge status={stage.status} />
                  </Stack>
                  {stage.detail === null ? null : (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      {stage.detail}
                    </Typography>
                  )}
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
      </AsyncState>
    </>
  );
}

export function LedgerExplorerPage() {
  const journal = useQuery({
    queryKey: ['operations', 'ledger'],
    queryFn: () => getApi('/api/v1/ops/ledger/latest', ledgerSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Ledger explorer"
        description="Inspect immutable balanced journals instead of mutable account-balance shortcuts."
      />
      <AsyncState
        loading={journal.isPending}
        label="Loading ledger journal"
        error={journal.isError ? toErrorMessage(journal.error) : null}
        onRetry={() => void journal.refetch()}
      >
        {journal.data === undefined ? null : (
          <Card variant="outlined">
            <CardContent>
              <Stack sx={{ flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', gap: 1 }}>
                <Box>
                  <Typography variant="h2">{journal.data.journalId}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Posted {formatDateTime(journal.data.postedAt)} · transaction {journal.data.transactionId}
                  </Typography>
                </Box>
                <StatusBadge status={journal.data.status} />
              </Stack>
              <Divider sx={{ my: 2 }} />
              <TableContainer>
                <Table aria-label="Ledger entries">
                  <TableHead>
                    <TableRow>
                      <TableCell>Side</TableCell>
                      <TableCell>Account</TableCell>
                      <TableCell align="right">Amount</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {journal.data.entries.map((entry) => (
                      <TableRow key={entry.id}>
                        <TableCell>{entry.side}</TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 700 }}>
                            {entry.accountName}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {entry.accountCode}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">{formatMoney(entry.amount, journal.data.currency)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        )}
      </AsyncState>
    </>
  );
}

export function SettlementPage() {
  const settlement = useQuery({
    queryKey: ['operations', 'settlement'],
    queryFn: () => getApi('/api/v1/ops/settlement', settlementSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Settlement"
        description="Monitor simulated participant liquidity and queued RTGS payments."
      />
      <AsyncState
        loading={settlement.isPending}
        label="Loading settlement"
        error={settlement.isError ? toErrorMessage(settlement.error) : null}
        onRetry={() => void settlement.refetch()}
      >
        {settlement.data === undefined ? null : (
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: 2 }}>
            {settlement.data.positions.map((position) => (
              <MetricCard
                key={position.participant}
                label={position.participant}
                value={formatMoney(position.availableLiquidity, position.currency)}
                helper={`Queued ${formatMoney(position.queuedAmount, position.currency)}`}
              />
            ))}
          </Box>
        )}
      </AsyncState>
    </>
  );
}

export function ClearingPage() {
  const clearing = useQuery({
    queryKey: ['operations', 'clearing'],
    queryFn: () => getApi('/api/v1/ops/clearing', clearingSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Clearing"
        description="Track net obligations and clearing-cycle state for lower-value interbank flows."
      />
      <AsyncState
        loading={clearing.isPending}
        label="Loading clearing"
        error={clearing.isError ? toErrorMessage(clearing.error) : null}
        onRetry={() => void clearing.refetch()}
      >
        {clearing.data === undefined ? null : (
          <Stack spacing={2}>
            <Card variant="outlined">
              <CardContent>
                <Typography variant="h2">{clearing.data.currentCycle?.id ?? 'No open cycle'}</Typography>
                {clearing.data.currentCycle === null ? null : <StatusBadge status={clearing.data.currentCycle.status} />}
              </CardContent>
            </Card>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: 2 }}>
              {clearing.data.netPositions.map((position) => (
                <MetricCard
                  key={position.participant}
                  label={position.participant}
                  value={formatMoney(position.netAmount, position.currency)}
                />
              ))}
            </Box>
          </Stack>
        )}
      </AsyncState>
    </>
  );
}

export function IsoMessagesPage() {
  const messages = useQuery({
    queryKey: ['operations', 'iso20022'],
    queryFn: () => getApi('/api/v1/ops/iso20022/messages', isoMessagesSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="ISO 20022 messages"
        description="Inspect simulator pacs.008, pacs.002, and pacs.004 style payment messages."
      />
      <AsyncState
        loading={messages.isPending}
        label="Loading ISO messages"
        error={messages.isError ? toErrorMessage(messages.error) : null}
        onRetry={() => void messages.refetch()}
        empty={messages.data?.length === 0}
      >
        <Stack spacing={1.5}>
          {messages.data?.map((message) => (
            <Card key={message.id} variant="outlined">
              <CardContent>
                <Stack sx={{ flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', gap: 1 }}>
                  <Box>
                    <Typography sx={{ fontWeight: 750 }}>{message.messageType}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {message.sender} to {message.receiver} · {message.messageId}
                    </Typography>
                  </Box>
                  <StatusBadge status={message.paymentStatus} />
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      </AsyncState>
    </>
  );
}

export function KafkaEventsPage() {
  const events = useQuery({
    queryKey: ['operations', 'kafka-events'],
    queryFn: () => getApi('/api/v1/ops/kafka/events', kafkaEventsSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Kafka events"
        description="Review transactional outbox events that downstream simulators consume idempotently."
      />
      <AsyncState
        loading={events.isPending}
        label="Loading Kafka events"
        error={events.isError ? toErrorMessage(events.error) : null}
        onRetry={() => void events.refetch()}
        empty={events.data?.length === 0}
      >
        <Stack spacing={1.5}>
          {events.data?.map((event) => (
            <Card key={event.eventId} variant="outlined">
              <CardContent>
                <Typography sx={{ fontWeight: 750 }}>{event.eventType}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {event.topic} · {event.aggregateType}/{event.aggregateId} · {formatDateTime(event.occurredAt)}
                </Typography>
              </CardContent>
            </Card>
          ))}
        </Stack>
      </AsyncState>
    </>
  );
}

export function CardProcessingPage() {
  const cards = useQuery({
    queryKey: ['operations', 'card-processing'],
    queryFn: () => getApi('/api/v1/ops/cards', cardProcessingSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Card processing"
        description="Monitor authorization, capture, reversal, ATM, and POS simulator activity."
      />
      <AsyncState
        loading={cards.isPending}
        label="Loading card processing"
        error={cards.isError ? toErrorMessage(cards.error) : null}
        onRetry={() => void cards.refetch()}
      >
        {cards.data === undefined ? null : (
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 2 }}>
            <MetricCard label="Authorizations" value={cards.data.totals.authorizationRequests.toLocaleString('en-KZ')} />
            <MetricCard label="Approved" value={cards.data.totals.approved.toLocaleString('en-KZ')} />
            <MetricCard label="Declined" value={cards.data.totals.declined.toLocaleString('en-KZ')} />
            <MetricCard label="Active holds" value={cards.data.totals.activeHolds.toLocaleString('en-KZ')} />
          </Box>
        )}
      </AsyncState>
    </>
  );
}

export function SimulationControlPage() {
  const simulation = useQuery({
    queryKey: ['operations', 'simulation'],
    queryFn: () => getApi('/api/v1/ops/simulation', simulationSchema),
  });

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Simulation"
        description="Control deterministic profiles, speed, virtual time, and bounded fault injection."
      />
      <AsyncState
        loading={simulation.isPending}
        label="Loading simulation"
        error={simulation.isError ? toErrorMessage(simulation.error) : null}
        onRetry={() => void simulation.refetch()}
      >
        {simulation.data === undefined ? null : (
          <Card variant="outlined">
            <CardContent>
              <Stack sx={{ flexDirection: { xs: 'column', sm: 'row' }, justifyContent: 'space-between', gap: 1 }}>
                <Box>
                  <Typography variant="h2">{simulation.data.profile} profile</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Virtual time {formatDateTime(simulation.data.virtualTime)} · speed {simulation.data.speed}x
                  </Typography>
                </Box>
                <StatusBadge status={simulation.data.status} />
              </Stack>
            </CardContent>
          </Card>
        )}
      </AsyncState>
    </>
  );
}

export function NotFoundPage() {
  return (
    <>
      <PageHeader
        eyebrow="Navigation"
        title="Page not found"
        description="This simulator console route is not available."
        actions={
          <Button component={RouterLinkBehavior} to="/ops/overview" startIcon={<ArrowBackRoundedIcon />}>
            Operations overview
          </Button>
        }
      />
    </>
  );
}
