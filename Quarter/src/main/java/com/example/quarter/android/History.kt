package com.example.quarter.android

import DataModel
import android.os.Bundle
import android.util.TypedValue
import android.widget.FrameLayout
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
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyManager: HistoryManager
    private var viewMode = MODE_LIST
    private var isIncomeMode = false
    private var isEditMode = false
    var onEntryClick: ((HistoryEntry, Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }

    private fun dismissWithAnimation() {
        binding.clickableBackground.animate().alpha(0f).setDuration(200).withEndAction {
            if (isAdded) parentFragmentManager.popBackStack()
        }.start()
        binding.frameForMetrics.animate().alpha(0f).setDuration(200).start()
        binding.modeToggle.animate().alpha(0f).setDuration(200).start()
        binding.typeToggle.animate().alpha(0f).setDuration(200).start()
        binding.editButton.animate().alpha(0f).setDuration(200).start()
        binding.confirmButton.animate().alpha(0f).setDuration(200).start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Адаптивные размеры от baseUnit
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val baseUnit = (if (isLandscape) screenHeight / 4.3 else screenWidth / 4.3).toFloat()
        val dp = resources.displayMetrics.density

        // Размер попапа: ~65% ширины экрана (portrait) или ~40% (landscape)
        val popupSize = if (isLandscape) (screenHeight * 0.60f).toInt() else (screenWidth * 0.72f).toInt()
        val popupMarginStart = (16 * dp).toInt()
        val popupMarginTop = (4 * dp).toInt()

        // frameForMetrics
        val popupHeight = (popupSize * 1.4f).toInt()
        (binding.frameForMetrics.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let {
            it.width = popupSize
            it.height = popupHeight
            binding.frameForMetrics.layoutParams = it
        }

        // Заголовок текст
        binding.historyTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.17f)
        val titlePadH = (baseUnit * 0.2f).toInt()
        val titlePadV = (baseUnit * 0.1f).toInt()
        binding.historyTitle.setPadding(titlePadH, titlePadV, titlePadH, titlePadV)

        // periodTotal, emptyText
        binding.periodTotal.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.13f)
        binding.emptyText.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.14f)

        // Оценим высоту заголовка для позиционирования
        val titleHeight = (titlePadV * 2 + baseUnit * 0.17f + 8 * dp).toInt()

        // modeToggle — справа от попапа
        (binding.modeToggle.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.marginStart = popupMarginStart + popupSize + (4 * dp).toInt()
            it.topMargin = popupMarginTop + titleHeight + (popupHeight * 0.4f).toInt()
            it.width = (baseUnit * 0.42f).toInt()
            binding.modeToggle.layoutParams = it
        }
        val modeItemHeight = (baseUnit * 0.46f).toInt()
        binding.listModeButton.layoutParams.height = modeItemHeight
        binding.listModeButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.20f)
        binding.categoryFilterButton.layoutParams.height = modeItemHeight
        binding.chartModeButton.layoutParams.height = modeItemHeight
        binding.chartModeButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.26f)

        // typeToggle — снизу от попапа
        (binding.typeToggle.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.marginStart = popupMarginStart + (popupSize * 0.6f).toInt()
            it.topMargin = popupMarginTop + titleHeight + popupHeight + (12 * dp).toInt()
            it.height = (baseUnit * 0.42f).toInt()
            binding.typeToggle.layoutParams = it
        }
        val typeItemWidth = (baseUnit * 0.5f).toInt()
        binding.expenseButton.layoutParams.width = typeItemWidth
        binding.expenseButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.22f)
        binding.incomeButton.layoutParams.width = typeItemWidth
        binding.incomeButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * 0.22f)

        // Кнопки edit/confirm/back — диаметр = высота заголовка
        binding.historyTitle.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val actionBtnSize = binding.historyTitle.measuredHeight
        val actionBtnMarginStart = popupMarginStart + popupSize - actionBtnSize - (2 * dp).toInt()
        listOf(binding.editButton, binding.confirmButton, binding.categoryBackButton).forEach { btn ->
            (btn.layoutParams as? FrameLayout.LayoutParams)?.let {
                it.width = actionBtnSize
                it.height = actionBtnSize
                it.marginStart = actionBtnMarginStart
                it.topMargin = popupMarginTop
                btn.layoutParams = it
            }
        }

        // Padding внутри попапа
        val popupPadding = (baseUnit * 0.24f).toInt()
        (binding.frameForMetrics.getChildAt(0) as? android.widget.LinearLayout)?.setPadding(popupPadding, popupPadding, popupPadding, popupPadding)

        // Анимация появления
        binding.clickableBackground.alpha = 0f
        binding.frameForMetrics.alpha = 0f
        binding.modeToggle.alpha = 0f
        binding.typeToggle.alpha = 0f
        binding.editButton.alpha = 0f
        binding.clickableBackground.animate().alpha(1f).setDuration(200).start()
        binding.frameForMetrics.animate().alpha(1f).setDuration(200).start()
        binding.modeToggle.animate().alpha(1f).setDuration(200).start()
        binding.typeToggle.animate().alpha(1f).setDuration(200).start()
        binding.editButton.animate().alpha(1f).setDuration(200).start()

        binding.clickableBackground.setOnClickListener { dismissWithAnimation() }
        binding.frameForMetrics.setOnClickListener { }
        binding.modeToggle.setOnClickListener { }
        binding.typeToggle.setOnClickListener { }

        binding.editButton.setOnClickListener {
            isEditMode = true
            binding.editButton.visibility = View.GONE
            binding.confirmButton.alpha = 1f
            binding.confirmButton.visibility = View.VISIBLE
            binding.modeToggle.visibility = View.GONE
            binding.typeToggle.visibility = View.GONE
            refreshView()
        }

        binding.confirmButton.setOnClickListener {
            isEditMode = false
            binding.confirmButton.visibility = View.GONE
            binding.editButton.alpha = 1f
            binding.editButton.visibility = View.VISIBLE
            binding.modeToggle.visibility = View.VISIBLE
            binding.typeToggle.visibility = View.VISIBLE
            refreshView()
        }

        historyManager = HistoryManager(requireContext())

        binding.listModeButton.setOnClickListener { switchMode(MODE_LIST) }
        binding.categoryFilterButton.setOnClickListener { switchMode(MODE_CATEGORY) }
        binding.chartModeButton.setOnClickListener { switchMode(MODE_CHART) }

        binding.expenseButton.setOnClickListener { switchType(false) }
        binding.incomeButton.setOnClickListener { switchType(true) }

        refreshView()
    }

    private fun switchType(income: Boolean) {
        if (isIncomeMode == income) return
        isIncomeMode = income
        binding.categoryBackButton.visibility = View.GONE
        updateTypeButtons()
        refreshView()
    }

    private fun switchMode(mode: Int) {
        if (viewMode == mode) return
        viewMode = mode
        binding.categoryBackButton.visibility = View.GONE
        updateModeButtons()
        refreshView()
    }

    private fun updateTypeButtons() {
        binding.expenseButton.setBackgroundColor(if (!isIncomeMode) 0xFF252525.toInt() else 0x00000000)
        binding.incomeButton.setBackgroundColor(if (isIncomeMode) 0xFF252525.toInt() else 0x00000000)
    }

    private fun updateModeButtons() {
        binding.listModeButton.setBackgroundColor(if (viewMode == MODE_LIST) 0xFF252525.toInt() else 0x00000000)
        binding.categoryFilterButton.setBackgroundColor(if (viewMode == MODE_CATEGORY) 0xFF252525.toInt() else 0x00000000)
        binding.chartModeButton.setBackgroundColor(if (viewMode == MODE_CHART) 0xFF252525.toInt() else 0x00000000)
        val prefix = if (isIncomeMode) "пополнений" else "трат"
        binding.historyTitle.text = when (viewMode) {
            MODE_CATEGORY -> "По категориям"
            MODE_CHART -> "Диаграмма"
            else -> "История $prefix"
        }
        // Кнопка edit только в режиме списка
        if (!isEditMode) {
            binding.editButton.visibility = if (viewMode == MODE_LIST) View.VISIBLE else View.GONE
        }
    }

    private fun refreshView() {
        updateModeButtons()
        val allEntries = if (isIncomeMode) historyManager.loadIncomeEntries() else historyManager.loadEntries()
        val periodTs = historyManager.getPeriodStartTimestamp()
        val currentEntries = allEntries.filter { it.timestamp >= periodTs }
        val oldEntries = allEntries.filter { it.timestamp < periodTs }

        // Итого за текущий период
        var currentTotal = currentEntries.sumOf { it.amount }
        if (currentEntries.isNotEmpty()) {
            binding.periodTotal.visibility = View.VISIBLE
            if (isIncomeMode) {
                binding.periodTotal.text = "${dataModel.roundMoney(currentTotal)}"
            } else {
                val totalMoney = dataModel.money.value ?: 0.0
                binding.periodTotal.text = "${dataModel.roundMoney(currentTotal)} из ${dataModel.roundMoney(totalMoney + currentTotal)}"
            }
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
            binding.emptyText.text = if (isIncomeMode) "Пополнения за текущий период\nпоявятся здесь" else "Траты за текущий период\nпоявятся здесь"
            binding.emptyText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.historyRecyclerView.adapter = HistoryAdapter(items, isIncomeMode, isEditMode, onItemClick = { entry ->
                onEntryClick?.invoke(entry, isIncomeMode)
                dismissWithAnimation()
            }, onCategoryClick = { entries, categoryName ->
                showCategoryEntriesPopup(entries, categoryName)
            }) { currentIndex ->
                val removed = historyManager.removeCurrentEntry(currentIndex, isIncomeMode)
                if (removed != null) {
                    if (isIncomeMode) {
                        // Удаление пополнения — вычитаем из бюджета
                        val currentMoney = dataModel.money.value ?: 0.0
                        dataModel.money.value = dataModel.roundMoney(currentMoney - removed.amount)

                        currentTotal = dataModel.roundMoney(currentTotal - removed.amount)
                        binding.periodTotal.text = "${dataModel.roundMoney(currentTotal)}"

                        val currentTodayLimit = dataModel.todayLimit.value ?: 0.0
                        val days = dataModel.numberOfDays.value ?: 1L
                        val perDay = if (days > 0) removed.amount / days else removed.amount
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit - perDay)
                    } else {
                        // Удаление траты — возвращаем в бюджет
                        val currentMoney = dataModel.money.value ?: 0.0
                        dataModel.money.value = dataModel.roundMoney(currentMoney + removed.amount)

                        currentTotal = dataModel.roundMoney(currentTotal - removed.amount)
                        val newTotalBudget = dataModel.roundMoney(currentMoney + removed.amount + currentTotal)
                        binding.periodTotal.text = "${currentTotal} из ${newTotalBudget}"

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

        fun addLegendItems(entries: List<HistoryEntry>) {
            val grouped = entries.groupBy { it.category }
            val sortedKeys = grouped.keys.sortedWith(
                compareBy<String?> { it == null }
                    .thenByDescending { key -> grouped[key]?.sumOf { it.amount } ?: 0.0 }
            )
            var colorIndex = 0
            sortedKeys.forEach { category ->
                val total = dataModel.roundMoney(grouped[category]!!.sumOf { it.amount })
                val label = category ?: "Без категории"
                val color = if (category == null) DonutChartView.NO_CATEGORY_COLOR
                else DonutChartView.COLORS[colorIndex++ % DonutChartView.COLORS.size]
                val catEntries = grouped[category]!!
                items.add(HistoryItem.CategoryLegend(label, total, color, catEntries.size, catEntries))
            }
        }

        addLegendItems(currentEntries)

        if (oldEntries.isNotEmpty()) {
            items.add(HistoryItem.PeriodDivider)
            addLegendItems(oldEntries)
        }

        return items
    }

    private fun showCategoryEntriesPopup(entries: List<HistoryEntry>, categoryName: String) {
        // Временно переключаемся в режим списка с фильтром по категории
        val items = mutableListOf<HistoryItem>()
        groupByDay(entries).forEach { (date, dayEntries) ->
            val dayTotal = dataModel.roundMoney(dayEntries.sumOf { it.amount })
            items.add(HistoryItem.DayHeader(date, dayTotal))
            dayEntries.forEach { entry ->
                items.add(HistoryItem.Old(entry))
            }
        }

        binding.historyTitle.text = categoryName
        binding.editButton.visibility = View.GONE
        binding.categoryBackButton.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE
        binding.historyRecyclerView.visibility = View.VISIBLE
        binding.historyRecyclerView.adapter = HistoryAdapter(items, isIncomeMode, false, onItemClick = { entry ->
            onEntryClick?.invoke(entry, isIncomeMode)
            dismissWithAnimation()
        }) { _ -> }

        // Клик на кнопку назад — вернуться к списку категорий
        binding.categoryBackButton.setOnClickListener {
            binding.categoryBackButton.visibility = View.GONE
            binding.editButton.visibility = View.VISIBLE
            refreshView()
        }
    }

    private fun groupByDay(entries: List<HistoryEntry>): Map<String, List<HistoryEntry>> {
        return entries.groupBy { it.date }.toSortedMap(compareByDescending { it })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MODE_LIST = 0
        private const val MODE_CATEGORY = 1
        private const val MODE_CHART = 2

        @JvmStatic
        fun newInstance() = History()
    }
}
