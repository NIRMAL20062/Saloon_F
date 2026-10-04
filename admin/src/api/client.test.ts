// @vitest-environment node
import { describe, expect, test } from "vitest";
import { createBackendClient, unwrap } from "./client";
import { ApiError, CLIENT_ERROR_CODES } from "./errors";

const BASE_URL = "http://backend.test";

/** A client whose "backend" answers every request with [respond]; the requests it saw are kept in [seen]. */
function fakeBackend(respond: (request: Request) => Response | Promise<Response>) {
  const seen: Request[] = [];
  const client = createBackendClient(BASE_URL, {
    fetch: async (input) => {
      const request = input as Request;
      seen.push(request);
      return respond(request);
    },
  });
  return { client, seen };
}

function json(body: unknown, status = 200, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", ...headers },
  });
}

async function failure(call: Promise<unknown>): Promise<ApiError> {
  const error = await call.then(
    () => undefined,
    (thrown: unknown) => thrown,
  );
  expect(error).toBeInstanceOf(ApiError);
  return error as ApiError;
}

describe("backend client", () => {
  test("returns the typed body of a successful call", async () => {
    const { client, seen } = fakeBackend(() => json({ status: "UP", version: "0.1.0" }));

    const health = await unwrap(client.GET("/health/live"));

    expect(health).toEqual({ status: "UP", version: "0.1.0" });
    expect(seen.map((request) => `${request.method} ${request.url}`)).toEqual([`GET ${BASE_URL}/health/live`]);
  });

  test("sends typed bodies and headers", async () => {
    const { client, seen } = fakeBackend(() => json({ id: "3f0c9a52-7d1e-4b8a-9a0e-2c1d5b6e7f80", side: "SALON" }));

    const me = await unwrap(
      client.PUT("/v1/me/side", { body: { side: "SALON" }, headers: { Authorization: "Bearer test-token" } }),
    );

    expect(me.side).toBe("SALON");
    expect(seen[0].method).toBe("PUT");
    expect(seen[0].headers.get("Authorization")).toBe("Bearer test-token");
    expect(await seen[0].json()).toEqual({ side: "SALON" });
  });

  test("calls that don't match the spec don't compile", () => {
    const { client } = fakeBackend(() => json({}));

    // Never invoked: the @ts-expect-error lines are what's tested (pnpm typecheck fails if they compile).
    const _calls = () => [
      // @ts-expect-error: no such path in docs/api/openapi.yaml
      client.GET("/v1/no-such-path"),
      // @ts-expect-error: ADMIN is not a UserSide
      client.PUT("/v1/me/side", { body: { side: "ADMIN" } }),
      // @ts-expect-error: the body is required
      client.PUT("/v1/me/profile"),
    ];
    expect(_calls).toBeTypeOf("function");
  });

  test("maps the backend error envelope to ApiError with its code, status and request ID", async () => {
    const { client } = fakeBackend(() =>
      json(
        { error: { code: "SIDE_ALREADY_CHOSEN", message: "You already chose.", requestId: "req-envelope" } },
        409,
        { "X-Request-Id": "req-header" },
      ),
    );

    const error = await failure(unwrap(client.PUT("/v1/me/side", { body: { side: "CUSTOMER" } })));

    expect(error.code).toBe("SIDE_ALREADY_CHOSEN");
    expect(error.status).toBe(409);
    expect(error.requestId).toBe("req-envelope");
  });

  test("never keeps the backend's message, so it can't reach a page", async () => {
    const { client } = fakeBackend(() =>
      json({ error: { code: "INTERNAL", message: "secret detail from the backend" } }, 500, { "X-Request-Id": "r1" }),
    );

    const error = await failure(unwrap(client.GET("/v1/me")));

    expect(error.code).toBe("INTERNAL");
    expect(error.requestId).toBe("r1");
    expect(JSON.stringify({ ...error, message: error.message })).not.toContain("secret detail");
  });

  test("an error without the envelope becomes UNEXPECTED_RESPONSE, keeping the status and header request ID", async () => {
    const { client } = fakeBackend(
      () => new Response("<html>Bad Gateway</html>", { status: 502, headers: { "X-Request-Id": "req-proxy" } }),
    );

    const error = await failure(unwrap(client.GET("/v1/me")));

    expect(error.code).toBe(CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE);
    expect(error.status).toBe(502);
    expect(error.requestId).toBe("req-proxy");
  });

  test("an empty error body becomes UNEXPECTED_RESPONSE", async () => {
    const { client } = fakeBackend(() => new Response(null, { status: 500, headers: { "Content-Length": "0" } }));

    const error = await failure(unwrap(client.GET("/v1/me")));

    expect(error.code).toBe(CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE);
    expect(error.status).toBe(500);
  });

  test("a successful status with broken JSON becomes UNEXPECTED_RESPONSE", async () => {
    const { client } = fakeBackend(
      () => new Response("{not json", { status: 200, headers: { "Content-Type": "application/json" } }),
    );

    const error = await failure(unwrap(client.GET("/health/live")));

    expect(error.code).toBe(CLIENT_ERROR_CODES.UNEXPECTED_RESPONSE);
  });

  test("backend down (connection refused) becomes BACKEND_UNREACHABLE", async () => {
    // Port 1 on localhost: nothing listens there, so the connection is refused straight away.
    const client = createBackendClient("http://127.0.0.1:1");

    const error = await failure(unwrap(client.GET("/health/live")));

    expect(error.code).toBe(CLIENT_ERROR_CODES.BACKEND_UNREACHABLE);
    expect(error.status).toBeUndefined();
    expect(error.requestId).toBeUndefined();
  });

  test("a backend that doesn't answer in time becomes BACKEND_UNREACHABLE", async () => {
    const client = createBackendClient(BASE_URL, {
      timeoutMs: 20,
      // Hangs until the request is aborted, like a backend that accepted the connection but never answers.
      fetch: (input) =>
        new Promise<Response>((_, reject) => {
          const { signal } = input as Request;
          signal.addEventListener("abort", () => reject(signal.reason));
        }),
    });

    const error = await failure(unwrap(client.GET("/health/live")));

    expect(error.code).toBe(CLIENT_ERROR_CODES.BACKEND_UNREACHABLE);
  });
});
