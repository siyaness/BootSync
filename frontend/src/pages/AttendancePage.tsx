import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { AppApiError, apiRequest } from '@/lib/api';
import { useApp } from '@/lib/store';
import { useIsMobile } from '@/hooks/use-mobile';
import { cn } from '@/lib/utils';
import type { AttendanceBulkFillResult, AttendanceStatus, TrainingDayCode, TrainingProfileSettings } from '@/lib/app-types';
import { useSearchParams } from 'react-router-dom';
import {
  ChevronLeft, ChevronRight, CheckCircle2, Clock, MinusCircle, XCircle,
  Save, Trash2, X, Loader2,
} from 'lucide-react';

type StatusOption = {
  value: AttendanceStatus;
  label: string;
  icon: typeof CheckCircle2;
  color: string;
  bg: string;
  markerClassName: string;
};

const STATUS_OPTIONS: StatusOption[] = [
  { value: '출석', label: '출석', icon: CheckCircle2, color: '#2E7D5E', bg: '#D1FAE5', markerClassName: 'rounded-full' },
  { value: '지각', label: '지각', icon: Clock, color: '#D97706', bg: '#FEF3C7', markerClassName: 'rounded-full ring-1 ring-[#D97706]/20' },
  { value: '조퇴', label: '조퇴', icon: MinusCircle, color: '#C2410C', bg: '#FFEDD5', markerClassName: 'rounded-[3px]' },
  { value: '결석', label: '결석', icon: XCircle, color: '#B91C1C', bg: '#FEE2E2', markerClassName: 'rounded-[2px] rotate-45' },
];

const DAY_NAMES = ['일', '월', '화', '수', '목', '금', '토'];

type CalendarDay = {
  date: number;
  dateStr: string;
  isCurrentMonth: boolean;
  isToday: boolean;
  isFuture: boolean;
  isWeekend: boolean;
};

