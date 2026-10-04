import { redirect } from "next/navigation";
import { readSession } from "@/auth/cookies";
import { AuthCard } from "@/components/AuthCard";
import { setUpAuthenticator } from "../../actions";
import { AuthenticatorSetup } from "../../forms";

export default async function AuthenticatorSetupPage() {
  const session = await readSession();
  if (!session) redirect("/login");
  if (session.mfa) redirect("/");
  return (
    <AuthCard
      title="Set up your authenticator app"
      intro={<p>Admins need a second step: a code from an app on your phone, such as Google Authenticator. This is a one-time setup.</p>}
    >
      <AuthenticatorSetup action={setUpAuthenticator} />
    </AuthCard>
  );
}
