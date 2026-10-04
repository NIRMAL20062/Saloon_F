import Link from "next/link";
import { AuthCard } from "@/components/AuthCard";

/** A login that isn't one of our admins (BE-020 said NOT_ADMIN). Its session was already ended. */
export default function NoAccessPage() {
  return (
    <AuthCard title="No access" intro={<p>This account isn&apos;t a Glide admin. Ask an admin to invite you.</p>}>
      <Link
        href="/login"
        className="block w-full rounded-lg border border-zinc-300 px-4 py-2.5 text-center font-medium text-zinc-900 hover:bg-zinc-50"
      >
        Log in with another email
      </Link>
    </AuthCard>
  );
}
