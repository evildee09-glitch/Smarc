package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AlertsTest {
    private val z = ZoneId.of("UTC"); private val day = 86_400_000L
    private val now = LocalDate.of(2026, 10, 26).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val cfg = AlertCfg(soonDays = 7, leadDays = 1, limitPct = 90)
    private val ana = Customer(id = "c", name = "Ana", creditLimit = 1000_00)
    private fun credit(amt: Long, due: Long?) = Entry(id = "a", idemKey = "a", customerId = "c", type = "CREDIT", principalDelta = amt, feeDelta = 0, dueDate = due)
    private fun build(e: List<Entry>, f: List<FollowUp> = emptyList()) = Alerts.build(e, listOf(ana), f, now, z, cfg)

    @Test fun dueTomorrowAlert() { val a = build(listOf(credit(100_00, now + day))).single(); assertEquals("Due tomorrow", a.title); assertFalse(a.urgent) }
    @Test fun dueDayOfIsUrgent() = assertTrue(build(listOf(credit(100_00, now))).single().urgent)
    @Test fun noAlertBeyondLeadWindow() = assertTrue(build(listOf(credit(100_00, now + 3 * day))).isEmpty())
    @Test fun missedPaymentShowsDaysLate() { val a = build(listOf(credit(100_00, now - 5 * day))).single(); assertEquals(AlertKind.MISSED, a.kind); assertTrue(a.detail.contains("5d")) }
    @Test fun missedPromiseAlert() {
        val f = FollowUp(customerId = "c", note = "", promiseDate = now - 2 * day)
        assertTrue(build(listOf(credit(100_00, now + 30 * day)), listOf(f)).any { it.kind == AlertKind.PROMISE })
    }
    @Test fun creditLimitWarning() {
        assertEquals("Near credit limit", build(listOf(credit(950_00, null))).single { it.kind == AlertKind.LIMIT }.title)
        assertEquals("Credit limit reached", build(listOf(credit(1000_00, null))).single { it.kind == AlertKind.LIMIT }.title)
    }
    @Test fun paidUpCustomerHasNoAlerts() {
        val pay = Entry(id = "p", idemKey = "p", customerId = "c", type = "PAYMENT", principalDelta = -100_00, feeDelta = 0, cashDelta = 100_00)
        assertTrue(build(listOf(credit(100_00, now - 5 * day), pay)).isEmpty())
    }
    @Test fun calendarGroupsByDueDate() {
        val items = listOf(OpenItem("c", now, 10_00, "1"), OpenItem("c", now, 5_00, "2"), OpenItem("c", now - 3 * day, 7_00, "3"))
        val m = Calendar.byDay(items, mapOf("c" to "Ana"), LocalDate.of(2026, 10, 26), z)
        assertEquals(2, m[LocalDate.of(2026, 10, 26)]!!.size); assertTrue(m[LocalDate.of(2026, 10, 23)]!!.single().overdue)
    }
}
