package ph.smarc

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Minimal XLSX writer (no library): numbers stay numbers, text is stored as inline strings so nothing can execute as a formula. */
object Xlsx {
    fun parseCsv(s: String): List<List<String>> {
        val rows = mutableListOf<List<String>>(); var row = mutableListOf<String>(); val cell = StringBuilder(); var q = false; var i = 0
        val t = s.removePrefix("\uFEFF")
        while (i < t.length) { val ch = t[i]
            when {
                q && ch == '"' -> if (i + 1 < t.length && t[i + 1] == '"') { cell.append('"'); i++ } else q = false
                q -> cell.append(ch)
                ch == '"' -> q = true
                ch == ',' -> { row.add(cell.toString()); cell.clear() }
                ch == '\r' -> {}
                ch == '\n' -> { row.add(cell.toString()); cell.clear(); rows.add(row); row = mutableListOf() }
                else -> cell.append(ch)
            }
            i++ }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
        return rows
    }
    private fun col(i: Int): String { var n = i + 1; val sb = StringBuilder(); while (n > 0) { val r = (n - 1) % 26; sb.insert(0, 'A' + r); n = (n - 1) / 26 }; return sb.toString() }
    private fun esc(s: String) = s.filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private val num = Regex("-?(0|[1-9]\\d{0,14})(\\.\\d+)?")
    private fun sheetXml(rows: List<List<String>>) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        rows.forEachIndexed { r, cells -> append("<row r=\"${r + 1}\">")
            cells.forEachIndexed { c, raw -> val ref = col(c) + (r + 1)
                val v = if (raw.length > 1 && raw[0] == '\'' && raw[1] in "=+-@\t\r") raw.substring(1) else raw   // drop the CSV formula-guard apostrophe
                when { v.isEmpty() -> {}; num.matches(v) -> append("<c r=\"$ref\"><v>$v</v></c>")
                    else -> append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>") } }
            append("</row>") }
        append("</sheetData></worksheet>")
    }
    fun build(rows: List<List<String>>, sheet: String = "Report"): ByteArray {
        val out = ByteArrayOutputStream(); val name = esc(sheet.filter { it !in "[]:*?/\\" }.take(31).ifBlank { "Report" })
        ZipOutputStream(out).use { z ->
            fun put(n: String, text: String) { z.putNextEntry(ZipEntry(n)); z.write(text.toByteArray(Charsets.UTF_8)); z.closeEntry() }
            put("[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>")
            put("_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>")
            put("xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"$name\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>")
            put("xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>")
            put("xl/worksheets/sheet1.xml", sheetXml(rows))
        }
        return out.toByteArray()
    }
}
