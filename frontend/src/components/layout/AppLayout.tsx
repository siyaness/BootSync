import { Link, NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, CalendarCheck, Code2, Coins, GraduationCap } from 'lucide-react';
import { useApp } from '@/lib/store';
import { useIsMobile } from '@/hooks/use-mobile';
import { cn } from '@/lib/utils';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: '대시보드' },
  { to: '/course-status', icon: GraduationCap, label: '과정 현황' },
  { to: '/attendance', icon: CalendarCheck, label: '출결 관리' },
  { to: '/allowance', icon: Coins, label: '훈련장려금' },
  { to: '/snippets', icon: Code2, label: '학습 노트' },
];

export default function AppLayout() {
  const { user } = useApp();
  const isMobile = useIsMobile();

  return (
    <div className="min-h-screen bg-[#F7F6F3]">
      {/* Pending deletion banner */}
      {user.accountStatus === 'pending_deletion' && (
        <div className="bg-[#FEF3C7] border-b border-[#D97706]/20 px-4 py-2.5 text-sm text-[#92400E] flex items-center justify-center gap-2 flex-wrap">
          <span>계정 삭제 요청이 접수되었습니다. 삭제 예정일: {user.deletionDate}</span>
        </div>
      )}

      {/* Desktop sidebar */}
      {!isMobile && (
        <aside className="fixed left-0 top-0 w-[220px] h-full bg-[#FDFCFB] border-r border-[#E2E4DF] z-40 flex flex-col">
          <Link to="/dashboard" className="block px-6 py-6 hover:bg-[#F7F6F3] transition-colors">
            <h1 className="text-xl font-bold text-[#1E2A3A] tracking-tight">
              Boot<span className="text-[#3D7A8A]">Sync</span>
            </h1>
            <p className="text-[11px] text-[#8A9BB0] mt-0.5">나의 훈련 기록</p>
          </Link>

          <nav className="flex-1 px-3 space-y-1">
            {navItems.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all',
                    isActive
                      ? 'bg-[#E8F4F6] text-[#3D7A8A]'
                      : 'text-[#4A5568] hover:bg-[#EFF0EC] hover:text-[#1E2A3A]'
                  )
                }
              >
                <item.icon className="w-[18px] h-[18px]" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          <Link to="/settings" className="block p-4 border-t border-[#E2E4DF] hover:bg-[#F7F6F3] transition-colors">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-[#3D7A8A] flex items-center justify-center">
                <span className="text-white text-xs font-semibold">
                  {user.displayName.charAt(0)}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-[#1E2A3A] truncate">{user.displayName}</p>
                <p className="text-[11px] text-[#8A9BB0] truncate">@{user.username}</p>
              </div>
            </div>
          </Link>
        </aside>
      )}

      {/* Mobile top bar */}
      {isMobile && (
        <header className="fixed top-0 left-0 right-0 h-14 bg-[#FDFCFB] border-b border-[#E2E4DF] z-40 flex items-center justify-between px-4">
          <Link to="/dashboard" className="text-lg font-bold text-[#1E2A3A] tracking-tight">
            Boot<span className="text-[#3D7A8A]">Sync</span>
          </Link>
          <Link to="/settings" className="w-8 h-8 rounded-full bg-[#3D7A8A] flex items-center justify-center">
            <span className="text-white text-xs font-semibold">
              {user.displayName.charAt(0)}
            </span>
          </Link>
        </header>
      )}

      {/* Main content */}
      <main
        className={cn(
          isMobile ? 'pt-14 pb-20 px-4' : 'ml-[220px] px-6 lg:px-10 py-8'
        )}
      >
        <div className="max-w-[960px] mx-auto">
          <Outlet />
        </div>
      </main>

      {/* Mobile bottom tab bar */}
      {isMobile && (
        <nav className="fixed bottom-0 left-0 right-0 h-16 bg-[#FDFCFB] border-t border-[#E2E4DF] z-40 flex items-center justify-around px-2 safe-area-pb">
          {navItems.map(item => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex flex-col items-center gap-0.5 px-3 py-1.5 rounded-lg transition-all min-w-[60px]',
                  isActive
                    ? 'text-[#3D7A8A]'
                    : 'text-[#8A9BB0]'
                )
              }
            >
              <item.icon className="w-5 h-5" />
              <span className="text-[10px] font-medium">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      )}
    </div>
  );
}
