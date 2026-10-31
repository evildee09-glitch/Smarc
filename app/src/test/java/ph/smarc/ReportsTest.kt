package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class ReportsTest {
    private fun credit(id: String, p: Long, f: Long, t: Long = 1000) = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = p, feeDelta = f, createdAt = t)
    private val pay = Entry(id = "p", idemKey = "p", customerId = "c", type = "PAYMENT", principalDelta = -500_00, feeDelta = -50_00, cashDelta = 550_00, incomeDelta = 50_00, createdAt = 2000)
    private val exp = CashEntry(idemKey = "x", kind = "EXPENSE", cashDelta = -10_00, expenseDelta = 10_00, category = "transport", createdAt = 1500)

    @Test fun csvEscapingAndInjectionGuard() {
        assertEquals("\"a,b\"", Csv.text("a,b")); assertEquals("'=1+1", Csv.text("=1+1")); assertEquals("\"say \"\"hi\"\"\"", Csv.text("say \"hi\""))
    }
    @Test fun plainAmounts() { assertEquals("-12.34", Fmt.plain(-1234)); assertEquals("0.05", Fmt.plain(5)); assertEquals("1500.00", Fmt.plain(150_000)) }
    @Test fun pnlSeparatesPrincipalFromProfit() {
        val p = Reports.pnl(listOf(credit("a", 500_00, 50_00), pay), listOf(exp), 0, 10_000)
        assertEquals(500_00L, p.issued); assertEquals(550_00L, p.collected); assertEquals(500_00L, p.recovered)
        assertEquals(50_00L, p.income); assertEquals(10_00L, p.expenses); assertEquals(40_00L, p.net)
    }
    @Test fun pnlRespectsPeriod() {
        val p = Reports.pnl(listOf(credit("a", 500_00, 50_00), pay), listOf(exp), 1800, 10_000)
        assertEquals(0L, p.issued); assertEquals(50_00L, p.income); assertEquals(0L, p.expenses)
    }
    @Test fun reversedCreditNetsOutOfIssued() {
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = -500_00, feeDelta = -50_00, reversalOf = "a", createdAt = 1100)
        assertEquals(0L, Reports.pnl(listOf(credit("a", 500_00, 50_00), rev), emptyList(), 0, 10_000).issued)
    }
    @Test fun statementRunningBalance() {
        val l = Statements.lines(Customer(id = "c", name = "Ana"), listOf(pay, credit("a", 500_00, 50_00)), "today") { "d" }
        assertEquals("Outstanding balance: PHP 0.00", l.last()); assertTrue(l.any { it.contains("550.00") })
    }
    @Test fun ledgerCsvHasHeaderAndRows() {
        val csv = Reports.ledgerCsv(listOf(credit("a", 500_00, 0), pay), mapOf("c" to "Ana, Jr"), 0, 10_000) { "d" }
        assertEquals(3, csv.trim().split("\r\n").size); assertTrue(csv.contains("\"Ana, Jr\""))
    }
}