const TRAINING_DAY_BY_INDEX: TrainingDayCode[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

function formatDateString(year: number, month: number, date: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
}

function formatYearMonth(year: number, month: number) {
  return `${year}-${String(month + 1).padStart(2, '0')}`;
}

function parseYearMonthParam(yearMonth: string | null, fallbackYear: number, fallbackMonth: number) {
  if (!yearMonth) {
    return { year: fallbackYear, month: fallbackMonth };
  }

  const match = /^(\d{4})-(\d{2})$/.exec(yearMonth.trim());
  if (!match) {
    return { year: fallbackYear, month: fallbackMonth };
  }

  const parsedYear = Number(match[1]);
  const parsedMonth = Number(match[2]) - 1;
  if (parsedMonth < 0 || parsedMonth > 11) {
    return { year: fallbackYear, month: fallbackMonth };
  }

  if (parsedYear > fallbackYear || (parsedYear === fallbackYear && parsedMonth > fallbackMonth)) {
    return { year: fallbackYear, month: fallbackMonth };
  }

  return { year: parsedYear, month: parsedMonth };
}

function readDateParts(dateStr: string) {
  const [year, month, date] = dateStr.split('-').map(Number);
  return {
    year,
    month,
    date,
    dayOfWeek: new Date(year, month - 1, date).getDay(),
  };
}

function formatSelectedDate(dateStr: string) {
  const { month, date, dayOfWeek } = readDateParts(dateStr);
  return `${month}월 ${date}일 ${DAY_NAMES[dayOfWeek]}요일`;
}

function formatPercent(value: number) {
  if (value >= 100) {
    return '100%';
  }
  return `${value.toFixed(1)}%`;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (error instanceof AppApiError) {
    return error.message;
  }
  return fallback;
}

function isDateWithinCourse(dateStr: string, trainingProfile: TrainingProfileSettings) {
  return dateStr >= trainingProfile.courseStartDate && dateStr <= trainingProfile.courseEndDate;
}

function isConfiguredTrainingDay(dateStr: string, trainingProfile: TrainingProfileSettings | null) {
  if (!trainingProfile?.configured) {
    return true;
  }

  if (!isDateWithinCourse(dateStr, trainingProfile)) {
    return false;
  }

  if (trainingProfile.holidayDates.includes(dateStr)) {
    return false;
  }

  const { dayOfWeek } = readDateParts(dateStr);
  return trainingProfile.trainingDays.includes(TRAINING_DAY_BY_INDEX[dayOfWeek]);
}

function getTrainingDayNotice(dateStr: string, trainingProfile: TrainingProfileSettings | null) {
  if (!trainingProfile?.configured) {
    return null;
  }

  if (!isDateWithinCourse(dateStr, trainingProfile)) {
    return '과정 기간 밖 날짜입니다.';
  }

  if (trainingProfile.holidayDates.includes(dateStr)) {
    return '휴강일로 설정된 날짜입니다.';
  }

  const { dayOfWeek } = readDateParts(dateStr);
  if (!trainingProfile.trainingDays.includes(TRAINING_DAY_BY_INDEX[dayOfWeek])) {
    return '내 과정 정보 기준 수업일이 아닙니다.';
  }

  return null;
}

function renderStatusMarker(option: StatusOption, sizeClassName = 'w-2 h-2') {
  return (
    <span
      className={cn('block shrink-0', sizeClassName, option.markerClassName)}
      style={{ backgroundColor: option.color }}
    />
  );
}

export default function AttendancePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const {
    trainingProfile,
    setAttendance,
    deleteAttendance,
    previewBulkFillPresentForTrainingDays,
    bulkFillPresentForTrainingDays,
    getAttendanceForDate,
    getAttendanceSummaryForMonth,
    getTrainingSummaryForMonth,
    loadTrainingProfile,
    loadAttendanceMonth,
  } = useApp();
  const isMobile = useIsMobile();

  const today = new Date();
  const todayYear = today.getFullYear();
  const todayMonth = today.getMonth();
  const todayDate = today.getDate();
  const initialMonthView = parseYearMonthParam(searchParams.get('yearMonth'), todayYear, todayMonth);
  const initialEditId = searchParams.get('editId')?.trim() || null;

  const [currentYear, setCurrentYear] = useState(initialMonthView.year);
  const [currentMonth, setCurrentMonth] = useState(initialMonthView.month);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<AttendanceStatus | null>(null);
  const [memo, setMemo] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [savingDate, setSavingDate] = useState<string | null>(null);
  const [savingStatus, setSavingStatus] = useState<AttendanceStatus | null>(null);
  const [memoSaving, setMemoSaving] = useState(false);
  const [deletingDate, setDeletingDate] = useState<string | null>(null);
  const [panelMessage, setPanelMessage] = useState<string | null>(null);
  const [panelError, setPanelError] = useState<string | null>(null);
  const [activeEditId, setActiveEditId] = useState<string | null>(initialEditId);
  const [bulkFillMessage, setBulkFillMessage] = useState<string | null>(null);
  const [bulkFillError, setBulkFillError] = useState<string | null>(null);
  const [bulkFillLoading, setBulkFillLoading] = useState(false);
  const [showBulkFillConfirm, setShowBulkFillConfirm] = useState(false);
  const [bulkFillPreviewLoading, setBulkFillPreviewLoading] = useState(false);
  const [bulkFillPreview, setBulkFillPreview] = useState<AttendanceBulkFillResult | null>(null);
  const detailPanelRef = useRef<HTMLDivElement | null>(null);

  const isCurrentOrFutureMonth = currentYear > todayYear || (currentYear === todayYear && currentMonth >= todayMonth);
  const desktopTwoColumnLayoutClass = !isMobile ? 'grid-cols-[minmax(0,1fr)_340px]' : '';
  const desktopContentWidthClass = !isMobile ? 'max-w-[960px]' : '';
  const desktopRowAlignmentClass = !isMobile ? 'items-start' : '';
  const desktopCalendarCardClass = !isMobile ? 'self-start' : '';
  const desktopDetailPanelClass = !isMobile ? 'self-start min-h-[620px]' : '';
  const currentYearMonth = formatYearMonth(currentYear, currentMonth);
  const defaultYearMonth = formatYearMonth(todayYear, todayMonth);

  const calendarDays = useMemo(() => {
    const firstDay = new Date(currentYear, currentMonth, 1);
    const lastDay = new Date(currentYear, currentMonth + 1, 0);
    const startPad = firstDay.getDay();
    const totalCalendarCells = 42;
    const days: CalendarDay[] = [];

    for (let index = 0; index < startPad; index++) {
      days.push({ date: 0, dateStr: '', isCurrentMonth: false, isToday: false, isFuture: false, isWeekend: false });
    }

    for (let date = 1; date <= lastDay.getDate(); date++) {
      const currentDate = new Date(currentYear, currentMonth, date);
      days.push({
        date,
        dateStr: formatDateString(currentYear, currentMonth, date),
        isCurrentMonth: true,
        isToday: currentYear === todayYear && currentMonth === todayMonth && date === todayDate,
        isFuture: currentDate > today,
        isWeekend: currentDate.getDay() === 0 || currentDate.getDay() === 6,
      });
    }

    while (days.length < totalCalendarCells) {
      days.push({ date: 0, dateStr: '', isCurrentMonth: false, isToday: false, isFuture: false, isWeekend: false });
    }

    return days;
  }, [currentMonth, currentYear, today, todayDate, todayMonth, todayYear]);

  const monthSummary = getAttendanceSummaryForMonth(currentYear, currentMonth);
  const summary = monthSummary ?? {
    yearMonth: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}`,
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
  const trainingSummary = getTrainingSummaryForMonth(currentYear, currentMonth);
  const overallSummaryLabel = trainingSummary?.courseLabel || trainingProfile?.courseLabel || '과정 기준';
  const isMonthlySummaryLoaded = Boolean(monthSummary);

  const selectedRecord = selectedDate ? getAttendanceForDate(selectedDate) : undefined;
  const selectedDateLabel = selectedDate ? formatSelectedDate(selectedDate) : '';
  const panelFeedback = panelError ?? panelMessage;
  const officialAttendanceRate = trainingSummary
    ? trainingSummary.attendanceRatePercent
    : null;

  const openDateInPanel = (dateStr: string) => {
    setSelectedDate(dateStr);
    setActiveEditId(null);
    setPanelMessage(null);
    setPanelError(null);
    if (!isMobile) {
      return;
    }

    window.requestAnimationFrame(() => {
      detailPanelRef.current?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    });
  };

  useEffect(() => {
    loadAttendanceMonth(currentYear, currentMonth).catch(() => {});
  }, [currentMonth, currentYear, loadAttendanceMonth]);

  useEffect(() => {
    loadTrainingProfile().catch(() => {});
  }, [loadTrainingProfile]);

  useLayoutEffect(() => {
    const nextParams = new URLSearchParams(searchParams);
    if (currentYearMonth === defaultYearMonth) {
      nextParams.delete('yearMonth');
    } else {
      nextParams.set('yearMonth', currentYearMonth);
    }

    if (activeEditId) {
      nextParams.set('editId', activeEditId);
    } else {
      nextParams.delete('editId');
    }

    if (nextParams.toString() !== searchParams.toString()) {
      setSearchParams(nextParams, { replace: true });
    }
  }, [activeEditId, currentYearMonth, defaultYearMonth, searchParams, setSearchParams]);

  useEffect(() => {
    if (!activeEditId) {
      return;
    }

    let cancelled = false;

    const hydrateLegacyEditSelection = async () => {
      try {
        const response = await apiRequest<{ id: number; date: string; status: AttendanceStatus; memo?: string | null }>(
          `/api/attendance/record/${activeEditId}`
        );
        if (!response || cancelled) {
          return;
        }

        const { year, month } = readDateParts(response.date);
        setCurrentYear(year);
        setCurrentMonth(month - 1);
        setSelectedDate(response.date);
        setPanelMessage(null);
        setPanelError(null);
      } catch {
        if (!cancelled) {
          setActiveEditId(null);
        }
      }
    };

    void hydrateLegacyEditSelection();

    return () => {
      cancelled = true;
    };
  }, [activeEditId]);

  useEffect(() => {
    if (!selectedDate) {
      setSelectedStatus(null);
      setMemo('');
      setShowDeleteConfirm(false);
      setPanelError(null);
      return;
    }

    const record = getAttendanceForDate(selectedDate);
    setSelectedStatus(record?.status ?? null);
    setMemo(record?.memo || '');
    setShowDeleteConfirm(false);
    setPanelError(null);
  }, [getAttendanceForDate, selectedDate]);

  const navigateMonth = (delta: number) => {
    let nextMonth = currentMonth + delta;
    let nextYear = currentYear;

    if (nextMonth < 0) {
      nextMonth = 11;
      nextYear--;
    }
    if (nextMonth > 11) {
      nextMonth = 0;
      nextYear++;
    }

    if (nextYear > todayYear || (nextYear === todayYear && nextMonth > todayMonth)) {
      return;
    }

    setCurrentYear(nextYear);
    setCurrentMonth(nextMonth);
    setSelectedDate(null);
    setActiveEditId(null);
    setPanelMessage(null);
    setPanelError(null);
  };

  const handleDayClick = (day: CalendarDay) => {
    const record = getAttendanceForDate(day.dateStr);
    const canSelect = !day.isFuture && (isConfiguredTrainingDay(day.dateStr, trainingProfile) || Boolean(record));

    if (!day.isCurrentMonth || !canSelect) {
      return;
    }

    openDateInPanel(day.dateStr);
  };

  const saveAttendanceStatus = async (
    date: string,
    status: AttendanceStatus,
    memoValue: string,
    successMessage?: string
  ) => {
    const existingRecord = getAttendanceForDate(date);
    const normalizedMemo = memoValue.trim();

    if (existingRecord?.status === status && (existingRecord.memo || '') === normalizedMemo) {
      if (successMessage) {
        setPanelMessage('이미 같은 내용으로 저장되어 있습니다.');
      }
      return;
    }

    setPanelError(null);
    setPanelMessage(null);

    setSavingDate(date);
    setSavingStatus(status);

    try {
      await setAttendance(date, status, normalizedMemo || undefined);
      if (successMessage) {
        setPanelMessage(successMessage);
      }
    } catch (error) {
      const message = getErrorMessage(error, '출결을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.');
      setPanelError(message);
    } finally {
      setSavingDate(null);
      setSavingStatus(null);
    }
  };

  const handleSelectedStatusClick = async (status: AttendanceStatus) => {
    if (!selectedDate) {
      return;
    }

    const normalizedMemo = memo.trim();
    const memoChanged = (selectedRecord?.memo || '') !== normalizedMemo;

    setSelectedStatus(status);
    await saveAttendanceStatus(
      selectedDate,
      status,
      memo,
      memoChanged
        ? `${formatSelectedDate(selectedDate)}을 ${status}으로 저장했고 메모도 함께 저장했습니다.`
        : `${formatSelectedDate(selectedDate)}을 ${status}으로 저장했습니다.`
    );
  };

  const handleMemoSave = async () => {
    if (!selectedDate) {
      return;
    }
    if (!selectedStatus) {
      setPanelError('먼저 출결 상태를 선택해 주세요.');
      return;
    }

    setMemoSaving(true);
    try {
      await saveAttendanceStatus(selectedDate, selectedStatus, memo, '메모를 저장했습니다.');
    } finally {
      setMemoSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedDate) {
      return;
    }

    setPanelError(null);
    setPanelMessage(null);
    setDeletingDate(selectedDate);

    try {
      await deleteAttendance(selectedDate);
      setSelectedStatus(null);
      setMemo('');
      setShowDeleteConfirm(false);
      setActiveEditId(null);
      setPanelMessage(`${selectedDateLabel} 기록을 삭제했습니다.`);
    } catch (error) {
      setPanelError(getErrorMessage(error, '출결을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.'));
    } finally {
      setDeletingDate(null);
    }
  };

  const openBulkFillConfirm = async () => {
    if (!trainingProfile?.configured) {
      setBulkFillError('과정 현황에서 내 과정 정보를 먼저 저장해 주세요.');
      setBulkFillMessage(null);
      return;
    }

    setBulkFillPreviewLoading(true);
    setBulkFillError(null);
    setBulkFillMessage(null);
    setShowBulkFillConfirm(false);

    try {
      const preview = await previewBulkFillPresentForTrainingDays();
      if (preview.createdCount === 0) {
        setBulkFillPreview(null);
        setBulkFillMessage('채울 빈 수업일이 없어 기존 기록을 그대로 유지했습니다.');
        return;
      }
      setBulkFillPreview(preview);
      setShowBulkFillConfirm(true);
    } catch (error) {
      setBulkFillPreview(null);
      setBulkFillError(getErrorMessage(error, '빈 수업일 일괄 출석 미리보기를 불러오지 못했습니다.'));
    } finally {
      setBulkFillPreviewLoading(false);
    }
  };

  const handleBulkFillPresent = async () => {
    if (!trainingProfile?.configured) {
      setBulkFillError('과정 현황에서 내 과정 정보를 먼저 저장해 주세요.');
      setBulkFillMessage(null);
      return;
    }

    setBulkFillLoading(true);
    setBulkFillError(null);
    setBulkFillMessage(null);

    try {
      const result = await bulkFillPresentForTrainingDays();
      await Promise.all([
        loadAttendanceMonth(currentYear, currentMonth),
        currentYear === todayYear && currentMonth === todayMonth
          ? Promise.resolve()
          : loadAttendanceMonth(todayYear, todayMonth),
      ]);

      if (result.createdCount === 0) {
        setBulkFillMessage('채울 빈 수업일이 없어 기존 기록을 그대로 유지했습니다.');
      } else {
        setBulkFillMessage(
          `${result.startDate}부터 ${result.endDate}까지 빈 수업일 ${result.createdCount}일을 출석으로 채웠습니다.`
        );
      }
      setShowBulkFillConfirm(false);
      setBulkFillPreview(null);
    } catch (error) {
      setBulkFillError(getErrorMessage(error, '빈 수업일 일괄 출석 처리를 완료하지 못했습니다.'));
    } finally {
      setBulkFillLoading(false);
    }
  };

  const getStatusDot = (dateStr: string) => {
    const record = getAttendanceForDate(dateStr);
    if (!record) {
      return null;
    }

    const config = STATUS_OPTIONS.find(option => option.value === record.status);
    if (!config) {
      return null;
    }

    return <div className="mx-auto mt-0.5">{renderStatusMarker(config, 'w-1.5 h-1.5')}</div>;
  };

  const renderStatusButtons = (
    targetDate: string,
    currentStatus: AttendanceStatus | null,
    onSelect: (status: AttendanceStatus) => Promise<void> | void,
    variant: 'today' | 'panel'
  ) => (
    <div className={cn(
      'grid gap-2',
      variant === 'today'
        ? (isMobile ? 'grid-cols-2' : 'grid-cols-4')
        : 'grid-cols-2'
    )}>
      {STATUS_OPTIONS.map(option => {
        const Icon = option.icon;
        const isActive = currentStatus === option.value;
        const isSaving = savingDate === targetDate && savingStatus === option.value;

        return (
          <button
            key={`${targetDate}-${option.value}`}
            onClick={() => { void onSelect(option.value); }}
            disabled={isSaving || deletingDate === targetDate}
            className={cn(
              'rounded-xl border-2 text-left transition-[background-color,border-color,box-shadow,color] disabled:opacity-60 disabled:cursor-not-allowed',
              variant === 'today' ? 'px-4 py-3.5' : 'px-3.5 py-3',
              isActive ? 'shadow-[0_4px_12px_rgba(30,42,58,0.08)]' : 'hover:shadow-[0_4px_12px_rgba(30,42,58,0.05)]'
            )}
            style={{
              borderColor: isActive ? option.color : '#E2E4DF',
              backgroundColor: isActive ? option.bg : '#FDFCFB',
              color: isActive ? option.color : '#1E2A3A',
            }}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2.5">
                <div
                  className="w-9 h-9 rounded-lg flex items-center justify-center"
                  style={{ backgroundColor: isActive ? 'rgba(255,255,255,0.55)' : option.bg }}
                >
                  <Icon className="w-4.5 h-4.5" style={{ color: option.color }} />
                </div>
                <div>
                  <p className="text-sm font-semibold">{option.label}</p>
                  <p className="text-[11px] opacity-75 min-h-[16px]">
                    {isSaving ? '저장 중...' : '클릭 즉시 저장'}
                  </p>
                </div>
              </div>
              {isSaving && <Loader2 className="w-4 h-4 animate-spin shrink-0 mt-0.5" />}
            </div>
          </button>
        );
      })}
    </div>
  );

  const renderSummaryTiles = (
    items: Array<{ key: string; label: string; value: number | string; color: string; bg: string }>
  ) => (
    <div className="grid grid-cols-4 gap-1.5">
      {items.map(item => (
        <div key={item.key} className="text-center px-2 py-2.5 rounded-lg" style={{ backgroundColor: item.bg }}>
          <p className="text-[17px] font-bold leading-none tabular-nums" style={{ color: item.color }}>{item.value}</p>
          <p className="text-[10px] font-medium mt-1" style={{ color: item.color }}>{item.label}</p>
        </div>
      ))}
    </div>
  );

  const summaryStrip = (
    <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-3.5">
      <div className="space-y-3">
        <div>
          <div className="flex items-center justify-between gap-3 mb-2.5">
            <h3 className="text-[14px] font-semibold text-[#1E2A3A]">전체 누적</h3>
            <span className="text-[11px] text-[#8A9BB0]">
              {overallSummaryLabel}
            </span>
          </div>
          {trainingSummary ? (
            <div className="space-y-2.5 min-h-[154px]">
              {renderSummaryTiles([
                { key: 'overall-completed', label: '수업일', value: trainingSummary.courseCompletedDays, color: '#3D7A8A', bg: '#E8F4F6' },
                { key: 'overall-effective-present', label: '출석', value: trainingSummary.effectivePresentDays, color: '#2E7D5E', bg: '#D1FAE5' },
                { key: 'overall-effective-absence', label: '결석', value: trainingSummary.effectiveAbsenceCount, color: '#B91C1C', bg: '#FEE2E2' },
                {
                  key: 'overall-rate',
                  label: '출석률',
                  value: officialAttendanceRate === null ? '-' : formatPercent(officialAttendanceRate),
                  color: '#3D7A8A',
                  bg: '#E8F4F6',
                },
              ])}
              <div className="rounded-lg bg-[#F7F6F3] border border-[#E2E4DF] px-3 py-2 text-[11px] text-[#8A9BB0] leading-[1.45] space-y-1">
                <p>수업일은 지금까지 실제로 진행된 전체 수업일입니다.</p>
                <p>입력한 기록: 출석 {trainingSummary.presentCount} · 지각 {trainingSummary.lateCount} · 조퇴 {trainingSummary.leaveEarlyCount} · 결석 {trainingSummary.absentCount}</p>
                <p>지각/조퇴 3회는 결석 1일로 계산합니다.</p>
                {trainingSummary.unrecordedCompletedDays > 0 && (
                  <p>아직 입력하지 않은 수업일 {trainingSummary.unrecordedCompletedDays}일은 공식 출석일 계산에서 제외됩니다.</p>
                )}
              </div>
            </div>
          ) : trainingProfile?.configured ? (
            <div className="space-y-2.5 min-h-[154px]">
              {renderSummaryTiles([
                { key: 'overall-loading-completed', label: '수업일', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                { key: 'overall-loading-present', label: '출석', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                { key: 'overall-loading-absence', label: '결석', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                { key: 'overall-loading-rate', label: '출석률', value: '-', color: '#4A5568', bg: '#EFF0EC' },
              ])}
              <div className="rounded-lg bg-[#F7F6F3] border border-[#E2E4DF] px-3 py-2 text-[11px] text-[#8A9BB0] leading-[1.45] min-h-[72px] flex flex-col justify-center">
                <p>전체 누적 출결을 불러오는 중입니다.</p>
                <p>잠시 후 이번 달 기록과 함께 표시됩니다.</p>
              </div>
            </div>
          ) : (
            <div className="rounded-lg bg-[#F7F6F3] border border-[#E2E4DF] px-3 py-2 text-[11px] text-[#8A9BB0] leading-[1.45]">
              과정 현황에서 내 과정 정보를 저장하면 전체 누적 출결도 함께 보여 줍니다.
            </div>
          )}
        </div>

        <div className="pt-3 border-t border-[#E2E4DF] min-h-[112px]">
          <div className="flex items-center justify-between gap-3 mb-2.5">
            <h3 className="text-[14px] font-semibold text-[#1E2A3A]">{currentMonth + 1}월 요약</h3>
            <span className="text-[11px] text-[#8A9BB0]">이번 달 누적</span>
          </div>
          {renderSummaryTiles(
            isMonthlySummaryLoaded
              ? [
                  { key: 'month-present', label: '출석', value: summary.presentCount, color: '#2E7D5E', bg: '#D1FAE5' },
                  { key: 'month-late', label: '지각', value: summary.lateCount, color: '#D97706', bg: '#FEF3C7' },
                  { key: 'month-leave', label: '조퇴', value: summary.leaveEarlyCount, color: '#D97706', bg: '#FEF3C7' },
                  { key: 'month-absent', label: '결석', value: summary.absentCount, color: '#B91C1C', bg: '#FEE2E2' },
                ]
              : [
                  { key: 'month-loading-present', label: '출석', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                  { key: 'month-loading-late', label: '지각', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                  { key: 'month-loading-leave', label: '조퇴', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                  { key: 'month-loading-absent', label: '결석', value: '-', color: '#4A5568', bg: '#EFF0EC' },
                ]
          )}
        </div>
      </div>
    </div>
  );

  const bulkFillToolbar = (
    <div className="rounded-xl bg-[#F7F6F3] border border-[#E2E4DF] px-4 py-3 mb-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-[#1E2A3A]">과거 기록 빠르게 채우기</p>
          <p className="text-[12px] text-[#8A9BB0] mt-1 leading-[1.45]">
            과정 시작일부터 오늘까지 빈 수업일만 출석으로 자동 채웁니다. 이후 예외 날짜만 수정하면 됩니다.
          </p>
        </div>
        <button
          type="button"
          onClick={() => { void openBulkFillConfirm(); }}
          disabled={bulkFillLoading || bulkFillPreviewLoading || !trainingProfile?.configured}
          className="inline-flex shrink-0 items-center justify-center h-9 px-3.5 rounded-lg border border-[#3D7A8A] text-[#3D7A8A] text-sm font-medium whitespace-nowrap hover:bg-[#E8F4F6] active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {bulkFillLoading ? '채우는 중...' : bulkFillPreviewLoading ? '미리보는 중...' : '빈 수업일 일괄 출석'}
        </button>
      </div>
      <div
        className={cn(
          'rounded-lg border px-3 py-2.5 text-[12px] leading-[1.45] mt-3',
          bulkFillError
            ? 'bg-[#FEE2E2] border-[#B91C1C]/20 text-[#991B1B]'
            : bulkFillMessage
              ? 'bg-[#D1FAE5] border-[#2E7D5E]/20 text-[#065F46]'
              : 'bg-white border-[#E2E4DF] text-[#8A9BB0]'
        )}
      >
        {bulkFillError
          ?? bulkFillMessage
          ?? (trainingProfile?.configured
            ? '이미 기록이 있는 날짜는 건드리지 않고, 비수업일/휴강일/미래 날짜는 자동으로 제외합니다. 실행 전 한 번 더 확인합니다.'
            : '과정 현황에서 수업 요일과 휴강일을 저장하면 이 기능을 사용할 수 있습니다.')}
      </div>
    </div>
  );

  const bulkFillConfirmDialog = showBulkFillConfirm && bulkFillPreview ? (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#1E2A3A]/35 px-4">
      <div className="w-full max-w-md rounded-2xl border border-[#E2E4DF] bg-[#FDFCFB] p-5 shadow-[0_18px_48px_rgba(30,42,58,0.18)]">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-[12px] font-semibold uppercase tracking-wider text-[#8A9BB0]">일괄 출석 확인</p>
            <h3 className="text-[20px] font-bold text-[#1E2A3A] mt-1">빈 수업일을 한 번에 채울까요?</h3>
          </div>
          <button
            type="button"
            onClick={() => {
              setShowBulkFillConfirm(false);
              setBulkFillPreview(null);
            }}
            className="text-[#8A9BB0] hover:text-[#4A5568] transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <p className="text-sm text-[#4A5568] leading-[1.55] mt-3">
          {bulkFillPreview.startDate}부터 {bulkFillPreview.endDate}까지 비어 있는 수업일
          <span className="font-semibold text-[#1E2A3A]"> {bulkFillPreview.createdCount}일</span>을 `출석`으로 채웁니다.
        </p>

        <div className="rounded-xl bg-[#F7F6F3] border border-[#E2E4DF] px-4 py-3 mt-4 space-y-2 text-[12px] text-[#4A5568] leading-[1.5]">
          <p>기존에 기록한 날짜는 그대로 둡니다.</p>
          <p>휴강일, 비수업일, 미래 날짜는 자동으로 제외합니다.</p>
          <p>실행 뒤에는 예외 날짜만 오른쪽 패널에서 수정하면 됩니다.</p>
        </div>

        <div className="flex items-center justify-end gap-2 mt-5">
          <button
            type="button"
            onClick={() => {
              setShowBulkFillConfirm(false);
              setBulkFillPreview(null);
            }}
            className="h-10 px-4 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm font-medium hover:bg-[#EFF0EC] transition-colors"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => { void handleBulkFillPresent(); }}
            disabled={bulkFillLoading}
            className="inline-flex items-center justify-center gap-2 h-10 px-4 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {bulkFillLoading && <Loader2 className="w-4 h-4 animate-spin" />}
            {bulkFillLoading ? '채우는 중...' : '확인하고 채우기'}
          </button>
        </div>
      </div>
    </div>
  ) : null;

  const detailPanel = selectedDate ? (
    <div
      ref={detailPanelRef}
      tabIndex={-1}
      className={cn(
      'bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5',
      desktopDetailPanelClass
      )}
    >
      <div className="flex items-center justify-between gap-3 mb-4">
        <div>
          <p className="text-[12px] font-semibold uppercase tracking-wider text-[#8A9BB0]">선택한 날짜</p>
          <h3 className="text-[20px] font-bold text-[#1E2A3A] mt-1">{formatSelectedDate(selectedDate)}</h3>
        </div>
        <button
          onClick={() => {
            setSelectedDate(null);
            setActiveEditId(null);
            setPanelMessage(null);
            setPanelError(null);
          }}
          className="text-[#8A9BB0] hover:text-[#4A5568] transition-colors"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      <div className="rounded-lg bg-[#F7F6F3] border border-[#E2E4DF] px-4 py-3 mb-4">
        <div className="flex items-center justify-between gap-3 min-h-[36px]">
          <p className="text-sm font-medium text-[#1E2A3A]">
            현재 상태: {selectedRecord?.status ?? '미저장'}
          </p>
          <div className="w-[116px] min-h-[36px] flex items-start justify-end">
            {selectedRecord ? (
              showDeleteConfirm ? (
                <div className="w-full flex flex-col items-end gap-2">
                  <p className="text-[11px] font-medium text-[#B91C1C] text-right">
                    이 기록을 삭제할까요?
                  </p>
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => { void handleDelete(); }}
                      disabled={deletingDate === selectedDate}
                      className="h-8 px-3 rounded-lg bg-[#B91C1C] text-white text-[12px] font-medium hover:bg-[#991B1B] transition-colors disabled:opacity-60 flex items-center gap-1.5"
                    >
                      {deletingDate === selectedDate && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                      삭제
                    </button>
                    <button
                      onClick={() => setShowDeleteConfirm(false)}
                      className="h-8 px-3 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-[12px] hover:bg-[#EFF0EC] transition-colors"
                    >
                      취소
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => setShowDeleteConfirm(true)}
                  className="inline-flex items-center justify-center gap-1.5 h-9 px-3 rounded-lg border border-[#B91C1C]/30 text-[#B91C1C] text-[12px] font-medium hover:bg-[#FEE2E2] transition-colors shrink-0"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  기록 삭제
                </button>
              )
            ) : (
              <span className="inline-flex items-center justify-center h-9 min-w-[74px] px-2.5 rounded-full text-[11px] font-semibold bg-[#EFF0EC] text-[#4A5568] shrink-0">
                미저장
              </span>
            )}
          </div>
        </div>
        <p className="text-[12px] text-[#8A9BB0] leading-[1.45] mt-2">
          상태 버튼으로 바로 저장할 수 있고, 메모를 먼저 적어두면 함께 저장됩니다.
        </p>
      </div>

      <div className="space-y-3">
        <div>
          <p className="text-sm font-medium text-[#1E2A3A] mb-2.5">빠른 상태 입력</p>
          {renderStatusButtons(selectedDate, selectedStatus, handleSelectedStatusClick, 'panel')}
        </div>

        <div className="h-[56px]">
          <div
            className={cn(
              'h-full rounded-lg border px-3 py-2.5 text-sm leading-[1.45] transition-colors',
              panelError
                ? 'bg-[#FEE2E2] border-[#B91C1C]/20 text-[#991B1B]'
                : panelMessage
                  ? 'bg-[#D1FAE5] border-[#2E7D5E]/20 text-[#065F46]'
                  : 'bg-[#F7F6F3] border-[#E2E4DF] text-[#8A9BB0]'
            )}
          >
            {panelFeedback ?? '상태 변경 결과는 여기에 표시됩니다.'}
          </div>
        </div>

        <div className="space-y-2">
          <label className="text-sm font-medium text-[#1E2A3A]">메모</label>
          <textarea
            value={memo}
            onChange={event => setMemo(event.target.value.slice(0, 200))}
            className="w-full h-24 px-3 py-2 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 resize-none transition-colors"
            placeholder="지각 사유나 간단한 메모를 남겨둘 수 있습니다."
          />
          <div className="flex items-center justify-between gap-3">
            <p className="text-[11px] text-[#8A9BB0]">
              {selectedStatus ? '메모만 바꿔도 저장할 수 있습니다.' : '상태를 먼저 선택하면 메모를 저장할 수 있습니다.'}
            </p>
            <p className="text-[11px] text-[#8A9BB0]">{memo.length}/200</p>
          </div>
          <button
            onClick={() => { void handleMemoSave(); }}
            disabled={!selectedStatus || memoSaving}
            className="w-full h-10 rounded-lg border border-[#3D7A8A] text-[#3D7A8A] text-sm font-medium hover:bg-[#E8F4F6] transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {memoSaving && <Loader2 className="w-4 h-4 animate-spin" />}
            <Save className="w-4 h-4" />
            메모 저장
          </button>
        </div>

      </div>
    </div>
  ) : (
    <div
      ref={detailPanelRef}
      tabIndex={-1}
      className={cn(
      'bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5',
      desktopDetailPanelClass
      )}
    >
      <div className="flex flex-col h-full">
        <div>
          <p className="text-[12px] font-semibold uppercase tracking-wider text-[#8A9BB0]">빠른 수정 패널</p>
          <h3 className="text-[20px] font-bold text-[#1E2A3A] mt-1">날짜를 선택해 바로 입력하세요</h3>
          <p className="text-sm text-[#4A5568] mt-3 leading-relaxed">
            달력에서 날짜를 누르면 이 패널에서 출석, 지각, 조퇴, 결석을 바로 저장할 수 있습니다.
            데스크톱에서는 오른쪽 패널을 띄워둔 채 연속으로 여러 날짜를 수정하기 좋습니다.
          </p>
        </div>

        <div className="rounded-lg bg-[#F7F6F3] border border-[#E2E4DF] px-4 py-3 mt-6">
          <div className="flex items-start justify-between gap-3">
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-[#1E2A3A]">선택 전 미리보기</p>
              <p className="text-[12px] text-[#8A9BB0] mt-1">
                날짜를 선택하면 현재 상태와 삭제 버튼이 이 자리에서 바로 열립니다.
              </p>
              {trainingProfile?.configured && (
                <p className="text-[12px] text-[#8A9BB0] mt-2">
                  내 과정 정보에 저장한 수업 요일과 휴강일이 아닌 날짜는 달력에서 비활성화됩니다.
                </p>
              )}
            </div>
            <div className="inline-flex items-center justify-center gap-1.5 h-9 px-3 rounded-lg border border-[#E2E4DF] text-[#8A9BB0] text-[12px] font-medium bg-[#FDFCFB] shrink-0">
              <Trash2 className="w-3.5 h-3.5" />
              기록 삭제
            </div>
          </div>
        </div>

        <div className="min-h-[52px] mt-4" />

        <div className="space-y-4 mt-auto">
          <div>
            <p className="text-sm font-medium text-[#1E2A3A] mb-2.5">빠른 상태 입력</p>
            <div className="grid grid-cols-2 gap-2">
              {STATUS_OPTIONS.map(option => (
                <div
                  key={`preview-${option.value}`}
                  className="rounded-xl border-2 border-[#E2E4DF] bg-[#FDFCFB] px-3.5 py-3 opacity-60"
                >
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ backgroundColor: option.bg }}>
                      <option.icon className="w-4.5 h-4.5" style={{ color: option.color }} />
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-[#1E2A3A]">{option.label}</p>
                      <p className="text-[10px] leading-none tracking-tight text-[#8A9BB0] whitespace-nowrap">바로 저장</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-[#1E2A3A]">메모</label>
            <div className="w-full h-24 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF]" />
            <div className="flex items-center justify-between gap-3">
              <p className="text-[11px] text-[#8A9BB0]">상태를 선택하면 메모 입력이 활성화됩니다.</p>
              <p className="text-[11px] text-[#8A9BB0]">0/200</p>
            </div>
            <div className="w-full h-10 rounded-lg border border-[#E2E4DF] bg-[#F7F6F3] text-[#8A9BB0] text-sm font-medium flex items-center justify-center gap-2">
              <Save className="w-4 h-4" />
              메모 저장
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className={cn('space-y-5 pb-4', desktopContentWidthClass)} style={{ overflowAnchor: 'none' }}>
      <h1 className="text-[22px] font-bold text-[#1E2A3A]">출결 관리</h1>

      <div className={cn('grid gap-5', desktopTwoColumnLayoutClass, desktopRowAlignmentClass)}>
        <div className={cn(
          'bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5',
          desktopCalendarCardClass
        )}>
          {bulkFillToolbar}

          <div className="flex items-center justify-between mb-5">
            <button
              type="button"
              onClick={() => navigateMonth(-1)}
              onMouseUp={event => event.currentTarget.blur()}
              className="w-9 h-9 rounded-lg border border-[#E2E4DF] flex items-center justify-center hover:bg-[#EFF0EC] transition-colors"
            >
              <ChevronLeft className="w-4 h-4 text-[#4A5568]" />
            </button>
            <h2 className="text-lg font-semibold text-[#1E2A3A]">
              {currentYear}년 {currentMonth + 1}월
            </h2>
            <button
              type="button"
              onClick={() => navigateMonth(1)}
              onMouseUp={event => event.currentTarget.blur()}
              disabled={isCurrentOrFutureMonth}
              className="w-9 h-9 rounded-lg border border-[#E2E4DF] flex items-center justify-center hover:bg-[#EFF0EC] transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight className="w-4 h-4 text-[#4A5568]" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-0 mb-2">
            {DAY_NAMES.map((dayName, index) => (
              <div
                key={dayName}
                className={cn(
                  'text-center text-[12px] font-medium py-1',
                  index === 0 ? 'text-[#B91C1C]' : index === 6 ? 'text-[#3D7A8A]' : 'text-[#8A9BB0]'
                )}
              >
                {dayName}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-0">
            {calendarDays.map((day, index) => {
              if (!day.isCurrentMonth) {
                return <div key={index} className="aspect-square" />;
              }

              const record = getAttendanceForDate(day.dateStr);
              const isSelected = selectedDate === day.dateStr;
              const isPastSavedTargetWithoutRecord = !record && !day.isFuture && !day.isToday;
              const isTrainingDay = isConfiguredTrainingDay(day.dateStr, trainingProfile);
              const isSelectable = !day.isFuture && (isTrainingDay || Boolean(record));
              const trainingNotice = getTrainingDayNotice(day.dateStr, trainingProfile);

              return (
                <button
                  key={day.dateStr}
                  onClick={() => handleDayClick(day)}
                  disabled={!isSelectable}
                  title={!isSelectable ? trainingNotice ?? '현재 입력할 수 없는 날짜입니다.' : undefined}
                  className={cn(
                    'aspect-square flex flex-col items-center justify-center rounded-lg text-sm transition-[background-color,border-color,box-shadow,color] relative',
                    !isSelectable && 'opacity-35 cursor-not-allowed',
                    day.isToday && !isSelected && 'ring-2 ring-[#3D7A8A] ring-offset-1',
                    isSelected && 'bg-[#E8F4F6] ring-2 ring-[#3D7A8A]',
                    isSelectable && !isSelected && 'hover:bg-[#EFF0EC]',
                    isPastSavedTargetWithoutRecord && 'border border-dashed border-[#C8CCC6]/60'
                  )}
                >
                  <span
                    className={cn(
                      'text-sm',
                      day.isToday ? 'font-bold text-[#3D7A8A]' : 'text-[#1E2A3A]',
                      day.isWeekend && new Date(currentYear, currentMonth, day.date).getDay() === 0 && 'text-[#B91C1C]'
                    )}
                  >
                    {day.date}
                  </span>
                  {getStatusDot(day.dateStr)}
                </button>
              );
            })}
          </div>
        </div>

        <div className={cn('space-y-5', desktopCalendarCardClass, isMobile && 'mt-5')}>
          {summaryStrip}
          {detailPanel}
        </div>
      </div>

      {bulkFillConfirmDialog}
    </div>
  );
}
