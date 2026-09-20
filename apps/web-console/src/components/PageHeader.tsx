import { Box, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description: string;
  eyebrow?: string;
  actions?: ReactNode;
}

export function PageHeader({ title, description, eyebrow, actions }: PageHeaderProps) {
  return (
    <Stack
      component="header"
      sx={{
        mb: 3,
        flexDirection: { xs: 'column', md: 'row' },
        alignItems: { xs: 'flex-start', md: 'center' },
        justifyContent: 'space-between',
        gap: 2,
      }}
    >
      <Box>
        {eyebrow === undefined ? null : (
          <Typography
            variant="overline"
            color="primary.dark"
            sx={{ fontWeight: 800, letterSpacing: '0.08em' }}
          >
            {eyebrow}
          </Typography>
        )}
        <Typography component="h1" variant="h1" gutterBottom>
          {title}
        </Typography>
        <Typography color="text.secondary" sx={{ maxWidth: 760 }}>
          {description}
        </Typography>
      </Box>
      {actions}
    </Stack>
  );
}
