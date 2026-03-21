import type { AttendanceMonthlySummary, TrainingSummary } from "@/lib/app-types";
import { getCurrentSeoulDateInfo, getDateDiffInDays } from "@/lib/seoul-time";

export type DashboardRiskTone = "good" | "warning" | "critical";

export interface DashboardRiskItem {
  key: string;
  tone: DashboardRiskTone;
  title: string;
  description: string;
}

function getProgressWarning(label: "지각" | "조퇴", count: number): DashboardRiskItem {
  if (count === 0) {
    return {
      key: label,
      tone: "good",
      title: `${label} 누적 0회`,
      description: `${label} 3회까지는 추가 공제 없이 기록됩니다.`,
    };
  }

  const remainder = count % 3;
  if (remainder === 0) {
    return {
      key: label,
      tone: "warning",
      title: `${label} ${count}회 누적`,
      description: `이미 결석 환산 ${Math.floor(count / 3)}회에 반영되었습니다. 다음 환산까지 ${label} 3회 여유가 있습니다.`,
    };
  }

  const remaining = 3 - remainder;
  return {
    key: label,
    tone: remaining === 1 ? "critical" : "warning",
    title: `${label} ${count}회 누적`,
    description: `${label} ${remaining}회 더 누적되면 결석 환산 1회가 추가됩니다.`,
  };
}

export function getDaysUntilCourseEnd(courseEndDate: string, now = new Date()) {
  return getDateDiffInDays(courseEndDate, getCurrentSeoulDateInfo(now).dateString);
}

export function formatCourseCountdown(daysUntilCourseEnd: number) {
  if (daysUntilCourseEnd < 0) {
    return "수료 완료";
  }
  if (daysUntilCourseEnd === 0) {
    return "D-Day";
  }
  return `D-${daysUntilCourseEnd}`;
}

export function buildMonthlyRiskItems(monthlySummary: AttendanceMonthlySummary): DashboardRiskItem[] {
  const items = [
    getProgressWarning("지각", monthlySummary.lateCount),
    getProgressWarning("조퇴", monthlySummary.leaveEarlyCount),
  ];

  if (monthlySummary.convertedAbsenceCount > 0) {
    items.push({
      key: "converted-absence",
      tone: monthlySummary.convertedAbsenceCount >= 2 ? "critical" : "warning",
      title: `결석 환산 ${monthlySummary.convertedAbsenceCount}회`,
      description: `이번 달 누적 공제는 ${monthlySummary.deductionAmount.toLocaleString("ko-KR")}원입니다.`,
    });
  } else {
    items.push({
      key: "converted-absence",
      tone: "good",
      title: "추가 공제 없음",
      description: "아직 결석 환산이 발생하지 않아 예상 장려금이 유지되고 있습니다.",
    });
  }

  return items;
}

export function buildTrainingRiskItems(trainingSummary: TrainingSummary | undefined): DashboardRiskItem[] {
  if (!trainingSummary) {
    return [];
  }

  const items: DashboardRiskItem[] = [];

  if (!trainingSummary.canReachThreshold) {
    items.push({
      key: "threshold",
      tone: "critical",
      title: "이제는 수료 기준을 맞추기 어려워요",
      description: `남은 수업일을 모두 출석해도 출석률 ${trainingSummary.thresholdPercent}%를 맞추기 어려운 상태입니다.`,
    });
  } else if (trainingSummary.remainingAbsenceBudget <= 2) {
    items.push({
      key: "threshold",
      tone: "warning",
      title: `앞으로 더 빠져도 되는 날 ${trainingSummary.remainingAbsenceBudget}일`,
      description: `이 숫자 안에서는 출석률 ${trainingSummary.thresholdPercent}%를 유지할 수 있지만, 여유가 거의 없습니다.`,
    });
  } else {
    items.push({
      key: "threshold",
      tone: "good",
      title: `앞으로 더 빠져도 되는 날 ${trainingSummary.remainingAbsenceBudget}일`,
      description: `이 숫자 안에서는 출석률 ${trainingSummary.thresholdPercent}%를 유지할 수 있어요.`,
    });
  }

  if (trainingSummary.unrecordedCompletedDays > 0) {
    items.push({
      key: "unrecorded",
      tone: "warning",
      title: `미입력 수업일 ${trainingSummary.unrecordedCompletedDays}일`,
      description: "이미 지난 수업일 중 아직 기록되지 않은 날짜가 있어 과정 기준 출석률 참고치가 낮아질 수 있습니다.",
    });
  }

  if (trainingSummary.monthHolidayDates.length > 0) {
    const visibleDates = trainingSummary.monthHolidayDates
      .map(date => date.slice(5).replace("-", "."))
      .join(", ");
    items.push({
      key: "holidays",
      tone: "good",
      title: `이번 달 반영된 휴강일 ${trainingSummary.monthHolidayDates.length}일`,
      description: `${visibleDates} 일정은 수업일 계산에서 제외됩니다.`,
    });
  }

  return items;
}
