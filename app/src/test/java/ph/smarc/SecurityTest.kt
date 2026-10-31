package ph.smarc
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SecurityTest {
    @Test fun passwordHashVerify() {
        val s = Passwords.newSalt(); val h = Passwords.hash("Secret123", s)
        assertTrue(Passwords.verify("Secret123", h, s)); assertFalse(Passwords.verify("secret123", h, s)); assertNotEquals(h, Passwords.hash("Secret123", Passwords.newSalt()))
    }
    @Test fun passwordPolicy() {
        assertNotNull(Passwords.policy("short1")); assertNotNull(Passwords.policy("onlyletters")); assertNotNull(Passwords.policy("12345678")); assertNull(Passwords.policy("goodpass1"))
    }
    @Test fun recoveryCodeShapeAndNormalizing() {
        val c = Passwords.recoveryCode(); assertTrue(Regex("([A-HJ-NP-Z2-9]{4}-){3}[A-HJ-NP-Z2-9]{4}").matches(c))
        assertEquals(c.replace("-", ""), Passwords.normRecovery(c.lowercase()))
    }
    @Test fun rolePermissions() {
        assertTrue(Perm.can(Role.COLLECTOR, Action.PAYMENT)); assertTrue(Perm.can(Role.COLLECTOR, Action.ADD_CUSTOMER))
        listOf(Action.CREDIT, Action.WRITE_OFF, Action.REVERSE, Action.ADJUST, Action.CASH_MOVES, Action.REPORTS_EXPORT, Action.AUDIT_VIEW, Action.BACKUP, Action.VIEW_FINANCIALS).forEach { assertFalse("$it", Perm.can(Role.COLLECTOR, it)) }
        assertTrue(Perm.can(Role.ADMIN, Action.CREDIT)); assertFalse(Perm.can(Role.ADMIN, Action.BACKUP)); assertFalse(Perm.can(Role.ADMIN, Action.ERASE_DATA))
        Action.values().forEach { assertTrue(Perm.can(Role.OWNER, it)); assertFalse(Perm.can(null, it)) }
        assertNull(Perm.check(Role.OWNER, Action.CREDIT)); assertNotNull(Perm.check(Role.COLLECTOR, Action.CREDIT)); assertNotNull(Perm.check(null, Action.PAYMENT))
    }
    @Test fun backupEncryptionRoundTripAndTamper() {
        val data = "ledger".toByteArray(); val blob = BackupCrypto.encrypt(data, "backup-pw1")
        assertArrayEquals(data, BackupCrypto.decrypt(blob, "backup-pw1"))
        assertThrows(IllegalArgumentException::class.java) { BackupCrypto.decrypt(blob, "wrong-pw1") }
        val bad = blob.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertThrows(IllegalArgumentException::class.java) { BackupCrypto.decrypt(bad, "backup-pw1") }
        assertThrows(IllegalArgumentException::class.java) { BackupCrypto.decrypt("nope".toByteArray(), "x") }
    }
    @Test fun backupDataJsonRoundTrip() {
        val d = BackupData(1, BizSettings(name = "Tindahan", currency = "USD", fyMonth = 4), listOf(Customer(id = "c", name = "Ana", creditLimit = 5000)),
            listOf(Entry(id = "e", idemKey = "k", customerId = "c", type = "CREDIT", principalDelta = 100, feeDelta = 5, dueDate = 9L, planId = "p", receiptNo = 3, reference = "r")),
            listOf(CashEntry(idemKey = "x", kind = "EXPENSE", cashDelta = -5, expenseDelta = 5, category = "fuel")), listOf(FollowUp(customerId = "c", note = "called", promiseDate = 5L)),
            listOf(AuditLog(action = "A", detail = "d")), listOf(User(username = "owner", displayName = "O", role = "OWNER", pwHash = "h", pwSalt = "s", recHash = "rh", recSalt = "rs")))
        val g = Gson(); assertEquals(d, g.fromJson(g.toJson(d), BackupData::class.java))
    }
    @Test fun lockoutBackoff() {
        assertEquals(0L, Lockout.waitMs(4, 1000, 1000)); assertEquals(30_000L, Lockout.waitMs(5, 1000, 1000)); assertEquals(60_000L, Lockout.waitMs(6, 1000, 1000))
        assertEquals(0L, Lockout.waitMs(5, 0, 31_000)); assertEquals(900_000L, Lockout.waitMs(20, 1000, 1000))
    }
    @Test fun sessionTimeout() {
        assertFalse(SessionPolicy.expired(0, 999_999, 5)); assertFalse(SessionPolicy.expired(1_000, 1_000 + 4 * 60_000L, 5)); assertTrue(SessionPolicy.expired(1_000, 1_000 + 5 * 60_000L, 5))
    }
    @Test fun financialYearStart() {
        val old = Cur.fyMonth
        try { Cur.fyMonth = 4; assertEquals(LocalDate.of(2025, 4, 1), Cur.fyStart(LocalDate.of(2026, 2, 10))); assertEquals(LocalDate.of(2026, 4, 1), Cur.fyStart(LocalDate.of(2026, 5, 1)))
            Cur.fyMonth = 1; assertEquals(LocalDate.of(2026, 1, 1), Cur.fyStart(LocalDate.of(2026, 12, 31))) } finally { Cur.fyMonth = old }
    }
    @Test fun currencyApply() {
        val s = Cur.symbol; val c = Cur.code
        try { Cur.apply(BizSettings(currency = "USD")); assertEquals("$", Cur.symbol); assertEquals("$1.50", 150L.peso()) } finally { Cur.symbol = s; Cur.code = c }
    }
}
