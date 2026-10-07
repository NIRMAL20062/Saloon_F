import Link from "next/link";
import { redirect } from "next/navigation";
import { readSession } from "@/auth/cookies";
import { AuthCard } from "@/components/AuthCard";
import { continueLogin } from "../actions";
import { ContinueForm } from "../forms";

/** A code was accepted, but Glide's server (or Supabase) didn't answer when deciding the next step. */
export default async function ContinueLoginPage() {
  if (!(await readSession())) redirect("/login");
  return (
    <AuthCard
      title="Couldn't finish logging in"
      intro={<p>Your code was accepted, but Glide&apos;s server didn&apos;t answer. Wait a moment and try again.</p>}
    >
      <ContinueForm action={continueLogin} />
      <p className="mt-4 text-center text-sm">
        <Link href="/login" className="text-zinc-600 underline hover:text-zinc-900">
          Start again
        </Link>
      </p>
    </AuthCard>
  );
}
