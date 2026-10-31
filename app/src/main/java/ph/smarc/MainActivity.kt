package ph.smarc

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import android.view.WindowManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

val Navy = Color(0xFF0B2545); val Emerald = Color(0xFF10B981); val Sky = Color(0xFF38BDF8); val Danger = Color(0xFFDC2626)
private val day = 86_400_000L
private fun Long.date() = Cur.fmt(Cur.datePattern, this)

class VM(app: Application) : AndroidViewModel(app) {
    val registry = BizRegistry(app)
    private val db = Dbs.open(app, registry.active())
    val dao = db.dao(); val svc = LedgerService(db)
    init { Export.onExport = { n -> viewModelScope.launch { dao.insertAudit(AuditLog(action = "EXPORT", detail = n, actor = session?.username ?: "?")) } } }
    val audit = dao.auditLog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = dao.summaries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totals = dao.totals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Totals(0, 0, 0, 0, 0, 0))
    val cashSvc = CashService(db)
    val cash = dao.cashTotals().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashTotals(0, 0))
    val cashEntries = dao.cashEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val prefs = app.getSharedPreferences("smarc_prefs", android.content.Context.MODE_PRIVATE)
    private val authPrefs = app.getSharedPreferences("smarc_auth_${registry.active()}", android.content.Context.MODE_PRIVATE)
    val accounts = Accounts(db, authPrefs) { id, eids -> Photos.delete(app, "c_$id"); eids.forEach { Attachments.deleteAll(app, it) } }
    val backup = BackupService(db, app.filesDir)
    var biz by mutableStateOf(BizStore.load(prefs, registry.active()))
    val business: String get() = biz.name
    init { Cur.apply(biz) }
    fun saveBiz(s: BizSettings) { BizStore.save(prefs, registry.active(), s); registry.rename(registry.active(), s.name); biz = s; Cur.apply(s) }
    var session by mutableStateOf<Session?>(null)
    var setupNeeded by mutableStateOf<Boolean?>(null)
    var bioOn by mutableStateOf(false)
    var lastBioUser by mutableStateOf<String?>(null)
    var leftAt = 0L
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
    fun finishOnboarding() { onboarded = true; prefs.edit().putBoolean("onboarded", true).apply() }
    var photoVersion by mutableIntStateOf(0)
    var multiUser by mutableStateOf(prefs.getBoolean("multi", false))
    fun setMulti(on: Boolean) { multiUser = on; prefs.edit().putBoolean("multi", on).apply() }
    /** Staff accounts / several businesses stay hidden for a single owner unless switched on (or already in use). */
    val showMulti: Boolean get() = multiUser || users.value.size > 1 || registry.all().size > 1
    val recons = dao.recons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var timeoutMin by mutableIntStateOf(authPrefs.getInt("timeout", 5))
    val users = db.users().all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val archived = dao.archivedCustomers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init { viewModelScope.launch { setupNeeded = accounts.needsSetup(); lastBioUser = accounts.lastBioUserId() } }
    fun can(a: Action) = Perm.can(session?.role, a)
    fun applySession(u: User?) {
        session = u?.let { Session(it.id, it.username, it.displayName, Role.valueOf(it.role), it.mustChange) }; bioOn = u?.bio ?: false
        val n = session?.username ?: "?"; svc.actor = n; svc.role = session?.role; cashSvc.actor = n; cashSvc.role = session?.role
        session?.let { s -> if (s.role == Role.OWNER && biz.retentionMonths > 0) viewModelScope.launch { accounts.autoRetention(s, biz.retentionMonths, System.currentTimeMillis()) } }
    }
    fun logout() { val s = session; applySession(null); viewModelScope.launch { s?.let { accounts.audit("LOGOUT", "", it.username) }; lastBioUser = accounts.lastBioUserId() } }
    fun checkTimeout() { if (session != null && SessionPolicy.expired(leftAt, System.currentTimeMillis(), timeoutMin)) logout() }
    fun setTimeout(m: Int) { timeoutMin = m; authPrefs.edit().putInt("timeout", m).apply() }
    suspend fun deleteBusiness(pw: String): Result<String> {
        val s = session ?: return Result.failure(SecurityException("Not signed in"))
        if (s.role != Role.OWNER) return Result.failure(SecurityException("Only the owner can delete a business"))
        accounts.verifyPassword(s.userId, pw).onFailure { return Result.failure(it) }
        val id = registry.active(); val app = getApplication<Application>()
        dao.dumpCustomers().forEach { Photos.delete(app, "c_${it.id}") }
        dao.dumpLedger().forEach { Attachments.deleteAll(app, it.id) }
        session = null; db.close(); app.deleteDatabase("smarc_$id.db")
        authPrefs.edit().clear().apply(); prefs.edit().remove("bs_$id").apply(); Photos.delete(app, "logo_$id"); KeyVault.forget(app, id); registry.remove(id)
        return Result.success("Deleted")
    }
    /** Allowed for the owner when signed in, or during first-time setup when no account exists yet. */
    suspend fun restore(blob: ByteArray, pw: String): Result<String> {
        if (session != null && !can(Action.BACKUP)) return Result.failure(SecurityException("Only the owner can restore a backup"))
        return backup.restore(blob, pw, biz, session?.username ?: "setup").map { saveBiz(it); applySession(null); setupNeeded = false; lastBioUser = accounts.lastBioUserId(); "Restored" }
    }
    val allEntries = dao.allEntries().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cfg = MutableStateFlow(AlertCfg(prefs.getInt("soon", 7), prefs.getInt("lead", 1), prefs.getInt("limit", 90), prefs.getBoolean("push", false), prefs.getInt("backup_days", 7)))
    val lastBackup = MutableStateFlow(prefs.getLong("last_backup", 0L))
    fun markBackup() { val t = System.currentTimeMillis(); lastBackup.value = t; prefs.edit().putLong("last_backup", t).apply() }
    fun saveCfg(c: AlertCfg) { cfg.value = c; prefs.edit().putInt("soon", c.soonDays).putInt("lead", c.leadDays).putInt("limit", c.limitPct).putBoolean("push", c.push).putInt("backup_days", c.backupDays).apply(); Notify.schedule(getApplication(), c.push) }
    val readKeys = MutableStateFlow(prefs.getStringSet("read", emptySet())!!.toSet())
    fun markRead(keys: Collection<String>, current: List<Alert>) {   // prune to keys that still exist so the set cannot grow forever
        readKeys.value = (readKeys.value + keys).intersect(current.map { it.key }.toSet()); prefs.edit().putStringSet("read", readKeys.value).apply() }
    val alerts = combine(dao.allEntries(), dao.customers(), dao.followUps(), cfg, lastBackup) { e, c, f, g, lb -> Alerts.build(e, c, f, System.currentTimeMillis(), Cur.zone, g, lb) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val aging = combine(dao.allEntries(), dao.customers(), dao.followUps(), cfg) { e, c, f, g -> Aging.build(e, c, f, System.currentTimeMillis(), soonDays = g.soonDays) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgingState())
    override fun onCleared() { db.close(); super.onCleared() }
    fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}

