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
import com.nhlstenden.appdev.rewards.manager.RewardsManager
import com.nhlstenden.appdev.features.rewards.viewmodels.RewardsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.widget.FrameLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RewardsFragment : Fragment(), SensorEventListener {
    private lateinit var pointsValue: TextView
    private lateinit var timerText: TextView
    private lateinit var openChestButton: MaterialButton
    private lateinit var rewardShopList: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private lateinit var rewardShopAdapter: RewardShopAdapter
    private val viewModel: RewardsViewModel by viewModels()
    

    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var shakeThreshold = 12f // Adjust as needed

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
        observeViewModel()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val now = System.currentTimeMillis()
            if (lastX != 0f || lastY != 0f || lastZ != 0f) {
                val deltaX = x - lastX
                val deltaY = y - lastY
                val deltaZ = z - lastZ
                val delta = Math.sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
                if (delta > shakeThreshold && now - lastShakeTime > 1000) { // 1s cooldown
                    lastShakeTime = now
                    if (openChestButton.isEnabled) {
                        openChestButton.performClick()
                    }
                }
            }
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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
        val rewardsManager = RewardsManager(requireContext(), resources)
        val rewards = rewardsManager.loadRewards()
        rewardShopAdapter = RewardShopAdapter(rewards.toMutableList(), { reward ->
            if (reward.title == "Extra Life (Bell Pepper)") {
                viewModel.purchaseBellPepper(reward.pointsCost)
                Toast.makeText(context, "Unlocked: ${reward.title}!", Toast.LENGTH_SHORT).show()
                true
            } else {
                viewModel.purchaseReward(reward.id, reward.pointsCost)
                Toast.makeText(context, "Unlocked: ${reward.title}!", Toast.LENGTH_SHORT).show()
                true
            }
        }, 0, { _ ->
            // Reward saving is now handled by the ViewModel
        })
        rewardShopList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rewardShopAdapter
        }
    }



    class RewardShopAdapter(
        private val rewards: MutableList<Reward>,
        private val onUnlockClick: (Reward) -> Boolean,
        private var currentPoints: Int,
        private val onSaveReward: (Int) -> Unit
    ) : RecyclerView.Adapter<RewardShopAdapter.ViewHolder>() {
        
        private var unlockedRewardIds: Set<Int> = emptySet()
        
        fun updatePoints(newPoints: Int) {
            currentPoints = newPoints
            notifyDataSetChanged()
        }
        
        fun updateUnlockedRewards(newUnlockedIds: Set<Int>) {
            unlockedRewardIds = newUnlockedIds
            updateRewardsUnlockedStatus()
            notifyDataSetChanged()
        }
        
        fun updateRewards(newRewards: MutableList<Reward>) {
            rewards.clear()
            rewards.addAll(newRewards)
            updateRewardsUnlockedStatus()
            notifyDataSetChanged()
        }
        
        private fun updateRewardsUnlockedStatus() {
            for (i in rewards.indices) {
                rewards[i] = rewards[i].copy(unlocked = unlockedRewardIds.contains(rewards[i].id))
            }
        }
        
        private fun canAffordReward(cost: Int): Boolean {
            return currentPoints >= cost
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reward_shop, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(rewards[position], position)
        }
        override fun getItemCount() = rewards.size
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val rewardIcon: ImageView = itemView.findViewById(R.id.rewardIcon)
            private val rewardTitle: TextView = itemView.findViewById(R.id.rewardTitle)
            private val rewardDescription: TextView = itemView.findViewById(R.id.rewardDescription)
            private val unlockButton: MaterialButton = itemView.findViewById(R.id.unlockButton)
            private val comingSoonSticker: FrameLayout = itemView.findViewById(R.id.comingSoonSticker)

            fun bind(reward: Reward, position: Int) {
                rewardIcon.setImageResource(reward.iconResId)
                rewardTitle.text = reward.title
                rewardDescription.text = reward.description

                // Handle unlocked state
                if (reward.unlocked) {
                    val pointsText = "${reward.pointsCost} pts"
                    val spannableString = SpannableString(pointsText)
                    spannableString.setSpan(StrikethroughSpan(), 0, pointsText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    unlockButton.text = spannableString
                    unlockButton.isEnabled = false
                    unlockButton.alpha = 0.5f
                    rewardIcon.alpha = 0.5f
                    rewardTitle.alpha = 0.5f
                    rewardDescription.alpha = 0.5f
                } else {
                    unlockButton.text = "${reward.pointsCost} pts"
                    unlockButton.isEnabled = canAffordReward(reward.pointsCost)
                    unlockButton.alpha = 1.0f
                    rewardIcon.alpha = 1.0f
                    rewardTitle.alpha = 1.0f
                    rewardDescription.alpha = 1.0f
                }

                // Handle coming soon sticker
                if (reward.id != 11) { // Not the course lobby music
                    comingSoonSticker.visibility = View.VISIBLE
                } else {
                    comingSoonSticker.visibility = View.GONE
                }

                unlockButton.setOnClickListener {
                    if (!reward.unlocked && onUnlockClick(reward)) {
                        onSaveReward(reward.id)
                        rewards[position] = reward.copy(unlocked = true)
                        notifyItemChanged(position)
                    }
                }
            }
        }
    }
} 