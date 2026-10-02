# Salon Platform v2: Customer App, Marketplace Payments and Disputes

> **Read this with [docs/DECISIONS.md](docs/DECISIONS.md): where they differ, the decisions win.** Changed since this spec:
> - §0 and C1.1: **one app** with a customer side and a salon side, not two apps and no multi-module split (D-023, D-024).
> - Login (§2, C1.2, §8 `POST /c/auth/otp/verify`): **Supabase phone OTP on the phone**; our backend only checks Supabase's
>   token, so it has **no** OTP endpoint (D-016).
> - §8 paths get a `/v1` prefix: `/v1/c/...`, `/v1/salon/...`, `/v1/admin/...`; login, side and profile for everyone at `/v1/me` (D-017, DF-18).
> - §7 `customers_app_users`: the profile lives on `app_users` instead (DF-18).
> - Admin web is Next.js (D-002).
> - Money (§3.4, §5): **online only, all through our Razorpay** (D-028, D-029). No cash: "pay at salon" means paying there by
>   link / UPI QR. So the "fall back to pay-at-salon" in the risks and cut list no longer avoids Razorpay.
> - The fee numbers in examples (10%, ₹50, 5–12%) are illustrations; the real platform fee is decided later (Q-013).
> - The sprint plan's dates are history: the deadline doesn't set the order (D-020); the order is in [tasks/README.md](tasks/README.md).
> - The next line refers to an earlier `Salon_App_Task_Wise_Development.md` that was never added to the repo. The current file
>   with that name is Claude's reconstruction (Q-009). The "WhatsApp + web link only" decision is replaced anyway (D-015).

**This file supersedes the "customers use WhatsApp + web link only" decision in `Salon_App_Task_Wise_Development.md`.** Everything else in that file (salon app, appointment engine, billing, admin) still stands unless changed here.

---

## 0. New Platform Shape

| Surface | Who | Tech |
|---|---|---|
| **Customer Android app** (new) | People booking salons | Kotlin + Jetpack Compose |
| **Salon Android app** | Owners/staff | Kotlin + Jetpack Compose |
| **Admin web app** | Our team | React + TypeScript |
| **Backend** | All | Ktor + PostgreSQL |
| **Booking web link** | Optional, for sharing on Instagram/WhatsApp | React (keep, it is cheap) |

**Android project layout (multi-module, shared code):**
```
/android
  /app-customer      → customer application
  /app-salon         → salon application
  /core-network      → Retrofit, auth, API models
  /core-database     → Room
  /core-ui           → design system
  /core-common       → utils, analytics
  /feature-booking   → shared booking/appointment UI pieces
```

---

## 1. Honest Scope Check (read this before building)

A two-sided marketplace has the **cold-start problem**: customers won't open an app with 3 salons in it, and salons won't join without customers.

**How to handle it in the program timeline:**
1. **Salon side is the product you sell** (subscription). It must be useful even with zero marketplace customers. That is what design partners and paying businesses will judge.
2. **Customer app launches in one city, only with your design-partner salons.** Do discovery simply (list + map + search), not a recommendation engine.
3. Salons bring their **existing regulars** into the app (QR at the counter, WhatsApp link → app). This solves the cold start without ad spend.
4. **Ask your faculty mentor** whether platform commission on online bookings counts as "usage-based recurring revenue". The PDF says per-transaction billing counts, but get written confirmation. Keep the **SaaS subscription as the guaranteed revenue**.

---

## 2. Customer App: Features

