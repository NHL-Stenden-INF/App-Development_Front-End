package com.nhlstenden.appdev

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
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
import java.util.concurrent.TimeUnit

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RewardsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RewardsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var pointsValue: TextView
    private lateinit var timerText: TextView
    private lateinit var openChestButton: MaterialButton
    private lateinit var rewardShopList: RecyclerView
    private var countDownTimer: CountDownTimer? = null
    private var currentPoints = 1234 // Mock points value
    private lateinit var rewardShopAdapter: RewardShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

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

        // Set up reward shop first
        setupRewardShop()

        // Set initial points after adapter is initialized
        updatePointsDisplay()

        // Set up daily reward timer
        setupDailyRewardTimer()

        // Set up achievements
        setupAchievements(view)

        return view
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
    }

    private fun canAffordReward(cost: Int): Boolean {
        return currentPoints >= cost
    }

    private fun setupDailyRewardTimer() {
        // TODO: Get last reward time from SharedPreferences
        val lastRewardTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12) // Example: 12 hours ago
        val timeUntilNextReward = TimeUnit.HOURS.toMillis(24) - (System.currentTimeMillis() - lastRewardTime)

        if (timeUntilNextReward > 0) {
            startCountDownTimer(timeUntilNextReward)
            openChestButton.isEnabled = false
        } else {
            timerText.text = "Ready to collect!"
            openChestButton.isEnabled = true
        }

        openChestButton.setOnClickListener {
            // Add random points between 50 and 200
            val rewardPoints = (50..200).random()
            addPoints(rewardPoints)
            Toast.makeText(context, "You earned $rewardPoints points!", Toast.LENGTH_SHORT).show()
            openChestButton.isEnabled = false
            startCountDownTimer(TimeUnit.HOURS.toMillis(24))
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
            }

            override fun onFinish() {
                timerText.text = "Ready to collect!"
                openChestButton.isEnabled = true
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
        val rewards = listOf(
            Reward("Premium Theme", "Unlock a beautiful dark theme", 500, R.drawable.ic_achievement, false),
            Reward("Custom Avatar", "Get a unique profile picture", 1000, R.drawable.ic_achievement, false),
            Reward("Advanced Stats", "View detailed progress analytics", 750, R.drawable.ic_achievement, true),
            Reward("Practice Mode", "Unlimited practice exercises", 1500, R.drawable.ic_achievement, false)
        )

        rewardShopAdapter = RewardShopAdapter(rewards, { reward ->
            if (canAffordReward(reward.pointsCost)) {
                currentPoints -= reward.pointsCost
                updatePointsDisplay()
                Toast.makeText(context, "Unlocked: ${reward.title}!", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "Not enough points!", Toast.LENGTH_SHORT).show()
                false
            }
        }, currentPoints)

        rewardShopList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = rewardShopAdapter
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RewardsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RewardsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
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
    private val rewards: List<Reward>,
    private val onUnlockClick: (Reward) -> Boolean,
    private var currentPoints: Int
) : RecyclerView.Adapter<RewardShopAdapter.ViewHolder>() {

    fun updatePoints(newPoints: Int) {
        currentPoints = newPoints
        notifyDataSetChanged()
    }

    private fun canAffordReward(cost: Int): Boolean {
        return currentPoints >= cost
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rewardIcon: ImageView = view.findViewById(R.id.rewardIcon)
        val rewardTitle: TextView = view.findViewById(R.id.rewardTitle)
        val rewardDescription: TextView = view.findViewById(R.id.rewardDescription)
        val pointsCost: TextView = view.findViewById(R.id.pointsCost)
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
            pointsCost.text = "${reward.pointsCost} pts"

            if (reward.isUnlocked) {
                unlockButton.text = "Unlocked"
                unlockButton.isEnabled = false
                rewardIcon.alpha = 0.5f
                rewardTitle.alpha = 0.5f
                rewardDescription.alpha = 0.5f
                pointsCost.alpha = 0.5f
            } else {
                unlockButton.text = "Unlock"
                unlockButton.isEnabled = true
                if (!canAffordReward(reward.pointsCost)) {
                    unlockButton.setBackgroundColor(ContextCompat.getColor(unlockButton.context, R.color.error))
                } else {
                    unlockButton.setBackgroundColor(ContextCompat.getColor(unlockButton.context, R.color.primary))
                }
                unlockButton.setOnClickListener {
                    if (onUnlockClick(reward)) {
                        unlockButton.text = "Unlocked"
                        unlockButton.isEnabled = false
                        rewardIcon.alpha = 0.5f
                        rewardTitle.alpha = 0.5f
                        rewardDescription.alpha = 0.5f
                        pointsCost.alpha = 0.5f
                    }
                }
            }
        }
    }

    override fun getItemCount() = rewards.size
}