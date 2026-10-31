package ph.smarc

import androidx.room.withTransaction
import java.math.RoundingMode

fun Long.orNo() = "OR-%06d".format(this)
fun Long.peso() = Cur.symbol + "%,.2f".format(this / 100.0)
fun parsePeso(s: String): Long? = s.trim().toBigDecimalOrNull()?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.toLong()?.takeIf { it >= 0 }

object Calc {
    /** Splits [total] into n parts that sum exactly to total; leftover centavos go to the earliest parts. */
    fun split(total: Long, n: Int): List<Long> { val b = total / n; val r = (total % n).toInt(); return List(n) { b + if (it < r) 1 else 0 } }
    /** Rule: payments settle fees/interest first, then principal. Returns (feePart, principalPart). */
    fun allocate(amount: Long, feeOwed: Long, principalOwed: Long): Pair<Long, Long> {
        require(amount in 1..(feeOwed + principalOwed)) { "Invalid payment amount" }
        val fee = minOf(amount, feeOwed)
        return fee to (amount - fee)
    }
}

class LedgerService(private val db: Db) {
    private val dao = db.dao()
    var actor: String = "?"; var role: Role? = null   // set from the signed-in session
    private suspend fun audit(a: String, d: String) = dao.insertAudit(AuditLog(action = a, detail = d, actor = actor))
    suspend fun addCustomer(c: Customer) = db.withTransaction { if (Perm.can(role, Action.ADD_CUSTOMER)) { dao.addCustomer(c); audit("CUSTOMER_ADD", c.name) } }

