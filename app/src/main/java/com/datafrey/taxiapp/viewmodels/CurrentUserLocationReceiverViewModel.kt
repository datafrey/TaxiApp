package com.datafrey.taxiapp.viewmodels

import android.Manifest
import android.app.Application
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.datafrey.taxiapp.ApplicationConstants
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

abstract class CurrentUserLocationReceiverViewModel(application: Application)
    : AndroidViewModel(application) {

    private var locationRequest: LocationRequest
    private var locationSettingsRequest: LocationSettingsRequest
    private var locationCallback: LocationCallback

    private var fusedLocationClient: FusedLocationProviderClient
    private var settingsClient: SettingsClient

    val isCurrentUserLocationUpdatesActive = MutableLiveData(false)

    init {
        locationRequest = buildLocationRequest()
        locationSettingsRequest = buildLocationSettingsRequest()
        locationCallback = buildCurrentUserLocationCallback()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        settingsClient = LocationServices.getSettingsClient(application)
    }

    private fun buildLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = 3000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    private fun buildLocationSettingsRequest(): LocationSettingsRequest {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        return builder.build()
    }

    protected abstract fun buildCurrentUserLocationCallback(): LocationCallback

    fun checkLocationPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    fun startCurrentUserLocationUpdates() {
        isCurrentUserLocationUpdatesActive.postValue(true)

        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                if (ActivityCompat.checkSelfPermission(getApplication(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getApplication(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@addOnSuccessListener
                }

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            }
            .addOnFailureListener { exception ->
                when ((exception as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvableApiException = (exception as ResolvableApiException)
                            resolvableApiException.startResolutionForResult(getApplication(),
                                ApplicationConstants.CHECK_SETTINGS_CODE
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            sie.printStackTrace()
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        isCurrentUserLocationUpdatesActive.postValue(false)
                    }
                }
            }
    }

    fun stopCurrentUserLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isCurrentUserLocationUpdatesActive.postValue(false)
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentUserLocationUpdates()
    }
}