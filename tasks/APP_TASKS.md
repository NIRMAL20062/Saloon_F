# 📱 Android Apps: To Do

Two apps from one project (spec v2): **Glide Salon** (owner, receptionist/manager, stylist) and **Glide** (customers).
Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit.
Flows come from [ChatGPT.md](../ChatGPT.md) (customer + marketplace) and [Salon_App_Task_Wise_Development.md](../Salon_App_Task_Wise_Development.md) (salon side).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Now: Phase 1, Login and accounts

### APP-004 · Two apps from one project: Glide Salon + Glide (customer)
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-003 · Spec: C1.1 / E1.1
- **Why:** both apps need login next; they must share network, theme and models instead of copying them.
- **Needs from team:** nothing.
- **Scope:**
  - `android/app-salon` (app "Glide Salon", `com.glide.salon`) and `android/app-customer` (app "Glide", `com.glide.customer`)
  - shared modules with today's code moved in: `core-network` (Retrofit, API errors, backend URL, network security rules) and `core-ui` (theme, shared screens)
  - both apps open the existing system-status screen for now (it becomes a debug screen once login exists)
  - NOT included: `core-database`, `core-common`, `feature-booking`. They're created by the first task that needs them.
- **Done when:**
  - [ ] both apps build (debug + release) and install side by side on the phone
  - [ ] Tests: all 29 existing tests still pass in their new modules; a launch test for each app
  - [ ] Security: network rules (HTTPS only, localhost in debug) apply to both apps; backups off in both
  - [ ] Flow: on the phone both apps show "Server UP / Database UP"

### APP-005 · Salon app: phone login
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004, BE-016 · Spec: E1.2
- **Why:** every salon user (owner, receptionist, stylist) signs in with their phone number.
- **Needs from team:** Supabase project with the phone provider on (Twilio) and test phone numbers (steps in the task report).
- **Flow:** open app → enter phone number (+91) → receive OTP (test numbers: fixed code, no SMS) → enter OTP → signed in.
  Stay signed in after closing the app; logout from a menu.
- **Done when:**
  - [ ] Screens: phone entry (validation: 10-digit Indian mobile), OTP entry (6 digits, resend after 60 s, wrong-code message), loading and error states
  - [ ] Session kept securely on the device and refreshed automatically; logout clears it
  - [ ] Every backend call sends the login token; `GET /v1/me` answers with the signed-in user
  - [ ] Tests: ViewModel tests for every state; Compose UI tests for every screen state; token added to requests
  - [ ] Security: OTP and tokens never logged; token stored encrypted; rate-limit message shown when Supabase limits OTPs
  - [ ] Flow: on the phone, log in with a test number → see who you are → close and reopen (still logged in) → logout

### APP-006 · Customer app: phone login + profile
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005, BE-018 · Spec: C1.2, customer flow 3.1 ("Install → Phone OTP")
- **Why:** customers book and pay, so the app must know who they are.
- **Needs from team:** nothing beyond APP-005.
- **Flow:** phone → OTP → first time only: "Your name" (required) + email (optional) → home. Profile screen: edit name/email, logout.
- **Done when:**
  - [ ] login screens reused from APP-005 (shared module), profile step only for new customers
  - [ ] Tests: every screen state; profile validation (name 2–60 chars, valid email)
  - [ ] Security: a customer can only read/change their own profile (backend test)
  - [ ] Flow: on the phone, new test number → name → home; reopen → straight to home; edit name; logout
  - Profile photo: later (needs file storage)

### APP-007 · Salon app: first login → create your salon, or join as invited staff
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005, BE-017 · Spec: E2.1, E3.2; spec v2 §4 onboarding
- **Why:** after login, an owner needs a salon; a stylist/receptionist invited by phone lands in their salon.
- **Needs from team:** nothing; fields below come from the plan (veto any).
- **Flow:**
  - not a member of any salon → "Create your salon": name, phone, address, type (men / women / unisex, from spec v2 filters) → you are its **owner**
  - invited by phone → "Join <salon name> as <role>?" → accept
  - member of more than one salon → pick one (switch later from the menu)
