package com.example.quarter.android

import DataModel
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.FragmentBudgetInputBinding

class BudgetInputFragment : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentBudgetInputBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBudgetInputBinding.inflate(inflater, container, false)
        binding.root.requestFocus()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var howMany = ""
        var isPlaceholder = true

        // Получение актуального кол-ва денег на весь срок
        dataModel.money.observe(activity as LifecycleOwner) {
            if (it != 0.0) {
                binding.howMany.text = it.toString()
                howMany = it.toString()
                isPlaceholder = false
            }
        }

        // Обработка кнопки save (применяет новое значение)
        binding.save.setOnClickListener {
            if (!isPlaceholder && howMany.isNotEmpty() && howMany != ".") {
                dataModel.money.value = "$howMany".toDouble()
                dataModel.saveClick.value = true
                HistoryManager(requireContext()).clear()
                parentFragmentManager.popBackStack()
            }
        }

        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Обработка кнопки стереть
        binding.buttondel.setOnClickListener {
            if (!isPlaceholder && howMany.isNotEmpty()) {
                howMany = howMany.substring(0, howMany.length - 1)
                binding.howMany.text = howMany
            }
        }

        // Подбиваем кнопки под размер дисплея
        val displayMetricsWidth = resources.displayMetrics.widthPixels
        val displayMetricsHeight = resources.displayMetrics.heightPixels
        fun <T : View> buttonMetrics(layout: T, metricsHeight: Double, metricsWidth: Double): Unit {
            val layoutParams = layout.layoutParams
            layoutParams.width = metricsWidth.toInt()
            layoutParams.height = metricsHeight.toInt()
            layout.layoutParams = layoutParams
        }

        // Связываем кнопки с функцией нажатия
        fun buttonBinding(variable: String): Unit {
            if ((variable == "." && howMany.contains("."))
                || (variable == "0" && howMany.isEmpty())) {}
            else {
                if (isPlaceholder) {
                    howMany = ""
                    isPlaceholder = false
                }
                howMany += variable
                binding.howMany.text = howMany
            }
        }

        // Обьявляем кнопки
        val buttons = listOf(
            binding.button0, binding.button1, binding.button2,
            binding.button3, binding.button4, binding.button5,
            binding.button6, binding.button7, binding.button8,
            binding.button9, binding.buttonPoint
        )

        // Задаем размеры
        for (button in buttons) {
            buttonMetrics(button,
                displayMetricsWidth/4.3, displayMetricsWidth/4.3)
            val buttonText = button.text.toString()
            button.setOnClickListener {buttonBinding(buttonText)}
        }
        buttonMetrics(binding.frameForMetrics,
            displayMetricsHeight/1.6, displayMetricsWidth/1.3)
    }

    companion object {
        @JvmStatic
        fun newInstance() = BudgetInputFragment()
    }
}