class MainActivity : FragmentActivity() {
    private val vm by viewModels<VM>()
    override fun onStop() { vm.leftAt = System.currentTimeMillis(); super.onStop() }
    override fun onStart() { super.onStart(); vm.checkTimeout() }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen(); super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE) // hide balances in recents/screenshots
        setContent {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Sky, secondary = Emerald, tertiary = Sky, background = Color(0xFF0B1220), surface = Color(0xFF111A2E))
                else lightColorScheme(primary = Navy, secondary = Emerald, tertiary = Sky)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { AuthGate(vm) { App(vm) } } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: VM) {
    var tab by remember { mutableIntStateOf(if (vm.can(Action.VIEW_FINANCIALS)) 0 else 1) }
    var selected by remember { mutableStateOf<String?>(null) }
    var addCustomer by remember { mutableStateOf(false) }
    val customers by vm.customers.collectAsState()
    val agg by vm.aging.collectAsState()
    val alerts by vm.alerts.collectAsState(); val readKeys by vm.readKeys.collectAsState(); val unread = alerts.count { it.key !in readKeys }
    val current = customers.firstOrNull { it.c.id == selected }
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840   // tablets: side rail + list/detail panes
    Scaffold(
        topBar = { TopAppBar(title = { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Avatar("logo_${vm.registry.active()}", 32.dp, vm.photoVersion, null, null); Spacer(Modifier.width(8.dp))
            Column { Text(vm.biz.name.ifBlank { "SmarC" }, color = Color.White); Text("Smart Credit. Clear Control.", style = MaterialTheme.typography.labelSmall, color = Sky) } } },
            actions = { AccountMenu(vm); IconButton({ tab = 6; selected = null }) { BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) { Icon(Icons.Default.Notifications, "Notifications, $unread unread") } } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy, actionIconContentColor = Color.White)) },
        bottomBar = { if (!wide) NavigationBar {
            if (vm.can(Action.VIEW_FINANCIALS)) NavigationBarItem(tab == 0 && current == null, { tab = 0; selected = null }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationBarItem(tab == 1 || current != null, { tab = 1; selected = null }, { Icon(Icons.Default.People, null) }, label = { Text("Customers") })
            NavigationBarItem(tab == 3 && current == null, { tab = 3; selected = null }, { Icon(Icons.Default.Alarm, null) }, label = { Text("Collect") })
            if (vm.can(Action.SETTINGS)) NavigationBarItem(tab == 2 && current == null, { tab = 2; selected = null }, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
        } },
        floatingActionButton = { if (current == null && tab < 2) FloatingActionButton({ addCustomer = true }, containerColor = Emerald) { Icon(Icons.Default.PersonAdd, "Add customer") } }
    ) { pad ->
        Row(Modifier.padding(pad).fillMaxSize()) {
        if (wide) NavigationRail {
            if (vm.can(Action.VIEW_FINANCIALS)) NavigationRailItem(tab == 0 && current == null, { tab = 0; selected = null }, { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationRailItem(tab == 1 || current != null, { tab = 1; selected = null }, { Icon(Icons.Default.People, null) }, label = { Text("Customers") })
            NavigationRailItem(tab == 3 && current == null, { tab = 3; selected = null }, { Icon(Icons.Default.Alarm, null) }, label = { Text("Collect") })
            if (vm.can(Action.SETTINGS)) NavigationRailItem(tab == 2 && current == null, { tab = 2; selected = null }, { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
        }
        Box(Modifier.weight(1f).fillMaxHeight().fillMaxWidth().wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max = if (wide) 1400.dp else 720.dp).fillMaxWidth().padding(16.dp)) {
            when {
                wide && tab == 1 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.width(380.dp).fillMaxHeight()) { CustomerList(customers, agg.rows.associateBy { it.c.id }) { selected = it } }
                    Box(Modifier.weight(1f).fillMaxHeight()) { if (current != null) CustomerScreen(vm, current) else Text("Select a customer to see their account.") } }
                current != null -> CustomerScreen(vm, current)
                tab == 0 -> Dashboard(vm, customers) { tab = 10 }
                tab == 2 -> MoreScreen(vm, { tab = 4 }, { tab = 5 }, { tab = 8 }, { tab = 9 })
                tab == 5 -> AuditScreen(vm) { tab = 2 }
                tab == 8 -> SecurityScreen(vm) { tab = 2 }
                tab == 10 -> SecurityScreen(vm, "backup") { tab = 0 }
                tab == 9 -> InsightsScreen(vm) { tab = 2 }
                tab == 4 -> ReportsScreen(vm) { tab = 2 }
                tab == 3 -> CollectionsScreen(vm, { tab = 7 }) { selected = it }
                tab == 6 -> NotificationsScreen(vm) { if (it.isEmpty()) { if (vm.can(Action.BACKUP)) tab = 10 } else selected = it }
                tab == 7 -> CalendarScreen(vm) { tab = 3 }
                else -> CustomerList(customers, agg.rows.associateBy { it.c.id }) { selected = it }
            }
        }
        }
    }
    if (addCustomer) CustomerDialog(null, customers.map { it.c }, { addCustomer = false }) { c -> vm.launch { vm.svc.addCustomer(c) }; addCustomer = false }
}

@Composable
fun CustomerList(list: List<CustomerSummary>, rows: Map<String, CollectRow>, open: (String) -> Unit) {
    var q by remember { mutableStateOf("") }; var flt by remember { mutableIntStateOf(0) }; var sort by remember { mutableIntStateOf(0) }
    Column {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Search name, phone or account no.") }, singleLine = true)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("All", "Overdue", "Owing", "Paid").forEachIndexed { i, t -> FilterChip(flt == i, { flt = i }, { Text(t) }) }
            listOf("A-Z", "Balance", "Due date").forEachIndexed { i, t -> FilterChip(sort == i, { sort = i }, { Text("Sort: $t") }) } }
        if (list.isEmpty()) Text("No customers yet. Tap + to add your first one.", Modifier.padding(top = 24.dp))
        LazyColumn { items(CustomerView.apply(list, rows, q, flt, sort)) { s ->
            val r = rows[s.c.id]; val od = (r?.daysLate ?: 0) > 0
            ListItem(headlineContent = { Text(s.c.name) },
                supportingContent = { Text(if (od) "Overdue ${r!!.daysLate} days" else r?.nextDue?.let { "Next due ${it.date()}" } ?: s.c.phone) },
                trailingContent = { Text(s.balance.peso(), color = if (od) Danger else if (s.balance > 0) MaterialTheme.colorScheme.primary else Emerald) },
                modifier = Modifier.clickable2 { open(s.c.id) })
            HorizontalDivider() } }
    }
}

