package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class DashTest {
    private val z = ZoneId.of("UTC")
    private fun at(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val now = at(2026, 10, 26)
    private fun credit(id: String, p: Long, t: Long, due: Long? = null) = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = p, feeDelta = 0, dueDate = due, createdAt = t)
    private fun pay(id: String, p: Long, t: Long) = Entry(id = id, idemKey = id, customerId = "c", type = "PAYMENT", principalDelta = -p, feeDelta = 0, cashDelta = p, createdAt = t)

    @Test fun deltaText() {
        assertEquals("▲ 10%", Trend.delta(110, 100)); assertEquals("▼ 10%", Trend.delta(90, 100)); assertEquals("new", Trend.delta(5, 0))
        assertEquals("no change", Trend.delta(0, 0)); assertEquals("", Trend.delta(5, null))
    }
    @Test fun collectionRateUsesAmountsDueInPeriod() {
        val e = listOf(credit("a", 1000, 1000, due = 5000), pay("p", 400, 6000))
        assertEquals(40.0, Reports.collectionRate(e, 4000, 9000)!!, 0.001)
        assertNull(Reports.collectionRate(e, 6000, 9000))
    }
    @Test fun reversedCreditNotDue() {
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = -1000, feeDelta = 0, reversalOf = "a", createdAt = 2000)
        assertEquals(0L, Reports.dueInPeriod(listOf(credit("a", 1000, 1000, due = 5000), rev), 0, 9000))
    }
    @Test fun monthlySeries() {
        val s = Trend.series(listOf(credit("a", 500_00, at(2026, 10, 3)), pay("p", 300_00, at(2026, 10, 10))), Period.MONTH, now, z)
        assertEquals(26, s.size); assertEquals(500_00L, s[2].issued); assertEquals(300_00L, s[9].collected)
        assertEquals(200_00L, s.last().receivables); assertEquals(0L, s[0].receivables)
    }
    @Test fun previousWindowIsSameElapsedSpan() {
        val (start, end) = Reports.previous(Period.MONTH, now, z)!!
        assertEquals(at(2026, 9, 1) - 12 * 3_600_000L, start); assertTrue(end < at(2026, 10, 1)); assertNull(Reports.previous(Period.ALL, now, z))
    }
}
