package com.trigger.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MacroDaoTest {
    private lateinit var database:TriggerDatabase
    private lateinit var dao:MacroDao
    @Before fun setUp() { val context=ApplicationProvider.getApplicationContext<Context>();database=Room.inMemoryDatabaseBuilder(context,TriggerDatabase::class.java).allowMainThreadQueries().build();dao=database.macroDao() }
    @After fun tearDown() { database.close() }
    @Test fun savesMacroAndOrderedSteps()=runBlocking {
        val macro=MacroEntity("m1","Test macro","example.game","[]",10)
        dao.save(macro,listOf(MacroStepEntity("m1",0,"tap"),MacroStepEntity("m1",1,"wait")))
        val result=dao.getWithSteps("m1")!!
        assertEquals("Test macro",result.macro.name);assertEquals(listOf(0,1),result.steps.sortedBy { it.stepIndex }.map { it.stepIndex })
        assertEquals(1,dao.observeMacros().first().size)
    }
    @Test fun deletingMacroCascadesToSteps()=runBlocking {
        dao.save(MacroEntity("m2","Delete","pkg","[]",20),listOf(MacroStepEntity("m2",0,"tap")))
        dao.deleteMacro("m2")
        val cursor=database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM macro_steps")
        cursor.use { it.moveToFirst();assertEquals(0,it.getInt(0)) }
    }
}
