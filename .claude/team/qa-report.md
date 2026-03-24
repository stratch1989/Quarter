# QA Report

Дата: 2026-03-24
Методология: чтение обоих отчётов (frontend, backend), верификация каждой находки по исходному коду, собственный анализ edge cases, lifecycle, навигации и интеграций. Smoke test сборки.

---

## Smoke Test

```
./gradlew :Quarter:assembleDebug
BUILD SUCCESSFUL in 8s
```

Сборка проходит. Компилятор выдаёт предупреждения:
- `Settings.kt:16` — `saveButtom` never used (подтверждает мёртвый код)
- `SettingsFragment.kt:29,30` — `howMany` assigned but never accessed, `daysText` never used
- `SubscriptionEntry.kt:43` — type mismatch (Nothing? vs String)
- `AuthFragment.kt`, `AuthManager.kt` — `GoogleSignIn` deprecated
- `BillingManager.kt` — `enablePendingPurchases()` deprecated
- `SubscriptionFragment.kt` — неиспользуемые параметры `purchase`, `result`

---

## Верификация Frontend-отчёта

### 1. Неиспользуемые drawables (~20 файлов)
**CONFIRMED.** Проверено grep-ом по проекту. Все перечисленные drawables (back.jpg, back_background.png, backgr.png, button_border.xml, button_border2.xml, button_border3.xml, close_button.png, dark_grey_button.png, grey_button.png, orange_button.png, enter_button.png, ic_globe.xml, ic_plus_minus.xml, circle_button_close_bg.xml, circle_button_delete_bg.xml, delete_circle.xml, popup_bg.xml, png.png, settings_window.png, settings_window_new.png, window_settings.png) не имеют ссылок из XML или Kotlin. Отдельно подтверждено, что `button_border2.xml` не используется нигде, включая другие layout-файлы.

### 2. Мёртвые layouts (test.xml, activity_settings.xml)
**CONFIRMED.** `test.xml` нигде не inflate-ится. `activity_settings.xml` используется только в `Settings.kt`, который является мёртвой Activity (см. ниже).

### 3. Мёртвые строки в strings.xml
**CONFIRMED.** Все записи (`messages_header`, `sync_header`, `dummy_button`, `dummy_content`, `hello_blank_fragment` и т.д.) используются только в `xml/root_preferences.xml`, который сам нигде не загружается.

### 4. Мёртвые цвета и стили
**CONFIRMED.** `light_blue_*`, `black_overlay`, `FullscreenAttrs`, `Widget.AppTheme.ButtonBar.Fullscreen`, `ThemeOverlay.Quarter.FullscreenContainer` — всё boilerplate от шаблона Fullscreen Activity, нигде не применяется в живом коде.

### 5. Hardcoded строки в layouts
**CONFIRMED.** Все 60+ перечисленных случаев hardcoded русского текста действительно присутствуют. `strings.xml` не содержит ни одной пользовательской строки из UI.

### 6. Отсутствие contentDescription у ImageView
**CONFIRMED.** Все перечисленные 8 случаев подтверждены. Проблема accessibility — реальная, но severity low для приложения без планов на accessibility.

### 7. Deprecated атрибуты (TextAppearance.AppCompat.Display4, tools:context с несуществующими классами)
**CONFIRMED.** `tools:context=".BlankFragment2"` (SettingsFragment.kt переименован), `.BlankFragment`, `.BlankFragment3` — старые имена классов. `paddingLeft` вместо `paddingStart` и `gravity="center|left"` вместо `center|start` — подтверждено.

### 8. Binding не обнуляется в onDestroyView
**CONFIRMED.** Ни один из 6 перечисленных фрагментов (SettingsFragment, BudgetInputFragment, DatePickerFragment, EveryDayQuestion, History, StreakFragment) не обнуляет binding. Все используют `lateinit var binding` без `onDestroyView()`. Потенциальная утечка View, хотя в данном приложении с одной Activity вероятность реальной утечки невелика.

### 9. `activity as LifecycleOwner` вместо `viewLifecycleOwner`
**CONFIRMED.** Все 11 перечисленных случаев подтверждены в SettingsFragment (3 случая), BudgetInputFragment (1), DatePickerFragment (2), EveryDayQuestion (5). Это реальная проблема: при пересоздании View фрагмента observer-ы накапливаются. Однако в текущей архитектуре (фрагменты показываются/скрываются через replace + addToBackStack) View пересоздаётся при возврате из backstack, и тогда каждый `onViewCreated` добавит новый observer с `activity` как LifecycleOwner — они не отпишутся пока Activity не будет уничтожена.

**Уточнение:** В MainActivity observer-ы привязаны к `this` (Activity), что корректно для Activity-скопированных observer-ов, так как Activity — это и есть LifecycleOwner.

### 10. Binding объявлен как public
**CONFIRMED.** Все фрагменты используют `lateinit var binding` (public по умолчанию). Minor — стилистическая проблема.

