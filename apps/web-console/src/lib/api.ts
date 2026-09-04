import type { ZodType } from 'zod';
import { z } from 'zod';

const errorEnvelopeSchema = z.object({
  error: z.object({
    code: z.string().optional(),
    message: z.string(),
    correlationId: z.string().optional(),
  }),
});

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly correlationId: string | undefined;

  constructor(
    message: string,
    options: { status: number; code?: string; correlationId?: string },
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = options.status;
    this.code = options.code;
    this.correlationId = options.correlationId;
  }
}

function readCookie(name: string): string | undefined {
  const prefix = `${encodeURIComponent(name)}=`;
  return document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
    ?.slice(prefix.length);
}

async function parseError(response: Response): Promise<ApiError> {
  const fallback = `Request failed with status ${response.status}.`;

  try {
    const body: unknown = await response.json();
    const parsed = errorEnvelopeSchema.safeParse(body);
    if (parsed.success) {
      return new ApiError(parsed.data.error.message, {
        status: response.status,
        ...(parsed.data.error.code === undefined ? {} : { code: parsed.data.error.code }),
        ...(parsed.data.error.correlationId === undefined
          ? {}
          : { correlationId: parsed.data.error.correlationId }),
      });
    }
  } catch {
    // A non-JSON error is represented by a safe, status-only message below.
  }

  return new ApiError(fallback, { status: response.status });
}

export async function getApi<T>(path: string, schema: ZodType<T>): Promise<T> {
  return requestApi(path, schema, { method: 'GET' });
}

export async function postCommand<TRequest extends object, TResponse>(
  path: string,
  payload: TRequest,
  schema: ZodType<TResponse>,
  idempotencyKey: string,
): Promise<TResponse> {
  if (idempotencyKey.length < 8) {
    throw new TypeError('A stable idempotency key is required for financial commands.');
  }

  return requestApi(path, schema, {
    method: 'POST',
    body: JSON.stringify(payload),
    headers: { 'Idempotency-Key': idempotencyKey },
  });
}

async function requestApi<T>(
  path: string,
  schema: ZodType<T>,
  init: RequestInit,
): Promise<T> {
  if (!path.startsWith('/api/v1/')) {
    throw new TypeError('API requests must use the same-origin /api/v1 boundary.');
  }

  const csrfToken = readCookie('XSRF-TOKEN');
  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    signal: AbortSignal.timeout(15_000),
    headers: {
      Accept: 'application/json',
      ...(init.body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(csrfToken === undefined ? {} : { 'X-XSRF-TOKEN': decodeURIComponent(csrfToken) }),
      ...init.headers,
    },
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  const envelope: unknown = await response.json();
  return z.object({ data: schema }).parse(envelope).data;
}

export function toErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.correlationId === undefined
      ? error.message
      : `${error.message} Correlation: ${error.correlationId}`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred.';
}
