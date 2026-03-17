package com.example.quarter.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quarter.android.databinding.FragmentHistoryBinding

class History : Fragment() {
    lateinit var binding: FragmentHistoryBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.closeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val historyManager = HistoryManager(requireContext())
        val entries = historyManager.loadEntries()

        if (entries.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.historyRecyclerView.adapter = HistoryAdapter(entries)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = History()
    }
}
