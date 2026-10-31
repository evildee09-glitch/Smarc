package ph.smarc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.math.RoundingMode
import java.util.Locale

val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `ledger` ADD COLUMN `tax` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `ledger` ADD COLUMN `items` TEXT NOT NULL DEFAULT ''")
    }
}

/** Tax rule: exclusive, added on top of the price. It is part of what the customer owes, but it is pass-through money: never income, never profit, never counted as cash you lent. */
object Tax { fun of(principal: Long, pct: Int): Long = if (pct <= 0) 0L else (principal * pct.coerceIn(0, 100) + 50) / 100 }

data class ItemLine(val name: String, val qtyMilli: Long, val unit: Long) { val total: Long get() = (qtyMilli * unit + 500) / 1000 }

/** Optional item lines on a credit. Quantities keep 3 decimals (kilos, litres); the lines add up to the price. Stored as text inside the entry. */
object Items {
    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim().take(60)
    fun parseQty(s: String): Long? {
        val d = s.trim().toBigDecimalOrNull() ?: return null
        return d.movePointRight(3).setScale(0, RoundingMode.HALF_UP).toLong().takeIf { it in 1..100_000_000 }
    }
    /** Rows are (name, qty, unit price). Completely blank rows are skipped. Null if a row is half-filled or invalid, or nothing is usable. */
    fun parseRows(rows: List<Triple<String, String, String>>): List<ItemLine>? {
        val out = mutableListOf<ItemLine>()
        for ((n, q, p) in rows) {
            if (n.isBlank() && p.isBlank()) continue
            val name = clean(n); val qty = parseQty(q.ifBlank { "1" }); val unit = if (p.isBlank()) null else parsePeso(p)
            if (name.isEmpty() || qty == null || unit == null || unit <= 0) return null
            out += ItemLine(name, qty, unit)
        }
        return out.takeIf { it.isNotEmpty() && it.size <= 50 }
    }
    fun total(rows: List<Triple<String, String, String>>): Long? = parseRows(rows)?.sumOf { it.total }
    fun encode(l: List<ItemLine>) = l.joinToString("\n") { "${it.qtyMilli}\t${it.unit}\t${it.name}" }
    fun decode(t: String): List<ItemLine> = t.lines().mapNotNull { ln -> val p = ln.split('\t', limit = 3)
        val q = p.getOrNull(0)?.toLongOrNull(); val u = p.getOrNull(1)?.toLongOrNull(); if (p.size < 3 || q == null || u == null) null else ItemLine(p[2], q, u) }
    fun qtyText(m: Long) = if (m % 1000 == 0L) "${m / 1000}" else String.format(Locale.US, "%.3f", m / 1000.0).trimEnd('0').trimEnd('.')
    fun format(t: String): List<String> = decode(t).map { "${qtyText(it.qtyMilli)} × ${it.name} @ ${it.unit.peso()} = ${it.total.peso()}" }
    /** Item names you used before with their latest price, for one-tap re-entry. */
    fun known(entries: List<Entry>): List<Pair<String, Long>> {
        val latest = HashMap<String, Triple<Long, String, Long>>()
        entries.filter { it.items.isNotBlank() }.forEach { e -> decode(e.items).forEach { l -> val k = l.name.lowercase(); val old = latest[k]; if (old == null || e.createdAt >= old.first) latest[k] = Triple(e.createdAt, l.name, l.unit) } }
        return latest.values.map { it.second to it.third }.sortedBy { it.first.lowercase() }.take(200)
    }
}

