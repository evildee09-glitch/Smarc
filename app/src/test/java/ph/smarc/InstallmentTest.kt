package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class InstallmentTest {
    private val z = ZoneId.of("UTC"); private val day = 86_400_000L
    private fun at(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun ld(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(z).toLocalDate()
    private val now = at(2026, 10, 26)
    private fun credit(id: String, amt: Long, due: Long, plan: String? = "p") = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = amt, feeDelta = 0, dueDate = due, planId = plan)
    private fun pay(amt: Long) = Entry(id = "pay", idemKey = "pay", customerId = "c", type = "PAYMENT", principalDelta = -amt, feeDelta = 0, cashDelta = amt)

    @Test fun splitKeepsTotalAndFrontLoadsRemainder() {
        assertEquals(listOf(34L, 33L, 33L), Calc.split(100, 3)); assertEquals(1000L, Calc.split(1000, 7).sum()); assertEquals(listOf(0L, 0L, 0L), Calc.split(0, 3))
    }
    @Test fun scheduleDates() {
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)), Schedule.dates(at(2026, 1, 31), 3, Freq.MONTHLY, z).map(::ld))
        assertEquals(LocalDate.of(2026, 1, 8), ld(Schedule.dates(at(2026, 1, 1), 2, Freq.WEEKLY, z)[1])); assertEquals(LocalDate.of(2026, 1, 16), ld(Schedule.dates(at(2026, 1, 1), 2, Freq.SEMI, z)[1]))
    }
    @Test fun paymentsSettleEarliestInstallmentsFirst() {
        val e = listOf(credit("a", 100_00, now - 20 * day), credit("b", 100_00, now - 10 * day), credit("c", 100_00, now + 10 * day), pay(150_00))
        val items = Plans.build(e).single().items
        assertEquals(listOf(0L, 50_00L, 100_00L), items.map { it.open })
        assertEquals(listOf("Paid", "Overdue", "Upcoming"), items.map { Plans.state(it, now, z) })
    }
    @Test fun reversedInstallmentsAreNotListed() {
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = -100_00, feeDelta = 0, reversalOf = "a")
        assertEquals(1, Plans.build(listOf(credit("a", 100_00, now), credit("b", 100_00, now + day), rev)).single().items.size)
    }
    @Test fun scheduleCsvListsEveryInstallment() {
        val csv = Plans.csv(listOf(credit("a", 100_00, now), credit("b", 100_00, now + day)), mapOf("c" to "Ana"), now) { "d" }
        assertEquals(3, csv.trim().split("\r\n").size); assertTrue(csv.contains("1/2") && csv.contains("2/2"))
    }
}