private fun Modifier.clickable2(f: () -> Unit) = this.then(Modifier.clickable(onClick = f))

@Composable
fun CustomerScreen(vm: VM, s: CustomerSummary) {
    val entries by remember(s.c.id) { vm.dao.entries(s.c.id) }.collectAsState(emptyList())
    var dialog by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var receipt by remember { mutableStateOf<String?>(null) }; var detail by remember { mutableStateOf<Entry?>(null) }; var edit by remember { mutableStateOf(false) }; var wo by remember { mutableStateOf(false) }; var menu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { if (Photos.save(ctx, "c_${s.c.id}", it)) vm.photoVersion++ } }
    fun handle(r: Result<String>, isReceipt: Boolean = false) { r.onSuccess { if (isReceipt) receipt = it else msg = it }.onFailure { msg = it.message } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileCard(s.c, entries, vm.photoVersion, if (vm.can(Action.EDIT_CUSTOMER)) ({ pick.launch("image/*") }) else null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.can(Action.CREDIT)) Button({ dialog = "credit" }) { Text("Record credit") }
            Button({ dialog = "payment" }, colors = ButtonDefaults.buttonColors(containerColor = Emerald)) { Text("Record payment") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton({ Export.statement(ctx, vm.business, s.c, entries) }) { Text("Statement (PDF)") }
            if (vm.can(Action.EDIT_CUSTOMER)) OutlinedButton({ edit = true }) { Text("Edit") }
            if (vm.can(Action.ADJUST)) Box { OutlinedButton({ menu = true }) { Text("More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Opening balance") }, { menu = false; dialog = "opening" })
                    DropdownMenuItem({ Text("Add fee / interest") }, { menu = false; dialog = "fee" })
                    DropdownMenuItem({ Text("Adjust balance") }, { menu = false; dialog = "adjust" })
                    DropdownMenuItem({ Text("Refund cash to customer") }, { menu = false; dialog = "refund" }) } }
            if (s.balance > 0 && vm.can(Action.WRITE_OFF)) OutlinedButton({ wo = true }) { Text("Write off") } }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        LazyColumn { items(entries) { e ->
            ListItem(headlineContent = { Text("${e.type}  ${(e.principalDelta + e.feeDelta).peso()}") },
                supportingContent = { Text("${e.createdAt.date()}${if (e.receiptNo > 0) " • " + e.receiptNo.orNo() else ""}${if (e.reference.isNotBlank()) " • ref " + e.reference else ""}${if (e.tax != 0L) " • incl. tax " + e.tax.peso() else ""}${if (e.items.isNotBlank()) " • itemized" else ""} ${e.note}") },
                trailingContent = { if (e.type != "REVERSAL" && e.type != "WRITE_OFF" && vm.can(Action.REVERSE)) TextButton({ vm.launch { handle(if (e.planId != null && e.type == "CREDIT") vm.svc.reversePlan(e.planId, "Corrected by owner") else vm.svc.reverse(e.id, "Corrected by owner")) } }) { Text(if (e.planId != null && e.type == "CREDIT") "Reverse plan" else "Reverse") } },
                modifier = Modifier.clickable2 { detail = e })
            HorizontalDivider() } }
    }
    detail?.let { en -> EntryDetail(en, s.c.name, entries, { detail = null }) { receipt = it } }
    if (edit) CustomerDialog(s.c, vm.customers.collectAsState().value.map { it.c }, { edit = false }) { c -> edit = false; vm.launch { handle(vm.svc.updateCustomer(c)) } }
    if (wo) ReasonDialog("Write off ${s.balance.peso()}", { wo = false }) { r -> wo = false; vm.launch { handle(vm.svc.writeOff(s.c, r)) } }
    dialog?.let { kind ->
        val key = remember { UUID.randomUUID().toString() }   // one key per dialog = double-tap safe
        if (kind == "credit") CreditDialog(Items.known(vm.allEntries.value), { dialog = null }) { p, f, first, n, freq, note, inv, tp, its -> dialog = null; vm.launch { handle(vm.svc.creditPlan(s.c, p, f, first, n, freq, note, key, inv, tp, its)) } }
        else if (kind == "payment") PaymentDialog({ dialog = null }) { amt, method, ref, note -> dialog = null; vm.launch { handle(vm.svc.payment(s.c, amt, method, ref, note, key), true) } }
        else EntryDialog(kind, { dialog = null }) { p, f, dueDays, up, reason -> dialog = null
            vm.launch { if (kind == "refund") handle(vm.svc.refund(s.c, p, reason, key), true) else handle(vm.svc.otherEntry(s.c, kind, p, f, dueDays?.let { System.currentTimeMillis() + it * day }, up, reason, key)) } }
    }
    receipt?.let { r -> AlertDialog({ receipt = null }, title = { Text("Payment receipt") }, text = { Text(r) },
        confirmButton = { TextButton({ ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, r), "Share receipt")) }) { Text("Share") } },
        dismissButton = { Row { TextButton({ Export.receipt(ctx, vm.business, s.c.name, r) }) { Text("PDF") }; TextButton({ receipt = null }) { Text("Close") } } }) }
}

