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
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.quarter.android.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.content.Context
import java.util.Locale

class MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private val dataModel: DataModel by viewModels()
    private var fictionalValue = ""

    var todayLimit = 0.0
    var avarageDailyValue = 0.0
    var howMany = 0.0
    var numberOfDays = 0L
    var dateFull = LocalDate.now().minusDays(1)
    var keyTodayLimit = 0.0
    var today = LocalDate.now()
    var lastDate = LocalDate.now()
    private var hasUnsavedChanges = false


    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadData()

        val result: TextView = findViewById(R.id.result)

        fun updateDayLimitText() {
            val hasBudget = howMany != 0.0
            val hasDate = numberOfDays > 0
            binding.dayLimit.text = when {
                hasBudget && hasDate -> "${howMany} на ${numberOfDays} дней"
                hasBudget && !hasDate -> "Укажите дату в настройках"
                !hasBudget && hasDate -> "Укажите бюджет в настройках"
                else -> "Введите дату и сумму в настройках"
            }
        }

        dataModel.money.observe(this) {
            howMany = dataModel.roundMoney(it)
            avarageDailyValue = dataModel.calculateDailyAverage(howMany, numberOfDays)
            updateDayLimitText()
            hasUnsavedChanges = true
        }

        dataModel.dateFull.observe(this) {
            dateFull = it
            numberOfDays = ChronoUnit.DAYS.between(today, dateFull)
            dataModel.numberOfDays.value = numberOfDays
            avarageDailyValue = dataModel.calculateDailyAverage(howMany, numberOfDays)
            updateDayLimitText()

            if (howMany != 0.0) {
                todayLimit = 0.0
                todayLimit += avarageDailyValue.toInt()
                if (keyTodayLimit != 0.0) {
                    todayLimit = keyTodayLimit
                    keyTodayLimit = 0.0
                }
                binding.result.text = todayLimit.toString()
                lastDate = today
            }
            hasUnsavedChanges = true
        }

        dataModel.keyTodayLimit.observe(this) {
            keyTodayLimit = it
            hasUnsavedChanges = true
        }

        dataModel.todayLimit.observe(this) {
            todayLimit = it
            binding.result.text = todayLimit.toString()
            hasUnsavedChanges = true
        }

        // Фрагмент Settings
        binding.settings.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, SettingsFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Фрагмент History
        binding.history.setOnClickListener {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, History.newInstance())
                .addToBackStack(null)
                .commit()
        }

        val value: TextView = findViewById(R.id.value)
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
        val historyManager = HistoryManager(this)
        buttonEnter.setOnClickListener {
            if ((fictionalValue.isNotEmpty()) && (value.text != ".")) {
                val fictionalDigit = fictionalValue.toDouble()
                val spendResult = dataModel.spend(fictionalDigit, todayLimit, howMany)
                todayLimit = spendResult.newTodayLimit
                howMany = spendResult.newBudget
                result.text = "$todayLimit"
                historyManager.addEntry(fictionalDigit)
                hasUnsavedChanges = true
                fictionalValue = ""
                value.text = ""
            }
        }

        dataModel.saveClick.observe(this) {
            todayLimit = 0.0
            todayLimit += (avarageDailyValue).toInt()
            result.text = todayLimit.toString()
            hasUnsavedChanges = true
        }

    }

    override fun onPause() {
        super.onPause()
        saveData()
    }

    private fun saveData() {
        if (!hasUnsavedChanges) return
        val result: String = binding.result.text.toString()
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("STRING_KEY", result)
        editor.putString("HOW_MANY", howMany.toString())
        editor.putLong("NUMBER_OF_DAYS", numberOfDays)
        editor.putString("AVARAGE_DAILY_VALUE", avarageDailyValue.toString())
        editor.putString("DATE_FULL", dateFull.toString())
        editor.putString("LAST_DATE", lastDate.toString())
        editor.apply()
        hasUnsavedChanges = false
    }

    // Читает значение как String, а если в SharedPreferences лежит старый Float — ловит ClassCastException
    private fun getStringOrFloat(prefs: android.content.SharedPreferences, key: String, default: String): String {
        return try {
            prefs.getString(key, default) ?: default
        } catch (e: ClassCastException) {
            prefs.getFloat(key, default.toFloat()).toString()
        }
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val result = getStringOrFloat(sharedPreferences, "STRING_KEY", "0")
        keyTodayLimit = result.toDouble()
        dataModel.keyTodayLimit.value = keyTodayLimit
        numberOfDays = sharedPreferences.getLong("NUMBER_OF_DAYS", 0L)
        avarageDailyValue = getStringOrFloat(sharedPreferences, "AVARAGE_DAILY_VALUE", "0.0").toDouble()

        //dateFull
        val dateFullString = getStringOrFloat(sharedPreferences, "DATE_FULL", "")
        if (dateFullString.isNotEmpty()) {
            dateFull = LocalDate.parse(dateFullString)
            dataModel.dateFull.value = dateFull
            val formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale("ru"))
            dataModel.dayText.value = dateFull.format(formatter)
        }

        //lastDate
        val lastDateString = getStringOrFloat(sharedPreferences, "LAST_DATE", "")
        if (lastDateString.isNotEmpty()) {
            lastDate = LocalDate.parse(lastDateString)
            dataModel.lastDate.value = lastDate
        }

        //howMany
        val sHowMany = getStringOrFloat(sharedPreferences, "HOW_MANY", "0.0").toDouble()
        dataModel.money.value = sHowMany
        howMany = dataModel.roundMoney(sHowMany)

        // новый день
        if (today != lastDate && numberOfDays > 1) {
            val days: Long = ChronoUnit.DAYS.between(lastDate, today)
            dataModel.calculateNewDayOptions(howMany, keyTodayLimit, numberOfDays, days)

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, EveryDayQuestion.newInstance())
                .addToBackStack(null)
                .commit()
        }
    }

    fun onFragmentClosed() {
        avarageDailyValue = dataModel.avarageDailyValue.value ?: avarageDailyValue
        keyTodayLimit = dataModel.keyTodayLimit.value ?: keyTodayLimit

        todayLimit = dataModel.roundMoney(keyTodayLimit)
        dataModel.todayLimit.value = todayLimit
        binding.result.text = todayLimit.toString()
        lastDate = today
        numberOfDays = ChronoUnit.DAYS.between(today, dateFull)
        binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
        hasUnsavedChanges = true
    }
}
