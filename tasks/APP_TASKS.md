# 📱 Android App: To Do

**One app for everyone** (D-023): at onboarding the user says whether they're a **customer** or a **salon** (owner or
staff, D-039), and the app shows that side's screens (D-024). The choice is final: no switching (D-030). Lives in `android/app`.
Every screen uses the shared design system (APP-011, D-031): modern, smooth and interactive.
Kotlin · Jetpack Compose + Material 3 · MVVM (UI → ViewModel → UseCase → Repository) · Hilt · Retrofit.
What to build: [docs/PRODUCT.md](../docs/PRODUCT.md) (the whole MVP, D-038).
Order across App / Web / Backend: [README.md § Build order](README.md#build-order) · Done so far: [APP_TASKS_COMPLETED.md](APP_TASKS_COMPLETED.md)

## Now: Phase 1, Login and onboarding

### APP-006 · Salon onboarding: create your salon, then verification
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005, BE-017 · Spec: PRODUCT §6.1 · Decisions: D-033, DF-24
- **Needs from team:** nothing; fields below come from the plan (veto any). Razorpay may ask for more KYC details in Phase 6.
- **Flow (owner):**
  - "Create your salon": name, phone, address, type (men / women / unisex, also a customer search filter) → you are its **owner**
  - bank details: account holder name, account number (typed twice), IFSC → "Submit for verification"
  - "Under verification" screen: what happens next; details can still be edited. When an admin approves (BE-022, WEB-007) the
    salon is **live** and the salon home opens; if rejected, the reason is shown → fix → submit again
- **Done when:**
  - [ ] Tests: every screen state; create → submit → under verification → live; rejected → fix → resubmit
  - [ ] Security: the account number is masked once saved and never logged; tenant isolation tests (BE-017)
  - [ ] Flow: on the phone, test number A creates "Test Salon A" and submits bank details → "Under verification"; an admin
    approves on the website → the app shows the salon as live

### APP-007 · Customer side: tabs
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-005 · Spec: PRODUCT §5
- **Scope:** bottom navigation **Home, Search, Bookings, Profile**; empty states for the first three until their features arrive; Profile shows name/email (edit) and logout.
- **Done when:** tests for navigation and Back; on the phone all four tabs open.

### APP-008 · Salon side: home + menu by role
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-006 · Decision: D-039
- **Scope:** salon home showing the salon's name and status (under verification / live) and your role. The owner gets the
  full menu; staff see only "My bookings" (empty until Phase 3). Menu: logout (one salon per person, so no "switch salon", D-035). The real tabs (calendar,
  customers, services…) arrive with Phase 2–3.
- **Done when:** tests for each role's home and menu; on the phone an owner and a staff member each see their salon and role.

### APP-009 · Customer side: location permission and city
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-007 · Spec: PRODUCT §5
- **Scope:** explain why location is needed → ask permission → if denied, pick a city from a list. Remember the choice.
- **Needs from team:** the launch city (one city first, with the design-partner salons). Which city?
- **Done when:** tests for allow / deny / "don't ask again"; on the phone both paths reach Home.

### APP-010 · Crash reports + analytics
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-004 · Spec: PRODUCT §7 (prove design-partner usage)
- **Needs from team:** in the Firebase project (already created), add the Android app `com.glide.android` and send its `google-services.json` privately, never via git.
- **Done when:** a test crash shows in Firebase; no personal data (phone, name) in analytics events.

### APP-012 · Add staff by phone
- **Phase:** 1 · **Status:** ⬜ To do · **Depends on:** APP-008, BE-017, BE-022 · Spec: PRODUCT §6.2 · Decisions: D-033, D-039, DF-23
- **Needs from team:** nothing.
- **Flow:**
  - the owner of a **live** salon → Staff → list (name, or phone until a name exists: Q-012; role) → "Add": phone number
    → added as staff; remove someone
  - that person logs in with that number → no "customer or salon?" question → salon side straight away
  - **Staff** see only "My bookings" and can accept their own bookings (Phase 3); owner screens are hidden **and** refused by the backend
- **Done when:**
  - [ ] Tests: every screen state; add, remove, number already a customer or already in another salon (refused, D-035), salon not live (no "Add" button, backend refuses)
  - [ ] Security: staff can't open owner screens or call their endpoints (403); an added number joins only that salon
  - [ ] Flow: on the phone, live Test Salon A's owner adds test number B as staff; B logs in → salon side with only "My bookings"

## Next phases (outline: written out in full when the phase starts)

**Phase 2: Salon side, setup** (PRODUCT §6.3)
- APP-1xx Working hours, closed days · services list/add/edit (name, category, duration, price) · staff working hours (adding staff is APP-012) · salon's customer list, search, add walk-in, history

**Phase 3: Salon side, appointments** (PRODUCT §6.4)
- APP-2xx Calendar day view per staff member · book walk-in/phone appointment · reschedule/cancel · complete / no-show (staff for their own, owner for any) · new-booking push alert · **staff accept their own bookings** (D-039) · **no internet = view only** (D-019)

**Phase 4: Customer side, find and book** (PRODUCT §5)
- APP-3xx Home: nearby salons, categories, top rated · search + filters (service, price, rating, open now, men/women/unisex) · salon page (photos, services, staff, reviews, policies, map) · cart (services + staff or "any") · slot picker · order summary with policy · My bookings (upcoming/past, status, reschedule, cancel with refund amount shown first, directions, call salon)

**Phase 5: Salon side, billing and invoices** (PRODUCT §6.5)
- APP-4xx Bill for a completed appointment · share invoice (paying a bill comes in Phase 6: online only, no cash, D-029)

**Phase 6: Payments, refunds, subscriptions** (PRODUCT §8; D-028: all online money through our Razorpay)
- APP-5xx Razorpay Checkout on the customer side (UPI, cards, wallets; full / advance / pay at salon by link or UPI QR) · salon bank/KYC setup (linked account) · payment link / UPI QR at the counter · day summary of online payments · refund status · salon payouts screen · salon subscription (one plan, ₹179–₹400 a month, D-032)

**Phase 7: Disputes, reviews, reports** (PRODUCT §6.6, §9)
- APP-6xx Report a problem (48 h, photos) · salon dispute inbox · reviews after visit, salon replies · notification preferences · owner reports

## Pre-launch (outline)

- APP-9xx **Staging build** with a visible STAGING banner via Firebase App Distribution. _Needs from team: testers' emails, a Firebase service-account key as a GitHub secret._
- APP-9xx Launcher icon + logo (the look itself is APP-011). _Needs from team: the logo._
- APP-9xx Release signing + Play Store listing (privacy policy, account deletion, data-safety form: PRODUCT §15). _Needs from team: Play Console account (US$25 one-time), upload keystore._

## Not planned

- A separate salon app and customer app: **one app** (D-023).
- Switching between the customer side and the salon side (D-030).
- Indian languages: English only (D-018). Text stays in resource files, so they can be added later.
