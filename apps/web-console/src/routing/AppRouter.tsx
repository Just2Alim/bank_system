import { Navigate } from 'react-router-dom';
import type { RouteObject } from 'react-router-dom';

import { AppShell } from '@/layout/AppShell';
import { AccountsPage } from '@/features/customer/AccountsPage';
import { CardsPage } from '@/features/customer/CardsPage';
import { PaymentsPage } from '@/features/customer/PaymentsPage';
import { TransactionDetailsPage } from '@/features/customer/TransactionDetailsPage';
import { TopologyPage } from '@/features/topology/TopologyPage';
import {
  CardProcessingPage,
  ClearingPage,
  IsoMessagesPage,
  KafkaEventsPage,
  LedgerExplorerPage,
  NewTransferPage,
  OperationsOverviewPage,
  SettlementPage,
  SimulationControlPage,
  TransactionTracePage,
  NotFoundPage,
} from '@/features/operations/OperationsPages';

export const appRoutes: RouteObject[] = [
  { path: '/', element: <Navigate to="/ops/overview" replace /> },
  {
    path: '/customer',
    element: <AppShell workspace="customer" />,
    children: [
      { index: true, element: <Navigate to="/customer/accounts" replace /> },
      { path: 'accounts', element: <AccountsPage /> },
      { path: 'cards', element: <CardsPage /> },
      { path: 'payments', element: <PaymentsPage /> },
      { path: 'transactions/:transactionId', element: <TransactionDetailsPage /> },
      { path: 'transfers/new', element: <NewTransferPage /> },
    ],
  },
  {
    path: '/ops',
    element: <AppShell workspace="operations" />,
    children: [
      { index: true, element: <Navigate to="/ops/overview" replace /> },
      { path: 'overview', element: <OperationsOverviewPage /> },
      { path: 'topology', element: <TopologyPage /> },
      { path: 'transactions/search/trace', element: <TransactionTracePage /> },
      { path: 'ledger', element: <LedgerExplorerPage /> },
      { path: 'settlement', element: <SettlementPage /> },
      { path: 'clearing', element: <ClearingPage /> },
      { path: 'iso20022', element: <IsoMessagesPage /> },
      { path: 'kafka', element: <KafkaEventsPage /> },
      { path: 'cards', element: <CardProcessingPage /> },
      { path: 'simulation', element: <SimulationControlPage /> },
    ],
  },
  {
    path: '*',
    element: <AppShell workspace="operations" />,
    children: [{ index: true, element: <NotFoundPage /> }],
  },
];
