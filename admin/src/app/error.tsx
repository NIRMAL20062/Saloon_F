"use client";

import { AuthCard } from "@/components/AuthCard";

/**
 * Any admin page that fails (e.g. Glide's server not answering): our own words, a reference to quote, and a retry.
 * Never shows the error's own text (admin/CLAUDE.md).
 */
export default function ErrorPage({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return (
    <AuthCard
      title="Something went wrong"
      intro={
        <p>
          This page couldn&apos;t be loaded, usually because Glide&apos;s server isn&apos;t answering. Try again in a minute.
          {error.digest ? <span className="mt-2 block text-xs text-zinc-500">Reference: {error.digest}</span> : null}
        </p>
      }
    >
      <button
        type="button"
        onClick={() => retry()}
        className="w-full rounded-lg bg-red-600 px-4 py-2.5 font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-600/40"
      >
        Try again
      </button>
    </AuthCard>
  );
}
