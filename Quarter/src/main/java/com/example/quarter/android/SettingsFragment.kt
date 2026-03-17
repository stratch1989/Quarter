package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.FragmentSettingsBinding
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class SettingsFragment : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var howMany = binding.howMany.text.toString()
        var daysText = binding.daysText.text.toString()
        val today = LocalDate.now()
        var dayLimit = 0


        // Получение актуального кол-ва денег на весь срок
        dataModel.money.observe(activity as LifecycleOwner) {
            if (it != 0.0) {
                binding.howMany.text = (Math.round(it * 100.0) / 100.0).toString()
                dayLimit = it.toInt()
                howMany = binding.howMany.text.toString()
            }
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
        dataModel.dayLimit.value = dayLimit.toDouble()
        binding.dayLimit.text = "${dayLimit} в день"


        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.frameMoney.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.place_holder, BudgetInputFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
        binding.frameCalendar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.place_holder, DatePickerFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
