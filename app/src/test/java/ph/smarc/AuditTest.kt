package ph.smarc
import org.junit.Assert.*
import org.junit.Test

class AuditTest {
    @Test fun csvOrdersOldestFirstAndEscapes() {
        val csv = AuditCsv.build(listOf(AuditLog(action = "PAYMENT", detail = "Ana, Jr ₱5.00", createdAt = 2), AuditLog(action = "EXPORT", detail = "=cmd", createdAt = 1))) { "t$it" }
        val rows = csv.trim().split("\r\n")
        assertEquals("Time,Actor,Action,Detail", rows[0]); assertTrue(rows[1].contains("EXPORT") && rows[1].contains("'=cmd")); assertTrue(rows[2].contains("\"Ana, Jr ₱5.00\""))
    }
}
