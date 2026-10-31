package ph.smarc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val OTHERS = setOf("OPENING", "FEE", "ADJUSTMENT")
/** Balance = creditIssued + other(opening/fees/adjustments) + feesAssessed - repaid - writtenOff (always true; verified in tests). */
data class Breakdown(val creditIssued: Long, val feesAssessed: Long, val repaid: Long, val writtenOff: Long, val principalDue: Long, val feeDue: Long, val other: Long = 0) {
    val balance get() = principalDue + feeDue
}

object Profile {
    fun breakdown(entries: List<Entry>): Breakdown {
        val typeOf = entries.associate { it.id to it.type }
        val credits = entries.filter { it.type == "CREDIT" || (it.type == "REVERSAL" && typeOf[it.reversalOf] == "CREDIT") }
        return Breakdown(credits.sumOf { it.principalDelta }, credits.sumOf { it.feeDelta }, entries.sumOf { it.cashDelta },
            -entries.filter { it.type == "WRITE_OFF" }.sumOf { it.principalDelta + it.feeDelta }, entries.sumOf { it.principalDelta }, entries.sumOf { it.feeDelta },
            entries.filter { it.type in OTHERS || (it.type == "REVERSAL" && typeOf[it.reversalOf] in OTHERS) }.sumOf { it.principalDelta + it.feeDelta })
    }
}

object Dupes {
    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
    private fun digits(s: String) = s.filter { it.isDigit() }.takeLast(10)
    fun lev(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) { val cur = IntArray(b.length + 1); cur[0] = i
            for (j in 1..b.length) cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = cur }
        return prev[b.length]
    }
    /** Warning only (never blocks): same normalized name, near-identical long name, or same phone (last 10 digits). */
    fun find(name: String, phone: String, others: List<Customer>, selfId: String? = null): List<Customer> {
        val n = norm(name); val p = digits(phone)
        return others.filter { it.id != selfId }.filter { o -> val on = norm(o.name)
            (n.isNotEmpty() && (n == on || (n.length >= 5 && lev(n, on) <= 1))) || (p.length >= 7 && p == digits(o.phone)) }
    }
}

object CustomerView {
    /** flt: 0 all, 1 overdue, 2 owing, 3 fully paid. sort: 0 name, 1 balance (high first), 2 next due. */
    fun apply(list: List<CustomerSummary>, rows: Map<String, CollectRow>, q: String, flt: Int, sort: Int): List<CustomerSummary> =
        list.filter { s -> (q.isBlank() || s.c.name.contains(q, true) || s.c.accountNo.contains(q, true) || s.c.phone.contains(q)) &&
            when (flt) { 1 -> (rows[s.c.id]?.daysLate ?: 0) > 0; 2 -> s.balance > 0; 3 -> s.balance == 0L; else -> true } }
            .let { l -> when (sort) { 1 -> l.sortedByDescending { it.balance }; 2 -> l.sortedBy { rows[it.c.id]?.nextDue ?: Long.MAX_VALUE }
                else -> l.sortedBy { it.c.name.lowercase() } } }
}

@Composable private fun KV(k: String, v: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(k); Text(v) }

