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

        // Инициализация текущих значений из DataModel
        fun setDaysTextWithCount(dateStr: String, dateFull: LocalDate) {
            val base = "По $dateStr"
            val days = ChronoUnit.DAYS.between(today, dateFull)
            if (days > 0) {
                val suffix = " ($days дн.)"
                val spannable = android.text.SpannableString(base + suffix)
                spannable.setSpan(android.text.style.RelativeSizeSpan(0.7f), base.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), base.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#888888")), base.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.daysText.text = spannable
            } else {
                binding.daysText.text = base
            }
        }

        var dateFull = dataModel.dateFull.value ?: LocalDate.now()

        dataModel.dayText.value?.let {
            setDaysTextWithCount(it, dateFull)
        }

        //Получение актуальной даты
        dataModel.dayText.observe(activity as LifecycleOwner) {
            setDaysTextWithCount(it, dateFull)
        }

        fun updateDayLimit() {
            val numberOfDays: Long = ChronoUnit.DAYS.between(today, dateFull)
            if (numberOfDays > 0 && dayLimit != 0) {
                val daily = dayLimit / numberOfDays.toInt()
                dataModel.dayLimit.value = daily.toDouble()
                binding.dayLimit.text = "${daily} в день"
            }
        }

        //Получение актуальной даты (полной)
        dataModel.dateFull.observe(activity as LifecycleOwner) {
            dateFull = it
            dataModel.dayText.value?.let { txt -> setDaysTextWithCount(txt, dateFull) }
            updateDayLimit()
        }
        updateDayLimit()


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
