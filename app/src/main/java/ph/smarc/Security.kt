package ph.smarc

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64 as AndroidBase64
import androidx.room.Room
import com.google.gson.Gson
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class Role(val label: String) { OWNER("Owner"), ADMIN("Administrator"), COLLECTOR("Collector") }
enum class Action { VIEW_FINANCIALS, ADD_CUSTOMER, EDIT_CUSTOMER, CREDIT, PAYMENT, REVERSE, WRITE_OFF, ADJUST, CASH_MOVES, REPORTS_EXPORT, AUDIT_VIEW, MANAGE_USERS, SETTINGS, BACKUP, ERASE_DATA, REFUND }

/** Role matrix. Enforced inside the services (not just by hiding buttons). No session/role = no permissions. */
object Perm {
    private val collector = setOf(Action.ADD_CUSTOMER, Action.PAYMENT)
    private val admin = Action.values().toSet() - setOf(Action.BACKUP, Action.ERASE_DATA)
    fun can(role: Role?, a: Action) = when (role) { Role.OWNER -> true; Role.ADMIN -> a in admin; Role.COLLECTOR -> a in collector; null -> false }
    fun check(role: Role?, a: Action): Result<String>? = if (can(role, a)) null else Result.failure(SecurityException("Not allowed for your role"))
}

object Passwords {
    private val rnd = SecureRandom()
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"   // no 0/O/1/I
    fun newSalt(): String = Base64.getEncoder().encodeToString(ByteArray(16).also { rnd.nextBytes(it) })
    fun hash(secret: String, salt: String): String = Base64.getEncoder().encodeToString(
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(secret.toCharArray(), Base64.getDecoder().decode(salt), 120_000, 256)).encoded)
    fun verify(secret: String, expected: String, salt: String) = MessageDigest.isEqual(expected.toByteArray(), hash(secret, salt).toByteArray())
    fun policy(pw: String): String? = when { pw.length < 8 -> "Use at least 8 characters"; pw.none { it.isLetter() } -> "Include at least one letter"; pw.none { it.isDigit() } -> "Include at least one number"; else -> null }
    fun recoveryCode(): String = (0 until 16).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.chunked(4).joinToString("-") { it.joinToString("") }
    fun normRecovery(s: String) = s.uppercase().filter { it.isLetterOrDigit() }
}

/** Password-based authenticated encryption (PBKDF2 + AES-256-GCM) for backup files. */
object BackupCrypto {
    private val MAGIC = "SMRC1".toByteArray()
    private fun key(pw: String, salt: ByteArray) = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pw.toCharArray(), salt, 120_000, 256)).encoded, "AES")
    fun encrypt(data: ByteArray, pw: String): ByteArray {
        val rnd = SecureRandom(); val salt = ByteArray(16).also { rnd.nextBytes(it) }; val iv = ByteArray(12).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv)); c.updateAAD(MAGIC)
        return MAGIC + salt + iv + c.doFinal(data)
    }
    fun decrypt(blob: ByteArray, pw: String): ByteArray {
        val h = MAGIC.size; require(blob.size > h + 28 && blob.copyOfRange(0, h).contentEquals(MAGIC)) { "This is not a SmarC backup file" }
        val salt = blob.copyOfRange(h, h + 16); val iv = blob.copyOfRange(h + 16, h + 28)
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(pw, salt), GCMParameterSpec(128, iv)); c.updateAAD(MAGIC)
        try { return c.doFinal(blob, h + 28, blob.size - h - 28) } catch (e: GeneralSecurityException) { throw IllegalArgumentException("Wrong password or damaged file") }
    }
}

object Lockout {   // 5 free attempts, then 30s doubling up to 15 min
    fun waitMs(fails: Int, lastFailAt: Long, now: Long): Long {
        if (fails < 5) return 0
        return maxOf(0L, minOf(900_000L, 30_000L shl (fails - 5).coerceAtMost(5)) - (now - lastFailAt))
    }
}
object SessionPolicy { fun expired(leftAt: Long, now: Long, minutes: Int) = leftAt > 0 && now - leftAt >= minutes * 60_000L }

data class BizSettings(val name: String = "My Business", val owner: String = "", val phone: String = "", val email: String = "",
    val currency: String = "PHP", val fyMonth: Int = 1, val dateFmt: String = "MMM d, yyyy", val tz: String = "", val reservePct: Int = 0,
    val t1: Int = 7, val t2: Int = 30, val t3: Int = 60, val badDebtPct: Int = 20, val retentionMonths: Int = 0, val taxPct: Int = 0)

