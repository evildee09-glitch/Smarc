package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class ProfileTest {
    private fun e(id: String, type: String, p: Long, f: Long, cash: Long = 0, inc: Long = 0, rev: String? = null) =
        Entry(id = id, idemKey = id, customerId = "c", type = type, principalDelta = p, feeDelta = f, cashDelta = cash, incomeDelta = inc, reversalOf = rev)
    private fun cust(id: String, name: String, phone: String = "", acct: String = "") = Customer(id = id, name = name, phone = phone, accountNo = acct)
    private fun sum(c: Customer, bal: Long) = CustomerSummary(c, bal, null)

    @Test fun breakdownPaidInFull() {
        val b = Profile.breakdown(listOf(e("a", "CREDIT", 500_00, 50_00), e("p", "PAYMENT", -500_00, -50_00, 550_00, 50_00)))
        assertEquals(500_00L, b.creditIssued); assertEquals(50_00L, b.feesAssessed); assertEquals(550_00L, b.repaid); assertEquals(0L, b.balance)
    }
    @Test fun breakdownIdentityWithWriteOff() {
        val b = Profile.breakdown(listOf(e("a", "CREDIT", 500_00, 0), e("w", "WRITE_OFF", -200_00, 0)))
        assertEquals(200_00L, b.writtenOff); assertEquals(300_00L, b.balance); assertEquals(b.balance, b.creditIssued + b.feesAssessed - b.repaid - b.writtenOff)
    }
    @Test fun reversedCreditNotCounted() =
        assertEquals(0L, Profile.breakdown(listOf(e("a", "CREDIT", 500_00, 0), e("r", "REVERSAL", -500_00, 0, rev = "a"))).creditIssued)

    @Test fun duplicateByNameAndPhone() {
        val others = listOf(cust("1", "Juan Dela Cruz", "0917 123 4567"), cust("2", "Ana"))
        assertEquals(1, Dupes.find("juan  dela-cruz", "", others).size)
        assertEquals(1, Dupes.find("Pedro", "+63 917 123 4567", others).size)
        assertTrue(Dupes.find("Anna", "", others).isEmpty())          // short names are not fuzzy-matched
        assertTrue(Dupes.find("Juan Dela Cruz", "", others, selfId = "1").isEmpty())
    }
    @Test fun listFilterAndSort() {
        val l = listOf(sum(cust("a", "Zed", acct = "A-9"), 0), sum(cust("b", "Bea"), 100_00), sum(cust("c", "Cal"), 50_00))
        assertEquals(listOf("a"), CustomerView.apply(l, emptyMap(), "", 3, 0).map { it.c.id })
        assertEquals(listOf("b", "c"), CustomerView.apply(l, emptyMap(), "", 2, 1).map { it.c.id })
        assertEquals(listOf("a"), CustomerView.apply(l, emptyMap(), "a-9", 0, 0).map { it.c.id })
        assertEquals(listOf("b", "c", "a"), CustomerView.apply(l, emptyMap(), "", 0, 0).map { it.c.id })
    }
}
