package ph.smarc

import android.content.SharedPreferences
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

@Entity(tableName = "users", indices = [Index("username", unique = true)])
data class User(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), val username: String, val displayName: String, val role: String,
    val pwHash: String, val pwSalt: String, val recHash: String, val recSalt: String,
    val active: Boolean = true, val mustChange: Boolean = false, val bio: Boolean = false, val createdAt: Long = System.currentTimeMillis(), val totp: String = ""
)

@Dao
interface UserDao {
    @Insert suspend fun insert(u: User)
    @Update suspend fun update(u: User)
    @Query("SELECT * FROM users WHERE username=:n COLLATE NOCASE") suspend fun byName(n: String): User?
    @Query("SELECT * FROM users WHERE id=:id") suspend fun byId(id: String): User?
    @Query("SELECT * FROM users ORDER BY createdAt") fun all(): Flow<List<User>>
    @Query("SELECT COUNT(*) FROM users") suspend fun count(): Int
    @Query("SELECT COUNT(*) FROM users WHERE role='OWNER' AND active=1") suspend fun activeOwners(): Int
    @Query("SELECT * FROM users") suspend fun dump(): List<User>
    @Query("DELETE FROM users") suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAll(l: List<User>)
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `username` TEXT NOT NULL, `displayName` TEXT NOT NULL, `role` TEXT NOT NULL, `pwHash` TEXT NOT NULL, `pwSalt` TEXT NOT NULL, `recHash` TEXT NOT NULL, `recSalt` TEXT NOT NULL, `active` INTEGER NOT NULL, `mustChange` INTEGER NOT NULL, `bio` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
    }
}

data class Session(val userId: String, val username: String, val name: String, val role: Role, val mustChange: Boolean)
class OtpRequired : Exception("Enter the 6-digit code from your authenticator app")

private fun <T> fail(m: String) = Result.failure<T>(IllegalArgumentException(m))
private fun <T> deny() = Result.failure<T>(SecurityException("Not allowed for your role"))

class Accounts(private val db: Db, private val prefs: SharedPreferences, private val onErase: (String, List<String>) -> Unit = { _, _ -> }) {
    private val users = db.users(); private val dao = db.dao()
    private suspend fun hashPw(s: String, salt: String) = withContext(Dispatchers.Default) { Passwords.hash(s, salt) }
    private suspend fun okPw(s: String, h: String, salt: String) = withContext(Dispatchers.Default) { Passwords.verify(s, h, salt) }
    suspend fun audit(action: String, detail: String, actor: String) = dao.insertAudit(AuditLog(action = action, detail = detail, actor = actor))
    suspend fun needsSetup() = users.count() == 0
    fun lastName(): String = prefs.getString("last_name", "") ?: ""
    suspend fun lastBioUserId(): String? = prefs.getString("last_user", null)?.let { users.byId(it) }?.takeIf { it.bio && it.active }?.id
    private fun canManage(a: Session, t: User) = a.role == Role.OWNER || (a.role == Role.ADMIN && t.role == Role.COLLECTOR.name)
    private fun userRegex() = Regex("[a-z0-9._-]{3,30}")

    suspend fun registerOwner(username: String, display: String, pw: String): Result<String> {
        val un = username.trim().lowercase()
        if (users.count() > 0) return fail("Setup is already complete")
        if (!userRegex().matches(un)) return fail("Username: 3-30 letters, numbers, . _ -")
        if (display.isBlank()) return fail("Enter your name")
        Passwords.policy(pw)?.let { return fail(it) }
        val salt = Passwords.newSalt(); val rs = Passwords.newSalt(); val code = Passwords.recoveryCode()
        users.insert(User(username = un, displayName = display.trim(), role = Role.OWNER.name, pwHash = hashPw(pw, salt), pwSalt = salt, recHash = hashPw(Passwords.normRecovery(code), rs), recSalt = rs))
        audit("REGISTER", "owner account created", un); return Result.success(code)
    }

