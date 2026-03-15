import { useState, FormEvent } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { Eye, EyeOff, Loader2, AlertCircle, AlertTriangle, Info } from 'lucide-react';
import { useApp } from '@/lib/store';

type BannerType = 'error' | 'warning' | 'info';

interface Banner {
  type: BannerType;
  message: string;
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useApp();
  const nextPath = searchParams.get('next');

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [banner, setBanner] = useState<Banner | null>(() => {
    const reason = searchParams.get('reason');
    if (reason === 'logged_out') return { type: 'info', message: '로그아웃되었습니다.' };
    if (reason === 'session_expired') return { type: 'warning', message: '세션이 만료되어 자동으로 로그아웃되었습니다. 다시 로그인해 주세요.' };
    if (reason === 'pending_delete') return { type: 'info', message: '계정 삭제 요청이 접수되어 현재 세션이 종료되었습니다.' };
    return null;
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;

    setLoading(true);
    setBanner(null);

    const result = await login(username, password);

    if (result.success) {
      const safeNextPath = nextPath && nextPath.startsWith('/') && !nextPath.startsWith('//')
        ? nextPath
        : '/dashboard';
      navigate(safeNextPath);
    } else {
      setLoading(false);
      if (result.error === 'invalid_credentials') {
        setBanner({ type: 'error', message: '아이디 또는 비밀번호가 올바르지 않습니다.' });
      } else if (result.error === 'rate_limit') {
        setBanner({ type: 'warning', message: '로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.' });
      } else if (result.error === 'inactive') {
        setBanner({ type: 'error', message: '비활성화된 계정입니다. 관리자에게 문의하세요.' });
      }
    }
  };

  const bannerStyles: Record<BannerType, string> = {
    error: 'bg-[#FEE2E2] border-[#B91C1C]/20 text-[#991B1B]',
    warning: 'bg-[#FEF3C7] border-[#D97706]/20 text-[#92400E]',
    info: 'bg-[#E8F4F6] border-[#3D7A8A]/20 text-[#1E2A3A]',
  };

  const bannerIcons: Record<BannerType, typeof AlertCircle> = {
    error: AlertCircle,
    warning: AlertTriangle,
    info: Info,
  };

  return (
    <div className="min-h-screen bg-[#F7F6F3] flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[400px]">
        {/* Logo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#1E2A3A] tracking-tight">
            Boot<span className="text-[#3D7A8A]">Sync</span>
          </h1>
          <p className="text-sm text-[#8A9BB0] mt-2">나의 훈련 기록, 한 곳에서</p>
        </div>

        {/* Card */}
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-6">
          {/* Banner */}
          {banner && (
            <div className={`flex items-start gap-2.5 p-3 rounded-lg border mb-5 text-sm ${bannerStyles[banner.type]}`}>
              {(() => {
                const Icon = bannerIcons[banner.type];
                return <Icon className="w-4 h-4 mt-0.5 shrink-0" />;
              })()}
              <span className="leading-relaxed">{banner.message}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Username */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">아이디</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                className="w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 transition-all disabled:opacity-60"
                placeholder="아이디를 입력하세요"
                disabled={loading}
                autoComplete="username"
              />
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="w-full h-10 px-3 pr-10 rounded-lg bg-[#EFF0EC] border border-[#E2E4DF] text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:border-[#3D7A8A] focus:ring-2 focus:ring-[#3D7A8A]/20 transition-all disabled:opacity-60"
                  placeholder="비밀번호를 입력하세요"
                  disabled={loading}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8A9BB0] hover:text-[#4A5568] transition-colors"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading || !username.trim() || !password.trim()}
              className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  로그인 중...
                </>
              ) : (
                '로그인'
              )}
            </button>
          </form>

          {/* Sign up link */}
          <div className="mt-5 pt-5 border-t border-[#E2E4DF] text-center">
            <p className="text-sm text-[#4A5568]">
              계정이 없으신가요?{' '}
              <Link to="/signup" className="text-[#3D7A8A] font-medium hover:underline">
                회원가입
              </Link>
            </p>
          </div>
        </div>

        {/* Footer note */}
        <p className="text-center text-[11px] text-[#8A9BB0] mt-6 leading-relaxed">
          BootSync는 국비지원 IT 훈련생을 위한<br />
          출결 관리 및 학습 도구입니다.
        </p>
      </div>
    </div>
  );
}