### 11. howMany/daysText бессмысленная инициализация (SettingsFragment:29-30)
**CONFIRMED.** Компилятор подтверждает: `howMany` assigned but never accessed, `daysText` never used. Мёртвый код.

### 12. AuthFragment.getFirebaseErrorMessage() — мёртвый код
**CONFIRMED.** Grep по всему проекту показывает ровно одно определение на строке 262, ни одного вызова.

### 13. EveryDayQuestion:61 — `dataModel.avarageDailyValue.value = avarageDailyValueSecondOption`
**NEEDS MORE CONTEXT / PARTIALLY CONFIRMED.** Frontend-отчёт утверждает, что значение устанавливается до получения observer-ом данных и `avarageDailyValueSecondOption = 0.0`. Технически LiveData при `observe()` диспатчит текущее значение синхронно, если LifecycleOwner уже в STARTED/RESUMED состоянии (что верно для activity). Поэтому к строке 61 локальная переменная уже обновлена observer-ом. **Реального бага с 0.0 нет в runtime.** Однако код по-прежнему хрупок: строка 61 устанавливает `avarageDailyValue` ещё до того, как пользователь сделал выбор. Это не баг, а "по замыслу" — option2 кликает без явной установки avarageDailyValue, полагаясь на значение из строки 61.

### 14. History.kt — binding внутри withEndAction анимации
**CONFIRMED.** В `dismissWithAnimation()` (строка 31) используется `binding.clickableBackground` внутри `withEndAction`. Однако есть проверка `if (isAdded)` перед `popBackStack()`, что снижает вероятность краша. Обращение к `binding` после уничтожения View всё ещё возможно теоретически, но `withEndAction` выполняется на главном потоке, а `onDestroyView` тоже — поэтому порядок вызовов детерминирован. Severity low.

### 15. SubscriptionsListFragment:98 — удаление по индексу `i`
**CONFIRMED, но severity ниже заявленной.** Код `subscriptions.removeAt(i)` находится внутри `setOnClickListener` лямбды. Переменная `i` захвачена из цикла `for (i in subscriptions.indices)`. После удаления вызывается `buildList()`, который полностью перестраивает UI с новыми лямбдами. Проблема: `i` — captured val из цикла, после `removeAt(i)` и `buildList()` старые лямбды уже не видны (View перестроены). Реального бага с IndexOutOfBoundsException нет, потому что между удалениями UI перестраивается. Но `onDelete?.invoke(i)` передаёт индекс `i` в MainActivity, где `subscriptions.removeAt(index)` вызывается **второй раз** — это ДВОЙНОЕ УДАЛЕНИЕ. Элемент удаляется и во фрагменте (строка 98), и в MainActivity callback (строка 494). Это реальный баг.

### 16. Мёртвые файлы (Settings.kt, MyApplicationTheme.kt, root_preferences.xml)
**CONFIRMED.** `Settings.kt` зарегистрирована в Manifest, но нигде не запускается. `MyApplicationTheme.kt` — Compose boilerplate. `root_preferences.xml` — не загружается.

### 17. Пустые FrameLayout frameAbout / frameXZ
**CONFIRMED.** Но SettingsFragment сам является мёртвым фрагментом (недостижим из UI), поэтому severity minimal.

---

## Верификация Backend-отчёта

### 1. AuthFragment.getFirebaseErrorMessage — мёртвый метод
**CONFIRMED.** (Совпадает с frontend-отчётом.)

### 2. PremiumGate.withPremium — мёртвый метод
**CONFIRMED.** Grep: 0 вызовов `PremiumGate.isPremium` и `PremiumGate.withPremium` во всём проекте.

### 3. SubscriptionFragment — недостижим из UI
**CONFIRMED.** Grep по `SubscriptionFragment.newInstance()` — единственное определение внутри companion object. Нигде не вызывается. Импорт в MainActivity:59 есть, но вызова нет.

### 4. DataModel.dayLimit — нет observer-ов
**CONFIRMED.** Grep: `dayLimit.observe` — результаты показывают только `keyTodayLimit.observe` и `todayLimit.observe`, ни одного `dayLimit.observe`. Значение записывается в SettingsFragment:75 и теряется.

### 5. DataModel.userName — нет observer-ов
**CONFIRMED.** Grep: 0 результатов для `userName.observe`.

### 6. DataModel.isLoggedIn — нет observer-ов
**CONFIRMED.** Grep: 0 результатов для `isLoggedIn.observe`. Чтение идёт через `AuthManager.isLoggedIn`.

### 7. SettingsFragment.howMany/daysText — мёртвые переменные
**CONFIRMED.** (Совпадает с frontend-отчётом и предупреждениями компилятора.)