### P0 (MVP)
| Module | Details |
|---|---|
| Auth | Phone OTP, name, optional email/photo |
| Discovery | Nearby salons (location), search by name/service, filters (service, price, rating, open now, gender: men/women/unisex) |
| Salon page | Photos, about, address + map, hours, services & prices, staff, ratings & reviews, policies |
| Booking | Select services → staff (or any) → date → slot → review → pay |
| Payments | Razorpay: UPI, cards, netbanking, wallets. Options: pay full online, pay advance, or pay at salon (if the salon allows) |
| My bookings | Upcoming / past, status tracker, reschedule, cancel, get directions, call salon |
| Refunds | Refund status and timeline visible in-app |
| Reviews | Rate after a completed visit (verified only) |
| Notifications | Push + WhatsApp for confirmations, reminders, refunds |
| Help and disputes | Raise an issue on any booking (see Section 5) |
| Invoices | Download receipts |

### P1
Favorites, coupons/promo codes, wallet/credits, rebook in one tap, in-app chat with salon, referral, saved payment methods, loyalty points, home-service bookings.

### P2
Packages/memberships, gift cards, "book for someone else", AI recommendations, group bookings.

---

## 3. Customer Flows

### 3.1 First-time booking
```
Install → Phone OTP → Allow location
→ Home: nearby salons, categories (Haircut, Facial, Bridal...)
→ Open salon → view services, reviews, photos
→ Add services to cart → choose staff or "Any available"
→ Pick date → see real-time slots → pick slot
→ Order summary (price, taxes, platform fee, cancellation policy)
→ Choose payment (full / advance / pay at salon)
→ Razorpay checkout → success
→ Slot LOCKED for 10 min during payment, then released if unpaid
→ Booking confirmed → push + WhatsApp
→ Reminder 2h before → arrive → service → rate
```

### 3.2 Reschedule
```
My bookings → Reschedule → new slots (same services)
→ allowed if within policy (e.g., 2h before start, max 2 times)
→ salon notified → no extra charge within policy
```

### 3.3 Cancel (customer)
```
Cancel → reason → app shows refund amount per policy BEFORE confirming
→ confirm → refund initiated automatically → status: Initiated → Processed
```

### 3.4 Pay at salon
```
Pay-at-salon bookings need a small refundable/deductible advance
(e.g., ₹50 or 10%) to cut no-shows. Remaining amount is paid at the salon
via cash / UPI, recorded on the salon bill.
```

---

## 4. Salon-Side Flow Changes (for marketplace)

| Area | Change |
|---|---|
| Onboarding | Add **bank/KYC step** for payouts (Razorpay linked account), upload photos, set location pin, choose policies |
| Listing control | Toggle "Visible in customer app", set booking lead time, max advance days |
| New booking | Booking from the app arrives as `BOOKED/CONFIRMED` with source `CUSTOMER_APP`, push alert to salon |
| Accept/reject | Option: auto-accept (default) or manual accept within 15 min |
| Service complete | Salon taps **Complete** → triggers payout release (Section 5) |
| Customer no-show | Salon marks No-show (with a timestamp) → a no-show fee applies per policy |
| Disputes | Salon sees disputes on its bookings, replies with evidence |
| Payouts screen | Pending, released, refunded, commission deducted, settlement history |
| Reviews | View and reply to reviews, report abusive ones |

---

## 5. Payments with Razorpay (Marketplace Money Flow)

### 5.1 Approach: Razorpay Route
Razorpay Route supports marketplace-style splits to linked accounts (the salons). **It must be enabled on your Razorpay account, and each salon must complete KYC as a linked account.** Confirm availability and current rules with Razorpay support in week 1. This is the highest-risk integration in the project.

> Compliance note: collecting money on behalf of others has legal and tax implications (GST on commission, TCS/TDS as an e-commerce operator, payment aggregator rules). Let Razorpay Route handle the regulated parts, and **consult a CA** before going live. Do not guess here.

### 5.2 Money flow
```
Customer pays ₹1000 (order via Razorpay)
   │
   ▼
Razorpay captures payment
   │  transfer to salon's linked account, created with ON HOLD
   ▼
Platform commission (e.g., 10% = ₹100) stays with us
Salon share (₹900) held until service COMPLETED
   │
   ▼  Salon marks Complete (or auto-complete 6h after slot end, if no dispute)
Release hold → settlement to salon bank (T+N per Razorpay schedule)
```

