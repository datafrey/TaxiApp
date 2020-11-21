package com.datafrey.taxiapp.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.datafrey.taxiapp.ApplicationConstants
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.viewmodels.CurrentUserLocationReceiverViewModel
import com.google.android.material.snackbar.Snackbar

open class LocationPermissionAskerActivity : AppCompatActivity() {

    private lateinit var viewModel: CurrentUserLocationReceiverViewModel

    protected fun setViewModelForLocationPermissionAsk(
        viewModel: CurrentUserLocationReceiverViewModel
    ) {
        this.viewModel = viewModel
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.checkLocationPermissions()) {
            requestLocationPermission()
        }
    }

    protected fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (shouldProvideRationale) {
            showSnackBar(getString(R.string.location_permission_is_needed_message), "OK") {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    ApplicationConstants.REQUEST_LOCATION_PERMISSION
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ApplicationConstants.REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(mainText: String, action: String, listener: View.OnClickListener) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mainText,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(action, listener).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ApplicationConstants.CHECK_SETTINGS_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d("MainActivity", "User has agreed to change location settings")
                        viewModel.startCurrentUserLocationUpdates()
                    }

                    Activity.RESULT_CANCELED -> {
                        Log.d("MainActivity", "User has not agreed to change location settings")
                        viewModel.isCurrentUserLocationUpdatesActive.postValue(false)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ApplicationConstants.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty()) {
                Log.d("onRequestPermissions", "Request was canceled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (viewModel.isCurrentUserLocationUpdatesActive.value!!) {
                    viewModel.startCurrentUserLocationUpdates()
                }
            } else {
                showSnackBar(
                    getString(R.string.turn_on_location_on_settings_message),
                    getString(R.string.settings_message_action)
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        R.string.app_name.toString(),
                        null
                    )

                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
    }
}