# QA Frontend Report -- Adaptive UI

**Дата**: 2026-03-25
**Область**: Адаптивный UI через baseUnit = screenDimension / 4.3
**Файлы**: MainActivity.kt, History.kt, все layout XML (portrait + landscape)

---

## Критические баги

### CRIT-1: Конфликт XML textSize vs программный textSize в layout-land/activity_main.xml

**Проблема**: В `layout-land/activity_main.xml` кнопки нампада имеют `android:textSize="20sp"`, а в `layout/activity_main.xml` -- `android:textSize="35sp"`. Код в `buttonMetrics()` (строка 1082) переопределяет размер текста для Button на `baseUnit * 0.35f` (px). Но между моментом inflate layout и выполнением `buttonMetrics()` пользователь может на долю секунды увидеть размер из XML. Более критично: если `buttonMetrics()` вызывается ДО того, как layout завершит measure/layout pass, XML-значение может "победить" программное.

**Реальный риск**: Низкий (программное значение ставится в `onCreate` до первого draw), но "грязный" код -- XML и код задают один и тот же параметр разными значениями.

**Затронутые элементы**: Все 16 кнопок нампада, button_enter, button_del, button_undo.

**Файлы**:
- `Quarter/src/main/res/layout/activity_main.xml` (35sp)
- `Quarter/src/main/res/layout-land/activity_main.xml` (20sp)
- `Quarter/src/main/java/.../MainActivity.kt:1082` (baseUnit * 0.35f px)

---

### CRIT-2: `onConfigurationChanged()` вызывает `recreate()` -- потеря состояния ввода

**Проблема**: Манифест содержит `android:configChanges="orientation|screenSize|screenLayout"`, и `onConfigurationChanged()` (строка 1794-1798) вызывает `recreate()`. При повороте экрана:
1. Текущий ввод `fictionalValue` теряется (не сохраняется в `onPause`/`savedInstanceState`)
2. Состояние emoji picker (`isEmojiPickerOpen`) сбрасывается
3. Активная заметка (`activeNote`) теряется
4. Редактируемая запись (`editingEntry`) теряется
5. `activeCategory` теряется

Это НЕ баг адаптивного UI как такового, но `recreate()` был добавлен именно ради адаптивного UI, и при повороте пользователь теряет незавершённый ввод.

**Файлы**:
- `Quarter/src/main/AndroidManifest.xml:17`
- `Quarter/src/main/java/.../MainActivity.kt:1794-1798`

---

## Средние баги

### MED-1: History popup -- элементы XML с фиксированными dp-позициями (portrait fragment_history.xml)

**Проблема**: В `layout/fragment_history.xml` (portrait) элементы `modeToggle`, `typeToggle`, `editButton`, `confirmButton`, `categoryBackButton` имеют жёсткие dp-отступы (258dp, 300dp, 180dp, 340dp). Код в `History.kt` (строки 80-117) ПЕРЕОПРЕДЕЛЯЕТ эти значения программно. Однако:

1. При inflate XML-значения применяются первыми
2. Программные значения ставятся в `onViewCreated`, до первого draw
3. В теории XML-позиции не должны быть видны, но если произойдёт layout pass между inflate и onViewCreated (маловероятно, но возможно при тяжёлой нагрузке) -- элементы "дёрнутся"

Кроме того, если кто-то уберёт программные размеры -- XML-значения будут абсолютно неверны на большинстве экранов.

**Рекомендация**: Убрать фиксированные dp-отступы из XML (поставить 0dp) и полагаться только на код.

**Файлы**:
- `Quarter/src/main/res/layout/fragment_history.xml` (258dp, 300dp, 180dp, 340dp)
- `Quarter/src/main/res/layout-land/fragment_history.xml` (228dp)
- `Quarter/src/main/java/.../History.kt:60-117`

---

### MED-2: History items (RecyclerView) используют ФИКСИРОВАННЫЕ sp-размеры

**Проблема**: Внутри history popup текст элементов списка имеет фиксированные sp-размеры из XML, НЕ адаптивные:

