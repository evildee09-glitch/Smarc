package ph.smarc
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class BackupReminderTest {
    private val z = ZoneId.of("UTC"); private val day = 86_400_000L; private val now = 1_800_000_000_000L; private val cfg = AlertCfg(backupDays = 7)
    private fun entry(ageDays: Long) = Entry(id = "a", idemKey = "a", customerId = "c", type = "CREDIT", principalDelta = 100_00, feeDelta = 0, createdAt = now - ageDays * day)
    private fun backupAlerts(e: List<Entry>, last: Long?, c: AlertCfg = cfg) = Alerts.build(e, emptyList(), emptyList(), now, z, c, last).filter { it.kind == AlertKind.BACKUP }

    @Test fun neverBackedUpAfterAWeekOfUse() { val a = backupAlerts(listOf(entry(10)), 0L).single(); assertFalse(a.urgent); assertTrue(a.detail.contains("No backup yet")) }
    @Test fun newUsersAreNotNaggedOnDayOne() = assertTrue(backupAlerts(listOf(entry(2)), 0L).isEmpty())
    @Test fun recentBackupIsFine() = assertTrue(backupAlerts(listOf(entry(30)), now - 3 * day).isEmpty())
    @Test fun staleBackupAlerts() = assertTrue(backupAlerts(listOf(entry(30)), now - 8 * day).single().detail.contains("8 days"))
    @Test fun urgentAfterTwiceTheInterval() = assertTrue(backupAlerts(listOf(entry(30)), now - 15 * day).single().urgent)
    @Test fun canBeSwitchedOff() = assertTrue(backupAlerts(listOf(entry(30)), 0L, AlertCfg(backupDays = 0)).isEmpty())
    @Test fun noRecordsNothingToBackUp() = assertTrue(backupAlerts(emptyList(), 0L).isEmpty())
    @Test fun notCheckedWhenNoBackupInfoIsGiven() = assertTrue(backupAlerts(listOf(entry(30)), null).isEmpty())
}
