package com.datafrey.taxiapp.drivermaps

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DriverMapsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverMapsViewModel::class.java)) {
            return DriverMapsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}