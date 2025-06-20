package com.nhlstenden.appdev.features.casino.games

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.nhlstenden.appdev.R


class HorseRaceFragment : BaseGameFragment(), RaceManager {
    var guineaHorseMap = HashMap<String, ImageView>(4)
    var guineaHorseHandlerList = ArrayList<GuineaHorseHandler>(4)
    private lateinit var finishLine: ImageView

    lateinit var betterGuineaHorse: String

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
                betterGuineaHorse = string
                startRace()
                guineaHorse.setOnClickListener(null)
            }

            guineaHorseHandlerList.add(GuineaHorseHandler(guineaHorse, string, this as RaceManager))
        }
    }

    fun startRace() {
        guineaHorseHandlerList.forEach { guineaHorseHandler ->
            guineaHorseHandler.start()
        }
    }

    override fun onRaceCompleted(guineaHorseName: String) {
        guineaHorseHandlerList.forEach { guineaHorseHandler ->
            guineaHorseHandler.stop()
        }
        Log.d("HorseRaceFragment", "The winner is: $guineaHorseName")

        val rewardedPoints = if (guineaHorseName == betterGuineaHorse) {
            viewModel.gamePoint.value!! * 3
        } else {
            viewModel.gamePoint.value!! / 3
        }

        return finishGame(rewardedPoints)
    }

    override fun getFinishline(): Float {
        return finishLine.y - guineaHorseMap.get("default")!!.measuredHeight + 60 // To compensate for the massive forehead that the horses have
    }
}