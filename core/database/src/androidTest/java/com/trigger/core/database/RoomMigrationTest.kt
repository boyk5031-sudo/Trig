package com.trigger.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    @Test fun migrationOneToTwoCreatesIndexedStepTable() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val callback=object:SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db:androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS `macros` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `packageName` TEXT NOT NULL, `stepsJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))") }
            override fun onUpgrade(db:androidx.sqlite.db.SupportSQLiteDatabase,oldVersion:Int,newVersion:Int) { }
        }
        val config=SupportSQLiteOpenHelper.Configuration.builder(context).name(null).callback(callback).build()
        val helper=FrameworkSQLiteOpenHelperFactory().create(config)
        try {
            val db=helper.writableDatabase
            TriggerDatabase.MIGRATION_1_2.migrate(db)
            val table=db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='macro_steps'")
            table.use { assertEquals(true,it.moveToFirst()) }
            val index=db.query("SELECT name FROM sqlite_master WHERE type='index' AND name='index_macro_steps_macroId'")
            index.use { assertEquals(true,it.moveToFirst()) }
        } finally { helper.close() }
    }
}
