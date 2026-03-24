package com.example.quarter.android.data

import DataModel
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.quarter.android.R
import com.example.quarter.android.auth.AuthManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Диалог выбора: использовать локальные данные или облачные.
 * Показывается при первом входе если в облаке уже есть данные.
 */
class DataConflictFragment : Fragment() {

    private val dataModel: DataModel by activityViewModels()
    private var cloudData: BudgetData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_data_conflict, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Блокируем кнопку назад — пользователь должен сделать выбор
        }

        val localInfo = view.findViewById<TextView>(R.id.local_info)
        val cloudInfo = view.findViewById<TextView>(R.id.cloud_info)

        // Показываем локальные данные
        val prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val localHowMany = try {
            (prefs.getString("HOW_MANY", "0") ?: "0").toDouble()
        } catch (_: Exception) { 0.0 }
        val localDays = prefs.getLong("NUMBER_OF_DAYS", 0L)
        localInfo.text = "Бюджет: ${localHowMany.toLong()}, дней: $localDays"

        // Загружаем облачные данные
        val uid = AuthManager.currentUser?.uid
        if (uid != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val data = FirestoreSync.loadBudgetFromFirestore(uid)
                cloudData = data
                if (data != null) {
                    cloudInfo.text = "Бюджет: ${data.howMany.toLong()}, дней: ${data.numberOfDays}"
                } else {
                    cloudInfo.text = "Нет данных"
                }
            }
        }

        // Использовать локальные данные → загрузить в Firestore
        view.findViewById<View>(R.id.btn_use_local).setOnClickListener {
            val currentUid = AuthManager.currentUser?.uid ?: return@setOnClickListener
            FirestoreSync.syncLocalToFirestore(requireContext(), currentUid)
            parentFragmentManager.popBackStack()
        }

        // Использовать облачные данные → загрузить в SharedPreferences
        view.findViewById<View>(R.id.btn_use_cloud).setOnClickListener {
            val data = cloudData ?: return@setOnClickListener
            applyCloudData(data)
            parentFragmentManager.popBackStack()
            // Перезагружаем данные
            (activity as? com.example.quarter.android.MainActivity)?.let { main ->
                main.recreate()
            }
        }

        view.findViewById<View>(R.id.clickable_background).setOnClickListener {
            // Не даём закрыть нажатием на фон — нужно выбрать
        }
    }

    private fun applyCloudData(data: BudgetData) {
        val prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("HOW_MANY", data.howMany.toString())
            .putLong("NUMBER_OF_DAYS", data.numberOfDays)
            .putString("AVARAGE_DAILY_VALUE", data.averageDailyValue.toString())
            .putString("DATE_FULL", data.dateFull)
            .putString("LAST_DATE", data.lastDate)
            .putString("STRING_KEY", data.todayLimit.toString())
            .putInt("DAY_STREAK", data.streakCount)
            .putString("STREAK_LAST_DATE", data.streakLastDate)
            .apply()
    }

    companion object {
        fun newInstance() = DataConflictFragment()
    }
}
