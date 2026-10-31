package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class AgingTest {
    private val z = ZoneId.of("UTC"); private val now = 1_700_000_000_000L; private val day = 86_400_000L
    private fun b(late: Long) = Aging.bucket(now - late * day, now, z)
    private fun credit(id: String, amt: Long, due: Long?) = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = amt, feeDelta = 0, dueDate = due)
    private fun pay(id: String, amt: Long) = Entry(id = id, idemKey = id, customerId = "c", type = "PAYMENT", principalDelta = -amt, feeDelta = 0, cashDelta = amt)

    @Test fun bucketBoundaries() {
        assertEquals(Bucket.CURRENT, b(-8)); assertEquals(Bucket.SOON, b(-7)); assertEquals(Bucket.TODAY, b(0))
        assertEquals(Bucket.D1_7, b(1)); assertEquals(Bucket.D1_7, b(7)); assertEquals(Bucket.D8_30, b(8)); assertEquals(Bucket.D8_30, b(30))
        assertEquals(Bucket.D31_60, b(31)); assertEquals(Bucket.D31_60, b(60)); assertEquals(Bucket.D60P, b(61))
        assertEquals(Bucket.CURRENT, Aging.bucket(null, now, z))
    }
    @Test fun paymentsSettleOldestDueFirst() {
        val items = Aging.open(listOf(credit("a", 100_00, now - 30 * day), credit("b", 100_00, now - 5 * day), pay("p", 150_00)))
        assertEquals(1, items.size); assertEquals(50_00L, items[0].amount); assertEquals(now - 5 * day, items[0].due)
    }
    @Test fun reversedPaymentReopensCredit() {
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = 100_00, feeDelta = 0, cashDelta = -100_00, reversalOf = "p")
        assertEquals(100_00L, Aging.open(listOf(credit("a", 100_00, now), pay("p", 100_00), rev)).sumOf { it.amount })
    }
    @Test fun reversedCreditIsNotOpen() {
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = -100_00, feeDelta = 0, reversalOf = "a")
        assertTrue(Aging.open(listOf(credit("a", 100_00, now), rev)).isEmpty())
    }
    @Test fun overdueTotalCountsOnlyOverdueBuckets() {
        val c = Customer(id = "c", name = "Ana")
        val s = Aging.build(listOf(credit("a", 100_00, now - 10 * day), credit("b", 40_00, now + 20 * day)), listOf(c), emptyList(), now, z)
        assertEquals(100_00L, s.overdueTotal); assertEquals(140_00L, s.rows[0].balance); assertEquals(10L, s.rows[0].daysLate)
    }
}
