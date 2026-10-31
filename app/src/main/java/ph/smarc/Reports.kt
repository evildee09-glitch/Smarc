package ph.smarc

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object Fmt {   // plain decimal, no currency glyph (safe for CSV/PDF fonts)
    fun plain(v: Long): String { val a = kotlin.math.abs(v); return (if (v < 0) "-" else "") + "${a / 100}." + "%02d".format(a % 100) }
}

object Csv {
    /** Escapes quotes/commas/newlines and neutralizes spreadsheet formula injection (=,+,-,@). */
    fun text(s: String): String {
        val v = if (s.isNotEmpty() && s[0] in "=+-@\t\r") "'$s" else s
        return if (v.any { it in ",\"\n\r" }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
    fun row(vararg c: String) = c.joinToString(",") + "\r\n"
}

data class Pnl(val issued: Long, val collected: Long, val recovered: Long, val income: Long, val loss: Long, val expenses: Long, val tax: Long = 0) { val net get() = income - loss - expenses }
enum class Period(val label: String) { ALL("All time"), MONTH("This month"), D30("Last 30 days"), YEAR("Financial year") }

object Reports {
    fun range(p: Period, now: Long, z: ZoneId = Cur.zone): Pair<Long, Long> {
        val d = Instant.ofEpochMilli(now).atZone(z).toLocalDate()
        fun ms(x: java.time.LocalDate) = x.atStartOfDay(z).toInstant().toEpochMilli()
        return when (p) { Period.ALL -> 0L to Long.MAX_VALUE; Period.MONTH -> ms(d.withDayOfMonth(1)) to now
            Period.D30 -> now - 30 * 86_400_000L to now; Period.YEAR -> ms(Cur.fyStart(d)) to now }
    }

    /** Activity-based P&L: deltas are attributed to the date each entry was posted (a reversal counts on its own date). */
    fun pnl(entries: List<Entry>, cash: List<CashEntry>, from: Long, to: Long): Pnl {
        val typeOf = entries.associate { it.id to it.type }
        val e = entries.filter { it.createdAt in from..to }
        val issued = e.sumOf { if (it.type == "CREDIT" || (it.type == "REVERSAL" && typeOf[it.reversalOf] == "CREDIT")) it.principalDelta - it.tax else 0L }
        val collected = e.sumOf { it.cashDelta }; val income = e.sumOf { it.incomeDelta }
        return Pnl(issued, collected, collected - income, income, e.sumOf { it.lossDelta }, cash.filter { it.createdAt in from..to }.sumOf { it.expenseDelta },
            e.sumOf { if (it.type == "CREDIT" || (it.type == "REVERSAL" && typeOf[it.reversalOf] == "CREDIT")) it.tax else 0L })
    }

    /** Previous comparison window. Month/year use the same elapsed span of the prior month/year so partial periods compare fairly. */
    fun previous(p: Period, now: Long, z: ZoneId = Cur.zone): Pair<Long, Long>? {
        val d = Instant.ofEpochMilli(now).atZone(z).toLocalDate()
        fun ms(x: java.time.LocalDate) = x.atStartOfDay(z).toInstant().toEpochMilli()
        return when (p) {
            Period.ALL -> null
            Period.D30 -> now - 60 * 86_400_000L to now - 30 * 86_400_000L - 1
            Period.MONTH -> { val s = d.withDayOfMonth(1); val ps = ms(s.minusMonths(1)); ps to minOf(ps + (now - ms(s)), ms(s) - 1) }
            Period.YEAR -> { val s = Cur.fyStart(d); val ps = ms(s.minusYears(1)); ps to minOf(ps + (now - ms(s)), ms(s) - 1) }
        }
    }

    /** Amount (principal + fee) of live credits whose due date falls inside the period. */
    fun dueInPeriod(entries: List<Entry>, from: Long, to: Long): Long {
        val reversed = entries.mapNotNull { it.reversalOf }.toSet()
        return entries.filter { it.type in OPENERS && it.id !in reversed && it.dueDate != null && it.dueDate!! in from..to }.sumOf { it.principalDelta + it.feeDelta }
    }
    /** Collection rate % = cash collected in period / amount falling due in period. Null when nothing fell due. Can exceed 100% if older balances are paid. */
    fun collectionRate(entries: List<Entry>, from: Long, to: Long): Double? {
        val due = dueInPeriod(entries, from, to); if (due <= 0) return null
        return entries.filter { it.createdAt in from..to }.sumOf { it.cashDelta }.toDouble() * 100 / due
    }

    fun pnlCsv(p: Pnl, cash: List<CashEntry>, from: Long, to: Long, label: String): String = buildString {
        append(Csv.row("Profit and loss", Csv.text(label)))
        append(Csv.row("Credit issued", Fmt.plain(p.issued))); append(Csv.row("Tax charged (pass-through, not income)", Fmt.plain(p.tax))); append(Csv.row("Cash collected", Fmt.plain(p.collected)))
        append(Csv.row("Principal recovered (not profit)", Fmt.plain(p.recovered))); append(Csv.row("Interest/fee income", Fmt.plain(p.income)))
        append(Csv.row("Bad-debt loss", Fmt.plain(-p.loss))); append(Csv.row("Operating expenses", Fmt.plain(-p.expenses)))
        append(Csv.row("Net realized profit", Fmt.plain(p.net))); append(Csv.row("", ""))
        append(Csv.row("Expenses by category", "Amount"))
        cash.filter { it.kind == "EXPENSE" && it.createdAt in from..to }.groupBy { it.category }.forEach { (k, l) -> append(Csv.row(Csv.text(k), Fmt.plain(l.sumOf { it.expenseDelta }))) }
    }

    fun ledgerCsv(entries: List<Entry>, names: Map<String, String>, from: Long, to: Long, date: (Long) -> String): String = buildString {
        append(Csv.row("Date", "Customer", "Type", "Principal", "Fee", "Cash", "Income", "Loss", "Note", "Reversal of"))
        entries.filter { it.createdAt in from..to }.sortedBy { it.createdAt }.forEach { e ->
            append(Csv.row(date(e.createdAt), Csv.text(names[e.customerId] ?: ""), e.type, Fmt.plain(e.principalDelta), Fmt.plain(e.feeDelta),
                Fmt.plain(e.cashDelta), Fmt.plain(e.incomeDelta), Fmt.plain(e.lossDelta), Csv.text(e.note), e.reversalOf ?: "")) }
    }

    fun agingCsv(a: AgingState, date: (Long) -> String): String = buildString {
        append(Csv.row("Customer", "Balance", "Status", "Days late", "Overdue amount", "Next due", "Last follow-up", "Promise date"))
        a.rows.forEach { r -> append(Csv.row(Csv.text(r.c.name), Fmt.plain(r.balance), r.worst.label, "${r.daysLate}", Fmt.plain(r.overdueAmt),
            r.nextDue?.let(date) ?: "", Csv.text(r.promise?.note ?: ""), r.promise?.promiseDate?.let(date) ?: "")) }
    }
}

object Statements {
    fun lines(c: Customer, entries: List<Entry>, generated: String, date: (Long) -> String): List<String> {
        val out = mutableListOf("Customer: ${c.name}", "Mobile: ${c.phone.ifBlank { "-" }}", "Generated: $generated", "",
            String.format(Locale.US, "%-12s %-10s %14s %14s", "Date", "Type", "Amount (${Cur.code})", "Balance (${Cur.code})"))
        var bal = 0L
        entries.sortedBy { it.createdAt }.forEach { e ->
            val amt = e.principalDelta + e.feeDelta; bal += amt
            out += String.format(Locale.US, "%-12s %-10s %14s %14s", date(e.createdAt), e.type.take(10), Fmt.plain(amt), Fmt.plain(bal))
            if (e.note.isNotBlank()) out += "    ${e.note.take(70)}"
        }
        return out + "" + "Outstanding balance: ${Cur.code} ${Fmt.plain(bal)}"
    }
}

object Export {
    var onExport: ((String) -> Unit)? = null   // set by VM to write an EXPORT audit entry
    private fun dir(ctx: Context) = File(ctx.cacheDir, "exports").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
    private fun safe(s: String) = s.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifBlank { "file" }
    private fun share(ctx: Context, f: File, mime: String) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share ${f.name}"))
    }
    fun csv(ctx: Context, name: String, text: String) { val f = File(dir(ctx), "${safe(name)}.csv"); f.writeText("\uFEFF$text"); share(ctx, f, "text/csv"); onExport?.invoke(f.name) }
    fun table(ctx: Context, xlsx: Boolean, name: String, data: String) {
        if (!xlsx) { csv(ctx, name, data); return }
        val f = File(dir(ctx), "${safe(name)}.xlsx"); f.writeBytes(Xlsx.build(Xlsx.parseCsv(data)))
        share(ctx, f, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); onExport?.invoke(f.name)
    }
    fun pdf(ctx: Context, name: String, heading: String, lines: List<String>) {
        val f = File(dir(ctx), "${safe(name)}.pdf"); val doc = PdfDocument()
        val body = Paint().apply { typeface = Typeface.MONOSPACE; textSize = 10f }
        val head = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 16f }
        var page: PdfDocument.Page? = null; var y = 0f; var n = 0
        fun newPage() { page?.let { doc.finishPage(it) }; n++; page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, n).create()); y = 50f
            if (n == 1) { page!!.canvas.drawText(heading, 40f, y, head); y += 28f } }
        newPage()
        lines.forEach { if (y > 800f) newPage(); page!!.canvas.drawText(it.take(84), 40f, y, body); y += 14f }
        page?.let { doc.finishPage(it) }; f.outputStream().use { doc.writeTo(it) }; doc.close(); share(ctx, f, "application/pdf"); onExport?.invoke(f.name)
    }
    fun statement(ctx: Context, biz: String, c: Customer, entries: List<Entry>) {
        val now = System.currentTimeMillis()
        pdf(ctx, "statement_${c.name}", "$biz - Account statement", Statements.lines(c, entries, now.dmy()) { it.dmy() })
    }
    fun receipt(ctx: Context, biz: String, customer: String, text: String) = pdf(ctx, "receipt_$customer", "$biz - Payment receipt", text.lines().map { it.replace(Cur.symbol, "${Cur.code} ") })
}

