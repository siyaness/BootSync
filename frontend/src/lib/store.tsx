import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { AppApiError, apiRequest } from "@/lib/api";
import type {
  AllowanceSummary,
  AttendanceBulkFillResult,
  AttendanceMonthlySummary,
  AttendanceRecord,
  AttendanceStatus,
  Snippet,
  TrainingProfileDraft,
  TrainingProfileSettings,
  TrainingSummary,
  User,
} from "@/lib/app-types";

type StoredAttendanceRecord = AttendanceRecord & { id: string };

type SessionResponse = {
  authenticated: boolean;
  csrf: {
    headerName: string;
    parameterName: string;
    token: string;
  };
  user: {
    username: string;
    displayName: string;
    recoveryEmail: string | null;
    emailVerified: boolean;
    accountStatus: "active" | "pending_deletion";
    deletionDate: string | null;
  } | null;
  recoveryEmailStatus: {
    hasVerifiedRecoveryEmail: boolean;
    maskedVerifiedRecoveryEmail: string | null;
    hasPendingVerification: boolean;
    maskedPendingRecoveryEmail: string | null;
    pendingPurposeLabel: string | null;
    pendingVerificationExpiresAt: string | null;
    developmentPreviewPath: string | null;
  } | null;
};

type AttendanceMonthResponse = {
  yearMonth: string;
  monthlySummary: AttendanceMonthlySummary;
  allowanceSummary: AllowanceSummary;
  trainingSummary: TrainingSummary | null;
  records: Array<{
    id: number;
    date: string;
    status: AttendanceStatus;
    memo?: string | null;
  }>;
};

type SnippetResponse = {
  id: number;
  title: string;
  content: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
};

interface LoginResult {
  success: boolean;
  error?: "invalid_credentials" | "rate_limit" | "inactive";
}

interface SignupResult {
  success: boolean;
  error?: string;
  fieldErrors?: Record<string, string>;
}

interface RecoveryEmailStatus {
  hasVerifiedRecoveryEmail: boolean;
  maskedVerifiedRecoveryEmail: string | null;
  hasPendingVerification: boolean;
  maskedPendingRecoveryEmail: string | null;
  pendingPurposeLabel: string | null;
  pendingVerificationExpiresAt: string | null;
  developmentPreviewPath: string | null;
}

interface AppState {
  sessionReady: boolean;
  isAuthenticated: boolean;
  user: User;
  recoveryEmailStatus: RecoveryEmailStatus;
  attendance: AttendanceRecord[];
  snippets: Snippet[];
  trainingProfile: TrainingProfileSettings | null;
  login: (username: string, password: string) => Promise<LoginResult>;
  signup: (data: {
    username: string;
    password: string;
    displayName: string;
    recoveryEmail: string;
  }) => Promise<SignupResult>;
  logout: () => Promise<void>;
  loadAttendanceMonth: (year: number, month: number) => Promise<void>;
  loadSnippets: () => Promise<void>;
  setAttendance: (date: string, status: AttendanceStatus, memo?: string) => Promise<void>;
  deleteAttendance: (date: string) => Promise<void>;
  previewBulkFillPresentForTrainingDays: () => Promise<AttendanceBulkFillResult>;
  bulkFillPresentForTrainingDays: () => Promise<AttendanceBulkFillResult>;
  getAttendanceForDate: (date: string) => AttendanceRecord | undefined;
  getAttendanceForMonth: (year: number, month: number) => AttendanceRecord[];
  getAttendanceSummaryForMonth: (year: number, month: number) => AttendanceMonthlySummary | undefined;
  getAllowanceSummaryForMonth: (year: number, month: number) => AllowanceSummary | undefined;
  getTrainingSummaryForMonth: (year: number, month: number) => TrainingSummary | undefined;
  addSnippet: (
    snippet: Omit<Snippet, "id" | "createdAt" | "updatedAt">,
    secretWarningToken?: string
  ) => Promise<Snippet>;
  updateSnippet: (
    id: string,
    data: Partial<Omit<Snippet, "id" | "createdAt">>,
    secretWarningToken?: string
  ) => Promise<void>;
  deleteSnippet: (id: string) => Promise<void>;
  getSnippet: (id: string) => Snippet | undefined;
  updateDisplayName: (name: string) => Promise<void>;
  changePassword: (currentPassword: string, newPassword: string, newPasswordConfirm: string) => Promise<void>;
  loadTrainingProfile: () => Promise<void>;
  updateTrainingProfile: (profile: TrainingProfileDraft) => Promise<void>;
  clearTrainingProfile: () => Promise<void>;
  updateRecoveryEmail: (email: string, currentPassword: string) => Promise<void>;
  verifyEmail: () => Promise<void>;
  resendVerification: () => Promise<void>;
  requestAccountDeletion: (currentPassword: string) => Promise<void>;
  refreshSession: () => Promise<void>;
}

