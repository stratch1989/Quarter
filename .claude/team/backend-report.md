# Backend Report

Аудит проведён по всем файлам из области работы. Каждый файл прочитан полностью.

---

## Мёртвый код

### Неиспользуемые методы

**AuthFragment.kt:262** — `getFirebaseErrorMessage(e: Exception): String`
Метод определён, но никогда не вызывается внутри файла и не экспортируется. Весь catch-блок при ошибке входа напрямую вызывает `showError(e.localizedMessage ...)`, минуя этот метод. Grep по всему проекту — 0 вызовов.

**PremiumGate.kt:28** — `inline fun withPremium(action, onLocked)`
Определён как публичный API для проверки Premium в любом месте. Grep по всему проекту показывает 0 вызовов этой функции. `PremiumGate.isPremium` тоже нигде не читается — вся логика идёт через `dataModel.isPremium.value`.

**SubscriptionFragment.kt** — весь класс
Импортируется в MainActivity (строка 59), но `SubscriptionFragment.newInstance()` нигде не вызывается. Grep: единственное упоминание newInstance — внутри самого класса (строка 203). Фрагмент недостижим из UI.

### Неиспользуемые поля / переменные

**DataModel.kt:11** — `val dayLimit: MutableLiveData<Double>`
Единственное место записи — `SettingsFragment.kt:75` (`dataModel.dayLimit.value = daily.toDouble()`). Ни одного `observe()` по всему проекту. Значение записывается и сразу теряется.

**DataModel.kt:21** — `val userName: MutableLiveData<String>`
Записывается в `AuthFragment` и `MainActivity`, но нет ни одного `observe()`. Значение никогда не отображается через LiveData.

**DataModel.kt:20** — `val isLoggedIn: MutableLiveData<Boolean>`
Аналогично — нет ни одного `observe()`. Состояние входа читается напрямую через `AuthManager.isLoggedIn`, а не через LiveData.

**SettingsFragment.kt:29-30** — переменные `howMany` и `daysText`
Инициализируются из `binding.*`, переписываются внутри observer, но нигде не используются для вычислений или отображения после инициализации.

**SettingsFragment.kt:32** — `var dayLimit = 0` (локальный Int)
Используется только в `updateDayLimit()` через integer-деление (`dayLimit / numberOfDays.toInt()`), что теряет дробную часть. При этом `dayLimit` инициализируется через `it.toInt()` из Double — см. раздел Баги.

**MainActivity.kt:97** — `private var isEmojiPickerOpen = false` — используется активно, не мёртвый.
**MainActivity.kt:98** — `private val selectedEmojis` — используется.

### Мёртвые импорты

**MainActivity.kt:59** — `import com.example.quarter.android.billing.SubscriptionFragment`
Импортирован, но не используется (SubscriptionFragment нигде не запускается, см. выше).

**AuthFragment.kt** — нет явно лишних импортов, но `getFirebaseErrorMessage` (который не вызывается) тянет за собой логику парсинга ошибок Firebase.

**DatePickerFragment.kt:16** — `import java.text.SimpleDateFormat`
Используется для форматирования даты в списке. Не является лишним, но используется вместо `java.time.format.DateTimeFormatter` (который уже импортирован в других файлах) — несогласованность.

---

## Баги в бизнес-логике

### DataModel.kt — `spend()`

**DataModel.kt:42-48** — **severity: medium**
`spend()` не валидирует входные данные:
- Отрицательный `amount`: `spend(-100.0, ...)` увеличит `todayLimit` и `howMany` — скрытое пополнение через функцию трат.
- `amount > howMany`: `newBudget` уйдёт в отрицательную зону без предупреждения — это в целом допустимо по замыслу, но нет явной документации и нет защиты от Double underflow.
- `amount = 0`: нет guard clause — тратится 0, добавляется запись в историю (вызывается из MainActivity:993) — нулевая запись в истории возможна если пользователь нажмёт Enter с пустым вводом. В MainActivity:975 есть проверка `fictionalValue.isNotEmpty()`, но `fictionalDigit.toDouble()` может дать `0.0` если `fictionalValue == "."`.