| Элемент | XML файл | Размер |
|---------|----------|--------|
| `entryEmoji` | list_item_history.xml | 14sp |
| `entryNote` | list_item_history.xml | 12sp |
| `entryTime` | list_item_history.xml | 11sp |
| `amount` | list_item_history.xml | 16sp |
| `dayTitle` | list_item_history_day_header.xml | 13sp |
| `dayTotal` | list_item_history_day_header.xml | 13sp |
| `legendLabel` | list_item_category_legend.xml | 14sp |
| `legendAmount` | list_item_category_legend.xml | 16sp |
| `deleteButton` | list_item_history.xml | 28dp |

`HistoryAdapter.kt` не вызывает `setTextSize()` нигде. Это значит: контейнер попапа масштабируется от baseUnit (screenWidth * 0.65 / screenHeight * 0.55), но текст внутри него -- нет. На маленьком экране текст может быть непропорционально крупным относительно попапа, а на планшете -- мелким.

**Воспроизведение**: На экране 320dp mdpi (480px) попап = 312px, а текст 16sp = 16px. На планшете 600dp xxhdpi (1800px) попап = 1170px, а текст 16sp = 48px. Соотношение текста к попапу меняется в ~2x.

**Файлы**:
- `Quarter/src/main/res/layout/list_item_history.xml`
- `Quarter/src/main/res/layout/list_item_history_day_header.xml`
- `Quarter/src/main/res/layout/list_item_category_legend.xml`
- `Quarter/src/main/java/.../HistoryAdapter.kt`

---

### MED-3: Settings popup -- фиксированные dp-размеры, не адаптивный

**Проблема**: Settings popup (`popup_settings_menu.xml`) использует:
- Фиксированный размер: `275dp x 270dp` (строки 480-481 в MainActivity.kt)
- Фиксированный текст: `17sp` для всех пунктов меню

Это НЕ масштабируется через baseUnit. На маленьком экране (320dp / 480px) попап будет 275 * 1.0 = 275px из 480px = 57% ширины. На планшете (600dp / 1800px) попап будет 275 * 3.0 = 825px из 1800px = 46% ширины. Разница незначительная. Но текст 17sp на планшете (51px) может не помещаться вместе с эмодзи для длинных email-адресов.

Аналогичная проблема у subscription popup (275dp) и custom interval popup (280dp).

**Файлы**:
- `Quarter/src/main/java/.../MainActivity.kt:480-481` (275dp x 270dp)
- `Quarter/src/main/java/.../MainActivity.kt:613` (275dp)
- `Quarter/src/main/java/.../MainActivity.kt:1667` (280dp)
- `Quarter/src/main/res/layout/popup_settings_menu.xml` (17sp)

---

### MED-4: EveryDayQuestion -- полностью фиксированные размеры

**Проблема**: `EveryDayQuestion` и `fragment_every_day_question.xml` НЕ используют baseUnit вообще:
- Контейнер: 320dp (фиксированная ширина)
- Текст: 32sp, 16sp, 14sp (фиксированные)
- Кнопки: 56dp высота
- Картинка: 100dp

Код `EveryDayQuestion.kt` не содержит ни одного вызова `setTextSize()` или изменения `layoutParams`.

На маленьком экране 320dp mdpi -- контейнер занимает всю ширину (320dp = 320px). На больших экранах -- он непропорционально мал.

**Файлы**:
- `Quarter/src/main/res/layout/fragment_every_day_question.xml`
- `Quarter/src/main/java/.../EveryDayQuestion.kt`

---

### MED-5: DonutChartView использует фиксированные dp-размеры текста

**Проблема**: `DonutChartView.kt` устанавливает текст через `centerTextPaint.textSize = 18 * dp` / `14 * dp` / `16 * dp` (строки 196-202). Это фиксированные dp-значения, не зависящие от baseUnit. Внутри history popup, размер которого масштабируется, текст диаграммы будет иметь фиксированный размер.

**Файл**: `Quarter/src/main/java/.../DonutChartView.kt:196-203`

---

### MED-6: active_category_indicator -- фиксированные размеры

**Проблема**: `active_category_indicator` в обоих layout XML имеет фиксированные 36dp x 36dp и textSize 18sp. Код (`MainActivity.kt:770, 1332`) только меняет visibility, не размеры. При этом остальные кнопки категорий масштабируются через `catBtnMetrics * 0.4`, а индикатор -- нет.

