package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class EntriesTest {
    private fun e(id: String, type: String, p: Long, f: Long = 0, due: Long? = null, cash: Long = 0, inc: Long = 0, rev: String? = null) =
        Entry(id = id, idemKey = id, customerId = "c", type = type, principalDelta = p, feeDelta = f, dueDate = due, cashDelta = cash, incomeDelta = inc, reversalOf = rev)
    private fun pay(amt: Long) = e("pay", "PAYMENT", -amt, 0, cash = amt)

    @Test fun receiptNumberFormat() { assertEquals("OR-000005", 5L.orNo()); assertEquals("OR-123456", 123456L.orNo()) }
    @Test fun openingBalanceIsSettledByPayments() = assertEquals(60_00L, Aging.open(listOf(e("o", "OPENING", 100_00), pay(40_00))).single().amount)
    @Test fun adjustmentDecreaseReducesOpenAmount() = assertEquals(70_00L, Aging.open(listOf(e("a", "CREDIT", 100_00, due = 1000), e("j", "ADJUSTMENT", -30_00))).single().amount)
    @Test fun adjustmentIncreaseCreatesOpenItem() = assertEquals(20_00L, Aging.open(listOf(e("j", "ADJUSTMENT", 20_00))).single().amount)
    @Test fun feeAssessmentStaysOpenAfterCreditIsPaid() {
        val items = Aging.open(listOf(e("a", "CREDIT", 100_00, due = 1000), e("f", "FEE", 0, 10_00), pay(100_00)))
        assertEquals(1, items.size); assertEquals(10_00L, items[0].amount)
    }
    @Test fun reversedAdjustmentIsIgnored() =
        assertEquals(100_00L, Aging.open(listOf(e("a", "CREDIT", 100_00, due = 1000), e("j", "ADJUSTMENT", -30_00), e("r", "REVERSAL", 30_00, rev = "j"))).single().amount)
    @Test fun breakdownIdentityIncludesOpeningAndAdjustments() {
        val b = Profile.breakdown(listOf(e("a", "CREDIT", 500_00), e("o", "OPENING", 200_00), e("j", "ADJUSTMENT", -50_00), e("p", "PAYMENT", -300_00, cash = 300_00)))
        assertEquals(350_00L, b.balance); assertEquals(150_00L, b.other)
        assertEquals(b.balance, b.creditIssued + b.feesAssessed + b.other - b.repaid - b.writtenOff)
    }
    @Test fun openingBalanceCountsAsDueInPeriod() = assertEquals(100_00L, Reports.dueInPeriod(listOf(e("o", "OPENING", 100_00, due = 5000)), 0, 9000))
}