### 8. SettingsFragment.dayLimit — integer-деление
**CONFIRMED.** Строка 74: `val daily = dayLimit / numberOfDays.toInt()` — `dayLimit` это `Int` (строка 32), деление целочисленное. Однако SettingsFragment недостижим из UI, поэтому **severity none (мёртвый код)**.

### 9. spend() — нет валидации входных данных
**CONFIRMED.** Метод не проверяет отрицательный amount, NaN, Infinity. Однако входные данные контролируются UI: numpad не позволяет ввести отрицательное число (нет кнопки "-"). Ввод "." без цифр отсекается проверкой `value.text != "."` (строка 975). Пустой ввод отсекается `fictionalValue.isNotEmpty()`. **Severity: low** — эксплойт через UI невозможен, только программно.

**Уточнение:** ввод "0" первым символом блокируется (`variable == "0" && fictionalValue.isEmpty()`), но возможен ввод ".5" что даст 0.5 — это корректное поведение.

### 10. calculateNewDayOptions — numberOfDays <= 1 возвращает без установки LiveData
**CONFIRMED.** При `numberOfDays <= 1` метод возвращает `return` без установки LiveData. В MainActivity:1462 проверка `numberOfDays > 1` гарантирует, что EveryDayQuestion не будет показан при numberOfDays <= 1. Условия согласованы — **НЕ баг**. Однако backend-отчёт сам это признаёт ("Они согласованы"), и далее описывает сценарий с большим daysSinceLastDate — **это реальная проблема** при длительном неиспользовании приложения.

### 11. calculateNewDayOptions:64 — `.toInt()` приведение
**CONFIRMED.** `(firstOption * daysSinceLastDate).toInt()` теряет дробную часть. Это реально для случаев `firstOption = 333.33, daysSinceLastDate = 3` → потеря 0.99. При этом `.toInt()` на Double вне диапазона Int даёт непредсказуемый результат (JVM spec). Severity: medium для потери копеек, low для overflow (нереалистичный сценарий).

### 12. calculateNewDayOptions:58 — отрицательный firstOption
**CONFIRMED.** Если `currentKeyTodayLimit > howMany`, `firstOption` отрицательный. EveryDayQuestion покажет отрицательные значения. Это логически корректно (перерасход бюджета), но UI не предупреждает пользователя.

### 13. roundMoney — overflow при больших числах
**CONFIRMED.** `Math.round(value * 100.0)` вернёт `Long.MAX_VALUE` при `value > 9.2 * 10^16`. Для бюджетного приложения нереалистично. Severity: negligible.

### 14. MainActivity:284-285 — `.toInt()` усечение среднесуточного значения
**CONFIRMED.** Строки 284-285 (`todayLimit += avarageDailyValue.toInt()`) и строка 1089 (`todayLimit += (avarageDailyValue).toInt()`) — обе усекают дробную часть. При бюджете 10000 на 30 дней: `avarageDailyValue = 333.33`, `todayLimit = 333`. За 30 дней потеря: 0.33 * 30 = 9.9 единиц. **Severity: medium** — реальная потеря денег пользователя.

### 15. EveryDayQuestion:61 — "race condition" с LiveData
**FALSE POSITIVE (в части timing).** Как описано выше, LiveData.observe() диспатчит значение синхронно при активном LifecycleOwner. К строке 61 локальная переменная уже содержит корректное значение. Однако дизайн кода хрупкий — если кто-то переместит строку 61 перед observe или сменит lifecycle owner.

### 16. DataModel:65 — keyTodayLimitSecondOption игнорирует daysSinceLastDate
**CONFIRMED.** `keyTodayLimitSecondOption.value = secondOption` (просто среднесуточное без учёта пропущенных дней). `keyTodayLimitFirstOption` учитывает: `currentKeyTodayLimit + (firstOption * daysSinceLastDate).toInt()`. Это разная логика двух опций: option1 = "унести остаток с прошлых дней", option2 = "начать с чистого среднего". Может быть by design, но пользователю это не объясняется.

### 17. SharedPreferences — непоследовательные типы (getStringOrFloat)
**CONFIRMED.** Есть `getStringOrFloat()` в MainActivity и `getDouble()` в FirestoreSync — обе решают одну задачу с ClassCastException guard. Технический долг, не баг.

### 18. SharedPreferences — неатомарность streak vs budget
**CONFIRMED.** Streak сохраняется отдельным `.apply()` (строки 1426-1436), а бюджет позже в `saveData()` → `editor.apply()`. При crash между ними streak обновится, бюджет нет. Severity: low — минорная потеря консистентности.

### 19. FirestoreSync — два отдельных вызова без транзакции
**CONFIRMED.** `syncLocalToFirestore()` (строки 57-65): `userDoc.set(...)` и `userDoc.update(...)` — fire-and-forget без `addOnFailureListener`. Частичная синхронизация возможна. Severity: medium.

