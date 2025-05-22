package com.nhlstenden.appdev

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import com.nhlstenden.appdev.models.RewardsManager

class RewardsFragment : Fragment() {
    private lateinit var pointsValue: TextView
    private lateinit var timerText: TextView
    private lateinit var openChestButton: MaterialButton
    private lateinit var rewardShopList: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private var currentPoints = 1234 // Mock points value
    private lateinit var rewardShopAdapter: RewardShopAdapter
    private val supabaseClient = SupabaseClient()
    private lateinit var userId: String
    private lateinit var authToken: String
    private var openedDailyAt: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_rewards, container, false)

        // Initialize views
        pointsValue = view.findViewById(R.id.pointsValue)
        timerText = view.findViewById(R.id.timerText)
        openChestButton = view.findViewById(R.id.openChestButton)
        rewardShopList = view.findViewById(R.id.rewardShopList)

        updatePointsDisplay()
        setupDailyRewardTimer()
        setupAchievements(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userData = activity?.intent?.getParcelableExtra<User>("USER_DATA")
        userId = userData?.id.toString()
        authToken = userData?.authToken ?: ""
        android.util.Log.d("Supabase", "AuthToken: $authToken")
        
        // Initialize the reward shop AFTER userId is initialized
        setupRewardShop()
        
        // Fetch points and opened_daily_at from Supabase
        CoroutineScope(Dispatchers.IO).launch {
            val response = supabaseClient.getUserAttributes(userId)
            if (response.code == 200) {
                val userResponse = JSONArray(response.body?.string())
                currentPoints = userResponse.getJSONObject(0).getInt("points")
                openedDailyAt = userResponse.getJSONObject(0).optString("opened_daily_at", null)
                withContext(Dispatchers.Main) {
                    updatePointsDisplay()
                    setupDailyRewardTimer() // Now that we have openedDailyAt
                }
            }
            
            // Check saved rewards to debug any issues - moved after userId initialization
            checkSavedRewards()
        }
    }

    private fun updatePointsDisplay() {
        pointsValue.text = String.format("%,d", currentPoints)
        // Only update adapter if it's initialized
        if (::rewardShopAdapter.isInitialized) {
            rewardShopAdapter.updatePoints(currentPoints)
        }
    }

    private fun addPoints(points: Int) {
        currentPoints += points
        updatePointsDisplay()
        updatePointsInSupabase()
    }

    private fun spendPoints(points: Int) {
        currentPoints -= points
        updatePointsDisplay()
        updatePointsInSupabase()
    }

    private fun canAffordReward(cost: Int): Boolean {
        return currentPoints >= cost
    }

    private fun setupDailyRewardTimer() {
        val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString() // YYYY-MM-DD
        if (openedDailyAt == null || openedDailyAt != today) {
            timerText.text = "Ready to collect!"
            openChestButton.isEnabled = true
            openChestButton.text = "Open Now"
            openChestButton.alpha = 1.0f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        } else {
            // Calculate ms until midnight UTC
            val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC)
            val msUntilMidnight = java.time.Duration.between(now, midnight).toMillis()
            startCountDownTimer(msUntilMidnight)
            openChestButton.isEnabled = false
            openChestButton.text = "Wait for next reward"
            openChestButton.alpha = 0.6f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
        }

        openChestButton.setOnClickListener {
            // Add random points between 1 and 100
            val rewardPoints = (1..100).random()
            addPoints(rewardPoints)
            Toast.makeText(context, "You earned $rewardPoints points!", Toast.LENGTH_SHORT).show()
            openChestButton.isEnabled = false
            openChestButton.text = "Wait for next reward"
            openChestButton.alpha = 0.6f
            openChestButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray))
            // Update opened_daily_at in Supabase
            CoroutineScope(Dispatchers.IO).launch {
                val todayDate = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()
                val response = supabaseClient.updateUserOpenedDaily(userId, todayDate, authToken)
                if (response.code == 204 || response.code == 200) {
                    openedDailyAt = todayDate
                    withContext(Dispatchers.Main) {
                        // Start timer to next midnight
                        val now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC)
                        val msUntilMidnight = java.time.Duration.between(now, midnight).toMillis()
                        startCountDownTimer(msUntilMidnight)
                    }
                } else {
                    android.util.Log.e("Supabase", "Failed to update opened_daily_at: ${response.code} ${response.body?.string()}")
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
        val achievements = listOf(
            Achievement("HTML Master", "Complete all HTML challenges", R.drawable.ic_medal_gold, true),
            Achievement("CSS Wizard", "Create 10 custom styles", R.drawable.ic_medal_silver, false),
            Achievement("SQL Expert", "Write 20 complex queries", R.drawable.ic_medal_bronze, false),
            Achievement("Code Ninja", "Complete 50 coding tasks", R.drawable.ic_medal_gold, true),
            Achievement("Style Guru", "Master all CSS properties", R.drawable.ic_medal_silver, false),
            Achievement("Database Pro", "Design 5 database schemas", R.drawable.ic_medal_bronze, true),
            Achievement("HTML Hero", "Create 15 responsive layouts", R.drawable.ic_medal_gold, false),
            Achievement("CSS Artist", "Design 20 animations", R.drawable.ic_medal_silver, false),
            Achievement("SQL Master", "Optimize 10 queries", R.drawable.ic_medal_bronze, false),
            Achievement("Debug Pro", "Fix 25 bugs", R.drawable.ic_medal_gold, true),
            Achievement("Git Expert", "Make 100 commits", R.drawable.ic_medal_silver, false),
            Achievement("Team Player", "Complete 5 group projects", R.drawable.ic_medal_bronze, true)
        )

        // Set up first row of achievements
        val firstRow = view.findViewById<ViewGroup>(R.id.achievement1)
        setupAchievementItem(firstRow, achievements[0])
        val secondRow = view.findViewById<ViewGroup>(R.id.achievement2)
        setupAchievementItem(secondRow, achievements[1])
        val thirdRow = view.findViewById<ViewGroup>(R.id.achievement3)
        setupAchievementItem(thirdRow, achievements[2])
        val fourthRow = view.findViewById<ViewGroup>(R.id.achievement4)
        setupAchievementItem(fourthRow, achievements[3])
        val fifthRow = view.findViewById<ViewGroup>(R.id.achievement5)
        setupAchievementItem(fifthRow, achievements[4])
        val sixthRow = view.findViewById<ViewGroup>(R.id.achievement6)
        setupAchievementItem(sixthRow, achievements[5])
    }

    private fun setupAchievementItem(container: ViewGroup, achievement: Achievement) {
        val icon = container.findViewById<ImageView>(R.id.achievementIcon)
        val title = container.findViewById<TextView>(R.id.achievementTitle)
        val description = container.findViewById<TextView>(R.id.achievementDescription)

        icon.setImageResource(achievement.iconResId)
        title.text = achievement.title
        description.text = achievement.description

        if (!achievement.isUnlocked) {
            icon.alpha = 0.5f
            title.alpha = 0.5f
            description.alpha = 0.5f
        }
    }

    private fun setupRewardShop() {
        // Use RewardsManager to load rewards from resources
        val rewardsManager = RewardsManager(resources)
        val rewards = rewardsManager.loadRewards()

        // Initialize adapter with rewards but don't load unlocked status yet
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
        
        // Now refresh to get unlocked status from server
        refreshRewardsList()
    }

    private fun fetchUnlockedRewards(callback: (List<String>) -> Unit) {
        // Add null safety check
        if (!::userId.isInitialized) {
            android.util.Log.e("RewardsFragment", "Cannot fetch unlocked rewards - userId not initialized")
            callback(emptyList())
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.getUserUnlockedRewards(userId, authToken)
                android.util.Log.d("RewardsFragment", "Fetch unlocked rewards response code: ${response.code}")
                if (response.code == 200) {
                    val responseBody = response.body?.string()
                    android.util.Log.d("RewardsFragment", "Unlocked rewards response: $responseBody")
                    val rewardsArray = JSONArray(responseBody ?: "[]")
                    val unlockedRewards = mutableListOf<String>()
                    
                    for (i in 0 until rewardsArray.length()) {
                        val rewardObj = rewardsArray.getJSONObject(i)
                        val rewardId = rewardObj.getString("reward_id")
                        unlockedRewards.add(rewardId)
                        android.util.Log.d("RewardsFragment", "Found unlocked reward: $rewardId")
                    }
                    
                    android.util.Log.d("RewardsFragment", "Total unlocked rewards: ${unlockedRewards.size}")
                    withContext(Dispatchers.Main) {
                        callback(unlockedRewards)
                    }
                } else {
                    android.util.Log.e("Supabase", "Failed to fetch unlocked rewards: ${response.code}")
                    withContext(Dispatchers.Main) {
                        callback(emptyList())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error fetching unlocked rewards: ${e.message}")
                android.util.Log.e("Supabase", "Stack trace: ${e.stackTraceToString()}")
                withContext(Dispatchers.Main) {
                    callback(emptyList())
                }
            }
        }
    }

    private fun saveUnlockedRewardToSupabase(rewardId: String) {
        android.util.Log.d("RewardsFragment", "Attempting to save reward: $rewardId for user: $userId")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.unlockReward(userId, rewardId, authToken)
                android.util.Log.d("RewardsFragment", "Unlock reward response code: ${response.code}")
                if (response.code != 201 && response.code != 200 && response.code != 204) {
                    val errorBody = response.body?.string()
                    android.util.Log.e("Supabase", "Failed to save unlocked reward: ${response.code} $errorBody")
                } else {
                    android.util.Log.d("Supabase", "Reward unlocked successfully: $rewardId")
                    // After successfully unlocking, refresh the rewards from server
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Reward unlocked and saved to server!", Toast.LENGTH_SHORT).show()
                        refreshRewardsList()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Error saving unlocked reward: ${e.message}")
                android.util.Log.e("Supabase", "Stack trace: ${e.stackTraceToString()}")
            }
        }
    }

    private fun refreshRewardsList() {
        android.util.Log.d("RewardsFragment", "Refreshing rewards list from server")
        // Use RewardsManager to load rewards from resources
        val rewardsManager = RewardsManager(resources)
        val rewards = rewardsManager.loadRewards()

        // Fetch unlocked rewards from Supabase
        fetchUnlockedRewards { unlockedRewardIds ->
            android.util.Log.d("RewardsFragment", "Refreshed unlocked rewards: $unlockedRewardIds")
            if (unlockedRewardIds.isNotEmpty()) {
                val updatedRewards = rewardsManager.updateUnlockedStatus(rewards, unlockedRewardIds)
                // Update the adapter with new rewards
                rewardShopAdapter.updateRewards(updatedRewards.toMutableList())
            }
        }
    }

    private fun updatePointsInSupabase() {
        CoroutineScope(Dispatchers.IO).launch {
            android.util.Log.d("Supabase", "Updating userId: $userId with points: $currentPoints")
            val response = supabaseClient.updateUserPoints(userId, currentPoints, authToken)
            if (response.code != 204 && response.code != 200) {
                android.util.Log.e("Supabase", "Failed to update points: ${response.code} ${response.body?.string()}")
            } else {
                android.util.Log.d("Supabase", "Points updated successfully! New value: $currentPoints")
            }
            // Immediately GET after PATCH to see the value in Supabase
            val getResponse = supabaseClient.getUserAttributes(userId)
            android.util.Log.d("Supabase", "GET after PATCH: ${getResponse.body?.string()}")
        }
    }

    private fun checkSavedRewards() {
        // Add null safety check for userId
        if (!::userId.isInitialized || userId.isEmpty()) {
            android.util.Log.e("RewardsFragment", "Cannot check saved rewards - userId not initialized")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = supabaseClient.getUserUnlockedRewards(userId, authToken)
                if (response.code == 200) {
                    val responseBody = response.body?.string()
                    android.util.Log.d("RewardsFragment", "SAVED REWARDS CHECK - Response: $responseBody")
                    
                    val rewardsArray = JSONArray(responseBody ?: "[]")
                    for (i in 0 until rewardsArray.length()) {
                        val rewardObj = rewardsArray.getJSONObject(i)
                        val rewardId = rewardObj.getString("reward_id")
                        val createdAt = rewardObj.optString("created_at", "unknown")
                        android.util.Log.d("RewardsFragment", "SAVED REWARD: ID=$rewardId, Created=$createdAt")
                    }
                } else {
                    android.util.Log.e("RewardsFragment", "Failed to check saved rewards: ${response.code}")
                }
            } catch (e: Exception) {
                android.util.Log.e("RewardsFragment", "Error checking saved rewards: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    /**
     * Helper function to log loaded rewards for debugging
     */
    private fun logRewards(rewards: List<Reward>) {
        rewards.forEachIndexed { index, reward ->
            android.util.Log.d("RewardsFragment", "Reward[$index]: ${reward.title}, ${reward.pointsCost}pts - ${reward.description}")
        }
    }
}

data class Achievement(
    val title: String,
    val description: String,
    val iconResId: Int,
    val isUnlocked: Boolean
)

data class Reward(
    val title: String,
    val description: String,
    val pointsCost: Int,
    val iconResId: Int,
    val isUnlocked: Boolean
)

class RewardShopAdapter(
    private val rewards: MutableList<Reward>,
    private val onUnlockClick: (Reward) -> Boolean,
    private var currentPoints: Int,
    private val onSaveReward: (String) -> Unit
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardIcon: ImageView = view.findViewById(R.id.rewardIcon)
        val rewardTitle: TextView = view.findViewById(R.id.rewardTitle)
        val rewardDescription: TextView = view.findViewById(R.id.rewardDescription)
        val unlockButton: MaterialButton = view.findViewById(R.id.unlockButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward_shop, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reward = rewards[position]
        holder.apply {
            rewardIcon.setImageResource(reward.iconResId)
            rewardTitle.text = reward.title
            rewardDescription.text = reward.description
            if (reward.isUnlocked) {
                // Show strikethrough text for unlocked items
                val pointsText = "${reward.pointsCost} pts"
                val spannableString = SpannableString(pointsText)
                spannableString.setSpan(StrikethroughSpan(), 0, pointsText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                unlockButton.text = spannableString
                unlockButton.isEnabled = false
                unlockButton.alpha = 0.5f
                unlockButton.setBackgroundColor(ContextCompat.getColor(unlockButton.context, R.color.gray))
                rewardIcon.alpha = 0.5f
                rewardTitle.alpha = 0.5f
                rewardDescription.alpha = 0.5f
            } else {
                // Show points cost in the button
                unlockButton.text = "${reward.pointsCost} pts"
                unlockButton.isEnabled = true
                unlockButton.alpha = 1.0f
                rewardIcon.alpha = 1.0f
                rewardTitle.alpha = 1.0f
                rewardDescription.alpha = 1.0f
                // Set color based on affordability
                if (!canAffordReward(reward.pointsCost)) {
                    unlockButton.setBackgroundColor(ContextCompat.getColor(unlockButton.context, R.color.error))
                } else {
                    unlockButton.setBackgroundColor(ContextCompat.getColor(unlockButton.context, R.color.primary))
                }
                unlockButton.setOnClickListener {
                    if (onUnlockClick(reward)) {
                        // Save the unlocked reward to the database using the callback
                        onSaveReward(reward.title)
                        // Update the reward's unlocked state in the list
                        rewards[position] = reward.copy(isUnlocked = true)
                        notifyItemChanged(position)
                    }
                }
            }
        }
    }

    override fun getItemCount() = rewards.size
}