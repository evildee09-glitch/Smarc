package ph.smarc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.time.LocalDate

fun Result<String>.text(): String = fold({ it }, { it.message ?: "Error" })
fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
fun restartApp(ctx: Context) { ctx.startActivity(Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); ctx.findActivity()?.finish() }
fun canBio(ctx: Context) = BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
/** Biometrics here is a convenience unlock for an already-enrolled user; the password remains the root credential. */
fun promptBio(act: FragmentActivity, ok: () -> Unit, err: (String) -> Unit) {
    val p = BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { ok() }
        override fun onAuthenticationError(code: Int, msg: CharSequence) { err(msg.toString()) } })
    p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock SmarC").setNegativeButtonText("Use password").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build())
}

@Composable fun PwField(v: String, set: (String) -> Unit, label: String) = OutlinedTextField(v, set, label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
@Composable fun ChipRow(content: @Composable RowScope.() -> Unit) = Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)

@Composable
fun AuthGate(vm: VM, content: @Composable () -> Unit) {
    val s = vm.session
    when {
        s != null && s.mustChange -> ForcedChange(vm, s)
        s != null -> content()
        vm.setupNeeded == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        vm.setupNeeded == true && !vm.onboarded -> Onboarding { vm.finishOnboarding() }
        vm.setupNeeded == true -> SetupWizard(vm)
        else -> LoginScreen(vm)
    }
}

