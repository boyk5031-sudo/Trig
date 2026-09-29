package com.trigger.automation.engine

import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.view.Surface
import android.view.WindowManager

data class DisplayInsets(val left:Int=0,val top:Int=0,val right:Int=0,val bottom:Int=0)

/** Converts source-view coordinates into physical display pixels, accounting for rotation and cutout insets. */
class CoordinateTransformer private constructor(
    private val width:Int,
    private val height:Int,
    private val rotation:Int,
    private val insets:DisplayInsets,
    @Suppress("UNUSED_PARAMETER") marker:Unit
) {
    constructor(context:Context):this(
        context.resources.displayMetrics.widthPixels.coerceAtLeast(1),
        context.resources.displayMetrics.heightPixels.coerceAtLeast(1),
        displayRotation(context),DisplayInsets(),Unit
    )
    /** Pure geometry constructor, also useful for validating real device display transforms. */
    constructor(width:Int,height:Int,rotation:Int,insets:DisplayInsets=DisplayInsets()):this(width.coerceAtLeast(1),height.coerceAtLeast(1),rotation,insets,Unit)

    fun toPhysical(viewX:Float,viewY:Float,viewWidth:Int,viewHeight:Int):PointF {
        val x=(viewX/viewWidth.coerceAtLeast(1)).coerceIn(0f,1f)
        val y=(viewY/viewHeight.coerceAtLeast(1)).coerceIn(0f,1f)
        val (u,v)=when(rotation) {
            Surface.ROTATION_90 -> y to (1f-x)
            Surface.ROTATION_180 -> (1f-x) to (1f-y)
            Surface.ROTATION_270 -> (1f-y) to x
            else -> x to y
        }
        val contentWidth=(width-insets.left-insets.right).coerceAtLeast(1)
        val contentHeight=(height-insets.top-insets.bottom).coerceAtLeast(1)
        return PointF(insets.left+u*contentWidth,insets.top+v*contentHeight)
    }
    companion object {
        private fun displayRotation(context:Context):Int = try {
            if(Build.VERSION.SDK_INT>=30) context.display?.rotation ?: Surface.ROTATION_0 else {
                @Suppress("DEPRECATION") (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
            }
        } catch (_:Exception) { Surface.ROTATION_0 }
    }
}
