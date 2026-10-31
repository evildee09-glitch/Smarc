package ph.smarc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp

fun Long.stamp(): String = Cur.fmt("MMM d, HH:mm", this)

object AuditCsv {
    fun build(list: List<AuditLog>, date: (Long) -> String): String = buildString {
        append(Csv.row("Time", "Actor", "Action", "Detail"))
        list.sortedBy { it.createdAt }.forEach { append(Csv.row(date(it.createdAt), Csv.text(it.actor), it.action, Csv.text(it.detail))) }
    }
}

@Composable
fun AuditScreen(vm: VM, back: () -> Unit) {
    val list by vm.audit.collectAsState(); val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Audit log", style = MaterialTheme.typography.titleLarge); TextButton(back) { Text("Back") } }
        Text("Credits, payments, reversals, write-offs, capital, expenses, customer changes, PIN changes and exports. Entries cannot be edited or deleted.", style = MaterialTheme.typography.bodySmall)
        Button({ Export.csv(ctx, "audit_log", AuditCsv.build(list) { it.stamp() }) }, enabled = list.isNotEmpty()) { Text("Export audit log (CSV)") }
        if (list.isEmpty()) Text("No activity recorded yet.")
        LazyColumn { items(list) { a ->
            ListItem(headlineContent = { Text(a.action) }, supportingContent = { Text("${a.actor}${if (a.detail.isNotBlank()) " • " + a.detail else ""}") },
                trailingContent = { Text(a.createdAt.stamp(), style = MaterialTheme.typography.labelSmall) })
            HorizontalDivider() } }
    }
}
