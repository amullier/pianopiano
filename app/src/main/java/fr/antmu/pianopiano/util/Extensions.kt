package fr.antmu.pianopiano.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import fr.antmu.pianopiano.PianoPianoApp

val Context.app: PianoPianoApp
    get() = applicationContext as PianoPianoApp

val Fragment.app: PianoPianoApp
    get() = requireContext().app

fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.setInvisible(invisible: Boolean) {
    visibility = if (invisible) View.INVISIBLE else View.VISIBLE
}

fun View.setMargins(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams ?: return
    left?.let { params.leftMargin = it }
    top?.let { params.topMargin = it }
    right?.let { params.rightMargin = it }
    bottom?.let { params.bottomMargin = it }
    layoutParams = params
}

fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

fun Float.dpToPx(context: Context): Float {
    return this * context.resources.displayMetrics.density
}

fun Long.formatDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}j ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
