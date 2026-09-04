import ArrowDownwardRoundedIcon from '@mui/icons-material/ArrowDownwardRounded';
import ArrowUpwardRoundedIcon from '@mui/icons-material/ArrowUpwardRounded';
import RemoveRoundedIcon from '@mui/icons-material/RemoveRounded';
import { Card, CardContent, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface MetricCardProps {
  label: string;
  value: ReactNode;
  deltaPercent?: number | null;
  helper?: string;
}

export function MetricCard({ label, value, deltaPercent, helper }: MetricCardProps) {
  const delta = deltaPercent ?? 0;
  const DeltaIcon = delta > 0 ? ArrowUpwardRoundedIcon : delta < 0 ? ArrowDownwardRoundedIcon : RemoveRoundedIcon;

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 650 }}>
          {label}
        </Typography>
        <Typography component="p" sx={{ mt: 0.75, fontSize: '1.65rem', fontWeight: 760, fontVariantNumeric: 'tabular-nums' }}>
          {value}
        </Typography>
        {deltaPercent === undefined ? null : (
          <Stack direction="row" alignItems="center" gap={0.5} sx={{ mt: 0.5 }}>
            <DeltaIcon aria-hidden="true" sx={{ fontSize: 16 }} />
            <Typography variant="caption" color="text.secondary">
              {Math.abs(delta).toFixed(1)}% from previous window
            </Typography>
          </Stack>
        )}
        {helper === undefined ? null : (
          <Typography variant="caption" color="text.secondary">
            {helper}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
