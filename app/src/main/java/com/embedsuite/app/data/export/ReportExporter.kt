package com.embedsuite.app.data.export

data class ExportResult(
    val format: ExportFormat,
    val content: String,
    val suggestedFileName: String
)

enum class ExportFormat {
    HTML,
    JSON
}

interface ReportExporter {
    suspend fun exportHtml(title: String, sections: Map<String, String>): ExportResult
    suspend fun exportJson(title: String, payload: Map<String, Any>): ExportResult
}

class StubReportExporter : ReportExporter {

    override suspend fun exportHtml(title: String, sections: Map<String, String>): ExportResult {
        val body = buildString {
            append("<html><head><title>").append(title).append("</title></head><body>")
            sections.forEach { (heading, text) ->
                append("<h2>").append(heading).append("</h2><pre>")
                append(text)
                append("</pre>")
            }
            append("</body></html>")
        }
        return ExportResult(
            format = ExportFormat.HTML,
            content = body,
            suggestedFileName = "${title.replace(' ', '_').lowercase()}.html"
        )
    }

    override suspend fun exportJson(title: String, payload: Map<String, Any>): ExportResult {
        val json = org.json.JSONObject(
            mapOf("title" to title) + payload.mapValues { it.value.toString() }
        ).toString(2)
        return ExportResult(
            format = ExportFormat.JSON,
            content = json,
            suggestedFileName = "${title.replace(' ', '_').lowercase()}.json"
        )
    }
}
