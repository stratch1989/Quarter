package com.example.quarter.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.quarter.android.databinding.FragmentStreakBinding

class StreakFragment : Fragment() {
    lateinit var binding: FragmentStreakBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStreakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val streak = arguments?.getInt("streak", 1) ?: 1

        binding.streakTitle.text = "$streak ${dayWord(streak)} подряд!"

        // Анимация появления
        binding.clickableBackground.alpha = 0f
        binding.frameForMetrics.alpha = 0f
        binding.clickableBackground.animate().alpha(1f).setDuration(200).start()
        binding.frameForMetrics.animate().alpha(1f).setDuration(200).start()

        binding.clickableBackground.setOnClickListener {
            dismissWithAnimation()
        }
        binding.frameForMetrics.setOnClickListener { }
    }

    private fun dismissWithAnimation() {
        binding.clickableBackground.animate().alpha(0f).setDuration(200).withEndAction {
            if (isAdded) parentFragmentManager.popBackStack()
        }.start()
        binding.frameForMetrics.animate().alpha(0f).setDuration(200).start()
    }

    private fun dayWord(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..19 -> "дней"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(streak: Int) = StreakFragment().apply {
            arguments = Bundle().apply { putInt("streak", streak) }
        }
    }
}
