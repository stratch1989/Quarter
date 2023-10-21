import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

public class DataModel : ViewModel() {
    val money: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val dayText: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    val dayNumber: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val dateFull: MutableLiveData<LocalDate> by lazy {
        MutableLiveData<LocalDate>()
    }
    val dayLimit: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val todayLimit: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val lastDate: MutableLiveData<LocalDate> by lazy {
        MutableLiveData<LocalDate>()
    }
    val saveClick: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    //два среднесуточных значения для выбора
    val avarageDailyValue: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val avarageDailyValueFirstOption: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val avarageDailyValueSecondOption: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }

    //два варианта для дневного лимита
    val keyTodayLimit: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val keyTodayLimitFirstOption: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
    val keyTodayLimitSecondOption: MutableLiveData<Float> by lazy {
        MutableLiveData<Float>()
    }
}