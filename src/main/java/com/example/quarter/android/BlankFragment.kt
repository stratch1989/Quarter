package com.example.quarter.android

import DataModel
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.FragmentBlankBinding

class BlankFragment : Fragment() {
    private val dataModel: DataModel by activityViewModels()
    lateinit var binding: FragmentBlankBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_blank, container, false)
        view.requestFocus()
        binding = FragmentBlankBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var howMany = binding.howMany.text.toString()

        // Получение актуального кол-ва денег на весь срок
        dataModel.money.observe(activity as LifecycleOwner) {
            binding.howMany.text = it.toString()
            howMany = binding.howMany.text.toString()
        }

        // Обработка кнопки save (применяет новое значение)
        binding.save.setOnClickListener {
            if (binding.howMany.text != ".") {
                dataModel.money.value = "$howMany".toFloat()
                activity?.onBackPressed()
            }
        }

        // Обратка фона на вызов метода выхода из фрагмента
        binding.clickableBackground.setOnClickListener {
            activity?.onBackPressed()
        }

        // Обработка кнопки стереть
        binding.buttondel.setOnClickListener {
            if (howMany.isNotEmpty()) {
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
        fun newInstance() = BlankFragment()
    }
}