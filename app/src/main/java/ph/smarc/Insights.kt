package ph.smarc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/** A cash count compared with what the books say. Recording it never changes the books. */
@Entity(tableName = "reconciliations")
data class Reconciliation(@PrimaryKey val id: String = UUID.randomUUID().toString(), val expected: Long, val counted: Long, val note: String, val actor: String, val createdAt: Long = System.currentTimeMillis())
val Reconciliation.diff: Long get() = counted - expected

val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `reconciliations` (`id` TEXT NOT NULL, `expected` INTEGER NOT NULL, `counted` INTEGER NOT NULL, `note` TEXT NOT NULL, `actor` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("ALTER TABLE `users` ADD COLUMN `totp` TEXT NOT NULL DEFAULT ''")
    }
}

data class CustProfit(val customerId: String, val name: String, val income: Long, val loss: Long, val collected: Long) { val net get() = income - loss }
data class Behavior(val score: Int?, val label: String, val onTimePct: Int?, val latePayments: Int, val avgDelayDays: Double, val settled: Int, val balance: Long, val overdueDays: Long, val usedPct: Int?, val factors: List<String>)
data class Forecast(val feesToCollect: Long, val expectedLoss: Long, val expectedExpenses: Long, val months: Int) { val estimate get() = feesToCollect - expectedLoss - expectedExpenses }

/** Replays a customer's ledger in date order. Payments settle the oldest-due credit first; the days between due date and payment date decide on-time vs late. */
object BehaviorCalc {
    private class Slot(val due: Long?, var rem: Long)
    fun analyze(all: List<Entry>, limit: Long, now: Long, z: ZoneId = Cur.zone, soonDays: Int = 7): Behavior {
        val reversed = all.mapNotNull { it.reversalOf }.toSet()
        val live = all.filter { it.type != "REVERSAL" && it.id !in reversed }.sortedBy { it.createdAt }
        val queue = mutableListOf<Slot>(); var onTime = 0L; var late = 0L; var lateEvents = 0; var delaySum = 0.0; var settled = 0
        for (e in live) {
            val amt = e.principalDelta + e.feeDelta
            if (e.type in OPENERS || ((e.type == "ADJUSTMENT" || e.type == "REFUND") && amt > 0)) { if (amt > 0) queue.add(Slot(e.dueDate, amt)) }
            else if (amt < 0) {
                var left = -amt; val pay = e.type == "PAYMENT"; var wasLate = false
                queue.sortWith(compareBy(nullsLast<Long>()) { s: Slot -> s.due })
                val iter = queue.iterator()
                while (iter.hasNext() && left > 0) {
                    val s = iter.next(); val use = minOf(left, s.rem); s.rem -= use; left -= use
                    if (pay) { val d = s.due?.let { Aging.daysLate(it, e.createdAt, z) } ?: 0L
                        if (d > 0) { late += use; delaySum += d.toDouble() * use; wasLate = true } else onTime += use }
                    if (s.rem == 0L) { iter.remove(); if (pay) settled++ }
                }
                if (wasLate) lateEvents++
            }
        }
        val bal = queue.sumOf { it.rem }
        val overdue = queue.mapNotNull { sl -> sl.due?.let { Aging.daysLate(it, now, z) } }.maxOrNull()?.coerceAtLeast(0L) ?: 0L
        val upcoming = queue.any { sl -> sl.due?.let { Aging.daysLate(it, now, z) in -soonDays..0 } == true }
        val used = if (limit > 0) (bal * 100 / limit).toInt() else null
        val total = onTime + late; val ratio = if (total > 0) onTime.toDouble() / total else null
        val avg = if (late > 0) delaySum / late else 0.0
        var score: Int? = null
        if (total > 0 || overdue > 0) {
            var pts = 100.0
            if (ratio != null) pts -= 40 * (1 - ratio)
            if (late > 0) pts -= minOf(20.0, avg / 3)
            pts -= when { overdue > 30 -> 20; overdue > 7 -> 10; overdue > 0 -> 5; else -> 0 }
            if (used != null && used > 80) pts -= minOf(20.0, 20.0 * (used - 80) / 20)
            score = pts.toInt().coerceIn(0, 100)
        }
        val label = when {
            score == null -> "Not enough history"
            lateEvents >= 2 && (ratio ?: 1.0) < 0.7 -> "Repeated late payments"
            (used ?: 0) >= 90 -> "High outstanding balance"
            overdue > 0 || upcoming -> "Monitor upcoming payments"
            else -> "Good payment history"
        }
        val f = mutableListOf<String>()
        if (ratio != null) f += "Paid on time: ${(ratio * 100).toInt()}% of the amount paid"
        if (lateEvents > 0) f += "Late payments: $lateEvents (about ${String.format(Locale.US, "%.1f", avg)} days late on average)"
        if (overdue > 0) f += "Overdue now: $overdue days"
        if (used != null) f += "Credit limit used: $used%"
        if (settled > 0) f += "Credits fully settled: $settled"
        if (f.isEmpty()) f += "No payments or overdue amounts yet"
        return Behavior(score, label, ratio?.let { (it * 100).toInt() }, lateEvents, avg, settled, bal, overdue, used, f)
    }
}

