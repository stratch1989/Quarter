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
        val entries = historyManager.loadEntries().toMutableList()

        if (entries.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.historyRecyclerView.adapter = HistoryAdapter(entries) { position ->
                val removed = historyManager.removeEntry(position)
                if (removed != null) {
                    // Возврат в общий бюджет
                    val currentMoney = dataModel.money.value ?: 0.0
                    dataModel.money.value = dataModel.roundMoney(currentMoney + removed.amount)

                    // Возврат в дневной лимит
                    val currentTodayLimit = dataModel.todayLimit.value ?: 0.0
                    val isToday = removed.date == LocalDate.now().toString()
                    if (isToday) {
                        // За сегодня — возвращаем целиком
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + removed.amount)
                    } else {
                        // За прошлые дни — размазываем на оставшиеся дни
                        val days = dataModel.avarageDailyValue.value?.let {
                            val money = dataModel.money.value ?: 0.0
                            if (it != 0.0) (money / it).toLong() else 1L
                        } ?: 1L
                        val perDay = if (days > 0) removed.amount / days else removed.amount
                        dataModel.todayLimit.value = dataModel.roundMoney(currentTodayLimit + perDay)
                    }
                }
                if (entries.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.historyRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = History()
    }
}
