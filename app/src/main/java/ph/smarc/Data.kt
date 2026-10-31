package ph.smarc

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** All money is stored as Long centavos. Balances are NEVER stored: they are sums over the immutable ledger. */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val phone: String = "", val address: String = "",
    val creditLimit: Long = 0, val status: String = "ACTIVE", val createdAt: Long = System.currentTimeMillis(),
    val email: String = "", val notes: String = "", val accountNo: String = ""
)

/** Signed deltas. CREDIT: +principal +fee. PAYMENT: -principal -fee, +cash, +income(fee part). REVERSAL = exact negation. */
@Entity(tableName = "ledger", indices = [Index("customerId"), Index("idemKey", unique = true), Index("reversalOf", unique = true)])
data class Entry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val idemKey: String, val customerId: String, val type: String,
    val principalDelta: Long, val feeDelta: Long, val cashDelta: Long = 0, val incomeDelta: Long = 0, val lossDelta: Long = 0,
    val dueDate: Long? = null, val note: String = "", val reversalOf: String? = null, val createdAt: Long = System.currentTimeMillis(), val planId: String? = null,
    val receiptNo: Long = 0, val reference: String = "",
    val tax: Long = 0, val items: String = ""
)

data class CustomerSummary(@Embedded val c: Customer, val balance: Long, val firstDue: Long?)
data class Totals(val collected: Long, val income: Long, val loss: Long, val receivables: Long, val principal: Long, val lent: Long)

