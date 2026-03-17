package com.example.quarter.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryAdapter(
    private val entries: List<HistoryEntry>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("ru"))

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amount: TextView = itemView.findViewById(R.id.historyAmount)
        val date: TextView = itemView.findViewById(R.id.historyDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.amount.text = "- ${entry.amount}"
        holder.date.text = LocalDate.parse(entry.date).format(dateFormatter)
    }

    override fun getItemCount() = entries.size
}
