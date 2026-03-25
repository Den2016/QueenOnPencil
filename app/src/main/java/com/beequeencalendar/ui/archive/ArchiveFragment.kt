package com.beequeencalendar.ui.archive

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beequeencalendar.R
import com.beequeencalendar.databinding.FragmentArchiveBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ArchiveFragment : Fragment() {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArchiveViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ArchiveAdapter(
            lifecycleOwner = viewLifecycleOwner,
            viewModel = viewModel,
            onClick = { graftId ->
                findNavController().navigate(
                    R.id.action_archive_to_graftEdit,
                    bundleOf("graftId" to graftId)
                )
            },
            onDelete = { graftId ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить прививку?")
                    .setMessage("Прививка и все связанные события будут удалены.")
                    .setPositiveButton("Удалить") { _, _ -> viewModel.delete(graftId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.graftings.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
