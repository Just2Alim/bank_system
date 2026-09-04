import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#075E63',
      dark: '#05474B',
      light: '#D9EEEF',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#355A7A',
    },
    background: {
      default: '#F4F7FA',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#102235',
      secondary: '#52667A',
    },
    divider: '#D7E0E8',
    success: { main: '#16784A' },
    warning: { main: '#9A6400' },
    error: { main: '#B42318' },
    info: { main: '#1D5E91' },
  },
  shape: {
    borderRadius: 10,
  },
  typography: {
    fontFamily:
      'Inter, ui-sans-serif, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontSize: 'clamp(1.6rem, 2.3vw, 2.15rem)', fontWeight: 720, lineHeight: 1.2 },
    h2: { fontSize: '1.25rem', fontWeight: 700, lineHeight: 1.3 },
    h3: { fontSize: '1rem', fontWeight: 700, lineHeight: 1.4 },
    button: { textTransform: 'none', fontWeight: 650 },
    body1: { lineHeight: 1.55 },
    body2: { lineHeight: 1.5 },
  },
  components: {
    MuiButtonBase: {
      defaultProps: { disableRipple: true },
      styleOverrides: { root: { minHeight: 44 } },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #D7E0E8',
          boxShadow: '0 1px 2px rgba(16, 34, 53, 0.04)',
        },
      },
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 650 } },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          color: '#52667A',
          background: '#F8FAFC',
          fontWeight: 700,
        },
      },
    },
  },
});
