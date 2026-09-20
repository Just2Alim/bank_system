import { z } from 'zod';

export const moneyStringSchema = z.string().regex(/^-?(?:0|[1-9]\d*)(?:\.\d{1,2})?$/);
export const currencySchema = z.string().regex(/^[A-Z]{3}$/);
export const statusSchema = z.string().min(1).max(48);

export const accountSchema = z.object({
  id: z.string(),
  displayName: z.string(),
  accountNumberMasked: z.string(),
  type: z.string(),
  currency: currencySchema,
  bookBalance: moneyStringSchema,
  availableBalance: moneyStringSchema,
  status: statusSchema,
  provider: z.string().optional(),
});
export const accountsSchema = z.array(accountSchema);
export type Account = z.infer<typeof accountSchema>;

export const integrationStatusSchema = z.object({
  provider: z.enum(['DEMO', 'BCC_SANDBOX']),
  connection: statusSchema,
  writeMode: z.enum(['DEMO_ONLY', 'READ_ONLY']),
  detail: z.string(),
});
export type IntegrationStatus = z.infer<typeof integrationStatusSchema>;

export const cardSchema = z.object({
  id: z.string(),
  displayName: z.string(),
  maskedPan: z.string(),
  scheme: z.string(),
  accountId: z.string(),
  status: statusSchema,
  expiresAt: z.string(),
  activeHoldAmount: moneyStringSchema,
  currency: currencySchema,
});
export const cardsSchema = z.array(cardSchema);
export type Card = z.infer<typeof cardSchema>;

export const transactionSchema = z.object({
  id: z.string(),
  createdAt: z.string(),
  description: z.string(),
  counterparty: z.string(),
  amount: moneyStringSchema,
  currency: currencySchema,
  direction: z.enum(['DEBIT', 'CREDIT']),
  rail: z.string(),
  status: statusSchema,
  correlationId: z.string(),
});
export const transactionsSchema = z.array(transactionSchema);
export type Transaction = z.infer<typeof transactionSchema>;

export const transactionDetailSchema = transactionSchema.extend({
  accountId: z.string(),
  bookingDate: z.string(),
  valueDate: z.string(),
  reference: z.string(),
  rejectionReason: z.string().nullable(),
});
export type TransactionDetail = z.infer<typeof transactionDetailSchema>;

const metricSchema = z.object({
  value: z.number(),
  deltaPercent: z.number().nullable(),
});
export const overviewSchema = z.object({
  generatedAt: z.string(),
  metrics: z.object({
    transactionsPerSecond: metricSchema,
    successfulPayments: metricSchema,
    failedPayments: metricSchema,
    internalTransfers: metricSchema,
    interbankTransfers: metricSchema,
    cardAuthorizations: metricSchema,
  }),
  throughput: z.array(
    z.object({
      timestamp: z.string(),
      successful: z.number().nonnegative(),
      failed: z.number().nonnegative(),
    }),
  ),
  serviceHealth: z.array(
    z.object({
      id: z.string(),
      name: z.string(),
      bank: z.string().nullable(),
      status: statusSchema,
      p95LatencyMs: z.number().nonnegative(),
      errorRatePercent: z.number().nonnegative(),
    }),
  ),
  settlementPositions: z.array(
    z.object({
      participant: z.string(),
      currency: currencySchema,
      balance: moneyStringSchema,
    }),
  ),
});
export type Overview = z.infer<typeof overviewSchema>;

export const topologyNodeSchema = z.object({
  id: z.string(),
  label: z.string(),
  group: z.string(),
  kind: z.string(),
  health: statusSchema,
  throughputPerSecond: z.number().nonnegative(),
  p95LatencyMs: z.number().nonnegative(),
  errorRatePercent: z.number().nonnegative(),
});
export const topologyEdgeSchema = z.object({
  id: z.string(),
  source: z.string(),
  target: z.string(),
  active: z.boolean(),
  eventCount: z.number().int().nonnegative(),
});
export const topologySchema = z.object({
  generatedAt: z.string(),
  nodes: z.array(topologyNodeSchema),
  edges: z.array(topologyEdgeSchema),
});
export type Topology = z.infer<typeof topologySchema>;
export type TopologyNode = z.infer<typeof topologyNodeSchema>;

export const traceSchema = z.object({
  transactionId: z.string(),
  correlationId: z.string(),
  amount: moneyStringSchema,
  currency: currencySchema,
  rail: z.string(),
  status: statusSchema,
  durationMs: z.number().nonnegative(),
  stages: z.array(
    z.object({
      id: z.string(),
      name: z.string(),
      service: z.string(),
      status: statusSchema,
      startedAt: z.string(),
      completedAt: z.string().nullable(),
      durationMs: z.number().nonnegative().nullable(),
      detail: z.string().nullable(),
    }),
  ),
});
export type TransactionTrace = z.infer<typeof traceSchema>;

