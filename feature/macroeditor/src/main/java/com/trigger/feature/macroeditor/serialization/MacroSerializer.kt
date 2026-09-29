package com.trigger.feature.macroeditor.serialization

import com.trigger.feature.macroeditor.model.Macro
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object MacroSerializer {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = false; classDiscriminator = "type"; encodeDefaults = true }
    fun encode(macro: Macro): String = json.encodeToString(Macro.serializer(), macro)
    @Throws(SerializationException::class)
    fun decode(source: String): Macro = json.decodeFromString(Macro.serializer(), source)
}
