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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.core.models.Reward
import com.nhlstenden.appdev.rewards.manager.RewardsManager
import com.nhlstenden.appdev.core.utils.UserManager
import com.nhlstenden.appdev.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

class RewardsFragment : Fragment(), SensorEventListener {
    private lateinit var pointsValue: TextView
    private lateinit var timerText: TextView
    private lateinit var openChestButton: MaterialButton
    private lateinit var rewardShopList: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private var currentPoints = 0
    private lateinit var rewardShopAdapter: RewardShopAdapter
    private val supabaseClient = SupabaseClient()
    private lateinit var userId: String
    private lateinit var authToken: String
    private var openedDailyAt: String? = null
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
        setupAchievements(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userData = UserManager.getCurrentUser()
        if (userData == null) {
            Toast.makeText(context, "Error: User data not found", Toast.LENGTH_LONG).show()
            currentPoints = 0
            updatePointsDisplay()
            return
        }
        userId = userData.id
        authToken = userData.authToken
        setupRewardShop()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        fetchUserAttributesAndUpdateUI()
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

    private fun fetchUserAttributesAndUpdateUI() {
        val userData = UserManager.getCurrentUser()
        if (userData == null) {
            Toast.makeText(context, "Error: User data not found", Toast.LENGTH_LONG).show()
            currentPoints = 0
            updatePointsDisplay()
            return
        }
        userId = userData.id
        authToken = userData.authToken
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.getUserAttributes(userId, authToken)
                val responseBody = response.body?.string()
                if (response.code == 200) {
                    val userResponse = JSONArray(responseBody)
                    if (userResponse.length() > 0) {
                        val userDataJson = userResponse.getJSONObject(0)
                        currentPoints = userDataJson.optInt("points", 0)
                        openedDailyAt = userDataJson.optString("opened_daily_at", null)
                        withContext(Dispatchers.Main) {
                            updatePointsDisplay()
                            setupDailyRewardTimer()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            currentPoints = 0
                            updatePointsDisplay()
                            Toast.makeText(context, "Error: User data not found", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        currentPoints = 0
                        updatePointsDisplay()
                        Toast.makeText(context, "Error: Failed to load user data", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentPoints = 0
                    updatePointsDisplay()
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updatePointsDisplay() {
        pointsValue.text = String.format("%,d", currentPoints)
        if (::rewardShopAdapter.isInitialized) {
            rewardShopAdapter.updatePoints(currentPoints)
        }
    }

    private fun setupDailyRewardTimer() {
        val today = LocalDate.now().toString()
        if (openedDailyAt == null || openedDailyAt != today) {
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
            openChestButton.text = "Wait for next reward"
            openChestButton.alpha = 0.6f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
        }
        openChestButton.setOnClickListener {
            openChestButton.isEnabled = false
            val rewardPoints = (1..100).random()
            CoroutineScope(Dispatchers.IO).launch {
                var retryCount = 0
                val maxRetries = 3
                var success = false
                while (!success && retryCount < maxRetries) {
                    try {
                        val todayDate = LocalDate.now().toString()
                        val response = supabaseClient.updateUserOpenedDaily(userId, todayDate, authToken)
                        if (response.code == 204 || response.code == 200) {
                            openedDailyAt = todayDate
                            val newPoints = currentPoints + rewardPoints
                            val pointsResponse = supabaseClient.updateUserPoints(userId, newPoints, authToken)
                            if (pointsResponse.code == 204 || pointsResponse.code == 200) {
                                withContext(Dispatchers.Main) {
                                    currentPoints = newPoints
                                    updatePointsDisplay()
                                    Toast.makeText(context, "You earned $rewardPoints points!", Toast.LENGTH_SHORT).show()
                                    openChestButton.text = "Wait for next reward"
                                    openChestButton.alpha = 0.6f
                                    openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
                                    val now = ZonedDateTime.now()
                                    val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                                    val msUntilMidnight = java.time.Duration.between(now, midnight).toMillis()
                                    startCountDownTimer(msUntilMidnight)
                                }
                                success = true
                            } else {
                                retryCount++
                                if (retryCount < maxRetries) {
                                    kotlinx.coroutines.delay(1000)
                                }
                            }
                        } else {
                            retryCount++
                            if (retryCount < maxRetries) {
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount < maxRetries) {
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                }
                if (!success) {
                    withContext(Dispatchers.Main) {
                        openChestButton.isEnabled = true
                        Toast.makeText(context, "Failed to collect reward after multiple attempts. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
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

    private fun setupAchievements(view: View) {
        val achievementIds = listOf(
            R.id.achievement1, R.id.achievement2, R.id.achievement3, R.id.achievement4, R.id.achievement5, R.id.achievement6,
            R.id.achievement7, R.id.achievement8, R.id.achievement9, R.id.achievement10, R.id.achievement11, R.id.achievement12
        )
        val titles = resources.getStringArray(R.array.achievement_titles)
        val descriptions = resources.getStringArray(R.array.achievement_descriptions)
        val icons = resources.getStringArray(R.array.achievement_icons)
        val unlockedIds = setOf<Int>() // TODO: Replace with actual unlocked achievement ids
        for (i in achievementIds.indices) {
            val achView = view.findViewById<View>(achievementIds[i])
            achView.findViewById<TextView>(R.id.achievementTitle).text = titles.getOrNull(i) ?: ""
            achView.findViewById<TextView>(R.id.achievementDescription).text = descriptions.getOrNull(i) ?: ""
            val iconName = icons.getOrNull(i) ?: "ic_achievement"
            val iconResId = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
            achView.findViewById<ImageView>(R.id.achievementIcon).setImageResource(if (iconResId != 0) iconResId else R.drawable.ic_achievement)
            achView.alpha = if (unlockedIds.contains(i + 1)) 1.0f else 0.4f
        }
    }

    private fun setupRewardShop() {
        val rewardsManager = RewardsManager(requireContext(), resources)
        val rewards = rewardsManager.loadRewards()
        rewardShopAdapter = RewardShopAdapter(rewards.toMutableList(), { reward ->
            if (canAffordReward(reward.pointsCost)) {
                spendPoints(reward.pointsCost)
                Toast.makeText(context, "Unlocked: ${reward.title}!", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "Not enough points!", Toast.LENGTH_SHORT).show()
                false
            }
        }, currentPoints, { rewardId ->
            saveUnlockedRewardToSupabase(rewardId)
        })
        rewardShopList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rewardShopAdapter
        }
        refreshRewardsList()
    }

    private fun canAffordReward(cost: Int): Boolean {
        return currentPoints >= cost
    }

    private fun spendPoints(points: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val newPoints = currentPoints - points
            val response = supabaseClient.updateUserPoints(userId, newPoints, authToken)
            if (response.code == 204 || response.code == 200) {
                withContext(Dispatchers.Main) {
                    currentPoints = newPoints
                    updatePointsDisplay()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to update points. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveUnlockedRewardToSupabase(rewardId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.unlockReward(userId, rewardId, authToken)
                if (response.code == 201 || response.code == 200 || response.code == 204) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Reward unlocked and saved to server!", Toast.LENGTH_SHORT).show()
                        refreshRewardsList()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save unlocked reward.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving unlocked reward: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshRewardsList() {
        val rewardsManager = RewardsManager(requireContext(), resources)
        val rewards = rewardsManager.loadRewards()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.getUserUnlockedRewards(userId, authToken)
                if (response.code == 200) {
                    val responseBody = response.body?.string()
                    val unlockedIds = mutableListOf<Int>()
                    if (responseBody != null) {
                        val arr = JSONArray(responseBody)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            unlockedIds.add(obj.getInt("reward_id"))
                        }
                    }
                    val updatedRewards = rewardsManager.updateUnlockedStatus(rewards, unlockedIds)
                    withContext(Dispatchers.Main) {
                        rewardShopAdapter.updateRewards(updatedRewards.toMutableList())
                    }
                }
            } catch (_: Exception) {}
        }
    }

    class RewardShopAdapter(
        private val rewards: MutableList<Reward>,
        private val onUnlockClick: (Reward) -> Boolean,
        private var currentPoints: Int,
        private val onSaveReward: (Int) -> Unit
    ) : RecyclerView.Adapter<RewardShopAdapter.ViewHolder>() {
        fun updatePoints(newPoints: Int) {
            currentPoints = newPoints
            notifyDataSetChanged()
        }
        fun updateRewards(newRewards: MutableList<Reward>) {
            rewards.clear()
            rewards.addAll(newRewards)
            notifyDataSetChanged()
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