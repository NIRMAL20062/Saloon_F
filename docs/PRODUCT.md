# Glide MVP: the product

The one description of what we are building for the MVP. Every rule here was decided by the team; the decision IDs (D-…, DF-…)
point to [DECISIONS.md](DECISIONS.md), which wins if the two ever disagree. **❓ = still open: ask the team before building it.**
Replaces the old planning files `ChatGPT.md` and `Salon_App_Task_Wise_Development.md` (D-038).

---

## 1. In short

Glide is a salon marketplace for India.
- **Customers** find salons near them, book a service with a staff member and a time, pay online, and manage their bookings.
- **Salons** run their day in the same app: calendar, services, staff, their customer list, bills. They pay Glide a monthly
  subscription, and Glide takes a small fee on online payments.
- **Our team** runs an internal admin website: verify salons, handle disputes and refunds, watch payments.

Three pieces: **one Android app** (customer side + salon side), a **Ktor backend** with PostgreSQL, and the **admin website**
(Next.js). Name "Glide" is a working name (Q-006). English only (D-018).

## 2. Rules that shape everything

| Rule | Decision |
|---|---|
| **One app.** At first login the person answers "customer or salon?"; the app shows only that side | D-023, D-024 |
| **That choice is final.** No "switch side" anywhere; a wrong pick is fixed by our team | D-030, DF-22 |
| **One salon per person** (as Owner or Staff). No "switch salon" | D-035 |
| **Two salon roles: Owner and Staff** | D-039 |
| **Salons are verified by our team** (profile + bank details) before they go live | D-033 |
| **Online payments only**, all through **our** Razorpay account; no cash | D-028, D-029 |
| Login is **phone number + SMS code** (Supabase); admins log in by email + authenticator app | D-016, DF-16 |
| **Modern, smooth, interactive screens** from one design system, looking like the team's mockup ([design/customer-flow-1.webp](design/customer-flow-1.webp)); **light mode only** | D-031, D-040, D-041 |
| No internet on the salon side = **view only** | D-019 |
| WhatsApp and Razorpay only through the backend; secrets only in environment variables; money in paise | product rules, DF-11 |
| Every salon's data is separate; a customer sees only their own data | D-025, D-027, D-036 |

## 3. Who uses it

| Who | Where | What they can do |
|---|---|---|
| **Customer** | app, customer side | find salons, book, pay, cancel/reschedule, review, report a problem |
| **Owner** | app, salon side | full control of the salon (D-039) |
| **Staff** | app, salon side | **only their own appointments**: see, accept, mark done or no-show (D-039) |
| **Admin** (our team) | admin website | verify salons, disputes, refunds, payments, support (D-002, D-013) |

No Manager or receptionist role in the MVP (D-039); add one later only if real salons ask for it.

### Salon permissions

```
                 SALON
                   │
             ┌─────┴─────┐
           OWNER        STAFF
             │            └── own appointments only
             ├── profile, hours, policies
             ├── staff (add / remove, working hours)
             ├── services and prices
             ├── all appointments, customers
             ├── bills, reports, revenue, cancellations and refunds
             ├── bank details
             └── Glide subscription
```

| Action | Owner | Staff |
|---|---|---|
| Salon profile, hours, policies | ✅ | ❌ |
| Staff (add by phone, remove, working hours) | ✅ | ❌ |
| Services and prices | ✅ | ❌ (sees the services of their own appointments) |
| Appointments | all | **their own**: see, accept, mark done, mark no-show |
| Customers | full list | only the basic details their appointment needs (name, phone) |
| Bills, reports, revenue, payouts, cancellations and refunds | ✅ | ❌ |
| Disputes and review replies | ✅ | ❌ |
| Bank details | ✅ | ❌ |
| Subscription (paying Glide) | ✅ | ❌ |

The backend enforces every cell: hiding a button is never the only guard.

## 4. Everyone: first time in the app

0. Welcome screen: a full-screen salon photo, "Look Good, Feel Amazing", **Get Started** (D-041).
1. Enter the 10-digit mobile number (+91) → a 6-digit code arrives by SMS → enter it (it verifies on the 6th digit). Stays
   logged in; log out from the menu. _(Built: APP-004.)_
2. A phone number that a salon already added as staff skips the next step and opens the salon side (DF-23).
3. Everyone else: **"How will you use Glide?"** → "I want to book salons" (customer) or "I run a salon" (owner). Final (D-030).

## 5. Customer side

