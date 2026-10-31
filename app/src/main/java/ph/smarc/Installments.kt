package ph.smarc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId

enum class Freq(val label: String) { WEEKLY("Weekly"), SEMI("Every 15 days"), MONTHLY("Monthly") }

object Schedule {
    /** Due dates for n installments starting at [first]. Monthly dates are computed from the first date (Jan 31 -> Feb 28 -> Mar 31). */
    fun dates(first: Long, n: Int, f: Freq, z: ZoneId = Cur.zone): List<Long> {
        val s = Instant.ofEpochMilli(first).atZone(z)
        return List(n) { k -> when (f) { Freq.WEEKLY -> s.plusWeeks(k.toLong()); Freq.SEMI -> s.plusDays(15L * k); Freq.MONTHLY -> s.plusMonths(k.toLong()) }.toInstant().toEpochMilli() }
    }
}

data class Inst(val no: Int, val total: Int, val due: Long?, val amount: Long, val open: Long)
data class PlanView(val planId: String, val customerId: String, val items: List<Inst>)

/** An installment plan is N ordinary CREDIT ledger entries sharing a planId, so aging, collections and dashboards work unchanged. */
object Plans {
    fun build(entries: List<Entry>): List<PlanView> {
        val reversed = entries.mapNotNull { it.reversalOf }.toSet(); val open = Aging.open(entries).associateBy { it.id }
        return entries.filter { it.type == "CREDIT" && it.planId != null && it.id !in reversed }.groupBy { it.planId!! }.map { (pid, l) ->
            val s = l.sortedBy { it.dueDate ?: Long.MAX_VALUE }
            PlanView(pid, s[0].customerId, s.mapIndexed { i, e -> Inst(i + 1, s.size, e.dueDate, e.principalDelta + e.feeDelta, open[e.id]?.amount ?: 0L) }) }
    }
    fun state(i: Inst, now: Long, z: ZoneId = Cur.zone): String {
        if (i.open == 0L) return "Paid"
        val d = i.due?.let { Aging.daysLate(it, now, z) } ?: return "Open"
        return when { d > 0 -> "Overdue"; d == 0L -> "Due today"; else -> "Upcoming" }
    }
    fun status(i: Inst, now: Long, z: ZoneId = Cur.zone): String {
        val st = state(i, now, z); if (st == "Paid") return st
        val d = i.due?.let { Aging.daysLate(it, now, z) }
        return (if (st == "Overdue") "Overdue ${d}d" else if (st == "Upcoming") "Due ${i.due!!.dmy()}" else st) + " • ${i.open.peso()} left"
    }
    fun csv(entries: List<Entry>, names: Map<String, String>, now: Long, date: (Long) -> String): String = buildString {
        append(Csv.row("Customer", "Plan", "Installment", "Due date", "Amount", "Remaining", "Status"))
        build(entries).sortedBy { names[it.customerId] ?: "" }.forEach { p -> p.items.forEach { i ->
            append(Csv.row(Csv.text(names[p.customerId] ?: ""), p.planId.take(8), "${i.no}/${i.total}", i.due?.let(date) ?: "", Fmt.plain(i.amount), Fmt.plain(i.open), state(i, now))) } }
    }
}

data class ItemRow(val name: String = "", val qty: String = "1", val price: String = "")

@Composable
fun CreditDialog(known: List<Pair<String, Long>>, close: () -> Unit, save: (Long, Long, Long?, Int, Freq, String, String, Int, String) -> Unit) {
    var amt by remember { mutableStateOf("") }; var fee by remember { mutableStateOf("") }; var days by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("1") }; var freq by remember { mutableStateOf(Freq.MONTHLY) }; var note by remember { mutableStateOf("") }; var inv by remember { mutableStateOf("") }
    var tax by remember { mutableStateOf("${Cur.taxPct}") }; var itemize by remember { mutableStateOf(false) }
    val rows = remember { mutableStateListOf(ItemRow()) }
    val lines = Items.parseRows(rows.map { Triple(it.name, it.qty, it.price) })
    val a: Long? = if (itemize) lines?.sumOf { it.total } else parsePeso(amt)
    val f = if (fee.isBlank()) 0L else parsePeso(fee); val n = count.toIntOrNull() ?: 0; val d = days.toLongOrNull(); val tp = tax.toIntOrNull() ?: 0
    val valid = a != null && f != null && a > 0 && n in 1..60 && a >= n && (n == 1 || d != null) && tp in 0..100
    val taxAmt = if (a != null) Tax.of(a, tp) else 0L
    val per = if (valid && n > 1) Calc.split(a!! + taxAmt, n)[0] + Calc.split(f!!, n)[0] else null
    AlertDialog(close, title = { Text("New credit") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Switch(itemize, { itemize = it }); Text("List the items sold") }
            if (itemize) {
                rows.forEachIndexed { i, r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(r.name, { rows[i] = r.copy(name = it) }, label = { Text("Item") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(r.qty, { rows[i] = r.copy(qty = it) }, label = { Text("Qty") }, singleLine = true, modifier = Modifier.width(64.dp))
                        OutlinedTextField(r.price, { rows[i] = r.copy(price = it) }, label = { Text("Price") }, singleLine = true, modifier = Modifier.width(88.dp))
                        if (rows.size > 1) TextButton({ rows.removeAt(i) }) { Text("x") } }
                    if (r.name.length >= 2) known.filter { it.first.contains(r.name, true) && !it.first.equals(r.name, true) }.take(3).forEach { (nm, pr) ->
                        TextButton({ rows[i] = r.copy(name = nm, price = Fmt.plain(pr)) }) { Text("$nm  ${pr.peso()}") } } }
                TextButton({ rows.add(ItemRow()) }, enabled = rows.size < 50) { Text("+ Add item") }
                Text("Items total: ${a?.peso() ?: "-"}  (fill name and price on every line you use)", style = MaterialTheme.typography.bodySmall)
            } else OutlinedTextField(amt, { amt = it }, label = { Text("Price / principal (${Cur.symbol})") }, singleLine = true, isError = amt.isNotBlank() && a == null)
            OutlinedTextField(tax, { tax = it.filter(Char::isDigit).take(3) }, label = { Text("Tax % added on top (not income)") }, singleLine = true)
            OutlinedTextField(fee, { fee = it }, label = { Text("Interest / service fee (${Cur.symbol}, optional)") }, singleLine = true, isError = f == null)
            if (a != null && a > 0) Text("Customer owes ${(a + taxAmt + (f ?: 0L)).peso()}" + if (taxAmt > 0) " (includes tax ${taxAmt.peso()})" else "", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(count, { count = it.filter(Char::isDigit).take(2) }, label = { Text("Number of installments (1 = single due date)") }, singleLine = true)
            OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text(if (n > 1) "First installment due in (days)" else "Due in (days)") }, singleLine = true)
            if (n > 1) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Freq.values().forEach { FilterChip(freq == it, { freq = it }, { Text(it.label) }) } }
                per?.let { Text("$n payments of about ${it.peso()} each", style = MaterialTheme.typography.bodySmall) } }
            OutlinedTextField(inv, { inv = it }, label = { Text("Invoice / reference no. (optional, must be unique)") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") })
        } },
        confirmButton = { TextButton({ save(a!!, f!!, d?.let { System.currentTimeMillis() + it * 86_400_000L }, n, freq, note, inv.trim(), tp, if (itemize) Items.encode(lines!!) else "") }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}
