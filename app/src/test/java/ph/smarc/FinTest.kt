package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class FinTest {
    @Test fun netProfitSubtractsLossAndExpenses() = assertEquals(20_00L, Fin.netProfit(50_00, 20_00, 10_00))
    @Test fun netProfitCanBeNegative() = assertEquals(-30_00L, Fin.netProfit(0, 20_00, 10_00))
    @Test fun issuedEqualsRecoveredPlusOutstandingPlusLoss() = assertEquals(500_00L, Fin.issued(400_00, 100_00, 0))
    @Test fun issuedIncludesWriteOffs() = assertEquals(500_00L, Fin.issued(300_00, 100_00, 100_00))
    @Test fun cashOnHandSeparateFromProfit() = assertEquals(1020_00L, Fin.cashOnHand(1000_00, 550_00, 500_00, 30_00))
    @Test fun principalRepaymentDoesNotChangeProfit() {
        // lend 500, collect 550 (50 is fee income): profit stays 50 regardless of principal returned
        assertEquals(50_00L, Fin.netProfit(income = 50_00, loss = 0, expenses = 0))
    }
}