### 20. Firestore — все write без обработки ошибок
**CONFIRMED.** `saveBudgetToFirestore`, `createProfile`, `updateSubscriptionStatus` — все `.set()` без failure listener. Severity: medium.

### 21. STRING_KEY сохраняет значение из TextView
**CONFIRMED.** Строки 1333-1334: `val result: String = binding.result.text.toString()` → `editor.putString("STRING_KEY", result)`. Во время preview ввода (updatePreview, строки 835-856) `result.text` содержит не todayLimit, а вычисленное preview-значение. Если пользователь начал ввод числа, видит preview и свернул приложение (что вызывает onPause → saveData), в SharedPreferences запишется preview-значение, а не реальный todayLimit.

**Однако**, при `onPause()` (строки 1305-1315) код не сбрасывает `fictionalValue` и не восстанавливает `result.text` к todayLimit перед saveData. **Severity: high** — это реальный баг с потерей данных.

**Уточнение:** На самом деле, `hasUnsavedChanges` проверяется в saveData. hasUnsavedChanges устанавливается в true при: observe money, observe dateFull, observe keyTodayLimit, observe todayLimit, и после нажатия Enter. Простой ввод цифр без нажатия Enter НЕ устанавливает hasUnsavedChanges. Поэтому если пользователь просто набирает число и сворачивает — `hasUnsavedChanges` = false (если не было других изменений), и STRING_KEY не перезаписывается. **Severity понижается до medium** — баг проявляется только если hasUnsavedChanges был true ДО начала ввода (например, пользователь потратил деньги, начал вводить новую сумму, свернул).

### 22. DataConflictFragment — recreate() может вызвать двойной диалог
**CONFIRMED.** Строка 77-79: `main.recreate()` перезапускает Activity, вызывает `loadData()`, который при `today != lastDate` покажет EveryDayQuestion. Нет флага "только что применили облачные данные". Severity: medium — возможен повторный показ EveryDayQuestion после выбора облачных данных.

### 23. Unused зависимости (Compose, navigation, preference — 8 штук)
**CONFIRMED.** Все 8 зависимостей подтверждены как неиспользуемые:
- 6 Compose зависимостей (ui, tooling, tooling-preview, foundation, material, activity-compose)
- navigation-fragment-ktx (навигация вручную через supportFragmentManager)
- preference (нет PreferenceFragmentCompat)

### 24. Дублирование DatePicker в MainActivity vs DatePickerFragment
**CONFIRMED.** MainActivity:383-410 использует системный DatePickerDialog, DatePickerFragment — RecyclerView. DatePickerFragment недостижим из живого UI (открывается только из мёртвого SettingsFragment), поэтому дублирование не опасно — просто мёртвый код.

### 25. Дублирование roundMoney
**CONFIRMED.** HistoryAdapter:131 — `Math.round(newTotal * 100.0) / 100.0` вместо вызова `dataModel.roundMoney()`. SettingsFragment:38 — аналогично.

### 26. Дублирование getStringOrFloat / getDouble
**CONFIRMED.** Идентичная логика в двух местах.

### 27. RuStoreBillingManager — placeholder CONSOLE_APP_ID
**CONFIRMED.** `CONSOLE_APP_ID = "YOUR_RUSTORE_CONSOLE_APP_ID"` — не заменён. Все операции с RuStore упадут.

### 28. RuStoreBillingManager — пустой addOnFailureListener в launchPurchaseFlow
**CONFIRMED.** Строка 43-44: пустой `addOnFailureListener {}`, callback не вызывается. Пользователь не получит ни результата, ни ошибки.

### 29. SubscriptionsListFragment:98 — хрупкий паттерн удаления по индексу
**CONFIRMED.** Backend-отчёт говорит "безопасно т.к. после удаления вызывается buildList()". Однако как показано в верификации frontend-отчёта, есть ДВОЙНОЕ УДАЛЕНИЕ: в самом фрагменте (строка 98) и в callback MainActivity (строка 494).

### 30. BillingManager не закрывается в PremiumManager
**CONFIRMED.** `checkGooglePlay()` создаёт `BillingManager`, подключается, но не вызывает `disconnect()` после завершения. BillingClient остаётся подключённым до GC.

### 31. SettingsFragment — недостижим из UI
**CONFIRMED.** Grep подтверждает: `SettingsFragment.newInstance()` — только в companion object. Нигде не вызывается из живого кода.

### 32. DatePickerFragment — недостижим из UI
**CONFIRMED.** Открывается только из мёртвого SettingsFragment.

---

## Пропущенные проблемы (новые находки)

### QA-1. ДВОЙНОЕ УДАЛЕНИЕ подписки
**Файл:** `SubscriptionsListFragment.kt:97-101` + `MainActivity.kt:493-496`
**Severity: high**
При нажатии кнопки удаления подписки:
1. `SubscriptionsListFragment:98` — `subscriptions.removeAt(i)` удаляет из своей локальной копии
2. `SubscriptionsListFragment:100` — `onDelete?.invoke(i)` вызывает callback в MainActivity
3. `MainActivity:494` — `subscriptions.removeAt(index)` удаляет ВТОРОЙ элемент (или кидает IndexOutOfBoundsException если последний элемент)

