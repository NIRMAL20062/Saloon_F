# 📱 Android App: To Do

**One app for everyone** (D-023): at onboarding the user says whether they're a **customer** or a **salon** (owner / manager /
staff, D-034), and the app shows that side's screens (D-024). The choice is final: no switching (D-030). Lives in `android/app`.
Every screen uses the shared design system (APP-011, D-031): modern, smooth and interactive.
Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit.
Flows come from [ChatGPT.md](../ChatGPT.md) (customer side, marketplace) and [Salon_App_Task_Wise_Development.md](../Salon_App_Task_Wise_Development.md) (salon side).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Now: Phase 1, Login and onboarding

### APP-004 · Phone login (everyone)
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** BE-016 · Spec: C1.2, E1.2
- **Why:** customers and salon people all sign in the same way: phone number + OTP.
- **Needs from team:** Supabase project with the phone provider on (Twilio) and test phone numbers (steps in the BE-016 report).
- **Flow:** open app → enter phone number (+91) → receive OTP (test numbers: fixed code, no SMS) → enter OTP → signed in.
  Stays signed in after closing the app; logout from the menu.
- **Done when:**
  - [ ] Screens: phone entry (10-digit Indian mobile), OTP entry (6 digits, resend after 60 s, wrong-code message), loading and error states
  - [ ] Session stored encrypted on the device and refreshed automatically; logout clears it
  - [ ] Every backend call sends the login token; `GET /v1/me` answers with the signed-in user
  - [ ] Tests: ViewModel tests for every state; Compose UI tests for every screen state; token added to requests
  - [ ] Security: OTP and tokens never logged; rate-limit message shown when Supabase limits OTPs
  - [ ] Flow: on the phone, log in with a test number → close and reopen (still logged in) → logout

### APP-011 · Look and feel: the rest of the design system
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004 · Decision: D-031
- **Why:** the team wants screens that feel as modern and smooth as popular consumer apps. APP-004 already built the base
  (team, 2026-10-03: "everything polished"): theme in `ui/theme/` (placeholder brand colours, light + dark, type, shapes,
  spacing) and `ui/components/` (buttons with press feedback + loading, +91 phone field, 6-box OTP field, field message,
  brand header, sheet card, error state, loading skeleton). This task adds what the next screens need, so they don't
  invent their own.
- **Needs from team:** optional: 2–3 apps whose look you like, and a brand colour / logo (one file to swap: `ui/theme/Color.kt`).
- **Scope:**
  - components: cards (incl. a salon card), chips, bottom sheet, top bar, list item, empty state, snackbar
  - a debug-only "components" screen showing every piece in light and dark
  - not included: logo and launcher icon (Pre-launch), screens of later tasks
- **Done when:**
  - [ ] Tests: Compose UI test per component state (normal, disabled, loading, error), light and dark
  - [ ] Accessibility: text contrast ≥ 4.5:1, touch targets ≥ 48 dp, TalkBack labels, works at the largest font size
  - [ ] Flow: on the phone, the components screen looks right in light and dark mode; the largest font still fits

### APP-005 · Onboarding: customer or salon?
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004, APP-011, BE-016, BE-018 · Decisions: D-024, D-030, DF-23
- **Why:** one app, two kinds of users; each sees only their own interface.
- **Needs from team:** nothing.
- **Flow (first login only):** "How will you use Glide?"
  - **"I want to book salons"** → your name (required) + email (optional) → customer side
  - **"I run a salon"** → salon onboarding (APP-006) → salon side. Managers and staff don't pick this: their salon adds their number (APP-012)
  - a number already added by a salon skips this screen and opens the salon side (DF-23)
  - the choice is saved on the backend and is **final**: no switching side (D-030)
- **Done when:**
  - [ ] Tests: every screen state; customer path, salon path, returning user goes straight to their side, added staff number skips the question
  - [ ] Security: a customer can only read/change their own profile (backend test)
  - [ ] Flow: on the phone, new test number → "book salons" → name → customer side; reopen → straight there

### APP-006 · Salon onboarding: create your salon, then verification
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005, BE-017 · Spec: E2.1; spec v2 §4 onboarding · Decisions: D-033, DF-24
- **Needs from team:** nothing; fields below come from the plan (veto any). Razorpay may ask for more KYC details in Phase 6.
- **Flow (owner):**
  - "Create your salon": name, phone, address, type (men / women / unisex, from spec v2 filters) → you are its **owner**
  - bank details: account holder name, account number (typed twice), IFSC → "Submit for verification"
  - "Under verification" screen: what happens next; details can still be edited. When an admin approves (BE-022, WEB-007) the
    salon is **live** and the salon home opens; if rejected, the reason is shown → fix → submit again
- **Done when:**
  - [ ] Tests: every screen state; create → submit → under verification → live; rejected → fix → resubmit
  - [ ] Security: the account number is masked once saved and never logged; tenant isolation tests (BE-017)
  - [ ] Flow: on the phone, test number A creates "Test Salon A" and submits bank details → "Under verification"; an admin
    approves on the website → the app shows the salon as live