**First time:** name (required) and email (optional) → allow location, or pick the city from a list (launch city ❓, APP-009).

**Tabs:** Home · Search · Bookings · Profile.

| Area | What it does |
|---|---|
| **Home** | nearby salons, categories (haircut, facial, bridal…), top rated |
| **Search** | by salon name or service; filters: service, price, rating, open now, men / women / unisex |
| **Salon page** | photos, about, address + map, hours, services and prices, staff, ratings and reviews, policies |
| **Booking** | pick services → a staff member or "any" → date → a free time → summary (price, taxes, fee, cancellation policy) → pay |
| **Payment** | Razorpay (UPI, cards, netbanking, wallets): pay in full, pay an advance, or "pay at the salon" (an advance now, the rest at the salon by link / UPI QR; never cash, D-029). The time is held for 10 minutes while paying |
| **After booking** | confirmation by push + WhatsApp; reminder 2 h before; the salon marks the visit done; the customer rates it |
| **My bookings** | upcoming / past with status; reschedule (within policy); cancel (the refund amount is shown **before** confirming, refund is automatic); directions; call the salon; receipts |
| **Report a problem** | within 48 h of the visit: type, text, up to 3 photos (§9) |
| **Profile** | name, email, notification settings, log out |

**Starting cancellation and refund policy** (shown before paying; admins can change it):

| Situation | Refund |
|---|---|
| Customer cancels more than 4 h before | 100% |
| Customer cancels 2–4 h before | 50% |
| Customer cancels less than 2 h before, or doesn't show up | none; the salon gets a no-show fee minus our fee |
| Salon cancels or doesn't honour the booking | 100% (+ a penalty on the salon's score) |
| Payment taken but booking failed, or paid twice | automatic refund |
| Service quality complaint | admin decides: re-do, partial or full refund, or reject |

Reschedule: allowed within the policy (e.g. up to 2 h before, at most twice), no extra charge.

## 6. Salon side

### 6.1 Onboarding and verification (D-033)
1. Owner: "Create your salon": name, phone, address, type (men / women / unisex), ❓ GST number.
2. Bank details: account holder name, account number (typed twice), IFSC. Stored encrypted, shown masked (DF-24).
3. "Submit for verification" → **under verification** (details can still be edited) → an admin approves → **live**, or rejects
   with a reason → fix → submit again.
4. Before going live in the customer app the salon also adds photos, a map pin and its policies, and passes Razorpay's KYC for
   its linked account (D-028).

Only a **live** salon can add staff, appear for customers and take bookings.

### 6.2 Staff (D-035, D-039)
Owner → Staff → add a phone number → that person logs in with the number and lands on the salon side, seeing only their own
appointments. Remove someone at any time (history stays). A number already used as a customer or in another salon can't be
added. Staff names: ❓ Q-012.

### 6.3 Setting up the salon
- Working hours per weekday, closed days (❓ breaks, holidays).
- Services: name, category, **duration**, **price**; unique name per salon (❓ price per staff level, variants, Q-S3 below).
- Which services each staff member does; staff working hours, days off.
- The salon's own customer list: name, phone, notes; quick add for walk-ins; visit history and spend.
- Deactivate a service or staff member without losing history.

### 6.4 Appointments (the heart of the product)
- Free times come from salon hours + staff hours + days off + service durations + existing bookings (❓ buffer between
  appointments, slot step).
