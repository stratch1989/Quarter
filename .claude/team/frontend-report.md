# Frontend Report

## Адаптивный UI -- отчёт о выполнении

### Формула масштабирования

Единый базовый юнит `baseUnit`:
- **Portrait**: `screenWidth / 4.3` (в пикселях)
- **Landscape**: `screenHeight / 4.3` (в пикселях)

Все размеры выражены как доля от `baseUnit`. Значения подобраны так, чтобы на эмуляторе-эталоне пропорции совпадали с исходными захардкоженными dp/sp.

---

### Что изменено

#### 1. `MainActivity.kt` -- основные размеры

| Элемент | Было | Стало (коэффициент * baseUnit) |
|---|---|---|
| `result` (основная сумма) | `70sp` в XML | `baseUnit * 0.7f` (COMPLEX_UNIT_PX) |
| `value` (набираемое число) | `60sp` в XML | `baseUnit * 0.6f` |
| `textView2` (метка "/день") | `24sp` в XML | `baseUnit * 0.24f` |
| `lastOperation` | `13sp` в XML | `baseUnit * 0.14f` |
| `noteFieldContainer` высота | `36dp` в XML | `baseUnit * 0.36f` |
| `noteField` текст | `13sp` в XML | `baseUnit * 0.13f` |
| `categoryField` высота (portrait) | `56dp` в XML | `baseUnit * 0.56f` |
| `dateTimeContainer` высота | `30dp` в XML | `baseUnit * 0.3f` |
| `dateFieldText` / `timeFieldText` | `12sp` в XML | `baseUnit * 0.12f` |
| Иконки settings/history | `36dp` в XML | `baseUnit * 0.36f` |
| streakButton | `36dp / 14sp` | `baseUnit * 0.36f / 0.14f` |
| streakCount | `9sp` | `baseUnit * 0.09f` |
| repeatButtonFrame | `36dp` | `baseUnit * 0.36f` |
| result topMargin | `44dp` в XML | `baseUnit * 0.44f` |
| dayLimit defaultMarginTop | `16 * dp` | `baseUnit * 0.16f` |
| dayLimit statusMarginTop | `-4 * dp` | `baseUnit * -0.04f` |

#### 2. `MainActivity.kt` -- автоуменьшение шрифта для длинных чисел

Было: `result.textSize = 70f` (SP). Стало: `result.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseUnit * factor)`.

Коэффициенты для `result`:
- len <= 7: 0.70, len 8: 0.60, len 9: 0.50, len 10: 0.42, else: 0.36

Коэффициенты для `textView2`:
- len <= 7: 0.24, len 8: 0.20, len 9: 0.17, len 10: 0.14, else: 0.12

Коэффициенты для `value`:
- len <= 7: 0.60, len 8: 0.50, len 9: 0.42, len 10: 0.36, else: 0.30

#### 3. `MainActivity.kt` -- dayLimit стили

- `resetDayLimitStyle()`: `textSize = 14f` (sp) -> `setTextSize(PX, baseUnit * 0.14f)`
- `setDayLimitStatusStyle()`: `textSize = 24f` (sp) -> `setTextSize(PX, baseUnit * 0.24f)`

#### 4. `MainActivity.kt` -- кнопки нампада

Функция `buttonMetrics()` дополнена: для `Button` устанавливает `setTextSize(PX, baseUnit * 0.35f)`.
После `buttonMetrics(buttonEnter)` отдельно: `buttonEnter.setTextSize(PX, baseUnit * 0.65f)` (символ крупнее).

#### 5. `MainActivity.kt` -- кнопки категорий (portrait)

Было:
```kotlin
val btnSize = if (isLandscapeMode) (catBtnMetrics * 0.4).toInt() else (38 * dp).toInt()
val gap = if (isLandscapeMode) (catBtnMetrics * 0.08).toInt() else (6 * dp).toInt()
```
Стало:
```kotlin
val btnSize = (catBtnMetrics * 0.4).toInt()
val gap = (catBtnMetrics * 0.08).toInt()
```
Теперь единообразно для обоих ориентаций.

Текст "+" кнопки: `textSize = 16f` -> `setTextSize(PX, baseUnit * 0.16f)`
Текст emoji: `textSize = 16f` -> `setTextSize(PX, baseUnit * 0.16f)`
Крестик удаления: `12 * dp` / `textSize = 7f` -> `baseUnit * 0.12f` / `baseUnit * 0.07f`