@Composable
fun BizFields(b: BizSettings, set: (BizSettings) -> Unit) {
    OutlinedTextField(b.name, { set(b.copy(name = it)) }, label = { Text("Business name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b.phone, { set(b.copy(phone = it)) }, label = { Text("Contact number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(b.email, { set(b.copy(email = it)) }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Text("Currency", style = MaterialTheme.typography.labelMedium)
    ChipRow { Cur.options.forEach { (c, sym) -> FilterChip(b.currency == c, { set(b.copy(currency = c)) }, { Text("$c $sym") }) } }
    Text("Financial year starts in", style = MaterialTheme.typography.labelMedium)
    ChipRow { java.time.Month.values().forEach { m -> FilterChip(b.fyMonth == m.value, { set(b.copy(fyMonth = m.value)) }, { Text(m.name.take(3).lowercase().replaceFirstChar(Char::uppercase)) }) } }
    Text("Date format", style = MaterialTheme.typography.labelMedium)
    ChipRow { Cur.dateFormats.forEach { f -> FilterChip(b.dateFmt == f, { set(b.copy(dateFmt = f)) }, { Text(f) }) } }
}

@Composable
fun SetupWizard(vm: VM) {
    var step by remember { mutableIntStateOf(1) }; var biz by remember { mutableStateOf(vm.biz.copy(name = "")) }
    var owner by remember { mutableStateOf("") }; var user by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }; var pw2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }; var code by remember { mutableStateOf("") }; var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SmarC", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Text("Smart Credit. Clear Control.")
        Text("Set up your business • step $step of 3", style = MaterialTheme.typography.labelLarge)
        when (step) {
            1 -> { BizFields(biz) { biz = it }
                OutlinedTextField(owner, { owner = it }, label = { Text("Your full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button({ step = 2 }, enabled = biz.name.isNotBlank() && owner.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Next") }
                RestoreButton(vm, "Restore from a backup instead") { } }
            2 -> { OutlinedTextField(user, { user = it }, label = { Text("Choose a username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                PwField(pw, { pw = it }, "Password (8+ characters, letters and numbers)"); PwField(pw2, { pw2 = it }, "Confirm password")
                err?.let { Text(it, color = Danger) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ step = 1 }) { Text("Back") }
                    Button({ if (pw != pw2) err = "Passwords do not match" else vm.launch { vm.accounts.registerOwner(user, owner, pw).onSuccess { code = it; vm.saveBiz(biz.copy(owner = owner.trim())); step = 3; err = null }.onFailure { err = it.message } } },
                        enabled = user.isNotBlank() && pw.isNotEmpty()) { Text("Create account") } } }
            else -> { Text("Save your recovery code", style = MaterialTheme.typography.titleMedium)
                Text(code, style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                Text("If you forget your password, this code is the only way back in (SmarC has no server to email you). Write it down and keep it safe. It will not be shown again.")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(saved, { saved = it }); Text("I have saved my recovery code") }
                Button({ vm.launch { vm.accounts.login(user, pw).onSuccess { vm.applySession(it); vm.setupNeeded = false } } }, enabled = saved, modifier = Modifier.fillMaxWidth()) { Text("Start using SmarC") } }
        }
    }
}

@Composable
fun LoginScreen(vm: VM) {
    val ctx = LocalContext.current; var user by remember { mutableStateOf(vm.accounts.lastName()) }; var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }; var forgot by remember { mutableStateOf(false) }; var needOtp by remember { mutableStateOf(false) }; var otp by remember { mutableStateOf("") }
    val allUsers by vm.users.collectAsState(); val solo = if (!vm.showMulti) allUsers.singleOrNull() else null; val uname = solo?.username ?: user
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SmarC", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary); Text(vm.biz.name)
        val all = vm.registry.all()
        if (all.size > 1) { Text("Business", style = MaterialTheme.typography.labelMedium)
            ChipRow { all.forEach { (id, n) -> FilterChip(id == vm.registry.active(), { if (id != vm.registry.active()) { vm.registry.setActive(id); restartApp(ctx) } }, { Text(n) }) } } }
        if (solo == null) OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        PwField(pw, { pw = it }, "Password")
        err?.let { Text(it, color = Danger) }
        if (needOtp) OutlinedTextField(otp, { otp = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit authenticator code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ vm.launch { vm.accounts.login(uname, pw, otp).onSuccess { vm.applySession(it); pw = ""; otp = ""; needOtp = false }.onFailure { if (it is OtpRequired) { needOtp = true; err = it.message } else { err = it.message; pw = ""; otp = "" } } } },
            enabled = uname.isNotBlank() && pw.isNotEmpty() && (!needOtp || otp.length == 6), modifier = Modifier.fillMaxWidth()) { Text("Log in") }
        val bioUser = vm.lastBioUser
        if (bioUser != null && canBio(ctx)) OutlinedButton({ (ctx.findActivity() as? FragmentActivity)?.let { a -> promptBio(a, { vm.launch { vm.accounts.loginBio(bioUser).onSuccess { vm.applySession(it) } } }, { err = it }) } }, modifier = Modifier.fillMaxWidth()) { Text("Unlock with fingerprint / face") }
        TextButton({ forgot = true }) { Text("Forgot password?") }
    }
    if (forgot) ForgotDialog(vm, uname) { forgot = false }
}

@Composable
fun ForgotDialog(vm: VM, user0: String, close: () -> Unit) {
    var user by remember { mutableStateOf(user0) }; var code by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }; var newCode by remember { mutableStateOf<String?>(null) }
    AlertDialog(close, title = { Text(if (newCode == null) "Reset password" else "Password changed") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (newCode == null) { OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true)
            OutlinedTextField(code, { code = it }, label = { Text("Recovery code") }, singleLine = true); PwField(pw, { pw = it }, "New password"); msg?.let { Text(it, color = Danger) } }
        else { Text("Your NEW recovery code (the old one no longer works). Save it now:"); Text(newCode!!, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) } } },
        confirmButton = { if (newCode == null) TextButton({ vm.launch { vm.accounts.resetWithRecovery(user, code, pw).onSuccess { newCode = it }.onFailure { msg = it.message } } }, enabled = user.isNotBlank() && code.isNotBlank() && pw.isNotEmpty()) { Text("Reset") } else TextButton(close) { Text("Done") } },
        dismissButton = { if (newCode == null) TextButton(close) { Text("Cancel") } })
}

@Composable
fun ChangePasswordDialog(vm: VM, s: Session, done: () -> Unit) {
    var old by remember { mutableStateOf("") }; var n1 by remember { mutableStateOf("") }; var n2 by remember { mutableStateOf("") }; var msg by remember { mutableStateOf<String?>(null) }
    AlertDialog({ if (!s.mustChange) done() }, title = { Text("Change password") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PwField(old, { old = it }, "Current password"); PwField(n1, { n1 = it }, "New password"); PwField(n2, { n2 = it }, "Confirm new password"); msg?.let { Text(it, color = Danger) } } },
        confirmButton = { TextButton({ if (n1 != n2) msg = "Passwords do not match" else vm.launch { vm.accounts.changePassword(s.userId, old, n1).onSuccess { vm.session = s.copy(mustChange = false); done() }.onFailure { msg = it.message } } }, enabled = old.isNotEmpty() && n1.isNotEmpty()) { Text("Save") } },
        dismissButton = { if (!s.mustChange) TextButton(done) { Text("Cancel") } else TextButton({ vm.logout() }) { Text("Log out") } })
}

