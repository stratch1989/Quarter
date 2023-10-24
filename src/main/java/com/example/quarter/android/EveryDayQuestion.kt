package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.FragmentEveryDayQuestionBinding
import java.time.temporal.ChronoUnit

class EveryDayQuestion : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentEveryDayQuestionBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_every_day_question, container, false)
        view.requestFocus()
        binding = FragmentEveryDayQuestionBinding.inflate(inflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            activity?.onBackPressed()
        }

        var avarageDailyValueFirstOption = 0f
        var avarageDailyValueSecondOption = 0f
        var keyTodayLimitFirstOption = 0f
        var keyTodayLimitSecondOption = 0f
        dataModel.keyTodayLimitFirstOption.observe(activity as LifecycleOwner){
            keyTodayLimitFirstOption = it
            var keyTodayLimit = 0f
            dataModel.keyTodayLimit.observe(activity as LifecycleOwner){
                keyTodayLimit = it
                binding.dayLimit.text = "Вчера вы сэкономили ${keyTodayLimit.toInt()} ₽"
                //binding.allToday.text =
                //    "${keyTodayLimitFirstOption.toInt()} вместо ${keyTodayLimit.toInt()} на сегодня"
            }
        }
        dataModel.avarageDailyValueFirstOption.observe(activity as LifecycleOwner){
            avarageDailyValueFirstOption = it
        }


        dataModel.avarageDailyValueSecondOption.observe(activity as LifecycleOwner){
            avarageDailyValueSecondOption = it
            var keyTodayLimit = 0f
            dataModel.keyTodayLimit.observe(activity as LifecycleOwner) {
                keyTodayLimit = it
                //binding.avarageDayLim.text =
                //    "Получится ${avarageDailyValueSecondOption.toInt()} вместо ${keyTodayLimit.toInt()} в день"
            }
        }
        dataModel.avarageDailyValue.value = avarageDailyValueSecondOption
        dataModel.keyTodayLimitSecondOption.observe(activity as LifecycleOwner){
            keyTodayLimitSecondOption = it
        }



        binding.option1.setOnClickListener {
            dataModel.avarageDailyValue.value = avarageDailyValueFirstOption

            dataModel.keyTodayLimit.value = keyTodayLimitFirstOption
            (requireActivity() as MainActivity).onFragmentClosed()
            activity?.onBackPressed()
        }
        binding.option2.setOnClickListener {
            dataModel.keyTodayLimit.value = keyTodayLimitSecondOption
            (requireActivity() as MainActivity).onFragmentClosed()
            activity?.onBackPressed()
        }

    }


    companion object {
        @JvmStatic
        fun newInstance() = EveryDayQuestion()
    }
}