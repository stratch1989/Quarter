# Frontend Report

## Неиспользуемые ресурсы

### Drawables — мёртвые файлы (ни одной ссылки в XML или Kotlin)

- `drawable/back.jpg` — нет ни одной ссылки
- `drawable/back_background.png` — нет ни одной ссылки
- `drawable/backgr.png` — нет ни одной ссылки
- `drawable/button_border.xml` — нет ни одной ссылки (используются только button_border2…7)
- `drawable/button_border2.xml` — нет ни одной ссылки в XML/Kotlin (только self-comment внутри файла)
- `drawable/button_border3.xml` — нет ни одной ссылки в XML/Kotlin
- `drawable/close_button.png` — нет ни одной ссылки
- `drawable/dark_grey_button.png` — нет ни одной ссылки (используется только dark_grey_button_inset)
- `drawable/grey_button.png` — нет ни одной ссылки
- `drawable/orange_button.png` — нет ни одной ссылки (используется только orange_button_inset)
- `drawable/enter_button.png` — нет ни одной ссылки (используется только enter_button_inset)
- `drawable/ic_globe.xml` — нет ни одной ссылки
- `drawable/ic_plus_minus.xml` — нет ни одной ссылки
- `drawable/circle_button_close_bg.xml` — нет ни одной ссылки
- `drawable/circle_button_delete_bg.xml` — нет ни одной ссылки
- `drawable/delete_circle.xml` — нет ни одной ссылки
- `drawable/popup_bg.xml` — нет ни одной ссылки (используется только history_popup_bg)
- `drawable/png.png` — нет ни одной ссылки
- `drawable/settings_window.png` — нет ни одной ссылки
- `drawable/settings_window_new.png` — нет ни одной ссылки
- `drawable/window_settings.png` — нет ни одной ссылки
- `drawable/rounded_corners.xml` — используется в fragment_budget_input.xml и fragment_date_picker.xml. ИСПОЛЬЗУЕТСЯ (оставить)

### Layouts — мёртвые файлы

- `layout/test.xml` — пустой RelativeLayout, нигде не inflate-ится. Очевидно тестовый файл-заглушка.
- `layout/activity_settings.xml` — inflate-ится в `Settings.kt`, но `Settings` — это мёртвая Activity (см. раздел «Неиспользуемые файлы»). По факту никогда не показывается пользователю.

### Strings — мёртвые записи (все строки в strings.xml)

Все 10 записей в `values/strings.xml` — шаблонный boilerplate от Android Studio, нигде не используются в коде или layouts:
- `messages_header`, `sync_header`, `signature_title`, `reply_title` — используются только в `xml/root_preferences.xml` (который сам нигде не загружается)
- `sync_title`, `attachment_title`, `attachment_summary_on`, `attachment_summary_off` — то же самое
- `dummy_button`, `dummy_content`, `hello_blank_fragment` — очевидные placeholder-ы, нигде не используются

### Colors — потенциально мёртвые записи

- `light_blue_600`, `light_blue_900`, `light_blue_A200`, `light_blue_A400` — используются только в стилях `ThemeOverlay.Quarter.FullscreenContainer` (values/themes.xml и values-night/themes.xml), но сам этот стиль нигде не применяется в коде или layouts проекта. Де-факто мёртвые цвета.
- `black_overlay` — используется только в `Widget.AppTheme.ButtonBar.Fullscreen` (styles.xml), который тоже нигде не применяется. Мёртвый цвет.

### Прочее

- `values/attrs.xml` — декларирует `FullscreenAttrs` с атрибутами `fullscreenBackgroundColor` / `fullscreenTextColor`. Нигде не используется в Java/Kotlin коде.
- `values/styles.xml` — стиль `Widget.AppTheme.ButtonBar.Fullscreen` нигде не применяется.
- `values/themes.xml` и `values-night/themes.xml` — стиль `ThemeOverlay.Quarter.FullscreenContainer` нигде не применяется. `DarkDatePickerTheme` используется.
- `xml/root_preferences.xml` — файл настроек PreferenceScreen, нигде не загружается (нет `R.xml.root_preferences` в Java/Kotlin).

