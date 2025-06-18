package com.nhlstenden.appdev.features.casino.games

import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.nhlstenden.appdev.R
import kotlin.random.Random

class HorseRaceFragment : BaseGameFragment() {
    var guineaHorseMap = HashMap<String, ImageView>(4)

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

        return view
    }

    override fun onStart() {
        super.onStart()

        guineaHorseMap.forEach { string, guineaHorse ->
            val drawable = guineaHorse.drawable as AnimationDrawable
            drawable.start()
            guineaHorseHandlerList.add(GuineaHorseHandler(guineaHorse, string))
        }

        startRace()
    }

    fun startRace() {
        guineaHorseHandlerList.forEach { guineaHorseHandler ->
            guineaHorseHandler.start()
        }
    }

    companion object {
        var guineaHorseHandlerList = ArrayList<GuineaHorseHandler>(4)
        val finishLine = Resources.getSystem().displayMetrics.heightPixels - 600

        fun announceWinner(guineaHorseName: String) {
            guineaHorseHandlerList.forEach { guineaHorseHandler ->
                guineaHorseHandler.stop()
                Log.d("HorseRaceFragment", "Guinea Horse $guineaHorseName Is the winner!")
            }
        }
    }
}