@Composable
fun AddCustomerDialog(close: () -> Unit, save: (Customer) -> Unit) {
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var limit by remember { mutableStateOf("") }
    AlertDialog(close, title = { Text("Add customer") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, label = { Text("Mobile number") }, singleLine = true)
        OutlinedTextField(limit, { limit = it }, label = { Text("Credit limit (${Cur.symbol}, 0 = none)") }, singleLine = true) } },
        confirmButton = { TextButton({ save(Customer(name = name.trim(), phone = phone.trim(), creditLimit = parsePeso(limit.ifBlank { "0" }) ?: 0)) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

@Composable
fun MoreScreen(vm: VM, openReports: () -> Unit, openAudit: () -> Unit, openSecurity: () -> Unit, openInsights: () -> Unit) {
    val t by vm.totals.collectAsState(); val c by vm.cash.collectAsState(); val list by vm.cashEntries.collectAsState()
    var dialog by remember { mutableStateOf<String?>(null) }; var msg by remember { mutableStateOf<String?>(null) }
    val cash = Fin.cashOnHand(c.capital, t.collected, t.lent, c.expenses)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("Cash on hand", cash.peso(), if (cash >= 0) Emerald else Danger, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Net capital added", c.capital.peso(), mod = Modifier.weight(1f)); Stat("Tied up in receivables", t.principal.peso(), mod = Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button({ dialog = "CAPITAL_IN" }) { Text("+ Capital") }
            OutlinedButton({ dialog = "CAPITAL_OUT" }) { Text("Withdraw") }
            Button({ dialog = "EXPENSE" }, colors = ButtonDefaults.buttonColors(containerColor = Danger)) { Text("Expense") } }
        if (vm.can(Action.VIEW_FINANCIALS)) OutlinedButton(openInsights) { Text("Insights & cash reconciliation") }
        if (vm.can(Action.AUDIT_VIEW)) OutlinedButton(openAudit) { Text("Audit log") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { if (vm.can(Action.REPORTS_EXPORT)) Button(openReports) { Text("Reports & exports") }; OutlinedButton(openSecurity) { Text("Security & settings") } }
        msg?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        LazyColumn { items(list) { e ->
            ListItem(headlineContent = { Text("${e.kind}  ${kotlin.math.abs(e.cashDelta).peso()}") },
                supportingContent = { Text("${e.createdAt.date()} ${e.category} ${e.note}") },
                trailingContent = { if (e.reversalOf == null) TextButton({ vm.launch { vm.cashSvc.void(e.id, "Voided by owner").let { r -> msg = r.getOrElse { it.message } } } }) { Text("Void") } })
            HorizontalDivider() } }
    }
    dialog?.let { kind ->
        val key = remember { UUID.randomUUID().toString() }
        CashDialog(kind, { dialog = null }) { amt, cat, note -> dialog = null; vm.launch { msg = vm.cashSvc.add(kind, amt, cat, note, key).getOrElse { it.message } } }
    }
}

@Composable
fun CashDialog(kind: String, close: () -> Unit, save: (Long, String, String) -> Unit) {
    var amt by remember { mutableStateOf("") }; var cat by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    val a = parsePeso(amt)
    AlertDialog(close, title = { Text(when (kind) { "CAPITAL_IN" -> "Add capital"; "CAPITAL_OUT" -> "Withdraw capital"; else -> "Record expense" }) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(amt, { amt = it }, label = { Text("Amount (${Cur.symbol})") }, singleLine = true, isError = amt.isNotBlank() && a == null)
            if (kind == "EXPENSE") OutlinedTextField(cat, { cat = it }, label = { Text("Category (e.g. transport, fees)") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") }) } },
        confirmButton = { TextButton({ save(a!!, cat, note) }, enabled = a != null && a > 0 && (kind != "EXPENSE" || cat.isNotBlank())) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

