import Link from "next/link";
import { redirect } from "next/navigation";
import { readPendingLogin } from "@/auth/cookies";
import { AuthCard } from "@/components/AuthCard";
import { verifyEmailCode } from "../actions";
import { CodeForm } from "../forms";

export default async function EmailCodePage() {
  const pending = await readPendingLogin();
  if (!pending) redirect("/login");
  return (
    <AuthCard
      title="Check your email"
      intro={
        <p>
          If <strong className="text-zinc-900">{pending.email}</strong> can log in, we&apos;ve sent it a 6-digit code. It
          works for a few minutes.
        </p>
      }
    >
      <CodeForm action={verifyEmailCode} label="Code from the email" />
      <p className="mt-4 text-center text-sm">
        <Link href="/login" className="text-zinc-600 underline hover:text-zinc-900">
          Use another email
        </Link>
      </p>
    </AuthCard>
  );
}
