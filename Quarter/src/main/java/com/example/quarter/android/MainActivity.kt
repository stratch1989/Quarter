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
import android.widget.FrameLayout
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
    private var lastIncomeAmount: Double? = null
    private var isAddMode = false
    private var isBudgetInputMode = false
    private var budgetInputValue = ""
    private var settingsPopup: PopupWindow? = null
    private var popupBudgetText: TextView? = null
    private var dayStreak = 1
    private var isEmojiPickerOpen = false
    private val selectedEmojis = mutableListOf<String>()
    private var activeCategory: String? = null

    private val categoryEmojis = mutableListOf(
        "🍔", "🍕", "🍞", "☕", "🥛", "🍎", "🥩", "🍣",
        "🚕", "🚌", "⛽", "🚇", "✈️", "🚗", "🅿️", "🛴",
        "🏠", "💡", "🚿", "📱", "📶", "🔧", "🧹", "🗑️",
        "👕", "👟", "👗", "🧥", "👜", "💍", "🕶️", "💈",
        "💊", "🏥", "🦷", "🧴", "💪", "🧘", "🏋️", "🩺",
        "🎬", "🎮", "📚", "🎵", "🎭", "🏖️", "⚽", "🎂",
        "🎁", "🐕", "👶", "🏫", "💻", "🛒", "🔑", "📦"
    )

    private val incomeCategoryEmojis = mutableListOf(
        "💰", "💳", "🏦", "💵", "💎", "📈", "🤑", "💸",
        "💼", "🏢", "👔", "🖥️", "⌨️", "📊", "🏭", "🛠️",
        "🎓", "📖", "✏️", "🧑‍🏫", "🏅", "📜", "🎯", "🧠",
        "🏡", "🔑", "📦", "🚗", "🛍️", "💍", "🎁", "🖼️",
        "🤝", "👨‍👩‍👧", "👴", "💌", "🙏", "❤️", "🫶", "🎉",
        "📱", "💻", "🎮", "🎵", "📸", "🎬", "✍️", "🛒"
    )

    private val selectedIncomeEmojis = mutableListOf<String>()


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

        fun updateTextView2(text: String, color: Int) {
            val cs = binding.textView2.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            val resultAmount = result.text.toString().toDoubleOrNull() ?: 0.0
            if (text == "/день" && resultAmount <= 999999 && resultAmount >= -999999) {
                // справа от суммы на baseline
                cs.startToEnd = R.id.result
                cs.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                cs.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                cs.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                cs.baselineToBaseline = R.id.result
                binding.textView2.text = "/день"
            } else {
                // снизу от суммы
                cs.startToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                cs.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                cs.topToBottom = R.id.result
                cs.baselineToBaseline = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                cs.topMargin = 0
                binding.textView2.text = text
            }
            binding.textView2.setTextColor(color)
            binding.textView2.layoutParams = cs
        }

        val defaultDayLimitMarginTop = (16 * resources.displayMetrics.density).toInt()
        val statusDayLimitMarginTop = (-4 * resources.displayMetrics.density).toInt()

        fun resetDayLimitStyle() {
            binding.dayLimit.textSize = 14f
            binding.dayLimit.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            binding.dayLimit.setTextColor(Color.parseColor("#888888"))
            (binding.dayLimit.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let {
                it.topMargin = defaultDayLimitMarginTop
                binding.dayLimit.layoutParams = it
            }
        }

        fun setDayLimitStatusStyle() {
            binding.dayLimit.textSize = 24f
            binding.dayLimit.setTypeface(binding.dayLimit.typeface, android.graphics.Typeface.BOLD)
            (binding.dayLimit.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let {
                it.topMargin = statusDayLimitMarginTop
                binding.dayLimit.layoutParams = it
            }
        }

        fun updateDayLimitText() {
            resetDayLimitStyle()
            val hasBudget = howMany != 0.0
            val hasDate = numberOfDays > 0
            if (hasBudget && hasDate) {
                binding.dayLimit.visibility = android.view.View.GONE
            } else {
                binding.dayLimit.visibility = android.view.View.VISIBLE
                binding.dayLimit.text = when {
                    hasBudget && !hasDate -> "Укажите дату в настройках"
                    !hasBudget && hasDate -> "Укажите бюджет в настройках"
                    else -> "Введите дату и сумму в настройках"
                }
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
            lastSpendAmount = null
            lastIncomeAmount = null
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
                val dateStr = "📅  По ${dateFull.format(formatter)}"
                val daysStr = " ($numberOfDays дн.)"
                val spannable = android.text.SpannableString(dateStr + daysStr)
                spannable.setSpan(android.text.style.RelativeSizeSpan(0.7f), dateStr.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), dateStr.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#888888")), dateStr.length, spannable.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                dateText.text = spannable
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

            // Правый верхний угол попапа совпадает с правым верхним углом кнопки
            val xOff = anchor.width - popupWidthPx
            popup.showAsDropDown(anchor, xOff, -anchor.height)
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

        // Streak popup
        binding.streakButton.setOnClickListener {
            if (settingsPopup != null) return@setOnClickListener
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.place_holder, StreakFragment.newInstance(dayStreak))
                .addToBackStack(null)
                .commit()
        }

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
                val emojis = if (isAddMode) incomeCategoryEmojis else categoryEmojis
                val selected = if (isAddMode) selectedIncomeEmojis else selectedEmojis
                if (!emojis.contains(text)) {
                    emojis.add(0, text)
                }
                if (!selected.contains(text)) {
                    selected.add(0, text)
                }
                activeCategory = text
                rebuildAll()
                handling = false
            }
        })

        rebuildAll = {
            // Перестроить строку выбранных категорий
            categoryContainer.removeAllViews()
            val dp = resources.displayMetrics.density
            val btnSize = (38 * dp).toInt()
            val gap = (6 * dp).toInt()
            val indicator = findViewById<TextView>(R.id.active_category_indicator)
            val currentSelected = if (isAddMode) selectedIncomeEmojis else selectedEmojis
            val currentEmojis = if (isAddMode) incomeCategoryEmojis else categoryEmojis
            // Кнопка + первая
            val addBtn = TextView(this).apply {
                id = R.id.category_add_btn
                layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                    marginEnd = gap
                }
                gravity = android.view.Gravity.CENTER
                text = "+"
                setTextColor(Color.parseColor("#555555"))
                textSize = 16f
                background = resources.getDrawable(R.drawable.category_add_button_bg, theme)
                setOnClickListener { toggleEmojiPicker() }
            }
            categoryContainer.addView(addBtn)
            for (emoji in currentSelected) {
                val isActive = emoji == activeCategory
                val container = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                        marginEnd = gap
                    }
                    background = resources.getDrawable(
                        if (isActive) R.drawable.category_emoji_active_bg else R.drawable.category_emoji_bg,
                        theme
                    )
                    clipToOutline = true
                }
                // Эмодзи
                val tv = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    text = emoji
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                }
                container.addView(tv)
                // Затемнение + крестик в режиме пикера
                if (isEmojiPickerOpen) {
                    val dim = View(this).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(Color.argb(120, 0, 0, 0))
                    }
                    container.addView(dim)
                    val xSize = (12 * dp).toInt()
                    val xMark = TextView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(xSize, xSize).apply {
                            gravity = android.view.Gravity.TOP or android.view.Gravity.END
                        }
                        text = "×"
                        setTextColor(Color.WHITE)
                        textSize = 7f
                        gravity = android.view.Gravity.CENTER
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(Color.argb(200, 80, 80, 80))
                        }
                    }
                    container.addView(xMark)
                }
                container.setOnClickListener {
                    if (isEmojiPickerOpen) {
                        currentSelected.remove(emoji)
                        if (activeCategory == emoji) {
                            activeCategory = null
                            indicator.visibility = View.GONE
                        }
                    } else {
                        if (activeCategory == emoji) {
                            activeCategory = null
                            indicator.visibility = View.GONE
                        } else {
                            activeCategory = emoji
                            indicator.text = emoji
                            indicator.visibility = View.VISIBLE
                        }
                    }
                    rebuildAll()
                }
                // Drag-and-drop для перестановки
                if (!isEmojiPickerOpen) {
                    container.setOnLongClickListener { v ->
                        val dragData = android.content.ClipData.newPlainText("emoji", emoji)
                        val shadow = View.DragShadowBuilder(v)
                        v.startDragAndDrop(dragData, shadow, emoji, 0)
                        v.alpha = 0.3f
                        true
                    }
                }
                categoryContainer.addView(container)
            }
            // Обработка drop — перестановка эмодзи
            categoryContainer.setOnDragListener { v, event ->
                when (event.action) {
                    android.view.DragEvent.ACTION_DROP -> {
                        val draggedEmoji = event.localState as String
                        val dropX = event.x
                        val container = v as LinearLayout
                        val dragSelected = if (isAddMode) selectedIncomeEmojis else selectedEmojis
                        // child 0 = кнопка +, эмодзи начинаются с 1
                        var targetIndex = dragSelected.size - 1
                        for (i in 1 until container.childCount) {
                            val child = container.getChildAt(i)
                            val childCenter = child.left + child.width / 2
                            if (dropX < childCenter) {
                                targetIndex = i - 1 // -1 т.к. кнопка + на позиции 0
                                break
                            }
                        }
                        val fromIndex = dragSelected.indexOf(draggedEmoji)
                        if (fromIndex != -1 && targetIndex != fromIndex) {
                            dragSelected.removeAt(fromIndex)
                            if (targetIndex > fromIndex) targetIndex--
                            dragSelected.add(targetIndex.coerceIn(0, dragSelected.size), draggedEmoji)
                        }
                        rebuildAll()
                        true
                    }
                    android.view.DragEvent.ACTION_DRAG_ENDED -> {
                        // Восстановить прозрачность если drag отменён
                        rebuildAll()
                        true
                    }
                    else -> true
                }
            }
            // Обновить индикатор
            if (activeCategory != null) {
                indicator.text = activeCategory
                indicator.visibility = View.VISIBLE
            } else {
                indicator.visibility = View.GONE
            }
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

            for (emoji in currentEmojis) {
                if (currentSelected.contains(emoji)) continue
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
                        currentEmojis.remove(emoji)
                        currentSelected.remove(emoji)
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
                                    currentSelected.add(0, emoji)
                                    activeCategory = emoji
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

        rebuildAllRef = rebuildAll
        rebuildAll()

        // Долгое нажатие на меню категорий — открыть режим списка эмодзи
        val longPressOpenPicker = Runnable {
            if (!isEmojiPickerOpen) {
                toggleEmojiPicker()
            }
        }
        binding.categoryScroll.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressHandler.postDelayed(longPressOpenPicker, 1500)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressOpenPicker)
                }
                MotionEvent.ACTION_MOVE -> {
                    // Отмена при значительном сдвиге (скролл)
                    if (event.historySize > 0) {
                        val dx = Math.abs(event.x - event.getHistoricalX(0))
                        if (dx > 10) longPressHandler.removeCallbacks(longPressOpenPicker)
                    }
                }
            }
            false // не перехватываем — скролл работает как обычно
        }

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
                    result.text = preview.toString()
                    result.setTextColor(Color.parseColor("#4CAF50"))
                    updateTextView2("/день", Color.parseColor("#4CAF50"))
                    binding.dayLimit.visibility = View.VISIBLE
                    binding.dayLimit.text = "Пополнение"
                    binding.dayLimit.setTextColor(Color.parseColor("#4CAF50"))
                    setDayLimitStatusStyle()
                } else {
                    val preview = dataModel.roundMoney(todayLimit - inputAmount)
                    if (preview < 0) {
                        val remainingBudget = howMany - inputAmount
                        val days = if (numberOfDays > 1) numberOfDays else 1L
                        val newDaily = dataModel.roundMoney(remainingBudget / days)
                        result.text = newDaily.toString()
                        result.setTextColor(Color.parseColor("#FF4444"))
                        updateTextView2("/день", Color.parseColor("#FF4444"))
                        binding.dayLimit.visibility = View.VISIBLE
                        binding.dayLimit.text = "Новый бюджет"
                        binding.dayLimit.setTextColor(Color.parseColor("#FF4444"))
                        setDayLimitStatusStyle()
                    } else {
                        val remainingBudget = dataModel.roundMoney(howMany - inputAmount)
                        result.text = preview.toString()
                        result.setTextColor(Color.WHITE)
                        updateTextView2("/день", Color.parseColor("#888888"))
                        resetDayLimitStyle()
                        if (binding.dayLimit.visibility == View.VISIBLE) {
                            binding.dayLimit.text = "${remainingBudget} на ${numberOfDays} дней"
                        }
                    }
                }
            } else {
                result.text = todayLimit.toString()
                if (isAddMode) {
                    result.setTextColor(Color.parseColor("#4CAF50"))
                    updateTextView2("/день", Color.parseColor("#4CAF50"))
                    binding.dayLimit.visibility = View.VISIBLE
                    binding.dayLimit.text = "Пополнение"
                    binding.dayLimit.setTextColor(Color.parseColor("#4CAF50"))
                    setDayLimitStatusStyle()
                } else {
                    result.setTextColor(Color.WHITE)
                    updateTextView2("/день", Color.parseColor("#888888"))
                    updateDayLimitText()
                    binding.dayLimit.setTextColor(Color.parseColor("#888888"))
                }
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
                    historyManager.addIncomeEntry(fictionalDigit, activeCategory)
                    lastIncomeAmount = fictionalDigit
                    lastSpendAmount = null
                    val categoryText = if (activeCategory != null) " $activeCategory" else ""
                    lastOperation.text = "+ ${fictionalDigit} ₽$categoryText"
                } else {
                    val spendResult = dataModel.spend(fictionalDigit, todayLimit, howMany)
                    todayLimit = spendResult.newTodayLimit
                    howMany = spendResult.newBudget
                    result.text = "$todayLimit"
                    historyManager.addEntry(fictionalDigit, activeCategory)
                    lastSpendAmount = fictionalDigit
                    lastIncomeAmount = null
                    val categoryText = if (activeCategory != null) " $activeCategory" else ""
                    lastOperation.text = "- ${fictionalDigit} ₽$categoryText"
                }
                // Сбросить активную категорию
                if (activeCategory != null) {
                    activeCategory = null
                    binding.activeCategoryIndicator.visibility = View.GONE
                    // Перестроить меню чтобы убрать подсветку
                    categoryContainer.post {
                        for (i in 0 until categoryContainer.childCount) {
                            val child = categoryContainer.getChildAt(i)
                            if (child.id != R.id.category_add_btn) {
                                child.background = resources.getDrawable(R.drawable.category_emoji_bg, theme)
                            }
                        }
                    }
                }
                hasUnsavedChanges = true
                fictionalValue = ""
                value.text = ""
                result.setTextColor(Color.WHITE)
                updateTextView2("/день", if (isAddMode) Color.parseColor("#4CAF50") else Color.parseColor("#888888"))
                updateDayLimitText()
                binding.dayLimit.setTextColor(Color.parseColor("#888888"))
            }
        }

        // Обработка кнопки отмены последней операции
        butUndo.setOnClickListener {
            if (lastIncomeAmount != null) {
                val amount = lastIncomeAmount!!
                todayLimit = dataModel.roundMoney(todayLimit - amount)
                howMany = dataModel.roundMoney(howMany - amount)
                dataModel.money.value = howMany
                dataModel.todayLimit.value = todayLimit
                result.text = "$todayLimit"
                historyManager.removeCurrentEntry(0, income = true)
                lastIncomeAmount = null
                lastOperation.text = ""
                hasUnsavedChanges = true
            } else if (lastSpendAmount != null) {
                val amount = lastSpendAmount!!
                todayLimit = dataModel.roundMoney(todayLimit + amount)
                howMany = dataModel.roundMoney(howMany + amount)
                dataModel.money.value = howMany
                dataModel.todayLimit.value = todayLimit
                result.text = "$todayLimit"
                historyManager.removeCurrentEntry(0)
                lastSpendAmount = null
                lastOperation.text = ""
                hasUnsavedChanges = true
            }
        }

        // Обработка кнопки +/-
        buttonMetrics(butPlusMinus)
        butPlusMinus.setOnClickListener {
            isAddMode = !isAddMode
            activeCategory = null
            updateTextView2("/день", if (isAddMode) Color.parseColor("#4CAF50") else Color.parseColor("#888888"))
            lastOperation.text = ""
            lastSpendAmount = null
            lastIncomeAmount = null
            rebuildAllRef?.invoke()
            updatePreview()
        }

        dataModel.clearUndo.observe(this) {
            lastSpendAmount = null
            lastIncomeAmount = null
            lastOperation.text = ""
        }

        dataModel.saveClick.observe(this) {
            todayLimit = 0.0
            todayLimit += (avarageDailyValue).toInt()
            dataModel.todayLimit.value = todayLimit
            result.text = todayLimit.toString()
            lastSpendAmount = null
            lastIncomeAmount = null
            lastOperation.text = ""
            hasUnsavedChanges = true
        }

    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Скрытие системной клавиатуры при тапе вне неё
        if (event.action == MotionEvent.ACTION_DOWN && binding.hiddenEmojiInput.hasFocus()) {
            binding.hiddenEmojiInput.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.hiddenEmojiInput.windowToken, 0)
        }
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

    private var rebuildAllRef: (() -> Unit)? = null

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
        rebuildAllRef?.invoke()
    }

    private fun saveBudgetInput() {
        if (!isBudgetInputMode) return
        val newBudget = budgetInputValue.toDoubleOrNull()
        if (newBudget != null && newBudget > 0) {
            dataModel.money.value = newBudget
            dataModel.saveClick.value = true
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
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Категории сохраняем всегда
        editor.putString("SELECTED_EMOJIS", selectedEmojis.joinToString(","))
        editor.putString("CATEGORY_EMOJIS", categoryEmojis.joinToString(","))
        editor.putString("SELECTED_INCOME_EMOJIS", selectedIncomeEmojis.joinToString(","))
        editor.putString("INCOME_CATEGORY_EMOJIS", incomeCategoryEmojis.joinToString(","))
        if (hasUnsavedChanges) {
            val result: String = binding.result.text.toString()
            editor.putString("STRING_KEY", result)
            editor.putString("HOW_MANY", howMany.toString())
            editor.putLong("NUMBER_OF_DAYS", numberOfDays)
            editor.putString("AVARAGE_DAILY_VALUE", avarageDailyValue.toString())
            editor.putString("DATE_FULL", dateFull.toString())
            editor.putString("LAST_DATE", lastDate.toString())
            hasUnsavedChanges = false
        }
        editor.apply()
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

        // Категории
        val savedSelected = sharedPreferences.getString("SELECTED_EMOJIS", null)
        if (!savedSelected.isNullOrEmpty()) {
            selectedEmojis.clear()
            selectedEmojis.addAll(savedSelected.split(","))
        }
        val savedCategories = sharedPreferences.getString("CATEGORY_EMOJIS", null)
        if (!savedCategories.isNullOrEmpty()) {
            categoryEmojis.clear()
            categoryEmojis.addAll(savedCategories.split(","))
        }
        val savedIncomeSelected = sharedPreferences.getString("SELECTED_INCOME_EMOJIS", null)
        if (!savedIncomeSelected.isNullOrEmpty()) {
            selectedIncomeEmojis.clear()
            selectedIncomeEmojis.addAll(savedIncomeSelected.split(","))
        }
        val savedIncomeCategories = sharedPreferences.getString("INCOME_CATEGORY_EMOJIS", null)
        if (!savedIncomeCategories.isNullOrEmpty()) {
            incomeCategoryEmojis.clear()
            incomeCategoryEmojis.addAll(savedIncomeCategories.split(","))
        }

        // Streak
        val streakLastDate = sharedPreferences.getString("STREAK_LAST_DATE", null)
        dayStreak = sharedPreferences.getInt("DAY_STREAK", 0)
        val todayStr = LocalDate.now().toString()
        if (streakLastDate == todayStr) {
            // Уже заходил сегодня — ничего не меняем
        } else if (streakLastDate == LocalDate.now().minusDays(1).toString()) {
            // Заходил вчера — продолжаем серию
            dayStreak++
            sharedPreferences.edit()
                .putInt("DAY_STREAK", dayStreak)
                .putString("STREAK_LAST_DATE", todayStr)
                .apply()
        } else {
            // Пропустил день(и) — серия с 1
            dayStreak = 1
            sharedPreferences.edit()
                .putInt("DAY_STREAK", 1)
                .putString("STREAK_LAST_DATE", todayStr)
                .apply()
        }
        binding.streakCount.text = dayStreak.toString()

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
        if (howMany != 0.0 && numberOfDays > 0) {
            binding.dayLimit.visibility = android.view.View.GONE
        } else {
            binding.dayLimit.visibility = android.view.View.VISIBLE
            binding.dayLimit.text = "${howMany} на ${numberOfDays} дней"
        }
        hasUnsavedChanges = true
    }
}
