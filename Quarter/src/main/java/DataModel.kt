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
}