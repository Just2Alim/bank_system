import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import PauseCircleOutlineRoundedIcon from '@mui/icons-material/PauseCircleOutlineRounded';
import SyncRoundedIcon from '@mui/icons-material/SyncRounded';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';
import { Chip } from '@mui/material';
import type { ChipProps } from '@mui/material';

type SemanticStatus = {
  label: string;
  color: NonNullable<ChipProps['color']>;
  icon: typeof CheckCircleOutlineRoundedIcon;
};

const STATUS_MAP: Readonly<Record<string, SemanticStatus>> = Object.freeze({
  UP: { label: 'Healthy', color: 'success', icon: CheckCircleOutlineRoundedIcon },
  HEALTHY: { label: 'Healthy', color: 'success', icon: CheckCircleOutlineRoundedIcon },
  COMPLETED: { label: 'Completed', color: 'success', icon: CheckCircleOutlineRoundedIcon },
  SETTLED: { label: 'Settled', color: 'success', icon: CheckCircleOutlineRoundedIcon },
  APPROVED: { label: 'Approved', color: 'success', icon: CheckCircleOutlineRoundedIcon },
  RUNNING: { label: 'Running', color: 'info', icon: SyncRoundedIcon },
  PROCESSING: { label: 'Processing', color: 'info', icon: SyncRoundedIcon },
  PENDING: { label: 'Pending', color: 'warning', icon: HourglassEmptyRoundedIcon },
  QUEUED: { label: 'Queued', color: 'warning', icon: HourglassEmptyRoundedIcon },
  QUEUED_LIQUIDITY: { label: 'Queued for liquidity', color: 'warning', icon: HourglassEmptyRoundedIcon },
  DEGRADED: { label: 'Degraded', color: 'warning', icon: WarningAmberRoundedIcon },
  PAUSED: { label: 'Paused', color: 'default', icon: PauseCircleOutlineRoundedIcon },
  STOPPED: { label: 'Stopped', color: 'default', icon: PauseCircleOutlineRoundedIcon },
  DOWN: { label: 'Down', color: 'error', icon: ErrorOutlineRoundedIcon },
  FAILED: { label: 'Failed', color: 'error', icon: ErrorOutlineRoundedIcon },
  REJECTED: { label: 'Rejected', color: 'error', icon: ErrorOutlineRoundedIcon },
  DECLINED: { label: 'Declined', color: 'error', icon: ErrorOutlineRoundedIcon },
});

export function StatusBadge({ status, size = 'small' }: { status: string; size?: 'small' | 'medium' }) {
  const normalized = status.toUpperCase();
  const semantic = STATUS_MAP[normalized] ?? {
    label: status.replaceAll('_', ' '),
    color: 'default' as const,
    icon: HourglassEmptyRoundedIcon,
  };
  const Icon = semantic.icon;

  return <Chip size={size} color={semantic.color} icon={<Icon />} label={semantic.label} variant="outlined" />;
}