#### 6. `MainActivity.kt` -- emoji picker grid

- Размер ячейки: `48 * dp` -> `baseUnit * 0.48f`
- Текст emoji: `textSize = 26f` -> `setTextSize(PX, baseUnit * 0.26f)`
- Кнопка клавиатуры: `48 * dp` -> `baseUnit * 0.48f`, padding `10 * dp` -> `baseUnit * 0.10f`

#### 7. `History.kt` -- адаптивный попап истории

Добавлена полная программная установка размеров в `onViewCreated()`:
- Размер попапа `frameForMetrics`: `screenWidth * 0.65` (portrait) / `screenHeight * 0.55` (landscape)
- Заголовок: `setTextSize(PX, baseUnit * 0.17f)`, padding `baseUnit * 0.2 / 0.1`
- periodTotal: `baseUnit * 0.13f`, emptyText: `baseUnit * 0.14f`
- modeToggle: ширина `baseUnit * 0.42f`, позиция рассчитана от popupSize
- Кнопки mode: высота `baseUnit * 0.46f`, текст `baseUnit * 0.20 / 0.26f`
- typeToggle: высота `baseUnit * 0.42f`, позиция от popupSize
- Кнопки type: ширина `baseUnit * 0.5f`, текст `baseUnit * 0.22f`
- edit/confirm/back: `baseUnit * 0.36f`, позиция от popupSize
- Внутренний padding попапа: `baseUnit * 0.24f`

#### 8. Удалена неиспользуемая переменная `dp` в rebuildAll lambda

---

### Попапы даты и времени -- редизайн и фикс поворота экрана

#### Задача 1: Фикс поворота экрана

**Проблема:** При повороте экрана попапы даты/времени не dismissятся, оставляя stale ссылки. `onConfigurationChanged()` вызывает `recreate()`, попапы теряются.

**Решение:**
- Добавлены поля `datePickerPopup: PopupWindow?` и `timePickerPopup: PopupWindow?` для отслеживания открытых попапов
- В `onPause()` добавлен dismiss для всех попапов: `datePickerPopup?.dismiss()`, `timePickerPopup?.dismiss()`, `subscriptionPopup?.dismiss()`
- При dismiss обнуляются ссылки (`datePickerPopup = null`, `timePickerPopup = null`)
- `popupOverlay` скрывается в `onDismissListener`
- При повторном открытии попапа, предыдущий автоматически закрывается

**Затронутые файлы:**
- `MainActivity.kt`: добавлены поля, обновлен `onPause()`, обновлены `showDatePickerPopup()` и `showTimePickerPopup()`

#### Задача 2: Редизайн попапов в стиле custom interval

**Было:** NumberPicker с DarkDatePickerTheme + текстовая кнопка "Готово"

