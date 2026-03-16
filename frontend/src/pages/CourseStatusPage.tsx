import { useEffect, useState } from 'react';
import { AppApiError } from '@/lib/api';
import type { AllowanceSummary, AttendanceMonthlySummary, TrainingDayCode } from '@/lib/app-types';
import {
  buildMonthlyRiskItems,
  buildTrainingRiskItems,
  formatCourseCountdown,
} from '@/lib/attendance-insights';
import { formatKRW } from '@/lib/display';
import { useApp } from '@/lib/store';
import { cn } from '@/lib/utils';
import {
  AlertTriangle,
  ChevronRight,
  Clock3,
  PencilLine,
  RotateCcw,
  Save,
  ShieldAlert,
  Target,
} from 'lucide-react';

const TRAINING_DAY_OPTIONS: Array<{ value: TrainingDayCode; label: string }> = [
  { value: 'MONDAY', label: '월' },
  { value: 'TUESDAY', label: '화' },
  { value: 'WEDNESDAY', label: '수' },
  { value: 'THURSDAY', label: '목' },
  { value: 'FRIDAY', label: '금' },
  { value: 'SATURDAY', label: '토' },
  { value: 'SUNDAY', label: '일' },
];

const RISK_TONE_STYLES = {
  good: {
    border: 'border-[#2E7D5E]/20',
    badge: 'bg-[#D1FAE5] text-[#2E7D5E]',
    text: 'text-[#2E7D5E]',
  },
  warning: {
    border: 'border-[#D97706]/20',
    badge: 'bg-[#FEF3C7] text-[#B45309]',
    text: 'text-[#92400E]',
  },
  critical: {
    border: 'border-[#B91C1C]/20',
    badge: 'bg-[#FEE2E2] text-[#B91C1C]',
    text: 'text-[#991B1B]',
  },
} as const;

function ErrorMessage({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="bg-[#FEE2E2] border border-[#B91C1C]/20 rounded-lg p-3 text-sm text-[#991B1B]">
      {message}
    </div>
  );
}

function NoticeMessage({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="bg-[#D1FAE5] border border-[#2E7D5E]/20 rounded-lg p-3 text-sm text-[#065F46]">
      {message}
    </div>
  );
}

function getNextConvertedAbsenceHint(label: '지각' | '조퇴', count: number) {
  if (count === 0) {
    return `${label} 3회 여유`;
  }

  const remainder = count % 3;
  if (remainder === 0) {
    return `이미 결석 환산 ${Math.floor(count / 3)}회 반영`;
  }

  return `${3 - remainder}회 더 누적되면 환산`;
}

function getLateLeaveWarning(lateCount: number, leaveEarlyCount: number) {
  const lateHint = getNextConvertedAbsenceHint('지각', lateCount);
  const leaveEarlyHint = getNextConvertedAbsenceHint('조퇴', leaveEarlyCount);

  return {
    value: `지각 ${lateCount}회 / 조퇴 ${leaveEarlyCount}회`,
    description: `지각: ${lateHint} · 조퇴: ${leaveEarlyHint}`,
  };
}

function formatTrainingDays(days: TrainingDayCode[]) {
  if (days.length === 0) {
    return '미설정';
  }

  return days
    .map(day => TRAINING_DAY_OPTIONS.find(option => option.value === day)?.label ?? day)
    .join(', ');
}

function getDefaultMonthlySummary(year: number, month: number): AttendanceMonthlySummary {
  return {
    yearMonth: `${year}-${String(month + 1).padStart(2, '0')}`,
    baseAllowanceAmount: 0,
    absenceDeductionAmount: 0,
    presentCount: 0,
    lateCount: 0,
    leaveEarlyCount: 0,
    absentCount: 0,
    convertedAbsenceCount: 0,
    deductionAmount: 0,
    expectedAllowanceAmount: 0,
  };
}

