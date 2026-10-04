import "server-only";
import createClient, { type Client } from "openapi-fetch";
import { getEnv } from "@/env";
import { ApiError, CLIENT_ERROR_CODES, errorFromResponse } from "./errors";
import type { paths } from "./schema";

export type BackendClient = Client<paths>;

/** How long one backend call may take before we give up and report the backend as unreachable. */
export const BACKEND_TIMEOUT_MS = 10_000;

/**
 * A typed client for the backend. Paths, parameters, bodies and responses all come from the generated
 * `schema.d.ts`, so a call that doesn't match the spec doesn't compile. Server only: the browser never talks to the
 * backend (admin/CLAUDE.md).
 */
export function createBackendClient(
  baseUrl: string,
  options: { fetch?: typeof globalThis.fetch; timeoutMs?: number } = {},
): BackendClient {
  const fetchImpl = options.fetch ?? globalThis.fetch;
  const timeoutMs = options.timeoutMs ?? BACKEND_TIMEOUT_MS;
  return createClient<paths>({
    baseUrl,
    fetch: (request) =>
      fetchImpl(
        new Request(request, {
          signal: AbortSignal.any([request.signal, AbortSignal.timeout(timeoutMs)]),
          // The backend never redirects; refusing keeps login tokens from following one to another host.
          redirect: "error",
        }),
      ),
  });
}

let shared: BackendClient | undefined;

/** The client for the configured backend (`API_BASE_URL`). */
export function backend(): BackendClient {
  shared ??= createBackendClient(getEnv().API_BASE_URL);
  return shared;
}

/**
 * Waits for a call and returns its typed body, or throws an [ApiError] for every kind of failure:
 * the backend's error envelope, an error without the envelope, broken JSON, or no answer at all.
 *
 * ```ts
 * const health = await unwrap(backend().GET("/health/live"));
 * ```
 */
export async function unwrap<D>(call: Promise<{ data?: D; error?: unknown; response: Response }>): Promise<D> {
  let result: Awaited<typeof call>;
  try {
    result = await call;
  } catch (cause) {
    // openapi-fetch rethrows network failures and aborts as-is, and JSON.parse errors for a broken 2xx body.
    const code = cause instanceof SyntaxError ? CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE : CLIENT_ERROR_CODES.BACKEND_UNREACHABLE;
    throw new ApiError(code, undefined, undefined, { cause });
  }
  const { data, error, response } = result;
  if (!response.ok) {
    throw errorFromResponse(error, response);
  }
  return data as D;
}