**Стало:** Кастомные спиннеры с стрелками, полностью в стиле `popup_custom_interval.xml`:
- Фон: `history_popup_bg` (темная карточка #181818 с gradient border)
- Заголовок: отдельная плашка с `history_popup_bg`, белый текст 15sp bold
- Input-поля: фон `category_emoji_bg` (#080808 с border #333333, radius 10dp)
- Стрелки: TextViews с треугольниками, #888888, 12sp
- Кнопка "Готово": ImageButton 36dp с @drawable/okey, positioned top|end

**Date picker:** День (1-31, wrap-around, корректировка по месяцу) + Месяц (янв-дек) + Год (2024-2030)
**Time picker:** Час (00-23, wrap-around, формат %02d) + ":" + Минута (00-59, wrap-around, формат %02d)

**Логика стрелок:** Touch-based repeat (как в custom interval):
- ACTION_DOWN: запуск action + задержка 400ms + повтор каждые 100ms
- ACTION_UP/CANCEL: остановка повтора

**Затронутые файлы:**
- `popup_date_picker.xml` -- полная переработка (NumberPicker -> кастомные спиннеры)
- `popup_time_picker.xml` -- полная переработка (NumberPicker -> кастомные спиннеры)
- `MainActivity.kt` -- `showDatePickerPopup()` и `showTimePickerPopup()` полностью переписаны

#### Задача 3: Адаптивные попапы настроек и подписок

**Settings popup:**
- Ширина: `min(screenWidth * 0.7, 275dp)` -- адаптивная, не больше 275dp
- Высота: `WRAP_CONTENT` вместо фиксированных 270dp
- Текст пунктов меню: `baseUnit * 0.14f` вместо фиксированных 17sp
- Добавлен `android:id="@+id/about_text"` в `popup_settings_menu.xml` для программного доступа

**Subscription popup:**
- Ширина: `min(screenWidth * 0.7, 275dp)` -- адаптивная, не больше 275dp
- Текст пунктов меню (daily/weekly/monthly): `baseUnit * 0.14f`

**Затронутые файлы:**
- `popup_settings_menu.xml` -- добавлен id about_text
- `MainActivity.kt` -- адаптивная ширина и размер текста для settings и subscription попапов

---

### Что оставлено фиксированным и почему

| Элемент | Размер | Причина |
|---|---|---|
| XML-размеры нампад-кнопок (96dp/42dp) | Переопределяются `buttonMetrics()` | Лишь fallback для IDE preview |
| popup_custom_interval.xml | `280 * dp` | Образцовый popup, не менять по ТЗ |
| list_item_history.xml мелкие отступы (4dp, 8dp) | Фиксированные dp | Слишком мелкие для заметного различия; dp масштабируется системой |
| list_item_category_legend.xml | Фиксированные dp/sp | Внутри адаптивного попапа; масштабирование попапа компенсирует |
| activity_main.xml мелкие margin (4dp, 2dp, 0.5dp) | Фиксированные dp | Декоративные отступы, незаметны |
| fragment_history.xml / fragment_history_land.xml | XML-размеры | Переопределяются программно в History.kt |

---

### Возможные проблемы

1. **Emoji picker columnCount = 6** -- захардкожен в XML. На очень узких экранах может не влезть. Можно вычислять: `columnCount = (popupWidth / cellSize).toInt()`.

2. **Landscape history** -- modeToggle и typeToggle используют `FrameLayout.LayoutParams` с абсолютным marginStart/marginTop. При очень узких landscape-экранах (foldables) элементы могут перекрываться. Решение: использовать ConstraintLayout в fragment_history.xml.

3. **Базовый размер textView2** -- при `baseUnit * 0.24f` на планшетах может быть крупноват. Можно добавить `maxTextSize` cap.

---

## Предыдущие замечания (архив)

<details>
<summary>Неиспользуемые ресурсы и проблемы в layouts</summary>

### Drawables -- мёртвые файлы (ни одной ссылки в XML или Kotlin)

- `drawable/back.jpg` -- нет ни одной ссылки
- `drawable/back_background.png` -- нет ни одной ссылки
- `drawable/backgr.png` -- нет ни одной ссылки
- `drawable/button_border.xml` -- нет ни одной ссылки (используются только button_border2...7)
- `drawable/button_border2.xml` -- нет ни одной ссылки в XML/Kotlin (только self-comment внутри файла)
- `drawable/button_border3.xml` -- нет ни одной ссылки в XML/Kotlin
- `drawable/close_button.png` -- нет ни одной ссылки
- `drawable/dark_grey_button.png` -- нет ни одной ссылки (используется только dark_grey_button_inset)
- `drawable/grey_button.png` -- нет ни одной ссылки
- `drawable/orange_button.png` -- нет ни одной ссылки (используется только orange_button_inset)
- `drawable/enter_button.png` -- нет ни одной ссылки (используется только enter_button_inset)
- `drawable/ic_globe.xml` -- нет ни одной ссылки
- `drawable/ic_plus_minus.xml` -- нет ни одной ссылки
- `drawable/circle_button_close_bg.xml` -- нет ни одной ссылки
- `drawable/circle_button_delete_bg.xml` -- нет ни одной ссылки
- `drawable/delete_circle.xml` -- нет ни одной ссылки
- `drawable/popup_bg.xml` -- нет ни одной ссылки (используется только history_popup_bg)
- `drawable/png.png` -- нет ни одной ссылки
- `drawable/settings_window.png` -- нет ни одной ссылки
- `drawable/settings_window_new.png` -- нет ни одной ссылки
- `drawable/window_settings.png` -- нет ни одной ссылки
- `drawable/rounded_corners.xml` -- используется в fragment_budget_input.xml и fragment_date_picker.xml. ИСПОЛЬЗУЕТСЯ (оставить)

### Layouts -- мёртвые файлы

- `layout/test.xml` -- пустой RelativeLayout, нигде не inflate-ится.
- `layout/activity_settings.xml` -- inflate-ится в `Settings.kt`, но `Settings` -- мёртвая Activity.

</details>
