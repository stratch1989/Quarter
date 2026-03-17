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

        // Итого за текущий период
        val totalMoney = dataModel.money.value ?: 0.0
        val currentTotal = currentEntries.sumOf { it.amount }
        if (currentEntries.isNotEmpty()) {
            binding.periodTotal.visibility = View.VISIBLE
            binding.periodTotal.text = "Потрачено: ${dataModel.roundMoney(currentTotal)} ₽ из ${dataModel.roundMoney(totalMoney + currentTotal)} ₽"
        }

        // Группировка по дням
        val items = mutableListOf<HistoryItem>()
        groupByDay(currentEntries).forEach { (date, entries) ->
            val dayTotal = dataModel.roundMoney(entries.sumOf { it.amount })
            items.add(HistoryItem.DayHeader(date, dayTotal))
            entries.forEach { entry ->
                val currentIndex = currentEntries.indexOf(entry)
                items.add(HistoryItem.Current(entry, currentIndex))
            }
        }

        if (oldEntries.isNotEmpty()) {
            items.add(HistoryItem.PeriodDivider)
            groupByDay(oldEntries).forEach { (date, entries) ->
                val dayTotal = dataModel.roundMoney(entries.sumOf { it.amount })
                items.add(HistoryItem.DayHeader(date, dayTotal))
                entries.forEach { entry ->
                    items.add(HistoryItem.Old(entry))
                }
            }
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

                    // Обновляем итого
                    val newCurrentTotal = dataModel.roundMoney(currentTotal - removed.amount)
                    val newTotalBudget = dataModel.roundMoney(currentMoney + removed.amount + newCurrentTotal)
                    binding.periodTotal.text = "Потрачено: ${newCurrentTotal} ₽ из ${newTotalBudget} ₽"

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

    private fun groupByDay(entries: List<HistoryEntry>): Map<String, List<HistoryEntry>> {
        return entries.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    companion object {
        @JvmStatic
        fun newInstance() = History()
    }
}