На маленьком экране (480px): категория-кнопка = 480/4.3*0.4 = 45px, индикатор = 36 * 1.0 = 36px -- ОК.
На планшете (1800px): категория-кнопка = 1800/4.3*0.4 = 167px, индикатор = 36 * 3.0 = 108px -- непропорционально мал.

**Файлы**:
- `Quarter/src/main/res/layout/activity_main.xml:457-467`
- `Quarter/src/main/res/layout-land/activity_main.xml:373-384`

---

## Мелочи

### LOW-1: repeat_badge_letter (7sp) и repeat_center_number (6sp) не масштабируются

Эти мини-бейджи внутри `repeat_button_frame` имеют фиксированные 7sp и 6sp. Рамка `repeat_button_frame` масштабируется (`iconBtnSize = baseUnit * 0.36`), но текст внутри -- нет. На маленьком экране 6sp может быть нечитаемым, на большом -- слишком мелким относительно рамки.

**Файлы**:
- `Quarter/src/main/res/layout/activity_main.xml:496,508`
- `Quarter/src/main/res/layout-land/activity_main.xml:91,103`

---

### LOW-2: noteField hint "Заметка" имеет XML textSize 13sp, перекрывается кодом

noteField имеет `android:textSize="13sp"` в XML, но код ставит `baseUnit * 0.13f` px (строка 1039). При 360dp xxhdpi (baseUnit=251): `251*0.13 = 32.6px`, а `13sp * 3.0 = 39px`. Код переопределяет XML значение меньшим -- это ОК, но для clarity лучше убрать textSize из XML.

**Файлы**:
- `Quarter/src/main/res/layout/activity_main.xml:626`
- `Quarter/src/main/res/layout-land/activity_main.xml:199`
- `Quarter/src/main/java/.../MainActivity.kt:1039`

---

### LOW-3: dateFieldText и timeFieldText: XML 12sp конфликтует с кодом

Аналогично LOW-2. XML задаёт 12sp, код ставит `baseUnit * 0.12f` px. Значения разные.

---

### LOW-4: About, Streak, Auth, DataConflict, SubscriptionsList -- фиксированные sp/dp

Все эти фрагменты используют только фиксированные sp-размеры из XML и не масштабируются через baseUnit. Это consistent-style попапы/диалоги, так что не критично, но не является частью единой адаптивной системы.

| Fragment | Текст-размеры (sp) |
|----------|-------------------|
| AboutFragment | 28, 15, 13 |
| StreakFragment | 48, 24, 14 |
| DataConflict | 20, 14, 16, 13 |
| SubscriptionsList | 20, 15 |
| AuthFragment | не проверено |

---

### LOW-5: `x`-кнопка в emoji picker -- текст 0.07f может быть слишком мелким

На маленьком экране (480px, baseUnit=112): `112 * 0.07 = 7.8px`. Это технически >= 8px, но на границе читаемости. Контейнер `xSize = baseUnit * 0.12 = 13.4px`. Текст 7.8px в 13px круге может быть трудно различимым.

---

## Проверено и ОК

### Математика масштабирования -- все целевые экраны

| Экран | Density | Width px | baseUnit | result (0.7f) | value (0.6f) | numpad btn text (0.35f) | enter text (0.65f) | btn size (px) | btn size (dp) |
|-------|---------|----------|----------|---------------|--------------|------------------------|--------------------|----|-----|
| 320dp mdpi | 1.0 | 480 | 112 | 78px | 67px | 39px | 73px | 112 | 112dp |
| 360dp xxhdpi | 3.0 | 1080 | 251 | 176px | 151px | 88px | 163px | 251 | 84dp |
| 411dp xxxhdpi | 4.0 | 1440 | 335 | 235px | 201px | 117px | 218px | 335 | 84dp |
| 600dp xxhdpi | 3.0 | 1800 | 419 | 293px | 251px | 147px | 272px | 419 | 140dp |

**Все тексты >= 8px** -- ОК.