---

## Проблемы в layouts

### Hardcoded строки (текст прямо в XML вместо @string/)

- `activity_main.xml:47` — `android:text="7"` (и все цифры numpad: 8,9,4,5,6,1,2,3,0,".",«↵»,«⁺∕-»)
- `activity_main.xml:449` — `android:text="/день"` — hardcoded русский текст
- `activity_main.xml:463` — `android:text="Введите дату и сумму в настройках"` — hardcoded строка
- `fragment_budget_input.xml:57` — `android:text="Бюджет не задан"` — hardcoded
- `fragment_budget_input.xml:117` — `android:text="Выберите дату"` — hardcoded
- `fragment_budget_input.xml:279` — `android:text="Сохранить"` — hardcoded
- `fragment_date_picker.xml:68` — `android:text="Сохранить"` — hardcoded
- `fragment_every_day_question.xml:45` — `android:text="Вау!"` — hardcoded
- `fragment_every_day_question.xml:60` — `android:text="Вчера вы сэкономили 1059 ₽"` — hardcoded placeholder с конкретной суммой (будет перезаписан программно, но в редакторе вводит в заблуждение)
- `fragment_every_day_question.xml:68` — `android:text="Осталось решить, что делать\nс деньгами"` — hardcoded
- `fragment_every_day_question.xml:88` — `android:text="Потратить всё сегодня"` — hardcoded
- `fragment_every_day_question.xml:109` — `android:text="Увеличить дневной бюджет"` — hardcoded
- `fragment_history.xml:34` — `android:text="История трат"` — hardcoded (будет перезаписан, но является placeholder)
- `fragment_history.xml:62` — `android:text="Траты за текущий период\nпоявятся здесь"` — hardcoded
- `fragment_history.xml:108` — `android:text="☰"`, `android:text="📊"`, `android:text="◎"` — icon symbols
- `fragment_history.xml:157,174` — `android:text="−"`, `android:text="+"` — symbols
- `fragment_about.xml:27` — `android:text="Quarter"` — hardcoded название приложения
- `fragment_about.xml:36` — `android:text="Личный бюджет-трекер"` — hardcoded
- `fragment_about.xml:44` — `android:text="Версия 1.0"` — hardcoded версия (не синхронизирована с versionName из build.gradle)
- `fragment_streak.xml:36` — `android:text="🔥"` — emoji symbol в XML
- `fragment_streak.xml:47` — `android:text="1 день подряд!"` — hardcoded placeholder
- `fragment_streak.xml:61` — `android:text="Заходите каждый день,\nчтобы не потерять серию"` — hardcoded
- `fragment_auth.xml:27` — `android:text="Вход"` — hardcoded
- `fragment_auth.xml:97` — `android:text="Войти"` — hardcoded
- `fragment_auth.xml:106` — `android:text="Войти через Google"` — hardcoded
- `fragment_auth.xml:115` — `android:text="Нет аккаунта? Зарегистрироваться"` — hardcoded
- `fragment_auth.xml:141` — `android:text="Выйти из аккаунта"` — hardcoded
- `fragment_data_conflict.xml:27` — `android:text="Данные найдены в облаке"` — hardcoded
- `fragment_data_conflict.xml:35` — `android:text="В вашем аккаунте уже есть сохранённые данные..."` — hardcoded
- `fragment_data_conflict.xml:55` — `android:text="📱  Данные с устройства"` — hardcoded
- `fragment_data_conflict.xml:63` — `android:text="Бюджет: — ₽, дней: —"` — hardcoded placeholder
- `fragment_subscription.xml:28` — `android:text="⭐ Premium"` — hardcoded
- `fragment_subscription.xml:35` — `android:text="• Синхронизация между устройствами\n..."` — hardcoded многострочный текст
- `fragment_subscription.xml:44` — `android:text="Загрузка цены..."` — hardcoded
- `fragment_subscription.xml:66` — `android:text="Подписаться"` — hardcoded
- `fragment_subscription.xml:77` — `android:text="Другие способы оплаты"` — hardcoded
- `fragment_subscriptions_list.xml:27` — `android:text="Текущие подписки"` — hardcoded
- `fragment_subscriptions_list.xml:49` — `android:text="Нет активных подписок"` — hardcoded
- `popup_settings_menu.xml:24` — `android:text="💰  Укажите бюджет"` — hardcoded
- `popup_settings_menu.xml:54` — `android:text="📅  Дата"` — hardcoded
- `popup_settings_menu.xml:83` — `android:text="ℹ️  О проекте"` — hardcoded
- `popup_settings_menu.xml:112` — `android:text="👤  Аккаунт"` — hardcoded
- `popup_custom_interval.xml:14` — `android:text="Свой интервал"` — hardcoded
- `popup_custom_interval.xml:30` — `android:text="Повторять каждые"` — hardcoded
- `popup_custom_interval.xml:62` — `android:text="2"` — hardcoded начальное значение
- `popup_custom_interval.xml:106` — `android:text="дн."` — hardcoded
- `popup_custom_interval.xml:130` — `android:text="Готово"` — hardcoded
- `popup_subscription_menu.xml:26` — `android:text="✕  Отключить"` — hardcoded
- `list_item_history_divider.xml:18` — `android:text="Предыдущие периоды"` — hardcoded

