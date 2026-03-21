import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { AppApiError } from '@/lib/api';
import { useApp } from '@/lib/store';
import { formatCourseCountdown } from '@/lib/attendance-insights';
import { getKoreanDateString, getRelativeTime, formatKRW } from '@/lib/display';
import { compareApiDateTimesDesc, getCurrentSeoulDateInfo } from '@/lib/seoul-time';
import {
  CalendarCheck, Code2, ArrowRight, Plus, AlertTriangle, Mail,
  CheckCircle2, Clock, XCircle, MinusCircle, ChevronRight, Zap,
} from 'lucide-react';
import { useIsMobile } from '@/hooks/use-mobile';
import { cn } from '@/lib/utils';

function getNextProgressHint(label: '지각' | '조퇴', count: number) {
  if (count === 0) {
    return `${label} 3회 여유`;
  }

  const remainder = count % 3;
  if (remainder === 0) {
    return `환산 ${Math.floor(count / 3)}회 반영`;
  }

  return `${3 - remainder}회 남음`;
}

export default function DashboardPage() {
  const RESEND_COOLDOWN_MS = 60 * 1000;
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const {
    user,
    recoveryEmailStatus,
    attendance,
    snippets,
    resendVerification,
    getAttendanceSummaryForMonth,
    getAllowanceSummaryForMonth,
    getTrainingSummaryForMonth,
    loadAttendanceMonth,
    loadSnippets,
  } = useApp();
  const isMobile = useIsMobile();
  const isWelcome = searchParams.get('welcome') === 'true';
  const [recoveryNotice, setRecoveryNotice] = useState<string | null>(null);
  const [recoveryError, setRecoveryError] = useState<string | null>(null);
  const [resendLockedUntil, setResendLockedUntil] = useState<number | null>(null);
  const resendCooldownActive = resendLockedUntil !== null;

  useEffect(() => {
    if (resendLockedUntil === null) {
      return;
    }

    const remaining = resendLockedUntil - Date.now();
    if (remaining <= 0) {
      setResendLockedUntil(null);
      return;
    }

    const timer = window.setTimeout(() => {
      setResendLockedUntil(null);
    }, remaining);

    return () => {
      window.clearTimeout(timer);
    };
  }, [resendLockedUntil]);

  const today = getCurrentSeoulDateInfo();
  const currentYear = today.year;
  const currentMonth = today.monthIndex;
  const todayStr = today.dateString;
  const todayRecord = attendance.find(r => r.date === todayStr);

  useEffect(() => {
    loadAttendanceMonth(currentYear, currentMonth).catch(() => {});
    loadSnippets().catch(() => {});
  }, [currentMonth, currentYear, loadAttendanceMonth, loadSnippets]);

  const monthlySummary = getAttendanceSummaryForMonth(currentYear, currentMonth) ?? {
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
  const allowanceSummary = getAllowanceSummaryForMonth(currentYear, currentMonth) ?? {
    referenceYearMonth: `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}`,
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

  const totalDays = monthlySummary.presentCount + monthlySummary.lateCount + monthlySummary.leaveEarlyCount + monthlySummary.absentCount;
  const attendanceRate = totalDays > 0 ? Math.round((monthlySummary.presentCount / totalDays) * 100) : 0;
  const courseCountdown = trainingSummary ? formatCourseCountdown(trainingSummary.daysUntilCourseEnd) : null;
  const recentSnippets = [...snippets].sort((a, b) => compareApiDateTimesDesc(a.updatedAt, b.updatedAt)).slice(0, 3);
  const lateHint = getNextProgressHint('지각', monthlySummary.lateCount);
  const leaveEarlyHint = getNextProgressHint('조퇴', monthlySummary.leaveEarlyCount);

  const handleResendVerification = async () => {
    if (resendCooldownActive) {
      return;
    }

    setRecoveryNotice(null);
    setRecoveryError(null);
    try {
      await resendVerification();
      setRecoveryNotice('인증 메일을 다시 발송했습니다.');
      setResendLockedUntil(Date.now() + RESEND_COOLDOWN_MS);
    } catch (error) {
      if (error instanceof AppApiError) {
        if (error.code === 'rate_limit') {
          setResendLockedUntil(Date.now() + RESEND_COOLDOWN_MS);
        }
        setRecoveryError(error.message);
        return;
      }
      setRecoveryError('인증 메일을 다시 발송하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  const statusConfig: Record<string, { icon: typeof CheckCircle2; color: string; bg: string }> = {
    '출석': { icon: CheckCircle2, color: '#2E7D5E', bg: '#D1FAE5' },
    '지각': { icon: Clock, color: '#D97706', bg: '#FEF3C7' },
    '조퇴': { icon: MinusCircle, color: '#D97706', bg: '#FEF3C7' },
    '결석': { icon: XCircle, color: '#B91C1C', bg: '#FEE2E2' },
  };

  return (
    <div className="space-y-5 pb-4">
      {/* Welcome banner */}
      {isWelcome && (
        <div className="bg-[#D1FAE5] border border-[#2E7D5E]/20 rounded-xl p-4 flex items-center gap-3 animate-fade-in-up">
          <Zap className="w-5 h-5 text-[#2E7D5E] shrink-0" />
          <p className="text-sm text-[#065F46]">
            환영합니다, <strong>{user.displayName}</strong>님! BootSync를 시작해보세요.
          </p>
        </div>
      )}

      {/* Greeting */}
      <div className="pt-2">
        <h1 className="text-[26px] md:text-[30px] font-bold text-[#1E2A3A] leading-tight">
          안녕하세요, {user.displayName}님 👋
        </h1>
        <p className="text-sm text-[#8A9BB0] mt-1">{getKoreanDateString()}</p>
      </div>

      {/* Recovery email banner */}
      {!user.emailVerified && (
        <div className="bg-[#FEF3C7] border border-[#D97706]/20 rounded-xl p-4 flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-[#D97706] shrink-0 mt-0.5" />
          <div className="flex-1 min-w-0">
            <p className="text-sm text-[#92400E] leading-relaxed">
              복구 이메일 인증이 완료되지 않았습니다. 일부 기능이 제한될 수 있습니다.
            </p>
            {recoveryEmailStatus.hasPendingVerification && (
              <p className="text-[12px] text-[#92400E] mt-1.5">
                {recoveryEmailStatus.maskedPendingRecoveryEmail || '새 복구 이메일'}에 대한 {recoveryEmailStatus.pendingPurposeLabel || '인증'}이 대기 중입니다.
              </p>
            )}
            {!recoveryEmailStatus.hasPendingVerification && (
              <p className="text-[12px] text-[#92400E] mt-1.5">
                아직 인증 메일을 다시 보낼 대상이 없습니다. 설정에서 복구 이메일을 먼저 등록해 주세요.
              </p>
            )}
            {recoveryNotice && (
              <p className="text-[12px] text-[#2E7D5E] mt-2">{recoveryNotice}</p>
            )}
            {recoveryError && (
              <p className="text-[12px] text-[#B91C1C] mt-2">{recoveryError}</p>
            )}
            <div className="flex items-center gap-3 mt-2.5 flex-wrap">
              {recoveryEmailStatus.hasPendingVerification && (
                <button
                  onClick={handleResendVerification}
                  disabled={resendCooldownActive}
                  className="text-sm font-medium text-[#92400E] bg-[#D97706]/10 hover:bg-[#D97706]/20 px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <Mail className="w-3.5 h-3.5" />
                  인증 메일 재발송
                </button>
              )}
              <Link to="/settings" className="text-sm text-[#92400E] underline hover:no-underline">
                설정에서 확인
              </Link>
              {recoveryEmailStatus.developmentPreviewPath && (
                <a href={recoveryEmailStatus.developmentPreviewPath} className="text-sm text-[#92400E] underline hover:no-underline">
                  로컬 확인 링크
                </a>
              )}
            </div>
            {resendCooldownActive && (
              <p className="text-[12px] text-[#92400E] mt-2">
                복구 이메일 재발송은 1분 뒤에 다시 시도할 수 있습니다.
              </p>
            )}
          </div>
        </div>
      )}

      {/* Grid layout */}
      <div className={cn('grid gap-5', !isMobile && 'grid-cols-2')}>
        {/* Left column */}
        <div className="space-y-5">
          {/* Today's attendance */}
          {todayRecord ? (
            <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-shadow">
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-[15px] font-semibold text-[#1E2A3A]">오늘의 출결</h3>
                <Link to="/attendance" className="text-[#3D7A8A] text-sm hover:underline flex items-center gap-1">
                  상세 <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              </div>
              <div className="flex items-center gap-3">
                {(() => {
                  const config = statusConfig[todayRecord.status];
                  const Icon = config.icon;
                  return (
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ backgroundColor: config.bg }}>
                        <Icon className="w-5 h-5" style={{ color: config.color }} />
                      </div>
                      <div>
                        <span className="inline-block px-2.5 py-1 rounded text-xs font-semibold" style={{ backgroundColor: config.bg, color: config.color }}>
                          {todayRecord.status}
                        </span>
                        {todayRecord.memo && (
                          <p className="text-[12px] text-[#8A9BB0] mt-1">{todayRecord.memo}</p>
                        )}
                      </div>
                    </div>
                  );
                })()}
              </div>
            </div>
          ) : (
            <div
              className="bg-[#FDFCFB] rounded-xl border-2 border-dashed border-[#C8CCC6] p-5 cursor-pointer hover:border-[#3D7A8A]/40 transition-colors"
              onClick={() => navigate('/attendance')}
            >
              <div className="flex items-center justify-between mb-3">
                <h3 className="text-[15px] font-semibold text-[#1E2A3A]">오늘의 출결</h3>
              </div>
              <p className="text-sm text-[#8A9BB0] mb-3">오늘 출결을 아직 입력하지 않았습니다</p>
              <button className="h-9 px-4 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all flex items-center gap-2">
                <CalendarCheck className="w-4 h-4" />
                입력하기
              </button>
            </div>
          )}

          {/* Monthly summary */}
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[15px] font-semibold text-[#1E2A3A]">
                {currentMonth + 1}월 출결 요약
              </h3>
              <Link to="/attendance" className="text-[#3D7A8A] text-sm hover:underline flex items-center gap-1">
                전체 보기 <ChevronRight className="w-3.5 h-3.5" />
              </Link>
            </div>
            <div className="grid grid-cols-4 gap-2 mb-4">
                {[
                { label: '출석', value: monthlySummary.presentCount, color: '#2E7D5E', bg: '#D1FAE5' },
                { label: '지각', value: monthlySummary.lateCount, color: '#D97706', bg: '#FEF3C7' },
                { label: '조퇴', value: monthlySummary.leaveEarlyCount, color: '#D97706', bg: '#FEF3C7' },
                { label: '결석', value: monthlySummary.absentCount, color: '#B91C1C', bg: '#FEE2E2' },
              ].map(item => (
                <div key={item.label} className="text-center p-2.5 rounded-lg" style={{ backgroundColor: item.bg }}>
                  <p className="text-lg font-bold" style={{ color: item.color }}>{item.value}</p>
                  <p className="text-[11px] font-medium" style={{ color: item.color }}>{item.label}</p>
                </div>
              ))}
            </div>
            {totalDays > 0 && (
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-[12px] text-[#8A9BB0]">기록 기준 출석률</span>
                  <span className="text-[12px] font-medium text-[#1E2A3A]">{attendanceRate}%</span>
                </div>
                <div className="h-2 bg-[#EFF0EC] rounded-full overflow-hidden">
                  <div
                    className="h-full bg-[#3D7A8A] rounded-full transition-all duration-500"
                    style={{ width: `${attendanceRate}%` }}
                  />
                </div>
              </div>
            )}
          </div>

          {/* Training risk */ }
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-shadow">
            <div className="flex items-start justify-between gap-3 mb-4">
              <div>
                <h3 className="text-[15px] font-semibold text-[#1E2A3A]">수강 리스크</h3>
                <p className="text-[12px] text-[#8A9BB0] mt-1">
                  핵심 지표만 짧게 보여주고, 자세한 내용은 과정 현황에서 확인합니다.
                </p>
              </div>
              <div className="w-10 h-10 rounded-xl bg-[#F8E6D8] flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-[#C96B3B]" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2.5 mb-4">
              <div className="rounded-xl bg-[#F7F6F3] px-3 py-3">
                <p className="text-[11px] text-[#8A9BB0]">수료 D-day</p>
                <p className="text-lg font-semibold text-[#1E2A3A] mt-1">{courseCountdown ?? '미설정'}</p>
                <p className="text-[11px] text-[#8A9BB0] mt-1">
                  {trainingSummary ? trainingSummary.courseEndDate : '과정 정보를 입력하면 표시됩니다.'}
                </p>
              </div>
              <div className="rounded-xl bg-[#F7F6F3] px-3 py-3">
                <p className="text-[11px] text-[#8A9BB0]">앞으로 더 빠져도 되는 날</p>
                <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                  {trainingSummary
                    ? trainingSummary.canReachThreshold
                      ? `${trainingSummary.remainingAbsenceBudget}일`
                      : '여유 없음'
                    : '미설정'}
                </p>
                <p className="text-[11px] text-[#8A9BB0] mt-1">
                  {!trainingSummary
                    ? '개인 과정 정보를 입력하면 계산됩니다.'
                    : trainingSummary.canReachThreshold
                      ? `${trainingSummary.thresholdPercent}% 출석률을 유지할 수 있는 여유`
                      : `${trainingSummary.thresholdPercent}% 출석률을 맞추기 어려운 상태입니다.`}
                </p>
              </div>
              <div className="rounded-xl bg-[#F7F6F3] px-3 py-3">
                <p className="text-[11px] text-[#8A9BB0]">이번 달 결석 환산</p>
                <p className="text-lg font-semibold text-[#1E2A3A] mt-1">
                  {monthlySummary.convertedAbsenceCount}회
                </p>
                <p className="text-[11px] text-[#8A9BB0] mt-1">
                  {allowanceSummary.recordedTrainingDays > 0
                    ? `예상 ${formatKRW(allowanceSummary.expectedAllowanceAmount)}`
                    : '단위기간 기록을 기준으로 계산됩니다.'}
                </p>
              </div>
              <div className="rounded-xl bg-[#F7F6F3] px-3 py-3">
                <p className="text-[11px] text-[#8A9BB0]">지각/조퇴 경고</p>
                <p className="text-sm font-semibold text-[#1E2A3A] mt-1">
                  지각 {monthlySummary.lateCount}회 / 조퇴 {monthlySummary.leaveEarlyCount}회
                </p>
                <p className="text-[11px] text-[#8A9BB0] mt-1 leading-relaxed">
                  지각: {lateHint} · 조퇴: {leaveEarlyHint}
                </p>
              </div>
            </div>
            <Link
              to="/course-status"
              className="inline-flex items-center gap-1.5 text-sm text-[#3D7A8A] font-medium hover:underline"
            >
              과정 현황에서 자세히 보기
              <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {/* Expected allowance */}
          <div
            className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-shadow cursor-pointer"
            onClick={() => navigate('/allowance')}
          >
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-[15px] font-semibold text-[#1E2A3A]">현재 단위기간 예상 훈련장려금</h3>
              <ChevronRight className="w-4 h-4 text-[#8A9BB0]" />
            </div>
            <p className="text-[28px] font-bold text-[#3D7A8A] tracking-tight">
              {formatKRW(allowanceSummary.expectedAllowanceAmount)}
            </p>
            <p className="text-[12px] text-[#8A9BB0] mt-1">
              {allowanceSummary.periodStartDate && allowanceSummary.periodEndDate
                ? `${allowanceSummary.periodStartDate} ~ ${allowanceSummary.periodEndDate}`
                : '현재 단위기간 기준'}
            </p>
            <p className="text-[11px] text-[#8A9BB0] mt-2">*현재 단위기간에 저장된 출결을 기준으로 계산한 추정값입니다.</p>
          </div>
        </div>

        {/* Right column */}
        <div className="space-y-5">
          {/* Recent study notes */}
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 hover:shadow-[0_4px_12px_rgba(30,42,58,0.08)] transition-shadow">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-[15px] font-semibold text-[#1E2A3A]">최근 학습 노트</h3>
              {snippets.length > 0 && (
                <Link to="/snippets" className="text-[#3D7A8A] text-sm hover:underline flex items-center gap-1">
                  전체 보기 <ChevronRight className="w-3.5 h-3.5" />
                </Link>
              )}
            </div>
            {recentSnippets.length > 0 ? (
              <div className="space-y-3">
                {recentSnippets.map(snippet => (
                  <Link
                    key={snippet.id}
                    to={`/snippets/${snippet.id}`}
                    className="block p-3 rounded-lg bg-[#F7F6F3] hover:bg-[#EFF0EC] transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="text-sm font-medium text-[#1E2A3A] truncate">{snippet.title}</h4>
                      <span className="text-[11px] text-[#8A9BB0] shrink-0">{getRelativeTime(snippet.updatedAt)}</span>
                    </div>
                    <div className="flex items-center gap-1.5 mt-1.5">
                      {snippet.tags.slice(0, 2).map(tag => (
                        <span key={tag} className="text-[10px] px-2 py-0.5 rounded bg-[#E8F4F6] text-[#3D7A8A] font-medium">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="text-center py-6">
                <div className="w-12 h-12 rounded-full bg-[#EFF0EC] flex items-center justify-center mx-auto mb-3">
                  <Code2 className="w-5 h-5 text-[#8A9BB0]" />
                </div>
                <p className="text-sm text-[#4A5568] mb-1">저장된 학습 노트가 없습니다.</p>
                <p className="text-[12px] text-[#8A9BB0] mb-3">첫 학습 노트를 만들어보세요.</p>
                <button
                  onClick={() => navigate('/snippets/new')}
                  className="h-8 px-3 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all flex items-center gap-1.5 mx-auto"
                >
                  <Plus className="w-3.5 h-3.5" />
                  학습 노트 추가
                </button>
              </div>
            )}
          </div>

          {/* Quick actions */}
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5">
            <h3 className="text-[15px] font-semibold text-[#1E2A3A] mb-3">빠른 실행</h3>
            <div className="flex gap-2 flex-wrap">
              <button
                onClick={() => navigate('/attendance')}
                className="h-9 px-4 rounded-lg border border-[#3D7A8A] text-[#3D7A8A] text-sm font-medium hover:bg-[#E8F4F6] active:scale-[0.97] transition-all flex items-center gap-2"
              >
                <CalendarCheck className="w-4 h-4" />
                오늘 출결 입력
              </button>
              <button
                onClick={() => navigate('/snippets/new')}
                className="h-9 px-4 rounded-lg border border-[#3D7A8A] text-[#3D7A8A] text-sm font-medium hover:bg-[#E8F4F6] active:scale-[0.97] transition-all flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                학습 노트 추가
              </button>
              <button
                onClick={() => navigate('/allowance')}
                className="h-9 px-4 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm font-medium hover:bg-[#EFF0EC] active:scale-[0.97] transition-all flex items-center gap-2"
              >
                장려금 보기
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
