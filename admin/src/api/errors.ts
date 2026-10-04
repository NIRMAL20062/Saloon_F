import { z } from "zod";

/** Codes the admin server uses when there is no backend error envelope to read one from. */
export const CLIENT_ERROR_CODES = {
  /** No answer: connection refused, DNS failure or timeout. */
  BACKEND_UNREACHABLE: "BACKEND_UNREACHABLE",
  /** An answer that isn't what the spec promises (e.g. an error without the envelope, or broken JSON). */
  UNEXPECTED_RESPONSE: "UNEXPECTED_RESPONSE",
} as const;

/**
 * The one error type for every failed backend call. Pages branch on [code] (the backend's `error.code`, or one of
 * [CLIENT_ERROR_CODES]) and show our own text plus [requestId]; the backend's message is deliberately not kept, so it
 * can't end up on screen.
 */
export class ApiError extends Error {
  override readonly name = "ApiError";

  constructor(
    readonly code: string,
    /** HTTP status, or undefined when the backend never answered. */
    readonly status: number | undefined,
    readonly requestId: string | undefined,
    options?: { cause?: unknown },
  ) {
    super(
      `Backend call failed: ${code}${status === undefined ? "" : ` (HTTP ${status})`}` +
        (requestId === undefined ? "" : ` [request ${requestId}]`),
      options,
    );
  }
}

/** Runtime check of the `ErrorResponse` envelope (docs/api/openapi.yaml): only the fields we rely on. */
const envelope = z.object({
  error: z.object({
    code: z.string().min(1),
    requestId: z.string().optional(),
  }),
});

/** Turns a non-2xx answer into an [ApiError], using the envelope when the body has one. */
export function errorFromResponse(body: unknown, response: Response): ApiError {
  const headerRequestId = response.headers.get("X-Request-Id") ?? undefined;
  const parsed = envelope.safeParse(body);
  if (!parsed.success) {
    return new ApiError(CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE, response.status, headerRequestId);
  }
  const { code, requestId } = parsed.data.error;
  return new ApiError(code, response.status, requestId ?? headerRequestId);
}
