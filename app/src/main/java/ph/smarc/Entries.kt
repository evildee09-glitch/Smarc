package ph.smarc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaymentDialog(close: () -> Unit, save: (Long, String, String, String) -> Unit) {
    var amt by remember { mutableStateOf("") }; var method by remember { mutableStateOf("Cash") }; var ref by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    val a = parsePeso(amt)
    AlertDialog(close, title = { Text("Record payment") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(amt, { amt = it }, label = { Text("Amount received (${Cur.symbol})") }, singleLine = true, isError = amt.isNotBlank() && a == null)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Cash", "Bank transfer", "E-wallet", "Other").forEach { FilterChip(method == it, { method = it }, { Text(it) }) } }
            if (method != "Cash") OutlinedTextField(ref, { ref = it }, label = { Text("Payment reference") }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text("Note") }) } },
        confirmButton = { TextButton({ save(a!!, method, ref.trim(), note.trim()) }, enabled = a != null && a > 0) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

/** kind: "opening" (balance owed before using SmarC), "fee" (fee/interest assessment), "adjust" (authorized correction, reason required). */
@Composable
fun EntryDialog(kind: String, close: () -> Unit, save: (Long, Long, Long?, Boolean, String) -> Unit) {
    var p by remember { mutableStateOf("") }; var f by remember { mutableStateOf("") }; var days by remember { mutableStateOf("") }
    var up by remember { mutableStateOf(true) }; var reason by remember { mutableStateOf("") }
    val pv = if (kind == "fee" || p.isBlank()) 0L else parsePeso(p); val fv = if (f.isBlank()) 0L else parsePeso(f)
    val valid = pv != null && fv != null && pv + fv > 0 && reason.isNotBlank()
    AlertDialog(close, title = { Text(when (kind) { "opening" -> "Opening balance"; "fee" -> "Add fee / interest"; "refund" -> "Refund cash to customer"; else -> "Adjust balance" }) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (kind == "adjust") { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(up, { up = true }, { Text("Increase") }); FilterChip(!up, { up = false }, { Text("Decrease") }) }
                Text("Adjustments correct amounts that were recorded wrongly. To forgive a debt, use Write off.", style = MaterialTheme.typography.bodySmall) }
            if (kind != "fee") OutlinedTextField(p, { p = it }, label = { Text(if (kind == "refund") "Refund amount (${Cur.symbol})" else "Principal (${Cur.symbol})") }, singleLine = true, isError = pv == null)
            if (kind != "refund") OutlinedTextField(f, { f = it }, label = { Text("Interest / fee (${Cur.symbol})") }, singleLine = true, isError = fv == null)
            if (kind != "adjust" && kind != "refund") OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text("Due in (days, optional)") }, singleLine = true)
            OutlinedTextField(reason, { reason = it }, label = { Text("Reason (required, kept in the ledger)") }) } },
        confirmButton = { TextButton({ save(pv!!, fv!!, days.toLongOrNull(), up, reason.trim()) }, enabled = valid) { Text("Save") } },
        dismissButton = { TextButton(close) { Text("Cancel") } })
}

object Receipts {
    /** Rebuilds a past payment receipt from the ledger (balance is as of that payment). */
    fun rebuild(e: Entry, all: List<Entry>, customer: String): String {
        val remaining = all.filter { it.createdAt < e.createdAt || (it.createdAt == e.createdAt && it.id <= e.id) }.sumOf { it.principalDelta + it.feeDelta }
        val voided = all.any { it.reversalOf == e.id }
        return "SmarC RECEIPT (REPRINT)\nReceipt No: ${e.receiptNo.orNo()}\nDate: ${e.createdAt.dmy()}\nCustomer: $customer\nDetails: ${e.note}${if (e.reference.isNotBlank()) " (ref ${e.reference})" else ""}\nReceived: ${e.cashDelta.peso()}\nApplied to fees/interest: ${e.incomeDelta.peso()}\nApplied to principal: ${(-e.principalDelta).peso()}\nBalance after this payment: ${remaining.peso()}\nStatus: ${if (voided) "VOIDED (payment was reversed)" else "PAID"}"
    }
}
