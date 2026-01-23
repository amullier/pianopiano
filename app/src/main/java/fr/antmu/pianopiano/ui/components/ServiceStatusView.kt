package fr.antmu.pianopiano.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.databinding.ViewServiceStatusBinding

class ServiceStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewServiceStatusBinding

    init {
        binding = ViewServiceStatusBinding.inflate(LayoutInflater.from(context), this)
        orientation = HORIZONTAL

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ServiceStatusView,
            0, 0
        ).apply {
            try {
                getString(R.styleable.ServiceStatusView_statusTitle)?.let {
                    binding.textTitle.text = it
                }
                val isActive = getBoolean(R.styleable.ServiceStatusView_statusActive, false)
                setActive(isActive)
            } finally {
                recycle()
            }
        }
    }

    fun setActive(active: Boolean) {
        binding.indicator.setImageResource(
            if (active) R.drawable.indicator_active else R.drawable.indicator_inactive
        )
        binding.textTitle.text = if (active) {
            context.getString(R.string.service_active)
        } else {
            context.getString(R.string.service_inactive)
        }
        binding.textDescription.text = if (active) {
            context.getString(R.string.service_description_active)
        } else {
            context.getString(R.string.service_description_inactive)
        }
    }
}
