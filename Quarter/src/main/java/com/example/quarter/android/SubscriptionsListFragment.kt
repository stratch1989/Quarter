package com.example.quarter.android

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SubscriptionsListFragment : Fragment() {

    private var subscriptions = mutableListOf<SubscriptionEntry>()
    private var onDelete: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscriptions_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.clickable_background).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val container = view.findViewById<LinearLayout>(R.id.subscriptions_container)
        val emptyText = view.findViewById<TextView>(R.id.empty_text)

        // Загружаем подписки
        val manager = SubscriptionManager(requireContext())
        subscriptions = manager.loadSubscriptions()

        if (subscriptions.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }

        emptyText.visibility = View.GONE
        buildList(container, manager)
    }

    private fun buildList(container: LinearLayout, manager: SubscriptionManager) {
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        for (i in subscriptions.indices) {
            val sub = subscriptions[i]

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (44 * dp).toInt()
                )
                setPadding((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            }

            // Emoji категории
            val emoji = TextView(requireContext()).apply {
                text = sub.category ?: "💸"
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(
                    (32 * dp).toInt(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Сумма и интервал
            val info = TextView(requireContext()).apply {
                text = "${sub.amount}₽ / ${sub.getDisplayInterval()}"
                setTextColor(Color.WHITE)
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Кнопка удаления
            val deleteBtn = TextView(requireContext()).apply {
                text = "✕"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    (36 * dp).toInt(),
                    (36 * dp).toInt()
                )
                setOnClickListener {
                    subscriptions.removeAt(i)
                    manager.saveSubscriptions(subscriptions)
                    onDelete?.invoke(i)
                    buildList(container, manager)
                    if (subscriptions.isEmpty()) {
                        view?.findViewById<TextView>(R.id.empty_text)?.visibility = View.VISIBLE
                    }
                }
            }

            row.addView(emoji)
            row.addView(info)
            row.addView(deleteBtn)
            container.addView(row)

            // Разделитель
            if (i < subscriptions.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (0.5 * dp).toInt()
                    ).apply {
                        setMargins((24 * dp).toInt(), 0, (24 * dp).toInt(), 0)
                    }
                    setBackgroundColor(Color.parseColor("#3A3A3C"))
                }
                container.addView(divider)
            }
        }
    }

    fun setOnDeleteListener(listener: (Int) -> Unit) {
        onDelete = listener
    }

    companion object {
        fun newInstance() = SubscriptionsListFragment()
    }
}
