package com.example.quarter.android

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class HistoryItem {
    data class DayHeader(val date: String, val total: Double) : HistoryItem()
    data class CategoryHeader(val category: String?, val total: Double) : HistoryItem()
    data class CategoryLegend(val label: String, val amount: Double, val color: Int) : HistoryItem()
    data class Current(val entry: HistoryEntry, val currentIndex: Int) : HistoryItem()
    object PeriodDivider : HistoryItem()
    data class Old(val entry: HistoryEntry) : HistoryItem()
}

class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val isIncome: Boolean = false,
    private val isEditMode: Boolean = false,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("ru"))
    private val today = LocalDate.now().toString()
    private val yesterday = LocalDate.now().minusDays(1).toString()

    companion object {
        const val TYPE_DAY_HEADER = 0
        const val TYPE_CURRENT = 1
        const val TYPE_PERIOD_DIVIDER = 2
        const val TYPE_OLD = 3
        const val TYPE_CATEGORY_HEADER = 4
        const val TYPE_CATEGORY_LEGEND = 5
    }

    class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTitle: TextView = itemView.findViewById(R.id.dayTitle)
        val dayTotal: TextView = itemView.findViewById(R.id.dayTotal)
    }

    class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.historyAmount)
        val date: TextView = itemView.findViewById(R.id.historyDate)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class LegendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dot: View = itemView.findViewById(R.id.legendDot)
        val label: TextView = itemView.findViewById(R.id.legendLabel)
        val amount: TextView = itemView.findViewById(R.id.legendAmount)
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryItem.DayHeader -> TYPE_DAY_HEADER
        is HistoryItem.CategoryHeader -> TYPE_CATEGORY_HEADER
        is HistoryItem.CategoryLegend -> TYPE_CATEGORY_LEGEND
        is HistoryItem.Current -> TYPE_CURRENT
        is HistoryItem.PeriodDivider -> TYPE_PERIOD_DIVIDER
        is HistoryItem.Old -> TYPE_OLD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DAY_HEADER, TYPE_CATEGORY_HEADER -> DayHeaderViewHolder(inflater.inflate(R.layout.list_item_history_day_header, parent, false))
            TYPE_CATEGORY_LEGEND -> LegendViewHolder(inflater.inflate(R.layout.list_item_category_legend, parent, false))
            TYPE_PERIOD_DIVIDER -> DividerViewHolder(inflater.inflate(R.layout.list_item_history_divider, parent, false))
            else -> EntryViewHolder(inflater.inflate(R.layout.list_item_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.DayHeader -> {
                holder as DayHeaderViewHolder
                holder.dayTitle.text = when (item.date) {
                    today -> "Сегодня"
                    yesterday -> "Вчера"
                    else -> LocalDate.parse(item.date).format(dateFormatter)
                }
                val sign = if (isIncome) "+" else "-"
                holder.dayTotal.text = "$sign ${item.total}"
            }
            is HistoryItem.CategoryHeader -> {
                holder as DayHeaderViewHolder
                holder.dayTitle.text = item.category ?: "Без категории"
                val sign = if (isIncome) "+" else "-"
                holder.dayTotal.text = "$sign ${item.total}"
            }
            is HistoryItem.CategoryLegend -> {
                holder as LegendViewHolder
                val bg = holder.dot.background as GradientDrawable
                bg.setColor(item.color)
                holder.label.text = item.label
                val formatted = if (item.amount == item.amount.toLong().toDouble())
                    item.amount.toLong().toString() else item.amount.toString()
                val sign = if (isIncome) "+" else "-"
                holder.amount.text = "$sign$formatted"
            }
            is HistoryItem.Current -> {
                holder as EntryViewHolder
                val sign = if (isIncome) "+" else "-"
                val cat = if (item.entry.category != null) " ${item.entry.category}" else ""
                val noteText = if (item.entry.note != null) {
                    val truncated = if (item.entry.note.length > 12) item.entry.note.take(12) + "..." else item.entry.note
                    " · $truncated"
                } else ""
                holder.amount.text = "$sign ${item.entry.amount}$cat$noteText"
                holder.date.text = ""
                holder.deleteButton.visibility = if (isEditMode) View.VISIBLE else View.GONE
                holder.deleteButton.setOnClickListener {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val currentItem = items[pos] as? HistoryItem.Current ?: return@setOnClickListener
                        onDelete(currentItem.currentIndex)
                        items.removeAt(pos)
                        notifyItemRemoved(pos)

                        // Обновляем currentIndex для оставшихся записей
                        for (i in items.indices) {
                            val item = items[i] as? HistoryItem.Current ?: continue
                            if (item.currentIndex > currentItem.currentIndex) {
                                items[i] = item.copy(currentIndex = item.currentIndex - 1)
                            }
                        }

                        // Находим заголовок над удалённой записью
                        var headerPos = pos - 1
                        while (headerPos >= 0 && items[headerPos] !is HistoryItem.DayHeader && items[headerPos] !is HistoryItem.CategoryHeader) {
                            headerPos--
                        }
                        if (headerPos < 0) return@setOnClickListener

                        // Считаем оставшиеся записи под этим заголовком
                        var newTotal = 0.0
                        var hasEntries = false
                        var i = headerPos + 1
                        while (i < items.size && items[i] is HistoryItem.Current) {
                            hasEntries = true
                            newTotal += (items[i] as HistoryItem.Current).entry.amount
                            i++
                        }

                        if (!hasEntries) {
                            items.removeAt(headerPos)
                            notifyItemRemoved(headerPos)
                        } else {
                            val roundedTotal = java.math.BigDecimal(newTotal.toString())
                                .setScale(2, java.math.RoundingMode.HALF_UP)
                                .toDouble()
                            when (val header = items[headerPos]) {
                                is HistoryItem.DayHeader -> items[headerPos] = header.copy(total = roundedTotal)
                                is HistoryItem.CategoryHeader -> items[headerPos] = header.copy(total = roundedTotal)
                                else -> return@setOnClickListener
                            }
                            notifyItemChanged(headerPos)
                        }
                    }
                }
            }
            is HistoryItem.Old -> {
                holder as EntryViewHolder
                val sign = if (isIncome) "+" else "-"
                val cat = if (item.entry.category != null) " ${item.entry.category}" else ""
                val noteText = if (item.entry.note != null) {
                    val truncated = if (item.entry.note.length > 12) item.entry.note.take(12) + "..." else item.entry.note
                    " · $truncated"
                } else ""
                holder.amount.text = "$sign ${item.entry.amount}$cat$noteText"
                holder.date.text = ""
                holder.deleteButton.visibility = View.GONE
            }
            is HistoryItem.PeriodDivider -> {}
        }
    }

    override fun getItemCount() = items.size
}