/** Estimate only. Never added to realized profit. Assumptions are shown on screen. */
object ForecastCalc {
    fun estimate(entries: List<Entry>, cash: List<CashEntry>, now: Long, badDebtPct: Int, z: ZoneId = Cur.zone): Forecast {
        val open = Aging.open(entries); val day = 86_400_000L
        val feeDue = entries.sumOf { it.feeDelta }.coerceAtLeast(0L)
        val risky = open.filter { it.due != null && Aging.daysLate(it.due, now, z) > Cur.t2 }.sumOf { it.amount }
        val loss = risky * badDebtPct.coerceIn(0, 100) / 100
        val monthlyExp = cash.filter { it.createdAt >= now - 90 * day }.sumOf { it.expenseDelta } / 3
        val lastDue = open.mapNotNull { it.due }.maxOrNull()
        val months = when { open.isEmpty() -> 0; lastDue == null -> 1; else -> (((lastDue - now) / day + 29) / 30).toInt().coerceIn(1, 12) }
        return Forecast(feeDue, loss, monthlyExp.coerceAtLeast(0L) * months, months)
    }
}

object Insights {
    fun profitByCustomer(entries: List<Entry>, names: Map<String, String>, from: Long, to: Long): List<CustProfit> =
        entries.filter { it.createdAt in from..to }.groupBy { it.customerId }
            .map { (id, l) -> CustProfit(id, names[id] ?: "", l.sumOf { it.incomeDelta }, l.sumOf { it.lossDelta }, l.sumOf { it.cashDelta }) }
            .filter { it.income != 0L || it.loss != 0L || it.collected != 0L }.sortedByDescending { it.net }
    fun behaviors(entries: List<Entry>, customers: List<Customer>, now: Long, soonDays: Int = 7): List<Pair<Customer, Behavior>> {
        val by = entries.groupBy { it.customerId }
        return customers.map { c -> c to BehaviorCalc.analyze(by[c.id].orEmpty(), c.creditLimit, now, soonDays = soonDays) }.sortedBy { it.second.score ?: 101 }
    }
    fun profitCsv(l: List<CustProfit>) = buildString {
        append(Csv.row("Customer", "Interest/fee income", "Bad-debt loss", "Net contribution", "Cash collected (net)"))
        l.forEach { append(Csv.row(Csv.text(it.name), Fmt.plain(it.income), Fmt.plain(-it.loss), Fmt.plain(it.net), Fmt.plain(it.collected))) }
    }
    fun behaviorCsv(rows: List<Pair<Customer, Behavior>>) = buildString {
        append(Csv.row("Customer", "Indicator (0-100)", "Status", "Paid on time %", "Late payments", "Avg days late", "Overdue days", "Credit used %", "Balance"))
        rows.forEach { (c, b) -> append(Csv.row(Csv.text(c.name), b.score?.toString() ?: "", b.label, b.onTimePct?.toString() ?: "", "${b.latePayments}",
            String.format(Locale.US, "%.1f", b.avgDelayDays), "${b.overdueDays}", b.usedPct?.toString() ?: "", Fmt.plain(b.balance))) }
    }
}

@Composable private fun Line(k: String, v: String, color: Color? = null) =
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(k); Text(v, color = color ?: MaterialTheme.colorScheme.onSurface) }