Это приводит к удалению ДВУХ подписок вместо одной, или к крашу при удалении последней подписки.

**Шаги воспроизведения:** Создать 2+ подписки, удалить первую → удалятся сразу две.

### QA-2. `fictionalValue.toDouble()` без safe-check
**Файл:** `MainActivity.kt:976`
**Severity: medium**
`fictionalValue.toDouble()` — unsafe. Хотя условие на строке 975 проверяет `fictionalValue.isNotEmpty()` и `value.text != "."`, теоретически `fictionalValue` может содержать некорректную строку. В строке 824 используется безопасный `toDoubleOrNull()`, но в строке 976 — нет. Если каким-то образом `fictionalValue` содержит невалидную строку, будет NumberFormatException.

### QA-3. `WRITE_EXTERNAL_STORAGE` permission не нужен
**Файл:** `AndroidManifest.xml:4`
**Severity: low**
Приложение запрашивает `WRITE_EXTERNAL_STORAGE`, но нигде не использует внешнее хранилище. Все данные в SharedPreferences.

### QA-4. `onPause()` не восстанавливает UI перед сохранением
**Файл:** `MainActivity.kt:1305-1315`
**Severity: medium**
В `onPause()`:
1. `isBudgetInputMode = false` и `budgetInputValue = ""` сбрасываются
2. `settingsPopup?.dismiss()` закрывает popup
3. Но `fictionalValue` НЕ сбрасывается
4. `result.text` может содержать preview-значение (не todayLimit)
5. `saveData()` сохраняет `binding.result.text` как STRING_KEY

Если `hasUnsavedChanges == true` и пользователь набрал число но не нажал Enter, в SharedPreferences попадёт preview-значение.

### QA-5. Нет обработки CoroutineScope leaks
**Файл:** `MainActivity.kt:198`, `AuthFragment.kt:37,143`, `DataConflictFragment.kt:53`
**Severity: medium**
`CoroutineScope(Dispatchers.Main).launch { ... }` создаёт standalone scope, привязанный не к lifecycle. Если Activity/Fragment уничтожится до завершения корутины:
- В AuthFragment: `onAuthSuccess` может вызваться после уничтожения фрагмента
- В DataConflictFragment: `cloudData` может быть установлен после уничтожения View
- В MainActivity: `dataModel.isPremium.value` может быть установлен после onDestroy

Нужно использовать `lifecycleScope` или `viewLifecycleOwner.lifecycleScope`.

### QA-6. Emoji с запятой ломает сохранение/загрузку
**Файл:** `MainActivity.kt:1328-1331, 1396-1414`
**Severity: medium**
Категории сохраняются через `joinToString(",")` и загружаются через `split(",")`. Если пользователь через системную клавиатуру введёт эмодзи-последовательность, содержащую запятую (маловероятно для эмодзи, но возможно если пользователь введёт текст вместо эмодзи через кнопку клавиатуры), данные будут повреждены при загрузке.

### QA-7. `SYNCED_TO_FIRESTORE` никогда не сбрасывается при sign out
**Файл:** `AuthFragment.kt:242-247`
**Severity: medium**
При sign out вызывается `AuthManager.signOut()`, но `SYNCED_TO_FIRESTORE` остаётся `true` в SharedPreferences. Если другой пользователь войдёт в аккаунт на том же устройстве, флаг будет `true`, и DataConflictFragment не покажется — локальные данные предыдущего пользователя молча перезапишут облачные данные нового.

### QA-8. `recreate()` в DataConflictFragment без dismiss
**Файл:** `DataConflictFragment.kt:77-79`
**Severity: medium**
`main.recreate()` вызывается после `parentFragmentManager.popBackStack()`. `popBackStack()` — асинхронный, а `recreate()` — синхронный. `recreate()` может выполниться ДО того, как фрагмент будет реально убран из backstack, что может привести к утечке или крашу при попытке доступа к уничтоженному View.

### QA-9. `dateFull` инициализирован как `now().minusDays(1)` — невалидная начальная дата
**Файл:** `MainActivity.kt:77`
**Severity: low**
`dateFull = LocalDate.now().minusDays(1)` — при первом запуске (до установки бюджета) `numberOfDays = ChronoUnit.DAYS.between(today, dateFull)` даст -1. Это обрабатывается проверками `numberOfDays > 0`, но семантически confusing.

