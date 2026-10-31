package ph.smarc

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class AlertCfg(val soonDays: Int = 7, val leadDays: Int = 1, val limitPct: Int = 90, val push: Boolean = false, val backupDays: Int = 7)
enum class AlertKind(val label: String) { DUE("Payment due"), MISSED("Missed payment"), PROMISE("Promise"), LIMIT("Credit limit"), BACKUP("Backup") }
data class Alert(val key: String, val kind: AlertKind, val customerId: String, val title: String, val detail: String, val urgent: Boolean)

/** Alerts are derived from ledger data every time (nothing sensitive is stored); only read/unread keys are remembered. */
object Alerts {
    fun build(entries: List<Entry>, customers: List<Customer>, follow: List<FollowUp>, now: Long, z: ZoneId, cfg: AlertCfg, lastBackup: Long? = null): List<Alert> {
        val cm = customers.associateBy { it.id }; val open = Aging.open(entries).filter { it.due != null && cm.containsKey(it.customerId) }
        val out = mutableListOf<Alert>()
        open.forEach { i -> val d = Aging.daysLate(i.due!!, now, z); val c = cm[i.customerId]!!
            if (d <= 0 && -d <= cfg.leadDays) out += Alert("DUE:${i.id}", AlertKind.DUE, c.id,
                when { d == 0L -> "Due today"; -d == 1L -> "Due tomorrow"; else -> "Due in ${-d} days" }, "${c.name} • ${i.amount.peso()}", d == 0L) }
        open.filter { Aging.daysLate(it.due!!, now, z) > 0 }.groupBy { it.customerId }.forEach { (cid, items) ->
            val oldest = items.minBy { it.due!! }
            out += Alert("MISSED:$cid:${oldest.id}", AlertKind.MISSED, cid, "Missed payment",
                "${cm[cid]!!.name} • ${items.sumOf { it.amount }.peso()} overdue (${Aging.daysLate(oldest.due!!, now, z)}d)", true) }
        val bal = entries.groupBy { it.customerId }.mapValues { (_, l) -> l.sumOf { it.principalDelta + it.feeDelta } }
        follow.groupBy { it.customerId }.forEach { (cid, l) -> val f = l.maxBy { it.createdAt }; val p = f.promiseDate; val c = cm[cid]
            if (c != null && p != null && (bal[cid] ?: 0L) > 0 && Aging.daysLate(p, now, z) > 0)
                out += Alert("PROMISE:${f.id}", AlertKind.PROMISE, cid, "Promise to pay missed", "${c.name} promised ${p.dmy()}", true) }
        customers.filter { it.creditLimit > 0 && (bal[it.id] ?: 0L) * 100 >= it.creditLimit * cfg.limitPct }.forEach { c ->
            val b = bal[c.id] ?: 0L; val atLimit = b >= c.creditLimit
            out += Alert("LIMIT:${c.id}:${if (atLimit) "max" else "near"}", AlertKind.LIMIT, c.id, if (atLimit) "Credit limit reached" else "Near credit limit",
                "${c.name} • ${b.peso()} of ${c.creditLimit.peso()}", atLimit) }
        if (lastBackup != null && cfg.backupDays > 0 && entries.isNotEmpty()) {   // standalone app: a backup file is the only safety net
            val ref = maxOf(lastBackup, entries.minOf { it.createdAt }); val ageDays = (now - ref) / 86_400_000L
            if (ageDays >= cfg.backupDays) out += Alert("BACKUP:$lastBackup", AlertKind.BACKUP, "", "Back up your data",
                if (lastBackup == 0L) "No backup yet. If this phone is lost, your records are lost." else "Last backup was $ageDays days ago", ageDays >= 2L * cfg.backupDays)
        }
        return out.sortedWith(compareByDescending<Alert> { it.urgent }.thenBy { it.kind.ordinal })
    }
}

data class DueItem(val customer: String, val amount: Long, val overdue: Boolean)
object Calendar {
    fun byDay(open: List<OpenItem>, names: Map<String, String>, today: LocalDate, z: ZoneId): Map<LocalDate, List<DueItem>> =
        open.filter { it.due != null }.groupBy { Instant.ofEpochMilli(it.due!!).atZone(z).toLocalDate() }
            .mapValues { (d, l) -> l.map { DueItem(names[it.customerId] ?: "", it.amount, d.isBefore(today)) } }
}

