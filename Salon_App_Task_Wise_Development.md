# Salon Platform v1: Salon App, Appointment Engine, Billing, Admin

> **Draft for team review**, written by Claude on 2026-10-02 at the team's request (Q-009). It is a reconstruction: the original
> salon plan that ChatGPT.md refers to was never added to the repo. **Only what has become a task in `tasks/` is approved**;
> every later epic is confirmed with the team when its phase starts.
> [ChatGPT.md](ChatGPT.md) (spec v2: customer side, marketplace payments, disputes) builds on this file; together they are the product plan.
> Built only from what the plan already says: spec v2 references (epics E2, E3, E4, E5, E8, E12; "salon app, appointment engine,
> billing, admin"), [docs/TECH_STACK.md](docs/TECH_STACK.md) and the team decisions in [docs/DECISIONS.md](docs/DECISIONS.md).
> **❓ = a detail the plan doesn't give. The team answers it before that epic starts. Nothing marked ❓ gets built on a guess.**
>
> How it is used: this file says **what** to build. When an epic is approved, it is split into small tasks
> (`BE-`, `APP-`, `WEB-`) in [tasks/](tasks/README.md), built one task at a time, and checked by the team after each task.

---

## 0. Platform shape (from spec v2)

| Surface | Who | Tech | Lives in |
|---|---|---|---|
| **Android app, salon side** | owner, manager, staff (D-034) | Kotlin + Compose | `android/app` (one app, D-023) |
| Android app, customer side | people booking salons | Kotlin + Compose | same app; side chosen once at onboarding, no switching (D-024, D-030) |
| Admin web | our team | Next.js (React + TS) | `admin/` |
| Backend | all | Ktor + PostgreSQL | `backend/` |
| Booking web link (optional) | customers via Instagram/WhatsApp | React | later |

Rules that apply to every epic: multi-tenant (`salon_id` on every salon-owned table, D-025), WhatsApp and Razorpay only through the backend,
secrets only in environment variables, money in paise, every money/admin action in the audit log.

## 1. Who can do what (roles, D-034)

| Action | Owner | Manager | Staff |
|---|---|---|---|
| Salon profile, hours, policies, bank details | ✅ | ✅ (bank details: Q-014) | ❌ |
| Services and prices | ✅ | ✅ | ❌ |
| Staff (add by phone, remove, hours) | ✅ | ✅ (adding managers: Q-014) | ❌ |
| Customers (salon's list) | ✅ | ✅ | ❌ |
| Appointments | all | all | **see and accept their own only** |
| Mark done / no-show, billing | ✅ | ✅ | ❌ |
| Reports, revenue | ✅ | ✅ | ❌ |
| Subscription (pay Glide) | ✅ | ✅ (Q-014) | ❌ |

Team decision (D-034): the manager also works in the salon and has every permission; staff only see and accept their own bookings.
A receptionist is added as Manager or Staff. One person in several salons: Q-016.

---

## E1: Foundations (accounts, tenants, safety)

**Goal:** everything later epics stand on. Partly listed already as Phase 1 in `tasks/`.

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E1.1 | ~~Two apps / multi-module split~~ **dropped: one app (D-023)**; onboarding asks "customer or salon?" (D-024) | A | One app shows the right side per user |
| E1.2 | Supabase Auth phone OTP (SMS via Twilio) in the app, both sides; backend verifies the token (D-016) | A-S/B | Login, logout, token refresh; test phone numbers work without SMS |
| E1.3 | `salons`, `salon_members(user_id, salon_id, role)`; every query scoped by `salon_id`, picked with the `X-Salon-Id` header after a membership check (D-026); Postgres row-level security with a limited database user as a second guard (D-027) | B | Test: salon A can't read/change salon B's data, for every endpoint |
| E1.4 | Roles and permissions from §1 enforced on the backend (not only hidden buttons) | B | Wrong role → 403 test per endpoint |
| E1.5 | Audit log (`audit_log`): who changed what, when, in which salon | B | Every create/update/delete writes one row |
| E1.6 | Seed script: 2 test salons × every user type (D-012) | B | Same users every run; documented in TESTING.md |
| E1.7 | Firebase Crashlytics + Analytics in the app (prove design-partner usage) | A | Test crash visible in Firebase |
| E1.8 | No-internet = view only (D-019): Room cache of the last loaded appointments/customers + banner | A-S | Airplane mode shows saved data, edit buttons disabled |

## E2: Salon onboarding

**Goal:** a salon owner installs the app and gets a working salon in a few minutes. (Spec v2 §4 adds marketplace steps: bank/KYC,
photos, map pin, policies; those are spec v2 tasks C2.6 and C4.2 and come later.)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E2.1 | Owner sign-up → create salon: name, phone, address, ❓ GST number?, salon type (men/women/unisex, also a spec v2 filter) + bank details (holder name, account number, IFSC; stored encrypted, DF-24) | A-S/B | Salon exists, owner is its first member |
| E2.5 | **Verification by our team (D-033):** submit → *under verification* → an admin approves (→ *live*) or rejects with a reason (→ fix, resubmit). Only a live salon adds staff, shows up for customers and takes bookings | A-S/W/B | Status changes audit-logged; nothing customer-facing before *live* |
| E2.2 | Working hours per weekday + ❓ breaks, ❓ holidays/closed days | A-S/B | Hours drive appointment availability (E5) |
| E2.3 | Salon settings screen (edit everything from E2.1–E2.2) | A-S | Changes saved, audit-logged |
| E2.4 | Onboarding checklist ("add services, add staff, first appointment") | A-S | Shows what's missing |

❓ Q-S2: exact sign-up fields (GST number?). ~~Is a salon approved by our team first?~~ Yes: D-033.

## E3: Services and staff

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E3.1 | Service catalogue: name, category (spec v2 examples: Haircut, Facial, Bridal), **duration**, **price (paise)**, ❓ gender, ❓ tax | A-S/B | Unique name per salon; price ≥ 0; duration > 0 |
| E3.2 | Staff: owner or manager adds a phone number with a role (Manager or Staff), **live salons only** → that number logs in with OTP and lands straight on the salon side (D-034, DF-23) | A-S/B | Only that phone number joins; refused before *live* |
| E3.3 | Which services each staff member can do | A-S/B | Booking only offers capable staff |
| E3.4 | Staff working hours, ❓ breaks, days off / leave | A-S/B | Availability respects them |
| E3.5 | Deactivate staff/service (keep history, hide from booking) | A-S/B | Old bills still show them |

❓ Q-S3: does price vary by stylist (senior stylist costs more)? Service add-ons/variants (e.g. short/long hair)?

## E4: Customers (the salon's own customer list)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E4.1 | Customer list per salon: name, phone, ❓ birthday, ❓ gender, notes | A-S/B | Phone unique per salon |
| E4.2 | Search by name/phone; add a walk-in customer in seconds | A-S | Debounced search |
| E4.3 | Customer detail: visit history, spend, upcoming appointments | A-S/B | Totals match bills |
| E4.4 | Link to the app user (`app_users`, DF-18) with the same phone (spec v2: salons bring their regulars into the app via QR/WhatsApp link) | B | ❓ needs the customer's consent? (DPDP Act) |

❓ Q-S4: can a salon import existing customers (contacts/CSV)? Marketing messages to customers (needs consent and WhatsApp templates)?

## E5: Appointment engine and calendar (the heart of the product)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E5.1 | Availability API: free slots from salon hours + staff hours + leave + service durations + existing bookings, ❓ buffer between appointments, ❓ slot step (15 min?) | B | Correct on edge cases (closing time, breaks, multi-service) |
| E5.2 | Create appointment (sources: `WALK_IN`, `PHONE`, `CUSTOMER_APP`) with one or more services and staff (or "any") | A-S/B | **Double booking impossible**, enforced by the database, tested with parallel requests |
| E5.3 | Statuses: `BOOKED → CONFIRMED → IN_PROGRESS → COMPLETED`, plus `CANCELLED`, `NO_SHOW` (spec v2 mentions BOOKED/CONFIRMED, Complete, No-show; ❓ IN_PROGRESS needed?) | B | Invalid transitions rejected (state-machine tests) |
| E5.4 | Calendar: day view per staff member, ❓ week view; tap a slot to book | A-S | Updates when someone else books (push or refresh) |
| E5.5 | Reschedule and cancel (salon side), with reason | A-S/B | Customer notified (E6) |
| E5.6 | Mark complete / no-show (spec v2: complete triggers payout release later; no-show fee per policy) | A-S/B | Timestamp stored; audit-logged |
| E5.7 | Accept/reject for app bookings: **the staff member accepts their own bookings** (D-034); spec v2 §4 had auto-accept by default or manual within 15 min | A-S/B | ❓ Is auto-accept still the default? Unanswered → ❓ auto-accept or auto-reject? |
| E5.8 | Slot lock (10 min) while a customer pays (spec v2 C3.3) | B | Lock expires; late payment handled (spec v2 §5.4) |

❓ Q-S5: buffer and slot step; can one appointment use two stylists (e.g. haircut + colour by different people)?

## E6: Notifications

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E6.1 | FCM push to salon staff: new booking, cancellation, reschedule | B/A-S | Arrives within seconds; tap opens the appointment |
| E6.2 | WhatsApp Business Cloud API (through backend only): booking confirmation, reminder, cancellation to customers | B | Templates approved by Meta; delivery status stored |
| E6.3 | Message log per salon (what was sent, delivered, failed) | B/A-S | Visible to owner |

Needs from team: Meta WhatsApp Business verification + approved templates (in progress, 2026-10-02). SMS fallback (MSG91/Twilio) ❓ wanted?

## E7: Reminders and daily summary (scheduler)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E7.1 | Scheduler in the backend (simple cron worker or Quartz, from the stack doc) | B | Survives restarts; no double sends |
| E7.2 | Reminder before each appointment (spec v2: 2 h before), ❓ also day-before? | B | Sent once, skipped if cancelled |
| E7.3 | Daily summary to the owner (today's appointments, yesterday's revenue) ❓ via push or WhatsApp, ❓ what time | B | Opt-out setting |

## E8: Billing at the salon (point of sale)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E8.1 | Bill for a completed appointment: services (prefilled), ❓ products sold, ❓ discounts, ❓ tips, ❓ GST | A-S/B | Totals computed on the server, in paise |
| E8.2 | ~~Record payment: cash, UPI, card (at counter)~~ **Online only (D-029):** a bill is paid through E8.3 or the customer's in-app payment; split payments allowed | A-S/B | Bill status `UNPAID → PARTIAL → PAID` |
| E8.3 | Collect online: Razorpay Payment Link / UPI QR through **our** Razorpay account with Route; platform fee to us, salon's share to its linked account (D-028). Needs the salon's KYC (spec v2 C4.2), so built in Phase 6 | B/A-S | Paid only after verified webhook; split matches the ledger |
| E8.4 | Pay-at-salon remainder from customer-app advance carried into the bill (spec v2 C4.10) | B | Advance shown, remainder correct |
| E8.5 | Day summary: online payments for the day (no cash, D-029) | A-S/B | Matches the sum of bills and Razorpay |

❓ Q-S6: does the salon sell products (shampoo etc.) and track stock? GST registered salons only, or all?

## E9: Invoices

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E9.1 | Server-side PDF invoice (OpenHTMLToPDF or iText, from the stack doc), stored in Cloudflare R2/S3 | B | Same bill → same PDF; private links that expire |
| E9.2 | Invoice numbering per salon, ❓ format, ❓ GST invoice fields | B | No gaps or duplicates |
| E9.3 | Share invoice to the customer by WhatsApp (E6) or download | A-S/B | Link works for the customer only |

## E10: Reports for the salon owner

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E10.1 | Dashboard: today/this week/month: revenue, appointments, no-shows, new vs returning customers | A-S/B | Numbers match bills and appointments |
| E10.2 | Per-staff report: appointments, revenue, ❓ commission % for staff | A-S/B | Owner and manager only |

❓ Q-S7: which 3–5 numbers matter most to your design-partner salons?

## E11: Admin web (our team)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E11.1 | Admin login: email code + authenticator app, invite-only, admins add admins (D-013, DF-16) | W/B | Non-admins get nothing |
| E11.2 | Salons list and detail (members, plan, activity, status), suspend/reactivate | W/B | Audit-logged |
| E11.3 | **Design-partner tracking** (spec v2 "never cut"): per salon, logins, appointments/bills per week, last active | W/B | Weekly view exportable ❓ |
| E11.4 | Audit log viewer | W/B | Filter by salon/user/date |
| E11.5 | Support tools: look up a salon/customer by phone, resend a message, fix a wrong customer/salon pick (DF-22) | W/B | Every action audit-logged |
| E11.6 | **Verify salons (D-033):** queue of salons under verification → profile + bank details → approve, or reject with a reason | W/B | Every decision and bank-details view audit-logged |
| E11.7 | Subscription price: change the plan's price (D-032, DF-25) | W/B | New price used for new subscriptions; audit-logged |
| | Spec v2 adds: dispute console (C5.6), payments explorer (C8.2), customers (C8.3), ~~listing approval (C8.4)~~ now E11.6, payouts (C8.5), GMV dashboard (C8.1) | | |

## E12: Subscriptions (how salons pay Glide)

| ID | Task | Owner | Acceptance |
|---|---|---|---|
| E12.1 | **One plan for now (D-032):** ₹179–₹400 a month, price kept in our database and editable by admins (E11.7, DF-25); more plans later; ❓ exact price, ❓ free trial length | B/P | Plan exists in Razorpay test mode; a price change creates a new Razorpay plan |
| E12.2 | Owner (or manager, Q-014) subscribes from the app (Razorpay Checkout), status from **verified webhooks only** | A-S/B | Replay-safe |
| E12.3 | Status in app + admin: trial, active, past due, cancelled | A-S/W/B | Matches Razorpay |
| E12.4 | Unpaid handling: ❓ grace period length, ❓ then read-only or blocked | B | Never deletes salon data |

❓ Q-S8: exact price within ₹179–₹400 and free trial; what happens to design-partner salons (free?); salons already paying when the price changes (Q-015)

---

## Data model (salon side, first draft; conventions in docs/DATABASE.md)

```
salons(id, name, phone, address, gst_number?, type, timezone,
       status[DRAFT|UNDER_VERIFICATION|LIVE|REJECTED|SUSPENDED], rejection_reason?, live_at?)   -- D-033
salon_bank_details(salon_id, holder_name, account_number_encrypted, account_last4, ifsc)       -- DF-24
salon_verifications(id, salon_id, admin_id, decision[APPROVED|REJECTED], reason?, decided_at)  -- history, D-033
salon_members(id, salon_id, phone, user_id?, role[OWNER|MANAGER|STAFF], status[ACTIVE|REMOVED], added_by)
                                       -- user_id filled at that number's first login (DF-23); exactly one OWNER
working_hours(id, salon_id, staff_id?, weekday, opens_at, closes_at)        -- staff_id null = salon hours
time_off(id, salon_id, staff_id, starts_at, ends_at, reason)
services(id, salon_id, name, category, duration_min, price_paise, active)
staff_services(salon_id, staff_id, service_id)
customers(id, salon_id, phone, name, notes, app_user_id?)                    -- salon's own list (E4)
appointments(id, salon_id, customer_id, source, status, starts_at, ends_at, created_by)
appointment_services(id, salon_id, appointment_id, service_id, staff_id, price_paise, duration_min)
bills(id, salon_id, appointment_id?, number, status, subtotal_paise, discount_paise, tax_paise, total_paise)
bill_items(id, salon_id, bill_id, kind[SERVICE|PRODUCT], ref_id, qty, unit_price_paise)
bill_payments(id, salon_id, bill_id, amount_paise, razorpay_payment_id)      -- online only, no cash (D-029)
invoices(id, salon_id, bill_id, number, pdf_key, issued_at)
subscription_plans(id, name, price_paise, razorpay_plan_id, active)                      -- platform table; one row now (D-032, DF-25)
subscriptions(id, salon_id, plan_id, price_paise, razorpay_subscription_id, status, current_period_end)
notifications(id, salon_id, channel[PUSH|WHATSAPP|SMS], template, to_ref, status, sent_at)
audit_log(id, salon_id?, actor_id, actor_type, action, entity, entity_id, before, after, at)
webhook_events(id, provider, event_id UNIQUE, type, received_at, processed_at)   -- idempotency
```

## API (paths per D-017)

```
SALON AUTH      Supabase Auth on the device → backend verifies the token
ONBOARDING      POST /v1/salon/salons   GET|PATCH /v1/salon/me/salon   PUT /v1/salon/bank-details
                POST /v1/salon/submit-for-verification   PUT /v1/salon/hours
SERVICES        GET|POST /v1/salon/services   PATCH /v1/salon/services/{id}
STAFF           GET|POST /v1/salon/staff   DELETE /v1/salon/staff/{id}   PUT /v1/salon/staff/{id}/hours
CUSTOMERS       GET|POST /v1/salon/customers   GET /v1/salon/customers/{id}
APPOINTMENTS    GET /v1/salon/availability   GET|POST /v1/salon/appointments
                POST /v1/salon/appointments/{id}/(confirm|start|complete|no-show|cancel|reschedule)
BILLING         POST /v1/salon/bills   POST /v1/salon/bills/{id}/payments   POST /v1/salon/bills/{id}/payment-link
INVOICES        GET /v1/salon/invoices/{id}/pdf
REPORTS         GET /v1/salon/reports/summary?from&to
SUBSCRIPTION    GET /v1/salon/subscription   POST /v1/salon/subscription
ADMIN           GET /v1/admin/salons   GET /v1/admin/salons/{id}   GET /v1/admin/audit-log
                POST /v1/admin/salons/{id}/(approve|reject)   PUT /v1/admin/subscription-plans/{id}
WEBHOOKS        POST /webhooks/razorpay
```

## Proposed phases (order follows spec v2: "salon core must work first, because it is what you sell")

| Phase | Epics | Demo at the end |
|---|---|---|
| 0 | skeleton, CI/CD (in progress) | three apps talk to the backend; CI green |
| 1 | E1, E2.1, E2.5, E3.2, E11.1, E11.6 | owner creates a salon → our team verifies it → owner adds staff; two test salons can't see each other |
| 2 | E2, E3, E4 | a salon is fully set up: hours, services, staff, customers |
| 3 | E5, E6.1 | salon books, reschedules, completes appointments on the calendar |
| 4 | spec v2 EC1–EC3, E6.2, E7 | a customer finds a salon and books on the customer side of the app (no payment yet) |
| 5 | E8.1, E9 | salon makes bills and shares invoices (paying them comes in Phase 6: online only, D-029) |
| 6 | spec v2 EC4, E8.2–E8.5, E12 | online payments in the app and at the counter, hold/release, refunds, salon subscriptions (test mode) |
| 7 | spec v2 EC5–EC8, E10, E11 | disputes, reviews, admin console, reports |
| Pre-launch | hosting, monitoring, backups, store release | design partners use staging builds |

## End-to-end scenarios (salon side; acceptance tests)

1. **Setup:** owner signs up → creates the salon with bank details → our team approves it (live) → owner adds hours, 3 services and 2 staff by phone → a staff member logs in with OTP, lands on the salon side and sees only their own bookings, which they accept.
2. **Walk-in:** manager adds a walk-in customer → books "Haircut" with the first free staff member → manager marks it done → customer pays the bill by UPI QR (online only, D-029) → invoice shared on WhatsApp.
3. **Double booking:** two managers book the same staff member at the same time → exactly one succeeds, the other sees "slot just taken".
4. **Tenant isolation:** owner of Salon A tries Salon B's appointment ID → 404.
5. **No internet:** phone goes offline → today's appointments still visible with a banner; "new appointment" disabled.
6. **Subscription lapses (test mode):** webhook marks past due → owner sees a banner → ❓ after the grace period the app becomes read-only, data kept.

## Definition of Done (every salon-side task)

Same as [tasks/README.md](tasks/README.md): tests (unit, integration on real Postgres, UI states), tenant-isolation test,
role test, audit-log entry for changes, end-to-end check on a real phone, report to the team before the next task.
Money features also follow spec v2's money DoD (test-mode success/failure/timeout/duplicate/refund, webhook replay, ledger balance).

## Questions for the team (answer per phase, not all now)

| ID | Before phase | Question |
|---|---|---|
| Q-S1 | 1 | ~~Permissions table~~ answered: D-034 (owner-only exceptions: Q-014) |
| Q-S2 | 2 | Sign-up fields (GST number?). ~~Does our team approve new salons first?~~ Yes: D-033 |
| Q-S3 | 2 | Price per stylist level? Service variants/add-ons? |
| Q-S4 | 2 | Import existing customers? Marketing messages? |
| Q-S5 | 3 | Buffer between appointments, slot step, multi-stylist appointments, unanswered app bookings |
| Q-S6 | 5 | Products/stock? GST on bills? Discounts/tips? |
| Q-S7 | 7 | Top numbers for the owner dashboard |
| Q-S8 | 6 | Exact price within ₹179–₹400 (D-032), trial, design-partner pricing, grace period, salons already paying when the price changes (Q-015) |
