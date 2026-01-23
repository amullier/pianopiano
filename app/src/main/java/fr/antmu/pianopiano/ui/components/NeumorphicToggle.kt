package fr.antmu.pianopiano.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import fr.antmu.pianopiano.R

class NeumorphicToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var isChecked = false
    private val thumb: View
    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    private val thumbMargin = resources.getDimensionPixelSize(R.dimen.toggle_thumb_margin)
    private val thumbSize = resources.getDimensionPixelSize(R.dimen.toggle_thumb_size)
    private val trackWidth = resources.getDimensionPixelSize(R.dimen.toggle_width)

    init {
        layoutParams = LayoutParams(
            resources.getDimensionPixelSize(R.dimen.toggle_width),
            resources.getDimensionPixelSize(R.dimen.toggle_height)
        )

        setBackgroundResource(R.drawable.bg_neumorphic_toggle_off)

        thumb = View(context).apply {
            setBackgroundResource(R.drawable.bg_neumorphic_thumb)
            layoutParams = LayoutParams(thumbSize, thumbSize).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                leftMargin = thumbMargin
            }
        }
        addView(thumb)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NeumorphicToggle,
            0, 0
        ).apply {
            try {
                isChecked = getBoolean(R.styleable.NeumorphicToggle_toggleChecked, false)
                updateUI(animate = false)
            } finally {
                recycle()
            }
        }

        setOnClickListener {
            toggle()
        }
    }

    fun setChecked(checked: Boolean, animate: Boolean = true) {
        if (isChecked != checked) {
            isChecked = checked
            updateUI(animate)
            onCheckedChangeListener?.invoke(isChecked)
        }
    }

    fun isChecked(): Boolean = isChecked

    fun toggle() {
        setChecked(!isChecked)
    }

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = listener
    }

    private fun updateUI(animate: Boolean) {
        val targetMargin = if (isChecked) {
            trackWidth - thumbSize - thumbMargin
        } else {
            thumbMargin
        }

        setBackgroundResource(
            if (isChecked) R.drawable.bg_neumorphic_toggle_on
            else R.drawable.bg_neumorphic_toggle_off
        )

        if (animate) {
            val currentMargin = (thumb.layoutParams as LayoutParams).leftMargin
            ValueAnimator.ofInt(currentMargin, targetMargin).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val params = thumb.layoutParams as LayoutParams
                    params.leftMargin = animator.animatedValue as Int
                    thumb.layoutParams = params
                }
                start()
            }
        } else {
            val params = thumb.layoutParams as LayoutParams
            params.leftMargin = targetMargin
            thumb.layoutParams = params
        }
    }
}
