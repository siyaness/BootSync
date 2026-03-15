import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppApiError } from '@/lib/api';
import { useApp } from '@/lib/store';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  Clock,
  Eye,
  EyeOff,
  Lock,
  LogOut,
  Mail,
  Shield,
  Trash2,
  User,
} from 'lucide-react';

type SubPage = 'main' | 'editName' | 'changePassword' | 'changeEmail' | 'deleteAccount';

function ErrorMessage({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="bg-[#FEE2E2] border border-[#B91C1C]/20 rounded-lg p-3 text-sm text-[#991B1B]">
      {message}
    </div>
  );
}

function formatPendingExpiresAt(value: string | null) {
  if (!value) return null;
  const [datePart = '', timePart = ''] = value.split('T');
  if (!datePart || !timePart) return value;
  return `${datePart.replace(/-/g, '.')} ${timePart.slice(0, 5)}`;
}

export default function SettingsPage() {
  const RESEND_COOLDOWN_MS = 60 * 1000;
  const navigate = useNavigate();
  const {
    user,
    recoveryEmailStatus,
    logout,
    updateDisplayName,
    changePassword,
    updateRecoveryEmail,
    resendVerification,
    requestAccountDeletion,
  } = useApp();

  const [subPage, setSubPage] = useState<SubPage>('main');
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [noticeMessage, setNoticeMessage] = useState<string | null>(null);
  const [resendLockedUntil, setResendLockedUntil] = useState<number | null>(null);

  const [newName, setNewName] = useState(user.displayName);

  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');
  const [showNewPw, setShowNewPw] = useState(false);
  const [pwSuccess, setPwSuccess] = useState(false);

  const [newEmail, setNewEmail] = useState('');
  const [emailCurrentPassword, setEmailCurrentPassword] = useState('');
  const [emailSent, setEmailSent] = useState(false);

  const [deleteUsername, setDeleteUsername] = useState('');
  const [deleteCurrentPassword, setDeleteCurrentPassword] = useState('');

  const pendingExpiry = formatPendingExpiresAt(recoveryEmailStatus.pendingVerificationExpiresAt);
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

  const goMain = () => {
    resetMessages();
    setSubPage('main');
  };

  const handleLogout = async () => {
    resetMessages();
    try {
      await logout();
      navigate('/login?reason=logged_out');
    } catch (error) {
      handleApiError(error);
    }
  };

  const handleSaveName = async () => {
    if (newName.trim().length < 2) return;
    resetMessages();
    try {
      await updateDisplayName(newName.trim());
      setNoticeMessage('표시 이름을 변경했습니다.');
      setSubPage('main');
    } catch (error) {
      handleApiError(error);
    }
  };

  const handleChangePassword = async () => {
    if (!currentPw || newPw.length < 10 || newPw !== confirmPw) return;
    resetMessages();
    try {
      await changePassword(currentPw, newPw, confirmPw);
      setPwSuccess(true);
      setTimeout(() => {
        setPwSuccess(false);
        setCurrentPw('');
        setNewPw('');
        setConfirmPw('');
        setSubPage('main');
        setNoticeMessage('비밀번호를 변경했습니다.');
      }, 1200);
    } catch (error) {
      handleApiError(error);
    }
  };

  const handleSendVerification = async () => {
    if (!newEmail.trim() || !emailCurrentPassword.trim()) return;
    resetMessages();
    try {
      await updateRecoveryEmail(newEmail, emailCurrentPassword);
      setEmailSent(true);
      setEmailCurrentPassword('');
    } catch (error) {
      handleApiError(error);
    }
  };

  const handleResendVerification = async () => {
    if (resendCooldownActive) {
      return;
    }

    resetMessages();
    try {
      await resendVerification();
      setNoticeMessage('인증 메일을 다시 발송했습니다.');
      setResendLockedUntil(Date.now() + RESEND_COOLDOWN_MS);
    } catch (error) {
      if (error instanceof AppApiError && error.code === 'rate_limit') {
        setResendLockedUntil(Date.now() + RESEND_COOLDOWN_MS);
      }
      handleApiError(error);
    }
  };

  const handleDeleteAccount = async () => {
    if (deleteUsername !== user.username || !deleteCurrentPassword.trim()) return;
    resetMessages();
    try {
      await requestAccountDeletion(deleteCurrentPassword);
      navigate('/login?reason=pending_delete');
    } catch (error) {
      handleApiError(error);
    }
  };

  if (subPage === 'editName') {
    return (
      <div className="max-w-[480px] space-y-5 pb-4">
        <button onClick={goMain} className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] transition-colors">
          <ArrowLeft className="w-4 h-4" /> 설정
        </button>
        <h1 className="text-[22px] font-bold text-[#1E2A3A]">표시 이름 변경</h1>
        <ErrorMessage message={errorMessage} />
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
          <div className="space-y-1.5">
            <label className="text-sm font-medium text-[#1E2A3A]">표시 이름</label>
            <input
              type="text"
              value={newName}
              onChange={event => setNewName(event.target.value)}
              className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
            />
          </div>
          <button
            onClick={handleSaveName}
            disabled={newName.trim().length < 2}
            className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-50"
          >
            저장
          </button>
        </div>
      </div>
    );
  }

  if (subPage === 'changePassword') {
    return (
      <div className="max-w-[480px] space-y-5 pb-4">
        <button onClick={goMain} className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] transition-colors">
          <ArrowLeft className="w-4 h-4" /> 설정
        </button>
        <h1 className="text-[22px] font-bold text-[#1E2A3A]">비밀번호 변경</h1>
        <ErrorMessage message={errorMessage} />

        {!user.emailVerified && (
          <div className="bg-[#FEF3C7] border border-[#D97706]/20 rounded-lg p-3 flex items-start gap-2 text-sm text-[#92400E]">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>복구 이메일이 인증되지 않은 상태에서 비밀번호를 변경하면 계정 복구가 어려울 수 있습니다.</span>
          </div>
        )}

        {pwSuccess ? (
          <div className="bg-[#D1FAE5] border border-[#2E7D5E]/20 rounded-xl p-5 text-center">
            <CheckCircle2 className="w-8 h-8 text-[#2E7D5E] mx-auto mb-2" />
            <p className="text-sm font-medium text-[#065F46]">비밀번호가 변경되었습니다.</p>
          </div>
        ) : (
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">현재 비밀번호</label>
              <input
                type="password"
                value={currentPw}
                onChange={event => setCurrentPw(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">새 비밀번호</label>
              <div className="relative">
                <input
                  type={showNewPw ? 'text' : 'password'}
                  value={newPw}
                  onChange={event => setNewPw(event.target.value)}
                  className="w-full h-10 px-3 pr-10 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                  placeholder="10자 이상"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPw(!showNewPw)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8A9BB0]"
                >
                  {showNewPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">새 비밀번호 확인</label>
              <input
                type="password"
                value={confirmPw}
                onChange={event => setConfirmPw(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
              />
              {confirmPw && newPw !== confirmPw && (
                <p className="text-[12px] text-[#B91C1C]">비밀번호가 일치하지 않습니다.</p>
              )}
            </div>
            <button
              onClick={handleChangePassword}
              disabled={!currentPw || newPw.length < 10 || newPw !== confirmPw}
              className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-50"
            >
              변경하기
            </button>
          </div>
        )}
      </div>
    );
  }

  if (subPage === 'changeEmail') {
    return (
      <div className="max-w-[480px] space-y-5 pb-4">
        <button
          onClick={() => { goMain(); setEmailSent(false); setNewEmail(''); setEmailCurrentPassword(''); }}
          className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> 설정
        </button>
        <h1 className="text-[22px] font-bold text-[#1E2A3A]">복구 이메일 변경</h1>
        <ErrorMessage message={errorMessage} />

        {emailSent ? (
          <div className="bg-[#D1FAE5] border border-[#2E7D5E]/20 rounded-xl p-5 text-center space-y-2">
            <Mail className="w-8 h-8 text-[#2E7D5E] mx-auto" />
            <p className="text-sm font-medium text-[#065F46]">인증 메일이 발송되었습니다.</p>
            <p className="text-[12px] text-[#8A9BB0]">새 이메일 인증이 완료되기 전까지 기존 이메일이 유지됩니다.</p>
            {recoveryEmailStatus.developmentPreviewPath && (
              <a
                href={recoveryEmailStatus.developmentPreviewPath}
                className="inline-flex items-center justify-center h-9 px-3 rounded-lg border border-[#2E7D5E]/20 bg-white text-sm font-medium text-[#2E7D5E] hover:bg-[#F4FBF7] transition-colors"
              >
                로컬 확인 링크
              </a>
            )}
            {recoveryEmailStatus.hasPendingVerification && (
              <div className="pt-2 space-y-2">
                <button
                  onClick={handleResendVerification}
                  disabled={resendCooldownActive}
                  className="inline-flex items-center justify-center h-9 px-3 rounded-lg border border-[#2E7D5E]/20 bg-white text-sm font-medium text-[#2E7D5E] hover:bg-[#F4FBF7] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  인증 메일 재발송
                </button>
                {resendCooldownActive && (
                  <p className="text-[12px] text-[#8A9BB0]">
                    복구 이메일 재발송은 1분 뒤에 다시 시도할 수 있습니다.
                  </p>
                )}
              </div>
            )}
          </div>
        ) : (
          <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
            {user.recoveryEmail && (
              <div className="text-sm text-[#4A5568]">
                현재 등록된 이메일: <span className="font-medium">{user.recoveryEmail}</span>
              </div>
            )}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">새 복구 이메일</label>
              <input
                type="email"
                value={newEmail}
                onChange={event => setNewEmail(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
                placeholder="example@email.com"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">현재 비밀번호</label>
              <input
                type="password"
                value={emailCurrentPassword}
                onChange={event => setEmailCurrentPassword(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20"
              />
            </div>
            <p className="text-[12px] text-[#8A9BB0] leading-relaxed">
              새 이메일로 인증 메일이 발송됩니다. 인증 완료 전까지 기존 이메일이 유지됩니다.
            </p>
            <button
              onClick={handleSendVerification}
              disabled={!newEmail.trim() || !emailCurrentPassword.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(newEmail)}
              className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-50"
            >
              인증 메일 발송
            </button>
          </div>
        )}
      </div>
    );
  }

  if (subPage === 'deleteAccount') {
    return (
      <div className="max-w-[480px] space-y-5 pb-4">
        <button onClick={goMain} className="flex items-center gap-1.5 text-sm text-[#4A5568] hover:text-[#1E2A3A] transition-colors">
          <ArrowLeft className="w-4 h-4" /> 설정
        </button>
        <h1 className="text-[22px] font-bold text-[#B91C1C]">계정 삭제</h1>
        <ErrorMessage message={errorMessage} />

        {!user.emailVerified && (
          <div className="bg-[#FEE2E2] border border-[#B91C1C]/20 rounded-lg p-3 flex items-start gap-2 text-sm text-[#991B1B]">
            <Lock className="w-4 h-4 shrink-0 mt-0.5" />
            <span>복구 이메일 인증 후 이용 가능합니다.</span>
          </div>
        )}

        {user.emailVerified && (
          <div className="bg-[#FDFCFB] rounded-xl border border-[#B91C1C]/20 shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-5 space-y-4">
            <div className="bg-[#FEE2E2] rounded-lg p-3 text-sm text-[#991B1B] leading-relaxed">
              계정을 삭제하면 모든 데이터가 영구적으로 삭제됩니다. 삭제 요청 후 7일 유예 기간이 지나면 데이터가 purge 대상이 됩니다.
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">아이디 확인</label>
              <input
                type="text"
                value={deleteUsername}
                onChange={event => setDeleteUsername(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#B91C1C] focus:ring-2 focus:ring-[#B91C1C]/20"
                placeholder={user.username}
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">현재 비밀번호</label>
              <input
                type="password"
                value={deleteCurrentPassword}
                onChange={event => setDeleteCurrentPassword(event.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] focus:outline-none focus:border-[#B91C1C] focus:ring-2 focus:ring-[#B91C1C]/20"
              />
            </div>
            <button
              onClick={handleDeleteAccount}
              disabled={deleteUsername !== user.username || !deleteCurrentPassword.trim()}
              className="w-full h-10 rounded-lg bg-[#B91C1C] text-white text-sm font-medium hover:bg-[#991B1B] active:scale-[0.97] transition-all disabled:opacity-50"
            >
              계정 삭제 요청
            </button>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="max-w-[480px] space-y-5 pb-4">
      <h1 className="text-[22px] font-bold text-[#1E2A3A]">설정</h1>
      {noticeMessage && (
        <div className="bg-[#D1FAE5] border border-[#2E7D5E]/20 rounded-lg p-3 text-sm text-[#065F46]">
          {noticeMessage}
        </div>
      )}
      <ErrorMessage message={errorMessage} />

      <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] overflow-hidden">
        <div className="px-5 py-3 bg-[#EFF0EC]/50">
          <h2 className="text-[13px] font-semibold text-[#8A9BB0] uppercase tracking-wider flex items-center gap-2">
            <User className="w-3.5 h-3.5" /> 프로필
          </h2>
        </div>
        <button
          onClick={() => { resetMessages(); setNewName(user.displayName); setSubPage('editName'); }}
          className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-[#F7F6F3] transition-colors"
        >
          <div className="text-left">
            <p className="text-sm text-[#4A5568]">표시 이름</p>
            <p className="text-sm font-medium text-[#1E2A3A]">{user.displayName}</p>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8A9BB0]" />
        </button>
        <div className="h-px bg-[#E2E4DF] mx-5" />
        <div className="px-5 py-3.5">
          <p className="text-sm text-[#4A5568]">아이디</p>
          <p className="text-sm font-medium text-[#8A9BB0]">@{user.username}</p>
        </div>
      </div>

      <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] overflow-hidden">
        <div className="px-5 py-3 bg-[#EFF0EC]/50">
          <h2 className="text-[13px] font-semibold text-[#8A9BB0] uppercase tracking-wider flex items-center gap-2">
            <Shield className="w-3.5 h-3.5" /> 보안
          </h2>
        </div>
        <button
          onClick={() => { resetMessages(); setSubPage('changePassword'); }}
          className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-[#F7F6F3] transition-colors"
        >
          <span className="text-sm text-[#1E2A3A]">비밀번호 변경</span>
          <ChevronRight className="w-4 h-4 text-[#8A9BB0]" />
        </button>
        <div className="h-px bg-[#E2E4DF] mx-5" />
        <button
          onClick={() => { resetMessages(); setSubPage('changeEmail'); }}
          className="w-full flex items-center justify-between px-5 py-3.5 hover:bg-[#F7F6F3] transition-colors"
        >
          <div className="text-left flex-1">
            <div className="flex items-center gap-2 mb-0.5">
              <span className="text-sm text-[#1E2A3A]">복구 이메일</span>
              {user.emailVerified ? (
                <span className="inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-full bg-[#D1FAE5] text-[#2E7D5E] font-medium">
                  <CheckCircle2 className="w-3 h-3" /> 인증 완료
                </span>
              ) : recoveryEmailStatus.hasPendingVerification ? (
                <span className="inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-full bg-[#FEF3C7] text-[#D97706] font-medium">
                  <Clock className="w-3 h-3" /> 인증 대기 중
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-full bg-[#FEE2E2] text-[#B91C1C] font-medium">
                  <AlertTriangle className="w-3 h-3" /> 미인증
                </span>
              )}
            </div>
            <p className="text-[12px] text-[#8A9BB0]">{user.recoveryEmail || '등록된 복구 이메일 없음'}</p>
          </div>
          <ChevronRight className="w-4 h-4 text-[#8A9BB0] shrink-0" />
        </button>
        {recoveryEmailStatus.hasPendingVerification && (
          <>
            <div className="h-px bg-[#E2E4DF] mx-5" />
            <div className="px-5 py-3 space-y-2">
              <p className="text-[12px] text-[#4A5568] leading-relaxed">
                {recoveryEmailStatus.maskedPendingRecoveryEmail || '새 복구 이메일'}에 대한 {recoveryEmailStatus.pendingPurposeLabel || '복구 이메일 인증'}이 대기 중입니다.
              </p>
              {pendingExpiry && (
                <p className="text-[11px] text-[#8A9BB0]">만료 예정: {pendingExpiry}</p>
              )}
              {recoveryEmailStatus.developmentPreviewPath && (
                <a
                  href={recoveryEmailStatus.developmentPreviewPath}
                  className="inline-flex items-center gap-1.5 text-sm text-[#3D7A8A] font-medium hover:underline"
                >
                  <Mail className="w-3.5 h-3.5" />
                  로컬 확인 링크
                </a>
              )}
            </div>
          </>
        )}
        {recoveryEmailStatus.hasPendingVerification && (
          <>
            <div className="h-px bg-[#E2E4DF] mx-5" />
            <div className="px-5 py-3">
              <button
                onClick={handleResendVerification}
                disabled={resendCooldownActive}
                className="text-sm text-[#3D7A8A] font-medium hover:underline flex items-center gap-1.5 disabled:no-underline disabled:opacity-60 disabled:cursor-not-allowed"
              >
                <Mail className="w-3.5 h-3.5" />
                인증 메일 재발송
              </button>
              {resendCooldownActive && (
                <p className="text-[12px] text-[#8A9BB0] mt-1.5">
                  복구 이메일 재발송은 1분 뒤에 다시 시도할 수 있습니다.
                </p>
              )}
            </div>
          </>
        )}
      </div>

      <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] overflow-hidden">
        <div className="px-5 py-3 bg-[#EFF0EC]/50">
          <h2 className="text-[13px] font-semibold text-[#8A9BB0] uppercase tracking-wider">계정</h2>
        </div>
        {showLogoutConfirm ? (
          <div className="px-5 py-3.5 flex items-center gap-3">
            <span className="text-sm text-[#4A5568]">로그아웃 하시겠습니까?</span>
            <button
              onClick={handleLogout}
              className="h-8 px-3 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all"
            >
              확인
            </button>
            <button
              onClick={() => setShowLogoutConfirm(false)}
              className="h-8 px-3 rounded-lg border border-[#E2E4DF] text-[#4A5568] text-sm hover:bg-[#EFF0EC] transition-all"
            >
              취소
            </button>
          </div>
        ) : (
          <button
            onClick={() => setShowLogoutConfirm(true)}
            className="w-full flex items-center gap-2 px-5 py-3.5 text-sm text-[#4A5568] hover:bg-[#F7F6F3] transition-colors"
          >
            <LogOut className="w-4 h-4" />
            로그아웃
          </button>
        )}
        <div className="h-px bg-[#E2E4DF] mx-5" />
        <button
          onClick={() => { resetMessages(); setSubPage('deleteAccount'); }}
          className="w-full flex items-center gap-2 px-5 py-3.5 text-sm text-[#B91C1C] hover:bg-[#FEE2E2]/30 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
          계정 삭제
        </button>
      </div>
    </div>
  );
}
