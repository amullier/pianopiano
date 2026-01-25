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
import kotlin.math.atan
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

    private val bikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    private val bikeWheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val path = Path()
    private var phase = 0f
    private var rotationAngle = 0f
    private var pedalAngle = 0f
    private var wheelRotation = 0f
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
        bikePaint.color = circleColor
        bikeWheelPaint.color = circleColor
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
                // Animation du pédalage et des roues
                pedalAngle = (phase * 5f) % (2 * Math.PI).toFloat()
                wheelRotation = (phase * 8f) % (2 * Math.PI).toFloat()
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun calculateSlopeAngle(x: Float): Float {
        val derivative = (amplitude / wavelength) * 2 * Math.PI *
                cos((x / wavelength * 2 * Math.PI + phase))
        return kotlin.math.atan(derivative).toFloat()
    }

    private fun drawWheel(canvas: Canvas, cx: Float, cy: Float, radius: Float, rotation: Float) {
        // Jante extérieure
        canvas.drawCircle(cx, cy, radius, bikeWheelPaint)
        // Moyeu central
        canvas.drawCircle(cx, cy, radius * 0.12f, bikeWheelPaint)

        // 4 rayons rotatifs
        val spokeCount = 4
        for (i in 0 until spokeCount) {
            val angle = rotation + (i * 2 * Math.PI / spokeCount).toFloat()
            val innerX = cx + cos(angle) * radius * 0.12f
            val innerY = cy + sin(angle) * radius * 0.12f
            val outerX = cx + cos(angle) * radius * 0.92f
            val outerY = cy + sin(angle) * radius * 0.92f
            canvas.drawLine(innerX, innerY, outerX, outerY, bikeWheelPaint)
        }
    }

    private fun drawBicycle(canvas: Canvas, wheelRadius: Float, wheelBase: Float) {
        val framePath = Path()

        // Positions des roues
        val rearWheelX = -wheelBase / 2
        val frontWheelX = wheelBase / 2
        val wheelsY = 0f

        // Position du pédalier (centré entre les deux roues)
        val pedalierX = 0f
        val pedalierY = wheelsY

        // Position du haut du tube de selle (où commence la tige de selle)
        val seatTubeTopX = rearWheelX + wheelBase * 0.35f
        val seatTubeTopY = -wheelRadius * 1.4f

        // Position de la selle (au bout de la tige de selle)
        val seatPostLength = wheelRadius * 0.6f
        val seatX = seatTubeTopX
        val seatY = seatTubeTopY - seatPostLength

        // Position du haut de la fourche (douille de direction)
        val headTubeX = frontWheelX - wheelBase * 0.08f
        val headTubeTopY = -wheelRadius * 1.3f
        val headTubeBottomY = -wheelRadius * 0.7f

        // === CADRE DU VÉLO ===

        // Tube de selle (seat tube) - du pédalier au haut du tube
        framePath.moveTo(pedalierX, pedalierY)
        framePath.lineTo(seatTubeTopX, seatTubeTopY)

        // Tube horizontal (top tube) - du haut du tube de selle à la douille de direction
        framePath.lineTo(headTubeX, headTubeTopY)

        // Tube diagonal (down tube) - de la douille au pédalier
        framePath.moveTo(headTubeX, headTubeBottomY)
        framePath.lineTo(pedalierX, pedalierY)

        // Hauban arrière (seat stay) - du haut du tube de selle à la roue arrière
        framePath.moveTo(seatTubeTopX, seatTubeTopY)
        framePath.lineTo(rearWheelX, wheelsY)

        // Base arrière (chain stay) - du pédalier à la roue arrière
        framePath.moveTo(pedalierX, pedalierY)
        framePath.lineTo(rearWheelX, wheelsY)

        // Douille de direction (head tube)
        framePath.moveTo(headTubeX, headTubeTopY)
        framePath.lineTo(headTubeX, headTubeBottomY)

        // Fourche avant - de la douille à la roue avant
        framePath.moveTo(headTubeX, headTubeBottomY)
        framePath.lineTo(frontWheelX, wheelsY)

        canvas.drawPath(framePath, bikePaint)

        // === TIGE DE SELLE ===
        canvas.drawLine(seatTubeTopX, seatTubeTopY, seatX, seatY, bikePaint)

        // === SELLE ===
        canvas.drawLine(seatX - wheelRadius * 0.35f, seatY,
                       seatX + wheelRadius * 0.25f, seatY, bikePaint)

        // === GUIDON COURBÉ (cintre de course, décalé de 45° sens horaire) ===
        val handlebarPath = Path()

        // La potence part de la douille et va vers l'avant/haut
        val potenceEndX = headTubeX + wheelRadius * 0.35f
        val potenceEndY = headTubeTopY - wheelRadius * 0.2f

        handlebarPath.moveTo(headTubeX, headTubeTopY)
        handlebarPath.lineTo(potenceEndX, potenceEndY)

        // Le cintre courbé vers le bas (décalé de 45° en sens horaire = vers l'avant et le bas)
        handlebarPath.quadTo(
            potenceEndX + wheelRadius * 0.25f, potenceEndY + wheelRadius * 0.15f,  // Point de contrôle
            potenceEndX + wheelRadius * 0.15f, potenceEndY + wheelRadius * 0.55f   // Point final
        )

        canvas.drawPath(handlebarPath, bikePaint)
    }

    private fun drawPedals(canvas: Canvas, wheelRadius: Float, wheelBase: Float, angle: Float) {
        // Position du pédalier (centré entre les deux roues)
        val pedalierX = 0f
        val pedalierY = 0f
        val crankLength = wheelRadius * 0.45f

        // Pédale droite
        val pedalX = pedalierX + cos(angle) * crankLength
        val pedalY = pedalierY + sin(angle) * crankLength

        // Pédale gauche (opposée)
        val pedalX2 = pedalierX + cos(angle + Math.PI).toFloat() * crankLength
        val pedalY2 = pedalierY + sin(angle + Math.PI).toFloat() * crankLength

        // Manivelles
        canvas.drawLine(pedalierX, pedalierY, pedalX, pedalY, bikePaint)
        canvas.drawLine(pedalierX, pedalierY, pedalX2, pedalY2, bikePaint)

        // Pédales (petits cercles)
        canvas.drawCircle(pedalX, pedalY, wheelRadius * 0.1f, bikePaint)
        canvas.drawCircle(pedalX2, pedalY2, wheelRadius * 0.1f, bikePaint)
    }

    private fun drawCyclist(canvas: Canvas, x: Float, y: Float) {
        val slopeAngle = calculateSlopeAngle(x)

        // Dimensions du vélo
        val wheelRadius = 9f * resources.displayMetrics.density // Roues plus petites
        val wheelBase = 32f * resources.displayMetrics.density // Cadre garde la même longueur

        canvas.save()
        // Positionner le vélo pour que les roues touchent la sinusoïde
        canvas.translate(x, y - wheelRadius)
        canvas.rotate(Math.toDegrees(slopeAngle.toDouble()).toFloat())

        // Dessiner les roues
        drawWheel(canvas, -wheelBase / 2, 0f, wheelRadius, wheelRotation)
        drawWheel(canvas, wheelBase / 2, 0f, wheelRadius, wheelRotation)

        // Dessiner le cadre
        drawBicycle(canvas, wheelRadius, wheelBase)

        // Dessiner les pédales
        drawPedals(canvas, wheelRadius, wheelBase, pedalAngle)

        canvas.restore()
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
        val ballY = centerY + amplitude * sin((centerX / wavelength * 2 * Math.PI + phase)).toFloat() - 13

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
