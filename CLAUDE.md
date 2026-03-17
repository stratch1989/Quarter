# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

This is a Gradle-based Android project (Kotlin DSL). Use the Gradle wrapper:

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew :Quarter:assembleDebug # Build only the app module
./gradlew clean                  # Clean build artifacts
```

No test suite exists. No linter is configured.

## Project Structure

- **`Quarter/`** - Android app module (the actual application)
- **`shared/`** - KMP shared module (unused by the app, scaffolding only)
- **`Quarter/Quarter/`** - iOS Xcode project (stub, not active)

The app module is at `Quarter/src/main/java/com/example/quarter/android/`. Two files (`DataModel.kt`, `DayAdapter.kt`) live in the default package at `Quarter/src/main/java/`.

## Architecture

**Android Views + ViewBinding** (NOT Compose, despite Compose dependencies in build.gradle). `FragmentActivity` host with fragment-based navigation.

### Key Classes

| Class | Role |
|---|---|
| `MainActivity` | Host activity. Owns the numpad UI, observes DataModel, saves state in `onPause()` |
| `DataModel` | Shared `ViewModel` with `MutableLiveData` fields + business logic methods (`spend()`, `calculateDailyAverage()`, `calculateNewDayOptions()`, `roundMoney()`) |
| `BudgetInputFragment` | Set total budget amount (numpad overlay). Layout: `fragment_budget_input.xml` |
| `SettingsFragment` | Settings/summary screen - shows budget and daily limit, links to BudgetInputFragment and DatePickerFragment. Layout: `fragment_settings.xml` |
| `DatePickerFragment` | Date picker - RecyclerView list of next 40 days via `DayAdapter`. Layout: `fragment_date_picker.xml` |
| `EveryDayQuestion` | Midnight carryover dialog - two options for recalculating daily limit |
| `History` | Placeholder history fragment (close button only) |
| `DayAdapter` | RecyclerView adapter for day selection, includes Russian day-word pluralization |

### Data Flow

1. User sets budget in `BudgetInputFragment` -> `DataModel.money`
2. User picks end date in `DatePickerFragment` -> `DataModel.dateFull`
3. `MainActivity` observes both, uses `DataModel.calculateDailyAverage()` for daily average
4. Spending via main numpad calls `DataModel.spend()` which updates `todayLimit` and `money`
5. State persists to `SharedPreferences` ("MyAppPrefs") in `onPause()`
6. On app start, if `today != lastDate`, `DataModel.calculateNewDayOptions()` computes carryover and `EveryDayQuestion` shows

### SharedPreferences Keys

`STRING_KEY` (today's limit as string), `HOW_MANY` (total budget as Double string), `NUMBER_OF_DAYS`, `AVARAGE_DAILY_VALUE`, `DATE_FULL`, `LAST_DATE`

## Conventions

- UI text and code comments are in **Russian**
- All fragments use `companion object { fun newInstance() }` factory pattern
- Fragments are loaded into `R.id.place_holder` FrameLayout via `supportFragmentManager`
- Budget math uses `Double` with `DataModel.roundMoney()` for rounding
- Button sizes are calculated dynamically from `displayMetrics.widthPixels`
- Min SDK 29 (Android 10), target SDK 33, compile SDK 34
- Kotlin 1.8.21, AGP 8.1.1
