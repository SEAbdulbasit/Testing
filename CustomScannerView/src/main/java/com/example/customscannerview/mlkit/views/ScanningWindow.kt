package com.example.customscannerview.mlkit.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.customscannerview.R


class ScanningWindow(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var boxLeftSide = 0F
    var boxTopSide = 0F
    var boxRightSide = 0F
    var boxBottomSide = 0F

    private val scrimPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_reticle_background)
    }

    private val eraserPaint: Paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val boxCornerRadius: Float =
        context.resources.getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius).toFloat()

    private var boxRect: RectF? = null
    var scanningBoxRect: RectF? = null

    fun setRectangleViewFinder() {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth = (overlayWidth * 0.85).toFloat()
        val boxHeight = (overlayHeight * 0.23).toFloat()
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        boxRect = RectF(
            cx - boxWidth / 2, cy - boxHeight / 1.5f, cx + boxWidth / 2, cy + boxHeight / 4.5f
        )
        boxLeftSide = cx - boxWidth / 2
        boxTopSide = cy - boxHeight / 1.5f
        boxRightSide = cx + boxWidth / 2
        boxBottomSide = cy + boxHeight / 4.5f
        invalidate()
    }
    fun setSquareViewFinder() {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth = overlayWidth * 72 / 100
        val boxHeight = overlayHeight * 38 / 100
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        boxRect = RectF(
            cx - boxWidth / 2,
            cy - boxHeight / 1.5f,
            cx + boxWidth / 2,
            cy + boxHeight / 4.5f
        )
        boxLeftSide = cx - boxWidth / 2
        boxTopSide = cy - boxHeight / 1.5f
        boxRightSide = cx + boxWidth / 2
        boxBottomSide = cy + boxHeight / 4.5f
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        boxRect?.let {
            // Draws the dark background scrim and leaves the box area clear.
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), scrimPaint)
            // As the stroke is always centered, so erase twice with FILL and STROKE respectively to clear
            // all area that the box rect would occupy.
            eraserPaint.style = Paint.Style.FILL
            canvas.drawRoundRect(it, boxCornerRadius, boxCornerRadius, eraserPaint)
            eraserPaint.style = Paint.Style.STROKE
            canvas.drawRoundRect(it, boxCornerRadius, boxCornerRadius, eraserPaint)
            // Draws the box.
            scanningBoxRect = it
        }

    }
}