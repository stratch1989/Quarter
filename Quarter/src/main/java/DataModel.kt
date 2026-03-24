import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public class DataModel : ViewModel() {
    val money: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val dayText: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val dayNumber: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val dateFull: MutableLiveData<LocalDate> by lazy { MutableLiveData<LocalDate>() }
    val todayLimit: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val lastDate: MutableLiveData<LocalDate> by lazy { MutableLiveData<LocalDate>() }
    val numberOfDays: MutableLiveData<Long> by lazy { MutableLiveData<Long>() }
    val saveClick: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
    val clearUndo: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    // Auth & Subscription
    val isPremium: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>(false) }

    //два среднесуточных значения для выбора
    val avarageDailyValue: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val avarageDailyValueFirstOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val avarageDailyValueSecondOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }

    //два варианта для дневного лимита
    val keyTodayLimit: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val keyTodayLimitFirstOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val keyTodayLimitSecondOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }

    // --- Бизнес-логика ---

    fun roundMoney(value: Double): Double {
        return java.math.BigDecimal(value.toString())
            .setScale(2, java.math.RoundingMode.HALF_UP)
            .toDouble()
    }

    fun calculateDailyAverage(totalBudget: Double, days: Long): Double {
        if (days <= 0) return 0.0
        return totalBudget / days
    }

    fun spend(amount: Double, currentTodayLimit: Double, currentBudget: Double): SpendResult {
        val newTodayLimit = roundMoney(currentTodayLimit - amount)
        val newBudget = roundMoney(currentBudget - amount)
        money.value = newBudget
        todayLimit.value = newTodayLimit
        return SpendResult(newTodayLimit, newBudget)
    }

    fun calculateNewDayOptions(
        howMany: Double,
        currentKeyTodayLimit: Double,
        numberOfDays: Long,
        daysSinceLastDate: Long
    ) {
        if (numberOfDays <= 1) return

        val firstOption = (howMany - currentKeyTodayLimit) / (numberOfDays - 1)
        avarageDailyValueFirstOption.value = firstOption

        val secondOption = howMany / (numberOfDays - 1).toDouble()
        avarageDailyValueSecondOption.value = secondOption

        keyTodayLimitFirstOption.value = roundMoney(currentKeyTodayLimit + firstOption * daysSinceLastDate)
        // Ограничить дневной лимит суммой оставшегося бюджета
        if (keyTodayLimitFirstOption.value!! > howMany) {
            keyTodayLimitFirstOption.value = howMany
        }

        keyTodayLimitSecondOption.value = secondOption
        if (keyTodayLimitSecondOption.value!! > howMany) {
            keyTodayLimitSecondOption.value = howMany
        }
    }

    data class SpendResult(val newTodayLimit: Double, val newBudget: Double)
}