### DataModel.kt — `calculateNewDayOptions()`

**DataModel.kt:56** — **severity: high**
```kotlin
if (numberOfDays <= 1) return
```
При `numberOfDays == 1` метод выходит без установки значений LiveData. Это означает, что `EveryDayQuestion` появится (условие в MainActivity:1462 `numberOfDays > 1`), но опции не будут вычислены — `keyTodayLimitFirstOption` и `keyTodayLimitSecondOption` останутся нулевыми. Фрагмент покажет "Вы сэкономили 0 ₽" и применит нулевой лимит.

Проблема: условие в `loadData()` (MainActivity:1462) использует `> 1`, а в `calculateNewDayOptions()` — `<= 1`. Они согласованы, но при `numberOfDays == 2` метод вызывается, делит на `2 - 1 = 1` — ОК. Однако если `numberOfDays` вычислено неверно (например, дата в прошлом), `ChronoUnit.DAYS.between(lastDate, today)` может быть очень большим, тогда `daysSinceLastDate >> numberOfDays`, и `keyTodayLimitFirstOption` уходит в астрономические значения.

**DataModel.kt:64** — **severity: medium**
```kotlin
keyTodayLimitFirstOption.value = currentKeyTodayLimit + (firstOption * daysSinceLastDate).toInt()
```
Приведение `(firstOption * daysSinceLastDate).toInt()` теряет дробную часть. Если пользователь не открывал приложение несколько дней, накопление округляется вниз. Кроме того, `toInt()` при больших значениях (overflow) даст неверный результат — Long не превращается в Int безопасно.

**DataModel.kt:58** — **severity: low**
```kotlin
val firstOption = (howMany - currentKeyTodayLimit) / (numberOfDays - 1)
```
Если `currentKeyTodayLimit > howMany` (пользователь потратил больше бюджета), `firstOption` будет отрицательным. В UI `EveryDayQuestion` покажет отрицательный дневной лимит без визуального предупреждения.

### DataModel.kt — `roundMoney()`

**DataModel.kt:35** — **severity: low**
```kotlin
fun roundMoney(value: Double): Double = Math.round(value * 100.0) / 100.0
```
`Math.round` возвращает `Long`. Деление `Long / 100.0` даёт `Double` — это корректно. Но при очень больших значениях (> 9 * 10^15) `Math.round` переполнится. Для бюджетного приложения практически недостижимо, но стоит использовать `BigDecimal.ROUND_HALF_UP` для финансовых вычислений.

### MainActivity.kt — логика округления лимита

**MainActivity.kt:284-285** — **severity: medium**
```kotlin
todayLimit = 0.0
todayLimit += avarageDailyValue.toInt()
```
Среднесуточное значение принудительно усекается до целого числа через `.toInt()`. Накопленная ошибка округления за 30 дней может составить до 30 единиц валюты. Аналогично в `saveClick.observe` (строка 1089).

### SettingsFragment.kt — целочисленное деление

**SettingsFragment.kt:74** — **severity: medium**
```kotlin
val daily = dayLimit / numberOfDays.toInt()
```
`dayLimit` — это `Int` (строка 32, `var dayLimit = 0`). Integer-деление теряет дробную часть. При бюджете 10 000 на 30 дней: `10000 / 30 = 333` вместо `333.33`. Это отображение в старом SettingsFragment (который не открывается из новых UI — см. раздел про мёртвые файлы), но логика некорректна.

### EveryDayQuestion.kt — race condition с LiveData

**EveryDayQuestion.kt:37-65** — **severity: medium**
Локальные переменные `avarageDailyValueFirstOption`, `keyTodayLimitFirstOption` и т.д. инициализируются как `0.0`, а значения из LiveData приходят асинхронно. Строка 61:
```kotlin
dataModel.avarageDailyValue.value = avarageDailyValueSecondOption
```
выполняется **до** того, как observer на строке 57 успел получить значение — `avarageDailyValueSecondOption` ещё равен 0.0 в момент записи. Это устанавливает `avarageDailyValue` в 0 при каждом открытии фрагмента.

