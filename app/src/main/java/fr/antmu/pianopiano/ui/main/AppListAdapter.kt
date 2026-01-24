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
    private val onSettingsClicked: (AppRepository.InstalledApp) -> Unit
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
                if (position != RecyclerView.NO_POSITION) {
                    val app = getItem(position)
                    binding.buttonSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
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
        }

        fun bind(app: AppRepository.InstalledApp) {
            binding.imageIcon.setImageDrawable(app.icon)
            binding.textName.text = app.appName
            // Ne pas notifier le listener lors du bind initial
            binding.toggle.setChecked(app.isConfigured, animate = false, notifyListener = false)

            // Afficher l'icône settings seulement si l'app est configurée
            binding.buttonSettings.visibility = if (app.isConfigured) View.VISIBLE else View.GONE
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
            return oldItem == newItem
        }
    }
}