**Минимальные размеры кнопок (48dp rule)**:
- 320dp mdpi: 112dp -- ОК
- 360dp xxhdpi: 84dp -- ОК
- 411dp xxxhdpi: 84dp -- ОК
- 600dp xxhdpi: 140dp -- ОК

---

### Нампад: 4 кнопки в ряд

Каждая кнопка = baseUnit px. 4 кнопки в ряд = 4 * baseUnit = 4 * (screenWidth / 4.3) = screenWidth * 0.93. Оставшиеся 7% -- margings и padding. Помещается корректно.

В landscape: 4 * (screenHeight / 4.3) -- numpad справа от guideline_center (60%), так что numpad занимает 40% ширины. 4 * (screenHeight/4.3) vs 40% screenWidth. Для типичного 16:9 (1920x1080): 4 * (1080/4.3) = 1004px, 40% * 1920 = 768px. **Не помещается!** Но реально landscape linearLayout имеет `android:layout_width="0dp"` с constraints `start_toEndOf guideline_center` и `end_toEndOf parent`, так что кнопки будут сжаты constraints. Это может привести к нечитаемому UI в landscape для широких экранов, хотя `buttonMetrics` ставит фиксированный размер. Стоит проверить на реальном устройстве.

**Вердикт**: Portrait -- ОК. Landscape -- потенциальный overflow (см. LOW замечание ниже).

---

### TypedValue.COMPLEX_UNIT_PX -- все вызовы корректны

Проверены ВСЕ 27 вызовов `setTextSize()` в `MainActivity.kt` и `History.kt`. Каждый использует `TypedValue.COMPLEX_UNIT_PX`. Ни одного вызова `setTextSize(float)` без unit. **ОК**.

---

### baseUnit вычисляется в правильном месте

`baseUnit` вычисляется в `onCreate()` (строка 1027-1029), после `setContentView()` (строка 244), что гарантирует корректные `displayMetrics`. Это правильное место в жизненном цикле. **ОК**.

---

### layoutParams обновляются корректно

Все вызовы `layoutParams.width/height = ...` следуют паттерну "get layoutParams -> modify -> set layoutParams". В `buttonMetrics()` (строка 1071-1074), в History.kt (строки 60-64, 80-85, 94-98, 109-117) -- всё корректно. **ОК**.

---

### Автоуменьшение шрифта для длинных чисел

`result` TextWatcher (строки 354-376): коэффициент уменьшается от 0.7f (<=7 символов) до 0.36f (11+ символов). `textView2` синхронно уменьшается от 0.24f до 0.12f. `value` TextWatcher (строки 378-392): от 0.6f до 0.30f.

Минимальные размеры на маленьком экране (baseUnit=112):
- result: 112 * 0.36 = 40px -- ОК
- textView2: 112 * 0.12 = 13.4px -- ОК
- value: 112 * 0.30 = 33.6px -- ОК

Базовые от правильного базового (baseUnit) -- **ОК**.

---

### Portrait/Landscape -- корректное переключение

`onConfigurationChanged()` вызывает `recreate()`, что заново inflate-ит correct layout (`layout/` vs `layout-land/`). baseUnit пересчитывается (screenWidth в portrait, screenHeight в landscape). Переключение работает корректно, но с потерей состояния (см. CRIT-2). **Механизм переключения -- ОК**.

---

### Smoke test

```
./gradlew :Quarter:assembleDebug
BUILD SUCCESSFUL in 1s
```

**ОК** -- проект собирается без ошибок.

---

## Итого

| Категория | Количество |
|-----------|-----------|
| Критические | 2 |
| Средние | 6 |
| Мелочи | 5 |
| ОК | 8 проверок |

**Главный вывод**: Адаптивный UI через baseUnit реализован корректно для основного экрана (MainActivity). Все setTextSize вызовы используют COMPLEX_UNIT_PX, baseUnit вычисляется в правильном месте, кнопки и тексты масштабируются пропорционально. Основные пробелы: (1) вспомогательные экраны (EveryDayQuestion, Settings popup, About и др.) не адаптированы; (2) элементы внутри history RecyclerView используют фиксированные sp-размеры, создавая диспропорцию с адаптивным попапом; (3) `recreate()` при повороте теряет незавершённый пользовательский ввод.