    suspend fun login(username: String, password: String, otp: String = ""): Result<User> {
        val n = username.trim().lowercase(); val fails = prefs.getInt("f_$n", 0); val now = System.currentTimeMillis()
        val wait = Lockout.waitMs(fails, prefs.getLong("l_$n", 0), now)
        if (wait > 0) return fail("Too many attempts. Try again in ${wait / 1000 + 1}s")
        val u = users.byName(n)
        if (u == null || !u.active || !okPw(password, u.pwHash, u.pwSalt)) {
            prefs.edit().putInt("f_$n", fails + 1).putLong("l_$n", now).apply(); audit("LOGIN_FAILED", n, n)
            return fail(if (u != null && !u.active) "This account is disabled" else "Wrong username or password")
        }
        val sec = (u.totp as String?).orEmpty()
        if (sec.isNotEmpty()) {
            if (otp.isBlank()) return Result.failure(OtpRequired())
            if (!Totp.verify(sec, otp, now)) { prefs.edit().putInt("f_$n", fails + 1).putLong("l_$n", now).apply(); audit("LOGIN_FAILED", "2fa", n); return fail("Wrong code") }
        }
        prefs.edit().remove("f_$n").remove("l_$n").putString("last_user", u.id).putString("last_name", u.username).apply(); audit("LOGIN", "", u.username); return Result.success(u)
    }

    suspend fun loginBio(userId: String): Result<User> {
        val u = users.byId(userId)?.takeIf { it.bio && it.active } ?: return fail("Biometric sign-in is not enabled")
        audit("LOGIN_BIO", "", u.username); return Result.success(u)
    }

    /** Returns a NEW recovery code (the old one stops working). */
    suspend fun resetWithRecovery(username: String, code: String, newPw: String): Result<String> {
        val n = username.trim().lowercase(); val fails = prefs.getInt("r_$n", 0); val now = System.currentTimeMillis()
        val wait = Lockout.waitMs(fails, prefs.getLong("rl_$n", 0), now)
        if (wait > 0) return fail("Too many attempts. Try again in ${wait / 1000 + 1}s")
        val u = users.byName(n)
        if (u == null || !okPw(Passwords.normRecovery(code), u.recHash, u.recSalt)) { prefs.edit().putInt("r_$n", fails + 1).putLong("rl_$n", now).apply(); audit("RESET_FAILED", n, n); return fail("Username or recovery code is not correct") }
        Passwords.policy(newPw)?.let { return fail(it) }
        val salt = Passwords.newSalt(); val rs = Passwords.newSalt(); val nc = Passwords.recoveryCode()
        users.update(u.copy(pwHash = hashPw(newPw, salt), pwSalt = salt, recHash = hashPw(Passwords.normRecovery(nc), rs), recSalt = rs, mustChange = false, active = u.active, totp = ""))
        prefs.edit().remove("r_$n").remove("rl_$n").remove("f_$n").remove("l_$n").apply(); audit("PASSWORD_RESET", "via recovery code", u.username); return Result.success(nc)
    }

    suspend fun changePassword(userId: String, old: String, new: String): Result<String> {
        val u = users.byId(userId) ?: return fail("Account not found")
        if (!okPw(old, u.pwHash, u.pwSalt)) return fail("Current password is not correct")
        Passwords.policy(new)?.let { return fail(it) }
        if (old == new) return fail("Choose a different password")
        val salt = Passwords.newSalt(); users.update(u.copy(pwHash = hashPw(new, salt), pwSalt = salt, mustChange = false)); audit("PASSWORD_CHANGED", "", u.username); return Result.success("Password changed")
    }

    suspend fun setBio(userId: String, on: Boolean) { users.byId(userId)?.let { users.update(it.copy(bio = on)); audit(if (on) "BIO_ENABLED" else "BIO_DISABLED", "", it.username) } }

