import { Card, CardContent, CardHeader } from '@mui/material';
import type { PropsWithChildren, ReactNode } from 'react';

interface SectionCardProps {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}

export function SectionCard({ title, subtitle, action, children }: PropsWithChildren<SectionCardProps>) {
  return (
    <Card>
      <CardHeader title={title} subheader={subtitle} action={action} slotProps={{ title: { variant: 'h2' } }} />
      <CardContent sx={{ pt: 0 }}>{children}</CardContent>
    </Card>
  );
}