@Composable fun ForcedChange(vm: VM, s: Session) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Set a new password to continue"); ChangePasswordDialog(vm, s) { } }

@Composable
fun AccountMenu(vm: VM) {
    var open by remember { mutableStateOf(false) }; var pw by remember { mutableStateOf(false) }
    vm.session?.let { s ->
        Box { IconButton({ open = true }) { Icon(Icons.Default.AccountCircle, "Account menu") }
            DropdownMenu(open, { open = false }) {
                Text("${s.name} (${s.role.label})", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                DropdownMenuItem({ Text("Change password") }, { open = false; pw = true }); DropdownMenuItem({ Text("Log out") }, { open = false; vm.logout() }) } }
        if (pw) ChangePasswordDialog(vm, s) { pw = false }
    }
}

@Composable
fun PasswordDialog(title: String, confirm: Boolean, close: () -> Unit, ok: (String) -> Unit) {
    var a by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }; val pol = if (confirm) Passwords.policy(a) else null
    AlertDialog(close, title = { Text(title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { PwField(a, { a = it }, "Backup password")
        if (confirm) { PwField(b, { b = it }, "Confirm password"); Text(pol ?: "Keep this password safe: without it the backup cannot be opened.", style = MaterialTheme.typography.bodySmall) } } },
        confirmButton = { TextButton({ ok(a) }, enabled = a.isNotEmpty() && (!confirm || (pol == null && a == b))) { Text("Continue") } }, dismissButton = { TextButton(close) { Text("Cancel") } })
}

@Composable
fun RestoreButton(vm: VM, label: String, done: () -> Unit) {
    val ctx = LocalContext.current; var blob by remember { mutableStateOf<ByteArray?>(null) }; var msg by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { blob = ctx.contentResolver.openInputStream(it)?.use { s -> s.readBytes() } } }
    OutlinedButton({ picker.launch(arrayOf("*/*")) }, enabled = vm.session == null || vm.can(Action.BACKUP), modifier = Modifier.fillMaxWidth()) { Text(label) }
    msg?.let { Text(it, color = Danger) }
    blob?.let { b -> PasswordDialog("Backup password", false, { blob = null }) { pw -> blob = null; vm.launch { vm.restore(b, pw).onSuccess { done() }.onFailure { msg = it.message } } } }
}

@Composable
fun SecurityScreen(vm: VM, startPage: String = "main", back: () -> Unit) {
    var page by remember { mutableStateOf(startPage) }
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Security & settings", style = MaterialTheme.typography.titleLarge); TextButton({ if (page == "main") back() else page = "main" }) { Text("Back") } }
        when (page) { "users" -> UsersPage(vm); "backup" -> BackupPage(vm); "archived" -> ArchivedPage(vm); else -> SecurityMain(vm) { page = it } }
    }
}

