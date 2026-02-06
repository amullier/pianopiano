package fr.antmu.pianopiano.ui.tutorial

import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.antmu.pianopiano.R

class TutorialManager(
    private val rootView: ViewGroup,
    private val onComplete: () -> Unit
) {

    private var overlayView: TutorialOverlayView? = null
    private var currentStepIndex = -1
    private val steps = buildSteps()
    private val totalSteps = steps.size

    private fun buildSteps(): List<TutorialStep> = listOf(
        TutorialStep(
            id = 0,
            titleResId = R.string.tutorial_recap_title,
            descriptionResId = R.string.tutorial_recap_description,
            targetViewFinder = null,
            spotlightShape = SpotlightShape.ROUNDED_RECT,
            spotlightPaddingDp = 0f,
            tooltipPosition = TooltipPosition.CENTER
        ),
        TutorialStep(
            id = 1,
            titleResId = R.string.tutorial_toggle_title,
            descriptionResId = R.string.tutorial_toggle_description,
            targetViewFinder = TargetViewFinder.RecyclerViewItem(
                recyclerViewId = R.id.recyclerApps,
                itemPosition = 0,
                childViewId = R.id.toggle
            ),
            spotlightShape = SpotlightShape.ROUNDED_RECT,
            spotlightPaddingDp = 8f,
            tooltipPosition = TooltipPosition.ABOVE
        ),
        TutorialStep(
            id = 2,
            titleResId = R.string.tutorial_settings_title,
            descriptionResId = R.string.tutorial_settings_description,
            targetViewFinder = TargetViewFinder.RecyclerViewItem(
                recyclerViewId = R.id.recyclerApps,
                itemPosition = 0,
                childViewId = -1 // entire row
            ),
            spotlightShape = SpotlightShape.ROUNDED_RECT,
            spotlightPaddingDp = 4f,
            tooltipPosition = TooltipPosition.ABOVE
        ),
        TutorialStep(
            id = 3,
            titleResId = R.string.tutorial_peanuts_title,
            descriptionResId = R.string.tutorial_peanuts_description,
            targetViewFinder = TargetViewFinder.ById(R.id.textPeanuts),
            spotlightShape = SpotlightShape.ROUNDED_RECT,
            spotlightPaddingDp = 8f,
            tooltipPosition = TooltipPosition.BELOW
        )
    )

    fun start() {
        if (overlayView != null) return

        val overlay = TutorialOverlayView(rootView.context)
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        rootView.addView(overlay)
        overlayView = overlay

        val tooltipView = overlay.getTooltipView() ?: return
        tooltipView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.buttonTutorialNext)
            .setOnClickListener { nextStep() }

        currentStepIndex = -1
        nextStep()
    }

    fun nextStep() {
        currentStepIndex++
        if (currentStepIndex >= totalSteps) {
            dismiss()
            onComplete()
            return
        }

        showStep(steps[currentStepIndex])
    }

    fun dismiss() {
        overlayView?.let { rootView.removeView(it) }
        overlayView = null
        currentStepIndex = -1
    }

    val isActive: Boolean
        get() = overlayView != null

    private fun showStep(step: TutorialStep) {
        val overlay = overlayView ?: return
        val tooltipView = overlay.getTooltipView() ?: return

        // Update text content
        tooltipView.findViewById<TextView>(R.id.textTutorialTitle).setText(step.titleResId)
        tooltipView.findViewById<TextView>(R.id.textTutorialDescription).setText(step.descriptionResId)

        // Show/hide logo for recap step
        val logoView = tooltipView.findViewById<ImageView>(R.id.imageTutorialLogo)
        logoView.visibility = if (step.targetViewFinder == null) View.VISIBLE else View.GONE

        // Update button text
        val buttonNext = tooltipView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.buttonTutorialNext)
        val isLastStep = currentStepIndex == totalSteps - 1
        buttonNext.setText(if (isLastStep) R.string.tutorial_done else R.string.tutorial_next)

        // Update step indicator dots
        updateStepIndicator(tooltipView)

        // Find target rect and set spotlight
        val targetRect = step.targetViewFinder?.let { findTargetRect(it, step.spotlightPaddingDp) }
        overlay.setSpotlight(targetRect, step.spotlightShape, step.tooltipPosition)
    }

    private fun findTargetRect(finder: TargetViewFinder, paddingDp: Float): RectF? {
        val density = rootView.resources.displayMetrics.density
        val padding = paddingDp * density

        val targetView: View? = when (finder) {
            is TargetViewFinder.ById -> rootView.findViewById(finder.viewId)
            is TargetViewFinder.RecyclerViewItem -> {
                val recyclerView = rootView.findViewById<RecyclerView>(finder.recyclerViewId) ?: return null
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(finder.itemPosition) ?: return null
                if (finder.childViewId == -1) {
                    viewHolder.itemView
                } else {
                    viewHolder.itemView.findViewById(finder.childViewId)
                }
            }
        }

        targetView ?: return null

        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)

        // Coordinates relative to the rootView
        val left = (location[0] - rootLocation[0]).toFloat() - padding
        val top = (location[1] - rootLocation[1]).toFloat() - padding
        val right = left + targetView.width + 2 * padding
        val bottom = top + targetView.height + 2 * padding

        return RectF(left, top, right, bottom)
    }

    private fun updateStepIndicator(tooltipView: View) {
        val container = tooltipView.findViewById<LinearLayout>(R.id.layoutStepIndicator)
        container.removeAllViews()

        val density = rootView.resources.displayMetrics.density
        val dotMargin = (4 * density).toInt()

        for (i in 0 until totalSteps) {
            val dot = View(rootView.context)
            val size = (8 * density).toInt()
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginEnd = dotMargin
            dot.layoutParams = lp
            dot.setBackgroundResource(
                if (i == currentStepIndex) R.drawable.indicator_active else R.drawable.indicator_inactive
            )
            container.addView(dot)
        }
    }
}
