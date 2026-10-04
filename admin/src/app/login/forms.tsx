"use client";

import { useActionState } from "react";
import type { FormState, SetupState } from "./actions";

type Action<S> = (state: S, form: FormData) => Promise<S>;

const input =
  "block w-full rounded-lg border border-zinc-300 px-3 py-2 text-zinc-900 placeholder:text-zinc-400 focus:border-red-600 focus:outline-none focus:ring-2 focus:ring-red-600/20";
const button =
  "mt-4 w-full rounded-lg bg-red-600 px-4 py-2.5 font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-600/40 disabled:opacity-60";

function ErrorText({ error }: { error?: string }) {
  return error ? (
    <p role="alert" className="mt-3 text-sm text-red-700">
      {error}
    </p>
  ) : null;
}

/** Step 1: which email to send the code to. */
export function EmailForm({ action }: { action: Action<FormState> }) {
  const [state, formAction, pending] = useActionState(action, {});
  return (
    <form action={formAction} noValidate>
      <label htmlFor="email" className="text-sm font-medium text-zinc-800">
        Email
      </label>
      <input id="email" name="email" type="email" autoComplete="email" required autoFocus className={`${input} mt-1`} />
      <ErrorText error={state.error} />
      <button type="submit" disabled={pending} className={button}>
        {pending ? "Sending…" : "Send code"}
      </button>
    </form>
  );
}

/** A 6-digit code: from the email, or from the authenticator app. */
export function CodeForm({
  action,
  label,
  submitLabel = "Continue",
}: {
  action: Action<FormState>;
  label: string;
  submitLabel?: string;
}) {
  const [state, formAction, pending] = useActionState(action, {});
  return (
    <form action={formAction} noValidate>
      <CodeInput label={label} />
      <ErrorText error={state.error} />
      <button type="submit" disabled={pending} className={button}>
        {pending ? "Checking…" : submitLabel}
      </button>
    </form>
  );
}

function CodeInput({ label }: { label: string }) {
  return (
    <>
      <label htmlFor="code" className="text-sm font-medium text-zinc-800">
        {label}
      </label>
      <input
        id="code"
        name="code"
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="[0-9]*"
        maxLength={6}
        required
        autoFocus
        className={`${input} mt-1 tracking-[0.4em]`}
      />
    </>
  );
}

/** First login: create the authenticator (QR + secret), then confirm it with its first code. */
export function AuthenticatorSetup({ action }: { action: Action<SetupState> }) {
  const [state, formAction, pending] = useActionState(action, {});
  const enrollment = state.enrollment;

  if (!enrollment) {
    return (
      <form action={formAction}>
        <input type="hidden" name="intent" value="start" />
        <ErrorText error={state.error} />
        <button type="submit" disabled={pending} className={button}>
          {pending ? "Preparing…" : "Set up authenticator app"}
        </button>
      </form>
    );
  }

  return (
    <form action={formAction} noValidate>
      <ol className="list-decimal space-y-1 pl-5 text-sm text-zinc-700">
        <li>Open your authenticator app (for example Google Authenticator) and add an account.</li>
        <li>Scan this QR code, or type the key below.</li>
        <li>Enter the 6-digit code the app shows.</li>
      </ol>
      {/* A data: URI from our own server; next/image adds nothing here. */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={enrollment.qrCode} alt="QR code for your authenticator app" width={192} height={192} className="mx-auto my-4" />
      <p className="text-xs text-zinc-600">Key for typing in by hand:</p>
      <code className="mb-4 block break-all rounded bg-zinc-100 px-2 py-1 text-sm text-zinc-900">{enrollment.secret}</code>
      <input type="hidden" name="intent" value="verify" />
      <input type="hidden" name="factorId" value={enrollment.factorId} />
      <CodeInput label="Code from the app" />
      <ErrorText error={state.error} />
      <button type="submit" disabled={pending} className={button}>
        {pending ? "Checking…" : "Finish setup"}
      </button>
    </form>
  );
}