### DataModel.kt — параметр `daysSinceLastDate` не используется при secondOption

**DataModel.kt:65** — **severity: low**
```kotlin
keyTodayLimitSecondOption.value = secondOption
```
`keyTodayLimitSecondOption` игнорирует `daysSinceLastDate`, хотя при пропуске нескольких дней должен учитывать накопление. Первая опция добавляет за каждый пропущенный день, вторая — нет.

---

## Проблемы с данными (SharedPreferences / Firestore)

### SharedPreferences — непоследовательные типы

**Проблема:** Числовые значения сохраняются как String (`putString("HOW_MANY", howMany.toString())`), но некоторый старый код мог использовать Float. Поэтому в нескольких местах есть `getStringOrFloat()` с `ClassCastException` guard. Это технический долг — если база данных уже полностью мигрирована на String, catch-блок в `getStringOrFloat` больше не нужен, но его присутствие маскирует потенциальные ошибки.

**Потенциальные последствия:** При добавлении новых типов данных разработчик может забыть добавить аналогичный guard и получить ClassCastException на продакшене у пользователей со старыми данными.

### SharedPreferences — атомарность

**Проблема:** В `saveData()` (MainActivity:1324-1360) применяется один `editor.apply()` в конце, что атомарно. Однако в `loadData()` streak сохраняется отдельным `sharedPreferences.edit()...apply()` (строки 1426-1436), а основные данные сохраняются позже в `saveData()`. Если приложение упадёт между этими двумя записями, streak будет обновлён, но бюджетные данные — нет.

**Потенциальные последствия:** Минорная потеря консистентности — счётчик дней вырастет, но данные бюджета могут устареть.

### SharedPreferences — частичная запись в Firestore sync

**FirestoreSync.kt:57-65** — `syncLocalToFirestore` выполняет два отдельных вызова:
```kotlin
userDoc.set(...)   // первый вызов
userDoc.update(...)  // второй вызов
```
Оба fire-and-forget без `addOnFailureListener`. Если первый вызов упадёт — данные не синхронизируются, но и ошибка не обрабатывается. Если первый успешен, а второй падает — история и период потеряны в Firestore, хотя бюджет записан. Нет транзакции.

**Потенциальные последствия:** Частичная синхронизация при нестабильном соединении. Пользователь думает, что данные сохранены.

### Firestore — все write-операции без обработки ошибок

`saveBudgetToFirestore` (строка 84), `createProfile` (строка 134), `updateSubscriptionStatus` (строка 160) — все вызывают `.set()` без `addOnFailureListener`. Пользователь не получит уведомления о сбое синхронизации.

**Потенциальные последствия:** Тихая потеря данных при сбое сети. Пользователь уверен, что данные сохранены в облаке, а они нет.

### SharedPreferences — ключ STRING_KEY сохраняет отображаемое значение

**MainActivity.kt:1333-1334**:
```kotlin
val result: String = binding.result.text.toString()
editor.putString("STRING_KEY", result)
```
Сохраняется строка из TextView, а не `todayLimit.toString()`. Если TextView содержит форматированное или усечённое значение — в SharedPreferences попадёт не то число. Это особенно важно потому, что при `updatePreview()` (строки 839-844) в `result.text` устанавливается `newDaily` (новый дневной лимит), а не `todayLimit`. Если пользователь ввёл сумму и не нажал Enter, затем свернул приложение — в SharedPreferences запишется значение превью, а не реальный лимит.

**Severity: high.** Потеря реального значения `todayLimit` при сворачивании во время ввода.

### DataConflictFragment — recreate() может привести к двойному диалогу

