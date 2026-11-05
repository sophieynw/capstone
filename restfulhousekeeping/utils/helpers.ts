// utils/helpers.ts
import {
  DaysOfTheWeek,
  type AvailabilitySlot,
  type Cleaner,
  type Cleaning,
} from '@/types/entityTypes';

const DAY_ORDER: Record<DaysOfTheWeek, number> = {
  [DaysOfTheWeek.MONDAY]: 0,
  [DaysOfTheWeek.TUESDAY]: 1,
  [DaysOfTheWeek.WEDNESDAY]: 2,
  [DaysOfTheWeek.THURSDAY]: 3,
  [DaysOfTheWeek.FRIDAY]: 4,
  [DaysOfTheWeek.SATURDAY]: 5,
  [DaysOfTheWeek.SUNDAY]: 6,
};

function timeToMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

export function sortAvailabilitySlots(
  slots: AvailabilitySlot[] | null | undefined,
): AvailabilitySlot[] {
  return [...(slots ?? [])].sort(
    (first, second) =>
      DAY_ORDER[first.dayOfWeek] - DAY_ORDER[second.dayOfWeek] ||
      timeToMinutes(first.startTime) - timeToMinutes(second.startTime),
  );
}

function timeToSeconds(time: string): number {
  const [hours, minutes, seconds = 0] = time.split(':').map(Number);
  return hours * 3600 + minutes * 60 + seconds;
}

export function getAvailableCleaners(
  cleaners: Cleaner[] | undefined,
  start: string | null,
  end: string | null,
): Cleaner[] {
  if (!start || !end) return [];

  const [startDate, startTime] = start.split('T');
  const [endDate, endTime] = end.split('T');
  if (startDate !== endDate || !startTime || !endTime) return [];

  const startSeconds = timeToSeconds(startTime);
  const endSeconds = timeToSeconds(endTime);
  if (!(endSeconds > startSeconds)) return [];

  // The API uses local calendar dates. UTC here only identifies the weekday.
  const weekdays = [
    DaysOfTheWeek.SUNDAY,
    DaysOfTheWeek.MONDAY,
    DaysOfTheWeek.TUESDAY,
    DaysOfTheWeek.WEDNESDAY,
    DaysOfTheWeek.THURSDAY,
    DaysOfTheWeek.FRIDAY,
    DaysOfTheWeek.SATURDAY,
  ];
  const day = weekdays[new Date(startDate).getUTCDay()];

  return (cleaners ?? []).filter((cleaner) => {
    const slots = (cleaner.availabilitySlots ?? [])
      .filter((slot) => slot.dayOfWeek === day && slot.startTime && slot.endTime)
      .sort((a, b) => timeToSeconds(a.startTime) - timeToSeconds(b.startTime));

    let coveredUntil = startSeconds;
    for (const slot of slots) {
      if (timeToSeconds(slot.startTime) > coveredUntil) break;
      coveredUntil = Math.max(coveredUntil, timeToSeconds(slot.endTime));
      if (coveredUntil >= endSeconds) return true;
    }
    return false;
  });
}

export function toTitleCase(value: string): string {
  return value.toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

// Example output: Fri Jul 24 8:30 am
export function toFriendlyDate(
  value: Date | string | null | undefined,
): string {
  if (value == null) return 'Date unavailable';

  const date = value instanceof Date ? value : new Date(value);

  if (Number.isNaN(date.getTime())) return 'Invalid date';

  return date
    .toLocaleString('en-CA', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    })
    .replaceAll(',', '');
}

// Example output: "2026-10-24T19:28:00"
// Used for API calls
export function formatLocalDateTime(date: Date): string {
  const pad = (value: number) => value.toString().padStart(2, '0');

  return (
    `${date.getFullYear()}-` +
    `${pad(date.getMonth() + 1)}-` +
    `${pad(date.getDate())}T` +
    `${pad(date.getHours())}:` +
    `${pad(date.getMinutes())}:00`
  );
}

// groups cleanings by date
export function groupCleaningsByDate(
  cleanings: Cleaning[] | null | undefined,
  now: Date = new Date(),
): {
  today: Cleaning[];
  upcoming: Cleaning[];
} {
  const today: Cleaning[] = [];
  const upcoming: Cleaning[] = [];

  const startOfToday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate(),
  );

  const startOfTomorrow = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate() + 1,
  );

  for (const cleaning of cleanings ?? []) {
    if (!cleaning.dateTimeStart) continue;

    const cleaningDate = new Date(cleaning.dateTimeStart);

    // Ignore invalid dates
    if (Number.isNaN(cleaningDate.getTime())) continue;

    if (cleaningDate >= startOfToday && cleaningDate < startOfTomorrow) {
      today.push(cleaning);
    } else if (cleaningDate >= startOfTomorrow) {
      upcoming.push(cleaning);
    }
  }

  return { today, upcoming };
}

export function timeStringToDate(time: string): Date {
  const [h, m] = time.split(':').map(Number);
  const d = new Date();
  d.setHours(h, m, 0, 0);
  return d;
}

export function dateToTimeString(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function formatTime(time: string) {
  const [hours, minutes] = time.split(':').map(Number);
  const period = hours >= 12 ? 'PM' : 'AM';
  const hour12 = hours % 12 || 12;
  return `${hour12}:${String(minutes).padStart(2, '0')} ${period}`;
}
