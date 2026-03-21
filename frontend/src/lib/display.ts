import type { Snippet } from "@/lib/app-types";
import { formatDateInSeoul, getSeoulDateParts, parseApiDateTimeToTimestamp } from "@/lib/seoul-time";

export function getKoreanDateString(date: Date = new Date()): string {
  return formatDateInSeoul(date, "ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
  });
}

export function getRelativeTime(dateStr: string): string {
  const timestamp = parseApiDateTimeToTimestamp(dateStr);
  if (!Number.isFinite(timestamp)) {
    return dateStr;
  }

  const diff = Math.max(0, Date.now() - timestamp);
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  if (days < 30) return `${Math.floor(days / 7)}주 전`;

  const { year, month, day } = getSeoulDateParts(new Date(timestamp));
  const monthText = String(month).padStart(2, "0");
  const dayText = String(day).padStart(2, "0");
  return `${year}. ${monthText}. ${dayText}`;
}

export function formatKRW(amount: number): string {
  return `${new Intl.NumberFormat("ko-KR").format(amount)}원`;
}

export function getAllTags(snippets: Pick<Snippet, "tags">[]): string[] {
  const tagSet = new Set<string>();
  snippets.forEach(snippet => snippet.tags.forEach(tag => tagSet.add(tag)));
  return Array.from(tagSet).sort();
}
