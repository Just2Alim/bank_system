import AccountBalanceRoundedIcon from '@mui/icons-material/AccountBalanceRounded';
import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded';
import ArticleOutlinedIcon from '@mui/icons-material/ArticleOutlined';
import AssessmentOutlinedIcon from '@mui/icons-material/AssessmentOutlined';
import AutorenewRoundedIcon from '@mui/icons-material/AutorenewRounded';
import CreditCardRoundedIcon from '@mui/icons-material/CreditCardRounded';
import DashboardRoundedIcon from '@mui/icons-material/DashboardRounded';
import DataObjectRoundedIcon from '@mui/icons-material/DataObjectRounded';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import HubRoundedIcon from '@mui/icons-material/HubRounded';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded';
import ReceiptLongRoundedIcon from '@mui/icons-material/ReceiptLongRounded';
import RouteRoundedIcon from '@mui/icons-material/RouteRounded';
import ScienceRoundedIcon from '@mui/icons-material/ScienceRounded';
import SwapHorizRoundedIcon from '@mui/icons-material/SwapHorizRounded';
import {
  AppBar,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import type { SvgIconComponent } from '@mui/icons-material';
import { useState } from 'react';
import { Outlet } from 'react-router-dom';

import { RouterNavLinkBehavior } from '@/components/RouterLinks';

export type Workspace = 'customer' | 'operations';

interface NavigationItem {
  label: string;
  to: string;
  icon: SvgIconComponent;
}

const CUSTOMER_NAVIGATION: readonly NavigationItem[] = Object.freeze([
  { label: 'Accounts', to: '/customer/accounts', icon: AccountBalanceRoundedIcon },
  { label: 'Cards', to: '/customer/cards', icon: CreditCardRoundedIcon },
  { label: 'Payments', to: '/customer/payments', icon: PaymentsRoundedIcon },
  { label: 'New transfer', to: '/customer/transfers/new', icon: SwapHorizRoundedIcon },
]);

const OPERATIONS_NAVIGATION: readonly NavigationItem[] = Object.freeze([
  { label: 'Overview', to: '/ops/overview', icon: DashboardRoundedIcon },
  { label: 'Live topology', to: '/ops/topology', icon: HubRoundedIcon },
  { label: 'Transaction trace', to: '/ops/transactions/search/trace', icon: RouteRoundedIcon },
  { label: 'Ledger', to: '/ops/ledger', icon: ReceiptLongRoundedIcon },
  { label: 'Settlement', to: '/ops/settlement', icon: AccountBalanceRoundedIcon },
  { label: 'Clearing', to: '/ops/clearing', icon: AutorenewRoundedIcon },
  { label: 'ISO 20022', to: '/ops/iso20022', icon: ArticleOutlinedIcon },
  { label: 'Kafka events', to: '/ops/kafka', icon: EventNoteRoundedIcon },
  { label: 'Card processing', to: '/ops/cards', icon: CreditCardRoundedIcon },
  { label: 'Simulation', to: '/ops/simulation', icon: ScienceRoundedIcon },
]);

const DRAWER_WIDTH = 264;

export function AppShell({ workspace }: { workspace: Workspace }) {
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('lg'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const navigation = workspace === 'customer' ? CUSTOMER_NAVIGATION : OPERATIONS_NAVIGATION;

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100%' }}>
      <Toolbar sx={{ px: 2.5 }}>
        <Stack sx={{ flexDirection: 'row', alignItems: 'center', gap: 1.25 }}>
          <Box
            aria-hidden="true"
            sx={{
              width: 34,
              height: 34,
              display: 'grid',
              placeItems: 'center',
              borderRadius: 1,
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              fontWeight: 800,
            }}
          >
            KZ
          </Box>
          <Box>
            <Typography sx={{ fontWeight: 800, lineHeight: 1.2 }}>Ecosystem Console</Typography>
            <Typography variant="caption" color="text.secondary">
              Educational simulator
            </Typography>
          </Box>
        </Stack>
      </Toolbar>
      <Divider />
      <Box sx={{ p: 1.5 }}>
        <Typography variant="overline" color="text.secondary" sx={{ px: 1.5, fontWeight: 800 }}>
          {workspace === 'customer' ? 'Customer banking' : 'Operations center'}
        </Typography>
        <List component="nav" aria-label={`${workspace} workspace`} sx={{ mt: 0.5 }}>
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <ListItemButton
                key={item.to}
                component={RouterNavLinkBehavior}
                to={item.to}
                onClick={() => setMobileOpen(false)}
                sx={{
                  mb: 0.25,
                  borderRadius: 1,
                  color: 'text.secondary',
                  '&.active': {
                    color: 'primary.dark',
                    bgcolor: 'primary.light',
                    '& .MuiListItemIcon-root': { color: 'primary.main' },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 38 }}>
                  <Icon fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={item.label} slotProps={{ primary: { sx: { fontWeight: 650 } } }} />
              </ListItemButton>
            );
          })}
        </List>
      </Box>
      <Box sx={{ flex: 1 }} />
      <Divider />
      <Box sx={{ p: 2 }}>
        <Typography variant="caption" color="text.secondary">
          Demo data or an explicitly configured bank sandbox. Production banking is never enabled here.
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          ml: { lg: `${DRAWER_WIDTH}px` },
          width: { lg: `calc(100% - ${DRAWER_WIDTH}px)` },
        }}
      >
        <Toolbar sx={{ gap: 1.5 }}>
          {desktop ? null : (
            <Tooltip title="Open navigation">
              <IconButton aria-label="Open navigation" onClick={() => setMobileOpen(true)}>
                <MenuRoundedIcon />
              </IconButton>
            </Tooltip>
          )}
          <Stack sx={{ minWidth: 0, flexDirection: 'row', alignItems: 'center', gap: 1 }}>
            <AssessmentOutlinedIcon color="primary" aria-hidden="true" />
            <Typography noWrap sx={{ fontWeight: 750 }}>
              {workspace === 'customer' ? 'Customer workspace' : 'Operations workspace'}
            </Typography>
          </Stack>
          <Box sx={{ flex: 1 }} />
          <Button
            component={RouterNavLinkBehavior}
            to={workspace === 'customer' ? '/ops/overview' : '/customer/accounts'}
            variant="outlined"
            size="small"
            startIcon={workspace === 'customer' ? <DataObjectRoundedIcon /> : <AccountTreeRoundedIcon />}
          >
            <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>
              Switch to {workspace === 'customer' ? 'operations' : 'customer'}
            </Box>
            <Box component="span" sx={{ display: { xs: 'inline', sm: 'none' } }}>
              Switch
            </Box>
          </Button>
        </Toolbar>
      </AppBar>
      <Box component="nav" aria-label={`${workspace} workspace`}>
        <Drawer
          variant={desktop ? 'permanent' : 'temporary'}
          open={desktop || mobileOpen}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: DRAWER_WIDTH,
              borderRightColor: 'divider',
            },
          }}
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        id="main-content"
        component="main"
        tabIndex={-1}
        sx={{
          flex: 1,
          minWidth: 0,
          pt: { xs: 11, sm: 12 },
          pb: 6,
          px: { xs: 2, sm: 3, xl: 5 },
          ml: { lg: `${DRAWER_WIDTH}px` },
        }}
      >
        <Box sx={{ maxWidth: 1600, mx: 'auto' }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
