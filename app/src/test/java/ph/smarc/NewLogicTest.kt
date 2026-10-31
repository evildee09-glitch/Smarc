package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.ZipInputStream

class NewLogicTest {
    private val z = ZoneId.of("UTC"); private val day = 86_400_000L
    private val t0 = LocalDate.of(2026, 1, 1).atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun credit(id: String, amt: Long, due: Long?, at: Long, fee: Long = 0) = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = amt, feeDelta = fee, dueDate = due, createdAt = at)
    private fun pay(id: String, amt: Long, at: Long) = Entry(id = id, idemKey = id, customerId = "c", type = "PAYMENT", principalDelta = -amt, feeDelta = 0, cashDelta = amt, createdAt = at)

    @Test fun repeatedLatePayments() {
        val b = BehaviorCalc.analyze(listOf(credit("a", 100_00, t0 + 30 * day, t0), pay("p1", 60_00, t0 + 40 * day), pay("p2", 40_00, t0 + 42 * day)), 0, t0 + 60 * day, z)
        assertEquals(56, b.score); assertEquals("Repeated late payments", b.label); assertEquals(2, b.latePayments); assertEquals(1, b.settled); assertEquals(0, b.onTimePct)
    }
    @Test fun goodHistory() {
        val b = BehaviorCalc.analyze(listOf(credit("a", 100_00, t0 + 30 * day, t0), pay("p", 100_00, t0 + 29 * day)), 0, t0 + 60 * day, z)
        assertEquals(100, b.score); assertEquals("Good payment history", b.label)
    }
    @Test fun notEnoughHistory() = assertNull(BehaviorCalc.analyze(listOf(credit("a", 100_00, t0 + 30 * day, t0)), 0, t0 + day, z).score)
    @Test fun overduePenalty() = assertEquals(90, BehaviorCalc.analyze(listOf(credit("a", 100_00, t0 + 10 * day, t0)), 0, t0 + 25 * day, z).score)
    @Test fun adjustmentsAreNotPayments() {
        val adj = Entry(id = "j", idemKey = "j", customerId = "c", type = "ADJUSTMENT", principalDelta = -100_00, feeDelta = 0, createdAt = t0 + day)
        assertNull(BehaviorCalc.analyze(listOf(credit("a", 100_00, t0 + 30 * day, t0), adj), 0, t0 + 60 * day, z).score)
    }
    @Test fun forecastUsesStatedAssumptions() {
        val cash = (1..3).map { CashEntry(idemKey = "x$it", kind = "EXPENSE", cashDelta = -30_00, expenseDelta = 30_00, createdAt = t0 - 10 * day) }
        val f = ForecastCalc.estimate(listOf(credit("a", 100_00, t0 - 40 * day, t0 - 60 * day, fee = 10_00)), cash, t0, 20, z)
        assertEquals(10_00L, f.feesToCollect); assertEquals(22_00L, f.expectedLoss); assertEquals(30_00L, f.expectedExpenses); assertEquals(-42_00L, f.estimate)
    }
    @Test fun xlsxHasPartsNumbersAndSafeText() {
        val zip = ZipInputStream(Xlsx.build(listOf(listOf("Name", "Amt"), listOf("Ana, Jr", "12.34"), listOf("'=1+1", "007"))).inputStream())
        val parts = HashMap<String, String>(); generateSequence { zip.nextEntry }.forEach { parts[it.name] = String(zip.readBytes()) }
        assertTrue(parts.keys.containsAll(listOf("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/worksheets/sheet1.xml")))
        val sheet = parts["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet.contains("<v>12.34</v>")); assertTrue(sheet.contains("Ana, Jr")); assertTrue(sheet.contains(">=1+1<")); assertTrue(sheet.contains(">007<")); assertFalse(sheet.contains("<v>007</v>"))
    }
    @Test fun csvParsing() = assertEquals(listOf(listOf("a", "b,c", "d \"q\""), listOf("1", "2", "3")), Xlsx.parseCsv("a,\"b,c\",\"d \"\"q\"\"\"\r\n1,2,3\r\n"))
    @Test fun totpRfc6238Vectors() {
        val s = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        assertArrayEquals("12345678901234567890".toByteArray(), Totp.decode(s))
        assertEquals("287082", Totp.code(s, 59_000L)); assertEquals("081804", Totp.code(s, 1_111_111_109_000L)); assertEquals("005924", Totp.code(s, 1_234_567_890_000L))
        assertTrue(Totp.verify(s, "287082", 59_000L + 30_000L)); assertFalse(Totp.verify(s, "287082", 59_000L + 120_000L))
        assertTrue(Totp.uri("SmarC", "ana", s).startsWith("otpauth://totp/SmarC:ana?secret=$s"))
    }
    @Test fun lendingReserve() { assertEquals(840_00L, Fin.available(1000_00, 800_00, 20)); assertEquals(0L, Fin.available(100_00, 800_00, 50)); assertEquals(500_00L, Fin.available(500_00, -50_00, 30)) }
    @Test fun receiptReprintShowsBalanceAtThatTime() {
        val p = Entry(id = "p", idemKey = "p", customerId = "c", type = "PAYMENT", principalDelta = -40_00, feeDelta = 0, cashDelta = 40_00, incomeDelta = 0, receiptNo = 7, note = "Cash", createdAt = 2000)
        val txt = Receipts.rebuild(p, listOf(credit("a", 100_00, null, 1000), p, credit("b", 500_00, null, 3000)), "Ana")
        assertTrue(txt.contains("OR-000007")); assertTrue(txt.contains("REPRINT")); assertTrue(txt.contains("60.00")); assertFalse(txt.contains("560.00")); assertTrue(txt.contains("PAID"))
    }
    @Test fun refundKeepsBreakdownIdentity() {
        val refund = Entry(id = "r", idemKey = "r", customerId = "c", type = "REFUND", principalDelta = 200_00, feeDelta = 0, cashDelta = -200_00, createdAt = 3000)
        val b = Profile.breakdown(listOf(credit("a", 500_00, null, 1000), pay("p", 500_00, 2000), refund))
        assertEquals(200_00L, b.balance); assertEquals(b.balance, b.creditIssued + b.feesAssessed + b.other - b.repaid - b.writtenOff)
        assertEquals(200_00L, Aging.open(listOf(credit("a", 500_00, null, 1000), pay("p", 500_00, 2000), refund)).single().amount)
    }
    @Test fun profitByCustomerNetsLoss() {
        val e = listOf(Entry(id = "p", idemKey = "p", customerId = "c", type = "PAYMENT", principalDelta = 0, feeDelta = 0, cashDelta = 50_00, incomeDelta = 50_00, createdAt = 10),
            Entry(id = "w", idemKey = "w", customerId = "c", type = "WRITE_OFF", principalDelta = 0, feeDelta = 0, lossDelta = 20_00, createdAt = 20))
        assertEquals(30_00L, Insights.profitByCustomer(e, mapOf("c" to "Ana"), 0, 100).single().net)
    }
    @Test fun configurableBucketsAndTimezone() {
        val (a, b, c) = Triple(Cur.t1, Cur.t2, Cur.t3); val tz = Cur.tz
        try { Cur.t1 = 3; Cur.t2 = 14; Cur.t3 = 30
            assertEquals("Overdue 1-3 days", Bucket.D1_7.label); assertEquals("Overdue 4-14 days", Bucket.D8_30.label); assertEquals(Bucket.D8_30, Aging.bucket(t0 - 5 * day, t0, z)); assertEquals(Bucket.D60P, Aging.bucket(t0 - 31 * day, t0, z))
            Cur.tz = "Asia/Manila"; assertEquals("1970-01-01 08:00", Cur.fmt("yyyy-MM-dd HH:mm", 0L)); Cur.tz = "UTC"; assertEquals("1970-01-01 00:00", Cur.fmt("yyyy-MM-dd HH:mm", 0L))
        } finally { Cur.t1 = a; Cur.t2 = b; Cur.t3 = c; Cur.tz = tz }
    }
}