export const ledgerSchema = z.object({
  journalId: z.string(),
  transactionId: z.string(),
  postedAt: z.string(),
  currency: currencySchema,
  status: statusSchema,
  debitsTotal: moneyStringSchema,
  creditsTotal: moneyStringSchema,
  balanced: z.boolean(),
  reversalOf: z.string().nullable(),
  entries: z.array(
    z.object({
      id: z.string(),
      side: z.enum(['DEBIT', 'CREDIT']),
      accountCode: z.string(),
      accountName: z.string(),
      amount: moneyStringSchema,
    }),
  ),
});
export type LedgerJournal = z.infer<typeof ledgerSchema>;

export const settlementSchema = z.object({
  generatedAt: z.string(),
  positions: z.array(
    z.object({
      participant: z.string(),
      currency: currencySchema,
      openingBalance: moneyStringSchema,
      settledDebit: moneyStringSchema,
      settledCredit: moneyStringSchema,
      availableLiquidity: moneyStringSchema,
      queuedAmount: moneyStringSchema,
    }),
  ),
  rtgsQueue: z.array(
    z.object({
      paymentId: z.string(),
      sender: z.string(),
      receiver: z.string(),
      amount: moneyStringSchema,
      currency: currencySchema,
      priority: z.number().int(),
      queuedAt: z.string(),
      status: statusSchema,
    }),
  ),
});
export type Settlement = z.infer<typeof settlementSchema>;

export const clearingSchema = z.object({
  generatedAt: z.string(),
  currentCycle: z
    .object({
      id: z.string(),
      status: statusSchema,
      closesAt: z.string(),
      paymentCount: z.number().int().nonnegative(),
      grossAmount: moneyStringSchema,
      currency: currencySchema,
    })
    .nullable(),
  obligations: z.array(
    z.object({
      debtor: z.string(),
      creditor: z.string(),
      grossAmount: moneyStringSchema,
      currency: currencySchema,
    }),
  ),
  netPositions: z.array(
    z.object({
      participant: z.string(),
      netAmount: moneyStringSchema,
      currency: currencySchema,
    }),
  ),
  recentCycles: z.array(
    z.object({
      id: z.string(),
      settledAt: z.string().nullable(),
      status: statusSchema,
      paymentCount: z.number().int().nonnegative(),
      grossAmount: moneyStringSchema,
      currency: currencySchema,
    }),
  ),
});
export type Clearing = z.infer<typeof clearingSchema>;

export const isoMessagesSchema = z.array(
  z.object({
    id: z.string(),
    messageType: z.string(),
    messageId: z.string(),
    sender: z.string(),
    receiver: z.string(),
    createdAt: z.string(),
    paymentStatus: statusSchema,
    originalMessageReference: z.string().nullable(),
    xml: z.string(),
  }),
);
export type IsoMessage = z.infer<typeof isoMessagesSchema>[number];

export const kafkaEventsSchema = z.array(
  z.object({
    eventId: z.string(),
    topic: z.string(),
    partition: z.number().int().nullable(),
    offset: z.string().nullable(),
    eventType: z.string(),
    aggregateType: z.string(),
    aggregateId: z.string(),
    occurredAt: z.string(),
    correlationId: z.string(),
    payload: z.record(z.string(), z.unknown()),
  }),
);
export type KafkaEvent = z.infer<typeof kafkaEventsSchema>[number];

export const cardProcessingSchema = z.object({
  generatedAt: z.string(),
  totals: z.object({
    authorizationRequests: z.number().int().nonnegative(),
    approved: z.number().int().nonnegative(),
    declined: z.number().int().nonnegative(),
    activeHolds: z.number().int().nonnegative(),
    captures: z.number().int().nonnegative(),
    reversals: z.number().int().nonnegative(),
    posTransactions: z.number().int().nonnegative(),
    atmTransactions: z.number().int().nonnegative(),
  }),
  recentAuthorizations: z.array(
    z.object({
      id: z.string(),
      bank: z.string(),
      maskedPan: z.string(),
      merchant: z.string(),
      channel: z.enum(['POS', 'ATM']),
      amount: moneyStringSchema,
      currency: currencySchema,
      status: statusSchema,
      createdAt: z.string(),
    }),
  ),
});
export type CardProcessing = z.infer<typeof cardProcessingSchema>;

export const simulationSchema = z.object({
  runId: z.string().nullable(),
  status: statusSchema,
  profile: z.enum(['SMALL', 'DEMO', 'LARGE']),
  speed: z.union([z.literal(1), z.literal(10), z.literal(60)]),
  virtualTime: z.string(),
  generatedTransactions: z.number().int().nonnegative(),
  lastActivityAt: z.string().nullable(),
});
export type Simulation = z.infer<typeof simulationSchema>;

export const commandReceiptSchema = z.object({
  commandId: z.string(),
  resourceId: z.string(),
  status: statusSchema,
  bookedAt: z.string().optional(),
});
export type CommandReceipt = z.infer<typeof commandReceiptSchema>;
