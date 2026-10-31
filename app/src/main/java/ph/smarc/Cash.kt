package ph.smarc

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Documented accounting identities (centavos). Cash and profit are kept separate. */
object Fin {
    /** Cash you may lend: cash on hand minus the reserve (a % of net capital) you chose to keep back. */
    fun available(cash: Long, capitalNet: Long, reservePct: Int) = maxOf(0L, cash - maxOf(0L, capitalNet) * reservePct.coerceIn(0, 100) / 100)
    fun netProfit(income: Long, loss: Long, expenses: Long) = income - loss - expenses
    /** Principal ever lent (net of corrections) = principal recovered + still outstanding + written off. */
    fun issued(recovered: Long, principalOutstanding: Long, loss: Long) = recovered + principalOutstanding + loss
    /** Lending model: cash = net capital + collections - principal lent - expenses. */
    fun cashOnHand(capitalNet: Long, collected: Long, issued: Long, expenses: Long) = capitalNet + collected - issued - expenses
}

class CashService(private val db: Db) {
    private val dao = db.dao()
    var actor: String = "?"; var role: Role? = null
    suspend fun add(kind: String, amount: Long, category: String, note: String, key: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.CASH_MOVES)?.let { return@withTransaction it }
        if (amount <= 0) return@withTransaction Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        if (kind == "EXPENSE" && category.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("Category is required"))
        val (cash, exp) = when (kind) { "CAPITAL_IN" -> amount to 0L; "CAPITAL_OUT" -> -amount to 0L; else -> -amount to amount }
        val n = dao.insertCash(CashEntry(idemKey = key, kind = kind, cashDelta = cash, expenseDelta = exp, category = category.trim(), note = note))
        if (n == -1L) Result.failure(IllegalStateException("Duplicate submission ignored")) else { dao.insertAudit(AuditLog(actor = actor, action = kind, detail = "${amount.peso()} $category".trim())); Result.success("Saved") }
    }
    /** The service recomputes the expected cash itself, so the screen cannot be used to fake a match. Records only; never edits the books. */
    suspend fun reconcile(counted: Long, note: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.CASH_MOVES)?.let { return@withTransaction it }
        if (counted < 0) return@withTransaction Result.failure(IllegalArgumentException("Counted cash cannot be negative"))
        val expected = Fin.cashOnHand(dao.capitalSum(), dao.cashSum(), dao.lentSum(), dao.expenseSum())
        if (counted != expected && note.isBlank()) return@withTransaction Result.failure(IllegalArgumentException("Explain the difference in the note"))
        dao.insertRecon(Reconciliation(expected = expected, counted = counted, note = note.trim(), actor = actor))
        dao.insertAudit(AuditLog(actor = actor, action = "RECONCILE", detail = "expected ${expected.peso()}, counted ${counted.peso()}"))
        Result.success(if (counted == expected) "Cash matches the books" else "Difference of ${(counted - expected).peso()} recorded")
    }

    suspend fun void(id: String, reason: String): Result<String> = db.withTransaction {
        Perm.check(role, Action.CASH_MOVES)?.let { return@withTransaction it }
        val e = dao.cashEntry(id) ?: return@withTransaction Result.failure(IllegalStateException("Entry not found"))
        if (reason.isBlank() || e.reversalOf != null) return@withTransaction Result.failure(IllegalStateException("Cannot void this entry"))
        val n = dao.insertCash(CashEntry(idemKey = "void-$id", kind = e.kind, cashDelta = -e.cashDelta, expenseDelta = -e.expenseDelta,
            category = e.category, note = "VOID: $reason", reversalOf = id))
        if (n == -1L) Result.failure(IllegalStateException("Already voided")) else { dao.insertAudit(AuditLog(actor = actor, action = "VOID_${e.kind}", detail = reason)); Result.success("Entry voided") }
    }
}