@Composable
fun ReportsScreen(vm: VM, back: () -> Unit) {
    val ctx = LocalContext.current
    val entries by vm.allEntries.collectAsState(); val cash by vm.cashEntries.collectAsState()
    val custs by vm.customers.collectAsState(); val aging by vm.aging.collectAsState()
    var period by remember { mutableStateOf(Period.MONTH) }; var xlsx by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis(); val (from, to) = Reports.range(period, now)
    val p = Reports.pnl(entries, cash, from, to); val names = custs.associate { it.c.id to it.c.name }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reports & exports", style = MaterialTheme.typography.titleLarge)
        ChipRow { FilterChip(!xlsx, { xlsx = false }, { Text("CSV") }); FilterChip(xlsx, { xlsx = true }, { Text("Excel (XLSX)") }) }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Period.values().forEach { FilterChip(period == it, { period = it }, { Text(it.label) }) } }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Credit issued" to p.issued, "Tax charged (not income)" to p.tax, "Cash collected" to p.collected, "Principal recovered" to p.recovered, "Interest/fee income" to p.income,
                "Bad-debt loss" to -p.loss, "Expenses" to -p.expenses).forEach { (k, v) -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(k); Text(v.peso()) } }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Net realized profit"); Text(p.net.peso(), color = if (p.net >= 0) Emerald else Danger) } } }
        val d = { t: Long -> Cur.fmt("yyyy-MM-dd HH:mm", t) }
        Button({ Export.table(ctx, xlsx, "profit_loss_${period.name}", Reports.pnlCsv(p, cash, from, to, period.label)) }, Modifier.fillMaxWidth()) { Text("Export profit & loss") }
        Button({ Export.table(ctx, xlsx, "ledger_${period.name}", Reports.ledgerCsv(entries, names, from, to, d)) }, Modifier.fillMaxWidth()) { Text("Export transaction ledger") }
        Button({ Export.table(ctx, xlsx, "aging_report", Reports.agingCsv(aging) { it.dmy() }) }, Modifier.fillMaxWidth()) { Text("Export aging report") }
        Button({ Export.table(ctx, xlsx, "installment_schedule", Plans.csv(entries, names, now) { it.dmy() }) }, Modifier.fillMaxWidth()) { Text("Export installment schedule") }
        Text("Customer statements and receipts are exported as PDF from each customer's page.", style = MaterialTheme.typography.bodySmall)
        TextButton(back) { Text("Back") }
    }
}

@Composable
fun BizDialog(current: String, close: () -> Unit, save: (String) -> Unit) {
    var n by remember { mutableStateOf(current) }
    AlertDialog(close, title = { Text("Business name") }, text = { OutlinedTextField(n, { n = it }, singleLine = true, label = { Text("Shown on receipts and statements") }) },
        confirmButton = { TextButton({ save(n) }, enabled = n.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(close) { Text("Cancel") } })
}
