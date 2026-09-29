package com.trigger.automation.engine

import org.junit.Assert.*
import org.junit.Test

class ExecutionLogTest {
    @Test fun redactSensitiveTextAndPackageParameters() {
        val log=ExecutionLog();log.record(ExecutionLogEntry(1,"com.vendor.game",0,"STEP_FAILED",message="token=secret123 package=com.vendor.game email=person@example.com"))
        val entry=log.snapshot().single()
        assertFalse(entry.message.orEmpty().contains("secret123"));assertFalse(entry.message.orEmpty().contains("com.vendor.game"));assertFalse(entry.message.orEmpty().contains("person@example.com"))
        assertFalse(entry.macroId.contains("com.vendor.game"))
    }
}
