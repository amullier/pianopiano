package fr.antmu.pianopiano.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import fr.antmu.pianopiano.R
import fr.antmu.pianopiano.databinding.FragmentMainBinding
import fr.antmu.pianopiano.databinding.ViewHeaderBinding
import fr.antmu.pianopiano.util.setVisible

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: AppListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupRecyclerView()
        setupActivationButton()
        observeViewModel()

        viewModel.loadApps()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
        viewModel.refreshPeanutCount()
    }

    private fun setupHeader() {
        val headerBinding = ViewHeaderBinding.bind(binding.header.root)

        headerBinding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_settings)
        }
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter { app, enabled ->
            viewModel.setAppConfigured(app, enabled)
        }

        binding.recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApps.adapter = adapter
    }

    private fun setupActivationButton() {
        binding.buttonActivateService.setOnClickListener {
            viewModel.openAccessibilitySettings()
        }
    }

    private fun observeViewModel() {
        viewModel.apps.observe(viewLifecycleOwner) { apps ->
            adapter.submitList(apps)
//            binding.textNoApps.setVisible(apps.isEmpty())
        }

        viewModel.isServiceActive.observe(viewLifecycleOwner) { isActive ->
            binding.serviceStatus.setActive(isActive)
            if (isActive) {
                binding.mainServiceEnabler.visibility = View.GONE
            }
        }

        viewModel.peanutCount.observe(viewLifecycleOwner) { count ->
            val headerBinding = ViewHeaderBinding.bind(binding.header.root)
            headerBinding.textPeanuts.text = getString(R.string.peanut_count_format, count)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
