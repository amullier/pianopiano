package fr.antmu.pianopiano.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.databinding.ItemAppBinding

class AppListAdapter(
    private val onToggleChanged: (AppRepository.InstalledApp, Boolean) -> Unit,
    private val onSettingsClicked: (AppRepository.InstalledApp) -> Unit,
    private val onInfoClicked: (AppRepository.InstalledApp) -> Unit,
    private val hasUsageStatsPermission: Boolean
) : ListAdapter<AppRepository.InstalledApp, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Définir le listener UNE SEULE FOIS lors de la création du ViewHolder
            binding.toggle.setOnCheckedChangeListener { isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && position < itemCount) {
                    val app = getItem(position)
                    binding.buttonInfo.visibility = if (hasUsageStatsPermission) View.VISIBLE else View.GONE
                    if (isChecked) {
                        binding.buttonSettings.visibility = View.VISIBLE
                    } else {
                        binding.buttonSettings.visibility = View.GONE
                    }
                    onToggleChanged(app, isChecked)
                }
            }

            binding.buttonSettings.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val app = getItem(position)
                    onSettingsClicked(app)
                }
            }

            binding.buttonInfo.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val app = getItem(position)
                    onInfoClicked(app)
                }
            }
        }

        fun bind(app: AppRepository.InstalledApp) {
            binding.imageIcon.setImageDrawable(app.icon)
            binding.textName.text = app.appName
            // Ne pas notifier le listener lors du bind initial
            binding.toggle.setChecked(app.isConfigured, animate = false, notifyListener = false)

            // Visibilité mutuellement exclusive : settings si configuré, info sinon
            binding.buttonInfo.visibility = if (hasUsageStatsPermission) View.VISIBLE else View.GONE
            if (app.isConfigured) {
                binding.buttonSettings.visibility = View.VISIBLE
            } else {
                binding.buttonSettings.visibility = View.GONE
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppRepository.InstalledApp>() {
        override fun areItemsTheSame(
            oldItem: AppRepository.InstalledApp,
            newItem: AppRepository.InstalledApp
        ): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(
            oldItem: AppRepository.InstalledApp,
            newItem: AppRepository.InstalledApp
        ): Boolean {
            // Comparer les champs explicitement (Drawable.equals compare par référence,
            // ce qui causerait des rebinds inutiles après chaque loadApps)
            return oldItem.packageName == newItem.packageName
                    && oldItem.appName == newItem.appName
                    && oldItem.isConfigured == newItem.isConfigured
        }
    }
}
