package ph.smarc

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.room.withTransaction
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.time.ZonedDateTime
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-GCM with a PBKDF2 key from the backup password. Wrong password or tampering makes decrypt return null. */
object BackupCrypto {
    private val MAGIC = "SMARCBAK1".toByteArray()
    private fun key(pw: String, salt: ByteArray) = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pw.toCharArray(), salt, 200_000, 256)).encoded, "AES")
    fun encrypt(plain: ByteArray, pw: String): ByteArray {
        val r = SecureRandom(); val salt = ByteArray(16).also { r.nextBytes(it) }; val iv = ByteArray(12).also { r.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv))
        return MAGIC + salt + iv + c.doFinal(plain)
    }
    fun decrypt(data: ByteArray, pw: String): ByteArray? {
        val h = MAGIC.size + 28
        if (data.size <= h + 16 || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
        return runCatching { val salt = data.copyOfRange(MAGIC.size, MAGIC.size + 16); val iv = data.copyOfRange(MAGIC.size + 16, h)
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv)); c.doFinal(data, h, data.size - h) }.getOrNull()
    }
}

data class BackupData(val customers: List<Customer>, val entries: List<Entry>, val cash: List<CashEntry>, val follow: List<FollowUp>,
                      val audit: List<AuditLog>, val users: List<User>, val settings: JSONObject)

object Backup {
    private fun JSONObject.l(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun JSONObject.s(k: String): String? = if (isNull(k)) null else getString(k)
    private fun <T> JSONObject.arr(k: String, f: (JSONObject) -> T): List<T> { val a = getJSONArray(k); return (0 until a.length()).map { f(a.getJSONObject(it)) } }

    fun encode(d: BackupData): JSONObject = JSONObject().put("format", 1).put("created", System.currentTimeMillis()).put("settings", d.settings)
        .put("customers", JSONArray(d.customers.map { JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("address", it.address).put("creditLimit", it.creditLimit)
            .put("status", it.status).put("createdAt", it.createdAt).put("email", it.email).put("notes", it.notes).put("accountNo", it.accountNo) }))
        .put("entries", JSONArray(d.entries.map { JSONObject().put("id", it.id).put("idemKey", it.idemKey).put("customerId", it.customerId).put("type", it.type).put("principalDelta", it.principalDelta)
            .put("feeDelta", it.feeDelta).put("cashDelta", it.cashDelta).put("incomeDelta", it.incomeDelta).put("lossDelta", it.lossDelta).put("dueDate", it.dueDate).put("note", it.note)
            .put("reversalOf", it.reversalOf).put("createdAt", it.createdAt).put("planId", it.planId).put("receiptNo", it.receiptNo).put("reference", it.reference) }))
        .put("cash", JSONArray(d.cash.map { JSONObject().put("id", it.id).put("idemKey", it.idemKey).put("kind", it.kind).put("cashDelta", it.cashDelta).put("expenseDelta", it.expenseDelta)
            .put("category", it.category).put("note", it.note).put("reversalOf", it.reversalOf).put("createdAt", it.createdAt) }))
        .put("follow", JSONArray(d.follow.map { JSONObject().put("id", it.id).put("customerId", it.customerId).put("note", it.note).put("promiseDate", it.promiseDate).put("createdAt", it.createdAt) }))
        .put("audit", JSONArray(d.audit.map { JSONObject().put("id", it.id).put("action", it.action).put("detail", it.detail).put("actor", it.actor).put("createdAt", it.createdAt) }))
        .put("users", JSONArray(d.users.map { JSONObject().put("id", it.id).put("username", it.username).put("displayName", it.displayName).put("role", it.role).put("salt", it.salt)
            .put("hash", it.hash).put("active", it.active).put("recoverySalt", it.recoverySalt).put("recoveryHash", it.recoveryHash).put("createdAt", it.createdAt) }))

    fun decode(o: JSONObject): BackupData {
        require(o.optInt("format") == 1) { "Unsupported backup format" }
        return BackupData(
            o.arr("customers") { Customer(it.getString("id"), it.getString("name"), it.getString("phone"), it.getString("address"), it.getLong("creditLimit"), it.getString("status"), it.getLong("createdAt"), it.getString("email"), it.getString("notes"), it.getString("accountNo")) },
            o.arr("entries") { Entry(it.getString("id"), it.getString("idemKey"), it.getString("customerId"), it.getString("type"), it.getLong("principalDelta"), it.getLong("feeDelta"), it.getLong("cashDelta"), it.getLong("incomeDelta"), it.getLong("lossDelta"),
                it.l("dueDate"), it.getString("note"), it.s("reversalOf"), it.getLong("createdAt"), it.s("planId"), it.getLong("receiptNo"), it.getString("reference")) },
            o.arr("cash") { CashEntry(it.getString("id"), it.getString("idemKey"), it.getString("kind"), it.getLong("cashDelta"), it.getLong("expenseDelta"), it.getString("category"), it.getString("note"), it.s("reversalOf"), it.getLong("createdAt")) },
            o.arr("follow") { FollowUp(it.getString("id"), it.getString("customerId"), it.getString("note"), it.l("promiseDate"), it.getLong("createdAt")) },
            o.arr("audit") { AuditLog(it.getString("id"), it.getString("action"), it.getString("detail"), it.getString("actor"), it.getLong("createdAt")) },
            o.arr("users") { User(it.getString("id"), it.getString("username"), it.getString("displayName"), it.getString("role"), it.getString("salt"), it.getString("hash"), it.getBoolean("active"), it.s("recoverySalt"), it.s("recoveryHash"), it.getLong("createdAt")) },
            o.getJSONObject("settings"))
    }

    suspend fun dump(db: Db): BackupData { val d = db.dao(); return BackupData(d.customersAll(), d.ledgerAll(), d.cashAll(), d.followAll(), d.auditAll(), d.usersAll(), AppCfg.toJson()) }
    suspend fun restoreInto(db: Db, b: BackupData) = db.withTransaction {
        val d = db.dao(); d.wipeLedger(); d.wipeCustomers(); d.wipeCash(); d.wipeFollow(); d.wipeAudit(); d.wipeUsers()
        d.putCustomers(b.customers); d.putEntries(b.entries); d.putCash(b.cash); d.putFollow(b.follow); d.putAudit(b.audit); d.putUsers(b.users)
    }

    suspend fun create(db: Db, pw: String): Result<ByteArray> {
        if (!Session.can(Perm.BACKUP)) return Result.failure(SecurityException("Only the owner can create backups"))
        Policy.password(pw)?.let { return Result.failure(IllegalArgumentException(it)) }
        val bytes = BackupCrypto.encrypt(encode(dump(db)).toString().toByteArray(), pw)
        db.dao().insertAudit(AuditLog(action = "BACKUP_CREATED", detail = "${bytes.size / 1024} KB")); return Result.success(bytes)
    }

    /** Replaces ALL data. A safety snapshot of the current data is written first (encrypted with the same password). */
    suspend fun restore(db: Db, bytes: ByteArray, pw: String, safetyDir: File): Result<String> {
        if (!Session.can(Perm.BACKUP)) return Result.failure(SecurityException("Only the owner can restore backups"))
        val plain = BackupCrypto.decrypt(bytes, pw) ?: return Result.failure(IllegalArgumentException("Wrong password, or this is not a SmarC backup"))
        val data = runCatching { decode(JSONObject(String(plain))) }.getOrElse { return Result.failure(IllegalArgumentException("The backup file is damaged")) }
        File(safetyDir, "pre_restore_${System.currentTimeMillis()}.smarcbak").writeBytes(BackupCrypto.encrypt(encode(dump(db)).toString().toByteArray(), pw))
        restoreInto(db, data); AppCfg.fromJson(data.settings)
        db.dao().insertAudit(AuditLog(action = "BACKUP_RESTORED", detail = "${data.entries.size} ledger entries")); return Result.success("Restored ${data.entries.size} ledger entries and ${data.customers.size} customers")
    }

    fun shareBytes(ctx: Context, name: String, bytes: ByteArray) {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }; val f = File(dir, name); f.writeBytes(bytes)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/octet-stream").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Save backup"))
    }
}

