import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '@/lib/store';
import { formatKRW } from '@/lib/display';
import { getCurrentSeoulDateInfo } from '@/lib/seoul-time';
import { CalendarCheck, AlertTriangle, ChevronDown, ChevronUp, Info } from 'lucide-react';

export default function AllowancePage() {
  const navigate = useNavigate();
  const { getAllowanceSummaryForMonth, loadAttendanceMonth } = useApp();
  const [rulesOpen, setRulesOpen] = useState(false);

  const today = getCurrentSeoulDateInfo();
  const currentYear = today.year;
  const currentMonth = today.monthIndex;
  const allowanceSummary = getAllowanceSummaryForMonth(currentYear, currentMonth);

  useEffect(() => {
    loadAttendanceMonth(currentYear, currentMonth).catch(() => {});
  }, [currentMonth, currentYear, loadAttendanceMonth]);

  if (!allowanceSummary) {
    return (
      <div className="space-y-5 pb-4">
        <h1 className="text-[22px] font-bold text-[#1E2A3A]">예상 훈련장려금</h1>
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-8 text-center">
          <p className="text-sm text-[#4A5568]">현재 단위기간 요약을 불러오는 중입니다.</p>
        </div>
      </div>
    );
  }

  const hasData = allowanceSummary.recordedTrainingDays > 0;
  const capOverflowDays = Math.max(0, allowanceSummary.recognizedAttendanceDays - allowanceSummary.payableAttendanceDays);

  return (
    <div className="space-y-5 pb-4">
      <h1 className="text-[22px] font-bold text-[#1E2A3A]">예상 훈련장려금</h1>

      {!hasData ? (
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-8 text-center">
          <div className="w-14 h-14 rounded-full bg-[#EFF0EC] flex items-center justify-center mx-auto mb-4">
            <CalendarCheck className="w-6 h-6 text-[#8A9BB0]" />
          </div>
          <p className="text-sm text-[#4A5568] mb-1">현재 단위기간 출결 데이터가 없습니다.</p>
          <p className="text-[12px] text-[#8A9BB0] mb-4">출결을 입력하면 현재 단위기간 예상 장려금이 계산됩니다.</p>
          <button
            onClick={() => navigate('/attendance')}
            className="h-10 px-5 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all mx-auto"
          >
            출결 입력하기
          </button>
        </div>
      ) : (
        <>
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-6 text-center">
            <p className="text-sm text-[#4A5568] mb-2">현재 단위기간 예상 훈련장려금</p>
            <p className="text-[12px] text-[#8A9BB0]">
              {allowanceSummary.periodStartDate} ~ {allowanceSummary.periodEndDate}
            </p>
            <p className="text-[36px] md:text-[42px] font-bold text-[#3D7A8A] tracking-tight leading-tight mt-3">
              {formatKRW(allowanceSummary.expectedAllowanceAmount)}
            </p>
            <p className="text-[11px] text-[#8A9BB0] mt-3 leading-relaxed max-w-sm mx-auto">
              *현재 단위기간에 저장된 출결만 반영한 추정값입니다. 미입력 수업일과 기관 최종 심사 결과에 따라 실제 지급액은 달라질 수 있습니다.
            </p>
          </div>

          {allowanceSummary.unrecordedCompletedDays > 0 && (
            <div className="bg-[#FEF3C7] border border-[#D97706]/20 rounded-xl p-4 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-[#D97706] shrink-0 mt-0.5" />
              <p className="text-sm text-[#92400E]">
                현재 단위기간에 아직 입력되지 않은 수업일이 {allowanceSummary.unrecordedCompletedDays}일 있습니다. 이 날짜들은 계산에서 제외됩니다.
              </p>
            </div>
          )}

          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5">
            <h3 className="text-[15px] font-semibold text-[#1E2A3A] mb-4">계산 내역</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">1일 지급액</span>
                <span className="text-sm font-medium text-[#1E2A3A]">{formatKRW(allowanceSummary.dailyAllowanceAmount)}</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">지급 상한 일수</span>
                <span className="text-sm font-medium text-[#1E2A3A]">{allowanceSummary.payableDayCap}일</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">인정 출석일수</span>
                <span className="text-sm font-medium text-[#1E2A3A]">{allowanceSummary.recognizedAttendanceDays}일</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">지급 반영 일수</span>
                <span className="text-sm font-medium text-[#1E2A3A]">{allowanceSummary.payableAttendanceDays}일</span>
              </div>
              {capOverflowDays > 0 && (
                <div className="flex items-center justify-between py-2">
                  <span className="text-sm text-[#4A5568]">상한 초과 일수</span>
                  <span className="text-sm font-medium text-[#D97706]">{capOverflowDays}일</span>
                </div>
              )}
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">결석 환산 횟수</span>
                <span className="text-sm font-medium text-[#1E2A3A]">{allowanceSummary.convertedAbsenceCount}회</span>
              </div>
              <div className="border-t border-[#E2E4DF]" />
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">기록된 출결</span>
                <span className="text-sm font-medium text-[#1E2A3A]">
                  출석 {allowanceSummary.presentCount} / 지각 {allowanceSummary.lateCount} / 조퇴 {allowanceSummary.leaveEarlyCount} / 결석 {allowanceSummary.absentCount}
                </span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-sm text-[#4A5568]">단위기간 수업일</span>
                <span className="text-sm font-medium text-[#1E2A3A]">
                  {allowanceSummary.completedScheduledDays}/{allowanceSummary.scheduledTrainingDays}일 완료
                </span>
              </div>
              <div className="border-t-2 border-[#1E2A3A]" />
              <div className="flex items-center justify-between py-2">
                <span className="text-sm font-semibold text-[#1E2A3A]">최종 예상액</span>
                <span className="text-lg font-bold text-[#3D7A8A]">{formatKRW(allowanceSummary.expectedAllowanceAmount)}</span>
              </div>
            </div>
          </div>

          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] overflow-hidden">
            <button
              onClick={() => setRulesOpen(!rulesOpen)}
              className="w-full flex items-center justify-between p-5 hover:bg-[#F7F6F3] transition-colors"
            >
              <div className="flex items-center gap-2">
                <Info className="w-4 h-4 text-[#3D7A8A]" />
                <span className="text-sm font-medium text-[#1E2A3A]">계산 방식 안내</span>
              </div>
              {rulesOpen ? <ChevronUp className="w-4 h-4 text-[#8A9BB0]" /> : <ChevronDown className="w-4 h-4 text-[#8A9BB0]" />}
            </button>
            {rulesOpen && (
              <div className="px-5 pb-5 space-y-3 text-sm text-[#4A5568] leading-relaxed">
                <div className="h-px bg-[#E2E4DF] mb-3" />
                <ul className="space-y-2">
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#3D7A8A] mt-2 shrink-0" />
                    <span>현재 단위기간 예상액은 <strong>1일 지급액 × 지급 반영 일수</strong>로 계산합니다.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#3D7A8A] mt-2 shrink-0" />
                    <span>지각 <strong>3회</strong> = 결석 <strong>1일</strong>, 조퇴 <strong>3회</strong> = 결석 <strong>1일</strong>로 환산됩니다.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#D97706] mt-2 shrink-0" />
                    <span>지각과 조퇴는 <strong>각각 따로</strong> 환산합니다.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#3D7A8A] mt-2 shrink-0" />
                    <span>지급 반영 일수는 인정 출석일수에서 시작하며, <strong>{allowanceSummary.payableDayCap}일</strong> 상한을 넘지 않습니다.</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#3D7A8A] mt-2 shrink-0" />
                    <span>미입력 수업일은 자동으로 결석 처리하지 않고 계산에서 제외합니다.</span>
                  </li>
                </ul>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
