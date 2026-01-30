package fr.antmu.pianopiano.ui.pause

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import fr.antmu.pianopiano.R

class StairAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val stairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var stairColor: Int = ContextCompat.getColor(context, R.color.accent_peanut)
    private var ballColor: Int = ContextCompat.getColor(context, R.color.accent_peanut)
    private var strokeWidth: Float = 1.5f * resources.displayMetrics.density
    private var ballRadius: Float = 5f * resources.displayMetrics.density

    private val stairPath = Path()
    private var animProgress = 0f  // 0 to stepCount (continuous)
    private var animator: ValueAnimator? = null

    private val stepCount = 5  // Number of steps
    private var stepWidth = 0f
    private var stepHeight = 0f

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StairAnimationView,
            0, 0
        ).apply {
            try {
                stairColor = getColor(R.styleable.StairAnimationView_stairColor, stairColor)
                ballColor = getColor(R.styleable.StairAnimationView_ballColor, ballColor)
                strokeWidth = getDimension(R.styleable.StairAnimationView_stairStrokeWidth, strokeWidth)
            } finally {
                recycle()
            }
        }

        stairPaint.color = stairColor
        stairPaint.strokeWidth = strokeWidth
        ballPaint.color = ballColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        stepWidth = w.toFloat() / (stepCount + 1)
        stepHeight = (h.toFloat() - ballRadius * 4) / (stepCount + 1)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, stepCount.toFloat()).apply {
            duration = stepCount * 1200L  // 1200ms per step
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        drawStairs(canvas)
        drawBall(canvas)
    }

    private fun drawStairs(canvas: Canvas) {
        stairPath.reset()

        // Stairs go from bottom-left to top-right
        // First step starts at left edge
        val startX = 0f
        val bottomY = height.toFloat() - stepHeight

        // Draw ground line before first step
        stairPath.moveTo(startX, bottomY)

        for (i in 0 until stepCount) {
            val x = startX + i * stepWidth
            val y = bottomY - i * stepHeight

            // Horizontal tread (where ball lands)
            stairPath.lineTo(x + stepWidth, y)
            // Vertical riser (going up)
            stairPath.lineTo(x + stepWidth, y - stepHeight)
        }

        // Final horizontal line at top
        val topX = startX + stepCount * stepWidth
        val topY = bottomY - stepCount * stepHeight
        stairPath.lineTo(topX + stepWidth, topY)

        canvas.drawPath(stairPath, stairPaint)
    }

    private fun drawBall(canvas: Canvas) {
        // Current step (0 = first step, 1 = second, etc.)
        val currentStep = animProgress.toInt().coerceIn(0, stepCount - 1)
        val progressInStep = animProgress - currentStep  // 0 to 1 within current jump

        // Starting position (on current step)
        val startX = stepWidth / 2 + currentStep * stepWidth
        val bottomY = height.toFloat() - stepHeight
        val startY = bottomY - currentStep * stepHeight - ballRadius

        // Ending position (on next step)
        val endX = startX + stepWidth
        val endY = startY - stepHeight

        // Interpolate X linearly
        val ballX = startX + (endX - startX) * progressInStep

        // Interpolate Y with parabolic jump
        val jumpHeight = stepHeight * 0.8f
        // Parabola: goes up then down, peaks at t=0.5
        val jumpArc = -4 * jumpHeight * progressInStep * (progressInStep - 1)
        val linearY = startY + (endY - startY) * progressInStep
        val ballY = linearY - jumpArc

        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
    }
}
