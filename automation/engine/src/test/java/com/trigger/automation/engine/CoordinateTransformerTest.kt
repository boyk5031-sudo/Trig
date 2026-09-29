package com.trigger.automation.engine

import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class CoordinateTransformerTest {
    @Test fun mapsAllFourRotations() {
        val expected=listOf(25f to 50f,25f to 150f,75f to 150f,75f to 50f)
        val rotations=listOf(Surface.ROTATION_0,Surface.ROTATION_90,Surface.ROTATION_180,Surface.ROTATION_270)
        rotations.forEachIndexed { index,rotation ->
            val point=CoordinateTransformer(100,200,rotation).toPhysical(25f,50f,100,200)
            assertEquals(expected[index].first,point.x,.01f); assertEquals(expected[index].second,point.y,.01f)
        }
    }
    @Test fun mapsAroundNotchAndSystemInsets() {
        val transform=CoordinateTransformer(100,200,Surface.ROTATION_0,DisplayInsets(left=10,top=20,right=30,bottom=40))
        val origin=transform.toPhysical(0f,0f,100,200); val end=transform.toPhysical(100f,200f,100,200)
        assertEquals(10f,origin.x,.01f); assertEquals(20f,origin.y,.01f)
        assertEquals(70f,end.x,.01f); assertEquals(160f,end.y,.01f)
    }
    @Test fun clampsCoordinatesToDisplayBounds() {
        val point=CoordinateTransformer(100,200,Surface.ROTATION_0).toPhysical(500f,-20f,100,200)
        assertEquals(100f,point.x,.01f);assertEquals(0f,point.y,.01f)
    }
}
