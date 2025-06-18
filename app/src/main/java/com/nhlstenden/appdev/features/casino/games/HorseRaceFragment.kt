package com.nhlstenden.appdev.features.casino.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhlstenden.appdev.R

class HorseRaceFragment : BaseGameFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_casino_plinko, container, false)
    }
}