### QA-10. `subscriptions` — мутабельный список разделяется между Activity и Fragment
**Файл:** `MainActivity.kt:90, 492-496` + `SubscriptionsListFragment.kt:15,37`
**Severity: medium**
`SubscriptionsListFragment.subscriptions` загружает свою копию из `SubscriptionManager.loadSubscriptions()`. MainActivity имеет свой `subscriptions`. Это два РАЗНЫХ списка. Удаление в одном не отражается в другом, кроме как через сохранение/загрузку. Вместе с QA-1 (двойное удаление) это создаёт дополнительную путаницу.

### QA-11. History: удаление записи не учитывает подписки
**Файл:** `History.kt:140-177`
**Severity: low**
При удалении записи, созданной автосписанием подписки, деньги возвращаются в бюджет, но подписка остаётся активной и спишет снова при следующем запуске приложения.

### QA-12. GoogleSignIn deprecated API
**Файл:** `AuthManager.kt:4-6, 64-69`, `AuthFragment.kt:17,35`
**Severity: medium**
`GoogleSignIn`, `GoogleSignInOptions`, `GoogleSignInClient` — deprecated (подтверждено компилятором). Google рекомендует перейти на Credential Manager API (`androidx.credentials`). На данный момент работает, но может быть удалено в будущих версиях play-services-auth.

---

## Edge Cases бизнес-логики

### DataModel.spend(amount, currentTodayLimit, currentBudget)

| Input | Ожидание | Реальность | Вердикт |
|-------|----------|------------|---------|
| amount = 0 | Нет эффекта | newTodayLimit = currentTodayLimit, newBudget = currentBudget. Запись 0.0 в историю | Работает, но бессмысленная запись. UI не пропускает 0 (кнопка "0" заблокирована первым символом) |
| amount < 0 (e.g. -100) | Ошибка/блокировка | todayLimit увеличивается, howMany увеличивается | UI не позволяет, нет кнопки "-" |
| amount = NaN | Ошибка | roundMoney(NaN) = NaN (Math.round(NaN) = 0L, 0L / 100.0 = 0.0). todayLimit и budget = 0.0 | roundMoney(NaN) на самом деле: NaN * 100.0 = NaN, Math.round(NaN) = 0, 0 / 100.0 = 0.0. Результат: обнуление бюджета! |
| amount = Infinity | Ошибка | roundMoney(Infinity) — Math.round(Infinity) = Long.MAX_VALUE. Деление на 100.0 = 9.223E16 | Некорректный результат |
| amount = Double.MAX_VALUE | Ошибка | overflow в умножении | Некорректный результат |

**Вывод:** UI защищает от невалидных значений. Программный вызов с NaN обнулит бюджет.

### DataModel.calculateDailyAverage(totalBudget, days)

| Input | Ожидание | Реальность | Вердикт |
|-------|----------|------------|---------|
| days = 0 | 0 или ошибка | return 0.0 | OK, корректный guard |
| days = 1 | totalBudget | totalBudget / 1 = totalBudget | OK |
| days < 0 | 0 или ошибка | return 0.0 (guard `days <= 0`) | OK |
| money = 0, days > 0 | 0 | 0.0 / days = 0.0 | OK |
| money < 0, days > 0 | отрицательный | отрицательный | OK, но UI не предупреждает |

### DataModel.calculateNewDayOptions(...)

| Сценарий | Результат | Проблема |
|----------|-----------|----------|
| numberOfDays = 1 | return без установки LiveData | EveryDayQuestion не покажется (guard в MainActivity), OK |
| numberOfDays = 2, howMany=1000, currentKey=400, daysSince=1 | firstOption=(1000-400)/1=600, secondOption=1000/1=1000. keyTodayLimitFirst=400+(600*1).toInt()=1000. keyTodayLimitSecond=1000 | OK |
| daysSinceLastDate = 30 (долгое отсутствие), numberOfDays = 10 | firstOption = (howMany-current)/9. keyTodayLimitFirst = current + (firstOption*30).toInt() — может быть >> howMany | Нет ограничения сверху. keyTodayLimitFirst может быть больше общего бюджета |
| lastDate в будущем (часовой пояс / ручная смена даты) | daysSinceLastDate < 0. keyTodayLimitFirst = current + (negative).toInt() | Может стать отрицательным |

### DataModel.roundMoney(value)

| Input | Ожидание | Реальность |
|-------|----------|------------|
| -1.555 | -1.56 | Math.round(-1.555 * 100.0) = Math.round(-155.5) = -155. -155 / 100.0 = -1.55 | **НЕ -1.56!** Math.round использует "round half up" для положительных, но для отрицательных округляет к нулю. -155.5 → -155 (не -156). Это банковское "round half to even"? Нет, это Java Math.round = floor(x + 0.5). floor(-155.5 + 0.5) = floor(-155.0) = -155 |
| 2.675 | 2.68 | 2.675 * 100.0 = 267.49999999999997 (IEEE 754!). Math.round(267.5) = 268, но Math.round(267.49999...) = 267. Результат: 2.67! | **Ошибка IEEE 754** — классическая проблема floating point |
| 0.1 + 0.2 | 0.3 | roundMoney(0.30000000000000004) → Math.round(30.000000000000004) = 30 → 0.3 | OK, roundMoney маскирует эту ошибку |

