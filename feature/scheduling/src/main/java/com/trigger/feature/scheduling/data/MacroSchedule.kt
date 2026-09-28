package com.trigger.feature.scheduling.data

import kotlinx.serialization.Serializable

@Serializable data class MacroSchedule(
    val id:String,
    val macroName:String,
    val macroJson:String,
    val intervalMinutes:Long=15,
    val initialDelayMinutes:Long=0,
    val requiresCharging:Boolean=false,
    val enabled:Boolean=true,
    val workId:String?=null
)
