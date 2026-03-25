# Backend Report — Guided Numpad Onboarding

## Реализовано

Все изменения в `Quarter/src/main/java/com/example/quarter/android/MainActivity.kt`.

### Новые переменные класса (строки ~102-108)
- `isFirstSetup: Boolean` — флаг активного setup mode
- `setupStep: Int` — текущий шаг (0=неактивен, 1=бюджет, 2=дата)
- `setupInputValue: String` — вводимое значение бюджета
- `cursorAnimator: ObjectAnimator?` — анимация мигающего курсора
- `setupDateCancelCount: Int` — счётчик отмен DatePicker
- `setupBackCallback: OnBackPressedCallback?` — блокировка Back

### Активация в `loadData()` (перед проверкой нового дня)
- `howMany == 0.0 && numberOfDays <= 0` → полный setup (шаг 1 + 2)
- `howMany > 0.0 && numberOfDays <= 0` → только DatePicker (шаг 2)
- Используется `binding.root.post {}` для отложенного вызова после полной инициализации UI

### Метод `enterSetupMode(step)`
- Устанавливает флаги, регистрирует OnBackPressedCallback
- Делегирует на `enterSetupStep1()` или `enterSetupStep2()`

### Метод `enterSetupStep1()` — ввод бюджета
- Затемняет элементы: history, streakButton, streakCount, repeatButtonFrame, settings, categoryField, categoryScroll (alpha 0.15)
- Скрывает: value, textView2, noteFieldContainer, dateTimeContainer, lastOperation (alpha 0)
- day_limit: "Укажите бюджет", оранжевый (#FF9800), bold, размер baseUnit * 0.20f
- result: мигающий курсор "_" через ObjectAnimator (alpha 1→0.3, 500ms, INFINITE, REVERSE)
- Нампад полностью активен, +/- заблокирован (disabled + alpha 0.3)

### Обработка ввода в `buttonBinding()`
- При нажатии цифры в setup step 1: остановка мигания курсора, обновление result.text
- Показ подсказки "Укажите бюджет\nНажмите ↵"
- Setup step 2: все нажатия нампада заблокированы

### Обработка Delete в setup mode
- Удаление последнего символа, возврат к мигающему курсору при пустом вводе
- Long press: полная очистка ввода

### Обработка Enter в setup mode
- Если число = 0 или пусто: подсказка мигает красным на 300ms
- Если число > 0: haptic feedback (CONFIRM), сохранение бюджета, переход к шагу 2
- В шаге 2: Enter заблокирован

### Метод `enterSetupStep2()` — выбор даты
- Нампад затемнён (alpha 0.15)
- day_limit: "До какого числа?", оранжевый bold
- result: показывает введённый бюджет
- Через 200ms вызывает `showSetupDatePicker()`

### Метод `showSetupDatePicker()`
- DatePickerDialog с DarkDatePickerTheme (переиспользование существующего стиля)
- minDate = завтра, initDate = +7 дней
- При выборе даты: haptic feedback, установка всех полей DataModel, вызов HistoryManager.updatePeriodStart(), saveData(), completeSetup()
- При отмене: повторный показ через 500ms; после 2 отмен — кликабельный текст "Нажмите чтобы выбрать дату"

### Метод `completeSetup()` — завершение
- Анимация 400ms DecelerateInterpolator: все элементы → alpha 1.0
- result → todayLimit, value → "0", textView2 → "/день"
- day_limit → reset стиля + GONE
- Разблокировка +/-, удаление OnBackPressedCallback
- isFirstSetup = false, setupStep = 0

### Защита observers от перезаписи UI
- `dataModel.money.observe`: `updateDayLimitText()` вызывается только если `!isFirstSetup`
- `dataModel.dateFull.observe`: полное тело observer обёрнуто в `if (!isFirstSetup)`
- `dataModel.todayLimit.observe`: `binding.result.text` обновляется только если `!isFirstSetup`

### Блокировка кнопок во время setup
- history, settings, streakButton, repeatButton, butPlusMinus, butUndo — return при `isFirstSetup`

## Ключевые решения

1. **`binding.root.post {}` для отложенного запуска**: setup mode вызывается после завершения `loadData()` и инициализации layout, чтобы baseUnit был рассчитан и все binding-ы доступны.

2. **Переиспользование DatePickerDialog**: используется тот же `R.style.DarkDatePickerTheme` и логика minDate, что и в настройках.

3. **Защита observers через `isFirstSetup`**: LiveData observers срабатывают при `dataModel.money.value = howMany` в setup mode и могут перезаписать UI. Гварды предотвращают это.

4. **OnBackPressedCallback вместо onBackPressed()**: используется современный API AndroidX для блокировки Back.

5. **Два уровня отмены DatePicker**: первая отмена показывает picker снова через 500ms; после второй — переключается на кликабельный текст, давая пользователю контроль.

## Edge Cases

1. **Бюджет 0 при повторном запуске**: если пользователь введёт бюджет, но не выберет дату, при следующем запуске `howMany > 0 && numberOfDays <= 0` — покажется только DatePicker (шаг 2).

2. **Точка без цифр**: кнопка "." разрешена как первый символ (будет "." → "0." при toDouble), но пустой setupInputValue.toDoubleOrNull() вернёт null → красная вспышка.

3. **onPause во время setup**: `onPause()` вызывает `saveData()`, что сохранит текущее состояние. При возврате `loadData()` обнаружит отсутствие бюджета/даты и повторно активирует setup.

4. **Ротация экрана**: `onConfigurationChanged` вызывает `recreate()`, что пересоздаст Activity. loadData() снова проверит условия и активирует setup mode при необходимости.

5. **HapticFeedbackConstants.CONFIRM**: требует API 30+. Проект использует minSdk 29. На API 29 вызов может быть проигнорирован (не упадёт, но не будет тактильного отклика).