    /** Returns the new user's recovery code (give it to them once). New users must change the temporary password at first login. */
    suspend fun addUser(a: Session, username: String, display: String, role: Role, tempPw: String): Result<String> {
        if (!Perm.can(a.role, Action.MANAGE_USERS) || (a.role != Role.OWNER && role != Role.COLLECTOR)) return deny()
        val un = username.trim().lowercase()
        if (!userRegex().matches(un)) return fail("Username: 3-30 letters, numbers, . _ -"); if (display.isBlank()) return fail("Enter a name")
        Passwords.policy(tempPw)?.let { return fail(it) }; if (users.byName(un) != null) return fail("That username is taken")
        val salt = Passwords.newSalt(); val rs = Passwords.newSalt(); val code = Passwords.recoveryCode()
        users.insert(User(username = un, displayName = display.trim(), role = role.name, pwHash = hashPw(tempPw, salt), pwSalt = salt, recHash = hashPw(Passwords.normRecovery(code), rs), recSalt = rs, mustChange = true))
        audit("USER_ADD", "$un as ${role.label}", a.username); return Result.success(code)
    }

    suspend fun setActive(a: Session, t: User, active: Boolean): Result<String> {
        if (!Perm.can(a.role, Action.MANAGE_USERS) || !canManage(a, t)) return deny()
        if (t.id == a.userId) return fail("You cannot disable your own account")
        if (!active && t.role == Role.OWNER.name && users.activeOwners() <= 1) return fail("At least one active owner is required")
        users.update(t.copy(active = active)); audit(if (active) "USER_ENABLED" else "USER_DISABLED", t.username, a.username); return Result.success("Updated")
    }

    suspend fun setRole(a: Session, t: User, role: Role): Result<String> {
        if (a.role != Role.OWNER) return deny()
        if (t.role == Role.OWNER.name && role != Role.OWNER && users.activeOwners() <= 1) return fail("At least one active owner is required")
        users.update(t.copy(role = role.name)); audit("USER_ROLE", "${t.username} -> ${role.label}", a.username); return Result.success("Role updated")
    }

    suspend fun adminReset(a: Session, t: User, tempPw: String): Result<String> {
        if (!Perm.can(a.role, Action.MANAGE_USERS) || !canManage(a, t) || t.id == a.userId) return deny()
        Passwords.policy(tempPw)?.let { return fail(it) }
        val salt = Passwords.newSalt(); users.update(t.copy(pwHash = hashPw(tempPw, salt), pwSalt = salt, mustChange = true, totp = "")); audit("PASSWORD_ADMIN_RESET", t.username, a.username); return Result.success("Temporary password set")
    }

    suspend fun verifyPassword(userId: String, pw: String): Result<String> {
        val u = users.byId(userId) ?: return fail("Account not found")
        return if (okPw(pw, u.pwHash, u.pwSalt)) Result.success("ok") else fail("Password is not correct")
    }
    suspend fun enableTotp(userId: String, secret: String, code: String): Result<String> {
        val u = users.byId(userId) ?: return fail("Account not found")
        if (!Totp.verify(secret, code, System.currentTimeMillis())) return fail("That code is not correct. Check the time on your phone and try again")
        users.update(u.copy(totp = secret)); audit("2FA_ENABLED", "", u.username); return Result.success("Two-factor login is on")
    }
    suspend fun disableTotp(userId: String, password: String): Result<String> {
        val u = users.byId(userId) ?: return fail("Account not found")
        if (!okPw(password, u.pwHash, u.pwSalt)) return fail("Password is not correct")
        users.update(u.copy(totp = "")); audit("2FA_DISABLED", "", u.username); return Result.success("Two-factor login is off")
    }
    /** Automatic retention: erases personal details of ARCHIVED customers idle for [months]; the ledger is kept. Runs when an owner signs in. */
    suspend fun autoRetention(a: Session, months: Int, now: Long): Int {
        if (months <= 0 || !Perm.can(a.role, Action.ERASE_DATA)) return 0
        val cutoff = now - months * 30L * 86_400_000L
        val archived = dao.dumpCustomers().filter { it.status == "ARCHIVED" && !it.name.startsWith("Erased customer") }; if (archived.isEmpty()) return 0
        val last = dao.dumpLedger().groupBy { it.customerId }.mapValues { (_, l) -> l.maxOf { it.createdAt } }
        var n = 0; archived.filter { (last[it.id] ?: it.createdAt) < cutoff }.forEach { c -> eraseCustomer(a, c).onSuccess { n++ } }
        if (n > 0) audit("AUTO_ERASE", "$n archived customer(s) past retention", a.username); return n
    }

