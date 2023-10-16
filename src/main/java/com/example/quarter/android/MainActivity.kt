package com.example.quarter.android

import DataModel
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.quarter.android.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale


private var fictionalValue = ""


class  MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private val dataModel: DataModel by viewModels()
    private var fictionalValue = ""
    //private var fictionalResult = 0.0
    private val handler = Handler()
    private val interval: Long = 60 * 1000



    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var today = LocalDate.now()
        var lastDate = LocalDate.now().minusDays(1)
        var howMany = 0.0f




        //Получение текущего значения всего кол-ва денег
        var dateFull = LocalDate.now()
        var dayLimit = 0.0f
        var todayLimit = 0.0f        // нужно единоразово получить после выставления суммы
        var avarageDailyValue = 0.0f

        dataModel.dateFull.observe(this) {
            dateFull = it
            val numberOfDays: Long = ChronoUnit.DAYS.between(today, dateFull)
            dataModel.money.observe(this) {
                howMany = it
                binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
                avarageDailyValue = (howMany / numberOfDays).toFloat()
                binding.result.text = todayLimit.toString()
            }
        }

        dataModel.money.observe(this) {
            howMany = it
            dataModel.dateFull.observe(this) {
                dateFull = it
                val numberOfDays: Long = ChronoUnit.DAYS.between(today, dateFull)
                binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
                avarageDailyValue = (howMany / numberOfDays).toFloat()
                binding.result.text = todayLimit.toString()
            }
        }
        todayLimit = avarageDailyValue


        // Запустить повторяющуюся задачу
        dataModel.dayLimit.observe(this){
            dayLimit = it
        }
        dataModel.todayLimit.observe(this) {
            todayLimit = it
        }
        val runnable = object : Runnable {
            override fun run() {
                today = LocalDate.now()
                // Проверить дату
                if (today != lastDate) {
                    // нужно дописать supportFragmentManager
                    var numberOfDays: Long = ChronoUnit.DAYS.between(lastDate, today)
                    todayLimit += (avarageDailyValue * numberOfDays).toInt()     // 3000 поменять на среднесуточную
                    binding.result.text = todayLimit.toString()
                    lastDate = today
                }
                else{
                }

                // Запустить задачу снова через заданный интервал
                handler.postDelayed(this, interval)
            }
        }

        handler.post(runnable)



                // Фрагмент Settings
        binding.settings.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, BlankFragment2.newInstance())
                .addToBackStack(null)
                .commit()
        }

        val value: TextView = findViewById(R.id.value)
        //val result: TextView = findViewById(R.id.result)
        val displayMetrics = resources.displayMetrics.widthPixels/4.3

        // Изменения размеров кнопок
        fun buttonMetrics(button: Button): Unit {
            val layoutParams = button.layoutParams
            layoutParams.width = displayMetrics.toInt()
            layoutParams.height = displayMetrics.toInt()
            button.layoutParams = layoutParams
        }

        // Обработка нажатия на цифры
        fun buttonBinding(variable: String): Unit {
            if ((variable == "." && fictionalValue.contains("."))
                || (variable == "0" && fictionalValue.isEmpty())) {}
            else {
                fictionalValue += variable
                value.text = fictionalValue
            }
        }

        // Обьявили все кнопки
        val buttonEnter: Button = findViewById(R.id.button_enter)
        val butDelete: Button = findViewById(R.id.button_del)
        val buttons = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9, R.id.button_point
        )

        // Изменили размер
        buttons.forEach { buttonId ->
            val button = findViewById<Button>(buttonId)
            buttonMetrics(button)
            val buttonText = button.text.toString()
            button.setOnClickListener {buttonBinding(buttonText)}
        }
        buttonMetrics(buttonEnter)
        buttonMetrics(butDelete)

        // Обработка кнопки удалить
        butDelete.setOnClickListener {
            if (fictionalValue.isNotEmpty()) {
                fictionalValue = fictionalValue.substring(0, fictionalValue.length - 1)
                value.text = fictionalValue
            }
        }

        // Обработка кнопки enter
        val result: TextView = findViewById(R.id.result)
        buttonEnter.setOnClickListener {
            if ((fictionalValue.isNotEmpty()) && (value.text != ".")) {
                val fictionalDigit = fictionalValue.toFloat()
                todayLimit -= fictionalDigit
                todayLimit = (Math.round(todayLimit.toFloat() * 100.0) / 100.0).toFloat()
                result.text = "$todayLimit"
                dataModel.money.value = howMany - fictionalDigit
                dataModel.todayLimit.value = todayLimit.toFloat()
                fictionalValue = ""
                value.text = ""
            }
        }
    }
}

