package fr.antmu.pianopiano.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import fr.antmu.pianopiano.R

class GlassmorphicCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.bg_glassmorphic_card)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GlassmorphicCard,
            0, 0
        ).apply {
            try {
                // Custom attributes can be handled here if needed
            } finally {
                recycle()
            }
        }
    }
}
