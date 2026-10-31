package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class CalcTest {
    @Test fun feesSettleBeforePrincipal() = assertEquals(50_00L to 50_00L, Calc.allocate(100_00, 50_00, 500_00))
    @Test fun partialPaymentOnlyCoversFees() = assertEquals(30_00L to 0L, Calc.allocate(30_00, 50_00, 500_00))
    @Test fun fullSettlement() = assertEquals(50_00L to 500_00L, Calc.allocate(550_00, 50_00, 500_00))
    @Test(expected = IllegalArgumentException::class) fun overpaymentRejected() { Calc.allocate(551_00, 50_00, 500_00) }
    @Test fun pesoParsing() { assertEquals(12_345L, parsePeso("123.45")); assertNull(parsePeso("-5")); assertNull(parsePeso("abc")) }
}
