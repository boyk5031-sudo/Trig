package com.trigger.automation.engine

import java.util.concurrent.CopyOnWriteArrayList

data class ExecutionLogEntry(
    val timestampMs: Long,
    val macroId: String,
    val stepIndex: Int?,
    val phase: String,
    val durationMs: Long? = null,
    val failureReason: ExecutionFailureReason? = null,
    val message: String? = null
)
class ExecutionLog(private val maxEntries: Int = 1000) {
    private val entries=CopyOnWriteArrayList<ExecutionLogEntry>()
    fun record(entry: ExecutionLogEntry) {
        val sanitized=entry.copy(macroId=sanitizeIdentifier(entry.macroId),message=entry.message?.let(::redactText))
        entries.add(sanitized); while(entries.size>maxEntries.coerceAtLeast(1)) entries.removeAt(0); ExecutionRuntime.append(sanitized)
    }
    fun snapshot(): List<ExecutionLogEntry> = entries.toList()
    fun clear() = entries.clear()
    private fun sanitizeIdentifier(value:String):String = if(value.matches(Regex("[A-Za-z0-9_-]{1,80}"))) value else "id-${value.hashCode().toUInt().toString(16)}"
    private fun redactText(value:String):String {
        val secrets=Regex("""(?i)\b(password|passwd|token|secret|authorization|api[_-]?key|email|phone|variable)\b\s*[:=]\s*("[^"]*"|'[^']*'|[^\s,;]+)""")
        val packageNames=Regex("""(?<![A-Za-z0-9_])(?:[A-Za-z][A-Za-z0-9_]*\.){2,}[A-Za-z][A-Za-z0-9_]*""")
        return packageNames.replace(secrets.replace(value,"$1=[REDACTED]"),"[PACKAGE]").take(300)
    }
}