**Рекомендация:** Вынести все пользовательские строки в `values/strings.xml`. Особенно критично для масштабируемости (добавление локализации).

### Отсутствие contentDescription у ImageView/ImageButton

- `fragment_settings.xml:45` — `<ImageView android:id="@+id/imageView" src="@drawable/ruble">` — нет contentDescription
- `fragment_settings.xml:96` — `<ImageView android:id="@+id/imageView1" src="@drawable/calendar">` — нет contentDescription
- `fragment_budget_input.xml:267` — `<ImageView android:id="@+id/save" src="@drawable/save_button">` — нет contentDescription
- `fragment_date_picker.xml:56` — `<ImageView android:id="@+id/save" src="@drawable/save_button">` — нет contentDescription
- `fragment_every_day_question.xml:33` — `<ImageView android:id="@+id/imageView2" src="@drawable/ico_for_quest">` — нет contentDescription
- `activity_settings.xml:10` — `<ImageView android:id="@+id/myImageView" src="@drawable/grey_image">` — нет contentDescription
- `activity_settings.xml:21` — `<ImageView android:id="@+id/cancel_button">` — нет contentDescription
- `activity_settings.xml:42` — `<ImageView android:id="@+id/save_button">` — нет contentDescription

**Рекомендация:** Добавить `android:contentDescription` на все ImageView/ImageButton. Для декоративных изображений использовать `android:contentDescription=""` или `android:importantForAccessibility="no"`.

### Deprecated атрибуты

- `activity_main.xml:256` — `android:textAppearance="@style/TextAppearance.AppCompat.Display4"` — устаревший стиль AppCompat, рекомендуется `TextAppearance.Material3.*`
- `activity_main.xml:358` — то же самое на TextView `@+id/value`
- `fragment_settings.xml:12` — `tools:context=".BlankFragment2"` — ссылка на несуществующий класс BlankFragment2 (переименован в SettingsFragment)
- `fragment_budget_input.xml:10` — `tools:context=".BlankFragment"` — ссылка на несуществующий класс BlankFragment
- `fragment_date_picker.xml:11` — `tools:context=".BlankFragment3"` — ссылка на несуществующий класс BlankFragment3
- `fragment_budget_input.xml:44` — `android:gravity="center|left"` — значение `left` устарело, используйте `center|start` для RTL-совместимости. Встречается в нескольких местах этого файла.
- `fragment_budget_input.xml:44,50,56,95,104` — `android:paddingLeft` вместо `android:paddingStart` — не RTL-совместимо
- `list_item_day.xml:8,9` — `android:paddingLeft` / `android:paddingRight` вместо `paddingStart`/`paddingEnd`
- `fragment_budget_input.xml` — `android:layout_width="96dp"` / `android:layout_height="96dp"` зафиксированы в XML, хотя в BudgetInputFragment.kt они программно пересчитываются. Это создаёт рассинхронизацию между XML и runtime.

### Прочие проблемы в layouts

