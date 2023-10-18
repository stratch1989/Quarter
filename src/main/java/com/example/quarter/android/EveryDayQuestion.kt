package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.quarter.android.databinding.FragmentEveryDayQuestionBinding

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
    }

    companion object {
        @JvmStatic
        fun newInstance() = EveryDayQuestion()
    }
}