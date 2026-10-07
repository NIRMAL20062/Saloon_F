import { requireAdmin } from "@/auth/require-admin";
import { logout } from "./login/actions";

/** Admin home. Real sections (salons to verify, admins…) arrive with their tasks. */
export default async function Home() {
  const admin = await requireAdmin();
  return (
    <div className="flex flex-1 flex-col bg-zinc-50">
      <header className="flex items-center justify-between border-b border-zinc-200 bg-white px-6 py-3">
        <p className="font-semibold text-red-600">Glide Admin</p>
        <form action={logout} className="flex items-center gap-4">
          <span className="text-sm text-zinc-600">{admin.email}</span>
          <button type="submit" className="rounded-lg border border-zinc-300 px-3 py-1.5 text-sm font-medium text-zinc-900 hover:bg-zinc-50">
            Log out
          </button>
        </form>
      </header>
      <main className="flex flex-1 flex-col items-center justify-center gap-2 p-8">
        <h1 className="text-3xl font-semibold text-zinc-900">Glide Admin</h1>
        <p className="text-zinc-600">You&apos;re logged in. Salon verification arrives with its task (WEB-007).</p>
      </main>
    </div>
  );
}
