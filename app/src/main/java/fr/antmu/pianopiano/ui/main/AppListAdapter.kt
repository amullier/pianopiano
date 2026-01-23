package fr.antmu.pianopiano.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.antmu.pianopiano.data.repository.AppRepository
import fr.antmu.pianopiano.databinding.ItemAppBinding

class AppListAdapter(
    private val onToggleChanged: (AppRepository.InstalledApp, Boolean) -> Unit
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

        fun bind(app: AppRepository.InstalledApp) {
            binding.imageIcon.setImageDrawable(app.icon)
            binding.textName.text = app.appName
            binding.toggle.setChecked(app.isConfigured, animate = false)

            binding.toggle.setOnCheckedChangeListener { isChecked ->
                onToggleChanged(app, isChecked)
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
            return oldItem == newItem
        }
    }
}
