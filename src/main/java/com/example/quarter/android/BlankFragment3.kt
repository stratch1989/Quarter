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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quarter.android.databinding.FragmentBlank2Binding
import com.example.quarter.android.databinding.FragmentBlank3Binding
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.time.format.DateTimeFormatter

class BlankFragment3 : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentBlank3Binding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blank3, container, false)
        view.requestFocus()
        binding = FragmentBlank3Binding.inflate(inflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            activity?.onBackPressed()
        }

        var numberOfDay = 0

        // numberOfDay нужен для отметки выбранного дня
        dataModel.dayNumber.observe(activity as LifecycleOwner) {
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
                dataModel.dayText.value = selectedDay
                dataModel.dateFull.value = dateFull                         ///// <------------
                dataModel.lastDate.value = LocalDate.now()
                activity?.onBackPressed()
            }
        }
        recyclerView.adapter = adapter



    }
    companion object {
        @JvmStatic
        fun newInstance() = BlankFragment3()
    }
}