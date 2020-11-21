package com.datafrey.taxiapp.viewmodelfactories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.datafrey.taxiapp.viewmodels.PassengerMapsViewModel

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