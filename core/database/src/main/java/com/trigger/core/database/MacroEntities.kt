package com.trigger.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName="macros")
data class MacroEntity(@PrimaryKey val id:String,val name:String,val packageName:String,val stepsJson:String,val updatedAt:Long)

@Entity(tableName="macro_steps",primaryKeys=["macroId","stepIndex"],foreignKeys=[ForeignKey(entity=MacroEntity::class,parentColumns=["id"],childColumns=["macroId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["macroId"])])
data class MacroStepEntity(val macroId:String,val stepIndex:Int,val stepJson:String)
