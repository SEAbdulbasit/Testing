package com.example.customscannerview.mlkit.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.customscannerview.R


class RectangleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var boxLeftSide=0F
    var boxTopSide=0F
    var boxRightSide=0F
    var boxBottomSide=0F
    private val boxPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_reticle_stroke)
        style = Paint.Style.STROKE
        isAntiAlias=true
//        pathEffect=DashPathEffect(floatArrayOf(10f,10f),0f)
        strokeWidth = context.resources.getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width).toFloat()
    }



   /* fun translateRect(rect: Rect) = RectF(
        translateX(rect.left.toFloat()),
        translateY(rect.top.toFloat()),
        translateX(rect.right.toFloat()),
        translateY(rect.bottom.toFloat())
    )
    fun translateX(x: Float): Float = x * 1.0f
    fun translateY(y: Float): Float = y * 1.0f*/

    private val scrimPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_reticle_background)
    }

    private val eraserPaint: Paint = Paint().apply {
        strokeWidth = boxPaint.strokeWidth
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val boxCornerRadius: Float =
        context.resources.getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius).toFloat()

    private var boxRect: RectF? = null

    fun setRectangleViewFinder() {
        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth = overlayWidth * 75 / 100
        val boxHeight = overlayHeight * 20 / 100
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        boxRect = RectF(cx - boxWidth / 2,
            cy - boxHeight / 1.5f,
            cx + boxWidth / 2,
            cy + boxHeight / 4.5f)
        boxLeftSide=cx - boxWidth / 2
        boxTopSide=cy - boxHeight / 1.5f
        boxRightSide=cx + boxWidth / 2
        boxBottomSide=cy + boxHeight / 4.5f
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
            canvas.drawRoundRect(it, boxCornerRadius, boxCornerRadius, boxPaint)
            drawRectangleBarView(canvas)
        }

    }


    fun drawRectangleBarView(canvas: Canvas) {
        val paint = Paint()
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth =8f
        paint.isAntiAlias = true

        val overlayWidth = width.toFloat()
        val overlayHeight = height.toFloat()
        val boxWidth = overlayWidth * 75 / 100
        val boxHeight = overlayHeight * 20 / 100
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2


        // Adjust according to your requirements..
        val length = canvas.width * 0.05f
        val corner = length * 0.05f
        val left = cx-boxWidth/2
        val top = cy-boxHeight/1.5f
        val right = cx+boxWidth/2
        val bottom = cy+boxHeight/4.5f




        val path = Path()

        // Top-Left corner..
        path.moveTo(left, top + length)
        path.lineTo(left, top + corner)
        path.cubicTo(left, top + corner, left, top, left + corner, top)
        path.lineTo(left + length, top)
//        cordinates.left.y= top+corner
        // Top-Right corner..
        path.moveTo(right - length, top)
        path.lineTo(right - corner, top)
        path.cubicTo(right - corner, top, right, top, right, top + corner)
        path.lineTo(right, top + length)
        // Bottom-Right corner..
        path.moveTo(right, bottom - length)
        path.lineTo(right, bottom - corner)
        path.cubicTo(right, bottom - corner, right, bottom, right - corner, bottom)
        path.lineTo(right - length, bottom)
        // Bottom-Left corner..
        path.moveTo(left + length, bottom)
        path.lineTo(left + corner, bottom)
        path.cubicTo(left + corner, bottom, left, bottom, left, bottom - corner)
        path.lineTo(left, bottom - length)

        // Draw path..
        canvas.drawPath(path, paint)

    }


}