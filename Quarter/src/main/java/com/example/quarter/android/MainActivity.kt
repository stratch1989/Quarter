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
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import java.util.Calendar
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
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
    private var isBudgetInputMode = false
    private var budgetInputValue = ""
    private var settingsPopup: PopupWindow? = null
    private var popupBudgetText: TextView? = null
    private var isEmojiPickerOpen = false
    private val selectedEmojis = mutableListOf<String>()

    private val categoryEmojis = mutableListOf(
        "🍔", "🍕", "🍞", "☕", "🥛", "🍎", "🥩", "🍣",
        "🚕", "🚌", "⛽", "🚇", "✈️", "🚗", "🅿️", "🛴",
        "🏠", "💡", "🚿", "📱", "📶", "🔧", "🧹", "🗑️",
        "👕", "👟", "👗", "🧥", "👜", "💍", "🕶️", "💈",
        "💊", "🏥", "🦷", "🧴", "💪", "🧘", "🏋️", "🩺",
        "🎬", "🎮", "📚", "🎵", "🎭", "🏖️", "⚽", "🎂",
        "🎁", "🐕", "👶", "🏫", "💻", "🛒", "🔑", "📦"
    )


    @SuppressLint("ClickableViewAccessibility")
    private fun setupBounceAnimation(view: View, maxScale: Float = 1.20f, shrinkAmount: Float = 0.11f) {
        var pressTime = 0L
        view.setOnTouchListener { v, event ->
            // Попап открыт и не режим бюджета — блокируем анимацию
            if (settingsPopup != null && !isBudgetInputMode) return@setOnTouchListener false
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
        val value: TextView = findViewById(R.id.value)

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
            // Если попап уже открыт — закрываем
            settingsPopup?.let { it.dismiss(); return@setOnClickListener }

            val popupView = LayoutInflater.from(this).inflate(R.layout.popup_settings_menu, null)
            val dpToPx = resources.displayMetrics.density
            val popupWidthPx = (275 * dpToPx).toInt()
            val popupHeightPx = (204 * dpToPx).toInt()
            val popup = PopupWindow(
                popupView,
                popupWidthPx,
                popupHeightPx,
                false // не фокусируемый — numpad работает под попапом
            )
            popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popup.elevation = 16f
            settingsPopup = popup

            // Затемнение фона
            binding.dimOverlay.visibility = View.VISIBLE
            binding.dimOverlayNumpad.visibility = View.VISIBLE
            binding.dimOverlay.animate().alpha(0.7f).setDuration(200).start()
            binding.dimOverlayNumpad.animate().alpha(0.7f).setDuration(200).start()

            popup.setOnDismissListener {
                settingsPopup = null
                popupBudgetText = null
                isBudgetInputMode = false
                budgetInputValue = ""
                // Убираем затемнение
                binding.dimOverlay.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.dimOverlay.visibility = View.GONE
                }.start()
                binding.dimOverlayNumpad.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.dimOverlayNumpad.visibility = View.GONE
                }.start()
            }

            // Бюджет
            popupBudgetText = popupView.findViewById(R.id.budget_text)
            if (howMany != 0.0) {
                popupBudgetText?.text = "💰  ${dataModel.roundMoney(howMany)}"
            }

            popupView.findViewById<LinearLayout>(R.id.menu_budget).setOnClickListener {
                isBudgetInputMode = true
                budgetInputValue = ""
                popupBudgetText?.text = "💰  _"
                popupBudgetText?.setTextColor(Color.parseColor("#FF9800"))
                // Плавно убираем затемнение с клавиатуры
                binding.dimOverlayNumpad.animate().alpha(0f).setDuration(300).withEndAction {
                    binding.dimOverlayNumpad.visibility = View.GONE
                }.start()
            }

            // Дата
            val dateText = popupView.findViewById<TextView>(R.id.date_text)
            val formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale("ru"))
            if (numberOfDays > 0) {
                dateText.text = "📅  По ${dateFull.format(formatter)}"
            }

            popupView.findViewById<LinearLayout>(R.id.menu_date).setOnClickListener {
                isBudgetInputMode = false
                budgetInputValue = ""
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
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_YEAR, 1)
                picker.datePicker.minDate = tomorrow.timeInMillis
                picker.show()
            }

            // О проекте
            popupView.findViewById<LinearLayout>(R.id.menu_about).setOnClickListener {
                isBudgetInputMode = false
                budgetInputValue = ""
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
            if (settingsPopup != null) return@setOnClickListener
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, History.newInstance())
                .addToBackStack(null)
                .commit()
        }

        // Кнопка добавления категории — открывает эмодзи-пикер
        val categoryAddBtn: TextView = findViewById(R.id.category_add_btn)
        val categoryContainer: LinearLayout = findViewById(R.id.category_container)
        val emojiPickerOverlay: View = findViewById(R.id.emoji_picker_overlay)
        val emojiGrid: GridLayout = findViewById(R.id.emoji_grid)

        val longPressHandler = android.os.Handler(mainLooper)
        val hiddenEmojiInput: EditText = findViewById(R.id.hidden_emoji_input)

        var rebuildAll: () -> Unit = {}

        // Обработка ввода эмодзи с клавиатуры
        hiddenEmojiInput.addTextChangedListener(object : android.text.TextWatcher {
            private var handling = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (handling) return
                val text = s?.toString()?.trim() ?: return
                if (text.isEmpty()) return
                handling = true
                hiddenEmojiInput.setText("")
                hiddenEmojiInput.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(hiddenEmojiInput.windowToken, 0)
                categoryEmojis.remove(text)
                categoryEmojis.add(0, text)
                selectedEmojis.remove(text)
                selectedEmojis.add(text)
                rebuildAll()
                handling = false
            }
        })

        rebuildAll = {
            // Перестроить строку выбранных категорий
            categoryContainer.removeAllViews()
            val dp = resources.displayMetrics.density
            val btnSize = (32 * dp).toInt()
            val gap = (4 * dp).toInt()
            for (emoji in selectedEmojis) {
                val tv = TextView(this).apply {
                    text = emoji
                    textSize = 16f
                    layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                        marginEnd = gap
                    }
                    gravity = android.view.Gravity.CENTER
                    background = resources.getDrawable(R.drawable.category_add_button_bg, theme)
                    setOnClickListener {
                        selectedEmojis.remove(emoji)
                        rebuildAll()
                    }
                }
                categoryContainer.addView(tv)
            }
            val addBtn = TextView(this).apply {
                id = R.id.category_add_btn
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
                gravity = android.view.Gravity.CENTER
                text = "+"
                setTextColor(Color.parseColor("#555555"))
                textSize = 16f
                background = resources.getDrawable(R.drawable.category_add_button_bg, theme)
                setOnClickListener { toggleEmojiPicker() }
            }
            categoryContainer.addView(addBtn)
            findViewById<HorizontalScrollView>(R.id.category_scroll).post {
                findViewById<HorizontalScrollView>(R.id.category_scroll).fullScroll(View.FOCUS_RIGHT)
            }

            // Перестроить сетку эмодзи
            emojiGrid.removeAllViews()
            // Кнопка клавиатуры — первый элемент
            val kbSize = (48 * dp).toInt()
            val kbPad = (10 * dp).toInt()
            val kbBtn = ImageButton(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = kbSize
                    height = kbSize
                }
                setImageResource(R.drawable.ic_keyboard)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(kbPad, kbPad, kbPad, kbPad)
                background = resources.getDrawable(R.drawable.keyboard_button_bg, theme)
                setOnClickListener {
                    hiddenEmojiInput.setText("")
                    hiddenEmojiInput.requestFocus()
                    hiddenEmojiInput.postDelayed({
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(hiddenEmojiInput, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                    }, 100)
                }
            }
            emojiGrid.addView(kbBtn)

            for (emoji in categoryEmojis) {
                if (selectedEmojis.contains(emoji)) continue
                @SuppressLint("ClickableViewAccessibility")
                val tv = TextView(this).apply {
                    text = emoji
                    textSize = 26f
                    val size = (48 * dp).toInt()
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = size
                        height = size
                    }
                    gravity = android.view.Gravity.CENTER
                    var longPressed = false
                    val deleteRunnable = Runnable {
                        longPressed = true
                        categoryEmojis.remove(emoji)
                        selectedEmojis.remove(emoji)
                        rebuildAll()
                    }
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                longPressed = false
                                longPressHandler.postDelayed(deleteRunnable, 2000)
                            }
                            MotionEvent.ACTION_UP -> {
                                longPressHandler.removeCallbacks(deleteRunnable)
                                if (!longPressed) {
                                    selectedEmojis.add(emoji)
                                    rebuildAll()
                                }
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                longPressHandler.removeCallbacks(deleteRunnable)
                            }
                        }
                        true
                    }
                }
                emojiGrid.addView(tv)
            }
        }

        categoryAddBtn.setOnClickListener { toggleEmojiPicker() }
        rebuildAll()

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
            // Попап открыт, но не режим бюджета — закрываем попап
            if (settingsPopup != null && !isBudgetInputMode) {
                settingsPopup?.dismiss()
                return
            }
            if (isBudgetInputMode) {
                if ((variable == "." && budgetInputValue.contains("."))
                    || (variable == "0" && budgetInputValue.isEmpty())
                    || budgetInputValue.replace(".", "").length >= 8) return
                budgetInputValue += variable
                popupBudgetText?.text = "💰  $budgetInputValue"
                return
            }
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
            if (settingsPopup != null && !isBudgetInputMode) {
                settingsPopup?.dismiss()
                return@setOnClickListener
            }
            if (isBudgetInputMode) {
                if (budgetInputValue.isNotEmpty()) {
                    budgetInputValue = budgetInputValue.dropLast(1)
                    popupBudgetText?.text = if (budgetInputValue.isEmpty()) "💰  _" else "💰  $budgetInputValue"
                }
                return@setOnClickListener
            }
            if (fictionalValue.isNotEmpty()) {
                fictionalValue = fictionalValue.substring(0, fictionalValue.length - 1)
                value.text = fictionalValue
                updatePreview()
            }
        }
        butDelete.setOnLongClickListener {
            if (isBudgetInputMode) {
                budgetInputValue = ""
                popupBudgetText?.text = "💰  _"
                return@setOnLongClickListener true
            }
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
            if (settingsPopup != null && !isBudgetInputMode) {
                settingsPopup?.dismiss()
                return@setOnClickListener
            }
            if (isBudgetInputMode) {
                saveBudgetInput()
                settingsPopup?.dismiss()
                return@setOnClickListener
            }
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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Закрытие эмодзи-пикера при тапе вне пикера и category_field
        if (event.action == MotionEvent.ACTION_DOWN && isEmojiPickerOpen && settingsPopup == null) {
            val touchX = event.rawX.toInt()
            val touchY = event.rawY.toInt()
            val pickerLoc = IntArray(2)
            binding.emojiPickerOverlay.getLocationOnScreen(pickerLoc)
            val pickerRect = Rect(pickerLoc[0], pickerLoc[1], pickerLoc[0] + binding.emojiPickerOverlay.width, pickerLoc[1] + binding.emojiPickerOverlay.height)
            val catLoc = IntArray(2)
            binding.categoryField.getLocationOnScreen(catLoc)
            val catRect = Rect(catLoc[0], catLoc[1], catLoc[0] + binding.categoryField.width, catLoc[1] + binding.categoryField.height)
            if (!pickerRect.contains(touchX, touchY) && !catRect.contains(touchX, touchY)) {
                toggleEmojiPicker()
            }
        }
        if (event.action == MotionEvent.ACTION_DOWN && settingsPopup != null) {
            val popup = settingsPopup ?: return super.dispatchTouchEvent(event)
            val popupView = popup.contentView
            val touchX = event.rawX.toInt()
            val touchY = event.rawY.toInt()

            // Проверяем попал ли тап в попап
            val popupLoc = IntArray(2)
            popupView.getLocationOnScreen(popupLoc)
            val popupRect = Rect(popupLoc[0], popupLoc[1], popupLoc[0] + popupView.width, popupLoc[1] + popupView.height)

            // Проверяем попал ли тап в numpad
            val numpadLoc = IntArray(2)
            binding.linearLayout.getLocationOnScreen(numpadLoc)
            val numpadRect = Rect(numpadLoc[0], numpadLoc[1], numpadLoc[0] + binding.linearLayout.width, numpadLoc[1] + binding.linearLayout.height)

            // Проверяем попал ли тап в кнопку настроек
            val settingsLoc = IntArray(2)
            binding.settings.getLocationOnScreen(settingsLoc)
            val settingsRect = Rect(settingsLoc[0], settingsLoc[1], settingsLoc[0] + binding.settings.width, settingsLoc[1] + binding.settings.height)

            // Проверяем попал ли тап в кнопку истории
            val historyLoc = IntArray(2)
            binding.history.getLocationOnScreen(historyLoc)
            val historyRect = Rect(historyLoc[0], historyLoc[1], historyLoc[0] + binding.history.width, historyLoc[1] + binding.history.height)

            // Тап на историю — просто закрываем попап, не открываем историю
            if (historyRect.contains(touchX, touchY)) {
                isBudgetInputMode = false
                budgetInputValue = ""
                popup.dismiss()
                return true // поглощаем тап
            }

            if (!popupRect.contains(touchX, touchY) && !numpadRect.contains(touchX, touchY) && !settingsRect.contains(touchX, touchY)) {
                isBudgetInputMode = false
                budgetInputValue = ""
                popup.dismiss()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun toggleEmojiPicker() {
        val overlay = binding.emojiPickerOverlay
        if (isEmojiPickerOpen) {
            overlay.animate().alpha(0f).setDuration(200).withEndAction {
                overlay.visibility = View.GONE
            }.start()
        } else {
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            overlay.animate().alpha(1f).setDuration(200).start()
        }
        isEmojiPickerOpen = !isEmojiPickerOpen
    }

    private fun saveBudgetInput() {
        if (!isBudgetInputMode) return
        val newBudget = budgetInputValue.toDoubleOrNull()
        if (newBudget != null && newBudget > 0) {
            dataModel.money.value = newBudget
        }
        isBudgetInputMode = false
        budgetInputValue = ""
        popupBudgetText = null
    }

    override fun onPause() {
        super.onPause()
        isBudgetInputMode = false
        budgetInputValue = ""
        settingsPopup?.dismiss()
        if (isEmojiPickerOpen) {
            binding.emojiPickerOverlay.visibility = View.GONE
            isEmojiPickerOpen = false
        }
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