/** Up to 3 photos of the paper receipt/invoice per entry, in app-private storage, downscaled. Included in the encrypted backup. */
object Attachments {
    const val MAX = 3
    private val nameRule = Regex("[A-Za-z0-9-]+_[1-3]\\.jpg")
    fun dirIn(filesDir: File) = File(filesDir, "attachments").apply { mkdirs() }
    fun dir(ctx: Context) = dirIn(ctx.filesDir)
    fun list(ctx: Context, entryId: String): List<File> = dir(ctx).listFiles { f -> f.name.startsWith("${entryId}_") }?.sortedBy { it.name } ?: emptyList()
    fun add(ctx: Context, entryId: String, bytes: ByteArray): Boolean = runCatching {
        var target: File? = null
        for (i in 1..MAX) { val f = File(dir(ctx), "${entryId}_$i.jpg"); if (!f.exists()) { target = f; break } }
        val f = target ?: return@runCatching false
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1; while (o.outWidth / s > 1024 || o.outHeight / s > 1024) s *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })!!
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 70, it) }; true
    }.getOrDefault(false)
    fun addFrom(ctx: Context, entryId: String, uri: Uri): Boolean = ctx.contentResolver.openInputStream(uri)?.use { add(ctx, entryId, it.readBytes()) } ?: false
    fun deleteAll(ctx: Context, entryId: String) { list(ctx, entryId).forEach { it.delete() } }
    fun exportAll(filesDir: File): Map<String, String> = dirIn(filesDir).listFiles()?.filter { it.isFile }?.associate { it.name to java.util.Base64.getEncoder().encodeToString(it.readBytes()) } ?: emptyMap()
    /** Replaces the attachment folder; only well-formed file names are accepted (no path tricks from a tampered file). */
    fun importAll(filesDir: File, files: Map<String, String>) {
        val d = dirIn(filesDir); d.listFiles()?.forEach { it.delete() }
        files.forEach { (n, b) -> if (nameRule.matches(n)) File(d, n).writeBytes(java.util.Base64.getDecoder().decode(b)) }
    }
}

@Composable
private fun Thumb(f: File, remove: () -> Unit) {
    val ctx = LocalContext.current; val bmp = remember(f.path, f.lastModified()) { BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap() }
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        if (bmp != null) Image(bmp, "Receipt photo", Modifier.size(88.dp).clickable { runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f), "image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }, contentScale = ContentScale.Crop)
        TextButton(remove) { Text("Remove") }
    }
}

/** Tap a ledger row: details, itemized lines, photos of the paper receipt, and the reprinted receipt for payments. */
@Composable
fun EntryDetail(e: Entry, customer: String, all: List<Entry>, close: () -> Unit, showReceipt: (String) -> Unit) {
    val ctx = LocalContext.current; var ver by remember { mutableIntStateOf(0) }
    val files = remember(e.id, ver) { Attachments.list(ctx, e.id) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { if (Attachments.addFrom(ctx, e.id, it)) ver++ } }
    var cam by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> cam?.let { f -> if (ok && Attachments.add(ctx, e.id, f.readBytes())) ver++; f.delete() }; cam = null }
    val isPay = e.type == "PAYMENT" && e.receiptNo > 0
    AlertDialog(close, title = { Text("${e.type}  ${(e.principalDelta + e.feeDelta).peso()}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${e.createdAt.dmy()}${if (e.receiptNo > 0) " • " + e.receiptNo.orNo() else ""}${if (e.reference.isNotBlank()) " • ref " + e.reference else ""}")
            if (e.tax != 0L) Text("Includes tax ${e.tax.peso()} (pass-through, not income)")
            if (e.note.isNotBlank()) Text(e.note)
            if (e.items.isNotBlank()) { Text("Items", style = MaterialTheme.typography.titleSmall); Items.format(e.items).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) } }
            Text("Photos of the paper receipt (${files.size}/${Attachments.MAX})", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { files.forEach { f -> Thumb(f) { f.delete(); ver++ } } }
            if (files.size < Attachments.MAX) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ val f = File(File(ctx.cacheDir, "exports").apply { mkdirs() }, "cap_${e.id}.jpg"); cam = f; camera.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)) }) { Text("Take photo") }
                OutlinedButton({ gallery.launch("image/*") }) { Text("Choose photo") } }
        } },
        confirmButton = { if (isPay) TextButton({ showReceipt(Receipts.rebuild(e, all, customer)); close() }) { Text("Show receipt") } else TextButton(close) { Text("Close") } },
        dismissButton = { if (isPay) TextButton(close) { Text("Close") } })
}
