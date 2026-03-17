import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public class DataModel : ViewModel() {
    val money: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val dayText: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    val dayNumber: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }
    val dateFull: MutableLiveData<LocalDate> by lazy { MutableLiveData<LocalDate>() }
    val dayLimit: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val todayLimit: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val lastDate: MutableLiveData<LocalDate> by lazy { MutableLiveData<LocalDate>() }
    val saveClick: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    //два среднесуточных значения для выбора
    val avarageDailyValue: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val avarageDailyValueFirstOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val avarageDailyValueSecondOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }

    //два варианта для дневного лимита
    val keyTodayLimit: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val keyTodayLimitFirstOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }
    val keyTodayLimitSecondOption: MutableLiveData<Double> by lazy { MutableLiveData<Double>() }

    // --- Бизнес-логика ---

    fun roundMoney(value: Double): Double = Math.round(value * 100.0) / 100.0

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
        currentAvarageDailyValue: Double,
        numberOfDays: Long,
        daysSinceLastDate: Long
    ) {
        if (numberOfDays <= 1) return

        val firstOption = (howMany - currentKeyTodayLimit + currentAvarageDailyValue) / (numberOfDays - 1)
        avarageDailyValueFirstOption.value = firstOption

        val secondOption = howMany / (numberOfDays - 1).toDouble()
        avarageDailyValueSecondOption.value = secondOption

        keyTodayLimitFirstOption.value = currentKeyTodayLimit + (firstOption * daysSinceLastDate).toInt()
        keyTodayLimitSecondOption.value = secondOption
    }

    data class SpendResult(val newTodayLimit: Double, val newBudget: Double)
}