**DataConflictFragment.kt:77-79** — после выбора облачных данных вызывается `activity?.recreate()`. Это перезапускает `MainActivity.onCreate()`, который снова вызывает `loadData()`. Если `today != lastDate`, снова покажется `EveryDayQuestion` поверх только что применённых данных. Нет флага "только что применили облачные данные".

---

## Unused зависимости (build.gradle)

**build.gradle.kts:59** — `androidx.compose.ui:ui:1.5.2`
Не используется. Проект полностью на Views + ViewBinding. Единственный Compose-файл — `MyApplicationTheme.kt`, который сам нигде не вызывается (см. ниже).

**build.gradle.kts:60** — `androidx.compose.ui:ui-tooling:1.5.2`
Не используется. Tooling нужен только для `@Preview` аннотаций в Compose.

**build.gradle.kts:61** — `androidx.compose.ui:ui-tooling-preview:1.5.2`
Не используется по той же причине.

**build.gradle.kts:62** — `androidx.compose.foundation:foundation:1.4.3`
Не используется.

**build.gradle.kts:63** — `androidx.compose.material:material:1.5.1`
Импортируется только в `MyApplicationTheme.kt` который сам является мёртвым файлом.

**build.gradle.kts:64** — `androidx.activity:activity-compose:1.7.2`
Не используется — нет `setContent {}` ни в одной Activity.

**build.gradle.kts:52** — `androidx.navigation:navigation-fragment-ktx:2.7.3`
Не используется. Навигация реализована вручную через `supportFragmentManager.beginTransaction()`. Grep по всему проекту не найдёт ни одного `findNavController()`, `NavGraph`, или `import androidx.navigation`.

**build.gradle.kts:56** — `androidx.preference:preference:1.2.1`
Не используется. `Settings.kt` использует `ComponentActivity`, не `PreferenceFragmentCompat`. Grep: нет ни одного импорта `androidx.preference`.

Итого 8 неиспользуемых зависимостей. Compose-блок (6 штук) значительно увеличивает размер APK (~3-5 МБ).

---

## Дублирование кода

### Дублирование 1: Inline DatePicker в MainActivity vs DatePickerFragment

**MainActivity.kt:383-410** vs **DatePickerFragment.kt:48-91**
В `MainActivity` встроен полноценный `DatePickerDialog` (системный) с той же логикой вычисления дней и установки `keyTodayLimit`, `dateFull`, `dayText`, `dayNumber`, `lastDate`. `DatePickerFragment` делает то же самое через RecyclerView. Два параллельных механизма выбора даты с частично разной логикой:
- MainActivity использует системный DatePickerDialog с минимальной датой "завтра"
- DatePickerFragment — кастомный список из 40 дней

Оба пути устанавливают разные поднаборы LiveData-полей. Риск расхождения при изменении логики.

### Дублирование 2: Сохранение бюджета из MainActivity vs saveBudgetInput()

**MainActivity.kt:977-982** (Enter при isAddMode) и **MainActivity.kt:1185-1187** (`saveBudgetInput`) — оба пути обновляют `dataModel.money.value`, но по-разному: один через `dataModel.spend()`/прямое присвоение, другой через `dataModel.saveClick.value = true`. Логика "сохранить бюджет" размазана по трём местам.

### Дублирование 3: Проверка Premium через checkGooglePlay()

**PremiumManager.kt:134-136** и **SubscriptionFragment.kt:79**
Оба вычисляют `expiresAt` как `System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000`. Magic constant "30 дней" вычисляется в двух местах независимо. Нет именованной константы.

### Дублирование 4: roundMoney — реализована в двух местах

**DataModel.kt:35**: `Math.round(value * 100.0) / 100.0`
**HistoryAdapter.kt:131**: `Math.round(newTotal * 100.0) / 100.0` (не вызывает `dataModel.roundMoney()`)
**SettingsFragment.kt:38**: `(Math.round(it * 100.0) / 100.0).toString()` (дублирует логику roundMoney напрямую)

