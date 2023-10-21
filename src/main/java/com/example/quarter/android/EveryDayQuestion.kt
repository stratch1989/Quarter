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


        binding.option1.setOnClickListener {
            var avarageDailyValueFirstOption = 0f
            dataModel.avarageDailyValueFirstOption.observe(activity as LifecycleOwner){
                avarageDailyValueFirstOption = it
            }
            dataModel.avarageDailyValue.value = avarageDailyValueFirstOption

            var keyTodayLimitFirstOption = 0f
            dataModel.keyTodayLimitFirstOption.observe(activity as LifecycleOwner){
                keyTodayLimitFirstOption = it
            }
            dataModel.keyTodayLimit.value = keyTodayLimitFirstOption
            (requireActivity() as MainActivity).onFragmentClosed()
            activity?.onBackPressed()
        }
        binding.option2.setOnClickListener {
            var avarageDailyValueSecondOption = 0f
            dataModel.avarageDailyValueSecondOption.observe(activity as LifecycleOwner){
                avarageDailyValueSecondOption = it
            }
            dataModel.avarageDailyValue.value = avarageDailyValueSecondOption

            var keyTodayLimitSecondOption = 0f
            dataModel.keyTodayLimitSecondOption.observe(activity as LifecycleOwner){
                keyTodayLimitSecondOption = it
            }
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