@Composable
fun NotificationsScreen(vm: VM, open: (String) -> Unit) {
    val alerts by vm.alerts.collectAsState(); val read by vm.readKeys.collectAsState(); val cfg by vm.cfg.collectAsState()
    var dlg by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            Row { TextButton({ vm.markRead(alerts.map { it.key }, alerts) }) { Text("Mark all read") }; TextButton({ dlg = true }) { Text("Settings") } } }
        if (alerts.isEmpty()) Text("You're all caught up.")
        LazyColumn { items(alerts) { a -> val unread = a.key !in read
            ListItem(headlineContent = { Text((if (unread) "● " else "") + a.title, color = if (a.urgent) Danger else MaterialTheme.colorScheme.primary) }, supportingContent = { Text(a.detail) },
                trailingContent = { Text(a.kind.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.clickable { vm.markRead(listOf(a.key), alerts); open(a.customerId) })
            HorizontalDivider() } }
    }
    if (dlg) AlertSettingsDialog(cfg, { dlg = false }) { vm.saveCfg(it); dlg = false }
}

@Composable
fun AlertSettingsDialog(cfg: AlertCfg, close: () -> Unit, save: (AlertCfg) -> Unit) {
    var soon by remember { mutableStateOf("${cfg.soonDays}") }; var lead by remember { mutableStateOf("${cfg.leadDays}") }; var pct by remember { mutableStateOf("${cfg.limitPct}") }
    var push by remember { mutableStateOf(cfg.push) }; var bk by remember { mutableStateOf("${cfg.backupDays}") }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (!ok) push = false }
    val s = soon.toIntOrNull(); val l = lead.toIntOrNull(); val p = pct.toIntOrNull(); val bd = bk.toIntOrNull()
    AlertDialog(close, title = { Text("Alert settings") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(soon, { soon = it.filter(Char::isDigit).take(3) }, label = { Text("\"Due soon\" window (days)") }, singleLine = true)
        OutlinedTextField(lead, { lead = it.filter(Char::isDigit).take(2) }, label = { Text("Alert before due date (days)") }, singleLine = true)
        OutlinedTextField(pct, { pct = it.filter(Char::isDigit).take(3) }, label = { Text("Warn at % of credit limit") }, singleLine = true)
        OutlinedTextField(bk, { bk = it.filter(Char::isDigit).take(2) }, label = { Text("Remind me to back up every (days, 0 = never)") }, singleLine = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(push, { on -> push = on; if (on && Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS) }); Text("Daily reminder notification") }
        Text("It only says how many accounts need attention (for example \"3 accounts need attention\"), never names or amounts, including on the lock screen.", style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton({ save(AlertCfg(s!!, l!!, p!!, push, bd!!)) }, enabled = s != null && l != null && p != null && bd != null && bd in 0..90 && s in 1..90 && l in 0..30 && p in 1..100) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

@Composable
fun CalendarScreen(vm: VM, back: () -> Unit) {
    val entries by vm.allEntries.collectAsState(); val custs by vm.customers.collectAsState()
    val z = Cur.zone; val today = LocalDate.now(z)
    var month by remember { mutableStateOf(YearMonth.from(today)) }; var sel by remember { mutableStateOf<LocalDate?>(today) }
    val days = remember(entries, custs) { Calendar.byDay(Aging.open(entries), custs.associate { it.c.id to it.c.name }, today, z) }
    val offset = month.atDay(1).dayOfWeek.value % 7; val weeks = (offset + month.lengthOfMonth() + 6) / 7
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton({ month = month.minusMonths(1) }) { Text("◀") }
            Text("${month.month.name.lowercase().replaceFirstChar(Char::uppercase)} ${month.year}", style = MaterialTheme.typography.titleMedium)
            TextButton({ month = month.plusMonths(1) }) { Text("▶") } }
        Row { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) } }
        for (w in 0 until weeks) Row { for (c in 0 until 7) { val dn = w * 7 + c - offset + 1
            if (dn in 1..month.lengthOfMonth()) { val d = month.atDay(dn); val items = days[d].orEmpty()
                Box(Modifier.weight(1f).aspectRatio(1f).background(if (d == sel) Sky.copy(alpha = 0.25f) else Color.Transparent).clickable { sel = d }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("$dn")
                        if (items.isNotEmpty()) Text("●", color = if (items.any { it.overdue }) Danger else Emerald, fontSize = 10.sp) } }
            } else Spacer(Modifier.weight(1f)) } }
        HorizontalDivider()
        Text("Scheduled this month: ${days.filterKeys { YearMonth.from(it) == month }.values.sumOf { l -> l.sumOf { it.amount } }.peso()}", style = MaterialTheme.typography.bodySmall)
        sel?.let { d -> Text(d.toString(), style = MaterialTheme.typography.titleSmall); val l = days[d].orEmpty()
            if (l.isEmpty()) Text("Nothing due.") else l.forEach { Text("${it.customer} • ${it.amount.peso()}" + if (it.overdue) " (overdue)" else "", color = if (it.overdue) Danger else MaterialTheme.colorScheme.primary) } }
        TextButton(back) { Text("Back") }
    }
}
