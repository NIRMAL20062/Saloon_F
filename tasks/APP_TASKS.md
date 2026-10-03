# 📱 Android App: To Do

**One app for everyone** (D-023): at onboarding the user says whether they're a **customer** or a **salon** (owner / receptionist /
stylist), and the app shows that side's screens (D-024). Lives in `android/app`.
Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit.
Flows come from [ChatGPT.md](../ChatGPT.md) (customer side, marketplace) and [Salon_App_Task_Wise_Development.md](../Salon_App_Task_Wise_Development.md) (salon side).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Now: Phase 1, Login and onboarding

### APP-004 · Phone login (everyone)
- **Phase:** 1 · **Status:** 🔄 In progress · **Owner:** Claude · **Depends on:** BE-016 · Spec: C1.2, E1.2
- **Why:** customers and salon people all sign in the same way: phone number + OTP.
- **Needs from team:** Supabase project with the phone provider on (Twilio) and test phone numbers (steps in the BE-016 report).
- **Flow:** open app → enter phone number (+91) → receive OTP (test numbers: fixed code, no SMS) → enter OTP → signed in.
  Stays signed in after closing the app; logout from the menu.
- **Look (team, 2026-10-03: "everything polished", D-031):** the login is built with the Glide theme and components now:
  brand gradient header, rounded sheet, +91 chip, six code boxes (blinking caret, pop-in digits, shake + haptic on a wrong
  code, SMS code autofill), auto-verify on the 6th digit, sliding steps, press feedback, light and dark mode.
- **Done when:**
  - [x] Screens: phone entry (10-digit Indian mobile), OTP entry (6 digits, resend after 60 s, wrong-code message), loading and error states (`LoginScreenTest` 12)
  - [x] Session stored encrypted on the device (Android Keystore AES-GCM) and refreshed automatically; logout clears it (`EncryptedSessionStoreTest`, `AuthRepositoryTest`)
  - [x] Every backend call sends the login token, refreshed once on 401; `GET /v1/me` answers with the signed-in user (`AuthenticatedClientTest`, `SignedInTest`)
  - [x] Tests: ViewModel tests for every state; Compose UI tests for every screen state and every component state (`ComponentsTest` 12); 91 app tests in total
  - [x] Security: OTP and tokens never logged (HTTP logs at BASIC level, debug only); rate-limit message shown when Supabase limits OTPs
  - [ ] Flow: on the phone, log in with a test number → close and reopen (still logged in) → logout

### APP-005 · Onboarding: customer or salon?
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004, BE-016, BE-018 · Decision: D-024
- **Why:** one app, two kinds of users; each sees only their own interface.
- **Needs from team:** nothing.
- **Flow (first login only):** "How will you use Glide?"
  - **"I want to book salons"** → your name (required) + email (optional) → customer side
  - **"I run or work at a salon"** → salon onboarding (APP-006) → salon side
  - the choice is saved on the backend, so it's remembered on any phone
  - someone who is both (e.g. a salon owner who also books elsewhere) can **switch side** from the menu
- **Done when:**
  - [ ] Tests: every screen state; customer path, salon path, returning user goes straight to their side, switch side
  - [ ] Security: a customer can only read/change their own profile (backend test)
  - [ ] Flow: on the phone, new test number → "book salons" → name → customer side; reopen → straight there

### APP-006 · Salon onboarding: create your salon or join by invite
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005, BE-017 · Spec: E2.1, E3.2; spec v2 §4 onboarding
- **Needs from team:** nothing; fields below come from the plan (veto any).
- **Flow:**
  - this phone number was invited by a salon → "Join <salon name> as <role>?" → accept → salon side
  - otherwise → "Create your salon": name, phone, address, type (men / women / unisex, from spec v2 filters) → you are its **owner**
  - member of more than one salon → pick one (switch later from the menu)
