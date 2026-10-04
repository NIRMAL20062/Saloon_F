// @vitest-environment node
import { NextRequest } from "next/server";
import { expect, test, vi } from "vitest";
import { TEST_ENV } from "@/test/next-fakes";
import { SESSION_COOKIE } from "./auth/session";

vi.mock("./env", () => ({ getEnv: () => TEST_ENV }));

const { proxy } = await import("./proxy");

test("a logged-out visit to a page redirects to /login", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/salons"));

  expect(response.status).toBe(307);
  expect(response.headers.get("location")).toBe("http://localhost:3000/login");
});

test("an unreadable session cookie is deleted on the way to the login page", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/", { headers: { cookie: `${SESSION_COOKIE}=forged` } }));

  expect(response.headers.get("location")).toBe("http://localhost:3000/login?expired=1");
  expect(response.headers.get("set-cookie")).toMatch(new RegExp(`${SESSION_COOKIE}=;.*Max-Age=0`, "i"));
});

test("the login page is served with a nonce CSP", async () => {
  const response = await proxy(new NextRequest("http://localhost:3000/login"));

  expect(response.headers.get("location")).toBeNull();
  expect(response.headers.get("content-security-policy")).toMatch(/script-src 'self' 'nonce-/);
});
