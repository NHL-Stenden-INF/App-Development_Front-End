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

    private val _isGameDone = MutableLiveData<Boolean>(false)
    val isGameDone: LiveData<Boolean> get() = _isGameDone

    fun setGamePoints(points: Int) {
        _gamePoints.value = points
    }

    fun setIsGameDone(isGameDone: Boolean) {
        _isGameDone.value = isGameDone
    }
}