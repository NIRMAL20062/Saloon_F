# Login options for Glide (research for Q-007)

> Researched 2026-10-02 for the team's question: "Supabase Auth everywhere? What do other apps do?"
> **Status: waiting for the team's decision.** Prices exclude 18% GST; ₹ at ~₹85–88/$. Vendor-sourced figures are marked (vendor).

## The short version

| Who logs in | Recommended | Cost per login (India) | Paperwork |
|---|---|---|---|
| Salon owner, stylist, receptionist | **Supabase Auth phone OTP**, sent as a **WhatsApp OTP** through our backend; SMS fallback | WhatsApp ≈ **₹0.14**; SMS ≈ ₹7 (Twilio intl, no DLT) or ≈ ₹0.2 (MSG91, needs DLT) | Meta business verification (to exceed 250 OTPs/day); DLT only if we use cheap Indian SMS |
| Salon's customers | **No login.** Signed, expiring links sent on WhatsApp (book / pay) | free | none |
| Our internal admins | **Same Supabase project, email OTP + mandatory authenticator-app MFA**; only people in our `admins` table are admins; admins invite admins | free (Brevo SMTP 300 emails/day) | none |

Original plan (Firebase phone OTP) still works with zero paperwork, but costs ≈ ₹6 per SMS, **needs a billing card (Blaze plan)**, and has no WhatsApp option.

## Why

**1. Supabase phone OTP doesn't send SMS by itself.** It needs Twilio / MessageBird / Vonage / Textlocal, or a **Send SMS Hook**
that calls our own backend, which can then send a WhatsApp OTP or an SMS through any provider. [1][2][3]
Free tier: 50,000 monthly users; 30 SMS/hour per project by default (adjustable); 60 s between OTPs per user. [4][22]

**2. India's DLT rules** (TRAI): cheap domestic SMS (MSG91 ≈ ₹0.16–0.25 [7]) needs the sending business registered on DLT
(≈ ₹5,900 one-time + ₹590/sender ID/year, 5–10 working days, Aadhaar/biometric verification since Feb 2025 (vendor) [8]).
Twilio's international route (≈ $0.083 ≈ ₹7) avoids DLT but shows a random number as sender. [5][6]

**3. Firebase phone OTP** now needs the paid Blaze plan (card on file); ≈ $0.07 (≈ ₹6) per SMS to India, first 10/day free;
Google sends the SMS, so no DLT for us, but also no WhatsApp and no custom provider. [9][10][12 (vendor)]

**4. WhatsApp OTP** (Meta "authentication" template): ≈ ₹0.115 per delivered message in India (rate card from 1 Oct 2026),
**~40× cheaper than SMS**, with Android one-tap/zero-tap autofill. New businesses: 250 recipients/24 h until Meta business
verification (then 2,000+). Indian providers now recommend WhatsApp first, SMS fallback (vendor). [13][14][15][16][17]

**5. Supabase email login** needs our own SMTP for real use (built-in mailer: 2 emails/hour, team addresses only). Free SMTP:
Brevo 300/day, Resend 3,000/month (100/day). [18][19][20]

**6. Supabase free-tier traps:**
- projects **pause after 1 week without activity**
- 500 MB database, 2 active projects
- **no automatic backups**: we must run our own nightly `pg_dump`
- auth logs are kept for only 1 hour
- authenticator-app MFA is free; SMS MFA is paid

Pro costs $25/month per project: no pausing, daily backups. [22][23]

**7. Using Supabase Auth with our Ktor backend:**
- Ktor verifies Supabase tokens with the public keys at `https://<project>.supabase.co/auth/v1/.well-known/jwks.json`. Use ES256 asymmetric keys; the old shared secret is not recommended. [24][28]
- On every request, check the issuer, `aud=authenticated` and expiry. On admin routes, also require MFA (`aal2`). [25][31]
- **Keep roles and `salon_id` in our own database** (`salon_members(user_id, salon_id, role)`, `admins`), not inside the token:
  - token claims go stale until the token refreshes
  - a stylist can work at two salons, which one `salon_id` claim can't express
  - our tenant rules stay testable on our own Postgres

**8. What other SaaS products do for internal admins (common patterns):**
1. Same login provider, but **our database decides who is an admin**; invite-only, no self sign-up. [29][30]
2. Or a completely separate login system for staff (another project, or Google Workspace SSO).
3. **Mandatory MFA for admins**, short sessions, and re-login before dangerous actions. [32]
4. Optional network gate in front of the panel (Cloudflare Access is free for up to 50 users). [33]
5. **Our own audit log** of who did what (already a Phase 1 task).

## Risks to plan for

- **Free tier pausing + no backups** (Supabase): if our app data also lived in Supabase's database, a pause stops everything.
  Keep nightly `pg_dump` (Pre-launch task) and budget Pro ($25/month) when the first real salon goes live. This deviates from
  D-010 "free tiers only", so it's the team's call when the time comes.
- **OTP abuse** (bots requesting OTPs cost us money): CAPTCHA, the per-hour limit, Indian numbers only.
- **Paperwork:** cheap SMS needs a registered business for DLT; WhatsApp needs Meta business verification beyond 250/day.

## What the team must decide (Q-007)

1. **Supabase Auth** (recommended above) **or Firebase Auth** (original plan) for app users?
2. If Supabase: **WhatsApp OTP first + SMS fallback**, or SMS only?
3. Customers: **no login (WhatsApp links)**, as recommended, or a real login?
4. Admins: **email OTP + authenticator-app MFA**, as recommended?

## Sources

[1] supabase.com/docs/guides/auth/phone-login · [2] supabase.com/docs/guides/auth/auth-hooks · [3] supabase.com/docs/guides/auth/auth-hooks/send-sms-hook ·
[4] supabase.com/docs/guides/auth/rate-limits · [5] twilio.com/en-us/sms/pricing/in · [6] twilio.com/en-us/guidelines/in/sms · [7] msg91.com/in/pricing/sms ·
[8] messagecentral.com/sms-guideline/india (vendor) · [9] firebase.google.com/pricing · [10] cloud.google.com/identity-platform/pricing ·
[12] minimoth.dev/blog/minimoth-vs-firebase-otp-pricing-india (vendor) · [13] developers.facebook.com/docs/whatsapp/pricing ·
[14] chatmaxima.com/whatsapp-api-pricing/india (tracks Meta's INR card) · [15] developers.facebook.com/docs/whatsapp/business-management-api/authentication-templates ·
[16] developers.facebook.com/docs/whatsapp/messaging-limits · [17] richautomate.in/blog/whatsapp-otp-authentication-india-2026 (vendor) ·
[18] supabase.com/docs/guides/auth/auth-smtp · [19] brevo.com/free-smtp-server · [20] resend.com/pricing · [22] supabase.com/pricing ·
[23] supabase.com/docs/guides/platform/backups · [24] supabase.com/docs/guides/auth/signing-keys · [25] supabase.com/docs/guides/auth/jwt-fields ·
[28] ktor.io/docs/server-jwt.html · [29] supabase.com/docs/reference/javascript/admin-api · [30] supabase.com/docs/guides/auth/auth-hooks/before-user-created-hook ·
[31] supabase.com/docs/guides/auth/auth-mfa · [32] cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html ·
[33] cloudflare.com/zero-trust/products/access
