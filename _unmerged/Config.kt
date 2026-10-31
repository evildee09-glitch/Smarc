package ph.smarc

import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Money { var symbol = "₱"; var code = "PHP" }

/** Business settings from the setup wizard. Plain values, persisted in SharedPreferences (no secrets here). */
object AppCfg {
    var business = "SmarC"; var phone = ""; var email = ""; var address = ""
    var currency = "PHP"; var tz: String = ZoneId.systemDefault().id; var fyMonth = 1; var dateFmt = "MMM d, yyyy"
    var sessionMin = 15; var retentionMonths = 0; var setupDone = false; var lastBackup = 0L
    val currencies = mapOf("PHP" to "₱", "USD" to "$", "EUR" to "€", "GBP" to "£", "SGD" to "S$", "AUD" to "A$")
    val zone: ZoneId get() = runCatching { ZoneId.of(tz) }.getOrDefault(ZoneId.systemDefault())
    /** Start of the financial year that contains [d]. */
    fun fyStart(d: LocalDate): LocalDate = if (d.monthValue >= fyMonth) LocalDate.of(d.year, fyMonth, 1) else LocalDate.of(d.year - 1, fyMonth, 1)
    fun fmt(ms: Long): String = SimpleDateFormat(dateFmt, Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone(zone) }.format(Date(ms))
    fun apply() { Money.symbol = currencies[currency] ?: "₱"; Money.code = if (currencies.containsKey(currency)) currency else "PHP" }

    fun load(sp: SharedPreferences) {
        business = sp.getString("biz", business) ?: business; phone = sp.getString("cfg_phone", "") ?: ""; email = sp.getString("cfg_email", "") ?: ""
        address = sp.getString("cfg_addr", "") ?: ""; currency = sp.getString("cfg_cur", "PHP") ?: "PHP"; tz = sp.getString("cfg_tz", tz) ?: tz
        fyMonth = sp.getInt("cfg_fy", 1); dateFmt = sp.getString("cfg_df", dateFmt) ?: dateFmt; sessionMin = sp.getInt("cfg_sess", 15)
        retentionMonths = sp.getInt("cfg_ret", 0); setupDone = sp.getBoolean("cfg_setup", false); lastBackup = sp.getLong("cfg_lastbk", 0L); apply()
    }
    fun save(sp: SharedPreferences) { sp.edit().putString("biz", business).putString("cfg_phone", phone).putString("cfg_email", email).putString("cfg_addr", address)
        .putString("cfg_cur", currency).putString("cfg_tz", tz).putInt("cfg_fy", fyMonth).putString("cfg_df", dateFmt).putInt("cfg_sess", sessionMin)
        .putInt("cfg_ret", retentionMonths).putBoolean("cfg_setup", setupDone).putLong("cfg_lastbk", lastBackup).apply() }
    fun toJson(): JSONObject = JSONObject().put("business", business).put("phone", phone).put("email", email).put("address", address).put("currency", currency)
        .put("tz", tz).put("fyMonth", fyMonth).put("dateFmt", dateFmt).put("sessionMin", sessionMin).put("retentionMonths", retentionMonths).put("setupDone", setupDone)
    fun fromJson(o: JSONObject) { business = o.optString("business", business); phone = o.optString("phone", ""); email = o.optString("email", ""); address = o.optString("address", "")
        currency = o.optString("currency", "PHP"); tz = o.optString("tz", tz); fyMonth = o.optInt("fyMonth", 1); dateFmt = o.optString("dateFmt", dateFmt)
        sessionMin = o.optInt("sessionMin", 15); retentionMonths = o.optInt("retentionMonths", 0); setupDone = o.optBoolean("setupDone", false); apply() }
}
