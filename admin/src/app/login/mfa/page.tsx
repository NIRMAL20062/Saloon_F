import { redirect } from "next/navigation";
import { readSession } from "@/auth/cookies";
import { AuthCard } from "@/components/AuthCard";
import { verifyAuthenticator } from "../actions";
import { CodeForm } from "../forms";

export default async function AuthenticatorCodePage() {
  const session = await readSession();
  if (!session) redirect("/login");
  if (session.mfa) redirect("/");
  return (
    <AuthCard title="Authenticator code" intro={<p>Enter the 6-digit code your authenticator app shows for Glide Admin.</p>}>
      <CodeForm action={verifyAuthenticator} label="Code from the app" submitLabel="Log in" />
    </AuthCard>
  );
}
