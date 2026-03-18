package com.example.quarter.android

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
    data class Current(val entry: HistoryEntry, val currentIndex: Int) : HistoryItem()
    object PeriodDivider : HistoryItem()
    data class Old(val entry: HistoryEntry) : HistoryItem()
}

class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
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

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryItem.DayHeader -> TYPE_DAY_HEADER
        is HistoryItem.Current -> TYPE_CURRENT
        is HistoryItem.PeriodDivider -> TYPE_PERIOD_DIVIDER
        is HistoryItem.Old -> TYPE_OLD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DAY_HEADER -> DayHeaderViewHolder(inflater.inflate(R.layout.list_item_history_day_header, parent, false))
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
                holder.dayTotal.text = "- ${item.total} ₽"
            }
            is HistoryItem.Current -> {
                holder as EntryViewHolder
                val cat = if (item.entry.category != null) " ${item.entry.category}" else ""
                holder.amount.text = "- ${item.entry.amount} ₽$cat"
                holder.date.text = ""
                holder.deleteButton.visibility = View.VISIBLE
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

                        // Находим заголовок дня над удалённой записью
                        var headerPos = pos - 1
                        while (headerPos >= 0 && items[headerPos] !is HistoryItem.DayHeader) {
                            headerPos--
                        }
                        if (headerPos < 0) return@setOnClickListener
                        val header = items[headerPos] as HistoryItem.DayHeader

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
                            // Удаляем пустой заголовок дня
                            items.removeAt(headerPos)
                            notifyItemRemoved(headerPos)
                        } else {
                            // Обновляем сумму в заголовке
                            items[headerPos] = header.copy(total = Math.round(newTotal * 100.0) / 100.0)
                            notifyItemChanged(headerPos)
                        }
                    }
                }
            }
            is HistoryItem.Old -> {
                holder as EntryViewHolder
                val cat = if (item.entry.category != null) " ${item.entry.category}" else ""
                holder.amount.text = "- ${item.entry.amount} ₽$cat"
                holder.date.text = ""
                holder.deleteButton.visibility = View.GONE
            }
            is HistoryItem.PeriodDivider -> {}
        }
    }

    override fun getItemCount() = items.size
}