object Cur {
    val options = listOf("PHP" to "₱", "USD" to "$", "EUR" to "€", "GBP" to "£", "SGD" to "S$", "AUD" to "A$")
    val dateFormats = listOf("MMM d, yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")
    var symbol = "₱"; var code = "PHP"; var datePattern = "MMM d, yyyy"; var fyMonth = 1
    var tz = ""; var reservePct = 0; var t1 = 7; var t2 = 30; var t3 = 60; var badDebtPct = 20; var taxPct = 0
    val zone: java.time.ZoneId get() = runCatching { java.time.ZoneId.of(tz) }.getOrDefault(java.time.ZoneId.systemDefault())
    fun fmt(pattern: String, ms: Long): String = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(zone) }.format(java.util.Date(ms))
    fun apply(s: BizSettings) {
        code = s.currency; symbol = options.firstOrNull { it.first == s.currency }?.second ?: s.currency; datePattern = s.dateFmt; fyMonth = s.fyMonth.coerceIn(1, 12)
        tz = s.tz; reservePct = s.reservePct.coerceIn(0, 100); badDebtPct = s.badDebtPct.coerceIn(0, 100); taxPct = s.taxPct.coerceIn(0, 100)
        t1 = s.t1.coerceAtLeast(1); t2 = s.t2.coerceAtLeast(t1 + 1); t3 = s.t3.coerceAtLeast(t2 + 1)
    }
    fun fyStart(d: LocalDate): LocalDate = LocalDate.of(if (d.monthValue >= fyMonth) d.year else d.year - 1, fyMonth, 1)
}

object BizStore {
    fun load(sp: SharedPreferences, id: String): BizSettings = sp.getString("bs_$id", null)?.let { runCatching { Gson().fromJson(it, BizSettings::class.java) }.getOrNull() } ?: BizSettings()
    fun save(sp: SharedPreferences, id: String, s: BizSettings) { sp.edit().putString("bs_$id", Gson().toJson(s)).apply() }
}

/** Each business is a separate encrypted database file, so data cannot mix between businesses. */
class BizRegistry(ctx: Context) {
    private val sp = ctx.getSharedPreferences("smarc_biz", Context.MODE_PRIVATE)
    fun all(): List<Pair<String, String>> = sp.getStringSet("list", emptySet())!!.map { it.substringBefore("|") to it.substringAfter("|") }.sortedBy { it.second.lowercase() }
    fun active(): String { sp.getString("active", null)?.let { return it }; return add("My Business").also { setActive(it) } }
    fun add(name: String): String { val id = java.util.UUID.randomUUID().toString().take(8); sp.edit().putStringSet("list", sp.getStringSet("list", emptySet())!! + "$id|${name.replace("|", " ")}").apply(); return id }
    fun remove(id: String) {
        val s = sp.getStringSet("list", emptySet())!!.filterNot { it.startsWith("$id|") }.toSet(); val e = sp.edit().putStringSet("list", s)
        if (sp.getString("active", null) == id) { val next = s.firstOrNull()?.substringBefore("|"); if (next != null) e.putString("active", next) else e.remove("active") }
        e.apply()
    }
    fun setActive(id: String) { sp.edit().putString("active", id).apply() }
    fun rename(id: String, name: String) { val s = sp.getStringSet("list", emptySet())!!.filterNot { it.startsWith("$id|") }.toSet() + "$id|${name.replace("|", " ")}"; sp.edit().putStringSet("list", s).apply() }
}

/** Per-business database key: random 32 bytes wrapped by a non-exportable Android Keystore key. Databases created by v1.0 keep their old shared key. */
object KeyVault {
    private const val ALIAS = "smarc_master"
    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        return g.generateKey()
    }
    private fun sp(ctx: Context) = ctx.getSharedPreferences("smarc_vault", Context.MODE_PRIVATE)
    fun forget(ctx: Context, id: String) { sp(ctx).edit().remove("dbkey_$id").apply() }
    fun dbPassphrase(ctx: Context, id: String): ByteArray {
        val sp = sp(ctx)
        val name = if (sp.contains("dbkey_$id")) "dbkey_$id" else if (sp.contains("dbkey") && ctx.getDatabasePath("smarc_$id.db").exists()) "dbkey" else "dbkey_$id"
        sp.getString(name, null)?.let { s -> val raw = AndroidBase64.decode(s, AndroidBase64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, raw.copyOfRange(0, 12))); return c.doFinal(raw, 12, raw.size - 12) }
        val pass = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, masterKey())
        sp.edit().putString(name, AndroidBase64.encodeToString(c.iv + c.doFinal(pass), AndroidBase64.NO_WRAP)).apply()
        return pass
    }
}

object Dbs {
    fun open(ctx: Context, bizId: String): Db {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(ctx, Db::class.java, "smarc_$bizId.db").openHelperFactory(SupportOpenHelperFactory(KeyVault.dbPassphrase(ctx, bizId)))
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).build()
    }
}
