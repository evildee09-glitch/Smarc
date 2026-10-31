package ph.smarc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File

/** Customer photos and business logos live in app-private storage (not in backups). Erasing a customer deletes the photo. */
object Photos {
    private fun dir(ctx: Context) = File(ctx.filesDir, "photos").apply { mkdirs() }
    fun file(ctx: Context, key: String) = File(dir(ctx), "$key.jpg")
    fun save(ctx: Context, key: String, uri: Uri): Boolean = runCatching {
        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1; while (o.outWidth / s > 512 || o.outHeight / s > 512) s *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })!!
        file(ctx, key).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }; true
    }.getOrDefault(false)
    fun delete(ctx: Context, key: String) { file(ctx, key).delete() }
    fun load(ctx: Context, key: String): ImageBitmap? = file(ctx, key).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
}

@Composable
fun Avatar(key: String, size: Dp, version: Int, placeholder: String?, onClick: (() -> Unit)?) {
    val ctx = LocalContext.current; val img = remember(key, version) { Photos.load(ctx, key) }
    val base = Modifier.size(size).clip(CircleShape).let { if (onClick != null) it.clickable(onClick = onClick) else it }
    if (img != null) Image(img, "Photo", base, contentScale = ContentScale.Crop)
    else if (placeholder != null) Box(base.background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) { Text(placeholder.uppercase(), color = MaterialTheme.colorScheme.primary) }
}

@Composable
fun Onboarding(done: () -> Unit) {
    val pages = listOf("Who owes me money?" to "See every customer's balance, who is overdue, and what is due this week. No more handwritten notebook.",
        "How much have I collected?" to "Every payment updates balances, receipts and your dashboard automatically. Collected, outstanding and overdue are always kept separate.",
        "What is my real profit?" to "SmarC separates money coming back (your capital) from income you actually earned, so you always see true profit.")
    var i by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SmarC", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Text("Smart Credit. Clear Control."); Spacer(Modifier.height(32.dp))
        Text(pages[i].first, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center); Spacer(Modifier.height(12.dp))
        Text(pages[i].second, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center); Spacer(Modifier.height(24.dp))
        Text(pages.indices.joinToString(" ") { if (it == i) "●" else "○" }); Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { TextButton(done) { Text("Skip") }; Button({ if (i < pages.lastIndex) i++ else done() }) { Text(if (i < pages.lastIndex) "Next" else "Get started") } }
    }
}

@Composable
fun BizAdvanced(b: BizSettings, set: (BizSettings) -> Unit) {
    Text("Time zone", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf("" to "Device", "Asia/Manila" to "Manila", "Asia/Singapore" to "Singapore", "Asia/Tokyo" to "Tokyo", "Asia/Dubai" to "Dubai", "Europe/London" to "London",
        "America/New_York" to "New York", "America/Los_Angeles" to "Los Angeles", "Australia/Sydney" to "Sydney", "UTC" to "UTC").forEach { (id, l) -> FilterChip(b.tz == id, { set(b.copy(tz = id)) }, { Text(l) }) } }
    Text("Overdue ranges (days)", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(Triple(7, 30, 60), Triple(3, 14, 30), Triple(10, 30, 90)).forEach { (x, y, z) -> FilterChip(b.t1 == x && b.t2 == y && b.t3 == z, { set(b.copy(t1 = x, t2 = y, t3 = z)) }, { Text("$x / $y / $z") }) } }
    Text("Cash reserve kept out of lending", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(0, 10, 20, 30, 50).forEach { FilterChip(b.reservePct == it, { set(b.copy(reservePct = it)) }, { Text("$it%") }) } }
    Text("Expected bad debt (for the profit estimate)", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(10, 20, 30, 50).forEach { FilterChip(b.badDebtPct == it, { set(b.copy(badDebtPct = it)) }, { Text("$it%") }) } }
    Text("Default tax added to credit sales (you can change it per sale)", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(0, 5, 10, 12, 15).forEach { FilterChip(b.taxPct == it, { set(b.copy(taxPct = it)) }, { Text(if (it == 0) "None" else "$it%") }) } }
    Text("Erase archived customers' personal details after", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(0 to "Never", 12 to "1 year", 24 to "2 years", 60 to "5 years").forEach { (m, l) -> FilterChip(b.retentionMonths == m, { set(b.copy(retentionMonths = m)) }, { Text(l) }) } }
}