- **Double booking is impossible**, enforced by the database.
- Bookings come from the customer app, walk-ins and phone calls; one or more services, a staff member or "any".
- Status: `BOOKED → CONFIRMED → COMPLETED`, or `CANCELLED`, `NO_SHOW` (❓ is `IN_PROGRESS` needed).
- **Staff accept their own bookings** (❓ is auto-accept still the default, and what happens if nobody answers).
- **Mark done** (this releases the salon's money, §8) or **no-show** (no-show fee per policy): the staff member for their own
  appointments, or the owner for any.
- Owner: calendar (day view per staff member), book walk-ins and phone bookings, reschedule or cancel with a reason (customer is
  told).
- Push alert to the salon for new, cancelled and moved bookings.
- No internet: today's appointments and customers stay visible; nothing can change until the connection is back (D-019).

### 6.5 Bills and invoices
- A bill for a completed appointment: services pre-filled, totals computed on the server (❓ products, discounts, tips, GST).
- Paid **online only**: by the customer's in-app payment, or a payment link / UPI QR at the counter (D-029). Split payments ok.
- Day summary of the day's online payments; invoice PDF, shared by WhatsApp or downloaded.

### 6.6 Money, disputes, reviews, reports
- **Payouts screen:** pending, released, refunded, our fee deducted, settlement history.
- **Disputes inbox:** answer customer complaints with text and photos within 24 h (§9).
- **Reviews:** read and reply; report abusive ones.
- **Reports:** revenue, appointments, no-shows, new vs returning customers; per staff member (❓ the 3–5 numbers that matter most).
- **Subscription:** §8.3.

Everything in §6.3–§6.6 is the owner's, except the staff member's own appointments (D-039).

## 7. Admin website (our team only)

- Login: email → code from the email → code from an authenticator app; admins invite admins (D-013, DF-16, DF-20).
- **Verify salons:** queue → profile + bank details → approve or reject with a reason (D-033).
- **Salons:** list, details, suspend / reactivate; **design-partner tracking** (logins, appointments and bills per week).
- **Disputes console:** queue (by type, age, amount, deadline), full case history, decide refunds, appeal, canned replies.
- **Payments:** look up, refund by hand (with a reason), watch payouts and holds, reconciliation report, totals dashboard
  (money taken, our fees, refunds).
- **Settings:** refund policy, subscription price (D-032), review moderation, customer lookup / block.
- **Support:** find anyone by phone, fix a wrong customer/salon pick (DF-22), resend a message.
- **Audit log:** every admin action and every money action is recorded and viewable.

## 8. Money

### 8.1 How a payment flows (D-028)
```
Customer pays online (in the app, or by link / UPI QR at the salon)
   → our Razorpay account (Razorpay Route)
   → our platform fee stays with us
   → the salon's share goes to the salon's Razorpay linked account, ON HOLD
   → visit marked done (or automatically 6 h after the slot if nobody complains) → released → salon's bank
   → if the customer reports a problem, the share stays frozen until it's settled
```
Platform fee: decided later, expected to be small because salons also pay the subscription (Q-013).

### 8.2 Rules that must never be broken
- The **server** creates every order and computes every amount; never trust an amount from the app.
- Only a **verified Razorpay webhook** changes a payment's state; handling the same webhook twice has no extra effect.
- Keep every Razorpay id (order, payment, transfer, refund) and a **double-entry ledger** so every rupee can be traced.
- Slot hold expires after 10 minutes; a payment that arrives after that confirms the booking only if the time is still free,
  otherwise it's refunded automatically.
- An **hourly reconciliation** compares Razorpay with our bookings and fixes or flags any mismatch.
- Payment states: `CREATED → PENDING → CAPTURED → HELD → RELEASED`, or `FAILED`, `REFUND_INITIATED → REFUNDED`,
  `FROZEN → RESOLVED` during a dispute.
- Before real money: a CA checks GST on our fee and TCS/TDS; the salon terms allow clawback for chargebacks and fraud.

### 8.3 Salon subscription (D-032)
One plan for now: **₹179–₹400 a month**, exact price ❓; the price is kept in our database and changed by admins on the website
(a change creates a new Razorpay plan, DF-25; salons already paying: ❓ Q-015). Paid with Razorpay Subscriptions; status only
from verified webhooks: trial, active, past due, cancelled. ❓ trial length, grace period, design-partner pricing, what happens
when unpaid (data is never deleted). More plans later.

## 9. Disputes

| Type | Raised by |
|---|---|
| Salon cancelled or didn't honour the booking; salon late | customer |
| Service quality; overcharged | customer |
| Customer no-show (salon wants the no-show fee) | salon |
| Paid but not booked; paid twice; refund not received | customer (also detected automatically) |
| "Service not done" vs "service done" | either |
| Bank chargeback | the customer's bank (through Razorpay) |
| Abuse or safety | either; goes straight to an admin, can suspend a salon at once |

**Flow:** the customer reports within 48 h → the salon's money for that booking is frozen → the salon has 24 h to reply →
agrees (automatic refund) / disagrees or no reply (small amounts: decided for the customer; otherwise an admin decides: full,
partial, reject, or re-do) → both told → each side may appeal once within 48 h.
**States:** `OPEN → AWAITING_SALON → (AWAITING_CUSTOMER) → IN_MEDIATION → RESOLVED_REFUND | RESOLVED_PARTIAL | RESOLVED_REJECTED
→ (APPEALED → FINAL) → CLOSED`. Deadlines: salon 24 h, admin first answer 12 h, solved within 3 days.
**Automatic:** paid but not booked → refund; duplicate payment → refund; salon cancels → full refund.
**Chargebacks:** a webhook opens a record and freezes the payout; an admin sends the evidence; the outcome goes to the ledger.

## 10. Notifications
- **Push** (Firebase): customers (confirmation, reminder, changes, refunds, dispute updates); salon staff (new, cancelled,
  moved bookings).
- **WhatsApp** (Meta Business API, approved templates, sent only by the backend): confirmation, reminder 2 h before,
  cancellation, refund processed, dispute update. Every message is logged with its delivery status.
- Customers choose their notification preferences.

## 11. Data (main tables)

Conventions in [DATABASE.md](DATABASE.md): uuid keys, money in paise, `salon_id` on salon-owned tables, row-level security.
```
app_users            one per person: phone, side (CUSTOMER / SALON, final), name, email
salons               name, phone, address, type, status (DRAFT → UNDER_VERIFICATION → LIVE | REJECTED | SUSPENDED)
salon_bank_details   holder name, account number (encrypted, last 4 shown), IFSC
salon_verifications  admin decisions with reasons
salon_members        salon, phone, user (set at first login), role OWNER / STAFF; one salon per phone
working_hours, time_off, services, staff_services
customers            the salon's own list (can link to app_users)
appointments, appointment_services        source: APP / WALK_IN / PHONE; no double booking
bills, bill_items, bill_payments, invoices                online payments only
salon_listing        location, photos, about, visible, rating, policies
payments, transfers, refunds, ledger_entries, webhook_events
disputes, dispute_messages, dispute_evidence, chargebacks
reviews              one per completed booking; salon reply; visible / hidden / reported
subscription_plans, subscriptions
notifications, audit_log, admins
```

## 12. Build order and where we are

| Phase | What | Status |
|---|---|---|
| 0 | Skeleton: backend, database, app, admin website, CI | ✅ done |
| 1 | Login, "customer or salon?", salon creation + verification, staff, admin login | 🔄 login built (BE-016, APP-004), rest next |
| 2 | Salon setup: hours, services, staff hours, customer list | ⬜ |
| 3 | Appointments: calendar, booking engine, staff accept, done / no-show, push | ⬜ |
| 4 | Customer: find salons, salon page, book (no payment yet), my bookings, WhatsApp + reminders | ⬜ |
| 5 | Bills and invoices | ⬜ |
| 6 | Payments: Razorpay Route, KYC, payouts, refunds, ledger, reconciliation, subscription | ⬜ |
| 7 | Disputes, reviews, reports, admin console | ⬜ |
| Pre-launch | Hosting, monitoring, backups, Play Store | ⬜ |

Task by task: [tasks/README.md](../tasks/README.md).

**If time runs short, cut from the top:** wallet / coupons / referrals / chat / favourites → map view and ranking → appeals,
reliability scores, review photos → automated chargeback tooling. **Never cut:** salon core, booking engine, customer booking,
refunds on cancellation, basic disputes with an admin decision, ledger and reconciliation, salon subscription, design-partner
tracking.

## 13. After the MVP (not now)
Favourites, coupons, wallet, one-tap rebook, chat with the salon, referrals, saved cards, loyalty points, home service,
packages / memberships, gift cards, "book for someone else", recommendations, group bookings, more subscription plans, a public
booking web link, salons inviting their regulars by QR, importing a salon's existing customers, marketing messages.

## 14. Open questions (asked per phase)
| Before | Question |
|---|---|
| Phase 2 | Staff names at first login (Q-012); GST number at sign-up; price per staff level, service variants |
| Phase 3 | Buffer and slot step; one appointment with two staff members; auto-accept and unanswered bookings; `IN_PROGRESS` |
| Phase 4 | Launch city |
| Phase 5 | Products / stock, discounts, tips, GST on bills, invoice format |
| Phase 6 | Platform fee (Q-013); exact subscription price, trial, grace period, design-partner pricing, price changes (Q-015) |
| Phase 7 | The 3–5 numbers owners care about most |

## 15. Risks
| Risk | What we do |
|---|---|
| Razorpay Route approval or a salon's KYC is slow | apply early; with online-only payments (D-029) there is no cash fallback |
| Legal and tax (GST, TCS, terms, privacy law) | a CA and a lawyer before real money |
| Few customers at the start | launch in one city with the design-partner salons |
| Disputes take too much time | clear policy, automation, strict deadlines, evidence in the app |
| Play Store review | privacy policy, account deletion, data-safety form |
