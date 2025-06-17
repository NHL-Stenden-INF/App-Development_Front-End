package com.nhlstenden.appdev.features.casino.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nhlstenden.appdev.features.task.viewmodels.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CasinoViewmodel @Inject constructor() : BaseViewModel() {
    private val _gamePoints = MutableLiveData<Int>()
    val gamePoint: LiveData<Int> get() = _gamePoints

    fun setGamePoints(points: Int) {
        _gamePoints.value = points
    }
}