@Composable
fun LogoButtons(vm: VM) {
    val ctx = LocalContext.current; val key = "logo_${vm.registry.active()}"
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { if (Photos.save(ctx, key, it)) vm.photoVersion++ } }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ pick.launch("image/*") }) { Text("Change logo") }; OutlinedButton({ Photos.delete(ctx, key); vm.photoVersion++ }) { Text("Remove logo") } }
}

@Composable
fun TwoFactorSection(vm: VM, s: Session) {
    val ctx = LocalContext.current; val users by vm.users.collectAsState()
    val on = (users.firstOrNull { it.id == s.userId }?.totp as String?).orEmpty().isNotEmpty()
    var setup by remember { mutableStateOf(false) }; var off by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf<String?>(null) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (on) "Two-factor login: ON" else "Two-factor login: off"); OutlinedButton({ if (on) off = true else setup = true }) { Text(if (on) "Turn off" else "Set up") } }
    msg?.let { Text(it) }
    if (setup) { val secret = remember { Totp.newSecret() }; var code by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog({ setup = false }, title = { Text("Set up two-factor login") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("1. Add this key to an authenticator app (Google Authenticator, Authy, Microsoft Authenticator, ...).")
            Text(secret.chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            OutlinedButton({ runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Totp.uri("SmarC", s.username, secret)))) }.onFailure { err = "No authenticator app found. Type the key above into one." } }) { Text("Open in authenticator app") }
            Text("2. Enter the 6-digit code it shows."); OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, singleLine = true, label = { Text("6-digit code") })
            err?.let { Text(it, color = Danger) }
            Text("If you lose the authenticator, your recovery code turns two-factor off.", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton({ vm.launch { vm.accounts.enableTotp(s.userId, secret, code).onSuccess { msg = it; setup = false }.onFailure { err = it.message } } }, enabled = code.length == 6) { Text("Turn on") } },
            dismissButton = { TextButton({ setup = false }) { Text("Cancel") } }) }
    if (off) { var pw by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog({ off = false }, title = { Text("Turn off two-factor login?") }, text = { Column { PwField(pw, { pw = it }, "Your password"); err?.let { Text(it, color = Danger) } } },
            confirmButton = { TextButton({ vm.launch { vm.accounts.disableTotp(s.userId, pw).onSuccess { msg = it; off = false }.onFailure { err = it.message } } }, enabled = pw.isNotEmpty()) { Text("Turn off") } },
            dismissButton = { TextButton({ off = false }) { Text("Cancel") } }) }
}

@Composable
fun DeleteBusinessSection(vm: VM) {
    val ctx = LocalContext.current; var ask by remember { mutableStateOf(false) }
    OutlinedButton({ ask = true }, Modifier.fillMaxWidth()) { Text("Delete this business...", color = Danger) }
    if (ask) { var typed by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog({ ask = false }, title = { Text("Delete ${vm.biz.name}?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("This permanently deletes every customer, transaction, user, photo and setting of this business from this phone. Make a backup first. It cannot be undone.")
            OutlinedTextField(typed, { typed = it }, singleLine = true, label = { Text("Type the business name to confirm") }); PwField(pw, { pw = it }, "Your password"); err?.let { Text(it, color = Danger) } } },
            confirmButton = { TextButton({ vm.launch { vm.deleteBusiness(pw).onSuccess { restartApp(ctx) }.onFailure { err = it.message } } }, enabled = typed.trim() == vm.biz.name && pw.isNotEmpty()) { Text("Delete", color = Danger) } },
            dismissButton = { TextButton({ ask = false }) { Text("Cancel") } }) }
}
