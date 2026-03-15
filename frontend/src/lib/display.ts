import type { Snippet } from "@/lib/app-types";

export function getKoreanDateString(date: Date = new Date()): string {
  const days = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"];
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const dayOfMonth = date.getDate();
  const dayName = days[date.getDay()];
  return `${year}년 ${month}월 ${dayOfMonth}일 ${dayName}`;
}

export function getRelativeTime(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "방금 전";
  if (minutes < 60) return `${minutes}분 전`;
  if (hours < 24) return `${hours}시간 전`;
  if (days < 7) return `${days}일 전`;
  if (days < 30) return `${Math.floor(days / 7)}주 전`;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const dayOfMonth = String(date.getDate()).padStart(2, "0");
  return `${year}. ${month}. ${dayOfMonth}`;
}

export function formatKRW(amount: number): string {
  return `${new Intl.NumberFormat("ko-KR").format(amount)}원`;
}

export function getAllTags(snippets: Pick<Snippet, "tags">[]): string[] {
  const tagSet = new Set<string>();
  snippets.forEach(snippet => snippet.tags.forEach(tag => tagSet.add(tag)));
  return Array.from(tagSet).sort();
}
