import { useState, FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff, Loader2, Info, Check, X } from 'lucide-react';
import { useApp } from '@/lib/store';
import { cn } from '@/lib/utils';

interface FieldErrors {
  username?: string;
  password?: string;
  passwordConfirm?: string;
  displayName?: string;
  recoveryEmail?: string;
}

function getPasswordStrength(password: string): { label: string; color: string; width: string } {
  if (password.length === 0) return { label: '', color: '', width: '0%' };
  if (password.length < 6) return { label: '약함', color: '#B91C1C', width: '25%' };
  if (password.length < 10) return { label: '보통', color: '#D97706', width: '60%' };
  if (/[A-Z]/.test(password) && /[0-9]/.test(password) && /[^A-Za-z0-9]/.test(password)) {
    return { label: '강함', color: '#2E7D5E', width: '100%' };
  }
  return { label: '보통', color: '#D97706', width: '60%' };
}

export default function SignupPage() {
  const navigate = useNavigate();
  const { signup } = useApp();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [recoveryEmail, setRecoveryEmail] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const validateField = (field: string, value: string): string | undefined => {
    switch (field) {
      case 'username':
        if (!value.trim()) return '아이디를 입력해 주세요.';
        if (value.length < 4 || value.length > 20) return '4~20자 사이로 입력해 주세요.';
        if (!/^[a-z0-9_]+$/.test(value)) return '영문 소문자, 숫자, 언더스코어만 사용할 수 있습니다.';
        return undefined;
      case 'password':
        if (!value) return '비밀번호를 입력해 주세요.';
        if (value.length < 10) return '10자 이상 입력해 주세요.';
        return undefined;
      case 'passwordConfirm':
        if (!value) return '비밀번호 확인을 입력해 주세요.';
        if (value !== password) return '비밀번호가 일치하지 않습니다.';
        return undefined;
      case 'displayName':
        if (!value.trim()) return '표시 이름을 입력해 주세요.';
        if (value.trim().length < 2) return '2자 이상 입력해 주세요.';
        return undefined;
      case 'recoveryEmail':
        if (!value.trim()) return '복구 이메일을 입력해 주세요.';
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '올바른 이메일 형식이 아닙니다.';
        return undefined;
      default:
        return undefined;
    }
  };

  const handleBlur = (field: string, value: string) => {
    setTouched(prev => ({ ...prev, [field]: true }));
    setErrors(prev => ({ ...prev, [field]: validateField(field, value) }));
  };

  const isValid =
    !validateField('username', username) &&
    !validateField('password', password) &&
    !validateField('passwordConfirm', passwordConfirm) &&
    !validateField('displayName', displayName) &&
    !validateField('recoveryEmail', recoveryEmail);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const allTouched: Record<string, boolean> = {
      username: true, password: true, passwordConfirm: true,
      displayName: true, recoveryEmail: true,
    };
    setTouched(allTouched);

    const newErrors: FieldErrors = {
      username: validateField('username', username),
      password: validateField('password', password),
      passwordConfirm: validateField('passwordConfirm', passwordConfirm),
      displayName: validateField('displayName', displayName),
      recoveryEmail: validateField('recoveryEmail', recoveryEmail),
    };
    setErrors(newErrors);

    if (Object.values(newErrors).some(e => e)) return;

    setLoading(true);
    const result = await signup({ username, password, displayName, recoveryEmail });
    if (result.success) {
      navigate('/dashboard?welcome=true');
      return;
    }

    setLoading(false);
    if (result.fieldErrors) {
      setErrors(prev => ({ ...prev, ...result.fieldErrors }));
    }
  };

  const pwStrength = getPasswordStrength(password);

  return (
    <div className="min-h-screen bg-[#F7F6F3] flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-[440px]">
        {/* Logo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-[#1E2A3A] tracking-tight">
            Boot<span className="text-[#3D7A8A]">Sync</span>
          </h1>
          <p className="text-sm text-[#8A9BB0] mt-2">회원가입</p>
        </div>

        {/* Card */}
        <div className="bg-[#FDFCFB] rounded-xl border border-[#E2E4DF] shadow-[0_1px_3px_rgba(30,42,58,0.06)] p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Username */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">아이디</label>
              <input
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value.toLowerCase())}
                onBlur={() => handleBlur('username', username)}
                className={cn(
                  'w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:ring-2 transition-all',
                  touched.username && errors.username
                    ? 'border-[#B91C1C] focus:ring-[#B91C1C]/20'
                    : 'border-[#E2E4DF] focus:border-[#3D7A8A] focus:ring-[#3D7A8A]/20'
                )}
                placeholder="4~20자, 영문 소문자/숫자/언더스코어"
                disabled={loading}
              />
              {touched.username && errors.username && (
                <p className="text-[12px] text-[#B91C1C] flex items-center gap-1">
                  <X className="w-3 h-3" /> {errors.username}
                </p>
              )}
              {touched.username && !errors.username && username && (
                <p className="text-[12px] text-[#2E7D5E] flex items-center gap-1">
                  <Check className="w-3 h-3" /> 사용 가능한 아이디입니다.
                </p>
              )}
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  onBlur={() => handleBlur('password', password)}
                  className={cn(
                    'w-full h-10 px-3 pr-10 rounded-lg bg-[#EFF0EC] border text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:ring-2 transition-all',
                    touched.password && errors.password
                      ? 'border-[#B91C1C] focus:ring-[#B91C1C]/20'
                      : 'border-[#E2E4DF] focus:border-[#3D7A8A] focus:ring-[#3D7A8A]/20'
                  )}
                  placeholder="10자 이상"
                  disabled={loading}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8A9BB0] hover:text-[#4A5568]"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {password && (
                <div className="space-y-1">
                  <div className="h-1.5 bg-[#EFF0EC] rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-300"
                      style={{ width: pwStrength.width, backgroundColor: pwStrength.color }}
                    />
                  </div>
                  <p className="text-[11px]" style={{ color: pwStrength.color }}>
                    비밀번호 강도: {pwStrength.label}
                  </p>
                </div>
              )}
              {touched.password && errors.password && (
                <p className="text-[12px] text-[#B91C1C] flex items-center gap-1">
                  <X className="w-3 h-3" /> {errors.password}
                </p>
              )}
            </div>

            {/* Password Confirm */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">비밀번호 확인</label>
              <div className="relative">
                <input
                  type={showPasswordConfirm ? 'text' : 'password'}
                  value={passwordConfirm}
                  onChange={e => setPasswordConfirm(e.target.value)}
                  onBlur={() => handleBlur('passwordConfirm', passwordConfirm)}
                  className={cn(
                    'w-full h-10 px-3 pr-10 rounded-lg bg-[#EFF0EC] border text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:ring-2 transition-all',
                    touched.passwordConfirm && errors.passwordConfirm
                      ? 'border-[#B91C1C] focus:ring-[#B91C1C]/20'
                      : 'border-[#E2E4DF] focus:border-[#3D7A8A] focus:ring-[#3D7A8A]/20'
                  )}
                  placeholder="비밀번호를 다시 입력하세요"
                  disabled={loading}
                />
                <button
                  type="button"
                  onClick={() => setShowPasswordConfirm(!showPasswordConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#8A9BB0] hover:text-[#4A5568]"
                  tabIndex={-1}
                >
                  {showPasswordConfirm ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {touched.passwordConfirm && errors.passwordConfirm && (
                <p className="text-[12px] text-[#B91C1C] flex items-center gap-1">
                  <X className="w-3 h-3" /> {errors.passwordConfirm}
                </p>
              )}
              {touched.passwordConfirm && !errors.passwordConfirm && passwordConfirm && (
                <p className="text-[12px] text-[#2E7D5E] flex items-center gap-1">
                  <Check className="w-3 h-3" /> 비밀번호가 일치합니다.
                </p>
              )}
            </div>

            {/* Display Name */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">표시 이름</label>
              <input
                type="text"
                value={displayName}
                onChange={e => setDisplayName(e.target.value)}
                onBlur={() => handleBlur('displayName', displayName)}
                className={cn(
                  'w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:ring-2 transition-all',
                  touched.displayName && errors.displayName
                    ? 'border-[#B91C1C] focus:ring-[#B91C1C]/20'
                    : 'border-[#E2E4DF] focus:border-[#3D7A8A] focus:ring-[#3D7A8A]/20'
                )}
                placeholder="앱 내에서 표시되는 이름입니다"
                disabled={loading}
              />
              {touched.displayName && errors.displayName && (
                <p className="text-[12px] text-[#B91C1C] flex items-center gap-1">
                  <X className="w-3 h-3" /> {errors.displayName}
                </p>
              )}
            </div>

            {/* Recovery Email */}
            <div className="space-y-1.5">
              <label className="text-sm font-medium text-[#1E2A3A]">복구 이메일</label>
              <input
                type="email"
                value={recoveryEmail}
                onChange={e => setRecoveryEmail(e.target.value)}
                onBlur={() => handleBlur('recoveryEmail', recoveryEmail)}
                className={cn(
                  'w-full h-10 px-3 rounded-lg bg-[#EFF0EC] border text-sm text-[#1E2A3A] placeholder:text-[#8A9BB0] focus:outline-none focus:ring-2 transition-all',
                  touched.recoveryEmail && errors.recoveryEmail
                    ? 'border-[#B91C1C] focus:ring-[#B91C1C]/20'
                    : 'border-[#E2E4DF] focus:border-[#3D7A8A] focus:ring-[#3D7A8A]/20'
                )}
                placeholder="example@email.com"
                disabled={loading}
              />
              {touched.recoveryEmail && errors.recoveryEmail && (
                <p className="text-[12px] text-[#B91C1C] flex items-center gap-1">
                  <X className="w-3 h-3" /> {errors.recoveryEmail}
                </p>
              )}
              <div className="flex items-start gap-2 p-3 bg-[#E8F4F6] rounded-lg text-[12px] text-[#1E2A3A] leading-relaxed">
                <Info className="w-3.5 h-3.5 mt-0.5 shrink-0 text-[#3D7A8A]" />
                <span>
                  복구 이메일은 로그인 ID가 아닙니다. 비밀번호 분실 시 계정 복구에만 사용됩니다. 가입 후 인증 메일이 발송됩니다.
                </span>
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading || !isValid}
              className="w-full h-10 rounded-lg bg-[#3D7A8A] text-white text-sm font-medium hover:bg-[#346A78] active:scale-[0.97] transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-2"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  가입 중...
                </>
              ) : (
                '회원가입'
              )}
            </button>
          </form>

          {/* Login link */}
          <div className="mt-5 pt-5 border-t border-[#E2E4DF] text-center">
            <p className="text-sm text-[#4A5568]">
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-[#3D7A8A] font-medium hover:underline">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
