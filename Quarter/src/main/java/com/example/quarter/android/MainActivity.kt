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
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.quarter.android.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.app.DatePickerDialog
import android.graphics.drawable.ColorDrawable
import java.util.Calendar
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
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
    private var lastSpendAmount: Double? = null
    private var isAddMode = false


    @SuppressLint("ClickableViewAccessibility")
    private fun setupBounceAnimation(view: View, maxScale: Float = 1.20f, shrinkAmount: Float = 0.11f) {
        var pressTime = 0L
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressTime = System.currentTimeMillis()
                    v.animate().cancel()
                    v.background?.colorFilter = PorterDuffColorFilter(
                        Color.argb(60, 255, 255, 255), PorterDuff.Mode.SRC_ATOP
                    )
                    v.animate()
                        .scaleX(maxScale).scaleY(maxScale)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    v.background?.clearColorFilter()
                    val held = System.currentTimeMillis() - pressTime
                    val factor = (held.coerceIn(0, 300) / 300f)
                    val currentScale = v.scaleX
                    val shrinkTo = 1.0f - (shrinkAmount * factor)
                    val shrinkX = ObjectAnimator.ofFloat(v, "scaleX", currentScale, shrinkTo)
                    val shrinkY = ObjectAnimator.ofFloat(v, "scaleY", currentScale, shrinkTo)
                    val shrink = AnimatorSet().apply {
                        playTogether(shrinkX, shrinkY)
                        duration = (200 * factor).toLong().coerceAtLeast(80)
                        interpolator = DecelerateInterpolator()
                    }
                    val restoreX = ObjectAnimator.ofFloat(v, "scaleX", shrinkTo, 1.0f)
                    val restoreY = ObjectAnimator.ofFloat(v, "scaleY", shrinkTo, 1.0f)
                    val restore = AnimatorSet().apply {
                        playTogether(restoreX, restoreY)
                        duration = (150 * factor).toLong().coerceAtLeast(60)
                        interpolator = DecelerateInterpolator()
                    }
                    AnimatorSet().apply {
                        playSequentially(shrink, restore)
                        start()
                    }
                }
            }
            false
        }
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadData()

        val result: TextView = findViewById(R.id.result)
        val lastOperation: TextView = findViewById(R.id.last_operation)

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
            if (numberOfDays > 0 && howMany != 0.0) {
                todayLimit = avarageDailyValue.toInt().toDouble()
                dataModel.todayLimit.value = todayLimit
                binding.result.text = todayLimit.toString()
            }
            lastSpendAmount = null
            binding.lastOperation.text = ""
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
            lastSpendAmount = null
            binding.lastOperation.text = ""
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

        // Попап-меню в стиле Apple
        binding.settings.setOnClickListener { anchor ->
            val popupView = LayoutInflater.from(this).inflate(R.layout.popup_settings_menu, null)
            val dpToPx = resources.displayMetrics.density
            val popupWidthPx = (275 * dpToPx).toInt()
            val popupHeightPx = (204 * dpToPx).toInt()
            val popup = PopupWindow(
                popupView,
                popupWidthPx,
                popupHeightPx,
                true
            )
            popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popup.elevation = 16f

            val budgetInput = popupView.findViewById<EditText>(R.id.budget_input)
            if (howMany != 0.0) {
                budgetInput.setText(dataModel.roundMoney(howMany).toString())
            }

            budgetInput.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val newBudget = v.text.toString().toDoubleOrNull()
                    if (newBudget != null && newBudget > 0) {
                        dataModel.money.value = newBudget
                    }
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                    popup.dismiss()
                    true
                } else false
            }

            popupView.findViewById<LinearLayout>(R.id.menu_budget).setOnClickListener {
                budgetInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(budgetInput, InputMethodManager.SHOW_IMPLICIT)
            }
            // Показываем текущую дату если она установлена
            val dateText = popupView.findViewById<TextView>(R.id.date_text)
            val formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale("ru"))
            if (numberOfDays > 0) {
                dateText.text = "📅  По ${dateFull.format(formatter)}"
            }

            popupView.findViewById<LinearLayout>(R.id.menu_date).setOnClickListener {
                popup.dismiss()
                val now = LocalDate.now()
                val initDate = if (numberOfDays > 0) dateFull else now.plusDays(7)
                val picker = DatePickerDialog(
                    this,
                    R.style.DarkDatePickerTheme,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                        val days = ChronoUnit.DAYS.between(now, selectedDate)
                        if (days > 0) {
                            if (howMany != 0.0) {
                                dataModel.keyTodayLimit.value = Math.round((howMany / days) * 100.0) / 100.0
                            }
                            dataModel.dateFull.value = selectedDate
                            dataModel.dayText.value = selectedDate.format(formatter)
                            dataModel.dayNumber.value = days.toInt()
                            dataModel.lastDate.value = now
                            HistoryManager(this).updatePeriodStart()
                        }
                    },
                    initDate.year,
                    initDate.monthValue - 1,
                    initDate.dayOfMonth
                )
                // Минимум — завтра
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                picker.datePicker.minDate = tomorrow.timeInMillis
                picker.show()
            }
            popupView.findViewById<LinearLayout>(R.id.menu_about).setOnClickListener {
                popup.dismiss()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.place_holder, AboutFragment.newInstance())
                    .addToBackStack(null)
                    .commit()
            }

            // Выравниваем правый край попапа с правым краем кнопки
            val xOff = anchor.width - popupWidthPx
            popup.showAsDropDown(anchor, xOff, 8)
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
        fun buttonMetrics(button: View): Unit {
            val layoutParams = button.layoutParams
            layoutParams.width = displayMetrics.toInt()
            layoutParams.height = displayMetrics.toInt()
            button.layoutParams = layoutParams
        }

        // Превью при вводе цифр
        fun updatePreview() {
            if (fictionalValue.isNotEmpty() && fictionalValue != ".") {
                val inputAmount = fictionalValue.toDoubleOrNull() ?: 0.0
                if (isAddMode) {
                    val preview = dataModel.roundMoney(todayLimit + inputAmount)
                    val newBudget = dataModel.roundMoney(howMany + inputAmount)
                    result.text = preview.toString()
                    result.setTextColor(Color.parseColor("#4CAF50"))
                    binding.textView2.text = "Пополнение"
                    binding.textView2.setTextColor(Color.parseColor("#4CAF50"))
                    binding.dayLimit.text = "${newBudget} на ${numberOfDays} дней"
                    binding.dayLimit.setTextColor(Color.parseColor("#888888"))
                } else {
                    val preview = dataModel.roundMoney(todayLimit - inputAmount)
                    if (preview < 0) {
                        val remainingBudget = howMany - inputAmount
                        val days = if (numberOfDays > 1) numberOfDays else 1L
                        val newDaily = dataModel.roundMoney(remainingBudget / days)
                        result.text = newDaily.toString()
                        result.setTextColor(Color.parseColor("#FF4444"))
                        binding.textView2.text = "Новый бюджет"
                        binding.textView2.setTextColor(Color.parseColor("#FF4444"))
                        binding.dayLimit.text = "${dataModel.roundMoney(remainingBudget)} на ${days} дней"
                        if (remainingBudget < 0) binding.dayLimit.setTextColor(Color.parseColor("#FF4444"))
                        else binding.dayLimit.setTextColor(Color.parseColor("#888888"))
                    } else {
                        val remainingBudget = dataModel.roundMoney(howMany - inputAmount)
                        result.text = preview.toString()
                        result.setTextColor(Color.WHITE)
                        binding.textView2.text = "На сегодня"
                        binding.textView2.setTextColor(Color.WHITE)
                        binding.dayLimit.text = "${remainingBudget} на ${numberOfDays} дней"
                        binding.dayLimit.setTextColor(Color.parseColor("#888888"))
                    }
                }
            } else {
                result.text = todayLimit.toString()
                result.setTextColor(Color.WHITE)
                binding.textView2.text = if (isAddMode) "Пополнение" else "На сегодня"
                binding.textView2.setTextColor(if (isAddMode) Color.parseColor("#4CAF50") else Color.WHITE)
                updateDayLimitText()
                binding.dayLimit.setTextColor(Color.parseColor("#888888"))
            }
        }

        // Обработка нажатия на цифры
        fun buttonBinding(variable: String): Unit {
            if ((variable == "." && fictionalValue.contains("."))
                || (variable == "0" && fictionalValue.isEmpty())
                || fictionalValue.replace(".", "").length >= 8) {}
            else {
                fictionalValue += variable
                value.text = fictionalValue
                updatePreview()
            }
        }

        // Обьявили все кнопки
        val buttonEnter: Button = findViewById(R.id.button_enter)
        val butDelete: ImageButton = findViewById(R.id.button_del)
        val butUndo: ImageButton = findViewById(R.id.button_undo)
        val buttons = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9, R.id.button_point
        )

        // Изменили размер
        buttons.forEach { buttonId ->
            val button = findViewById<Button>(buttonId)
            buttonMetrics(button)
            setupBounceAnimation(button)
            val buttonText = button.text.toString()
            button.setOnClickListener {buttonBinding(buttonText)}
        }
        buttonMetrics(buttonEnter)
        buttonMetrics(butDelete)
        buttonMetrics(butUndo)

        // Анимация для оранжевых и +/- кнопок
        val butPlusMinus: Button = findViewById(R.id.button_plus_minus)
        setupBounceAnimation(buttonEnter, maxScale = 1.12f, shrinkAmount = 0.07f)
        setupBounceAnimation(butDelete)
        setupBounceAnimation(butUndo)
        setupBounceAnimation(butPlusMinus)

        // Обработка кнопки удалить
        butDelete.setOnClickListener {
            if (fictionalValue.isNotEmpty()) {
                fictionalValue = fictionalValue.substring(0, fictionalValue.length - 1)
                value.text = fictionalValue
                updatePreview()
            }
        }
        butDelete.setOnLongClickListener {
            if (fictionalValue.isNotEmpty()) {
                fictionalValue = ""
                value.text = fictionalValue
                updatePreview()
            }
            true
        }

        // Обработка кнопки enter
        val historyManager = HistoryManager(this)
        buttonEnter.setOnClickListener {
            if ((fictionalValue.isNotEmpty()) && (value.text != ".")) {
                val fictionalDigit = fictionalValue.toDouble()
                if (isAddMode) {
                    todayLimit = dataModel.roundMoney(todayLimit + fictionalDigit)
                    howMany = dataModel.roundMoney(howMany + fictionalDigit)
                    dataModel.money.value = howMany
                    dataModel.todayLimit.value = todayLimit
                    result.text = "$todayLimit"
                    lastSpendAmount = null
                    lastOperation.text = "+ ${fictionalDigit} ₽"
                } else {
                    val spendResult = dataModel.spend(fictionalDigit, todayLimit, howMany)
                    todayLimit = spendResult.newTodayLimit
                    howMany = spendResult.newBudget
                    result.text = "$todayLimit"
                    historyManager.addEntry(fictionalDigit)
                    lastSpendAmount = fictionalDigit
                    lastOperation.text = "- ${fictionalDigit} ₽"
                }
                hasUnsavedChanges = true
                fictionalValue = ""
                value.text = ""
                result.setTextColor(Color.WHITE)
                binding.textView2.text = if (isAddMode) "Пополнение" else "На сегодня"
                binding.textView2.setTextColor(if (isAddMode) Color.parseColor("#4CAF50") else Color.WHITE)
                updateDayLimitText()
                binding.dayLimit.setTextColor(Color.parseColor("#888888"))
            }
        }

        // Обработка кнопки отмены последней траты
        butUndo.setOnClickListener {
            val amount = lastSpendAmount ?: return@setOnClickListener
            todayLimit = dataModel.roundMoney(todayLimit + amount)
            howMany = dataModel.roundMoney(howMany + amount)
            dataModel.money.value = howMany
            dataModel.todayLimit.value = todayLimit
            result.text = "$todayLimit"
            // Удаляем последнюю запись из истории (первая в списке = последняя добавленная)
            historyManager.removeCurrentEntry(0)
            lastSpendAmount = null
            lastOperation.text = ""
            hasUnsavedChanges = true
        }

        // Обработка кнопки +/-
        buttonMetrics(butPlusMinus)
        butPlusMinus.setOnClickListener {
            isAddMode = !isAddMode
            binding.textView2.text = if (isAddMode) "Пополнение" else "На сегодня"
            binding.textView2.setTextColor(if (isAddMode) Color.parseColor("#4CAF50") else Color.WHITE)
            lastOperation.text = ""
            updatePreview()
        }

        dataModel.clearUndo.observe(this) {
            lastSpendAmount = null
            lastOperation.text = ""
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
