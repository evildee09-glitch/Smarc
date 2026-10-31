package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TaxItemsTest {
    private fun rows(vararg r: Triple<String, String, String>) = r.toList()
    @Test fun taxRoundsHalfUpAndIsCapped() { assertEquals(1200L, Tax.of(10_000, 12)); assertEquals(40L, Tax.of(333, 12)); assertEquals(0L, Tax.of(100, 0)); assertEquals(100L, Tax.of(100, 150)) }
    @Test fun grossSplitsExactly() { val gross = 100_00L + Tax.of(100_00, 12); assertEquals(gross, Calc.split(gross, 3).sum()); assertEquals(Tax.of(100_00, 12), Calc.split(Tax.of(100_00, 12), 3).sum()) }
    @Test fun itemTotalsAddUpAndBlankRowsAreSkipped() {
        val l = Items.parseRows(rows(Triple("Rice", "2", "50"), Triple("Eggs", "1.5", "10"), Triple("", "", "")))!!
        assertEquals(2, l.size); assertEquals(11_500L, l.sumOf { it.total })
    }
    @Test fun quantityRounding() = assertEquals(333L, Items.parseRows(rows(Triple("Rope", "0.333", "10")))!!.single().total)
    @Test fun invalidRowsAreRejected() {
        assertNull(Items.parseRows(rows(Triple("", "1", "5")))); assertNull(Items.parseRows(rows(Triple("Rice", "0", "5")))); assertNull(Items.parseRows(rows(Triple("Rice", "1", ""))))
        assertNull(Items.parseRows(rows(Triple("", "", "")))); assertNull(Items.parseRows(rows(Triple("Rice", "abc", "5")))); assertNull(Items.parseRows(rows(Triple("Rice", "-1", "5"))))
    }
    @Test fun encodeDecodeAndTabsInNames() {
        val l = Items.parseRows(rows(Triple("A\tB", "1", "10"), Triple("Soap", "3", "7.50")))!!; val back = Items.decode(Items.encode(l))
        assertEquals(l, back); assertEquals("A B", back[0].name)
    }
    @Test fun formatting() {
        assertEquals("1.5", Items.qtyText(1500)); assertEquals("2", Items.qtyText(2000)); assertEquals("0.25", Items.qtyText(250))
        val s = Items.format(Items.encode(Items.parseRows(rows(Triple("Rice", "2", "50")))!!)).single(); assertTrue(s.contains("Rice") && s.contains("100.00"))
    }
    @Test fun knownItemsKeepLatestPrice() {
        fun e(id: String, t: Long, price: String) = Entry(id = id, idemKey = id, customerId = "c", type = "CREDIT", principalDelta = 1, feeDelta = 0, createdAt = t, items = Items.encode(Items.parseRows(rows(Triple("Rice", "1", price)))!!))
        assertEquals(listOf("Rice" to 6000L), Items.known(listOf(e("a", 1, "50"), e("b", 2, "60"))))
    }
    @Test fun pnlExcludesTaxFromIssuedAndReportsItSeparately() {
        val e = Entry(id = "a", idemKey = "a", customerId = "c", type = "CREDIT", principalDelta = 112_00, feeDelta = 0, tax = 12_00, createdAt = 10)
        val rev = Entry(id = "r", idemKey = "r", customerId = "c", type = "REVERSAL", principalDelta = -112_00, feeDelta = 0, tax = -12_00, reversalOf = "a", createdAt = 20)
        val p = Reports.pnl(listOf(e), emptyList(), 0, 100); assertEquals(100_00L, p.issued); assertEquals(12_00L, p.tax); assertEquals(0L, p.income)
        val q = Reports.pnl(listOf(e, rev), emptyList(), 0, 100); assertEquals(0L, q.issued); assertEquals(0L, q.tax)
    }
    @Test fun attachmentsBackupRoundTripRejectsBadNames() {
        val a = Files.createTempDirectory("att").toFile(); val b = Files.createTempDirectory("att2").toFile()
        try { Attachments.dirIn(a); File(Attachments.dirIn(a), "abc-1_1.jpg").writeBytes(byteArrayOf(1, 2, 3))
            val exported = Attachments.exportAll(a); assertEquals(setOf("abc-1_1.jpg"), exported.keys)
            Attachments.importAll(b, exported + ("../evil.jpg" to "AAAA") + ("x_9.jpg" to "AAAA"))
            assertArrayEquals(byteArrayOf(1, 2, 3), File(Attachments.dirIn(b), "abc-1_1.jpg").readBytes()); assertEquals(1, Attachments.dirIn(b).listFiles()!!.size); assertFalse(File(b, "evil.jpg").exists())
        } finally { a.deleteRecursively(); b.deleteRecursively() }
    }
}
