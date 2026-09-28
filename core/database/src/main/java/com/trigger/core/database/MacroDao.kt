package com.trigger.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Relation
import androidx.room.Embedded
import kotlinx.coroutines.flow.Flow

@Dao interface MacroDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertMacro(macro:MacroEntity)
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertSteps(steps:List<MacroStepEntity>)
    @Query("DELETE FROM macro_steps WHERE macroId=:macroId") suspend fun deleteSteps(macroId:String)
    @Query("DELETE FROM macros WHERE id=:macroId") suspend fun deleteMacro(macroId:String)
    @Query("SELECT * FROM macros ORDER BY updatedAt DESC") fun observeMacros():Flow<List<MacroEntity>>
    @Transaction @Query("SELECT * FROM macros WHERE id=:macroId") suspend fun getWithSteps(macroId:String):MacroWithSteps?
    @Transaction suspend fun save(macro:MacroEntity,steps:List<MacroStepEntity>) { insertMacro(macro);deleteSteps(macro.id);if(steps.isNotEmpty()) insertSteps(steps) }
}
data class MacroWithSteps(@Embedded val macro:MacroEntity,@Relation(parentColumn="id",entityColumn="macroId") val steps:List<MacroStepEntity>)
