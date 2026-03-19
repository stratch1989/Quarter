package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
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
    ): View {
        binding = FragmentEveryDayQuestionBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Блокируем закрытие по фону — выбор обязателен
        binding.clickableBackground.setOnClickListener { }

        // Блокируем системную кнопку «Назад»
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { }
            }
        )

        var avarageDailyValueFirstOption = 0.0
        var avarageDailyValueSecondOption = 0.0
        var keyTodayLimitFirstOption = 0.0
        var keyTodayLimitSecondOption = 0.0
        var keyTodayLimit = 0.0

        // Наблюдатели вынесены на один уровень (не вложенные)
        dataModel.keyTodayLimit.observe(activity as LifecycleOwner) {
            keyTodayLimit = it
            binding.dayLimit.text = "Вчера вы сэкономили ${keyTodayLimit.toInt()} ₽"
        }

        dataModel.keyTodayLimitFirstOption.observe(activity as LifecycleOwner) {
            keyTodayLimitFirstOption = it
        }

        dataModel.avarageDailyValueFirstOption.observe(activity as LifecycleOwner) {
            avarageDailyValueFirstOption = it
        }

        dataModel.avarageDailyValueSecondOption.observe(activity as LifecycleOwner) {
            avarageDailyValueSecondOption = it
        }

        dataModel.avarageDailyValue.value = avarageDailyValueSecondOption

        dataModel.keyTodayLimitSecondOption.observe(activity as LifecycleOwner) {
            keyTodayLimitSecondOption = it
        }

        binding.option1.setOnClickListener {
            dataModel.avarageDailyValue.value = avarageDailyValueFirstOption

            dataModel.keyTodayLimit.value = keyTodayLimitFirstOption
            (requireActivity() as MainActivity).onFragmentClosed()
            parentFragmentManager.popBackStack()
        }
        binding.option2.setOnClickListener {
            dataModel.keyTodayLimit.value = keyTodayLimitSecondOption
            (requireActivity() as MainActivity).onFragmentClosed()
            parentFragmentManager.popBackStack()
        }

    }


    companion object {
        @JvmStatic
        fun newInstance() = EveryDayQuestion()
    }
}