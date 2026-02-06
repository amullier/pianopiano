package fr.antmu.pianopiano.ui.tutorial

import androidx.annotation.StringRes

data class TutorialStep(
    val id: Int,
    @StringRes val titleResId: Int,
    @StringRes val descriptionResId: Int,
    val targetViewFinder: TargetViewFinder?,
    val spotlightShape: SpotlightShape,
    val spotlightPaddingDp: Float,
    val tooltipPosition: TooltipPosition
)

sealed class TargetViewFinder {
    data class ById(val viewId: Int) : TargetViewFinder()
    data class RecyclerViewItem(
        val recyclerViewId: Int,
        val itemPosition: Int,
        val childViewId: Int // -1 = entire row
    ) : TargetViewFinder()
}

enum class SpotlightShape {
    ROUNDED_RECT,
    CIRCLE
}

enum class TooltipPosition {
    ABOVE,
    BELOW,
    CENTER
}
