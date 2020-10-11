package com.datafrey.taxiapp.passengermaps

import android.location.Location
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.currentuserlocationreceiving.LocationPermissionAskerActivity
import com.datafrey.taxiapp.databinding.ActivityPassengerMapsBinding
import com.datafrey.taxiapp.util.toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_passenger_maps.*

class PassengerMapsActivity : LocationPermissionAskerActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap

    private lateinit var viewModel: PassengerMapsViewModel

    private var passengerMarker: Marker? = null
    private var foundDriverMarker: Marker? = null

    private var zoomToLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPassengerMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this, PassengerMapsViewModelFactory(application))
            .get(PassengerMapsViewModel::class.java)

        setViewModelForLocationPermissionAsk(viewModel)

        binding.let {
            it.viewModel = viewModel
            it.lifecycleOwner = this
            it.executePendingBindings()
        }

        viewModel.passengerCurrentLocation.observe(this, { drawPassengerMarker(it) })
        viewModel.foundDriverCurrentLocation.observe(this, { drawFoundDriverMarker(it) })

        viewModel.noDriversFound.observe(this, {
            if (it) uiReactionIfNoDriversFound()
        })

        viewModel.driverRefusedTheOrder.observe(this, {
            if (it) uiReactionIfDriverRefusedTheOrder()
        })

        signOutButton.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel.startCurrentUserLocationUpdates()
    }

    private fun uiReactionIfNoDriversFound() {
        toast(R.string.no_drivers_found_message)
        viewModel.noDriversFoundMessageShown()
    }

    private fun uiReactionIfDriverRefusedTheOrder() {
        toast(R.string.driver_refused_the_order_message)
        viewModel.driverRefusedTheOrderMessageShown()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        drawPassengerMarker(viewModel.passengerCurrentLocation.value)
        drawFoundDriverMarker(viewModel.foundDriverCurrentLocation.value)
    }

    private fun drawPassengerMarker(location: Location?) {
        if (this::googleMap.isInitialized) {
            passengerMarker?.remove()
            location?.let {
                val passengerLocation = LatLng(it.latitude, it.longitude)

                if (zoomToLocation) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(12f))
                    zoomToLocation = false
                }

                passengerMarker = googleMap.addMarker(
                    MarkerOptions().position(passengerLocation)
                        .title(getString(R.string.self_marker_title))
                )
            }
        }
    }

    private fun drawFoundDriverMarker(location: Location?) {
        if (this::googleMap.isInitialized) {
            foundDriverMarker?.remove()
            location?.let {
                val nearestDriverLocation = LatLng(it.latitude, it.longitude)

                foundDriverMarker = googleMap.addMarker(
                    MarkerOptions().position(nearestDriverLocation)
                        .title(
                            getString(
                                R.string.driver_marker_title,
                                viewModel.driverInfo!!.name
                            )
                        )
                        .icon(
                            BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                )
            }
        }
    }

}