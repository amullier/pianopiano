package fr.antmu.pianopiano.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import fr.antmu.pianopiano.R

class DiscreteSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val seekBar: AppCompatSeekBar
    private val valueLabel: TextView
    private var values: List<Int> = listOf(6, 12, 18, 24, 30)
    private var currentIndex: Int = 0
    private var onValueChangeListener: ((Int) -> Unit)? = null

    init {
        orientation = VERTICAL

        val padding = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        setPadding(0, padding, 0, padding)

        valueLabel = TextView(context).apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
            }
        }
        addView(valueLabel)

        seekBar = AppCompatSeekBar(context).apply {
            max = values.size - 1
            progress = currentIndex
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentIndex = progress
                    updateLabel()
                    if (fromUser) {
                        onValueChangeListener?.invoke(values[currentIndex])
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        addView(seekBar)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DiscreteSlider,
            0, 0
        ).apply {
            try {
                val minVal = getInteger(R.styleable.DiscreteSlider_sliderMinValue, 6)
                val maxVal = getInteger(R.styleable.DiscreteSlider_sliderMaxValue, 30)
                val step = getInteger(R.styleable.DiscreteSlider_sliderStep, 6)
                val currentVal = getInteger(R.styleable.DiscreteSlider_sliderValue, 12)

                setRange(minVal, maxVal, step)
                setValue(currentVal)
            } finally {
                recycle()
            }
        }

        updateLabel()
    }

    fun setRange(min: Int, max: Int, step: Int) {
        values = (min..max step step).toList()
        seekBar.max = values.size - 1
        if (currentIndex >= values.size) {
            currentIndex = values.size - 1
        }
        seekBar.progress = currentIndex
        updateLabel()
    }

    fun setValue(value: Int) {
        val index = values.indexOf(value)
        if (index >= 0) {
            currentIndex = index
            seekBar.progress = currentIndex
            updateLabel()
        }
    }

    fun getValue(): Int = values.getOrElse(currentIndex) { values.first() }

    fun setOnValueChangeListener(listener: (Int) -> Unit) {
        onValueChangeListener = listener
    }

    private fun updateLabel() {
        valueLabel.text = context.getString(R.string.settings_pause_duration_format, getValue())
    }
}
