export function formatDateTimeLabel(
  value: Date | string | null | undefined,
  { includeWeekday = false }: { includeWeekday?: boolean } = {},
): string {
  if (value == null) return 'Date unavailable';

  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return 'Invalid date';

  const day = date.toLocaleDateString('en-US', {
    weekday: includeWeekday ? 'short' : undefined,
    month: 'short',
    day: 'numeric',
  });
  const time = date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });

  return `${day.replace(/,/g, '')}, ${time.replace(/\s/g, '')}`;
}
