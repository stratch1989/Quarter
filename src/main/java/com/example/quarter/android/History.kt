package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.quarter.android.databinding.FragmentEveryDayQuestionBinding
import com.example.quarter.android.databinding.FragmentHistoryBinding

class History : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentHistoryBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        view.requestFocus()
        binding = FragmentHistoryBinding.inflate(inflater)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = History ()
    }
}