- `fragment_settings.xml:125-144` — два пустых FrameLayout с id `frameAbout` и `frameXZ` без контента. `frameAbout` нигде не используется в SettingsFragment.kt. `frameXZ` тоже нигде не используется — это мёртвые заглушки.
- `activity_main.xml:419-430` — `<TextView android:id="@+id/streakButton">` используется как кнопка (`android:clickable="true"`, `android:focusable="true"`), но не является Button/ImageButton. Нет `android:role` или accessibility-аннотации.
- `activity_settings.xml` — полностью устаревший layout (используется мёртвой Activity Settings), ширина в dp зафиксирована: `android:layout_width="534dp"` — не адаптивно.
- `fragment_every_day_question.xml:76,97` — `<FrameLayout android:clickable="true">` без `android:focusable="true"` — неполная accessibility.
- `fragment_subscription.xml:35` — весь список фич (`•  Синхронизация…`) — единый hardcoded многострочный текст. Сложно поддерживать и локализовать.

---

## Проблемы во фрагментах

### Утечки View: binding не обнуляется в onDestroyView

Ни в одном из фрагментов, использующих ViewBinding, нет `onDestroyView()` с `binding = null` (кроме SubscriptionFragment, где обнуляются только billingManager).

Затронутые файлы:
- `SettingsFragment.kt` — `lateinit var binding: FragmentSettingsBinding` не обнуляется
- `BudgetInputFragment.kt` — `lateinit var binding: FragmentBudgetInputBinding` не обнуляется
- `DatePickerFragment.kt` — `lateinit var binding: FragmentDatePickerBinding` не обнуляется
- `EveryDayQuestion.kt` — `lateinit var binding: FragmentEveryDayQuestionBinding` не обнуляется
- `History.kt` — `lateinit var binding: FragmentHistoryBinding` не обнуляется
- `StreakFragment.kt` — `lateinit var binding: FragmentStreakBinding` не обнуляется

**Рекомендация:** Использовать backing field `_binding: T? = null` + `override fun onDestroyView() { _binding = null }`, доступ через `binding get() = _binding!!`. Это стандартный паттерн для предотвращения утечек памяти при использовании ViewBinding во фрагментах.

### Lifecycle: использование `activity as LifecycleOwner` вместо `viewLifecycleOwner`

Это критическая проблема. Во всех фрагментах LiveData наблюдается через `activity as LifecycleOwner`, а не через `viewLifecycleOwner`. При пересоздании View фрагмента (без пересоздания Activity) подписки накапливаются — каждый раз добавляется новый observer.

- `SettingsFragment.kt:36` — `dataModel.money.observe(activity as LifecycleOwner)`
- `SettingsFragment.kt:67` — `dataModel.dayText.observe(activity as LifecycleOwner)`
- `SettingsFragment.kt:81` — `dataModel.dateFull.observe(activity as LifecycleOwner)`
- `BudgetInputFragment.kt:31` — `dataModel.money.observe(activity as LifecycleOwner)`
- `DatePickerFragment.kt:43` — `dataModel.dayNumber.observe(activity as LifecycleOwner)`
- `DatePickerFragment.kt:68` — `dataModel.money.observe(activity as LifecycleOwner)`
- `EveryDayQuestion.kt:44` — `dataModel.keyTodayLimit.observe(activity as LifecycleOwner)`
- `EveryDayQuestion.kt:49` — `dataModel.keyTodayLimitFirstOption.observe(activity as LifecycleOwner)`
- `EveryDayQuestion.kt:53` — `dataModel.avarageDailyValueFirstOption.observe(activity as LifecycleOwner)`
- `EveryDayQuestion.kt:57` — `dataModel.avarageDailyValueSecondOption.observe(activity as LifecycleOwner)`
- `EveryDayQuestion.kt:63` — `dataModel.keyTodayLimitSecondOption.observe(activity as LifecycleOwner)`

**Рекомендация:** Заменить `activity as LifecycleOwner` на `viewLifecycleOwner` везде внутри `onViewCreated()`.

### Null-safety при работе с binding