@Dao
interface LedgerDao {
    @Insert suspend fun addCustomer(c: Customer)
    @androidx.room.Update suspend fun updateCustomer(c: Customer)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(e: Entry): Long
    @Query("SELECT COALESCE(SUM(principalDelta),0) FROM ledger WHERE customerId=:id") suspend fun principalOf(id: String): Long
    @Query("SELECT COALESCE(SUM(feeDelta),0) FROM ledger WHERE customerId=:id") suspend fun feeOf(id: String): Long
    @Query("SELECT * FROM ledger WHERE id=:id") suspend fun entry(id: String): Entry?
    @Query("SELECT * FROM ledger WHERE customerId=:id ORDER BY createdAt DESC") fun entries(id: String): Flow<List<Entry>>
    @Query("""SELECT c.*, COALESCE(SUM(l.principalDelta+l.feeDelta),0) AS balance,
        MIN(CASE WHEN l.type='CREDIT' THEN l.dueDate END) AS firstDue
        FROM customers c LEFT JOIN ledger l ON l.customerId=c.id WHERE c.status!='ARCHIVED' GROUP BY c.id ORDER BY c.name""")
    fun summaries(): Flow<List<CustomerSummary>>
    @Query("""SELECT COALESCE(SUM(cashDelta),0) collected, COALESCE(SUM(incomeDelta),0) income,
        COALESCE(SUM(lossDelta),0) loss, COALESCE(SUM(principalDelta+feeDelta),0) receivables, COALESCE(SUM(principalDelta),0) principal,
        COALESCE(SUM(CASE WHEN type IN ('CREDIT','ADJUSTMENT') OR (type='REVERSAL' AND reversalOf IN (SELECT id FROM ledger WHERE type IN ('CREDIT','ADJUSTMENT'))) THEN principalDelta - tax ELSE 0 END),0) lent FROM ledger""")
    fun totals(): Flow<Totals>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCash(e: CashEntry): Long
    @Query("SELECT * FROM cash_entries WHERE id=:id") suspend fun cashEntry(id: String): CashEntry?
    @Query("SELECT * FROM cash_entries ORDER BY createdAt DESC") fun cashEntries(): Flow<List<CashEntry>>
    @Query("""SELECT COALESCE(SUM(CASE WHEN kind IN ('CAPITAL_IN','CAPITAL_OUT') THEN cashDelta ELSE 0 END),0) capital,
        COALESCE(SUM(expenseDelta),0) expenses FROM cash_entries""")
    fun cashTotals(): Flow<CashTotals>
    @Query("SELECT * FROM ledger") fun allEntries(): Flow<List<Entry>>
    @Query("SELECT * FROM customers WHERE status!='ARCHIVED'") fun customers(): Flow<List<Customer>>
    @Insert suspend fun insertFollowUp(f: FollowUp)
    @Insert suspend fun insertAudit(a: AuditLog)
    @Query("SELECT COALESCE(MAX(receiptNo),0) FROM ledger") suspend fun lastReceipt(): Long
    @Query("SELECT * FROM customers WHERE status='ARCHIVED' ORDER BY name") fun archivedCustomers(): Flow<List<Customer>>
    @Query("UPDATE follow_ups SET note='' WHERE customerId=:id") suspend fun scrubFollowUps(id: String)
    @Query("SELECT * FROM customers") suspend fun dumpCustomers(): List<Customer>
    @Query("SELECT * FROM ledger") suspend fun dumpLedger(): List<Entry>
    @Query("SELECT * FROM cash_entries") suspend fun dumpCash(): List<CashEntry>
    @Query("SELECT * FROM follow_ups") suspend fun dumpFollow(): List<FollowUp>
    @Query("SELECT * FROM audit_log") suspend fun dumpAudit(): List<AuditLog>
    @Query("DELETE FROM ledger") suspend fun clearLedger()
    @Query("DELETE FROM customers") suspend fun clearCustomers()
    @Query("DELETE FROM cash_entries") suspend fun clearCash()
    @Query("DELETE FROM follow_ups") suspend fun clearFollow()
    @Query("DELETE FROM audit_log") suspend fun clearAudit()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCustomers(l: List<Customer>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putLedger(l: List<Entry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCash(l: List<CashEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putFollow(l: List<FollowUp>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAudit(l: List<AuditLog>)
    @Insert suspend fun insertRecon(r: Reconciliation)
    @Query("SELECT * FROM reconciliations ORDER BY createdAt DESC") fun recons(): Flow<List<Reconciliation>>
    @Query("SELECT * FROM reconciliations") suspend fun dumpRecons(): List<Reconciliation>
    @Query("DELETE FROM reconciliations") suspend fun clearRecons()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecons(l: List<Reconciliation>)
    @Query("SELECT COUNT(DISTINCT COALESCE(planId, id)) FROM ledger WHERE type='CREDIT' AND reference=:r AND id NOT IN (SELECT reversalOf FROM ledger WHERE reversalOf IS NOT NULL)") suspend fun refCount(r: String): Int
    @Query("SELECT COALESCE(SUM(cashDelta),0) FROM ledger") suspend fun cashSum(): Long
    @Query("SELECT COALESCE(SUM(CASE WHEN type IN ('CREDIT','ADJUSTMENT') OR (type='REVERSAL' AND reversalOf IN (SELECT id FROM ledger WHERE type IN ('CREDIT','ADJUSTMENT'))) THEN principalDelta - tax ELSE 0 END),0) FROM ledger") suspend fun lentSum(): Long
    @Query("SELECT COALESCE(SUM(CASE WHEN kind IN ('CAPITAL_IN','CAPITAL_OUT') THEN cashDelta ELSE 0 END),0) FROM cash_entries") suspend fun capitalSum(): Long
    @Query("SELECT COALESCE(SUM(expenseDelta),0) FROM cash_entries") suspend fun expenseSum(): Long
    @Query("SELECT * FROM ledger WHERE planId=:p AND type='CREDIT'") suspend fun planEntries(p: String): List<Entry>
    @Query("SELECT * FROM ledger WHERE customerId=:id") suspend fun entriesOf(id: String): List<Entry>
    @Query("SELECT * FROM audit_log ORDER BY createdAt DESC LIMIT 500") fun auditLog(): Flow<List<AuditLog>>
    @Query("SELECT * FROM follow_ups ORDER BY createdAt DESC") fun followUps(): Flow<List<FollowUp>>
}

/** Capital moves and expenses. Signed deltas; a void is the exact negation (same kind) linked by reversalOf. */
@Entity(tableName = "cash_entries", indices = [Index("idemKey", unique = true), Index("reversalOf", unique = true)])
data class CashEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), val idemKey: String, val kind: String,
    val cashDelta: Long, val expenseDelta: Long, val category: String = "", val note: String = "",
    val reversalOf: String? = null, val createdAt: Long = System.currentTimeMillis()
)
data class CashTotals(val capital: Long, val expenses: Long)

/** Append-only collection log: contact notes and promise-to-pay dates. */
@Entity(tableName = "follow_ups", indices = [Index("customerId")])
data class FollowUp(@PrimaryKey val id: String = UUID.randomUUID().toString(), val customerId: String, val note: String,
    val promiseDate: Long? = null, val createdAt: Long = System.currentTimeMillis())

/** Append-only activity trail (no update/delete DAO methods exist for it). Actor is the signed-in role; single-owner for now. */
@Entity(tableName = "audit_log")
data class AuditLog(@PrimaryKey val id: String = UUID.randomUUID().toString(), val action: String, val detail: String,
    val actor: String = "Owner", val createdAt: Long = System.currentTimeMillis())

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cash_entries` (`id` TEXT NOT NULL, `idemKey` TEXT NOT NULL, `kind` TEXT NOT NULL, `cashDelta` INTEGER NOT NULL, `expenseDelta` INTEGER NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `reversalOf` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_entries_idemKey` ON `cash_entries` (`idemKey`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_entries_reversalOf` ON `cash_entries` (`reversalOf`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `follow_ups` (`id` TEXT NOT NULL, `customerId` TEXT NOT NULL, `note` TEXT NOT NULL, `promiseDate` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_follow_ups_customerId` ON `follow_ups` (`customerId`)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `accountNo` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_log` (`id` TEXT NOT NULL, `action` TEXT NOT NULL, `detail` TEXT NOT NULL, `actor` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `ledger` ADD COLUMN `planId` TEXT") }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ledger` ADD COLUMN `receiptNo` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ledger` ADD COLUMN `reference` TEXT NOT NULL DEFAULT ''")
    }
}

@Database(entities = [Customer::class, Entry::class, CashEntry::class, FollowUp::class, AuditLog::class, User::class, Reconciliation::class], version = 10, exportSchema = false)
abstract class Db : RoomDatabase() { abstract fun dao(): LedgerDao; abstract fun users(): UserDao }
