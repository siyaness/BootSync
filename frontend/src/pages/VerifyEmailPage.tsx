import { useEffect, useMemo, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { AppApiError, apiRequest } from '@/lib/api';
import { useApp } from '@/lib/store';
import { CheckCircle2, Info, Clock, XCircle, Mail, Loader2, Lock } from 'lucide-react';

type VerificationPurpose = 'signup' | 'change';
type ViewState = 'loading' | 'confirm' | 'success' | 'already_used' | 'invalid' | 'login_required' | 'error';

type RecoveryEmailPreviewResponse = {
  purpose: string;
  maskedTargetEmail: string | null;
  verificationExpiresAt: string | null;
  alreadyConsumed: boolean;
  invalid: boolean;
};

type RecoveryEmailConfirmResponse = {
  verified: boolean;
  maskedRecoveryEmail: string | null;
  message: string;
};

type SessionResponse = {
  authenticated: boolean;
  csrf: {
    headerName: string;
    token: string;
  };
};

function normalizePurpose(value: string | null): VerificationPurpose | null {
  if (value === 'signup' || value === 'change') {
    return value;
  }
  return null;
}

function formatDateTime(value: string | null) {
  if (!value) return null;
  const [datePart = '', timePart = ''] = value.split('T');
  if (!datePart || !timePart) return value;
  return `${datePart.replace(/-/g, '.')} ${timePart.slice(0, 5)}`;
}

export default function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const { isAuthenticated, refreshSession } = useApp();

  const purpose = useMemo(() => normalizePurpose(searchParams.get('purpose')), [searchParams]);
  const token = searchParams.get('token')?.trim() || '';

  const [viewState, setViewState] = useState<ViewState>('loading');
  const [maskedEmail, setMaskedEmail] = useState<string | null>(null);
  const [expiresAt, setExpiresAt] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const verificationTitle = purpose === 'change' ? '복구 이메일 변경 확인' : '복구 이메일 인증';
  const verificationDescription = purpose === 'change'
    ? '설정에서 요청한 새 복구 이메일을 지금 이 화면에서 확정합니다.'
    : '회원가입 때 입력한 복구 이메일을 최종 복구 수단으로 등록합니다.';
  const nextLoginTarget = purpose && token
    ? `/login?next=${encodeURIComponent(`/verify-email?purpose=${purpose}&token=${token}`)}`
    : '/login';

  useEffect(() => {
    let ignore = false;

    const loadPreview = async () => {
      if (!purpose || !token) {
        if (!ignore) {
          setMessage('유효하지 않은 인증 링크입니다.');
          setViewState('invalid');
        }
        return;
      }

      setViewState('loading');
      setMessage(null);

      try {
        const preview = await apiRequest<RecoveryEmailPreviewResponse>(
          `/api/recovery-email/preview?purpose=${purpose}&token=${encodeURIComponent(token)}`
        );

        if (!preview || ignore) {
          return;
        }

        setMaskedEmail(preview.maskedTargetEmail);
        setExpiresAt(formatDateTime(preview.verificationExpiresAt));

        if (preview.invalid) {
          setMessage('유효하지 않거나 만료된 인증 링크입니다.');
          setViewState('invalid');
          return;
        }

        if (preview.alreadyConsumed) {
          setMessage(
            purpose === 'change'
              ? '이미 복구 이메일 변경이 완료된 링크입니다.'
              : '이미 인증이 완료된 링크입니다.'
          );
          setViewState('already_used');
          return;
        }

        setViewState('confirm');
      } catch (error) {
        if (ignore) {
          return;
        }
        if (error instanceof AppApiError && error.status === 401) {
          setMessage('로그인 후 복구 이메일 변경을 확인해 주세요.');
          setViewState('login_required');
          return;
        }
        if (error instanceof AppApiError) {
          setMessage(error.message);
          setViewState('error');
          return;
        }
        setMessage('인증 링크를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.');
        setViewState('error');
      }
    };

    loadPreview();

    return () => {
      ignore = true;
    };
  }, [purpose, token]);

  const handleConfirm = async () => {
    if (!purpose || !token) {
      setMessage('유효하지 않은 인증 링크입니다.');
      setViewState('invalid');
      return;
    }

    setSubmitting(true);
    setMessage(null);

    try {
      const session = await apiRequest<SessionResponse>('/api/auth/session');
      if (!session) {
        throw new AppApiError({
          message: '보안 토큰을 불러오지 못했습니다.',
          status: 400,
        });
      }

      const result = await apiRequest<RecoveryEmailConfirmResponse>('/api/recovery-email/confirm', {
        method: 'POST',
        headers: {
          [session.csrf.headerName]: session.csrf.token,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          purpose,
          token,
        }),
      });

      if (!result) {
        throw new AppApiError({
          message: '인증 결과를 확인하지 못했습니다.',
          status: 500,
        });
      }

      setMaskedEmail(result.maskedRecoveryEmail);
      setMessage(result.message);

      if (result.verified) {
        await refreshSession();
        setViewState('success');
        return;
      }

      setViewState('invalid');
    } catch (error) {
      if (error instanceof AppApiError && error.status === 401) {
        setMessage('로그인 후 복구 이메일 변경을 확인해 주세요.');
        setViewState('login_required');
        return;
      }
      if (error instanceof AppApiError) {
        setMessage(error.message);
        setViewState('error');
        return;
      }
      setMessage('인증을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.');
      setViewState('error');
    } finally {
      setSubmitting(false);
    }
  };

  const primaryLink = isAuthenticated ? '/dashboard' : '/login';
  const secondaryLink = isAuthenticated ? '/settings' : '/login';

  return (
    <div className="min-h-screen bg-[#F7F6F3] flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[420px]">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-[#1E2A3A] tracking-tight">
            Boot<span className="text-[#3D7A8A]">Sync</span>
          </h1>
          <p className="text-sm text-[#8A9BB0] mt-2">{verificationDescription}</p>
        </div>

        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-6">
          {viewState === 'loading' && (
            <div className="text-center space-y-4 py-4">
              <Loader2 className="w-7 h-7 text-[#3D7A8A] mx-auto animate-spin" />
              <p className="text-sm text-[#4A5568]">인증 링크를 확인하고 있습니다.</p>
            </div>
          )}

          {viewState === 'confirm' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#E8F4F6] flex items-center justify-center mx-auto">
                <Mail className="w-6 h-6 text-[#3D7A8A]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">{verificationTitle}</h2>
              <p className="text-sm text-[#4A5568] leading-relaxed">
                {purpose === 'change'
                  ? '이 이메일 주소로 복구 이메일을 변경하시겠습니까?'
                  : '이 이메일 주소를 복구 이메일로 인증하시겠습니까?'}
              </p>
              <p className="text-sm font-medium text-[#1E2A3A] bg-[#EFF0EC] rounded-lg px-4 py-2">
                {maskedEmail || '확인할 이메일'}
              </p>
              {expiresAt && (
                <p className="text-[12px] text-[#8A9BB0]">만료 예정: {expiresAt}</p>
              )}
              <button
                onClick={handleConfirm}
                disabled={submitting}
                className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-60 flex items-center justify-center gap-2"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                확인하고 적용하기
              </button>
              <p className="text-[11px] text-[#8A9BB0]">
                본인이 요청하지 않은 경우 이 페이지를 닫아주세요.
              </p>
            </div>
          )}

          {viewState === 'success' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#D1FAE5] flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-7 h-7 text-[#2E7D5E]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">확인이 완료되었습니다.</h2>
              <p className="text-sm text-[#4A5568]">{message || '복구 이메일이 정상적으로 반영되었습니다.'}</p>
              {maskedEmail && (
                <p className="text-sm font-medium text-[#1E2A3A] bg-[#EFF0EC] rounded-lg px-4 py-2">
                  {maskedEmail}
                </p>
              )}
              <Link
                to={primaryLink}
                className="block w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all leading-10 text-center"
              >
                BootSync 열기
              </Link>
            </div>
          )}

          {viewState === 'already_used' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#E8F4F6] flex items-center justify-center mx-auto">
                <Info className="w-7 h-7 text-[#3D7A8A]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">이미 처리된 인증 링크입니다.</h2>
              <p className="text-sm text-[#4A5568]">{message || '이미 인증이 완료된 링크입니다.'}</p>
              <Link
                to={primaryLink}
                className="block w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all leading-10 text-center"
              >
                {isAuthenticated ? '대시보드로 이동' : '로그인하기'}
              </Link>
            </div>
          )}

          {viewState === 'invalid' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#FEE2E2] flex items-center justify-center mx-auto">
                <XCircle className="w-7 h-7 text-[#B91C1C]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">유효하지 않은 인증 링크입니다.</h2>
              <p className="text-sm text-[#4A5568] leading-relaxed">
                {message || '링크가 올바르지 않거나 이미 만료되었습니다. 필요하면 다시 인증 메일을 요청해 주세요.'}
              </p>
              <Link
                to={secondaryLink}
                className="block w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all leading-10 text-center"
              >
                {isAuthenticated ? '설정으로 이동' : '로그인하기'}
              </Link>
            </div>
          )}

          {viewState === 'login_required' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#FEF3C7] flex items-center justify-center mx-auto">
                <Lock className="w-7 h-7 text-[#D97706]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">로그인이 필요합니다.</h2>
              <p className="text-sm text-[#4A5568] leading-relaxed">
                {message || '복구 이메일 변경 링크는 로그인한 상태에서만 확인할 수 있습니다.'}
              </p>
              <p className="text-[12px] text-[#8A9BB0]">
                로그인 후 이 인증 화면으로 다시 돌아옵니다.
              </p>
              <Link
                to={nextLoginTarget}
                className="block w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all leading-10 text-center"
              >
                로그인하러 가기
              </Link>
            </div>
          )}

          {viewState === 'error' && (
            <div className="text-center space-y-4">
              <div className="w-14 h-14 rounded-full bg-[#FEF3C7] flex items-center justify-center mx-auto">
                <Clock className="w-7 h-7 text-[#D97706]" />
              </div>
              <h2 className="text-lg font-semibold text-[#1E2A3A]">잠시 후 다시 시도해 주세요.</h2>
              <p className="text-sm text-[#4A5568] leading-relaxed">
                {message || '인증 정보를 불러오는 중 문제가 발생했습니다.'}
              </p>
              <Link
                to={primaryLink}
                className="block w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] transition-all leading-10 text-center"
              >
                {isAuthenticated ? '대시보드로 이동' : '로그인하기'}
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
