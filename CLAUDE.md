# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Quarter is a Kotlin Multiplatform (KMP) personal budget/spending tracker app. The user sets a total budget and an end date, and the app calculates a daily spending limit, tracks expenses via a numpad interface, and carries over remaining budget across days.

The app is primarily developed for **Android** using Views + ViewBinding (not Compose for UI layout). The iOS target exists as a KMP scaffold but is not actively developed (just shows a greeting).

## Build Commands

```bash
# Build the Android app
./gradlew :Quarter:assembleDebug

# Build the shared KMP module
./gradlew :shared:build

# Run Android unit tests
./gradlew :Quarter:testDebugUnitTest

# Run shared module tests (common + Android)
./gradlew :shared:testDebugUnitTest

# Clean
./gradlew clean
```

## Architecture

### Gradle Modules

- **Root** (`build.gradle.kts`) - Plugin version declarations (AGP 8.1.1, Kotlin 1.8.21)
- **`:Quarter`** - Android application module (package: `com.example.quarter.android`, minSdk 29, targetSdk 33)
- **`:shared`** - KMP library module (package: `com.example.quarter`, targets: Android + iOS)

### Android App Structure (`Quarter/src/main/`)

The Android app uses **Fragments** for navigation (not Jetpack Navigation component) with a single `FragmentActivity` host:

- **`MainActivity`** - Main screen with numpad for expense entry. Uses a `Handler` polling loop (1s interval) to detect date changes at midnight. Persists state via `SharedPreferences` ("MyAppPrefs").
- **`BlankFragment2`** - Settings overlay showing current budget summary. Entry point to money and calendar sub-screens.
- **`BlankFragment`** - Money input fragment for setting total budget amount.
- **`BlankFragment3`** - Calendar/date picker fragment using `RecyclerView` to select the budget end date (up to 40 days out).
- **`EveryDayQuestion`** - Placeholder fragment (currently just a dismissible overlay).
- **`Settings`** - Separate activity for settings (minimal, mostly unused).
- **`DataModel`** - `ViewModel` with `MutableLiveData` fields shared across fragments via `activityViewModels()`. Central state holder for: `money`, `dateFull`, `dayLimit`, `todayLimit`, `lastDate`, `saveClick`, `dayNumber`, `dayText`.
- **`DayAdapter`** - `RecyclerView.Adapter` for the date selection list. Includes Russian day-pluralization logic.
- **`MyApplicationTheme`** - Jetpack Compose theme (defined but not used for main UI layout).

### Shared KMP Module (`shared/src/`)

Minimal KMP scaffold with `Platform` expect/actual interface and `Greeting` class. Not used by the Android app's core logic.

### Key Patterns

- **State sharing**: Fragments communicate with `MainActivity` through a shared `DataModel` ViewModel using `LiveData` observers.
- **Persistence**: `SharedPreferences` with keys `STRING_KEY` (today's limit), `HOW_MANY` (total budget), `NUMBER_OF_DAYS`, `DATE_FULL`.
- **UI sizing**: Button dimensions are calculated dynamically from `displayMetrics.widthPixels`.
- **Language**: UI text and code comments are in Russian.
- **ViewBinding**: All layouts use ViewBinding (`ActivityMainBinding`, `FragmentBlankBinding`, etc.).

### Layout Files (`Quarter/src/main/res/layout/`)

- `activity_main.xml` - Main screen with numpad and result display
- `fragment_blank.xml` - Money input overlay
- `fragment_blank2.xml` - Settings/summary overlay
- `fragment_blank3.xml` - Date picker with RecyclerView
- `fragment_every_day_question.xml` - Daily question overlay
- `activity_settings.xml` - Settings activity layout
- `list_item_day.xml` - RecyclerView item for date selection
