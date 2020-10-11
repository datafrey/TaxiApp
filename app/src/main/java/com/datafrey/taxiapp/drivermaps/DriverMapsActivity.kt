package com.datafrey.taxiapp.drivermaps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.currentuserlocationreceiving.LocationPermissionAskerActivity
import com.datafrey.taxiapp.databinding.ActivityDriverMapsBinding
import com.datafrey.taxiapp.util.cancelNotifications
import com.datafrey.taxiapp.util.toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_driver_maps.*

class DriverMapsActivity : LocationPermissionAskerActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var viewModel: DriverMapsViewModel

    private var driverMarker: Marker? = null
    private var passengerMarker: Marker? = null

    private var zoomToLocation = true
    private var zoomToPassenger = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityDriverMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this, DriverMapsViewModelFactory(application))
            .get(DriverMapsViewModel::class.java)

        setViewModelForLocationPermissionAsk(viewModel)

        binding.let {
            it.viewModel = viewModel
            it.lifecycleOwner = this
            it.executePendingBindings()
        }

        viewModel.driverCurrentLocation.observe(this, { drawDriverMarker(it) })
        viewModel.passengerCurrentLocation.observe(this, { drawPassengerMarker(it) })

        viewModel.orderExecutionEnded.observe(this, {
            if (it) {
                toast(R.string.end_of_order_execution_message)
                viewModel.orderExecutionEndedMessageShown()
            }
        })

        signOutButton.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        createNotificationChannel(
            getString(R.string.driver_notification_channel_id),
            getString(R.string.driver_notification_channel_name)
        )

        viewModel.startCurrentUserLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        drawDriverMarker(viewModel.driverCurrentLocation.value)
        drawPassengerMarker(viewModel.passengerCurrentLocation.value)
    }

    private fun drawDriverMarker(location: Location?) {
        if (this::googleMap.isInitialized) {
            driverMarker?.remove()
            location?.let {
                val driverLocation = LatLng(it.latitude, it.longitude)

                if (zoomToLocation) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation))
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
                    zoomToLocation = false
                }

                driverMarker = googleMap.addMarker(
                    MarkerOptions().position(driverLocation)
                        .title(getString(R.string.self_marker_title))
                )
            }
        }
    }

    private fun drawPassengerMarker(location: Location?) {
        if (this::googleMap.isInitialized) {
            passengerMarker?.remove()
            location?.let {
                val passengerLocation = LatLng(it.latitude, it.longitude)

                if (zoomToPassenger) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
                    zoomToPassenger = false
                }

                passengerMarker = googleMap.addMarker(
                    MarkerOptions().position(passengerLocation)
                        .title(
                            getString(
                                R.string.passenger_marker_title,
                                viewModel.passengerInfo?.name
                            )
                        )
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            )
                        )
                )
            }
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationChannel.run {
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                description = getString(R.string.driver_notification_channel_description)
            }

            val notificationManager = this.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.allowNotifications()
    }

    override fun onResume() {
        super.onResume()
        viewModel.disableNotifications()

        val notificationManager = ContextCompat.getSystemService(
            application, NotificationManager::class.java
        ) as NotificationManager
        notificationManager.cancelNotifications()
    }

}