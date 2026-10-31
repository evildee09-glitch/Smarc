package ph.smarc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Bin(val start: Long, val end: Long, val label: String)
data class Point(val label: String, val issued: Long, val collected: Long, val receivables: Long)

object Trend {
    fun bins(p: Period, now: Long, z: ZoneId): List<Bin> {
        val today = Instant.ofEpochMilli(now).atZone(z).toLocalDate()
        fun ms(d: LocalDate) = d.atStartOfDay(z).toInstant().toEpochMilli()
        return when (p) {
            Period.MONTH -> (1..today.dayOfMonth).map { d -> val x = today.withDayOfMonth(d); Bin(ms(x), ms(x.plusDays(1)) - 1, "$d") }
            Period.D30 -> (29 downTo 0).map { k -> val x = today.minusDays(k.toLong()); Bin(ms(x), ms(x.plusDays(1)) - 1, "${x.monthValue}/${x.dayOfMonth}") }
            Period.YEAR -> { val s0 = Cur.fyStart(today); val n = java.time.temporal.ChronoUnit.MONTHS.between(s0, today.withDayOfMonth(1)).toInt()
                (0..n).map { k -> val x = s0.plusMonths(k.toLong()); Bin(ms(x), ms(x.plusMonths(1)) - 1, x.month.name.take(3)) } }
            Period.ALL -> (11 downTo 0).map { k -> val x = today.withDayOfMonth(1).minusMonths(k.toLong()); Bin(ms(x), ms(x.plusMonths(1)) - 1, x.month.name.take(3)) }
        }
    }
    /** Receivables = running ledger balance at the end of each bin (all history up to that point). */
    fun series(entries: List<Entry>, p: Period, now: Long, z: ZoneId = Cur.zone): List<Point> = bins(p, now, z).map { b ->
        val x = Reports.pnl(entries, emptyList(), b.start, b.end)
        Point(b.label, x.issued, x.collected, entries.filter { it.createdAt <= b.end }.sumOf { it.principalDelta + it.feeDelta })
    }
    fun delta(cur: Long, prev: Long?): String = when {
        prev == null -> ""
        prev == 0L -> if (cur == 0L) "no change" else "new"
        else -> { val pct = (cur - prev) * 100 / kotlin.math.abs(prev); when { pct > 0 -> "▲ $pct%"; pct < 0 -> "▼ ${-pct}%"; else -> "no change" } }
    }
}

@Composable
fun Stat(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary, mod: Modifier = Modifier, sub: String = "") =
    Card(mod) { Column(Modifier.padding(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall) } }

@Composable
fun BarPairChart(pts: List<Point>) {
    val max = (pts.maxOfOrNull { maxOf(it.issued, it.collected) } ?: 0L).coerceAtLeast(1)
    val desc = "Bar chart of credit issued versus collected. Total issued ${pts.sumOf { it.issued }.peso()}, collected ${pts.sumOf { it.collected }.peso()}."
    Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = desc }) {
        val w = size.width / maxOf(pts.size, 1); val bw = w * 0.4f
        pts.forEachIndexed { i, p -> val x = i * w + w * 0.08f
            val h1 = size.height * p.issued.coerceAtLeast(0) / max; val h2 = size.height * p.collected.coerceAtLeast(0) / max
            drawRect(Sky, Offset(x, size.height - h1), Size(bw, h1)); drawRect(Emerald, Offset(x + bw, size.height - h2), Size(bw, h2)) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(pts.firstOrNull()?.label ?: "", style = MaterialTheme.typography.labelSmall)
        Text("■ Issued  ■ Collected", style = MaterialTheme.typography.labelSmall); Text(pts.lastOrNull()?.label ?: "", style = MaterialTheme.typography.labelSmall) }
}

@Composable
fun LineChart(vals: List<Long>, color: Color) {
    val max = (vals.maxOrNull() ?: 0L).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(110.dp).semantics { contentDescription = "Line chart of outstanding receivables over time, latest ${(vals.lastOrNull() ?: 0L).peso()}" }) {
        if (vals.size < 2) return@Canvas
        val path = Path()
        vals.forEachIndexed { i, v -> val x = size.width * i / (vals.size - 1); val y = size.height * (1f - v.coerceAtLeast(0).toFloat() / max); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        drawPath(path, color, style = Stroke(width = 5f))
    }
}

@Composable
fun HBars(items: List<Triple<String, Long, Color>>) {
    val max = (items.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { items.forEach { (l, v, c) ->
        Column { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(l, style = MaterialTheme.typography.bodySmall); Text(v.peso(), style = MaterialTheme.typography.bodySmall) }
            Box(Modifier.fillMaxWidth(v.coerceAtLeast(0).toFloat() / max).height(8.dp).background(c)) } } }
}

