const SEOUL_TIME_ZONE = "Asia/Seoul";
const SEOUL_UTC_OFFSET_MINUTES = 9 * 60;
const MILLISECONDS_PER_MINUTE = 60 * 1000;
const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const LOCAL_DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$/;
const OFFSET_SUFFIX_PATTERN = /(Z|[+-]\d{2}:\d{2})$/;

const seoulDatePartsFormatter = new Intl.DateTimeFormat("en-US", {
  timeZone: SEOUL_TIME_ZONE,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

export type SeoulDateParts = {
  year: number;
  month: number;
  monthIndex: number;
  day: number;
};

function padTwoDigits(value: number) {
  return String(value).padStart(2, "0");
}

function readNumericPart(parts: Intl.DateTimeFormatPart[], type: "year" | "month" | "day") {
  const value = parts.find(part => part.type === type)?.value;
  if (!value) {
    throw new Error(`Missing ${type} part for Seoul time conversion.`);
  }
  return Number(value);
}

function normalizeMilliseconds(fractionalPart?: string) {
  if (!fractionalPart) {
    return 0;
  }
  return Number(fractionalPart.slice(0, 3).padEnd(3, "0"));
}

function buildEpochDay(year: number, monthIndex: number, day: number) {
  return Math.floor(Date.UTC(year, monthIndex, day) / MILLISECONDS_PER_DAY);
}

function toDateValue(value: Date | string | number) {
  if (value instanceof Date) {
    return value;
  }
  if (typeof value === "string") {
    return new Date(parseApiDateTimeToTimestamp(value));
  }
  return new Date(value);
}

export function formatYearMonth(year: number, monthIndex: number) {
  return `${year}-${padTwoDigits(monthIndex + 1)}`;
}

export function formatDateString(year: number, monthIndex: number, day: number) {
  return `${formatYearMonth(year, monthIndex)}-${padTwoDigits(day)}`;
}

export function parseDateOnly(dateStr: string): SeoulDateParts {
  const match = DATE_ONLY_PATTERN.exec(dateStr);
  if (!match) {
    throw new Error(`Invalid date string: ${dateStr}`);
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);

  return {
    year,
    month,
    monthIndex: month - 1,
    day,
  };
}

export function getSeoulDateParts(date: Date = new Date()): SeoulDateParts {
  const parts = seoulDatePartsFormatter.formatToParts(date);
  const year = readNumericPart(parts, "year");
  const month = readNumericPart(parts, "month");
  const day = readNumericPart(parts, "day");

  return {
    year,
    month,
    monthIndex: month - 1,
    day,
  };
}

export function getCurrentSeoulDateInfo(now: Date = new Date()) {
  const dateParts = getSeoulDateParts(now);
  return {
    ...dateParts,
    dateString: formatDateString(dateParts.year, dateParts.monthIndex, dateParts.day),
    yearMonth: formatYearMonth(dateParts.year, dateParts.monthIndex),
  };
}

export function getDayOfWeek(year: number, monthIndex: number, day: number) {
  return new Date(Date.UTC(year, monthIndex, day)).getUTCDay();
}

export function getDayOfWeekFromDateString(dateStr: string) {
  const { year, monthIndex, day } = parseDateOnly(dateStr);
  return getDayOfWeek(year, monthIndex, day);
}

export function getMonthStartDayOfWeek(year: number, monthIndex: number) {
  return getDayOfWeek(year, monthIndex, 1);
}

export function getDaysInMonth(year: number, monthIndex: number) {
  return new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
}

export function getDateDiffInDays(dateStr: string, referenceDateStr: string) {
  const target = parseDateOnly(dateStr);
  const reference = parseDateOnly(referenceDateStr);

  return buildEpochDay(target.year, target.monthIndex, target.day)
    - buildEpochDay(reference.year, reference.monthIndex, reference.day);
}

export function parseApiDateTimeToTimestamp(value: string) {
  if (OFFSET_SUFFIX_PATTERN.test(value)) {
    return new Date(value).getTime();
  }

  const dateOnlyMatch = DATE_ONLY_PATTERN.exec(value);
  if (dateOnlyMatch) {
    const { year, monthIndex, day } = parseDateOnly(value);
    return Date.UTC(year, monthIndex, day) - (SEOUL_UTC_OFFSET_MINUTES * MILLISECONDS_PER_MINUTE);
  }

  const match = LOCAL_DATE_TIME_PATTERN.exec(value);
  if (!match) {
    return new Date(value).getTime();
  }

  const year = Number(match[1]);
  const monthIndex = Number(match[2]) - 1;
  const day = Number(match[3]);
  const hour = Number(match[4]);
  const minute = Number(match[5]);
  const second = Number(match[6] ?? "0");
  const milliseconds = normalizeMilliseconds(match[7]);

  return Date.UTC(year, monthIndex, day, hour, minute, second, milliseconds)
    - (SEOUL_UTC_OFFSET_MINUTES * MILLISECONDS_PER_MINUTE);
}

export function compareApiDateTimesDesc(left: string, right: string) {
  return parseApiDateTimeToTimestamp(right) - parseApiDateTimeToTimestamp(left);
}

export function formatDateInSeoul(
  value: Date | string | number,
  locale: string,
  options: Intl.DateTimeFormatOptions
) {
  return new Intl.DateTimeFormat(locale, {
    ...options,
    timeZone: SEOUL_TIME_ZONE,
  }).format(toDateValue(value));
}
