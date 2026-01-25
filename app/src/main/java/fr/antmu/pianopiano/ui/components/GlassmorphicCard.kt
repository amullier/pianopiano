package fr.antmu.pianopiano.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import fr.antmu.pianopiano.R

class GlassmorphicCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL // Important !
        setBackgroundResource(R.drawable.bg_glassmorphic_card)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GlassmorphicCard,
            0, 0
        ).apply {
            try {
                // Gérer les attributs custom si nécessaire
            } finally {
                recycle()
            }
        }
    }
}

