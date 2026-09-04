const DATE_TIME_FORMATTER = new Intl.DateTimeFormat('en-KZ', {
  dateStyle: 'medium',
  timeStyle: 'medium',
  timeZone: 'Asia/Almaty',
});

export function formatDateTime(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 'Invalid timestamp' : DATE_TIME_FORMATTER.format(date);
}

export function formatDuration(milliseconds: number): string {
  if (!Number.isFinite(milliseconds) || milliseconds < 0) {
    return '—';
  }
  if (milliseconds < 1000) {
    return `${Math.round(milliseconds)} ms`;
  }
  return `${(milliseconds / 1000).toFixed(2)} s`;
}
