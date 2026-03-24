import { Suspense, lazy } from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import { AppProvider, useApp } from "@/lib/store";
import { TooltipProvider } from "@/components/ui/tooltip";

// Lazy-loaded pages
const LoginPage = lazy(() => import("@/pages/LoginPage"));
const SignupPage = lazy(() => import("@/pages/SignupPage"));
const DashboardPage = lazy(() => import("@/pages/DashboardPage"));
const CourseStatusPage = lazy(() => import("@/pages/CourseStatusPage"));
const AttendancePage = lazy(() => import("@/pages/AttendancePage"));
const AllowancePage = lazy(() => import("@/pages/AllowancePage"));
const SnippetsPage = lazy(() => import("@/pages/SnippetsPage"));
const SnippetDetailPage = lazy(() => import("@/pages/SnippetDetailPage"));
const SnippetEditorPage = lazy(() => import("@/pages/SnippetEditorPage"));
const SettingsPage = lazy(() => import("@/pages/SettingsPage"));
const VerifyEmailPage = lazy(() => import("@/pages/VerifyEmailPage"));
const AppLayout = lazy(() => import("@/components/layout/AppLayout"));

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, sessionReady } = useApp();
  const location = useLocation();
  if (!sessionReady) {
    return null;
  }
  if (!isAuthenticated) {
    const nextPath = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?next=${encodeURIComponent(nextPath || "/")}`} replace />;
  }
  return <>{children}</>;
}

function PublicRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, sessionReady } = useApp();
  const location = useLocation();
  if (!sessionReady) {
    return null;
  }
  if (isAuthenticated) {
    const nextPath = new URLSearchParams(location.search).get("next");
    const safeNextPath = nextPath && nextPath.startsWith("/") && !nextPath.startsWith("//")
      ? nextPath
      : "/dashboard";
    return <Navigate to={safeNextPath} replace />;
  }
  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[#F7F6F3] flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-xl font-bold text-[#1E2A3A] tracking-tight mb-2">
              Boot<span className="text-[#3D7A8A]">Sync</span>
            </h1>
            <div className="w-6 h-6 border-2 border-[#3D7A8A] border-t-transparent rounded-full animate-spin mx-auto" />
          </div>
        </div>
      }
    >
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route path="/signup" element={<PublicRoute><SignupPage /></PublicRoute>} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />

        {/* Protected routes with layout */}
        <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="course-status" element={<CourseStatusPage />} />
          <Route path="attendance" element={<AttendancePage />} />
          <Route path="allowance" element={<AllowancePage />} />
          <Route path="snippets" element={<SnippetsPage />} />
          <Route path="snippets/new" element={<SnippetEditorPage />} />
          <Route path="snippets/:id" element={<SnippetDetailPage />} />
          <Route path="snippets/:id/edit" element={<SnippetEditorPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

function App() {
  return (
    <AppProvider>
      <TooltipProvider>
        <AppRoutes />
      </TooltipProvider>
    </AppProvider>
  );
}

export default App;
