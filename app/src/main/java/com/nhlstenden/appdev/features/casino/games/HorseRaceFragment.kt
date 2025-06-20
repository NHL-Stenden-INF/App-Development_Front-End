package com.nhlstenden.appdev.features.casino.games

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.nhlstenden.appdev.R
import com.nhlstenden.appdev.features.casino.interfaces.RaceManager


class HorseRaceFragment : BaseGameFragment(), RaceManager {
    var guineaHorseMap = HashMap<String, ImageView>(4)
    var guineaHorseHandlerList = ArrayList<GuineaHorseHandler>(4)
    private lateinit var finishLine: ImageView

    lateinit var betterGuineaHorse: String
    lateinit var winnerGuineaHorse: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casino_horse_races, container, false)

        guineaHorseMap.put("default", view.findViewById<ImageView>(R.id.guineaHorse))
        guineaHorseMap.put("red", view.findViewById<ImageView>(R.id.guineaHorseRed))
        guineaHorseMap.put("green", view.findViewById<ImageView>(R.id.guineaHorseGreen))
        guineaHorseMap.put("yellow", view.findViewById<ImageView>(R.id.guineaHorseYellow))

        finishLine = view.findViewById<ImageView>(R.id.finishLine)

        return view
    }

    override fun onStart() {
        super.onStart()

        guineaHorseMap.forEach { string, guineaHorse ->
            val drawable = guineaHorse.drawable as AnimationDrawable
            drawable.start()

            guineaHorse.setOnClickListener {
                guineaHorse.setOnClickListener(null)
                betterGuineaHorse = string
                startGame()
            }

            guineaHorseHandlerList.add(GuineaHorseHandler(guineaHorse, string, this as RaceManager))
        }
    }


    override fun startGame() {
        guineaHorseHandlerList.forEach { guineaHorseHandler ->
            guineaHorseHandler.start()
        }
    }

    override fun calculateScore(score: Int): Int {
        return if (winnerGuineaHorse == betterGuineaHorse) {
            (score * 3).toInt()
        } else {
            (score / 3).toInt()
        }
    }

    override fun onRaceCompleted(guineaHorseName: String) {
        guineaHorseHandlerList.forEach { guineaHorseHandler ->
            guineaHorseHandler.stop()
        }
        winnerGuineaHorse = guineaHorseName
        Log.d("HorseRaceFragment", "The winner is: $guineaHorseName")

        val rewardedPoints = calculateScore(viewModel.gamePoint.value!!)

        return finishGame(rewardedPoints)
    }

    override fun getFinishline(): Float {
        return finishLine.y - guineaHorseMap.get("default")!!.measuredHeight + 60 // To compensate for the massive forehead that the horses have
    }
}