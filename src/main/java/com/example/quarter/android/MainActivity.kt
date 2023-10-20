package com.example.quarter.android

/*
    Возможные варианты проверки приложения:

    1. назначить сумму и дату и вычесть произвольную сумму
    2. назначить дату и сумму и вычесть произвольную сумму

    3. назначить сумму и выбрать день сегодня
    4. выбрать сегодня и назначить сумму

    5. сменить дату и вычесть произвольную сумму
    6. сменить сумму и вычесть произвольную сумму

    7. изменить текущий день, с помощью сис настроек устройства
            должна прибавиться среднесуточная
    8. сделать вычет из суммы на текущий день, а затем сменить дату
 */

import DataModel
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.quarter.android.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import android.content.Context



private var fictionalValue = ""


class  MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private val dataModel: DataModel by viewModels()
    private var fictionalValue = ""
    private val handler = Handler()
    private val interval: Long = 1000

    var todayLimit = 0f // сумма на день
    var avarageDailyValue = 0f // среднесуточное
    //var resulta = ""
    var howMany = 0f
    var numberOfDays = 0L
    var dateFull = LocalDate.now().minusDays(1)

    var keyTodayLimit = 0f

    var lAvarageDailyValue = -0.001f

    var today = LocalDate.now()
    var lastDate = LocalDate.now()


    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadData()

        val result: TextView = findViewById(R.id.result)

        dataModel.dateFull.observe(this) {
            dateFull = it
            numberOfDays = ChronoUnit.DAYS.between(today, dateFull)
            dataModel.money.observe(this) {
                howMany = it
                binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
                avarageDailyValue = (howMany / numberOfDays)
            }
        }

        // Эксперементриую с решением бага связанным со сменой даты
        // !!!!!!!!! Тут нашелся косяк !!!!!!!!!!!!
        // Если трачу не весь дневной лимит, то сумма остается прежней
        // Можно сделать проверку дня не в ежесекундном режиме, а в сохранении
        dataModel.dateFull.observe(this) {
            if (howMany != 0.0f){
                todayLimit = 0.0f
                todayLimit += (avarageDailyValue).toInt()
                if (keyTodayLimit != 0f) {
                    todayLimit = keyTodayLimit
                    keyTodayLimit = 0f
                }
                binding.result.text = todayLimit.toString()
                lastDate = today
            }
        }

        // проверка на смену суток
        val runnable = object : Runnable {
            override fun run() {
                today = LocalDate.now()
                // Проверить дату
                /*
                if ((today != lastDate) && (avarageDailyValue != 0.0f)) {
                    // нужно дописать supportFragmentManager
                    val days: Long = (ChronoUnit.DAYS.between(lastDate, today))
// заменил averageDailyValue для того что бы после смены суток добавлялась обновленная дневная сумма
                    avarageDailyValue = (howMany / (numberOfDays-1))

                    todayLimit += (avarageDailyValue * days).toInt()
                    binding.result.text = todayLimit.toString()


                    lastDate = today
                    numberOfDays = ChronoUnit.DAYS.between(today, dateFull)
                    binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
                }
                 */
                handler.postDelayed(this, interval) //интервал - каждая секнда
                saveData()
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

        fun ButtonEnter(fictionalDigit: Float) {
            todayLimit -= fictionalDigit
            todayLimit = (Math.round(todayLimit * 100.0) / 100.0).toFloat()
            result.text = "$todayLimit"
            dataModel.money.value = howMany - fictionalDigit
            dataModel.todayLimit.value = todayLimit.toFloat()
        }

        // Обработка кнопки enter
        buttonEnter.setOnClickListener {
            if ((fictionalValue.isNotEmpty()) && (value.text != ".")) {
                val fictionalDigit = fictionalValue.toFloat()
                ButtonEnter(fictionalDigit)
                fictionalValue = ""
                value.text = ""
            }
        }

        dataModel.saveClick.observe(this) {
            // суть этой штуки в том, что бы заполнялось поле дневного лимита в первый день
            // после выставления деняк, а так же закрывает баг с неизменяющимся дневным лимитом
            // после смены денег
            todayLimit = 0.0f
            todayLimit += ((avarageDailyValue).toInt())
            result.text = todayLimit.toString()
        }

    }


    // Функция сохранения данных в SharedPreferences
    private fun saveData() {

        // result howMany numberOfDays dateFull
        // возможно проблема в val


        val result: String = binding.result.text.toString()        /////
        val sHowMany: Float = howMany
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val dateFull = dateFull.toString()
        val sLastDate = lastDate.toString()

        editor.putString("STRING_KEY", result)    // result

        editor.putFloat("HOW_MANY", sHowMany)      // какой-то жесткий баг
        editor.putLong("NUMBER_OF_DAYS", numberOfDays)
        editor.putFloat("AVARAGE_DAILY_VALUE", avarageDailyValue)  // по сути не нужно

        editor.putString("DATE_FULL", dateFull)
        editor.putString("LAST_DATE", sLastDate)



        editor.apply()
    }

    // Функция восстановления данных из SharedPreferences
    private fun loadData() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        val result: String? = sharedPreferences.getString("STRING_KEY", "0")   // result
        keyTodayLimit = result!!.toFloat()

        numberOfDays = sharedPreferences.getLong("NUMBER_OF_DAYS", 0L)
        lAvarageDailyValue = sharedPreferences.getFloat("AVARAGE_DAILY_VALUE", 0f)

        //dateFull
        val dateFullString = sharedPreferences.getString("DATE_FULL", "")
        if (dateFullString!!.isNotEmpty()) {    // преобразование LocalDate! в String
            dateFull = LocalDate.parse(dateFullString)
            dataModel.dateFull.value = dateFull
        } else {
            LocalDate.now()
        }

        //lastDate
        val lastDateString = sharedPreferences.getString("LAST_DATE", "")
        if (lastDateString!!.isNotEmpty()) {    // преобразование LocalDate! в String
            lastDate = LocalDate.parse(lastDateString)
            dataModel.lastDate.value = lastDate
        } else {
            LocalDate.now()
        }

        //howMany
        val sHowMany = sharedPreferences.getFloat("HOW_MANY", 0f)
        dataModel.money.value = sHowMany
        howMany = sHowMany

        binding.history.text = "$lastDate $today"

        // новый день
        if (today != lastDate) {
            // нужно дописать supportFragmentManager
            val days: Long = (ChronoUnit.DAYS.between(lastDate, today))
            avarageDailyValue = (howMany / (numberOfDays-1))
            keyTodayLimit += (avarageDailyValue * days).toInt()
            binding.result.text = todayLimit.toString()
            binding.history.text = "$keyTodayLimit"
            lastDate = today
            numberOfDays = ChronoUnit.DAYS.between(today, dateFull)
            binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
        }
    }
}