### APP-007 · Customer side: tabs
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005 · Spec: C1.3
- **Scope:** bottom navigation **Home, Search, Bookings, Profile** (spec v2); empty states for the first three until their features arrive; Profile shows name/email (edit) and logout.
- **Done when:** tests for navigation and Back; on the phone all four tabs open.

### APP-008 · Salon side: home + menu by role
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-006 · Decision: D-034
- **Scope:** salon home showing the salon's name and status (under verification / live) and your role. Owner and manager get the
  full menu; staff see only "My bookings" (empty until Phase 3). Menu: logout, plus "switch salon" only if Q-016 keeps several
  salons per person (the app then sends the chosen salon with each request, `X-Salon-Id`, D-026). The real tabs (calendar,
  customers, services…) arrive with Phase 2–3.
- **Done when:** tests for each role's home and menu; on the phone an owner, a manager and a staff member each see their salon and role.

### APP-009 · Customer side: location permission and city
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-007 · Spec: C1.4, customer flow 3.1 ("Allow location")
- **Scope:** explain why location is needed → ask permission → if denied, pick a city from a list. Remember the choice.
- **Needs from team:** the launch city (spec v2: "one city, design-partner salons"). Which city?
- **Done when:** tests for allow / deny / "don't ask again"; on the phone both paths reach Home.

### APP-010 · Crash reports + analytics
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004 · Spec: E1.7 (prove design-partner usage)
- **Needs from team:** in the Firebase project (already created), add the Android app `com.glide.android` and send its `google-services.json` privately, never via git.
- **Done when:** a test crash shows in Firebase; no personal data (phone, name) in analytics events.

### APP-012 · Add staff by phone
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-008, BE-017, BE-022 · Spec: E3.2 · Decisions: D-033, D-034, DF-23
- **Needs from team:** nothing.
- **Flow:**
  - owner or manager of a **live** salon → Staff → list (name, or phone until a name exists: Q-012; role) → "Add": phone number
    + role (Manager or Staff) → added; remove someone
  - that person logs in with that number → no "customer or salon?" question → salon side straight away
  - **Staff** see only "My bookings" and can accept their own bookings (Phase 3); owner/manager screens are hidden **and** refused by the backend
- **Done when:**
  - [ ] Tests: every screen state; add, remove, number already a customer (refused: "ask Glide support"), salon not live (no "Add" button, backend refuses)
  - [ ] Security: staff can't open owner/manager screens or call their endpoints (403); an added number joins only that salon
  - [ ] Flow: on the phone, live Test Salon A's owner adds test number B as staff; B logs in → salon side with only "My bookings"

## Next phases (outline: written out in full when the phase starts)

**Phase 2: Salon side, setup** (salon plan E2–E4; spec v2 §4)
- APP-1xx Working hours, closed days · services list/add/edit (name, category, duration, price) · staff working hours (adding staff is APP-012) · salon's customer list, search, add walk-in, history

**Phase 3: Salon side, appointments** (salon plan E5; spec v2 §4)
- APP-2xx Calendar day view per staff member · book walk-in/phone appointment · reschedule/cancel · complete / no-show (owner/manager) · new-booking push alert · **staff accept their own bookings** (D-034) · **no internet = view only** (D-019)

**Phase 4: Customer side, find and book** (spec v2 EC2–EC3; flows 3.1–3.3)
- APP-3xx Home: nearby salons, categories, top rated · search + filters (service, price, rating, open now, men/women/unisex) · salon page (photos, services, staff, reviews, policies, map) · cart (services + staff or "any") · slot picker · order summary with policy · My bookings (upcoming/past, status, reschedule, cancel with refund amount shown first, directions, call salon)

**Phase 5: Salon side, billing and invoices** (salon plan E8–E9)
- APP-4xx Bill for a completed appointment · share invoice (paying a bill comes in Phase 6: online only, no cash, D-029)

**Phase 6: Payments, refunds, subscriptions** (spec v2 EC4; salon plan E8.3, E12; D-028: all online money through our Razorpay)
- APP-5xx Razorpay Checkout on the customer side (UPI, cards, wallets; full / advance / pay at salon by link or UPI QR) · salon bank/KYC setup (linked account) · payment link / UPI QR at the counter · day summary of online payments · refund status · salon payouts screen · salon subscription (one plan, ₹179–₹400 a month, D-032)

**Phase 7: Disputes, reviews, reports** (spec v2 EC5–EC7; salon plan E10)
- APP-6xx Report a problem (48 h, photos) · salon dispute inbox · reviews after visit, salon replies · notification preferences · owner reports

## Pre-launch (outline)

- APP-9xx **Staging build** with a visible STAGING banner via Firebase App Distribution. _Needs from team: testers' emails, a Firebase service-account key as a GitHub secret._
- APP-9xx Launcher icon + logo (the look itself is APP-011). _Needs from team: the logo._
- APP-9xx Release signing + Play Store listing (privacy policy, account deletion, data-safety form: spec v2 risks). _Needs from team: Play Console account (US$25 one-time), upload keystore._

## Not planned

- A separate salon app and customer app: **one app** (D-023), even though spec v2 §0 lists two.
- Switching between the customer side and the salon side (D-030).
- Indian languages: English only (D-018). Text stays in resource files, so they can be added later.
