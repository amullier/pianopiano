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

    enum class ServiceStatus {
        INACTIVE,
        PARTIAL,
        ACTIVE
    }

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
        setStatus(if (active) ServiceStatus.ACTIVE else ServiceStatus.INACTIVE)
    }

    fun setStatus(status: ServiceStatus) {
        when (status) {
            ServiceStatus.INACTIVE -> {
                binding.indicator.setImageResource(R.drawable.indicator_inactive)
                binding.textTitle.text = context.getString(R.string.service_inactive)
                setDescription(context.getString(R.string.service_description_inactive))
            }
            ServiceStatus.PARTIAL -> {
                binding.indicator.setImageResource(R.drawable.indicator_partial)
                binding.textTitle.text = context.getString(R.string.service_partial)
                setDescription(context.getString(R.string.service_description_partial))
            }
            ServiceStatus.ACTIVE -> {
                binding.indicator.setImageResource(R.drawable.indicator_active)
                binding.textTitle.text = context.getString(R.string.service_active)
                setDescription(context.getString(R.string.service_description_active))
            }
        }
    }

    private fun setDescription(description: String) {
        if (description.isEmpty()) {
            binding.textDescription.visibility = GONE
        } else {
            binding.textDescription.visibility = VISIBLE
            binding.textDescription.text = description
        }
    }
}