---

## Lifecycle проблемы

### 1. Поворот экрана
`MainActivity` использует `FragmentActivity` без `android:configChanges` в manifest. При повороте:
- Activity уничтожается и пересоздаётся
- `DataModel` (ViewModel) переживает конфигурацию — OK
- Но `todayLimit`, `howMany`, `avarageDailyValue`, `numberOfDays` и другие поля хранятся как свойства Activity (не в ViewModel) — **ТЕРЯЮТСЯ при повороте**
- `onPause()` → `saveData()` сохранит их в SharedPreferences
- `onCreate()` → `loadData()` восстановит из SharedPreferences
- Но `fictionalValue` (текущий ввод) будет потерян, как и `isAddMode`, `activeCategory`, `selectedEmojis` (последние три восстанавливаются из SharedPreferences только для emojis)
- **Severity: medium** — потеря текущего ввода при повороте

### 2. Process death
- `onPause()` вызывается, `saveData()` срабатывает
- Данные из SharedPreferences восстановятся в `loadData()`
- Но: `lastSpendAmount`, `lastIncomeAmount` (undo) теряются — undo невозможен после process death. **Acceptable.**
- `fictionalValue` теряется. **Acceptable.**

### 3. Fragment back stack
- Все фрагменты добавляются через `replace + addToBackStack`
- Физическая кнопка "Назад" корректно закрывает фрагменты через `onBackPressed` → `popBackStack`
- **Исключение:** `EveryDayQuestion` перехватывает кнопку "Назад" через `OnBackPressedCallback` — корректно, выбор обязателен

### 4. Observer accumulation
При переходе: MainActivity → Fragment (replace) → назад (popBackStack):
- Fragment View уничтожается, Fragment остаётся в памяти
- При повторном показе: `onCreateView` + `onViewCreated` вызываются заново
- Observer-ы с `activity as LifecycleOwner` НЕ отписываются (activity жива)
- Каждый показ фрагмента добавляет новый observer
- **Severity: medium** — при многократном открытии/закрытии фрагмента observer-ы накапливаются

---

## Навигация

### Достижимость фрагментов из UI

| Фрагмент | Достижим? | Как |
|----------|-----------|-----|
| History | Да | binding.history click |
| EveryDayQuestion | Да | loadData() при today != lastDate |
| StreakFragment | Да | binding.streakButton click |
| AboutFragment | Да | popup menu → "О проекте" |
| AuthFragment | Да | popup menu → "Аккаунт" |
| DataConflictFragment | Да | после первого входа при конфликте данных |
| SubscriptionsListFragment | Да | repeat button popup → "Текущие подписки" |
| BudgetInputFragment | Нет | Только из мёртвого SettingsFragment |
| DatePickerFragment | Нет | Только из мёртвого SettingsFragment |
| SettingsFragment | Нет | Нигде не вызывается |
| SubscriptionFragment | Нет | Нигде не вызывается |

### Dead-end фрагменты
- Все достижимые фрагменты имеют кнопку закрытия (clickableBackground → popBackStack) или анимацию dismiss
- EveryDayQuestion блокирует "Назад" — корректно (обязательный выбор)
- DataConflictFragment блокирует фоновый клик — корректно (обязательный выбор), но НЕ блокирует кнопку "Назад" — пользователь может нажать системную кнопку назад и уйти без выбора

### Физическая кнопка "Назад"
Работает корректно для всех фрагментов кроме EveryDayQuestion (заблокирована) и DataConflictFragment (НЕ заблокирована — баг, пользователь может избежать выбора).

---

## Интеграция

### Firebase Auth без интернета
`AuthManager.signInWithEmail/registerWithEmail` — suspend-функции, используют `await()`. При отсутствии интернета Firebase SDK бросит exception, который ловится в `AuthFragment:155-157` с показом ошибки. **OK.**

`AuthManager.signInWithGoogle` — аналогично, exception ловится в строке 43-45. **OK.**

### Google Play Billing недоступен
`BillingManager.connect()` — callback `onConnected(false)` при ошибке. `SubscriptionFragment:94` — `if (!connected) return@connect` — цена не загрузится, кнопка останется с "Загрузка цены...". **Нет сообщения об ошибке пользователю.** Severity: low.

### Deep link quarter://payment
`AndroidManifest.xml:25-30` — intent-filter настроен. `handleDeepLink()` (строки 1293-1303) проверяет scheme и host, перепроверяет Premium. **OK.**

