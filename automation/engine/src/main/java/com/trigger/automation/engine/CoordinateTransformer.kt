package com.trigger.automation.engine

import android.content.Context
import android.graphics.PointF
import android.view.Surface
import android.view.WindowInsets
import android.view.WindowManager
import android.os.Build

/** Converts coordinates local to a source View into natural display coordinates. */
class CoordinateTransformer(private val context: Context) {
    fun toPhysical(viewX: Float, viewY: Float, viewWidth: Int, viewHeight: Int): PointF {
        val dm = context.resources.displayMetrics
        val width = viewWidth.coerceAtLeast(1).toFloat(); val height = viewHeight.coerceAtLeast(1).toFloat()
        val rotation = if (Build.VERSION.SDK_INT >= 30) context.display?.rotation ?: Surface.ROTATION_0 else {
            @Suppress("DEPRECATION") (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        }
        val physicalW = dm.widthPixels.toFloat(); val physicalH = dm.heightPixels.toFloat()
        val x = (viewX / width).coerceIn(0f,1f); val y = (viewY / height).coerceIn(0f,1f)
        val normalized = when(rotation) {
            Surface.ROTATION_90 -> PointF(y, 1f-x)
            Surface.ROTATION_180 -> PointF(1f-x,1f-y)
            Surface.ROTATION_270 -> PointF(1f-y,x)
            else -> PointF(x,y)
        }
        return PointF(normalized.x * physicalW, normalized.y * physicalH)
    }
}