@Composable
fun SecurityMain(vm: VM, go: (String) -> Unit) {
    val ctx = LocalContext.current; var biz by remember { mutableStateOf(false) }; var add by remember { mutableStateOf(false) }; var msg by remember { mutableStateOf<String?>(null) }
    val s = vm.session ?: return
    Text("Signed in as ${s.name} (${s.role.label})")
    if (vm.can(Action.SETTINGS)) OutlinedButton({ biz = true }, Modifier.fillMaxWidth()) { Text("Business settings") }
    if (vm.can(Action.MANAGE_USERS) && vm.showMulti) OutlinedButton({ go("users") }, Modifier.fillMaxWidth()) { Text("Staff & permissions") }
    if (vm.can(Action.BACKUP)) OutlinedButton({ go("backup") }, Modifier.fillMaxWidth()) { Text("Backup & restore") }
    if (vm.can(Action.ERASE_DATA)) OutlinedButton({ go("archived") }, Modifier.fillMaxWidth()) { Text("Archived customers & data erasure") }
    if (canBio(ctx)) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(vm.bioOn, { on -> if (on) (ctx.findActivity() as? FragmentActivity)?.let { a -> promptBio(a, { vm.launch { vm.accounts.setBio(s.userId, true); vm.bioOn = true } }, { msg = it }) } else vm.launch { vm.accounts.setBio(s.userId, false); vm.bioOn = false } })
        Text("Unlock with fingerprint / face") }
    TwoFactorSection(vm, s)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Switch(vm.multiUser, { vm.setMulti(it) }); Text("Advanced: staff users and several businesses") }
    Text("Log out after leaving the app for", style = MaterialTheme.typography.labelMedium)
    ChipRow { listOf(1, 5, 15, 30).forEach { FilterChip(vm.timeoutMin == it, { vm.setTimeout(it) }, { Text("$it min") }) } }
    if (vm.showMulti) Text("Businesses (each one has its own encrypted database)", style = MaterialTheme.typography.labelMedium)
    if (vm.showMulti) ChipRow { vm.registry.all().forEach { (id, n) -> FilterChip(id == vm.registry.active(), { if (id != vm.registry.active()) { vm.logout(); vm.registry.setActive(id); restartApp(ctx) } }, { Text(n) }) } }
    if (s.role == Role.OWNER && vm.showMulti) OutlinedButton({ add = true }, Modifier.fillMaxWidth()) { Text("Add another business") }
    if (s.role == Role.OWNER) DeleteBusinessSection(vm)
    msg?.let { Text(it, color = Danger) }
    if (vm.showMulti) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("What each role can do", style = MaterialTheme.typography.titleSmall)
        Text("Owner: everything, including backup/restore and data erasure.", style = MaterialTheme.typography.bodySmall)
        Text("Administrator: everything except backup/restore and erasure; manages collectors.", style = MaterialTheme.typography.bodySmall)
        Text("Collector: add customers, record payments, follow-ups. No credit, reversals, write-offs, reports or settings.", style = MaterialTheme.typography.bodySmall) } }
    if (biz) { var draft by remember { mutableStateOf(vm.biz) }
        AlertDialog({ biz = false }, title = { Text("Business settings") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) { BizFields(draft) { draft = it }; BizAdvanced(draft) { draft = it }; LogoButtons(vm) } },
            confirmButton = { TextButton({ vm.saveBiz(draft); biz = false }, enabled = draft.name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton({ biz = false }) { Text("Cancel") } }) }
    if (add) { var n by remember { mutableStateOf("") }
        AlertDialog({ add = false }, title = { Text("Add another business") }, text = { Column { OutlinedTextField(n, { n = it }, label = { Text("Business name") }, singleLine = true); Text("You will create a separate owner account for it.", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { TextButton({ val id = vm.registry.add(n.trim()); vm.logout(); vm.registry.setActive(id); restartApp(ctx) }, enabled = n.isNotBlank()) { Text("Create") } }, dismissButton = { TextButton({ add = false }) { Text("Cancel") } }) }
}

@Composable
fun UsersPage(vm: VM) {
    val users by vm.users.collectAsState(); val me = vm.session ?: return
    var add by remember { mutableStateOf(false) }; var reset by remember { mutableStateOf<User?>(null) }; var info by remember { mutableStateOf<String?>(null) }; var msg by remember { mutableStateOf<String?>(null) }
    Button({ add = true }, Modifier.fillMaxWidth()) { Text("Add staff user") }
    msg?.let { Text(it) }
    users.forEach { u -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${u.displayName} (@${u.username})", style = MaterialTheme.typography.titleSmall); Text("${Role.valueOf(u.role).label}${if (!u.active) " • disabled" else ""}${if (u.mustChange) " • must change password" else ""}", style = MaterialTheme.typography.bodySmall)
        if (u.id != me.userId) ChipRow {
            TextButton({ vm.launch { msg = vm.accounts.setActive(me, u, !u.active).text() } }) { Text(if (u.active) "Disable" else "Enable") }
            TextButton({ reset = u }) { Text("Reset password") }
            if (me.role == Role.OWNER) Role.values().forEach { r -> FilterChip(u.role == r.name, { vm.launch { msg = vm.accounts.setRole(me, u, r).text() } }, { Text(r.label) }) } } } } }
    if (add) AddUserDialog(me, vm, { add = false }) { info = it; add = false }
    reset?.let { u -> var pw by remember { mutableStateOf("") }
        AlertDialog({ reset = null }, title = { Text("Temporary password for ${u.displayName}") }, text = { PwField(pw, { pw = it }, "Temporary password") },
            confirmButton = { TextButton({ vm.launch { msg = vm.accounts.adminReset(me, u, pw).text(); reset = null } }, enabled = pw.isNotEmpty()) { Text("Set") } }, dismissButton = { TextButton({ reset = null }) { Text("Cancel") } }) }
    info?.let { c -> AlertDialog({ info = null }, title = { Text("Recovery code") }, text = { Column { Text("Give this to the new user once. They can reset their password with it.")
        Text(c, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium) } }, confirmButton = { TextButton({ info = null }) { Text("Done") } }) }
}

