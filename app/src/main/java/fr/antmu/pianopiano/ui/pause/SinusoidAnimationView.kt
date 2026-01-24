package fr.antmu.pianopiano.ui.pause

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import fr.antmu.pianopiano.R
import kotlin.math.cos
import kotlin.math.sin

class SinusoidAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rotationIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(100, 255, 255, 255) // Petit point noir pour montrer la rotation
    }

    private val path = Path()
    private var phase = 0f
    private var rotationAngle = 0f
    private var animator: ValueAnimator? = null

    private var sinusoidColor: Int = ContextCompat.getColor(context, R.color.accent_primary)
    private var circleColor: Int = ContextCompat.getColor(context, R.color.accent_peanut)
    private var strokeWidth: Float = 4f * resources.displayMetrics.density
    private var amplitude: Float = 30f * resources.displayMetrics.density
    private var wavelength: Float = 200f * resources.displayMetrics.density
    private var speed: Float = 0.3f
    private var circleRadius: Float = 5f * resources.displayMetrics.density

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SinusoidAnimationView,
            0, 0
        ).apply {
            try {
                sinusoidColor = getColor(R.styleable.SinusoidAnimationView_sinusoidColor, sinusoidColor)
                circleColor = getColor(R.styleable.SinusoidAnimationView_circleColor, circleColor)
                strokeWidth = getDimension(R.styleable.SinusoidAnimationView_sinusoidStrokeWidth, strokeWidth)
                amplitude = getDimension(R.styleable.SinusoidAnimationView_sinusoidAmplitude, amplitude)
                wavelength = getDimension(R.styleable.SinusoidAnimationView_sinusoidWavelength, wavelength)
                speed = getFloat(R.styleable.SinusoidAnimationView_sinusoidSpeed, speed)
            } finally {
                recycle()
            }
        }

        linePaint.color = sinusoidColor
        linePaint.strokeWidth = strokeWidth
        circlePaint.color = circleColor
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
        animator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = (4000 / speed).toLong() // Plus lent
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                phase = animation.animatedValue as Float
                // Calculer l'angle de rotation (effet de roulement)
                rotationAngle = phase * 10f // 10x plus vite pour effet visible
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

        val centerY = height / 2f
        val centerX = width / 2f
        val startX = 0f
        val endX = width.toFloat()

        // Dessiner la sinusoïde
        path.reset()
        var x = startX
        val step = 5f

        path.moveTo(x, centerY + amplitude * sin(phase))

        while (x < endX) {
            x += step
            val y = centerY + amplitude * sin((x / wavelength * 2 * Math.PI + phase)).toFloat()
            path.lineTo(x, y)
        }

        canvas.drawPath(path, linePaint)

        // Calculer la position de la boule au centre
        val ballY = centerY + amplitude * sin((centerX / wavelength * 2 * Math.PI + phase)).toFloat() - 12

        // Dessiner la boule principale (couleur unie)
        canvas.drawCircle(centerX, ballY, circleRadius, circlePaint)

        // Dessiner le petit point indicateur de rotation
        // Le point tourne autour du centre de la boule
        val indicatorDistance = circleRadius * 0.5f
        val indicatorX = centerX + cos(rotationAngle.toDouble()).toFloat() * indicatorDistance
        val indicatorY = ballY + sin(rotationAngle.toDouble()).toFloat() * indicatorDistance

        canvas.drawCircle(
            indicatorX,
            indicatorY,
            circleRadius * 0.25f,
            rotationIndicatorPaint
        )
    }
}