### Дублирование 5: Загрузка/парсинг Double из SharedPreferences

`getStringOrFloat()` в **MainActivity.kt:1362-1369** и `getDouble()` в **FirestoreSync.kt:195-201** решают одну задачу: читать Double, который мог быть сохранён как Float или String. Идентичная логика в двух местах.

### Дублирование 6: Логика выбора опции карри-овер

**EveryDayQuestion.kt:37-65** содержит локальные копии переменных, дублирующих поля DataModel. Вся логика `keyTodayLimit`, `avarageDailyValueFirstOption` и т.д. читается из LiveData в локальные переменные и обратно записывается в LiveData. Промежуточные переменные — лишние.

---

## Неиспользуемые файлы

**`Quarter/src/main/java/com/example/quarter/android/MyApplicationTheme.kt`**
Чистый Compose-файл. Функция `MyApplicationTheme` нигде не вызывается — grep по всему проекту даёт 0 совпадений. Файл является заглушкой от начального KMP-шаблона. Тянет за собой 6 Compose-зависимостей.

**`Quarter/src/main/java/com/example/quarter/android/Settings.kt`**
Старая Activity (ComponentActivity) с двумя кнопками, из которых работает только Cancel (открывает MainActivity через Intent). Save-кнопка объявлена, но не имеет обработчика. Активити объявлена в AndroidManifest (строка 16), но никогда не запускается из кода — нет ни одного `Intent(context, Settings::class.java)` во всём проекте. Это полностью вытесненная старая реализация настроек, заменённая на PopupWindow в MainActivity.

**`Quarter/src/main/java/com/example/quarter/android/SettingsFragment.kt`**
Фрагмент `SettingsFragment` нигде не открывается из MainActivity или других фрагментов. Единственные вызовы `SettingsFragment.newInstance()` — внутри самого файла (companion object). Настройки перенесены в PopupWindow прямо в MainActivity. Фрагмент содержит устаревшую логику с integer-делением (см. Баги).

**`Quarter/src/main/java/com/example/quarter/android/DatePickerFragment.kt`**
Открывается только из `SettingsFragment.kt` (строка 102), который сам является мёртвым файлом. Таким образом, `DatePickerFragment` тоже недостижим из живого UI. Логика дублируется встроенным DatePickerDialog в MainActivity.

### Уточнение: BillingManager не закрывается

**PremiumManager.kt:127-148** — `checkGooglePlay()` создаёт `BillingManager`, подключается и после проверки подписки не вызывает `billing.disconnect()`. BillingClient остаётся подключённым. В `SubscriptionFragment.onDestroyView()` вызов `disconnect()` есть (строка 197), но в PremiumManager — нет.

---

## Дополнительные наблюдения (вне формальных категорий)

**RuStoreBillingManager.kt:14** — `CONSOLE_APP_ID = "YOUR_RUSTORE_CONSOLE_APP_ID"` — placeholder не заменён. При работе с RuStore все операции упадут с ошибкой аутентификации. TODO-комментарий присутствует (строка 13).

**RuStoreBillingManager.kt:37-46** — `launchPurchaseFlow` при ошибке покупки игнорирует её (пустой `addOnFailureListener`) и не вызывает callback. Пользователь нажимает "Подписаться", ничего не происходит, никакой ошибки.

**SubscriptionsListFragment.kt:98** — удаление по индексу `i` в цикле `for (i in subscriptions.indices)`. Если `setOnClickListener` срабатывает после изменения списка (теоретически), индекс устарел. В данной реализации безопасно т.к. после удаления вызывается `buildList()`, но паттерн хрупкий.

**History.kt — удаление из истории**: при удалении старых записей (HistoryItem.Old) кнопка удаления скрыта (`View.GONE`) — это правильно. Но при groupByDay используется `compareByDescending`, что сортирует по String. Формат даты ISO 8601 (YYYY-MM-DD) корректно сортируется лексикографически, поэтому работает правильно — не баг, но неочевидно.
