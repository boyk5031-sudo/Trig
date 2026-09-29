package com.trigger.app.navigation

import android.content.Context
import com.trigger.feature.macroeditor.model.Macro
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

class MacroCatalog(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("macro_catalog",Context.MODE_PRIVATE)
    private val json=Json { ignoreUnknownKeys=true }
    @Synchronized fun all():List<Macro> = try { json.decodeFromString(ListSerializer(Macro.serializer()),prefs.getString(KEY,"[]") ?: "[]") } catch (_:Exception) { emptyList() }
    @Synchronized fun get(id:String?):Macro? = all().firstOrNull { it.id==id }
    @Synchronized fun save(macro:Macro) { val entries=all().filterNot { it.id==macro.id }+macro; prefs.edit().putString(KEY,json.encodeToString(ListSerializer(Macro.serializer()),entries)).apply() }
    @Synchronized fun create():Macro=Macro(UUID.randomUUID().toString(),"New macro","",emptyList()).also(::save)
    companion object { private const val KEY="macros" }
}