@Composable
fun InsightsScreen(vm: VM, back: () -> Unit) {
    val ctx = LocalContext.current
    val entries by vm.allEntries.collectAsState(); val cashE by vm.cashEntries.collectAsState(); val custs by vm.customers.collectAsState()
    val t by vm.totals.collectAsState(); val c by vm.cash.collectAsState(); val recons by vm.recons.collectAsState(); val cfg by vm.cfg.collectAsState()
    var period by remember { mutableStateOf(Period.MONTH) }; var xlsx by remember { mutableStateOf(false) }
    var counted by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var msg by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<Pair<Customer, Behavior>?>(null) }
    val now = System.currentTimeMillis(); val (from, to) = Reports.range(period, now)
    val names = custs.associate { it.c.id to it.c.name }
    val expected = Fin.cashOnHand(c.capital, t.collected, t.lent, c.expenses); val available = Fin.available(expected, c.capital, Cur.reservePct)
    val profits = remember(entries, period, custs) { Insights.profitByCustomer(entries, names, from, to) }
    val behaviors = remember(entries, custs, cfg) { Insights.behaviors(entries, custs.map { it.c }, now, cfg.soonDays) }
    val fc = remember(entries, cashE) { ForecastCalc.estimate(entries, cashE, now, Cur.badDebtPct) }
    val cv = if (counted.isBlank()) null else parsePeso(counted)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Insights & cash", style = MaterialTheme.typography.titleLarge); TextButton(back) { Text("Back") } }
        Text("Cash reconciliation", style = MaterialTheme.typography.titleMedium)
        Text("The books say you should have ${expected.peso()} in cash.")
        OutlinedTextField(counted, { counted = it }, label = { Text("Cash actually counted (${Cur.symbol})") }, singleLine = true, isError = counted.isNotBlank() && cv == null, modifier = Modifier.fillMaxWidth())
        if (cv != null) { val diff = cv - expected; Text(if (diff == 0L) "Matches the books" else "Difference: ${diff.peso()}", color = if (diff == 0L) Emerald else Danger) }
        OutlinedTextField(note, { note = it }, label = { Text("Note (required if different)") }, modifier = Modifier.fillMaxWidth())
        Button({ vm.launch { msg = vm.cashSvc.reconcile(cv!!, note).text(); counted = ""; note = "" } }, enabled = cv != null) { Text("Record reconciliation") }
        msg?.let { Text(it) }
        recons.take(5).forEach { r -> Text("${r.createdAt.dmy()} • expected ${r.expected.peso()} • counted ${r.counted.peso()} • difference ${r.diff.peso()}${if (r.note.isNotBlank()) " • " + r.note else ""}",
            style = MaterialTheme.typography.bodySmall, color = if (r.diff == 0L) Emerald else Danger) }
        Text("A reconciliation only records the difference; it does not change your books. Explain a gap with an expense or capital entry.", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Lending capital", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Cash on hand", expected.peso(), mod = Modifier.weight(1f)); Stat("Available to lend", available.peso(), Emerald, Modifier.weight(1f), if (Cur.reservePct > 0) "after ${Cur.reservePct}% reserve" else "no reserve set") }
        HorizontalDivider()
        Text("Estimated future profit", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Line("Fees/interest still to collect", fc.feesToCollect.peso()); Line("Expected losses (${Cur.badDebtPct}% of balances overdue > ${Cur.t2} days)", (-fc.expectedLoss).peso())
            Line("Expected expenses (${fc.months} month${if (fc.months == 1) "" else "s"})", (-fc.expectedExpenses).peso()); HorizontalDivider()
            Line("Estimate (not realized)", fc.estimate.peso(), if (fc.estimate >= 0) Emerald else Danger)
            Text("Assumes open fees are collected except the expected losses, and expenses continue at your last-3-month average until the last open due date. It is an estimate only and is never added to realized profit.", style = MaterialTheme.typography.bodySmall) } }
        HorizontalDivider()
        Text("Profit by customer", style = MaterialTheme.typography.titleMedium)
        ChipRow { Period.values().forEach { FilterChip(period == it, { period = it }, { Text(it.label) }) } }
        if (profits.isEmpty()) Text("No income or losses in this period.")
        profits.take(10).forEach { Line(it.name.ifBlank { "Customer" }, it.net.peso(), if (it.net >= 0) Emerald else Danger) }
        Text("Net contribution = interest/fee income earned minus bad debt. Expenses are business-wide and not split by customer. Profit per individual credit is not shown because payments are not tied to a single credit.", style = MaterialTheme.typography.bodySmall)
        ChipRow { FilterChip(!xlsx, { xlsx = false }, { Text("CSV") }); FilterChip(xlsx, { xlsx = true }, { Text("Excel (XLSX)") }) }
        Button({ Export.table(ctx, xlsx, "profit_by_customer_${period.name}", Insights.profitCsv(profits)) }, Modifier.fillMaxWidth()) { Text("Export profit by customer") }
        HorizontalDivider()
        Text("Customer payment behaviour", style = MaterialTheme.typography.titleMedium)
        Text("An internal business indicator from 0 to 100, based only on your own records. It is not a credit bureau score and never approves or declines credit for you.", style = MaterialTheme.typography.bodySmall)
        behaviors.forEach { (cu, b) -> ListItem(headlineContent = { Text(cu.name) }, supportingContent = { Text(b.label) },
            trailingContent = { Text(b.score?.toString() ?: "-", color = when { b.score == null -> MaterialTheme.colorScheme.onSurface; b.score >= 80 -> Emerald; b.score >= 60 -> MaterialTheme.colorScheme.primary; else -> Danger }) },
            modifier = Modifier.clickable { detail = cu to b }) }
        Button({ Export.table(ctx, xlsx, "payment_behaviour", Insights.behaviorCsv(behaviors)) }, Modifier.fillMaxWidth(), enabled = behaviors.isNotEmpty()) { Text("Export payment behaviour") }
    }
    detail?.let { (cu, b) -> AlertDialog({ detail = null }, title = { Text("${cu.name}: ${b.score?.toString() ?: "no score"}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(b.label, style = MaterialTheme.typography.titleSmall); b.factors.forEach { Text("• $it") }
            Text("Indicator = 100 − 40 × (share of paid amount that was late) − up to 20 for average days late (1 point per 3 days) − 5 / 10 / 20 if overdue now by 1-7 / 8-30 / over 30 days − up to 20 when more than 80% of the credit limit is used.", style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton({ detail = null }) { Text("Close") } }) }
}