@Composable
fun ProfileCard(c: Customer, entries: List<Entry>, photoVersion: Int = 0, onPhoto: (() -> Unit)? = null) {
    val b = remember(entries) { Profile.breakdown(entries) }; val open = remember(entries) { Aging.open(entries) }
    var more by remember { mutableStateOf(false) }; val now = System.currentTimeMillis()
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Avatar("c_${c.id}", 56.dp, photoVersion, c.name.take(1), onPhoto); Text(c.name, style = MaterialTheme.typography.headlineSmall) }
        if (c.status != "ACTIVE") Text(c.status, color = Danger, style = MaterialTheme.typography.labelMedium)
        val contact = listOf(c.phone, c.email, if (c.accountNo.isBlank()) "" else "Acct ${c.accountNo}").filter { it.isNotBlank() }.joinToString(" • ")
        if (contact.isNotEmpty()) Text(contact, style = MaterialTheme.typography.bodySmall)
        Text("Outstanding ${b.balance.peso()}", style = MaterialTheme.typography.titleLarge, color = if (b.balance > 0) MaterialTheme.colorScheme.primary else Emerald)
        if (c.creditLimit > 0) Text("Limit ${c.creditLimit.peso()} • Available ${maxOf(0, c.creditLimit - b.balance).peso()} • Used ${b.balance * 100 / c.creditLimit}%", style = MaterialTheme.typography.bodySmall)
        TextButton({ more = !more }) { Text(if (more) "Hide breakdown" else "Show breakdown") }
        if (more) {
            KV("Credit issued (principal)", b.creditIssued.peso()); KV("Interest / fees assessed", b.feesAssessed.peso()); if (b.other != 0L) KV("Opening / fees / adjustments", b.other.peso())
            KV("Repayments received", b.repaid.peso()); KV("Written off", b.writtenOff.peso()); HorizontalDivider()
            KV("Principal outstanding", b.principalDue.peso()); KV("Fees outstanding", b.feeDue.peso())
            if (open.isNotEmpty()) { Text("Open credits", style = MaterialTheme.typography.titleSmall)
                open.forEach { i -> val d = i.due?.let { Aging.daysLate(it, now) }
                    KV(i.amount.peso(), when { d == null -> "no due date"; d > 0 -> "overdue $d days"; d == 0L -> "due today"; else -> "due ${i.due!!.dmy()}" }) } }
            Plans.build(entries).forEach { p -> Text("Installment plan", style = MaterialTheme.typography.titleSmall)
                p.items.forEach { i -> KV("${i.no}/${i.total} • ${i.amount.peso()}", Plans.status(i, now)) } }
            if (c.notes.isNotBlank()) Text("Notes: ${c.notes}", style = MaterialTheme.typography.bodySmall)
        } } }
}

@Composable
fun CustomerDialog(initial: Customer?, others: List<Customer>, close: () -> Unit, save: (Customer) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }; var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }; var email by remember { mutableStateOf(initial?.email ?: "") }
    var acct by remember { mutableStateOf(initial?.accountNo ?: "") }; var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var limit by remember { mutableStateOf(if ((initial?.creditLimit ?: 0) > 0) Fmt.plain(initial!!.creditLimit) else "") }
    var status by remember { mutableStateOf(initial?.status ?: "ACTIVE") }
    val lim = if (limit.isBlank()) 0L else parsePeso(limit); val dupes = Dupes.find(name, phone, others, initial?.id)
    AlertDialog(close, title = { Text(if (initial == null) "Add customer" else "Edit customer") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Mobile number") }, singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("Email (optional)") }, singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("Address") })
            OutlinedTextField(acct, { acct = it }, label = { Text("Account / reference no. (optional)") }, singleLine = true)
            OutlinedTextField(limit, { limit = it }, label = { Text("Credit limit (${Cur.symbol}, blank = none)") }, singleLine = true, isError = lim == null)
            OutlinedTextField(notes, { notes = it }, label = { Text("Internal notes") })
            if (initial != null) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ACTIVE", "INACTIVE", "RESTRICTED", "ARCHIVED").forEach { FilterChip(status == it, { status = it }, { Text(it.lowercase().replaceFirstChar(Char::uppercase)) }) } }
            if (dupes.isNotEmpty()) Text("Possible duplicate: ${dupes.joinToString { it.name }}", color = Danger, style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { TextButton({ save((initial ?: Customer(name = "")).copy(name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim(),
            accountNo = acct.trim(), notes = notes.trim(), creditLimit = lim ?: 0, status = status)) }, enabled = name.isNotBlank() && lim != null) {
            Text(if (dupes.isNotEmpty()) "Save anyway" else "Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

@Composable
fun ReasonDialog(title: String, close: () -> Unit, confirm: (String) -> Unit) {
    var r by remember { mutableStateOf("") }
    AlertDialog(close, title = { Text(title) }, text = { OutlinedTextField(r, { r = it }, label = { Text("Reason (required, kept in the ledger)") }) },
        confirmButton = { TextButton({ confirm(r.trim()) }, enabled = r.isNotBlank()) { Text("Confirm") } }, dismissButton = { TextButton(close) { Text("Cancel") } })
}
