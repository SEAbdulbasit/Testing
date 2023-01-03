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
    private var boxCornerRadius: Float =
        context.resources.getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius).toFloat()

    private var boxRect: RectF? = null
    var scanningBoxRect: RectF? = null

    fun setRectangleViewFinder(barcodeWindow: ScanWindow) {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth =
            if (barcodeWindow.width == 0f) (overlayWidth * 0.85).toFloat() else barcodeWindow.width
        val boxHeight =
            if (barcodeWindow.height == 0f) (overlayHeight * 0.23).toFloat() else barcodeWindow.height

        val cx = overlayWidth / 2
        val cy =
            if (barcodeWindow.verticalStartingPosition == 0f) overlayHeight / 2 else barcodeWindow.verticalStartingPosition + (boxHeight / 2)
        boxRect = RectF(
            cx - boxWidth / 2, cy - boxHeight / 2f, cx + boxWidth / 2, cy + boxHeight / 2f
        )
        boxLeftSide = cx - boxWidth / 2
        boxTopSide = cy - boxHeight / 2f
        boxRightSide = cx + boxWidth / 2
        boxBottomSide = cy + boxHeight / 2f
        boxCornerRadius = barcodeWindow.radius

        invalidate()
    }

    fun setSquareViewFinder(barcodeWindow: ScanWindow) {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth =
            if (barcodeWindow.width == 0f) (overlayWidth * 0.72.toFloat()) else barcodeWindow.width
        val boxHeight =
            if (barcodeWindow.height == 0f) (overlayHeight * 0.38.toFloat()) else barcodeWindow.height

        val cx = overlayWidth / 2
        val cy =
            if (barcodeWindow.verticalStartingPosition == 0f) overlayHeight / 2 else barcodeWindow.verticalStartingPosition + (boxHeight / 2)
        boxRect = RectF(
            cx - boxWidth / 2, if (barcodeWindow.verticalStartingPosition == 0f) {
                cy - boxHeight / 2f
            } else {
                barcodeWindow.verticalStartingPosition
            }, cx + boxWidth / 2, if (barcodeWindow.verticalStartingPosition == 0f) {
                cy + (boxHeight / 2f)
            } else {
                barcodeWindow.verticalStartingPosition + boxHeight
            }
        )

        boxLeftSide = cx - boxWidth / 2
        boxTopSide = cy - boxHeight / 2f
        boxRightSide = cx + boxWidth / 2
        boxBottomSide = cy + boxHeight / 2f
        boxCornerRadius = barcodeWindow.radius

        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        boxRect?.let {
            // Draws the dark background scrim and leaves the box area clear.
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
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