function getDefaultAllowanceSummary(year: number, month: number): AllowanceSummary {
  return {
    referenceYearMonth: `${year}-${String(month + 1).padStart(2, '0')}`,
    periodStartDate: '',
    periodEndDate: '',
    scheduledTrainingDays: 0,
    completedScheduledDays: 0,
    recordedTrainingDays: 0,
    unrecordedCompletedDays: 0,
    presentCount: 0,
    lateCount: 0,
    leaveEarlyCount: 0,
    absentCount: 0,
    convertedAbsenceCount: 0,
    recognizedAttendanceDays: 0,
    payableAttendanceDays: 0,
    dailyAllowanceAmount: 0,
    payableDayCap: 0,
    maximumAllowanceAmount: 0,
    expectedAllowanceAmount: 0,
  };
}

export default function CourseStatusPage() {
  const today = new Date();
  const currentYear = today.getFullYear();
  const currentMonth = today.getMonth();
  const {
    trainingProfile,
    getAttendanceSummaryForMonth,
    getAllowanceSummaryForMonth,
    getTrainingSummaryForMonth,
    loadAttendanceMonth,
    loadTrainingProfile,
    updateTrainingProfile,
    clearTrainingProfile,
  } = useApp();

  const [trainingProfileLoaded, setTrainingProfileLoaded] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [showClearTrainingConfirm, setShowClearTrainingConfirm] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [noticeMessage, setNoticeMessage] = useState<string | null>(null);

  const [courseLabel, setCourseLabel] = useState('');
  const [courseStartDate, setCourseStartDate] = useState('');
  const [courseEndDate, setCourseEndDate] = useState('');
  const [attendanceThresholdPercent, setAttendanceThresholdPercent] = useState('80');
  const [dailyAllowanceAmount, setDailyAllowanceAmount] = useState('15800');
  const [payableDayCap, setPayableDayCap] = useState('20');
  const [trainingDays, setTrainingDays] = useState<TrainingDayCode[]>(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']);
  const [holidayDatesInput, setHolidayDatesInput] = useState('');

  const monthlySummary = getAttendanceSummaryForMonth(currentYear, currentMonth) ?? getDefaultMonthlySummary(currentYear, currentMonth);
  const allowanceSummary = getAllowanceSummaryForMonth(currentYear, currentMonth) ?? getDefaultAllowanceSummary(currentYear, currentMonth);
  const trainingSummary = getTrainingSummaryForMonth(currentYear, currentMonth);
  const courseCountdown = trainingSummary ? formatCourseCountdown(trainingSummary.daysUntilCourseEnd) : '미설정';
  const thresholdValue = !trainingSummary
    ? '미설정'
    : trainingSummary.canReachThreshold
      ? `${trainingSummary.remainingAbsenceBudget}일`
      : '여유 없음';
  const thresholdDescription = !trainingSummary
    ? '개인 과정 정보를 입력하면 계산됩니다.'
    : trainingSummary.canReachThreshold
      ? `이 숫자 안에서는 전체 출석률 ${trainingSummary.thresholdPercent}%를 유지할 수 있어요.`
      : `남은 수업일을 모두 출석해도 전체 출석률 ${trainingSummary.thresholdPercent}%를 맞추기 어려운 상태입니다.`;
  const lateLeaveWarning = getLateLeaveWarning(monthlySummary.lateCount, monthlySummary.leaveEarlyCount);
  const riskItems = [
    ...buildTrainingRiskItems(trainingSummary),
    ...buildMonthlyRiskItems(monthlySummary),
  ];

  useEffect(() => {
    let active = true;

    Promise.allSettled([
      loadTrainingProfile(),
      loadAttendanceMonth(currentYear, currentMonth),
    ]).then(results => {
      if (!active) {
        return;
      }

      const rejected = results.find((result): result is PromiseRejectedResult => result.status === 'rejected');
      if (rejected) {
        const reason = rejected.reason;
        if (reason instanceof AppApiError) {
          setErrorMessage(reason.message);
        } else {
          setErrorMessage('과정 현황을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }
      }

      setTrainingProfileLoaded(true);
    });

    return () => {
      active = false;
    };
  }, [currentMonth, currentYear, loadAttendanceMonth, loadTrainingProfile]);

  useEffect(() => {
    if (!trainingProfile) {
      setCourseLabel('');
      setCourseStartDate('');
      setCourseEndDate('');
      setAttendanceThresholdPercent('80');
      setDailyAllowanceAmount('15800');
      setPayableDayCap('20');
      setTrainingDays(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']);
      setHolidayDatesInput('');
      return;
    }

    setCourseLabel(trainingProfile.courseLabel ?? '');
    setCourseStartDate(trainingProfile.courseStartDate);
    setCourseEndDate(trainingProfile.courseEndDate);
    setAttendanceThresholdPercent(String(trainingProfile.attendanceThresholdPercent));
    setDailyAllowanceAmount(String(trainingProfile.dailyAllowanceAmount));
    setPayableDayCap(String(trainingProfile.payableDayCap));
    setTrainingDays(trainingProfile.trainingDays);
    setHolidayDatesInput(trainingProfile.holidayDates.join('\n'));
  }, [trainingProfile]);

  const resetMessages = () => {
    setErrorMessage(null);
    setNoticeMessage(null);
  };

  const handleApiError = (error: unknown) => {
    if (error instanceof AppApiError) {
      setErrorMessage(error.message);
      return;
    }
    setErrorMessage('요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.');
  };

  const toggleTrainingDay = (day: TrainingDayCode) => {
    setTrainingDays(current =>
      current.includes(day)
        ? current.filter(value => value !== day)
        : [...current, day]
    );
  };

  const parseHolidayDates = (value: string) => {
    const tokens = value
      .split(/[\s,]+/)
      .map(token => token.trim())
      .filter(Boolean);

    const invalidToken = tokens.find(token => !/^\d{4}-\d{2}-\d{2}$/.test(token));
    if (invalidToken) {
      throw new AppApiError({
        message: '휴강일은 YYYY-MM-DD 형식으로 입력해 주세요.',
        status: 400,
        fieldErrors: { holidayDates: '휴강일은 YYYY-MM-DD 형식으로 입력해 주세요.' },
      });
    }

    return [...new Set(tokens)].sort();
  };

  const handleSaveTrainingProfile = async () => {
    resetMessages();
    try {
      await updateTrainingProfile({
        courseLabel: courseLabel.trim() || null,
        courseStartDate,
        courseEndDate,
        attendanceThresholdPercent: Number(attendanceThresholdPercent),
        dailyAllowanceAmount: Number(dailyAllowanceAmount),
        payableDayCap: Number(payableDayCap),
        trainingDays,
        holidayDates: parseHolidayDates(holidayDatesInput),
      });
      setNoticeMessage('내 과정 정보를 저장했습니다.');
      setIsEditing(false);
      setShowClearTrainingConfirm(false);
    } catch (error) {
      handleApiError(error);
    }
  };

  const handleClearTrainingProfile = async () => {
    resetMessages();
    try {
      await clearTrainingProfile();
      setNoticeMessage('개인 과정 정보를 초기화했습니다.');
      setIsEditing(false);
      setShowClearTrainingConfirm(false);
    } catch (error) {
      handleApiError(error);
    }
  };

  return (
    <div className="space-y-5 pb-4">
      <div className="pt-2">
        <h1 className="text-[26px] md:text-[30px] font-bold text-[#1E2A3A] leading-tight">과정 현황</h1>
        <p className="text-sm text-[#8A9BB0] mt-1">
          수료 일정과 출석 리스크, 현재 단위기간 장려금 흐름을 한눈에 확인하고 필요할 때만 내 과정 정보를 수정할 수 있습니다.
        </p>
      </div>

      <NoticeMessage message={noticeMessage} />
      <ErrorMessage message={errorMessage} />

      <section className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h2 className="text-[15px] font-semibold text-[#1E2A3A]">오늘 기준 요약</h2>
            <p className="text-[12px] text-[#8A9BB0] mt-1">
              대시보드에는 같은 항목의 짧은 요약만 보이고, 상세 내용은 여기에서 확인합니다.
            </p>
          </div>
          <div className="w-10 h-10 rounded-xl bg-[#F8E6D8] flex items-center justify-center shrink-0">
            <AlertTriangle className="w-5 h-5 text-[#C96B3B]" />
          </div>
        </div>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
            <p className="text-[11px] text-[#8A9BB0]">수료 D-day</p>
            <p className="text-lg font-semibold text-[#1E2A3A] mt-1">{courseCountdown}</p>
            <p className="text-[11px] text-[#8A9BB0] mt-1">
              {trainingSummary ? trainingSummary.courseEndDate : '과정 정보를 입력하면 표시됩니다.'}
            </p>
          </div>
          <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
            <p className="text-[11px] text-[#8A9BB0]">앞으로 더 빠져도 되는 날</p>
            <p className="text-lg font-semibold text-[#1E2A3A] mt-1">{thresholdValue}</p>
            <p className="text-[11px] text-[#8A9BB0] mt-1">{thresholdDescription}</p>
          </div>
          <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
            <p className="text-[11px] text-[#8A9BB0]">이번 달 결석 환산</p>
            <p className="text-lg font-semibold text-[#1E2A3A] mt-1">{monthlySummary.convertedAbsenceCount}회</p>
            <p className="text-[11px] text-[#8A9BB0] mt-1">
              {allowanceSummary.recordedTrainingDays > 0
                ? `예상 ${formatKRW(allowanceSummary.expectedAllowanceAmount)}`
                : '단위기간 기록을 기준으로 계산됩니다.'}
            </p>
          </div>
          <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
            <p className="text-[11px] text-[#8A9BB0]">지각/조퇴 경고</p>
            <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{lateLeaveWarning.value}</p>
            <p className="text-[11px] text-[#8A9BB0] mt-1 leading-relaxed">{lateLeaveWarning.description}</p>
          </div>
        </div>
      </section>

      {!trainingSummary && (
        <section className="rounded-xl border border-dashed border-[#C8CCC6] bg-[#FDFCFB] px-5 py-4">
          <div className="flex items-start gap-3">
            <ShieldAlert className="w-5 h-5 text-[#C96B3B] shrink-0 mt-0.5" />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-[#1E2A3A]">개인 과정 정보를 입력하면 과정 기준 리스크가 활성화됩니다.</p>
              <p className="text-[12px] text-[#6B7280] mt-1 leading-relaxed">
                 시작일, 종료일, 수업 요일, 휴강일을 저장하면 수료 D-day, 앞으로 더 빠져도 되는 날, 남은 수업일을 내 상황에 맞게 계산합니다.
              </p>
              <button
                type="button"
                onClick={() => { resetMessages(); setIsEditing(true); }}
                className="inline-flex items-center gap-1.5 text-sm text-[#3D7A8A] font-medium mt-2 hover:underline"
              >
                과정 정보 입력
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </section>
      )}

      <section className="grid gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <h2 className="text-[15px] font-semibold text-[#1E2A3A]">상세 현황</h2>
              <p className="text-[12px] text-[#8A9BB0] mt-1">
                과정 일정, 이번 달 출결, 현재 단위기간 장려금 흐름을 함께 참고한 보조 지표입니다.
              </p>
            </div>
            <Target className="w-5 h-5 text-[#3D7A8A] shrink-0" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="rounded-xl bg-[#E8F4F6] px-3.5 py-3.5">
              <p className="text-[11px] text-[#5C7B86]">현재까지 출석률</p>
              <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                {trainingSummary ? `${trainingSummary.attendanceRatePercent}%` : '미설정'}
              </p>
              <p className="text-[11px] text-[#5C7B86] mt-1">
                {trainingSummary
                  ? `지금까지 진행된 수업일 기준, 수료 기준 ${trainingSummary.thresholdPercent}%와 비교`
                  : '과정 정보를 저장하면 계산됩니다.'}
              </p>
            </div>
            <div className="rounded-xl bg-[#E8F4F6] px-3.5 py-3.5">
              <p className="text-[11px] text-[#5C7B86]">이번 달 수업일</p>
              <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                {trainingSummary
                  ? `${trainingSummary.monthCompletedDays}/${trainingSummary.monthScheduledDays}일`
                  : '미설정'}
              </p>
              <p className="text-[11px] text-[#5C7B86] mt-1">
                {trainingSummary
                  ? `남은 수업일 ${trainingSummary.monthRemainingDays}일`
                  : '월 요약만 계속 확인할 수 있습니다.'}
              </p>
            </div>
            <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
              <p className="text-[11px] text-[#8A9BB0]">미입력 수업일</p>
              <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                {trainingSummary ? `${trainingSummary.unrecordedCompletedDays}일` : '-'}
              </p>
              <p className="text-[11px] text-[#8A9BB0] mt-1">
                {trainingSummary
                  ? '이미 지난 수업일 중 아직 기록되지 않은 날짜'
                  : '개인 과정 기준 계산 전입니다.'}
              </p>
            </div>
            <div className="rounded-xl bg-[#F7F6F3] px-3.5 py-3.5">
              <p className="text-[11px] text-[#8A9BB0]">현재 단위기간 예상 장려금</p>
              <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                {formatKRW(allowanceSummary.expectedAllowanceAmount)}
              </p>
              <p className="text-[11px] text-[#8A9BB0] mt-1">
                지급 반영 {allowanceSummary.payableAttendanceDays}일 / 인정 출석 {allowanceSummary.recognizedAttendanceDays}일
              </p>
            </div>
          </div>

          <div className="space-y-2.5">
            {riskItems.map(item => {
              const toneStyle = RISK_TONE_STYLES[item.tone];
              return (
                <div
                  key={item.key}
                  className={cn('rounded-xl border bg-white px-3.5 py-3', toneStyle.border)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className={cn('text-sm font-semibold', toneStyle.text)}>{item.title}</p>
                      <p className="text-[12px] text-[#6B7280] mt-1 leading-relaxed">{item.description}</p>
                    </div>
                    <span className={cn('shrink-0 rounded-full px-2 py-1 text-[10px] font-semibold', toneStyle.badge)}>
                      {item.tone === 'good' ? '안정' : item.tone === 'warning' ? '주의' : '경고'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <div className="space-y-5">
          <section className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 className="text-[15px] font-semibold text-[#1E2A3A]">내 과정 정보</h2>
                <p className="text-[12px] text-[#8A9BB0] mt-1">
                  {isEditing
                    ? '이 카드 안에서 바로 수정하고 저장할 수 있습니다.'
                    : '현재 상태를 먼저 보고, 바꿀 때만 이 카드 안에서 수정합니다.'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => { resetMessages(); setIsEditing(current => !current); setShowClearTrainingConfirm(false); }}
                disabled={!trainingProfileLoaded}
                className="inline-flex shrink-0 items-center justify-center gap-1.5 whitespace-nowrap h-9 px-3 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm font-medium hover:bg-[#EFF0EC] transition-all disabled:opacity-60"
              >
                <PencilLine className="w-4 h-4" />
                {isEditing ? '취소' : trainingProfile?.configured ? '과정 정보 수정' : '과정 정보 입력'}
              </button>
            </div>

            {!trainingProfileLoaded ? (
              <div className="rounded-xl bg-[#F7F6F3] px-4 py-6 text-sm text-[#8A9BB0]">
                과정 정보를 불러오는 중입니다.
              </div>
            ) : isEditing ? (
              <div className="space-y-4">
                <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                  <p className="text-[12px] text-[#6B7280] leading-relaxed">
                    일정과 규칙을 바꾸면 과정 기준 리스크 계산이 즉시 다시 반영됩니다.
                  </p>
                </div>

                <div className="space-y-1.5">
                  <label className="text-sm font-medium text-[#1E2A3A]">과정 이름</label>
                  <input
                    type="text"
                    value={courseLabel}
                    onChange={event => setCourseLabel(event.target.value)}
                    className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                    placeholder="예: DevOps 국비 9기"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <label className="text-sm font-medium text-[#1E2A3A]">과정 시작일</label>
                    <input
                      type="date"
                      value={courseStartDate}
                      onChange={event => setCourseStartDate(event.target.value)}
                      className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-sm font-medium text-[#1E2A3A]">과정 종료일</label>
                    <input
                      type="date"
                      value={courseEndDate}
                      onChange={event => setCourseEndDate(event.target.value)}
                      className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-1.5">
                    <label className="text-sm font-medium text-[#1E2A3A]">수료 기준 출석률</label>
                    <input
                      type="number"
                      min="1"
                      max="100"
                      value={attendanceThresholdPercent}
                      onChange={event => setAttendanceThresholdPercent(event.target.value)}
                      className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-sm font-medium text-[#1E2A3A]">1일 지급액</label>
                    <input
                      type="number"
                      min="0"
                      value={dailyAllowanceAmount}
                      onChange={event => setDailyAllowanceAmount(event.target.value)}
                      className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-sm font-medium text-[#1E2A3A]">지급 상한 일수</label>
                  <input
                    type="number"
                    min="1"
                    value={payableDayCap}
                    onChange={event => setPayableDayCap(event.target.value)}
                    className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                  />
                  <p className="text-[12px] text-[#8A9BB0]">단위기간에서 실제 지급에 반영되는 최대 일수입니다. 기본값은 20일입니다.</p>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium text-[#1E2A3A]">수업 요일</label>
                  <div className="flex gap-2 flex-wrap">
                    {TRAINING_DAY_OPTIONS.map(option => {
                      const selected = trainingDays.includes(option.value);
                      return (
                        <button
                          key={option.value}
                          type="button"
                          onClick={() => toggleTrainingDay(option.value)}
                          className={`h-9 px-3 rounded-lg border text-sm font-medium transition-colors ${
                            selected
                              ? 'border-[#3D7A8A] bg-[#E8F4F6] text-[#3D7A8A]'
                              : 'border-[#E2E4DF] bg-white text-[#4A5568] hover:bg-[#F7F6F3]'
                          }`}
                        >
                          {option.label}
                        </button>
                      );
                    })}
                  </div>
                  <p className="text-[12px] text-[#8A9BB0]">실제 수업이 진행되는 요일만 선택해 주세요.</p>
                </div>

                <div className="space-y-1.5">
                  <label className="text-sm font-medium text-[#1E2A3A]">휴강일</label>
                  <textarea
                    value={holidayDatesInput}
                    onChange={event => setHolidayDatesInput(event.target.value)}
                    rows={4}
                    className="w-full px-3 py-2.5 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 resize-none"
                    placeholder={'YYYY-MM-DD\nYYYY-MM-DD'}
                  />
                  <p className="text-[12px] text-[#8A9BB0]">줄바꿈이나 쉼표로 여러 날짜를 입력할 수 있습니다.</p>
                </div>

                <div className="flex gap-2 flex-wrap">
                  <button
                    type="button"
                    onClick={handleSaveTrainingProfile}
                    disabled={!courseStartDate || !courseEndDate || trainingDays.length === 0}
                    className="inline-flex items-center justify-center gap-1.5 h-10 px-4 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-50"
                  >
                    <Save className="w-4 h-4" />
                    저장
                  </button>
                </div>

                {trainingProfile?.configured && (
                  <div className="pt-1">
                    {showClearTrainingConfirm ? (
                      <div className="rounded-lg border border-[#B91C1C]/20 bg-[#FEE2E2]/50 p-3 space-y-2">
                        <p className="text-sm text-[#991B1B]">개인 과정 정보를 초기화하시겠습니까?</p>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={handleClearTrainingProfile}
                            className="inline-flex items-center justify-center gap-1.5 h-9 px-3 rounded-lg bg-[#B91C1C] text-white text-sm font-medium hover:bg-[#991B1B] transition-all"
                          >
                            <RotateCcw className="w-4 h-4" />
                            초기화
                          </button>
                          <button
                            type="button"
                            onClick={() => setShowClearTrainingConfirm(false)}
                            className="h-9 px-3 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm hover:bg-[#EFF0EC] transition-all"
                          >
                            취소
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        type="button"
                        onClick={() => setShowClearTrainingConfirm(true)}
                        className="inline-flex items-center gap-1.5 text-sm text-[#B91C1C] hover:underline"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        개인 과정 정보 초기화
                      </button>
                    )}
                  </div>
                )}
              </div>
            ) : trainingProfile?.configured ? (
              <div className="space-y-3">
                <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                  <p className="text-[11px] text-[#8A9BB0]">과정명</p>
                  <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{trainingProfile.courseLabel || '내 과정'}</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                    <p className="text-[11px] text-[#8A9BB0]">과정 일정</p>
                    <p className="text-sm font-semibold text-[#1E2A3A] mt-1">
                      {trainingProfile.courseStartDate} ~ {trainingProfile.courseEndDate}
                    </p>
                  </div>
                  <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                    <p className="text-[11px] text-[#8A9BB0]">수료 기준</p>
                    <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{trainingProfile.attendanceThresholdPercent}%</p>
                  </div>
                  <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                    <p className="text-[11px] text-[#8A9BB0]">1일 지급액</p>
                    <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{formatKRW(trainingProfile.dailyAllowanceAmount)}</p>
                  </div>
                  <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                    <p className="text-[11px] text-[#8A9BB0]">지급 상한 일수</p>
                    <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{trainingProfile.payableDayCap}일</p>
                    <p className="text-[11px] text-[#8A9BB0] mt-1">최대 예상액 {formatKRW(trainingProfile.maximumAllowanceAmount)}</p>
                  </div>
                </div>
                <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                  <p className="text-[11px] text-[#8A9BB0]">수업 요일</p>
                  <p className="text-sm font-semibold text-[#1E2A3A] mt-1">{formatTrainingDays(trainingProfile.trainingDays)}</p>
                </div>
                <div className="rounded-xl bg-[#F7F6F3] px-4 py-3.5">
                  <p className="text-[11px] text-[#8A9BB0]">휴강일</p>
                  {trainingProfile.holidayDates.length > 0 ? (
                    <div className="flex flex-wrap gap-2 mt-2">
                      {trainingProfile.holidayDates.map(date => (
                        <span
                          key={date}
                          className="inline-flex items-center px-2.5 py-1 rounded-full bg-white border border-[#E2E4DF] text-[11px] text-[#4A5568]"
                        >
                          {date}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm font-semibold text-[#1E2A3A] mt-1">등록된 휴강일 없음</p>
                  )}
                </div>
              </div>
            ) : (
              <div className="rounded-xl border border-dashed border-[#C8CCC6] bg-[#F7F6F3] px-4 py-4">
                <p className="text-sm font-semibold text-[#1E2A3A]">아직 저장된 과정 정보가 없습니다.</p>
                <p className="text-[12px] text-[#6B7280] mt-1 leading-relaxed">
                  과정명을 꼭 입력하지 않아도 되지만, 일정과 수업 요일이 있어야 과정 기준 계산이 가능합니다.
                </p>
              </div>
            )}
          </section>
        </div>
      </section>

      {trainingSummary && (
        <section className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5">
          <div className="flex items-center gap-2 text-[#8A9BB0] text-[12px]">
            <Clock3 className="w-4 h-4" />
            <span>
              {trainingSummary.courseLabel || '내 과정'} 기준 참고치이며, 장려금은 현재 단위기간의 저장된 출결과 `1일 지급액 x 지급 반영 일수` 규칙으로 계산합니다.
            </span>
          </div>
        </section>
      )}
    </div>
  );
}
