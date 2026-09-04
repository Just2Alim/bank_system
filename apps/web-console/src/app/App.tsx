import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { useState } from 'react';
import { RouterProvider, createBrowserRouter } from 'react-router-dom';

import { appRoutes } from '@/routing/AppRouter';
import { theme } from '@/theme/theme';

const router = createBrowserRouter(appRoutes);

export function App() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 5_000,
            retry: (failureCount, error) => {
              if (error instanceof Error && error.name === 'ZodError') {
                return false;
              }
              return failureCount < 2;
            },
          },
          mutations: { retry: false },
        },
      }),
  );

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
