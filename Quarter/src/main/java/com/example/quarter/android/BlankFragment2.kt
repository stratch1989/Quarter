package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.FragmentBlank2Binding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


class BlankFragment2 : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentBlank2Binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blank2, container, false)
        view.requestFocus()
        binding = FragmentBlank2Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var howMany = binding.howMany.text.toString()
        var daysText = binding.daysText.text.toString()
        val today = LocalDate.now()
        var dayLimit = 0


        // Получение актуального кол-ва денег на весь срок
        dataModel.money.observe(activity as LifecycleOwner) {
            binding.howMany.text = (Math.round(it * 100.0) / 100.0).toString()
            dayLimit = it.toInt()
            howMany = binding.howMany.text.toString()
        }

        //Получение актуальной даты
        dataModel.dayText.observe(activity as LifecycleOwner) {
            val dayText = it
            binding.daysText.text = "По ${dayText}"
        }

        //Получение актуальной даты (полной)
        var dateFull = LocalDate.now()
        dataModel.dateFull.observe(activity as LifecycleOwner) {
            dateFull = it
        }

        // Заполняем дневной лимит
        val numberOfDays: Long = ChronoUnit.DAYS.between(today, dateFull)
        if (numberOfDays.toInt() != 0){
            dayLimit = dayLimit/numberOfDays.toInt()
        }
        dataModel.dayLimit.value = dayLimit.toFloat()
        binding.dayLimit.text = "${dayLimit} в день"


        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            activity?.onBackPressed()
        }

        binding.frameMoney.setOnClickListener {
            fragmentManager?.beginTransaction()
                ?.replace(R.id.place_holder, BlankFragment.newInstance())
                ?.addToBackStack(null)
                ?.commit()
        }
        binding.frameCalendar.setOnClickListener {
            fragmentManager?.beginTransaction()
                ?.replace(R.id.place_holder, BlankFragment3.newInstance())
                ?.addToBackStack(null)
                ?.commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = BlankFragment2()
    }
}