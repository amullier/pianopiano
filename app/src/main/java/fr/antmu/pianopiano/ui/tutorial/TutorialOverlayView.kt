package fr.antmu.pianopiano.ui.tutorial

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import fr.antmu.pianopiano.R

class TutorialOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC0A0E27.toInt()
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33EE6A1B
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }

    private var spotlightRect: RectF? = null
    private var animatedRect: RectF? = null
    private var spotlightShape: SpotlightShape = SpotlightShape.ROUNDED_RECT
    private val cornerRadius = 16f * resources.displayMetrics.density

    private var tooltipView: View? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true
        isFocusable = true

        tooltipView = LayoutInflater.from(context).inflate(R.layout.view_tutorial_tooltip, this, false)
        addView(tooltipView)
    }

    fun getTooltipView(): View? = tooltipView

    fun setSpotlight(targetRect: RectF?, shape: SpotlightShape, tooltipPosition: TooltipPosition) {
        spotlightShape = shape

        val oldRect = animatedRect?.let { RectF(it) }
        spotlightRect = targetRect

        if (targetRect == null) {
            animatedRect = null
            positionTooltip(tooltipPosition, null)
            tooltipView?.alpha = 0f
            tooltipView?.animate()
                ?.alpha(1f)
                ?.setDuration(300)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()
            invalidate()
            return
        }

        if (oldRect == null) {
            animatedRect = RectF(targetRect)
            positionTooltip(tooltipPosition, targetRect)
            tooltipView?.alpha = 0f
            tooltipView?.animate()
                ?.alpha(1f)
                ?.setDuration(300)
                ?.setInterpolator(DecelerateInterpolator())
                ?.start()
            invalidate()
            return
        }

        // Morph animation from old to new spotlight
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 350
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val fraction = anim.animatedFraction
            animatedRect = RectF(
                lerp(oldRect.left, targetRect.left, fraction),
                lerp(oldRect.top, targetRect.top, fraction),
                lerp(oldRect.right, targetRect.right, fraction),
                lerp(oldRect.bottom, targetRect.bottom, fraction)
            )
            invalidate()
        }

        // Animate tooltip: fade out, reposition, fade in
        tooltipView?.animate()
            ?.alpha(0f)
            ?.setDuration(150)
            ?.withEndAction {
                positionTooltip(tooltipPosition, targetRect)
                tooltipView?.animate()
                    ?.alpha(1f)
                    ?.setDuration(200)
                    ?.setInterpolator(DecelerateInterpolator())
                    ?.start()
            }
            ?.start()

        animator.start()
    }

    private fun positionTooltip(position: TooltipPosition, targetRect: RectF?) {
        val tooltip = tooltipView ?: return
        val marginHorizontal = (16 * resources.displayMetrics.density).toInt()
        val marginVertical = (12 * resources.displayMetrics.density).toInt()

        tooltip.post {
            val lp = tooltip.layoutParams as LayoutParams

            when (position) {
                TooltipPosition.CENTER -> {
                    lp.width = LayoutParams.MATCH_PARENT
                    lp.height = LayoutParams.WRAP_CONTENT
                    lp.gravity = android.view.Gravity.CENTER
                    lp.leftMargin = marginHorizontal
                    lp.rightMargin = marginHorizontal
                    lp.topMargin = 0
                    lp.bottomMargin = 0
                }
                TooltipPosition.BELOW -> {
                    lp.width = LayoutParams.MATCH_PARENT
                    lp.height = LayoutParams.WRAP_CONTENT
                    lp.gravity = android.view.Gravity.TOP
                    lp.leftMargin = marginHorizontal
                    lp.rightMargin = marginHorizontal
                    lp.topMargin = (targetRect?.bottom?.toInt() ?: 0) + marginVertical
                    lp.bottomMargin = 0
                }
                TooltipPosition.ABOVE -> {
                    lp.width = LayoutParams.MATCH_PARENT
                    lp.height = LayoutParams.WRAP_CONTENT
                    lp.gravity = android.view.Gravity.TOP
                    lp.leftMargin = marginHorizontal
                    lp.rightMargin = marginHorizontal
                    // Measure tooltip to position above target
                    tooltip.measure(
                        MeasureSpec.makeMeasureSpec(width - 2 * marginHorizontal, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    )
                    lp.topMargin = (targetRect?.top?.toInt() ?: 0) - tooltip.measuredHeight - marginVertical
                    lp.bottomMargin = 0
                }
            }
            tooltip.layoutParams = lp
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Draw overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Cut out spotlight hole
        animatedRect?.let { rect ->
            when (spotlightShape) {
                SpotlightShape.ROUNDED_RECT -> {
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, clearPaint)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
                }
                SpotlightShape.CIRCLE -> {
                    val cx = rect.centerX()
                    val cy = rect.centerY()
                    val radius = maxOf(rect.width(), rect.height()) / 2f
                    canvas.drawCircle(cx, cy, radius, clearPaint)
                    canvas.drawCircle(cx, cy, radius, glowPaint)
                }
            }
        }

        // Draw children (tooltip) on top
        super.dispatchDraw(canvas)
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
}
