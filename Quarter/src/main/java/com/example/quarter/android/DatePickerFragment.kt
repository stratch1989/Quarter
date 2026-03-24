package com.example.quarter.android

import DataModel
import DayAdapter
import DayItem
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quarter.android.databinding.FragmentDatePickerBinding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar

class DatePickerFragment : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    private var _binding: FragmentDatePickerBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDatePickerBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        var numberOfDay = 0

        // numberOfDay нужен для отметки выбранного дня
        dataModel.dayNumber.observe(viewLifecycleOwner) {
            numberOfDay = it
        }

        // Заполняет список дней
        fun generateDays(): List<DayItem> {
            val dayList = mutableListOf<DayItem>()
            val calendar = Calendar.getInstance()

            val today = LocalDate.now()
            // что бы была разница между сегодня и сегодня - 1 день
            val betweenToday = today.plusDays(1)

            dayList.add(DayItem("Сегодня", 1, numberOfDay, betweenToday))

            for (i in 0 until 39) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val date = SimpleDateFormat("dd MMMM").format(calendar.time)
                val currentDate = today.plusDays(i+2.toLong())
                dayList.add(DayItem(date, i+2, numberOfDay, currentDate))
            }
            return dayList
        }

        var money = 0.0
        dataModel.money.observe(viewLifecycleOwner) {
            money = it
        }

        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val dayList = generateDays()
        val adapter = DayAdapter(dayList) { position ->
            // Обработка нажатия на элемент списка, position - позиция выбранного дня
            val selectedDay = dayList[position].day
            val selectedNumberOfDay = dayList[position].number
            val dateFull = dayList[position].dateFull
            binding.save.setOnClickListener {
                dataModel.dayNumber.value = selectedNumberOfDay
                dataModel.keyTodayLimit.value = Math.round((money/selectedNumberOfDay) * 100.0) / 100.0
                dataModel.dayText.value = selectedDay
                dataModel.dateFull.value = dateFull
                dataModel.lastDate.value = LocalDate.now()
                HistoryManager(requireContext()).updatePeriodStart()
                parentFragmentManager.popBackStack()
            }
        }
        recyclerView.adapter = adapter
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = DatePickerFragment()
    }
}
