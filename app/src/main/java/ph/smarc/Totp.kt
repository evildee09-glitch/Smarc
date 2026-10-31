package ph.smarc

import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** RFC 6238 time-based one-time passwords (works offline with any authenticator app). */
object Totp {
    private const val B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"; private val rnd = SecureRandom()
    fun newSecret(): String = (0 until 32).map { B32[rnd.nextInt(32)] }.joinToString("")
    fun decode(s: String): ByteArray {
        var buf = 0; var bits = 0; val out = ByteArrayOutputStream()
        for (ch in s.uppercase().filter { it in B32 }) { buf = ((buf shl 5) or B32.indexOf(ch)) and 0xFFFF; bits += 5; if (bits >= 8) { bits -= 8; out.write((buf shr bits) and 0xFF) } }
        return out.toByteArray()
    }
    fun code(secret: String, atMs: Long, digits: Int = 6, step: Long = 30): String {
        val counter = atMs / 1000 / step; val msg = ByteArray(8) { i -> (counter shr (8 * (7 - i))).toByte() }
        val mac = Mac.getInstance("HmacSHA1"); mac.init(SecretKeySpec(decode(secret), "HmacSHA1")); val h = mac.doFinal(msg)
        val o = h[h.size - 1].toInt() and 0xF
        val bin = ((h[o].toInt() and 0x7F) shl 24) or ((h[o + 1].toInt() and 0xFF) shl 16) or ((h[o + 2].toInt() and 0xFF) shl 8) or (h[o + 3].toInt() and 0xFF)
        var p = 1; repeat(digits) { p *= 10 }
        return (bin % p).toString().padStart(digits, '0')
    }
    fun verify(secret: String, input: String, now: Long, window: Int = 1): Boolean { val x = input.trim().toByteArray(); return (-window..window).any { MessageDigest.isEqual(code(secret, now + it * 30_000L).toByteArray(), x) } }
    fun uri(issuer: String, account: String, secret: String): String {
        fun e(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        return "otpauth://totp/${e(issuer)}:${e(account)}?secret=$secret&issuer=${e(issuer)}&algorithm=SHA1&digits=6&period=30"
    }
}