object Privacy {
    suspend fun purgeAudit(db: Db, months: Int): Int {
        if (months <= 0) return 0
        return db.dao().deleteAuditBefore(ZonedDateTime.now(AppCfg.zone).minusMonths(months.toLong()).toInstant().toEpochMilli())
    }
    /** Removes personal details of archived customers; ledger entries stay so the books still reconcile. */
    suspend fun anonymizeArchived(db: Db): Result<Int> = db.withTransaction {
        if (!Session.can(Perm.BACKUP)) return@withTransaction Result.failure(SecurityException("Only the owner can do this"))
        val d = db.dao(); val l = d.archivedCustomers()
        l.forEach { d.updateCustomer(it.copy(name = "Archived customer ${it.id.take(6)}", phone = "", address = "", email = "", notes = "", accountNo = "")) }
        d.insertAudit(AuditLog(action = "ANONYMIZED", detail = "${l.size} archived customers")); Result.success(l.size)
    }
}

/** Encrypted Room database (SQLCipher). Fresh key per install, kept only in wrapped form. A plaintext database from older builds is migrated once. */
object SecureDb {
    private fun build(ctx: Context, name: String, key: ByteArray?): Db {
        val b = Room.databaseBuilder(ctx, Db::class.java, name).addMigrations(*ALL_MIGRATIONS)
        if (key != null) { System.loadLibrary("sqlcipher"); b.openHelperFactory(SupportOpenHelperFactory(key)) }
        return b.build()
    }
    fun open(ctx: Context): Db = build(ctx, "smarc_secure.db", KeyVault.dbKey(ctx))
    suspend fun migrateLegacy(ctx: Context, secure: Db) {
        val f = ctx.getDatabasePath("smarc.db"); if (!f.exists()) return
        val old = build(ctx, "smarc.db", null)
        try { restoreCheck(old, secure) } finally { old.close() }
        listOf("", "-wal", "-shm", "-journal").forEach { File(f.path + it).delete() }   // only reached if the copy succeeded
    }
    private suspend fun restoreCheck(old: Db, secure: Db) {
        val data = Backup.dump(old); Backup.restoreInto(secure, data)
        check(secure.dao().ledgerAll().size == data.entries.size) { "Migration verification failed" }
    }
}
