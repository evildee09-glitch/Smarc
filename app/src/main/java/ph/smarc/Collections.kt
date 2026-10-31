package ph.smarc

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

fun Long.dmy(): String = Cur.fmt(Cur.datePattern, this)

/** Ordered by severity (ordinal). */
enum class Bucket {
    CURRENT, SOON, TODAY, D1_7, D8_30, D31_60, D60P;
    /** Overdue ranges are configurable (Business settings), so the labels are computed. */
    val label: String get() = when (this) { CURRENT -> "Not yet due"; SOON -> "Due soon"; TODAY -> "Due today"; D1_7 -> "Overdue 1-${Cur.t1} days"
        D8_30 -> "Overdue ${Cur.t1 + 1}-${Cur.t2} days"; D31_60 -> "Overdue ${Cur.t2 + 1}-${Cur.t3} days"; D60P -> "Overdue over ${Cur.t3} days" }
}
val OPENERS = setOf("CREDIT", "OPENING", "FEE")   // entry types that create an amount owed with an (optional) due date
data class OpenItem(val customerId: String, val due: Long?, val amount: Long, val id: String = "")
data class CollectRow(val c: Customer, val balance: Long, val worst: Bucket, val daysLate: Long, val overdueAmt: Long, val nextDue: Long?, val promise: FollowUp?)
data class AgingState(val rows: List<CollectRow> = emptyList(), val buckets: Map<Bucket, Long> = emptyMap(), val overdueTotal: Long = 0)

object Aging {
    private fun date(ms: Long, z: ZoneId) = Instant.ofEpochMilli(ms).atZone(z).toLocalDate()
    /** Whole calendar days past due (negative = days remaining). */
    fun daysLate(due: Long, now: Long, z: ZoneId = Cur.zone): Long = ChronoUnit.DAYS.between(date(due, z), date(now, z))

    fun bucket(due: Long?, now: Long, z: ZoneId = Cur.zone, soonDays: Int = 7): Bucket {
        if (due == null) return Bucket.CURRENT
        val d = daysLate(due, now, z)
        return when { d > Cur.t3 -> Bucket.D60P; d > Cur.t2 -> Bucket.D31_60; d > Cur.t1 -> Bucket.D8_30; d >= 1 -> Bucket.D1_7
            d == 0L -> Bucket.TODAY; d >= -soonDays -> Bucket.SOON; else -> Bucket.CURRENT }
    }

    /** Open amount per credit: settled money (payments + write-offs, excluding reversed ones) is applied oldest-due first (FIFO). */
    fun open(entries: List<Entry>): List<OpenItem> = entries.groupBy { it.customerId }.flatMap { (cid, list) ->
        val reversed = list.mapNotNull { it.reversalOf }.toSet()
        val live = list.filter { it.type != "REVERSAL" && it.id !in reversed }
        var settled = live.filter { it.type == "PAYMENT" || it.type == "WRITE_OFF" || (it.type == "ADJUSTMENT" && it.principalDelta + it.feeDelta < 0) }.sumOf { -(it.principalDelta + it.feeDelta) }
        live.filter { it.type in OPENERS || (it.type in setOf("ADJUSTMENT", "REFUND") && it.principalDelta + it.feeDelta > 0) }.sortedWith(compareBy(nullsLast<Long>()) { e: Entry -> e.dueDate }).mapNotNull { cr ->
            val amt = cr.principalDelta + cr.feeDelta; val applied = minOf(settled, amt); settled -= applied
            (amt - applied).takeIf { it > 0 }?.let { OpenItem(cid, cr.dueDate, it, cr.id) }
        }
    }