- `SettingsFragment.kt:18` — `lateinit var binding` объявлен как `public` (`lateinit var`, не `private`). Если к binding обратятся снаружи после `onDestroyView()`, произойдёт `UninitializedPropertyAccessException`.
- Аналогично во всех других фрагментах — binding объявлен как `public lateinit var`.

**Рекомендация:** Сделать binding `private`.

### Прочие проблемы во фрагментах

- `SettingsFragment.kt:29-30` — переменные `howMany` и `daysText` инициализируются из пустых текстовых полей (`binding.howMany.text.toString()`), реальные значения всё равно придут через observer. Инициализация бессмысленна.
- `AuthFragment.kt:262-272` — приватный метод `getFirebaseErrorMessage()` объявлен, но нигде не вызывается внутри класса. Мёртвый код.
- `EveryDayQuestion.kt:61` — `dataModel.avarageDailyValue.value = avarageDailyValueSecondOption` вызывается до того, как observer для `avarageDailyValueSecondOption` успеет получить значение. В момент выполнения `avarageDailyValueSecondOption = 0.0`, что приводит к некорректной установке значения.
- `History.kt` — `binding` нигде не `private`, а весь класс достаточно объёмный. При `dismissWithAnimation()` используется `binding` внутри `withEndAction` — если фрагмент будет уничтожен до завершения анимации, возможен краш.
- `SubscriptionsListFragment.kt:98` — `subscriptions.removeAt(i)` внутри цикла по индексу с последующим `buildList()` — после удаления элемента индексы смещаются, и при удалении нескольких элементов подряд возможен `IndexOutOfBoundsException`. Безопаснее передавать объект, а не индекс.

---

## Неиспользуемые файлы

- `Settings.kt` + `activity_settings.xml` — класс `Settings : ComponentActivity` является полноценной Activity, зарегистрированной в AndroidManifest.xml. Однако нигде в проекте нет `Intent(…, Settings::class.java)` или явного запуска этой Activity из Kotlin-кода. `Settings.kt` сам открывает `MainActivity` по нажатию cancel, что указывает на то, что это остаток от старой архитектуры. Де-факто мёртвая Activity.

- `MyApplicationTheme.kt` — Compose-тема (`@Composable fun MyApplicationTheme`). Нигде не используется в проекте (проект использует Views, не Compose). Это boilerplate от Android Studio при создании Compose-проекта.

- `xml/root_preferences.xml` — файл PreferenceScreen, нигде не загружается (нет `R.xml.root_preferences` в коде).

---

## Общие замечания

1. **Крупнейшая структурная проблема** — использование `activity as LifecycleOwner` вместо `viewLifecycleOwner` во всех фрагментах с LiveData. Это потенциальная причина накопления observer-ов при каждом показе фрагмента и, как следствие, неожиданного многократного срабатывания колбэков.

2. **Отсутствие `onDestroyView()` с обнулением binding** — во всех 6 фрагментах с ViewBinding. Стандартная практика Android требует обнулять binding в `onDestroyView()`, чтобы не удерживать ссылку на View-иерархию после её уничтожения.

3. **Колоссальный технический долг по строкам** — весь пользовательский текст жёстко зашит в XML-layouts. `strings.xml` содержит только устаревший boilerplate. Ни одна строка из UI не вынесена в ресурсы.

4. **Мёртвый Compose-код** — `MyApplicationTheme.kt` (Compose) существует в проекте на Views. Compose зависимости в `build.gradle.kts` тоже, по всей видимости, подтягиваются впустую.

5. **Накопленный drawable-мусор** — ~20 drawable-файлов (PNG и XML) не используются. Среди них несколько дублирующих PNG-вариантов кнопок, которые были заменены inset-обёртками (`dark_grey_button_inset`, `orange_button_inset`, `enter_button_inset`).

6. **fragment_settings.xml** содержит два пустых FrameLayout (`frameAbout`, `frameXZ`) без содержимого — очевидные заглушки, не подключённые к логике фрагмента.

7. **Версия приложения в layout** — `fragment_about.xml:44` содержит `android:text="Версия 1.0"` жёстко зашитой строкой. При обновлении версии в `build.gradle.kts` этот текст не обновится автоматически.
