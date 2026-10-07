import type { ReactNode } from "react";

/** The frame of every login screen: brand, title, short explanation, then the form. */
export function AuthCard({ title, children, intro }: { title: string; intro?: ReactNode; children: ReactNode }) {
  return (
    <main className="flex flex-1 items-center justify-center bg-zinc-50 p-4">
      <div className="w-full max-w-sm rounded-2xl border border-zinc-200 bg-white p-8 shadow-sm">
        <p className="text-sm font-semibold tracking-wide text-red-600">Glide Admin</p>
        <h1 className="mt-2 text-2xl font-semibold text-zinc-900">{title}</h1>
        {intro ? <div className="mt-2 text-sm text-zinc-600">{intro}</div> : null}
        <div className="mt-6">{children}</div>
      </div>
    </main>
  );
}
