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
    private var animProgress = 0f  // 0 to 1 for full cycle
    private var animator: ValueAnimator? = null

    private val stepsUp = 3      // Steps going up
    private val stepsDown = 3    // Steps going down
    private val totalSteps = stepsUp + stepsDown
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
        stepWidth = w.toFloat() / 4f
        stepHeight = (h.toFloat() - ballRadius * 6) / (stepsUp + 1)
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
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalSteps * 1000L  // 1s per step
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

        // Total horizontal distance for one complete cycle
        val cycleWidth = totalSteps * stepWidth

        // Scroll offset: moves from 0 to cycleWidth over the animation
        val scrollX = -animProgress * cycleWidth

        // Base Y position (bottom level)
        val bottomY = height.toFloat() - stepHeight

        // Draw stairs pattern (repeated to fill screen + buffer)
        // Pattern: flat -> up steps -> peak -> down steps -> flat (repeat)

        val startX = scrollX - cycleWidth  // Start off-screen left

        // Move to starting point
        stairPath.moveTo(startX, bottomY)

        // Draw multiple cycles to cover the screen
        for (cycle in 0..2) {
            val cycleStartX = startX + cycle * cycleWidth

            // Initial flat section
            stairPath.lineTo(cycleStartX + stepWidth * 0.5f, bottomY)

            // Going up
            for (i in 0 until stepsUp) {
                val x = cycleStartX + stepWidth * 0.5f + i * stepWidth
                val y = bottomY - i * stepHeight

                // Vertical riser
                stairPath.lineTo(x, y - stepHeight)
                // Horizontal tread
                stairPath.lineTo(x + stepWidth, y - stepHeight)
            }

            // Going down
            for (i in 0 until stepsDown) {
                val x = cycleStartX + stepWidth * 0.5f + stepsUp * stepWidth + i * stepWidth
                val y = bottomY - (stepsUp - 1 - i) * stepHeight

                // Vertical drop
                stairPath.lineTo(x, y)
                // Horizontal tread
                stairPath.lineTo(x + stepWidth, y)
            }

            // Final flat to connect to next cycle
            stairPath.lineTo(cycleStartX + cycleWidth + stepWidth * 0.5f, bottomY)
        }

        canvas.drawPath(stairPath, stairPaint)
    }

    private fun drawBall(canvas: Canvas) {
        // Ball stays at center X
        val ballX = width / 2f

        // Calculate which step the ball is on based on animation progress
        val stepIndex = (animProgress * totalSteps).toInt()
        val progressInStep = (animProgress * totalSteps) - stepIndex

        // Calculate the Y position of the step under the ball
        val currentStepY: Float = if (stepIndex < stepsUp) {
            // Going up
            height.toFloat() - stepHeight - stepIndex * stepHeight
        } else {
            // Going down
            val downIndex = stepIndex - stepsUp
            height.toFloat() - stepHeight - (stepsUp - 1 - downIndex) * stepHeight
        }

        // Ball bounces on each step (small parabolic arc)
        val bounceHeight = stepHeight * 0.4f
        val bounceArc = -4 * bounceHeight * progressInStep * (progressInStep - 1)

        val ballY = currentStepY - ballRadius - bounceArc

        canvas.drawCircle(ballX, ballY, ballRadius, ballPaint)
    }
}