    /** Right-to-erasure: wipes personal details of an ARCHIVED customer but keeps the financial ledger intact. */
    suspend fun eraseCustomer(a: Session, c: Customer): Result<String> = db.withTransaction {
        if (!Perm.can(a.role, Action.ERASE_DATA)) return@withTransaction deny<String>()
        if (c.status != "ARCHIVED") return@withTransaction fail<String>("Only archived customers can be erased")
        dao.updateCustomer(c.copy(name = "Erased customer ${c.id.take(4)}", phone = "", address = "", email = "", notes = "", accountNo = ""))
        dao.scrubFollowUps(c.id); onErase(c.id, dao.entriesOf(c.id).map { it.id }); dao.insertAudit(AuditLog(action = "ERASE_PERSONAL_DATA", detail = "customer ${c.id.take(8)}", actor = a.username))
        Result.success("Personal details erased")
    }
}

data class BackupData(val v: Int, val settings: BizSettings, val customers: List<Customer>, val ledger: List<Entry>, val cash: List<CashEntry>,
    val follow: List<FollowUp>, val audit: List<AuditLog>, val users: List<User>, val recons: List<Reconciliation> = emptyList(), val files: Map<String, String> = emptyMap())

class BackupService(private val db: Db, private val filesDir: java.io.File) {
    private val dao = db.dao(); private val users = db.users(); private val gson = Gson()
    suspend fun export(settings: BizSettings, pw: String): ByteArray {
        val data = db.withTransaction { BackupData(1, settings, dao.dumpCustomers(), dao.dumpLedger(), dao.dumpCash(), dao.dumpFollow(), dao.dumpAudit(), users.dump(), dao.dumpRecons()) }.let { it.copy(files = withContext(Dispatchers.IO) { Attachments.exportAll(filesDir) }) }
        return withContext(Dispatchers.Default) { BackupCrypto.encrypt(gson.toJson(data).toByteArray(), pw) }
    }
    /** Replaces ALL data in this business with the backup (an encrypted safety copy of the current data is kept first). */
    suspend fun restore(blob: ByteArray, pw: String, current: BizSettings, actor: String): Result<BizSettings> {
        val data = try { withContext(Dispatchers.Default) { gson.fromJson(String(BackupCrypto.decrypt(blob, pw)), BackupData::class.java) } }
            catch (e: Exception) { return fail(e.message ?: "Could not read the backup") }
        if (data == null || data.v != 1) return fail("Unsupported backup version")
        runCatching { java.io.File(filesDir, "pre_restore_${System.currentTimeMillis()}.smarcbak").writeBytes(export(current, pw)) }
        db.withTransaction {
            dao.clearLedger(); dao.clearCustomers(); dao.clearCash(); dao.clearFollow(); dao.clearAudit(); dao.clearRecons(); users.clear()
            dao.putCustomers(data.customers); dao.putLedger(data.ledger.map { it.copy(items = (it.items as String?).orEmpty()) }); dao.putCash(data.cash); dao.putFollow(data.follow); dao.putAudit(data.audit); dao.putRecons(data.recons ?: emptyList()); users.putAll(data.users.map { it.copy(totp = (it.totp as String?).orEmpty()) })
            dao.insertAudit(AuditLog(action = "RESTORE", detail = "${data.ledger.size} ledger entries", actor = actor))
        }
        withContext(Dispatchers.IO) { Attachments.importAll(filesDir, data.files ?: emptyMap()) }
        return Result.success(data.settings)
    }
}
