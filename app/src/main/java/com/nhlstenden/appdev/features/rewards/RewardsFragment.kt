package com.nhlstenden.appdev.rewards.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Reward
import com.nhlstenden.appdev.core.models.RewardType
import com.nhlstenden.appdev.features.rewards.adapters.RewardShopAdapter
import com.nhlstenden.appdev.features.rewards.components.ShakeDetector
import com.nhlstenden.appdev.features.rewards.managers.RewardsManager
import com.nhlstenden.appdev.features.rewards.viewmodels.RewardsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RewardsFragment : Fragment() {
    
    @Inject
    lateinit var rewardsManager: RewardsManager
    
    private lateinit var pointsValue: TextView
    private lateinit var timerText: TextView
    private lateinit var openChestButton: MaterialButton
    private lateinit var rewardShopList: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var rewardShopAdapter: RewardShopAdapter
    private lateinit var shakeDetector: ShakeDetector
    
    private val viewModel: RewardsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rewards, container, false)
        pointsValue = view.findViewById(R.id.pointsValue)
        timerText = view.findViewById(R.id.timerText)
        openChestButton = view.findViewById(R.id.openChestButton)
        rewardShopList = view.findViewById(R.id.rewardShopList)
        
        // Start points icon animation
        val pointsIcon = view.findViewById<ImageView>(R.id.pointsIcon)
        (pointsIcon.drawable as? android.graphics.drawable.AnimationDrawable)?.start()
        

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRewardShop()
        setupShakeDetector()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector(
            context = requireContext(),
            onShakeDetected = {
                if (::openChestButton.isInitialized && openChestButton.isEnabled) {
                    openChestButton.performClick()
                }
            }
        )
        lifecycle.addObserver(shakeDetector)
    }



    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updatePointsDisplay(state.points)
                setupDailyRewardTimer(state.openedDailyAt, state.canCollectDailyReward, state.isCollectingReward)
                
                state.error?.let { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
                
                // Only show reward amount toast if it's a new reward (not initial load)
                if (state.lastRewardAmount > 0 && state.canCollectDailyReward == false) {
                    Toast.makeText(context, "You earned ${state.lastRewardAmount} points!", Toast.LENGTH_SHORT).show()
                }
                
                if (::rewardShopAdapter.isInitialized) {
                    rewardShopAdapter.updatePoints(state.points)
                    rewardShopAdapter.updateUnlockedRewards(state.unlockedRewardIds)
                }
            }
        }
    }

    private fun updatePointsDisplay(points: Int) {
        pointsValue.text = String.format("%,d", points)
    }

    private fun setupDailyRewardTimer(openedDailyAt: String?, canCollect: Boolean, isCollecting: Boolean) {
        if (canCollect && !isCollecting) {
            timerText.text = "Ready to collect!"
            openChestButton.isEnabled = true
            openChestButton.text = "Open Now"
            openChestButton.alpha = 1.0f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        } else {
            val now = ZonedDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val msUntilMidnight = java.time.Duration.between(now, midnight).toMillis()
            startCountDownTimer(msUntilMidnight)
            openChestButton.isEnabled = false
            openChestButton.text = if (isCollecting) "Collecting..." else "Wait for next reward"
            openChestButton.alpha = 0.6f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        
        openChestButton.setOnClickListener {
            if (canCollect && !isCollecting) {
                viewModel.collectDailyReward()
            }
        }
    }

    private fun startCountDownTimer(millisUntilFinished: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millisUntilFinished, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                timerText.text = "Next reward in: %02d:%02d:%02d".format(hours, minutes, seconds)
                openChestButton.text = "Wait for next reward"
                openChestButton.alpha = 0.6f
                openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
            }
            override fun onFinish() {
                timerText.text = "Ready to collect!"
                openChestButton.isEnabled = true
                openChestButton.text = "Open Now"
                openChestButton.alpha = 1.0f
                openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            }
        }.start()
    }



    private fun setupRewardShop() {
        val rewards = rewardsManager.loadRewards()
        rewardShopAdapter = RewardShopAdapter(
            rewards = rewards.toMutableList(),
            onRewardPurchase = { reward, rewardType ->
                handleRewardPurchase(reward, rewardType)
            }
        )
        rewardShopList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rewardShopAdapter
        }
    }
    
    private fun handleRewardPurchase(reward: Reward, rewardType: RewardType) {
        viewModel.purchaseRewardByType(rewardType, reward.id, reward.pointsCost)
        Toast.makeText(context, "Unlocked: ${reward.title}!", Toast.LENGTH_SHORT).show()
    }



} 