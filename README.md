# SmarC — Smart Credit. Clear Control. (Stage 1 MVP, Android native)
Kotlin + Jetpack Compose + Room. Open this folder in Android Studio (Koala+, JDK 17); it generates the Gradle wrapper on sync. Run `app`, or `./gradlew test`.

## Financial rules (documented, enforced in Ledger.kt)
- Money = Long centavos. Balances are derived from the immutable ledger, never stored.
- Payment allocation: fees/interest first, then principal. Overpayment rejected.
- Income is recognized when fee/interest is paid. Principal repayment = capital recovered, never profit.
- Reversal = negated entry (unique index blocks double reversal). No deletes.
- Write-off: principal written off is recorded as loss. Duplicate taps blocked by unique idempotency key.
- Overdue (MVP) uses each customer's earliest credit due date; per-installment aging comes with Phase 3.

## Added in v0.2
Capital in/out, expenses (void-only, no deletes), cash on hand, net realized profit, salted-PBKDF2 PIN lock (auto-relock on background, FLAG_SECURE), DB migration 1->2.
Cash model: lending (credit principal leaves cash). Withdrawals are not blocked when they exceed cash on hand.

## Added in v0.3
Per-credit aging (payments applied oldest-due first), 7 aging buckets, accurate overdue totals, priority collection list, follow-up log with promise-to-pay (missed promises flagged), user-initiated SMS reminder, DB migration 2->3.

## Added in v0.4
Reports & exports: period P&L (all time/month/30 days/year), ledger CSV, aging CSV, P&L CSV (formula-injection safe, Excel-friendly UTF-8 BOM), customer statement PDF, receipt PDF, business name on documents, FileProvider sharing (exports live in cache and are cleared on next export).
P&L is activity-based: each entry counts on the date it was posted.

## Added in v0.5
Customer profile with balance breakdown (identity: balance = issued + fees - repaid - written off), open credits with due status, credit utilization; edit customer (email, notes, account no., status), archive (blocked while a balance exists), write-off with required reason, duplicate warning (name/phone), customer list filters (overdue/owing/paid) and sorting; DB migration 3->4.

## Added in v0.6
Dashboard: period filter (month/30 days/year/all) with fair previous-period comparison, collection rate (collected / amount falling due in period), available lending capital (= cash on hand, floored at 0), active customers, charts drawn with Compose Canvas (issued vs collected, receivables over time, receivables by due status, top customers).

## Added in v0.7
Append-only audit log (customer add/edit, credit, payment, reversal, write-off, capital/expense/void, PIN change, every CSV/PDF export) written inside the same DB transaction as the financial change; audit screen + CSV export; DB migration 4->5. Actor is always 'Owner' until staff roles exist.

## Added in v0.8
Installment plans: a credit can be split into 2-60 installments (weekly / every 15 days / monthly). Stored as N ordinary CREDIT entries sharing a planId, so aging, overdue, collection rate and dashboards need no special cases. Centavo remainders go to the earliest installments; all-or-nothing posting; whole-plan reversal only while nothing is paid; per-installment status in the profile; installment schedule CSV; DB migration 5->6.

## Added in v0.9
Notification center (bell with unread badge): due today/tomorrow, missed payment, missed promise-to-pay, near/at credit limit - derived from ledger data, read state remembered per alert; configurable due-soon window, alert lead days and limit-warning %; collection calendar (month grid, per-day amounts due, overdue marked). No system/lock-screen notifications yet.

## Added in v0.10
More entry types: opening balance (owed before SmarC; does not reduce cash), fee/interest assessment, authorized adjustment (increase/decrease, reason required, cannot drop below what is owed; corrects recording errors - use Write off to forgive debt). Payment methods (cash/bank/e-wallet/other) + reference, sequential receipt numbers (OR-000001, assigned inside the DB transaction). Cash on hand now uses 'lent' = credits + adjustments (net of reversals). DB migration 6->7.

## Added in v1.0 - accounts & security
Local accounts (this app has no server): setup wizard (business, currency, financial-year start, date format, owner account), username + password (PBKDF2-SHA256, salted), one-time recovery code for forgotten passwords, lockout after 5 failed tries (persisted), biometric convenience unlock, inactivity logout (1/5/15/30 min after leaving the app), forced password change for staff, roles Owner / Administrator / Collector enforced inside the services, staff management, per-user audit trail. Multiple businesses = separate databases. Databases are encrypted with SQLCipher; the key is wrapped by the Android Keystore. Encrypted backups (PBKDF2 + AES-GCM) with restore; archived-customer personal-data erasure. IMPORTANT: databases from earlier builds were unencrypted and are NOT migrated - uninstall/reinstall for a clean start.

## Added in v1.1
Insights screen (More > Insights): cash reconciliation (service recomputes expected cash; difference needs a note; books are never auto-changed), lending reserve %, estimated future profit (assumptions shown, never mixed with realized profit), profit by customer, payment-behaviour indicator 0-100 with visible formula (internal indicator, never approves/declines credit). XLSX export (own writer, no library) next to CSV. Cash refunds (re-open debt, reverse fee income when needed), receipt reprint (tap a payment), unique invoice numbers on credit. Configurable overdue ranges, privacy-safe daily reminder notification (count only), TOTP two-factor login (RFC 6238, offline), per-business encryption keys, business deletion, automatic retention erasure, timezone, logo, customer photos, dark mode, splash, adaptive icon, onboarding, width-capped tablet layout. Instrumented tests (androidTest) for concurrency, duplicate prevention, roles and isolation.

## Added in v1.2 (standalone, single owner)
Backup reminder (every N days, default 7; counts from first record so new users are not nagged), 'Back up now' banner on Home, last-backup date, one-tap route to the backup page. Single-owner mode: staff users, roles and extra businesses are hidden unless switched on (Security > Advanced); login shows only a password when exactly one account exists.

## Added in v1.3
Decisions made: TAX = exclusive %, added on top of the price, part of what the customer owes, pass-through (never income/profit, never counted as cash lent), shown as its own line in the P&L export; default % in business settings, changeable per sale; installments split tax too. ITEMS = optional lines (name, qty to 3 decimals, price) that add up to the price, with one-tap re-entry of past items. PHOTOS = up to 3 pictures of the paper receipt per entry (camera or gallery), downscaled, in app-private storage, included in the encrypted backup, deleted when the customer is erased. TABLETS (>= 840dp) = side navigation rail + customer list/detail panes. DB migration 9->10.

## Not yet built
See chat summary for the full remaining list.


## First build on Windows
The Gradle wrapper is not bundled. Run once: `powershell -ExecutionPolicy Bypass -File .\setup-wrapper.ps1` (needs internet, JDK 17 or Android Studio, Android SDK). Then `.\gradlew.bat assembleDebug`. Or just open the folder in Android Studio.