@Composable
fun AddUserDialog(me: Session, vm: VM, close: () -> Unit, created: (String) -> Unit) {
    var un by remember { mutableStateOf("") }; var nm by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.COLLECTOR) }; var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(close, title = { Text("Add staff user") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(nm, { nm = it }, label = { Text("Full name") }, singleLine = true); OutlinedTextField(un, { un = it }, label = { Text("Username") }, singleLine = true)
        PwField(pw, { pw = it }, "Temporary password (they must change it)")
        ChipRow { Role.values().filter { me.role == Role.OWNER || it == Role.COLLECTOR }.forEach { FilterChip(role == it, { role = it }, { Text(it.label) }) } }
        err?.let { Text(it, color = Danger) } } },
        confirmButton = { TextButton({ vm.launch { vm.accounts.addUser(me, un, nm, role, pw).onSuccess { created(it) }.onFailure { err = it.message } } }, enabled = un.isNotBlank() && nm.isNotBlank() && pw.isNotEmpty()) { Text("Create") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

@Composable
fun BackupPage(vm: VM) {
    val ctx = LocalContext.current; var ask by remember { mutableStateOf(false) }; var pending by remember { mutableStateOf<ByteArray?>(null) }; var msg by remember { mutableStateOf<String?>(null) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val b = pending; pending = null
        if (uri != null && b != null) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(b) }; msg = "Backup saved"; vm.markBackup(); vm.launch { vm.accounts.audit("BACKUP", "encrypted backup created", vm.session?.username ?: "?") } } }
    Text("Backups are encrypted with a password you choose. Store the file somewhere safe (another phone, cloud drive, computer). SmarC cannot recover a forgotten backup password.", style = MaterialTheme.typography.bodySmall)
    Text(vm.lastBackup.collectAsState().value.let { if (it == 0L) "No backup made yet" else "Last backup: ${it.dmy()}" }, style = MaterialTheme.typography.titleSmall)
    Button({ ask = true }, Modifier.fillMaxWidth()) { Text("Create encrypted backup") }
    RestoreButton(vm, "Restore from backup (replaces ALL current data)") { msg = "Restored. Please log in again." }
    msg?.let { Text(it) }
    if (ask) PasswordDialog("Backup password", true, { ask = false }) { pw -> ask = false; vm.launch { pending = vm.backup.export(vm.biz, pw); saver.launch("smarc-backup-${LocalDate.now()}.smarcbak") } }
}

@Composable
fun ArchivedPage(vm: VM) {
    val list by vm.archived.collectAsState(); val me = vm.session ?: return
    var target by remember { mutableStateOf<Customer?>(null) }; var msg by remember { mutableStateOf<String?>(null) }
    Text("Archived customers have no balance. Erasing removes their name, contact details, notes and follow-up notes but keeps the financial ledger so your books still add up. This cannot be undone.", style = MaterialTheme.typography.bodySmall)
    msg?.let { Text(it) }
    if (list.isEmpty()) Text("No archived customers.")
    list.forEach { c -> ListItem(headlineContent = { Text(c.name) }, supportingContent = { Text(c.phone.ifBlank { "no contact details" }) }, trailingContent = { TextButton({ target = c }) { Text("Erase") } }) }
    target?.let { c -> AlertDialog({ target = null }, title = { Text("Erase personal details?") }, text = { Text("${c.name} will be replaced by an anonymous label. This cannot be undone.") },
        confirmButton = { TextButton({ vm.launch { msg = vm.accounts.eraseCustomer(me, c).text(); target = null } }) { Text("Erase") } }, dismissButton = { TextButton({ target = null }) { Text("Cancel") } }) }
}
