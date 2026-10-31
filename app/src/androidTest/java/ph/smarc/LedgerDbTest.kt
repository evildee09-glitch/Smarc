package ph.smarc

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device/emulator (./gradlew connectedAndroidTest). Proves the money rules hold under real concurrent writes. */
@RunWith(AndroidJUnit4::class)
class LedgerDbTest {
    private lateinit var db: Db; private lateinit var svc: LedgerService
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), Db::class.java).build(); svc = LedgerService(db).also { it.role = Role.OWNER; it.actor = "test" } }
    @After fun close() { db.close() }
    private suspend fun newCustomer(limit: Long = 0): Customer = Customer(name = "Ana", creditLimit = limit).also { db.dao().addCustomer(it) }
    private suspend fun lend(c: Customer, amt: Long) = svc.creditPlan(c, amt, 0, null, 1, Freq.MONTHLY, "", "seed-${System.nanoTime()}").getOrThrow()

    @Test fun concurrentPaymentsNeverOverpayAndReceiptNumbersAreUniqueAndSequential() = runBlocking {
        val c = newCustomer(); lend(c, 1000_00)
        val results = (1..20).map { i -> async(Dispatchers.IO) { svc.payment(c, 100_00, "Cash", "", "", "p$i") } }.awaitAll()
        assertEquals(10, results.count { it.isSuccess })
        assertEquals(0L, db.dao().principalOf(c.id) + db.dao().feeOf(c.id))
        assertEquals((1L..10L).toList(), db.dao().dumpLedger().filter { it.type == "PAYMENT" }.map { it.receiptNo }.sorted())
    }
    @Test fun creditLimitHoldsUnderConcurrency() = runBlocking {
        val c = newCustomer(500_00)
        val r = (1..10).map { i -> async(Dispatchers.IO) { svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "k$i") } }.awaitAll()
        assertEquals(5, r.count { it.isSuccess }); assertEquals(500_00L, db.dao().principalOf(c.id))
    }
    @Test fun duplicateSubmissionIsIgnored() = runBlocking {
        val c = newCustomer(); lend(c, 100_00)
        assertTrue(svc.payment(c, 10_00, "Cash", "", "", "same").isSuccess); assertTrue(svc.payment(c, 10_00, "Cash", "", "", "same").isFailure)
        assertEquals(1, db.dao().dumpLedger().count { it.type == "PAYMENT" })
    }
    @Test fun anEntryCanOnlyBeReversedOnce() = runBlocking {
        val c = newCustomer(); lend(c, 100_00); val id = db.dao().dumpLedger().single().id
        assertTrue(svc.reverse(id, "typo").isSuccess); assertTrue(svc.reverse(id, "again").isFailure); assertEquals(0L, db.dao().principalOf(c.id))
    }
    @Test fun refundCannotExceedWhatWasPaid() = runBlocking {
        val c = newCustomer(); lend(c, 100_00); svc.payment(c, 60_00, "Cash", "", "", "p1")
        assertTrue(svc.refund(c, 70_00, "x", "r1").isFailure); assertTrue(svc.refund(c, 60_00, "returned goods", "r2").isSuccess); assertEquals(100_00L, db.dao().principalOf(c.id))
    }
    @Test fun invoiceNumbersCannotBeReused() = runBlocking {
        val c = newCustomer(); assertTrue(svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "a", "INV-1").isSuccess)
        assertTrue(svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "b", "INV-1").isFailure)
    }
    @Test fun rolesAreEnforcedInTheService() = runBlocking {
        val c = newCustomer(); lend(c, 100_00); svc.role = Role.COLLECTOR
        assertTrue(svc.creditPlan(c, 10_00, 0, null, 1, Freq.MONTHLY, "", "k").isFailure); assertTrue(svc.writeOff(c, "x").isFailure); assertTrue(svc.payment(c, 10_00, "Cash", "", "", "pp").isSuccess)
    }
    @Test fun cashReconciliationRecordsDifferencesOnlyWithAnExplanation() = runBlocking {
        val cash = CashService(db).also { it.role = Role.OWNER; it.actor = "test" }
        assertTrue(cash.add("CAPITAL_IN", 1000_00, "", "", "c1").isSuccess)
        assertTrue(cash.reconcile(900_00, "").isFailure); assertTrue(cash.reconcile(900_00, "coins missing").isSuccess); assertTrue(cash.reconcile(1000_00, "").isSuccess)
    }
    @Test fun businessesAreIsolated() = runBlocking {
        val other = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), Db::class.java).build()
        try { newCustomer(); assertEquals(1, db.dao().dumpCustomers().size); assertEquals(0, other.dao().dumpCustomers().size) } finally { other.close() }
    }

    @Test fun taxIsOwedButNeverCountedAsLentCash() = runBlocking {
        val c = newCustomer(); svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "t1", "", 12, "").getOrThrow()
        assertEquals(112_00L, db.dao().principalOf(c.id)); assertEquals(100_00L, db.dao().lentSum())
        svc.reverse(db.dao().dumpLedger().single().id, "typo").getOrThrow()
        assertEquals(0L, db.dao().principalOf(c.id)); assertEquals(0L, db.dao().lentSum())
    }
    @Test fun taxCountsTowardTheCreditLimit() = runBlocking {
        val c = newCustomer(110_00); assertTrue(svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "t2", "", 12, "").isFailure)
        assertTrue(svc.creditPlan(c, 100_00, 0, null, 1, Freq.MONTHLY, "", "t3", "", 5, "").isSuccess)
    }
    @Test fun itemsAreStoredOnceAndTaxIsSplitAcrossInstallments() = runBlocking {
        val c = newCustomer(); svc.creditPlan(c, 300_00, 0, System.currentTimeMillis(), 3, Freq.MONTHLY, "", "t4", "", 12, "1000\t30000\tRice").getOrThrow()
        val rows = db.dao().dumpLedger(); assertEquals(1, rows.count { it.items.isNotBlank() }); assertEquals(36_00L, rows.sumOf { it.tax }); assertEquals(336_00L, rows.sumOf { it.principalDelta })
    }
}
