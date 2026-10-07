import { redirect } from "next/navigation";
import { readSession } from "@/auth/cookies";
import { IDLE_LIMIT_MS } from "@/auth/session";
import { AuthCard } from "@/components/AuthCard";
import { sendCode } from "./actions";
import { EmailForm } from "./forms";

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { expired } = await searchParams;
  // After a logout, show the form: going back to / with a session the backend refused would loop (WEB-008).
  if (!expired && (await readSession())?.mfa) redirect("/");
  return (
    <AuthCard
      title="Log in"
      intro={
        <>
          {expired ? (
            <p role="status" className="mb-2 rounded-lg bg-amber-50 px-3 py-2 text-amber-900">
              You were logged out after {IDLE_LIMIT_MS / 60_000} minutes without activity, or because your login ended. Log
              in again.
            </p>
          ) : null}
          <p>We&apos;ll email you a login code. Only Glide team admins can log in.</p>
        </>
      }
    >
      <EmailForm action={sendCode} />
    </AuthCard>
  );
}
