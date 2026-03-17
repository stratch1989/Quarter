import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quarter.android.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class DayItem(val day: String, val number: Int, val numberOfDay: Int, val dateFull: LocalDate)

class DayAdapter(
    private val dayList: List<DayItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    var selectedDay: Int? = null // Объявление переменной здесь

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        val dayFrame: FrameLayout = itemView.findViewById(R.id.dayFrame)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_day, parent, false)
        return DayViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {

        val currentItem = dayList[position]
        var numberOfDay = currentItem.numberOfDay
        val spellingDay = when {
            (currentItem.number % 10 == 1 && currentItem.number != 11) -> "день"
            (currentItem.number % 10 in 2..4 && currentItem.number !in 12..14) -> "дня"
            else -> "дней"
        }

        holder.dayTextView.text = "${currentItem.day} - ${currentItem.number} ${spellingDay}"
        if (position == selectedDay) {
            holder.dayTextView.text = "\u2713 ${currentItem.day} - ${currentItem.number} ${spellingDay}"
        }
        else if ((currentItem.number == numberOfDay)&&(position != numberOfDay)){
                holder.dayTextView.text = "\u2611 ${currentItem.day} - ${currentItem.number} ${spellingDay}"
        }

        holder.dayFrame.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                selectedDay = position
                notifyDataSetChanged()
                onItemClick(position)
            }
        }
    }

    override fun getItemCount() = dayList.size
}