private val Amber = Color(0xFFF59E0B)

@Composable
fun Dashboard(vm: VM, customers: List<CustomerSummary>, openBackup: () -> Unit = {}) {
    val t by vm.totals.collectAsState(); val c by vm.cash.collectAsState(); val entries by vm.allEntries.collectAsState()
    val cashE by vm.cashEntries.collectAsState(); val aging by vm.aging.collectAsState()
    var period by remember { mutableStateOf(Period.MONTH) }
    val now = System.currentTimeMillis(); val z = Cur.zone; val (from, to) = Reports.range(period, now, z)
    val cur = Reports.pnl(entries, cashE, from, to); val prevR = Reports.previous(period, now, z)
    val prev = prevR?.let { Reports.pnl(entries, cashE, it.first, it.second) }
    val series = remember(entries, period) { Trend.series(entries, period, now, z) }
    val rate = Reports.collectionRate(entries, from, to)
    val cashNow = Fin.cashOnHand(c.capital, t.collected, t.lent, c.expenses)
    
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (vm.can(Action.BACKUP)) vm.alerts.collectAsState().value.firstOrNull { it.kind == AlertKind.BACKUP }?.let { b ->
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { Text(b.title, style = MaterialTheme.typography.titleSmall); Text(b.detail, style = MaterialTheme.typography.bodySmall) }
                Button(openBackup) { Text("Back up now") } } } }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Period.values().forEach { FilterChip(period == it, { period = it }, { Text(it.label) }) } }
        if (prevR != null) Text("Compared with " + when (period) { Period.MONTH -> "the same days last month"; Period.D30 -> "the previous 30 days"; else -> "the same days last year" }, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Credit issued", cur.issued.peso(), mod = Modifier.weight(1f), sub = Trend.delta(cur.issued, prev?.issued))
            Stat("Cash collected", cur.collected.peso(), Emerald, Modifier.weight(1f), Trend.delta(cur.collected, prev?.collected))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Interest/fee income", cur.income.peso(), Emerald, Modifier.weight(1f), Trend.delta(cur.income, prev?.income))
            Stat("Net profit", cur.net.peso(), if (cur.net >= 0) Emerald else Danger, Modifier.weight(1f), Trend.delta(cur.net, prev?.net))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Collection rate", rate?.let { "%.0f%%".format(it) } ?: "—", mod = Modifier.weight(1f), sub = "collected ÷ amount due in period")
            Stat("Principal recovered", cur.recovered.peso(), mod = Modifier.weight(1f), sub = "capital back, not profit")
        }
        Text("Right now", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Total receivables", t.receivables.peso(), mod = Modifier.weight(1f))
            Stat("Overdue", aging.overdueTotal.peso(), if (aging.overdueTotal > 0) Danger else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Cash on hand", cashNow.peso(), if (cashNow >= 0) MaterialTheme.colorScheme.primary else Danger, Modifier.weight(1f))
            Stat("Available to lend", Fin.available(cashNow, c.capital, Cur.reservePct).peso(), Emerald, Modifier.weight(1f), if (Cur.reservePct > 0) "after ${Cur.reservePct}% reserve" else "= cash on hand")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("Active customers", "${customers.count { it.balance > 0 }}", mod = Modifier.weight(1f))
            Stat("Bad debt (all time)", t.loss.peso(), if (t.loss > 0) Danger else MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Text("Credit issued vs collected", style = MaterialTheme.typography.titleMedium); BarPairChart(series)
        Text("Outstanding receivables", style = MaterialTheme.typography.titleMedium); LineChart(series.map { it.receivables }, MaterialTheme.colorScheme.primary)
        val dist = Bucket.values().mapNotNull { b -> aging.buckets[b]?.takeIf { it > 0 }?.let { Triple(b.label, it, when { b == Bucket.CURRENT -> Sky; b.ordinal < Bucket.D1_7.ordinal -> Amber; else -> Danger }) } }
        if (dist.isNotEmpty()) { Text("Receivables by due status", style = MaterialTheme.typography.titleMedium); HBars(dist) }
        val top = customers.filter { it.balance > 0 }.sortedByDescending { it.balance }.take(5).map { Triple(it.c.name, it.balance, MaterialTheme.colorScheme.primary) }
        if (top.isNotEmpty()) { Text("Top customers by balance", style = MaterialTheme.typography.titleMedium); HBars(top) }
        Text("Every figure is calculated from recorded ledger entries. Collection rate can exceed 100% when older balances are paid.", style = MaterialTheme.typography.bodySmall)
    }
}