### minSdk 26 совместимость
Проверены все API:
- `LocalDate`, `ChronoUnit`, `DateTimeFormatter` — API 26+, OK
- `startDragAndDrop()` — API 24+, OK
- `View.DragShadowBuilder` — API 11+, OK
- `PackageManager.getInstallSourceInfo()` — API 30, но обёрнут в `Build.VERSION.SDK_INT >= R` check с fallback на deprecated `getInstallerPackageName()`, OK
- `BillingClient.enablePendingPurchases()` — deprecated, но работает на всех версиях
- **Нет проблем совместимости с API 26.**

---

## Итоговый приоритизированный список

### Critical
1. **QA-1. Двойное удаление подписки** — `SubscriptionsListFragment:98` + `MainActivity:494`. Удаляют элемент из двух разных списков по одному индексу. Может вызвать IndexOutOfBoundsException или удаление двух подписок вместо одной.

### High
2. **STRING_KEY сохраняет preview-значение** (backend-report #21) — при сворачивании приложения во время ввода суммы, если hasUnsavedChanges=true, в SharedPreferences попадёт вычисленное preview-значение вместо реального todayLimit. CONFIRMED.
3. **`.toInt()` усечение среднесуточного значения** (backend-report #14) — потеря до 30 единиц валюты за период. CONFIRMED.

### Medium
4. **`activity as LifecycleOwner` вместо `viewLifecycleOwner`** (frontend-report #9) — накопление observer-ов при повторном показе фрагментов. CONFIRMED.
5. **QA-5. CoroutineScope leaks** — standalone scope не привязан к lifecycle. Возможен краш при доступе к уничтоженным View.
6. **QA-7. SYNCED_TO_FIRESTORE не сбрасывается при sign out** — конфликт данных при смене аккаунта.
7. **QA-8. recreate() после popBackStack()** — неопределённый порядок операций.
8. **Firestore write без обработки ошибок** (backend-report #19, #20) — тихая потеря данных. CONFIRMED.
9. **DataConflictFragment recreate двойной диалог** (backend-report #22) — CONFIRMED.
10. **QA-10. Рассинхронизация списков подписок** между Activity и Fragment.
11. **QA-4. onPause не восстанавливает UI перед saveData** — preview value может попасть в SharedPreferences.
12. **DataConflictFragment: кнопка "Назад" не заблокирована** — пользователь может уйти без выбора.
13. **QA-12. GoogleSignIn deprecated** — работает сейчас, но может сломаться в будущих версиях.
14. **RuStore placeholder CONSOLE_APP_ID** (backend-report #27) — все RuStore операции нерабочие. CONFIRMED.
15. **QA-6. Запятая в emoji ломает сохранение** — маловероятно, но возможно при ручном вводе текста.
16. **calculateNewDayOptions: нет верхнего ограничения при большом daysSinceLastDate** — keyTodayLimit может превысить общий бюджет. CONFIRMED.
17. **Потеря состояния при повороте экрана** (fictionalValue, isAddMode).

### Low
18. **Binding не обнуляется в onDestroyView** (frontend-report #8) — CONFIRMED. Потенциальная утечка, но маловероятная в данной архитектуре.
19. **roundMoney для отрицательных чисел** — округление -1.555 к -1.55 вместо -1.56. Мелкая неточность.
20. **roundMoney IEEE 754** — 2.675 → 2.67 вместо 2.68. Классическая проблема floating point, исправляется переходом на BigDecimal.
21. **Hardcoded строки в layouts** (frontend-report #5) — CONFIRMED. Блокирует локализацию.
22. **Неиспользуемые drawables** (~20 файлов) — CONFIRMED. Мусор.
23. **Неиспользуемые зависимости** (8 штук) — CONFIRMED. Увеличивают размер APK.
24. **Мёртвые файлы** (Settings.kt, SettingsFragment.kt, DatePickerFragment.kt, BudgetInputFragment.kt, MyApplicationTheme.kt) — CONFIRMED.
25. **Мёртвый код** (getFirebaseErrorMessage, PremiumGate.withPremium, dayLimit, isLoggedIn, userName) — CONFIRMED.
26. **Нет contentDescription** (frontend-report #6) — CONFIRMED. Accessibility.
27. **WRITE_EXTERNAL_STORAGE** не используется — CONFIRMED.
28. **RuStore launchPurchaseFlow пустой failure handler** — CONFIRMED.
29. **BillingManager не disconnect в PremiumManager** — CONFIRMED.

### Verified OK
- `calculateDailyAverage` guard clause для days <= 0
- Deep link обработка
- minSdk 26 совместимость — нет проблем
- EveryDayQuestion блокировка кнопки "Назад"
- SharedPreferences атомарность (один `editor.apply()` для основных данных)
- HistoryManager — `removeCurrentEntry` корректно работает с индексами
- StoreDetector — правильный VERSION check для getInstallSourceInfo vs getInstallerPackageName
- EveryDayQuestion:61 "race condition" — FALSE POSITIVE (LiveData dispatches synchronously)
