export class AppApiError extends Error {
  code: string;
  status: number;
  fieldErrors: Record<string, string>;
  payload: unknown;

  constructor(params: {
    message: string;
    code?: string;
    status: number;
    fieldErrors?: Record<string, string>;
    payload?: unknown;
  }) {
    super(params.message);
    this.name = "AppApiError";
    this.code = params.code || "api_error";
    this.status = params.status;
    this.fieldErrors = params.fieldErrors || {};
    this.payload = params.payload;
  }
}

export async function readJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text) as T;
}

export async function apiRequest<T>(input: RequestInfo | URL, init: RequestInit = {}): Promise<T | null> {
  const response = await fetch(input, {
    credentials: "same-origin",
    ...init,
  });

  const data = await readJson<
    T & {
      code?: string;
      message?: string;
      fieldErrors?: Record<string, string>;
      secretWarningToken?: string;
    }
  >(response);
  if (!response.ok) {
    throw new AppApiError({
      message: data?.message || "요청을 처리하지 못했습니다.",
      code: data?.code || (data?.secretWarningToken ? "secret_warning_required" : "api_error"),
      status: response.status,
      fieldErrors: data?.fieldErrors,
      payload: data,
    });
  }

  return data;
}