- **Done when:**
  - [ ] Tests: every screen state; owner, invited staff and no-invite paths
  - [ ] Security: an invite works only for the invited phone number; tenant isolation tests (BE-017)
  - [ ] Flow: on the phone, test number A creates "Test Salon A"; owner invites test number B as stylist; B logs in, picks salon, joins

### APP-007 · Customer side: tabs
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005 · Spec: C1.3
- **Scope:** bottom navigation **Home, Search, Bookings, Profile** (spec v2); empty states for the first three until their features arrive; Profile shows name/email (edit), switch side, logout.
- **Done when:** tests for navigation and Back; on the phone all four tabs open.

### APP-008 · Salon side: home + menu
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-006
- **Scope:** salon home showing salon name and your role; menu: switch salon, switch side, logout. The salon side's real tabs (calendar, customers, services…) arrive with Phase 2–3 features.
- **Done when:** tests for menu actions; an owner and a stylist each see their salon and role on the phone.

### APP-009 · Customer side: location permission and city
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-007 · Spec: C1.4, customer flow 3.1 ("Allow location")
- **Scope:** explain why location is needed → ask permission → if denied, pick a city from a list. Remember the choice.
- **Needs from team:** the launch city (spec v2: "one city, design-partner salons"). Which city?
- **Done when:** tests for allow / deny / "don't ask again"; on the phone both paths reach Home.

### APP-010 · Crash reports + analytics
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004 · Spec: E1.7 (prove design-partner usage)
- **Needs from team:** in the Firebase project (already created), add the Android app `com.glide.android` and send its `google-services.json` privately, never via git.
- **Done when:** a test crash shows in Firebase; no personal data (phone, name) in analytics events.

## Next phases (outline: written out in full when the phase starts)

**Phase 2: Salon side, setup** (salon plan E2–E4; spec v2 §4)
- APP-1xx Working hours, closed days · services list/add/edit (name, category, duration, price) · staff list, invite by phone, roles, working hours · salon's customer list, search, add walk-in, history

**Phase 3: Salon side, appointments** (salon plan E5; spec v2 §4)
- APP-2xx Calendar day view per stylist · book walk-in/phone appointment · reschedule/cancel · complete / no-show · new-booking push alert · accept/reject app bookings (auto-accept default) · **no internet = view only** (D-019)

**Phase 4: Customer side, find and book** (spec v2 EC2–EC3; flows 3.1–3.3)
- APP-3xx Home: nearby salons, categories, top rated · search + filters (service, price, rating, open now, men/women/unisex) · salon page (photos, services, staff, reviews, policies, map) · cart (services + staff or "any") · slot picker · order summary with policy · My bookings (upcoming/past, status, reschedule, cancel with refund amount shown first, directions, call salon)

**Phase 5: Salon side, billing and invoices** (salon plan E8–E9)
- APP-4xx Bill for a completed appointment · cash/UPI/card payments · payment link / UPI QR · day close · share invoice

**Phase 6: Payments, refunds, subscriptions** (spec v2 EC4; salon plan E12)
- APP-5xx Razorpay Checkout on the customer side (UPI, cards, wallets; full / advance / pay at salon) · refund status · salon payouts screen · salon subscription

**Phase 7: Disputes, reviews, reports** (spec v2 EC5–EC7; salon plan E10)
- APP-6xx Report a problem (48 h, photos) · salon dispute inbox · reviews after visit, salon replies · notification preferences · owner reports

## Pre-launch (outline)

- APP-9xx **Staging build** with a visible STAGING banner via Firebase App Distribution. _Needs from team: testers' emails, a Firebase service-account key as a GitHub secret._
- APP-9xx Launcher icon + brand colours/fonts. _Needs from team: designs from Figma._
- APP-9xx Release signing + Play Store listing (privacy policy, account deletion, data-safety form: spec v2 risks). _Needs from team: Play Console account (US$25 one-time), upload keystore._

## Not planned

- A separate salon app and customer app: **one app** (D-023), even though spec v2 §0 lists two.
- Indian languages: English only (D-018). Text stays in resource files, so they can be added later.
