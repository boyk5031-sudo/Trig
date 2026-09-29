package com.trigger.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities=[MacroEntity::class,MacroStepEntity::class],version=2,exportSchema=true)
abstract class TriggerDatabase:RoomDatabase() {
    abstract fun macroDao():MacroDao
    companion object {
        val MIGRATION_1_2=object:Migration(1,2) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `macro_steps` (`macroId` TEXT NOT NULL, `stepIndex` INTEGER NOT NULL, `stepJson` TEXT NOT NULL, PRIMARY KEY(`macroId`, `stepIndex`), FOREIGN KEY(`macroId`) REFERENCES `macros`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_macro_steps_macroId` ON `macro_steps` (`macroId`)")
            }
        }
        @Volatile private var instance:TriggerDatabase?=null
        fun get(context:Context):TriggerDatabase=instance ?: synchronized(this) { instance ?: Room.databaseBuilder(context.applicationContext,TriggerDatabase::class.java,"trigger.db").addMigrations(MIGRATION_1_2).build().also { instance=it } }
    }
}