### 5.3 Payment state machine
```
CREATED → PENDING → CAPTURED → HELD → RELEASED (to salon)
                         │         └──► (dispute) FROZEN → RESOLVED
                         ├──► FAILED
                         └──► REFUND_INITIATED → REFUNDED (full/partial)
```

### 5.4 Rules for correctness (do not skip)
- **Server creates the order.** Never trust the amount from the app.
- **Only the verified webhook** (`payment.captured`, `payment.failed`, `refund.processed`, dispute events) changes payment state. Verify the signature. Make handling idempotent using `webhook_events`.
- Store every Razorpay id: `order_id`, `payment_id`, `transfer_id`, `refund_id`.
- Keep a **double-entry ledger** (Section 7) so every rupee can be traced: customer paid, commission, GST, salon share, refund.
- Slot is locked when the order is created. Expire the lock after 10 min. If payment is captured after expiry (late webhook), either confirm if the slot is still free, else **auto-refund**.
- "Money deducted but booking failed" is the top complaint on every platform. Build a **reconciliation job** (hourly) comparing Razorpay payments against the bookings DB and auto-fixing or flagging mismatches.

### 5.5 Fee structure (starting hypothesis, validate with salons)
| Component | Value |
|---|---|
| Salon SaaS subscription | ₹499 / ₹999 per month (guaranteed revenue) |
| Commission on app bookings | 5 to 12% (lower for repeat customers, 0% on the salon's own regulars via their own link) |
| Customer convenience fee | Small flat amount, shown transparently (optional) |
| Razorpay gateway fee | Account for it in your margin |

---

## 6. Dispute and Refund System

### 6.1 Dispute types
| # | Scenario | Raised by |
|---|---|---|
| D1 | Salon cancelled or did not honour the booking | Customer |
| D2 | Customer did not show up (salon wants a no-show fee) | Salon |
| D3 | Service quality unsatisfactory | Customer |
| D4 | Overcharged / price differs from the app | Customer |
| D5 | Salon late by more than X min | Customer |
| D6 | Payment deducted, booking not confirmed | Customer (auto-detected too) |
| D7 | Duplicate payment | Customer (auto-detected too) |
| D8 | Refund not received | Customer |
| D9 | Customer claims service not done / salon claims done | Both |
| D10 | Bank **chargeback** via Razorpay | Customer's bank |
| D11 | Abusive behaviour / safety complaint | Either |

### 6.2 Default refund policy (make configurable by admin)
| Situation | Outcome |
|---|---|
| Customer cancels > 4h before | 100% refund |
| Customer cancels 2 to 4h before | e.g., 50% refund |
| Customer cancels < 2h or no-show | No refund, salon gets a no-show fee less commission |
| Salon cancels or no-show by salon | 100% refund + goodwill credit ₹50 (P1) + salon penalty score |
| Payment failed but money debited | Auto-refund by Razorpay or us within 5 to 7 working days |
| Duplicate payment | Auto-refund of the extra payment |
| Service quality | Admin decides: re-service, partial refund, or reject |

Show the policy **before** payment. Disputes without a clear policy are impossible to win.

### 6.3 Dispute flow (customer-raised, D3/D4/D5/D9)
```
Customer opens booking → "Report a problem" (within 48h of the slot)
→ choose type → add description + up to 3 photos
→ Payout for this booking is FROZEN (salon share stays on hold)
→ Salon notified → must respond within 24h (reply + evidence)
   ├── Salon agrees → auto refund (full/partial) → CLOSED
   └── Salon disagrees / no reply
         ├── No reply in 24h → auto-resolve in the customer's favour (D3/D4 small amounts)
         └── Disputed → ADMIN MEDIATION
               → admin reviews: booking timeline, photos, messages, history
               → decision: full refund / partial / reject / re-service
               → both parties notified → refund or release executed
               → either side can appeal once (senior admin) within 48h
→ Closed, outcome logged, feeds the trust score
```

### 6.4 Dispute states
```
OPEN → AWAITING_SALON → AWAITING_CUSTOMER (if more info needed)
     → IN_MEDIATION → RESOLVED_REFUND | RESOLVED_PARTIAL | RESOLVED_REJECTED
     → APPEALED → FINAL
     → CLOSED
```
**SLAs:** salon response 24h, admin first response 12h, resolution within 3 days.

### 6.5 Automated detections (no human needed)
- Payment captured but no booking → auto-refund.
- Duplicate `payment_id` for the same slot → refund the duplicate.
- Salon cancels → auto full refund.
- Refund processed more than 7 days ago but still not credited → escalate to Razorpay support.

### 6.6 Chargebacks (D10)
Razorpay notifies disputes via webhooks (`payment.dispute.*`) and the dashboard. Process:
1. Webhook → create `chargeback` record, freeze the related payout.
2. Admin gathers evidence (booking, completion timestamp, invoice, chat, salon confirmation).
3. Submit evidence through Razorpay within its deadline.
4. Outcome: won → release; lost → debit the salon share (per your salon T&C) and mark the customer risk.
> Your **salon T&Cs must allow clawback** for chargebacks and fraud. Get this wording reviewed.

### 6.7 Trust and abuse controls
- **Verified reviews only** (completed bookings). Salon can reply and report.
- **Customer reliability score** (no-shows, refund-abuse count) and **salon reliability score** (cancellations, dispute rate). Repeated offenders get restrictions or a suspension.
- Refund-abuse limit per customer (e.g., 3 quality disputes in 30 days → manual review).
- Safety reporting (D11) goes directly to admin with priority and can suspend a salon immediately.
- Keep all communication **in-platform** (booking messages) as evidence.

### 6.8 Admin dispute console (web)
| Feature | Details |
|---|---|
| Queue | Filter by type, age, amount, SLA breach |
| Case view | Timeline: booking, payments, messages, photos, previous disputes of both parties |
| Actions | Refund (full/partial), reject, request info, re-service, penalise salon, warn customer, escalate |
| Templates | Canned responses and decision reasons |
| Money panel | Held amount, commission, refund impact, ledger preview before confirming |
| Analytics | Dispute rate per salon, per type, average resolution time, refund % of GMV |
| Audit | Every decision logged with admin id |

---

## 7. Data Model Additions

```
customers_app_users(id, phone, name, email, photo_url, fcm_token, status,
                    reliability_score)
salon_listing(salon_id, lat, lng, photos[], about, visible, avg_rating,
              rating_count, reliability_score, policies_json)
bookings_payment(id, appointment_id, order_id, payment_id, amount, advance,
                 commission, gst_on_commission, salon_share, status, held_until)
transfers(id, payment_id, razorpay_transfer_id, salon_id, amount,
          on_hold, released_at)
refunds(id, payment_id, razorpay_refund_id, amount, reason, status,
        initiated_by, created_at, processed_at)
ledger_entries(id, txn_id, account, debit, credit, ref_type, ref_id, at)
  -- accounts: CUSTOMER_PAYMENT, PLATFORM_COMMISSION, SALON_PAYABLE,
  --           REFUND, GATEWAY_FEE, TAX
disputes(id, booking_id, type, raised_by, status, description, resolution,
         refund_amount, assigned_admin, sla_due_at, created_at, closed_at)
dispute_messages(id, dispute_id, sender_type, message, created_at)
dispute_evidence(id, dispute_id, uploaded_by, file_url, type)
chargebacks(id, payment_id, razorpay_dispute_id, status, due_by, evidence_url)
reviews(id, appointment_id, customer_id, salon_id, rating, text,
        salon_reply, status[VISIBLE|HIDDEN|REPORTED])
salon_kyc(salon_id, razorpay_account_id, status, bank_verified)
promo_codes(id, code, type, value, min_amount, expiry)       -- P1
user_favorites(customer_id, salon_id)                         -- P1
```

---

## 8. API Additions

```
CUSTOMER AUTH   POST /c/auth/otp/verify
DISCOVERY       GET /c/salons?lat&lng&q&service&rating&openNow
                GET /c/salons/{id}   GET /c/salons/{id}/reviews
BOOKING         GET /c/availability   POST /c/bookings
                GET /c/bookings   POST /c/bookings/{id}/cancel
                POST /c/bookings/{id}/reschedule
PAYMENT         POST /c/payments/order   POST /c/payments/verify
                GET /c/bookings/{id}/refund-status
REVIEWS         POST /c/bookings/{id}/review
DISPUTES        POST /c/bookings/{id}/disputes   GET /c/disputes
                POST /c/disputes/{id}/messages
SALON SIDE      GET /salon/payouts   GET /salon/disputes
                POST /salon/disputes/{id}/respond   POST /salon/reviews/{id}/reply
                POST /salon/kyc/start
ADMIN           GET /admin/disputes   POST /admin/disputes/{id}/decision
                GET /admin/payments   POST /admin/payments/{id}/refund
                GET /admin/reconciliation   GET /admin/chargebacks
WEBHOOKS        POST /webhooks/razorpay  (payment, refund, transfer, dispute events)
```

---

# TASK-WISE DEVELOPMENT (new/changed epics)

Owners: **A-C** = Customer Android, **A-S** = Salon Android, **B** = Backend, **W** = Admin web, **P** = Product/Integrations/Ops.

## EC1: Customer App Foundation
| ID | Task | Owner | AC |
|---|---|---|---|
| C1.1 | Multi-module Android setup (customer + salon apps share core) | A-C | Both apps build from one repo |
| C1.2 | Customer auth (OTP, profile) | A-C/B | Login, logout, token refresh |
| C1.3 | Customer design system and navigation (bottom nav: Home, Search, Bookings, Profile) | A-C | Consistent with the shared theme |
| C1.4 | Location permission and city selection | A-C | Works with the permission denied fallback |

## EC2: Discovery
| ID | Task | Owner | AC |
|---|---|---|---|
| C2.1 | Listing API with geo query (PostGIS or Haversine) + filters + pagination | B | Under 500 ms for 1k salons |
| C2.2 | Home screen: nearby, categories, top rated | A-C | Loads with skeletons, cached |
| C2.3 | Search (name/service), filter sheet | A-C/B | Debounced, correct results |
| C2.4 | Map view with salon pins | A-C | Google Maps SDK |
| C2.5 | Salon detail page (photos, services, staff, reviews, policies) | A-C | All data from the API |
| C2.6 | Salon listing management (photos upload, location pin, about) | A-S/B | Image compression, CDN |

## EC3: Customer Booking
| ID | Task | Owner | AC |
|---|---|---|---|
| C3.1 | Cart: multi-service selection, staff choice | A-C | Price and duration update live |
| C3.2 | Slot picker (uses availability API) | A-C | No stale slots, slot lock on checkout |
| C3.3 | Slot lock/expiry service (10 min TTL) | B | Released on timeout |
| C3.4 | Order summary with taxes, fees, policy | A-C | Matches server totals |
| C3.5 | My Bookings: upcoming/past/cancelled, detail, status timeline | A-C | Realtime updates via push |
| C3.6 | Reschedule and cancel with policy engine | A-C/B | Refund amount shown before confirm |
| C3.7 | Directions, call salon, add to calendar | A-C | Intents work |

## EC4: Payments (Marketplace)
| ID | Task | Owner | AC |
|---|---|---|---|
| C4.1 | Razorpay account, Route enablement, test keys, written decision on the settlement model | P | Confirmed with Razorpay |
| C4.2 | Salon KYC / linked account onboarding flow | B/A-S | Salon status: pending, verified, rejected |
| C4.3 | Create order with transfer on hold, commission calculation | B | Amounts verified server-side |
| C4.4 | Razorpay Checkout in the customer app (UPI intent, cards, wallets) | A-C | Success, failure, cancel handled |
| C4.5 | Webhook handlers (payment, refund, transfer, dispute) with signature check + idempotency | B | Replays are safe |
| C4.6 | Payout release on completion / auto-complete job | B | Hold released, ledger updated |
| C4.7 | Refund engine (full/partial, policy-driven, via Razorpay API) | B | Status synced via webhook |
| C4.8 | Ledger (double-entry) + payout and earnings screens | B/A-S | Totals reconcile to Razorpay |
| C4.9 | Hourly reconciliation job + alerts | B | Mismatch report to admin |
| C4.10 | Pay-at-salon and advance modes | B/A-C | Remaining amount carried to the bill |
| C4.11 | Invoice/receipt generation (customer and commission invoice to the salon) | B | PDF downloadable |

## EC5: Disputes and Support
| ID | Task | Owner | AC |
|---|---|---|---|
| C5.1 | Dispute data model + state machine + SLA timers | B | Invalid transitions rejected |
| C5.2 | Customer: report a problem (type, text, photos) | A-C | Within the 48h window only |
| C5.3 | Salon: dispute inbox, respond with evidence | A-S | Notified by push and WhatsApp |
| C5.4 | Payout freeze on dispute and release on resolution | B | Money never moves while frozen |
| C5.5 | Auto-resolution rules (no response, salon cancel, duplicate payment) | B | Covered by tests |
| C5.6 | Admin dispute console (queue, case view, decision, appeal) | W/B | Decision executes the refund |
| C5.7 | Chargeback handling flow + evidence pack | B/W/P | Deadline reminders |
| C5.8 | Policy configuration screen (refund tiers, windows) | W/B | Changes apply to new bookings only |
| C5.9 | Customer/salon reliability score + restrictions | B | Visible in admin |
| C5.10 | T&Cs, refund policy, privacy policy (DPDP Act consent), dispute policy documents | P | Reviewed, shown in-app |
| C5.11 | In-app help center, FAQs, "Contact support" (P1: in-app chat) | A-C/P | Opens a ticket |

## EC6: Reviews and Trust
| ID | Task | Owner | AC |
|---|---|---|---|
| C6.1 | Review after completed visit (rating + text + optional photo) | A-C/B | Only verified bookings |
| C6.2 | Salon reply, report review | A-S/B | Moderation queue in admin |
| C6.3 | Rating aggregation, listing ranking (rating, distance, reliability) | B | Simple weighted score |

## EC7: Notifications
| ID | Task | Owner |
|---|---|---|
| C7.1 | FCM push to customers (confirmation, reminder, reschedule, refund, dispute updates) | B/A-C |
| C7.2 | WhatsApp templates for customers: confirmation, reminder, refund processed, dispute update | P/B |
| C7.3 | Notification preferences | A-C |

## EC8: Admin Additions
| ID | Task | Owner |
|---|---|---|
| C8.1 | GMV, commission, refunds, dispute rate, take-rate dashboard | W/B |
| C8.2 | Payments explorer + manual refund (with reason, audit) | W/B |
| C8.3 | Customer management (view, block, reliability score) | W/B |
| C8.4 | Salon approval for marketplace listing (KYC verified + photos + policy complete) | W/B |
| C8.5 | Payout monitoring and holds | W/B |

---

# Revised Sprint Plan (Aug 15 to Oct 31)

This is bigger than the original scope, so **sequence matters**. Salon core must work first, because it is what you sell.

| Sprint | Dates | Scope | Demo |
|---|---|---|---|
| S0 | Aug 15-21 | Setup, Figma for both apps, **Razorpay Route + WhatsApp applications**, mentor check on revenue model | Repos, wireframes approved |
| S1 | Aug 22-Sep 4 | Salon onboarding, services, staff, customers (E2, E3, E4) | Salon fully set up |
| S2 | Sep 5-18 | Appointment engine + salon calendar (E5), customer auth + discovery (EC1, EC2) | Customer finds a salon |
| S3 | Sep 19-Oct 2 | Customer booking (EC3), WhatsApp/push, billing (E8) | **End-to-end booking without payment** → first design partner live |
| S4 | Oct 3-16 | Payments + refunds + ledger (EC4), subscriptions (E12) | Pay, hold, release, refund in test mode |
| S5 | Oct 17-31 | Disputes (EC5 core), reviews, admin console, QA | **MVP submission** + pricing conversation |

Tip: if payments or KYC slip, release the customer app with **pay-at-salon** only and ship Razorpay in the next sprint. Do not delay MVP submission for it.

---

# End-to-End Scenarios (acceptance tests)

**1. Happy path:** Customer finds the salon → books Haircut ₹500 → pays ₹500 via UPI → the booking is confirmed → salon completes → ₹450 is released to the salon, ₹50 is platform commission → review posted.

**2. Customer cancels in time:** Cancels 5h before → sees "₹500 refund" → confirms → refund initiated → push when processed → transfer reversed, ledger updated.

**3. Salon no-show:** Customer waits, raises a dispute (D1) → payout frozen → the salon doesn't reply in 24h → auto refund 100% + salon penalty → customer notified.

**4. Quality dispute:** Customer reports a bad service with photos → salon disagrees → admin reviews → 50% refund → salon gets ₹225 minus commission → both notified, appeal window opens.

**5. Money deducted, no booking:** Payment captured, the slot lock expired, and another customer took the slot → the reconciliation or webhook handler auto-refunds → customer is told "Refund initiated".

**6. Chargeback:** Webhook → chargeback record → payout frozen → admin submits evidence → outcome applied to the ledger.

---

# Risks Specific to the Marketplace

| Risk | Mitigation |
|---|---|
| Razorpay Route approval or KYC delays | Apply in week 1; fall back to pay-at-salon + manual payouts |
| Legal/tax (GST, TCS, T&Cs, DPDP) | Consult a CA and a lawyer before taking real money |
| Cold start | One city, design-partner salons, salon-driven customer onboarding via QR |
| Disputes overwhelm a 3 to 4 person team | Automate common cases, clear policy, strict SLAs, evidence in-app |
| Scope too large for 11 weeks | Follow the cut list below |
| Fraud (fake bookings, refund abuse, collusion) | OTP, reliability scores, limits, reconciliation |
| Play Store review | Physical services paid via Razorpay are generally outside Play Billing, but verify the current policy and provide a privacy policy, account deletion, and a data safety form |

---

# Cut List (cut from the bottom up)

1. Wallet, coupons, referrals, in-app chat, favourites
2. Map view, ranking algorithm (use a simple distance sort)
3. Appeal flow, reliability scores, review photos
4. Razorpay customer payments → fall back to pay-at-salon with an advance
5. Automated chargeback tooling (handle manually in the Razorpay dashboard)

**Never cut:** salon app core, appointment engine, customer booking, refunds on cancellation, the basic dispute flow with admin decision, ledger and reconciliation, subscription billing for salons, and the design-partner tracking in admin.

---

# Definition of Done (money and dispute features)

- [ ] Tested in Razorpay test mode for success, failure, timeout, duplicate and refund
- [ ] Webhook replay test passes (no double effects)
- [ ] Ledger balances after every scenario above
- [ ] Policy and refund amounts displayed before the user confirms
- [ ] Audit log entry for every admin or system money action
- [ ] Reviewed by a second developer and signed off on by the business lead