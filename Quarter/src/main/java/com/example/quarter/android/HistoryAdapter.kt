package com.example.quarter.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class HistoryItem {
    data class Current(val entry: HistoryEntry, val currentIndex: Int) : HistoryItem()
    object Divider : HistoryItem()
    data class Old(val entry: HistoryEntry) : HistoryItem()
}

class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("ru"))

    companion object {
        const val TYPE_CURRENT = 0
        const val TYPE_DIVIDER = 1
        const val TYPE_OLD = 2
    }

    class CurrentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.historyAmount)
        val date: TextView = itemView.findViewById(R.id.historyDate)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteButton)
    }

    class OldViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.historyAmount)
        val date: TextView = itemView.findViewById(R.id.historyDate)
        val deleteButton: TextView = itemView.findViewById(R.id.deleteButton)
    }

    class DividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HistoryItem.Current -> TYPE_CURRENT
        is HistoryItem.Divider -> TYPE_DIVIDER
        is HistoryItem.Old -> TYPE_OLD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DIVIDER -> DividerViewHolder(inflater.inflate(R.layout.list_item_history_divider, parent, false))
            else -> CurrentViewHolder(inflater.inflate(R.layout.list_item_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.Current -> {
                holder as CurrentViewHolder
                holder.amount.text = "- ${item.entry.amount}"
                holder.date.text = LocalDate.parse(item.entry.date).format(dateFormatter)
                holder.deleteButton.visibility = View.VISIBLE
                holder.deleteButton.setOnClickListener {
                    val pos = holder.adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val currentItem = items[pos] as? HistoryItem.Current ?: return@setOnClickListener
                        onDelete(currentItem.currentIndex)
                        items.removeAt(pos)
                        notifyItemRemoved(pos)
                    }
                }
            }
            is HistoryItem.Old -> {
                holder as CurrentViewHolder
                holder.amount.text = "- ${item.entry.amount}"
                holder.date.text = LocalDate.parse(item.entry.date).format(dateFormatter)
                holder.deleteButton.visibility = View.GONE
            }
            is HistoryItem.Divider -> {}
        }
    }

    override fun getItemCount() = items.size
}
