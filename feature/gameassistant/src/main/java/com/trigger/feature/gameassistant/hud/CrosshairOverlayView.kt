package com.trigger.feature.gameassistant.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.View
import java.util.concurrent.atomic.AtomicReference

enum class CrosshairStyle { DOT, CROSSHAIR, CIRCLE_DOT, OPEN_CROSS }
data class CrosshairConfig(val style: CrosshairStyle = CrosshairStyle.CROSSHAIR, val sizeDp: Float = 20f, val color: Int = Color.GREEN, val opacity: Float = 1f, val offsetXDp: Float = 0f, val offsetYDp: Float = 0f)
/** Full-screen transparent, zero-touch view: it never consumes game input. */
class CrosshairOverlayView(context: Context) : View(context) {
    private val config = AtomicReference(CrosshairConfig())
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    fun update(value: CrosshairConfig) { config.set(value.copy(sizeDp = value.sizeDp.coerceIn(8f, 40f), opacity = value.opacity.coerceIn(0f, 1f))); postInvalidateOnAnimation() }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); val c = config.get(); val d = resources.displayMetrics.density; val cx = width / 2f + c.offsetXDp*d; val cy = height / 2f + c.offsetYDp*d; val r = c.sizeDp*d/2f
        paint.color = c.color; paint.alpha = (255*c.opacity).toInt(); paint.strokeWidth = (2*d).coerceAtLeast(1f)
        when(c.style) { CrosshairStyle.DOT -> { paint.style = Paint.Style.FILL; canvas.drawCircle(cx,cy,(r/5).coerceAtLeast(2*d),paint) }; CrosshairStyle.CROSSHAIR -> { paint.style=Paint.Style.STROKE; canvas.drawLine(cx-r,cy,cx-r/3,cy,paint); canvas.drawLine(cx+r/3,cy,cx+r,cy,paint); canvas.drawLine(cx,cy-r,cx,cy-r/3,paint); canvas.drawLine(cx,cy+r/3,cx,cy+r,paint) }; CrosshairStyle.CIRCLE_DOT -> { paint.style=Paint.Style.STROKE; canvas.drawCircle(cx,cy,r,paint); paint.style=Paint.Style.FILL; canvas.drawCircle(cx,cy,2*d,paint) }; CrosshairStyle.OPEN_CROSS -> { paint.style=Paint.Style.STROKE; val gap=r/3; canvas.drawLine(cx-r,cy,cx-gap,cy,paint); canvas.drawLine(cx+gap,cy,cx+r,cy,paint); canvas.drawLine(cx,cy-r,cx,cy-gap,paint); canvas.drawLine(cx,cy+gap,cx,cy+r,paint) } }
    }
}
