package fr.antmu.pianopiano.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import fr.antmu.pianopiano.R

class NeumorphicButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var isButtonPressed = false

    init {
        setBackgroundResource(R.drawable.bg_neumorphic_button)
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        minHeight = resources.getDimensionPixelSize(R.dimen.button_height)
        minWidth = resources.getDimensionPixelSize(R.dimen.button_min_width)

        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.spacing_lg)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.spacing_md)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NeumorphicButton,
            0, 0
        ).apply {
            try {
                getString(R.styleable.NeumorphicButton_neumorphicText)?.let {
                    text = it
                }
            } finally {
                recycle()
            }
        }

        isClickable = true
        isFocusable = true
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1.0f else 0.5f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isButtonPressed = true
                setBackgroundResource(R.drawable.bg_neumorphic_button_pressed)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isButtonPressed = false
                setBackgroundResource(R.drawable.bg_neumorphic_button)
            }
        }
        return super.onTouchEvent(event)
    }
}
