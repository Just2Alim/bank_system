import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded';
import { Box, Button, Card, CardContent, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import { AsyncState } from '@/components/AsyncState';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import { getApi, toErrorMessage } from '@/lib/api';
import { formatDateTime } from '@/lib/date';
import { topologySchema } from '@/lib/schemas';

export function TopologyPage() {
  const topology = useQuery({
    queryKey: ['operations', 'topology'],
    queryFn: () => getApi('/api/v1/ops/topology', topologySchema),
  });

  const activeRoutes = topology.data?.edges.filter((edge) => edge.active).length ?? 0;

  return (
    <>
      <PageHeader
        eyebrow="Operations center"
        title="Live banking topology"
        description="Watch bank cores, ledgers, national payment rails, card processors, and event streams as isolated simulator services."
      />
      <AsyncState
        loading={topology.isPending}
        label="Loading live topology"
        error={topology.isError ? toErrorMessage(topology.error) : null}
        onRetry={() => void topology.refetch()}
        empty={topology.data?.nodes.length === 0}
        emptyTitle="No services reported"
        emptyBody="Topology nodes will appear when the simulator publishes health snapshots."
      >
        {topology.data === undefined ? null : (
          <Stack spacing={2}>
            <Card variant="outlined">
              <CardContent>
                <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
                  <Typography sx={{ fontWeight: 750 }}>
                    {activeRoutes} active {activeRoutes === 1 ? 'route' : 'routes'}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Snapshot {formatDateTime(topology.data.generatedAt)}
                  </Typography>
                </Stack>
              </CardContent>
            </Card>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))',
                gap: 2,
              }}
            >
              {topology.data.nodes.map((node) => (
                <Button
                  key={node.id}
                  variant="outlined"
                  aria-label={node.label}
                  sx={{
                    p: 0,
                    justifyContent: 'stretch',
                    textAlign: 'left',
                    color: 'text.primary',
                    borderColor: 'divider',
                  }}
                >
                  <CardContent sx={{ width: '100%' }}>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
                      <Stack direction="row" gap={1} alignItems="center">
                        <AccountTreeRoundedIcon color="primary" aria-hidden="true" />
                        <Box>
                          <Typography sx={{ fontWeight: 780 }}>{node.label}</Typography>
                          <Typography variant="caption" color="text.secondary">
                            {node.group} · {node.kind}
                          </Typography>
                        </Box>
                      </Stack>
                      <StatusBadge status={node.health} />
                    </Stack>
                    <Stack direction="row" gap={2} sx={{ mt: 2 }}>
                      <Typography variant="body2">{node.throughputPerSecond.toFixed(1)} tx/s</Typography>
                      <Typography variant="body2" color="text.secondary">
                        p95 {node.p95LatencyMs.toFixed(0)} ms
                      </Typography>
                    </Stack>
                  </CardContent>
                </Button>
              ))}
            </Box>
          </Stack>
        )}
      </AsyncState>
    </>
  );
}