const EMPTY_USER: User = {
  username: "",
  displayName: "",
  recoveryEmail: "",
  emailVerified: false,
  accountStatus: "active",
};

const EMPTY_RECOVERY_EMAIL_STATUS: RecoveryEmailStatus = {
  hasVerifiedRecoveryEmail: false,
  maskedVerifiedRecoveryEmail: null,
  hasPendingVerification: false,
  maskedPendingRecoveryEmail: null,
  pendingPurposeLabel: null,
  pendingVerificationExpiresAt: null,
  developmentPreviewPath: null,
};

const AppContext = createContext<AppState | null>(null);

function formatYearMonth(year: number, month: number) {
  return `${year}-${String(month + 1).padStart(2, "0")}`;
}

function mapUser(session: SessionResponse): User {
  if (!session.user) {
    return EMPTY_USER;
  }

  return {
    username: session.user.username,
    displayName: session.user.displayName,
    recoveryEmail: session.user.recoveryEmail || "",
    emailVerified: session.user.emailVerified,
    accountStatus: session.user.accountStatus,
    deletionDate: session.user.deletionDate || undefined,
  };
}

function mapSnippet(snippet: SnippetResponse): Snippet {
  return {
    id: String(snippet.id),
    title: snippet.title,
    content: snippet.content,
    tags: snippet.tags,
    createdAt: snippet.createdAt,
    updatedAt: snippet.updatedAt,
  };
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [sessionReady, setSessionReady] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState<User>(EMPTY_USER);
  const [recoveryEmailStatus, setRecoveryEmailStatus] = useState<RecoveryEmailStatus>(EMPTY_RECOVERY_EMAIL_STATUS);
  const [attendanceRecords, setAttendanceRecords] = useState<StoredAttendanceRecord[]>([]);
  const [attendanceMonthlySummaries, setAttendanceMonthlySummaries] = useState<Record<string, AttendanceMonthlySummary>>({});
  const [attendanceAllowanceSummaries, setAttendanceAllowanceSummaries] = useState<Record<string, AllowanceSummary>>({});
  const [attendanceTrainingSummaries, setAttendanceTrainingSummaries] = useState<Record<string, TrainingSummary>>({});
  const [snippets, setSnippets] = useState<Snippet[]>([]);
  const [trainingProfile, setTrainingProfile] = useState<TrainingProfileSettings | null>(null);

  const csrfRef = useRef<{ headerName: string; token: string } | null>(null);

  const clearLocalState = useCallback(() => {
    csrfRef.current = null;
    setIsAuthenticated(false);
    setUser(EMPTY_USER);
    setRecoveryEmailStatus(EMPTY_RECOVERY_EMAIL_STATUS);
    setAttendanceRecords([]);
    setAttendanceMonthlySummaries({});
    setAttendanceAllowanceSummaries({});
    setAttendanceTrainingSummaries({});
    setSnippets([]);
    setTrainingProfile(null);
  }, []);

  const mergeAttendanceMonth = useCallback((response: AttendanceMonthResponse) => {
    setAttendanceMonthlySummaries(prev => ({
      ...prev,
      [response.yearMonth]: response.monthlySummary,
    }));
    setAttendanceAllowanceSummaries(prev => ({
      ...prev,
      [response.yearMonth]: response.allowanceSummary,
    }));
    if (response.trainingSummary) {
      setAttendanceTrainingSummaries(prev => ({
        ...prev,
        [response.yearMonth]: response.trainingSummary,
      }));
    } else {
      setAttendanceTrainingSummaries(prev => {
        const next = { ...prev };
        delete next[response.yearMonth];
        return next;
      });
    }
    setAttendanceRecords(prev => {
      const retained = prev.filter(record => !record.date.startsWith(response.yearMonth));
      const nextMonth = response.records.map(record => ({
        id: String(record.id),
        date: record.date,
        status: record.status,
        memo: record.memo || undefined,
      }));

      return [...retained, ...nextMonth].sort((a, b) => a.date.localeCompare(b.date));
    });
  }, []);

  const loadAttendanceMonth = useCallback(async (year: number, month: number) => {
    const response = await apiRequest<AttendanceMonthResponse>(
      `/api/attendance?yearMonth=${formatYearMonth(year, month)}`
    );
    if (response) {
      mergeAttendanceMonth(response);
    }
  }, [mergeAttendanceMonth]);

  const loadSnippets = useCallback(async () => {
    const response = await apiRequest<SnippetResponse[]>("/api/snippets");
    setSnippets((response || []).map(mapSnippet));
  }, []);

  const refreshSession = useCallback(async () => {
    const session = await apiRequest<SessionResponse>("/api/auth/session");
    if (!session) {
      clearLocalState();
      setSessionReady(true);
      return;
    }

    csrfRef.current = {
      headerName: session.csrf.headerName,
      token: session.csrf.token,
    };

    if (!session.authenticated) {
      clearLocalState();
      setSessionReady(true);
      return;
    }

    setIsAuthenticated(true);
    setUser(mapUser(session));
    setRecoveryEmailStatus(session.recoveryEmailStatus || EMPTY_RECOVERY_EMAIL_STATUS);
    setSessionReady(true);

    const now = new Date();
    await Promise.allSettled([
      loadAttendanceMonth(now.getFullYear(), now.getMonth()),
      loadSnippets(),
    ]);
  }, [clearLocalState, loadAttendanceMonth, loadSnippets]);

  const ensureCsrf = useCallback(async () => {
    if (!csrfRef.current) {
      const session = await apiRequest<SessionResponse>("/api/auth/session");
      if (session) {
        csrfRef.current = {
          headerName: session.csrf.headerName,
          token: session.csrf.token,
        };
      }
    }

    if (!csrfRef.current) {
      throw new AppApiError({
        message: "보안 토큰을 초기화하지 못했습니다.",
        status: 400,
      });
    }

    return csrfRef.current;
  }, []);

  const requestWithCsrf = useCallback(async <T,>(
    url: string,
    method: string,
    body?: unknown
  ) => {
    const csrf = await ensureCsrf();
    const headers: HeadersInit = {
      [csrf.headerName]: csrf.token,
    };

    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    return apiRequest<T>(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }, [ensureCsrf]);

  const login = useCallback(async (username: string, password: string): Promise<LoginResult> => {
    try {
      await requestWithCsrf("/api/auth/login", "POST", { username, password });
      await refreshSession();
      return { success: true };
    } catch (error) {
      if (error instanceof AppApiError) {
        if (error.code === "rate_limit") {
          return { success: false, error: "rate_limit" };
        }
        if (error.code === "inactive_account") {
          return { success: false, error: "inactive" };
        }
        return { success: false, error: "invalid_credentials" };
      }
      throw error;
    }
  }, [refreshSession, requestWithCsrf]);

  const signup = useCallback(async (data: {
    username: string;
    password: string;
    displayName: string;
    recoveryEmail: string;
  }): Promise<SignupResult> => {
    try {
      await requestWithCsrf("/api/auth/signup", "POST", data);
      await refreshSession();
      return { success: true };
    } catch (error) {
      if (error instanceof AppApiError) {
        return {
          success: false,
          error: error.message,
          fieldErrors: error.fieldErrors,
        };
      }
      throw error;
    }
  }, [refreshSession, requestWithCsrf]);

  const logout = useCallback(async () => {
    await requestWithCsrf("/api/auth/logout", "POST");
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  const setAttendance = useCallback(async (date: string, status: AttendanceStatus, memo?: string) => {
    const response = await requestWithCsrf<AttendanceMonthResponse>(`/api/attendance/${date}`, "PUT", {
      status,
      memo: memo || "",
    });
    if (response) {
      mergeAttendanceMonth(response);
    }
  }, [mergeAttendanceMonth, requestWithCsrf]);

  const deleteAttendance = useCallback(async (date: string) => {
    const target = attendanceRecords.find(record => record.date === date);
    if (!target) {
      return;
    }
    const response = await requestWithCsrf<AttendanceMonthResponse>(`/api/attendance/${target.id}`, "DELETE");
    if (response) {
      mergeAttendanceMonth(response);
    }
  }, [attendanceRecords, mergeAttendanceMonth, requestWithCsrf]);

  const bulkFillPresentForTrainingDays = useCallback(async () => {
    const response = await requestWithCsrf<AttendanceBulkFillResult>("/api/attendance/bulk-fill/present", "POST");
    if (!response) {
      throw new AppApiError({
        message: "빈 수업일 일괄 출석 처리를 완료하지 못했습니다.",
        status: 500,
      });
    }
    return response;
  }, [requestWithCsrf]);

  const previewBulkFillPresentForTrainingDays = useCallback(async () => {
    const response = await apiRequest<AttendanceBulkFillResult>("/api/attendance/bulk-fill/present/preview");
    if (!response) {
      throw new AppApiError({
        message: "빈 수업일 일괄 출석 미리보기를 불러오지 못했습니다.",
        status: 500,
      });
    }
    return response;
  }, []);

  const getAttendanceForDate = useCallback((date: string) => {
    const record = attendanceRecords.find(item => item.date === date);
    if (!record) {
      return undefined;
    }
    return {
      date: record.date,
      status: record.status,
      memo: record.memo,
    } satisfies AttendanceRecord;
  }, [attendanceRecords]);

  const getAttendanceForMonth = useCallback((year: number, month: number) => {
    const prefix = formatYearMonth(year, month);
    return attendanceRecords
      .filter(record => record.date.startsWith(prefix))
      .map(record => ({
        date: record.date,
        status: record.status,
        memo: record.memo,
      }));
  }, [attendanceRecords]);

  const getAttendanceSummaryForMonth = useCallback((year: number, month: number) => {
    return attendanceMonthlySummaries[formatYearMonth(year, month)];
  }, [attendanceMonthlySummaries]);

  const getAllowanceSummaryForMonth = useCallback((year: number, month: number) => {
    return attendanceAllowanceSummaries[formatYearMonth(year, month)];
  }, [attendanceAllowanceSummaries]);

  const getTrainingSummaryForMonth = useCallback((year: number, month: number) => {
    return attendanceTrainingSummaries[formatYearMonth(year, month)];
  }, [attendanceTrainingSummaries]);

  const addSnippet = useCallback(async (
    snippet: Omit<Snippet, "id" | "createdAt" | "updatedAt">,
    secretWarningToken = ""
  ) => {
    const response = await requestWithCsrf<SnippetResponse>("/api/snippets", "POST", {
      title: snippet.title,
      content: snippet.content,
      tags: snippet.tags,
      secretWarningToken,
    });

    if (!response) {
      throw new AppApiError({
        message: "학습 노트를 저장하지 못했습니다.",
        status: 500,
      });
    }

    const savedSnippet = mapSnippet(response);
    setSnippets(prev => [savedSnippet, ...prev.filter(item => item.id !== savedSnippet.id)]);
    return savedSnippet;
  }, [requestWithCsrf]);

  const updateSnippet = useCallback(async (
    id: string,
    data: Partial<Omit<Snippet, "id" | "createdAt">>,
    secretWarningToken = ""
  ) => {
    const existing = snippets.find(snippet => snippet.id === id);
    if (!existing) {
      return;
    }

    const response = await requestWithCsrf<SnippetResponse>(`/api/snippets/${id}`, "PUT", {
      title: data.title ?? existing.title,
      content: data.content ?? existing.content,
      tags: data.tags ?? existing.tags,
      secretWarningToken,
    });

    if (!response) {
      return;
    }

    const updated = mapSnippet(response);
    setSnippets(prev =>
      prev
        .map(snippet => (snippet.id === id ? updated : snippet))
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    );
  }, [requestWithCsrf, snippets]);

  const deleteSnippet = useCallback(async (id: string) => {
    await requestWithCsrf(`/api/snippets/${id}`, "DELETE");
    setSnippets(prev => prev.filter(snippet => snippet.id !== id));
  }, [requestWithCsrf]);

  const getSnippet = useCallback((id: string) => {
    return snippets.find(snippet => snippet.id === id);
  }, [snippets]);

  const updateDisplayName = useCallback(async (name: string) => {
    await requestWithCsrf("/api/settings/profile", "PATCH", { displayName: name });
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  const changePassword = useCallback(async (
    currentPassword: string,
    newPassword: string,
    newPasswordConfirm: string
  ) => {
    await requestWithCsrf("/api/settings/password", "POST", {
      currentPassword,
      newPassword,
      newPasswordConfirm,
    });
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  const loadTrainingProfile = useCallback(async () => {
    const response = await apiRequest<TrainingProfileSettings>("/api/settings/training-profile");
    setTrainingProfile(response);
  }, []);

  const updateTrainingProfile = useCallback(async (profile: TrainingProfileDraft) => {
    const response = await requestWithCsrf<TrainingProfileSettings>("/api/settings/training-profile", "PUT", profile);
    if (response) {
      setTrainingProfile(response);
    }
    setAttendanceMonthlySummaries({});
    setAttendanceAllowanceSummaries({});
    setAttendanceTrainingSummaries({});
    const now = new Date();
    await loadAttendanceMonth(now.getFullYear(), now.getMonth());
  }, [loadAttendanceMonth, requestWithCsrf]);

  const clearTrainingProfile = useCallback(async () => {
    await requestWithCsrf("/api/settings/training-profile", "DELETE");
    const response = await apiRequest<TrainingProfileSettings>("/api/settings/training-profile");
    setTrainingProfile(response);
    setAttendanceMonthlySummaries({});
    setAttendanceAllowanceSummaries({});
    setAttendanceTrainingSummaries({});
    const now = new Date();
    await loadAttendanceMonth(now.getFullYear(), now.getMonth());
  }, [loadAttendanceMonth, requestWithCsrf]);

  const updateRecoveryEmail = useCallback(async (email: string, currentPassword: string) => {
    await requestWithCsrf("/api/settings/recovery-email", "POST", {
      newRecoveryEmail: email,
      currentPassword,
    });
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  const verifyEmail = useCallback(async () => {
    await refreshSession();
  }, [refreshSession]);

  const resendVerification = useCallback(async () => {
    await requestWithCsrf("/api/settings/recovery-email/resend", "POST");
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  const requestAccountDeletion = useCallback(async (currentPassword: string) => {
    await requestWithCsrf("/api/settings/account-deletion", "POST", {
      currentPassword,
    });
    await refreshSession();
  }, [refreshSession, requestWithCsrf]);

  useEffect(() => {
    refreshSession().catch(() => {
      setSessionReady(true);
    });
  }, [refreshSession]);

  return (
    <AppContext.Provider
      value={{
        sessionReady,
        isAuthenticated,
        user,
        recoveryEmailStatus,
        attendance: attendanceRecords.map(record => ({
          date: record.date,
          status: record.status,
          memo: record.memo,
        })),
        snippets,
        trainingProfile,
        login,
        signup,
        logout,
        loadAttendanceMonth,
        loadSnippets,
        setAttendance,
        deleteAttendance,
        previewBulkFillPresentForTrainingDays,
        bulkFillPresentForTrainingDays,
        getAttendanceForDate,
        getAttendanceForMonth,
        getAttendanceSummaryForMonth,
        getAllowanceSummaryForMonth,
        getTrainingSummaryForMonth,
        addSnippet,
        updateSnippet,
        deleteSnippet,
        getSnippet,
        updateDisplayName,
        changePassword,
        loadTrainingProfile,
        updateTrainingProfile,
        clearTrainingProfile,
        updateRecoveryEmail,
        verifyEmail,
        resendVerification,
        requestAccountDeletion,
        refreshSession,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error("useApp must be used within AppProvider");
  }
  return ctx;
}