    fun build(entries: List<Entry>, customers: List<Customer>, follow: List<FollowUp>, now: Long, z: ZoneId = Cur.zone, soonDays: Int = 7): AgingState {
        val open = open(entries); val byCust = open.groupBy { it.customerId }
        val lastFu = follow.groupBy { it.customerId }.mapValues { (_, l) -> l.maxBy { it.createdAt } }
        val buckets = Bucket.values().associateWith { b -> open.filter { bucket(it.due, now, z, soonDays = soonDays) == b }.sumOf { it.amount } }
        val rows = customers.mapNotNull { c ->
            val items = byCust[c.id] ?: return@mapNotNull null
            val cls = items.map { it to bucket(it.due, now, z, soonDays = soonDays) }
            CollectRow(c, items.sumOf { it.amount }, cls.maxBy { it.second.ordinal }.second,
                items.mapNotNull { it.due }.maxOfOrNull { daysLate(it, now, z) }?.coerceAtLeast(0) ?: 0,
                cls.filter { it.second.ordinal >= Bucket.D1_7.ordinal }.sumOf { it.first.amount },
                items.mapNotNull { it.due }.filter { daysLate(it, now, z) <= 0 }.minOrNull(), lastFu[c.id])
        }.sortedWith(compareByDescending<CollectRow> { it.daysLate }.thenByDescending { it.overdueAmt })
        return AgingState(rows, buckets, buckets.filterKeys { it.ordinal >= Bucket.D1_7.ordinal }.values.sum())
    }
}

@Composable
fun CollectionsScreen(vm: VM, openCalendar: () -> Unit, open: (String) -> Unit) {
    val a by vm.aging.collectAsState(); val ctx = LocalContext.current
    var target by remember { mutableStateOf<CollectRow?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(openCalendar) { Text("Open collection calendar") }
        Text("Account aging", style = MaterialTheme.typography.titleMedium)
        Bucket.values().forEach { b -> val v = a.buckets[b] ?: 0L
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(b.label); Text(v.peso(), color = if (b.ordinal >= Bucket.D1_7.ordinal && v > 0) Danger else MaterialTheme.colorScheme.primary) } }
        HorizontalDivider()
        Text("Priority collection list", style = MaterialTheme.typography.titleMedium)
        if (a.rows.isEmpty()) Text("Nothing outstanding.")
        LazyColumn { items(a.rows) { r ->
            val p = r.promise?.promiseDate
            val missed = p != null && Aging.daysLate(p, System.currentTimeMillis()) > 0
            ListItem(headlineContent = { Text(r.c.name) },
                supportingContent = { Text(buildString {
                    append(r.worst.label); if (r.daysLate > 0) append(" • ${r.daysLate}d late")
                    if (p != null) append(if (missed) " • promise MISSED (${p.dmy()})" else " • promised ${p.dmy()}")
                    r.promise?.note?.takeIf { it.isNotBlank() }?.let { append("\n$it") } }) },
                trailingContent = { Text(r.balance.peso(), color = if (r.daysLate > 0) Danger else MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { open(r.c.id) })
            Row {
                TextButton({ target = r }) { Text("Log follow-up") }
                if (r.c.phone.isNotBlank()) TextButton({   // opens the SMS app pre-filled; the owner reviews and sends (consent stays with the owner)
                    val body = "Hello ${r.c.name}, this is a friendly reminder of your outstanding balance of ${r.balance.peso()}. Thank you!"
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(r.c.phone)}")).putExtra("sms_body", body)) }) { Text("SMS reminder") } }
            HorizontalDivider() } }
    }
    target?.let { r -> FollowUpDialog(r.c.name, { target = null }) { note, days -> target = null
        vm.launch { vm.dao.insertFollowUp(FollowUp(customerId = r.c.id, note = note, promiseDate = days?.let { System.currentTimeMillis() + it * 86_400_000L })) } } }
}

@Composable
fun FollowUpDialog(name: String, close: () -> Unit, save: (String, Long?) -> Unit) {
    var note by remember { mutableStateOf("") }; var days by remember { mutableStateOf("") }
    AlertDialog(close, title = { Text("Follow-up: $name") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(note, { note = it }, label = { Text("What happened? (call, visit, message)") })
        OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text("Promise to pay in (days, optional)") }, singleLine = true)
        Text("Only contact customers who agreed to be contacted.", style = MaterialTheme.typography.bodySmall) } },
        confirmButton = { TextButton({ save(note.trim(), days.toLongOrNull()) }, enabled = note.isNotBlank() || days.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}
