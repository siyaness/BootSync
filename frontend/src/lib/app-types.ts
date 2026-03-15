export type AttendanceStatus = "출석" | "지각" | "조퇴" | "결석";

export interface AttendanceRecord {
  date: string;
  status: AttendanceStatus;
  memo?: string;
}

export interface AttendanceMonthlySummary {
  yearMonth: string;
  baseAllowanceAmount: number;
  absenceDeductionAmount: number;
  presentCount: number;
  lateCount: number;
  leaveEarlyCount: number;
  absentCount: number;
  convertedAbsenceCount: number;
  deductionAmount: number;
  expectedAllowanceAmount: number;
}

export interface AttendanceBulkFillResult {
  startDate: string;
  endDate: string;
  createdCount: number;
}

export interface AllowanceSummary {
  referenceYearMonth: string;
  periodStartDate: string;
  periodEndDate: string;
  scheduledTrainingDays: number;
  completedScheduledDays: number;
  recordedTrainingDays: number;
  unrecordedCompletedDays: number;
  presentCount: number;
  lateCount: number;
  leaveEarlyCount: number;
  absentCount: number;
  convertedAbsenceCount: number;
  recognizedAttendanceDays: number;
  payableAttendanceDays: number;
  dailyAllowanceAmount: number;
  payableDayCap: number;
  maximumAllowanceAmount: number;
  expectedAllowanceAmount: number;
}

export interface TrainingSummary {
  courseLabel: string | null;
  courseStartDate: string;
  courseEndDate: string;
  thresholdPercent: number;
  daysUntilCourseEnd: number;
  monthScheduledDays: number;
  monthCompletedDays: number;
  monthRemainingDays: number;
  monthHolidayDates: string[];
  courseScheduledDays: number;
  courseCompletedDays: number;
  courseRemainingDays: number;
  recordedCompletedDays: number;
  presentCount: number;
  lateCount: number;
  leaveEarlyCount: number;
  absentCount: number;
  unrecordedCompletedDays: number;
  effectiveAbsenceCount: number;
  effectivePresentDays: number;
  attendanceRatePercent: number;
  minimumRequiredPresentDays: number;
  remainingAbsenceBudget: number;
  canReachThreshold: boolean;
  belowThreshold: boolean;
}

export type TrainingDayCode =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface TrainingProfileSettings {
  configured: boolean;
  courseLabel: string | null;
  courseStartDate: string;
  courseEndDate: string;
  attendanceThresholdPercent: number;
  dailyAllowanceAmount: number;
  payableDayCap: number;
  maximumAllowanceAmount: number;
  trainingDays: TrainingDayCode[];
  holidayDates: string[];
}

export interface TrainingProfileDraft {
  courseLabel: string | null;
  courseStartDate: string;
  courseEndDate: string;
  attendanceThresholdPercent: number;
  dailyAllowanceAmount: number;
  payableDayCap: number;
  trainingDays: TrainingDayCode[];
  holidayDates: string[];
}

export interface Snippet {
  id: string;
  title: string;
  tags: string[];
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  username: string;
  displayName: string;
  recoveryEmail: string;
  emailVerified: boolean;
  accountStatus: "active" | "pending_deletion";
  deletionDate?: string;
}
