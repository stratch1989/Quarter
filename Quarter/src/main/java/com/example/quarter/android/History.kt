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
    private lateinit var historyManager: HistoryManager
    private var viewMode = MODE_LIST

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }

    private fun dismissWithAnimation() {
        binding.clickableBackground.animate().alpha(0f).setDuration(200).withEndAction {
            if (isAdded) parentFragmentManager.popBackStack()
        }.start()
        binding.frameForMetrics.animate().alpha(0f).setDuration(200).start()
        binding.modeToggle.animate().alpha(0f).setDuration(200).start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Анимация появления
        binding.clickableBackground.alpha = 0f
        binding.frameForMetrics.alpha = 0f
        binding.modeToggle.alpha = 0f
        binding.clickableBackground.animate().alpha(1f).setDuration(200).start()
        binding.frameForMetrics.animate().alpha(1f).setDuration(200).start()
        binding.modeToggle.animate().alpha(1f).setDuration(200).start()

        binding.clickableBackground.setOnClickListener { dismissWithAnimation() }
        binding.frameForMetrics.setOnClickListener { }
        binding.modeToggle.setOnClickListener { }

        historyManager = HistoryManager(requireContext())

        binding.listModeButton.setOnClickListener { switchMode(MODE_LIST) }
        binding.categoryFilterButton.setOnClickListener { switchMode(MODE_CATEGORY) }
        binding.chartModeButton.setOnClickListener { switchMode(MODE_CHART) }

        refreshView()
    }

    private fun switchMode(mode: Int) {
        if (viewMode == mode) return
        viewMode = mode
        updateModeButtons()
        refreshView()
    }

    private fun updateModeButtons() {
        binding.listModeButton.setBackgroundColor(if (viewMode == MODE_LIST) 0xFF252525.toInt() else 0x00000000)
        binding.categoryFilterButton.setBackgroundColor(if (viewMode == MODE_CATEGORY) 0xFF252525.toInt() else 0x00000000)
        binding.chartModeButton.setBackgroundColor(if (viewMode == MODE_CHART) 0xFF252525.toInt() else 0x00000000)
        binding.historyTitle.text = when (viewMode) {
            MODE_CATEGORY -> "По категориям"
            MODE_CHART -> "Диаграмма"
            else -> "История трат"
        }
    }

    private fun refreshView() {
        val allEntries = historyManager.loadEntries()
        val periodTs = historyManager.getPeriodStartTimestamp()
        val currentEntries = allEntries.filter { it.timestamp >= periodTs }
        val oldEntries = allEntries.filter { it.timestamp < periodTs }

        // Итого за текущий период
        val totalMoney = dataModel.money.value ?: 0.0
        var currentTotal = currentEntries.sumOf { it.amount }
        if (currentEntries.isNotEmpty()) {
            binding.periodTotal.visibility = View.VISIBLE
            binding.periodTotal.text = "Потрачено: ${dataModel.roundMoney(currentTotal)} ₽ из ${dataModel.roundMoney(totalMoney + currentTotal)} ₽"
        } else {
            binding.periodTotal.visibility = View.GONE
        }

        if (viewMode == MODE_CHART) {
            showChart(currentEntries)
            return
        }

        // Список
        binding.chartContainer.visibility = View.GONE
        val items = if (viewMode == MODE_CATEGORY) {
            buildCategoryItems(currentEntries, oldEntries)
        } else {
            buildNormalItems(currentEntries, oldEntries)
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
                    val currentMoney = dataModel.money.value ?: 0.0
                    dataModel.money.value = dataModel.roundMoney(currentMoney + removed.amount)

                    currentTotal = dataModel.roundMoney(currentTotal - removed.amount)
                    val newTotalBudget = dataModel.roundMoney(currentMoney + removed.amount + currentTotal)
                    binding.periodTotal.text = "Потрачено: ${currentTotal} ₽ из ${newTotalBudget} ₽"

                    val currentTodayLimit = dataModel.todayLimit.value ?: 0.0
                    val isToday = removed.date == LocalDate.now().toString()
                    if (isToday) {
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + removed.amount)
                    } else {
                        val days = dataModel.numberOfDays.value ?: 1L
                        val perDay = if (days > 0) removed.amount / days else removed.amount
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + perDay)
                    }

                    dataModel.clearUndo.value = true
                }
            }
        }
    }

    private fun showChart(currentEntries: List<HistoryEntry>) {
        binding.historyRecyclerView.visibility = View.GONE
        binding.emptyText.visibility = View.GONE

        if (currentEntries.isEmpty()) {
            binding.chartContainer.visibility = View.GONE
            binding.emptyText.visibility = View.VISIBLE
            return
        }

        binding.chartContainer.visibility = View.VISIBLE

        val grouped = currentEntries.groupBy { it.category }
        // Сортировка: по сумме убывание, без категории в конце
        val sortedKeys = grouped.keys.sortedWith(
            compareBy<String?> { it == null }
                .thenByDescending { key -> grouped[key]?.sumOf { it.amount } ?: 0.0 }
        )

        var colorIndex = 0
        val segments = sortedKeys.map { category ->
            val amount = dataModel.roundMoney(grouped[category]!!.sumOf { it.amount })
            val label = category ?: "Без категории"
            val color = if (category == null) DonutChartView.NO_CATEGORY_COLOR
            else DonutChartView.COLORS[colorIndex++ % DonutChartView.COLORS.size]
            DonutChartView.Segment(label, amount, color)
        }

        binding.donutChart.setData(segments)
    }

    private fun buildNormalItems(
        currentEntries: List<HistoryEntry>,
        oldEntries: List<HistoryEntry>
    ): MutableList<HistoryItem> {
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

        return items
    }

    private fun buildCategoryItems(
        currentEntries: List<HistoryEntry>,
        oldEntries: List<HistoryEntry>
    ): MutableList<HistoryItem> {
        val items = mutableListOf<HistoryItem>()

        val grouped = currentEntries.groupBy { it.category }
        val sortedKeys = grouped.keys.sortedWith(
            compareBy<String?> { it == null }.thenBy { it ?: "" }
        )
        sortedKeys.forEach { category ->
            val entries = grouped[category] ?: return@forEach
            val total = dataModel.roundMoney(entries.sumOf { it.amount })
            items.add(HistoryItem.CategoryHeader(category, total))
            entries.forEach { entry ->
                val currentIndex = currentEntries.indexOf(entry)
                items.add(HistoryItem.Current(entry, currentIndex))
            }
        }

        if (oldEntries.isNotEmpty()) {
            items.add(HistoryItem.PeriodDivider)
            val oldGrouped = oldEntries.groupBy { it.category }
            val sortedOldKeys = oldGrouped.keys.sortedWith(
                compareBy<String?> { it == null }.thenBy { it ?: "" }
            )
            sortedOldKeys.forEach { category ->
                val entries = oldGrouped[category] ?: return@forEach
                val total = dataModel.roundMoney(entries.sumOf { it.amount })
                items.add(HistoryItem.CategoryHeader(category, total))
                entries.forEach { entry ->
                    items.add(HistoryItem.Old(entry))
                }
            }
        }

        return items
    }

    private fun groupByDay(entries: List<HistoryEntry>): Map<String, List<HistoryEntry>> {
        return entries.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    companion object {
        private const val MODE_LIST = 0
        private const val MODE_CATEGORY = 1
        private const val MODE_CHART = 2

        @JvmStatic
        fun newInstance() = History()
    }
}
