package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quarter.android.databinding.FragmentHistoryBinding
import java.time.LocalDate

class History : Fragment() {
    private val dataModel: DataModel by activityViewModels()
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
        val allEntries = historyManager.loadEntries()
        val periodTs = historyManager.getPeriodStartTimestamp()

        val currentEntries = allEntries.filter { it.timestamp >= periodTs }
        val oldEntries = allEntries.filter { it.timestamp < periodTs }

        // Собираем список элементов для адаптера
        val items = mutableListOf<HistoryItem>()
        currentEntries.forEachIndexed { index, entry ->
            items.add(HistoryItem.Current(entry, index))
        }
        if (currentEntries.isNotEmpty() && oldEntries.isNotEmpty()) {
            items.add(HistoryItem.Divider)
        }
        oldEntries.forEach { entry ->
            items.add(HistoryItem.Old(entry))
        }

        if (items.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.historyRecyclerView.adapter = HistoryAdapter(items) { currentIndex ->
                val removed = historyManager.removeCurrentEntry(currentIndex)
                if (removed != null) {
                    // Возврат в общий бюджет
                    val currentMoney = dataModel.money.value ?: 0.0
                    dataModel.money.value = dataModel.roundMoney(currentMoney + removed.amount)

                    // Возврат в дневной лимит
                    val currentTodayLimit = dataModel.todayLimit.value ?: 0.0
                    val isToday = removed.date == LocalDate.now().toString()
                    if (isToday) {
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + removed.amount)
                    } else {
                        val days = dataModel.numberOfDays.value ?: 1L
                        val perDay = if (days > 0) removed.amount / days else removed.amount
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + perDay)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = History()
    }
}