    suspend fun credit(c: Customer, principal: Long, fee: Long, due: Long?, note: String, key: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.CREDIT)?.let { return@withTransaction it }
        if (principal <= 0 || fee < 0) return@withTransaction Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        if (c.status != "ACTIVE") return@withTransaction Result.failure(IllegalStateException("Customer is ${c.status.lowercase()}"))
        val bal = dao.principalOf(c.id) + dao.feeOf(c.id)
        if (c.creditLimit > 0 && bal + principal + fee > c.creditLimit)
            return@withTransaction Result.failure(IllegalStateException("Exceeds credit limit of ${c.creditLimit.peso()}"))
        val n = dao.insert(Entry(idemKey = key, customerId = c.id, type = "CREDIT", principalDelta = principal, feeDelta = fee, dueDate = due, note = note))
        if (n == -1L) Result.failure(IllegalStateException("Duplicate submission ignored")) else { audit("CREDIT", "${c.name} ${(principal + fee).peso()}"); Result.success("Credit of ${(principal + fee).peso()} recorded") }
    }

    /** Returns receipt text. Receipt numbers are sequential and assigned inside the transaction, so they cannot repeat. */
    suspend fun payment(c: Customer, amount: Long, method: String, ref: String, note: String, key: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.PAYMENT)?.let { return@withTransaction it }
        val p = dao.principalOf(c.id); val f = dao.feeOf(c.id)
        if (amount <= 0 || amount > p + f) return@withTransaction Result.failure(IllegalArgumentException("Payment must be between ${Cur.symbol}0.01 and ${(p + f).peso()}"))
        val (fee, prin) = Calc.allocate(amount, f, p)
        val no = dao.lastReceipt() + 1
        val n = dao.insert(Entry(idemKey = key, customerId = c.id, type = "PAYMENT", principalDelta = -prin, feeDelta = -fee, cashDelta = amount, incomeDelta = fee,
            note = "$method $note".trim(), receiptNo = no, reference = ref))
        if (n == -1L) return@withTransaction Result.failure(IllegalStateException("Duplicate submission ignored"))
        audit("PAYMENT", "${no.orNo()} ${c.name} ${amount.peso()}")
        Result.success("SmarC RECEIPT\nReceipt No: ${no.orNo()}\nDate: ${System.currentTimeMillis().dmy()}\nCustomer: ${c.name}\nMethod: $method${if (ref.isNotBlank()) " (ref $ref)" else ""}\nReceived: ${amount.peso()}\nApplied to fees/interest: ${fee.peso()}\nApplied to principal: ${prin.peso()}\nRemaining balance: ${(p + f - amount).peso()}\nStatus: PAID")
    }

    /** Posted entries are never deleted; reversal inserts the exact negation (unique index blocks double reversal). */
    suspend fun reverse(entryId: String, reason: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.REVERSE)?.let { return@withTransaction it }
        val e = dao.entry(entryId) ?: return@withTransaction Result.failure(IllegalStateException("Entry not found"))
        if (reason.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("A reason is required"))
        if (e.type == "REVERSAL") return@withTransaction Result.failure(IllegalStateException("Cannot reverse a reversal"))
        val n = dao.insert(Entry(idemKey = "rev-$entryId", customerId = e.customerId, type = "REVERSAL", principalDelta = -e.principalDelta,
            feeDelta = -e.feeDelta, cashDelta = -e.cashDelta, incomeDelta = -e.incomeDelta, lossDelta = -e.lossDelta, tax = -e.tax, note = reason, reversalOf = entryId))
        if (n == -1L) Result.failure(IllegalStateException("Already reversed")) else { audit("REVERSAL", "entry $entryId: $reason"); Result.success("Entry reversed") }
    }

    /** Credit with N installments = N CREDIT entries (own due date each) sharing a planId. All-or-nothing in one transaction. */
    suspend fun creditPlan(c: Customer, principal: Long, fee: Long, firstDue: Long?, n: Int, freq: Freq, note: String, key: String, ref: String = "", taxPct: Int = 0, items: String = ""): Result<String> = db.withTransaction {
        Perm.check(role, Action.CREDIT)?.let { return@withTransaction it }
        if (principal <= 0 || fee < 0 || n !in 1..60) return@withTransaction Result.failure(IllegalArgumentException("Invalid amount or installment count"))
        if (principal < n) return@withTransaction Result.failure(IllegalArgumentException("Amount is too small to split into $n installments"))
        if (n > 1 && firstDue == null) return@withTransaction Result.failure(IllegalArgumentException("First due date is required for installments"))
        if (c.status != "ACTIVE") return@withTransaction Result.failure(IllegalStateException("Customer is ${c.status.lowercase()}"))
        val tax = Tax.of(principal, taxPct); val gross = principal + tax   // tax is added on top of the price
        val bal = dao.principalOf(c.id) + dao.feeOf(c.id)
        if (c.creditLimit > 0 && bal + gross + fee > c.creditLimit) return@withTransaction Result.failure(IllegalStateException("Exceeds credit limit of ${c.creditLimit.peso()}"))
        if (ref.isNotBlank() && dao.refCount(ref.trim()) > 0) return@withTransaction Result.failure(IllegalStateException("Invoice number ${ref.trim()} is already used"))
        val pid = if (n > 1) java.util.UUID.randomUUID().toString() else null
        val ps = Calc.split(gross, n); val fs = Calc.split(fee, n); val ts = Calc.split(tax, n)
        val dues: List<Long?> = if (firstDue == null) List(n) { null } else Schedule.dates(firstDue, n, freq)
        for (i in 0 until n) {
            val r = dao.insert(Entry(idemKey = "$key-$i", customerId = c.id, type = "CREDIT", principalDelta = ps[i], feeDelta = fs[i], dueDate = dues[i],
                note = if (n > 1) "Installment ${i + 1}/$n $note".trim() else note, planId = pid, reference = ref.trim(), tax = ts[i], items = if (i == 0) items else ""))
            if (r == -1L) return@withTransaction Result.failure(IllegalStateException("Duplicate submission ignored"))
        }
        audit("CREDIT", "${c.name} ${(gross + fee).peso()}" + if (n > 1) " in $n installments" else "")
        Result.success("Credit of ${(gross + fee).peso()} recorded${if (tax > 0) " (incl. tax ${tax.peso()})" else ""}" + if (n > 1) " in $n installments" else "")
    }

    /** Only allowed while no installment of the plan has been paid (payments settle oldest-due first). */
    suspend fun reversePlan(planId: String, reason: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.REVERSE)?.let { return@withTransaction it }
        if (reason.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("A reason is required"))
        val plan = dao.planEntries(planId); if (plan.isEmpty()) return@withTransaction Result.failure(IllegalStateException("Plan not found"))
        val all = dao.entriesOf(plan[0].customerId); val open = Aging.open(all).associateBy { it.id }
        val reversed = all.mapNotNull { it.reversalOf }.toSet(); val live = plan.filter { it.id !in reversed }
        if (live.isEmpty()) return@withTransaction Result.failure(IllegalStateException("Plan already reversed"))
        if (live.any { (open[it.id]?.amount ?: 0L) != it.principalDelta + it.feeDelta })
            return@withTransaction Result.failure(IllegalStateException("Some installments are already paid. Reverse those payments first"))
        live.forEach { e -> dao.insert(Entry(idemKey = "rev-${e.id}", customerId = e.customerId, type = "REVERSAL", principalDelta = -e.principalDelta, feeDelta = -e.feeDelta, tax = -e.tax, note = reason, reversalOf = e.id)) }
        audit("REVERSAL", "plan ${planId.take(8)}: $reason"); Result.success("Installment plan reversed")
    }

    /** Opening balance, fee/interest assessment, and authorized adjustments. A reason is mandatory; everything lands in the ledger and audit log. */
    suspend fun otherEntry(c: Customer, kind: String, principal: Long, fee: Long, due: Long?, increase: Boolean, reason: String, key: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.ADJUST)?.let { return@withTransaction it }
        if (reason.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("A reason is required"))
        if (principal < 0 || fee < 0 || principal + fee <= 0) return@withTransaction Result.failure(IllegalArgumentException("Enter an amount greater than zero"))
        if (kind == "fee" && principal != 0L) return@withTransaction Result.failure(IllegalArgumentException("A fee assessment has no principal"))
        if (c.status == "ARCHIVED") return@withTransaction Result.failure(IllegalStateException("Customer is archived"))
        val type = when (kind) { "opening" -> "OPENING"; "fee" -> "FEE"; else -> "ADJUSTMENT" }
        var sign = 1L
        if (kind == "adjust") {
            val p = dao.principalOf(c.id); val f = dao.feeOf(c.id)
            if (increase) { if (c.creditLimit > 0 && p + f + principal + fee > c.creditLimit) return@withTransaction Result.failure(IllegalStateException("Exceeds credit limit of ${c.creditLimit.peso()}")) }
            else { if (principal > p || fee > f) return@withTransaction Result.failure(IllegalStateException("Cannot reduce more than the outstanding principal (${p.peso()}) or fees (${f.peso()})")); sign = -1L }
        }
        val n = dao.insert(Entry(idemKey = key, customerId = c.id, type = type, principalDelta = sign * principal, feeDelta = sign * fee, dueDate = if (kind == "adjust") null else due, note = reason))
        if (n == -1L) return@withTransaction Result.failure(IllegalStateException("Duplicate submission ignored"))
        val amt = (if (sign < 0) "-" else "") + (principal + fee).peso()
        audit(type, "${c.name} $amt: $reason"); Result.success("$type $amt recorded")
    }

    /** Cash handed back to a customer. Re-opens the matching debt (they paid, then got money back) and reverses fee income if the refund goes beyond principal recovered. */
    suspend fun refund(c: Customer, amount: Long, reason: String, key: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.REFUND)?.let { return@withTransaction it }
        if (reason.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("A reason is required"))
        if (amount <= 0) return@withTransaction Result.failure(IllegalArgumentException("Enter an amount greater than zero"))
        val all = dao.entriesOf(c.id); val paid = all.sumOf { it.cashDelta }; val recovered = paid - all.sumOf { it.incomeDelta }
        if (amount > paid) return@withTransaction Result.failure(IllegalStateException("Cannot refund more than the customer has paid (${paid.peso()})"))
        val prin = minOf(amount, maxOf(0L, recovered)); val fee = amount - prin
        val n = dao.insert(Entry(idemKey = key, customerId = c.id, type = "REFUND", principalDelta = prin, feeDelta = fee, cashDelta = -amount, incomeDelta = -fee, note = reason))
        if (n == -1L) return@withTransaction Result.failure(IllegalStateException("Duplicate submission ignored"))
        audit("REFUND", "${c.name} ${amount.peso()}: $reason")
        Result.success("SmarC REFUND VOUCHER\nDate: ${System.currentTimeMillis().dmy()}\nCustomer: ${c.name}\nCash returned: ${amount.peso()}\nReason: $reason\nBalance now owed: ${(all.sumOf { it.principalDelta + it.feeDelta } + amount).peso()}")
    }

    suspend fun updateCustomer(c: Customer): Result<String> = db.withTransaction {
        Perm.check(role, Action.EDIT_CUSTOMER)?.let { return@withTransaction it }
        if (c.name.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("Name is required"))
        val bal = dao.principalOf(c.id) + dao.feeOf(c.id)
        if (c.status == "ARCHIVED" && bal != 0L) return@withTransaction Result.failure(IllegalStateException("Settle or write off the ${bal.peso()} balance before archiving"))
        dao.updateCustomer(c); audit("CUSTOMER_EDIT", "${c.name} (${c.status})"); Result.success("Customer saved")
    }

    suspend fun writeOff(c: Customer, reason: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.WRITE_OFF)?.let { return@withTransaction it }
        val p = dao.principalOf(c.id); val f = dao.feeOf(c.id)
        if (p + f <= 0 || reason.isBlank()) return@withTransaction Result.failure(IllegalStateException("Nothing to write off, or reason missing"))
        dao.insert(Entry(idemKey = "wo-${c.id}-${System.currentTimeMillis()}", customerId = c.id, type = "WRITE_OFF", principalDelta = -p, feeDelta = -f, lossDelta = p, note = reason))
        audit("WRITE_OFF", "${c.name} ${(p + f).peso()}: $reason")
        Result.success("Written off ${(p + f).peso()}")
    }
}
