package com.datafrey.taxiapp.passengermaps

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PassengerMapsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PassengerMapsViewModel::class.java)) {
            return PassengerMapsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}