- **Done when:**
  - [ ] Tests: every screen state; owner, invited staff and no-invite paths
  - [ ] Security: an invite works only for the invited phone number; tenant isolation tests (BE-017)
  - [ ] Flow: on the phone, test number A creates "Test Salon A"; owner invites test number B as stylist; B logs in and joins

### APP-008 · Customer app: navigation shell
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-006 · Spec: C1.3
- **Scope:** bottom navigation **Home, Search, Bookings, Profile** (spec v2); empty states for the first three until their features arrive; Profile from APP-006.
- **Done when:** tests for navigation and back behaviour; on the phone all four tabs open and Back works as expected.

### APP-009 · Customer app: location permission and city
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-008 · Spec: C1.4, customer flow 3.1 ("Allow location")
- **Scope:** explain why location is needed → ask permission → if denied, pick a city from a list. Remember the choice.
- **Needs from team:** the launch city list (spec v2: "one city, design-partner salons"). Which city?
- **Done when:** tests for allow / deny / "don't ask again"; on the phone both paths reach Home.

### APP-010 · Crash reports + analytics in both apps
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004 · Spec: E1.7 (prove design-partner usage)
- **Needs from team:** in the Firebase project (already created), add both apps (`com.glide.salon`, `com.glide.customer`) and send the two `google-services.json` files privately, never via git.
- **Done when:** a test crash shows in Firebase for each app; no personal data (phone, name) in analytics events.

## Next phases (outline: written out in full when the phase starts)

**Phase 2: Salon setup** (salon plan E2–E4; spec v2 §4)
- APP-1xx Salon settings: working hours, closed days · services list/add/edit (name, category, duration, price) · staff list, invite by phone, roles, working hours · salon's customer list, search, add walk-in, customer history

**Phase 3: Appointments** (salon plan E5; spec v2 §4)
- APP-2xx Calendar day view per stylist · book walk-in/phone appointment · reschedule/cancel · complete / no-show · new-booking push alert · accept/reject app bookings (auto-accept default) · **no internet = view only** (D-019)

**Phase 4: Customer finds and books** (spec v2 EC2–EC3; flows 3.1–3.3)
- APP-3xx Home: nearby salons, categories, top rated · search + filters (service, price, rating, open now, men/women/unisex) · salon page (photos, services, staff, reviews, policies, map) · cart (services + staff or "any") · slot picker · order summary with policy · My bookings (upcoming/past, status, reschedule, cancel with refund amount shown first, directions, call salon)

**Phase 5: Billing and invoices** (salon plan E8–E9)
- APP-4xx Bill for a completed appointment · cash/UPI/card payments · payment link / UPI QR · day close · share invoice

**Phase 6: Payments, refunds, subscriptions** (spec v2 EC4; salon plan E12)
- APP-5xx Razorpay Checkout in the customer app (UPI, cards, wallets; full / advance / pay at salon) · refund status · salon payouts screen · salon subscription

**Phase 7: Disputes, reviews, reports** (spec v2 EC5–EC7; salon plan E10)
- APP-6xx Report a problem (48 h, photos) · salon dispute inbox · reviews after visit, salon replies · notification preferences · owner reports

## Pre-launch (outline)

- APP-9xx **Staging builds** with a visible STAGING banner via Firebase App Distribution. _Needs from team: testers' emails, a Firebase service-account key as a GitHub secret._
- APP-9xx Launcher icons + brand colours/fonts. _Needs from team: designs from Figma._
- APP-9xx Release signing + Play Store listings (privacy policy, account deletion, data-safety form: spec v2 risks). _Needs from team: Play Console account (US$25 one-time), upload keystore._

## Not planned

- Indian languages: English only (D-018). Text stays in resource files, so they can be added later.
