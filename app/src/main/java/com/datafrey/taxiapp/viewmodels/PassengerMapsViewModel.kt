package com.datafrey.taxiapp.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.model.DatabaseNodeNames
import com.datafrey.taxiapp.model.Order
import com.datafrey.taxiapp.model.User
import com.datafrey.taxiapp.util.stringResourceToString
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.roundToInt

class PassengerMapsViewModel(application: Application) :
    CurrentUserLocationReceiverViewModel(application) {

    private val passengerId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private val passengerUserReference = FirebaseDatabase.getInstance()
        .reference.child(DatabaseNodeNames.USERS_NODE_NAME).child(passengerId)

    private val passengersGeoFireReference = FirebaseDatabase.getInstance()
        .reference.child(DatabaseNodeNames.PASSENGERS_GEOFIRE_NODE_NAME)

    private val driversGeoFireReference by lazy {
        FirebaseDatabase.getInstance().reference
            .child(DatabaseNodeNames.DRIVERS_GEOFIRE_NODE_NAME)
    }

    private var searchRadius = 1.0
    private var isDriverFound = false
    private var searchDriverGeoQuery: GeoQuery? = null
    private var searchDriverGeoQueryEventListener: GeoQueryEventListener? = null

    private var foundDriverLocationReference: DatabaseReference? = null
    private var foundDriverLocationValueEventListener: ValueEventListener? = null
    private var distanceToDriver: Int? = null

    var driverInfo: User? = null
        private set

    private var orderIsActive = false
    private var orderReference: DatabaseReference? = null
    private var orderValueEventListener: ValueEventListener? = null

    private val _passengerCurrentLocation = MutableLiveData<Location?>()
    val passengerCurrentLocation: LiveData<Location?>
        get() = _passengerCurrentLocation

    private val _foundDriverCurrentLocation = MutableLiveData<Location?>()
    val foundDriverCurrentLocation: LiveData<Location?>
        get() = _foundDriverCurrentLocation

    private val _noDriversFound = MutableLiveData(false)
    val noDriversFound: LiveData<Boolean>
        get() = _noDriversFound

    fun noDriversFoundMessageShown() {
        _noDriversFound.value = false
    }

    private val _driverRefusedTheOrder = MutableLiveData(false)
    val driverRefusedTheOrder: LiveData<Boolean>
        get() = _driverRefusedTheOrder

    fun driverRefusedTheOrderMessageShown() {
        _driverRefusedTheOrder.value = false
    }

    private val _bookTaxiButtonText = MutableLiveData(R.string.book_taxi_button_default_text)
    val bookTaxiButtonText: LiveData<String>
        get() = Transformations.map(_bookTaxiButtonText) {
            stringResourceToString(it, distanceToDriver)
        }

    private val _cancelCloseOrderButtonText =
        MutableLiveData(R.string.cancel_close_order_button_cancel_text)
    val cancelCloseOrderButtonText: LiveData<String>
        get() = Transformations.map(_cancelCloseOrderButtonText) {
            stringResourceToString(it)
        }

    private val _bookTaxiButtonEnabled = MutableLiveData(true)
    val bookTaxiButtonEnabled: LiveData<Boolean>
        get() = _bookTaxiButtonEnabled

    private val _cancelCloseOrderButtonEnabled = MutableLiveData(false)
    val cancelCloseOrderButtonEnabled: LiveData<Boolean>
        get() = _cancelCloseOrderButtonEnabled

    private fun setDefaultUiValues() {
        _bookTaxiButtonText.postValue(R.string.book_taxi_button_default_text)
        _cancelCloseOrderButtonText.postValue(R.string.cancel_close_order_button_cancel_text)
        _bookTaxiButtonEnabled.postValue(true)
        _cancelCloseOrderButtonEnabled.postValue(false)
    }

    override fun buildCurrentUserLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                _passengerCurrentLocation.value = p0!!.lastLocation
                updatePassengerLocationInDatabase()
            }
        }
    }

    private fun updatePassengerLocationInDatabase() {
        val geoFire = GeoFire(passengersGeoFireReference)
        geoFire.setLocation(
            passengerId, GeoLocation(
                _passengerCurrentLocation.value!!.latitude,
                _passengerCurrentLocation.value!!.longitude
            )
        ) { key, error -> /* An onCompleteListener just to make it work. */ }
    }

    fun findNearestTaxi() {
        _bookTaxiButtonText.postValue(R.string.book_taxi_button_getting_taxi_text)
        _cancelCloseOrderButtonText.postValue(R.string.cancel_close_order_button_cancel_text)
        _bookTaxiButtonEnabled.postValue(false)
        _cancelCloseOrderButtonEnabled.postValue(true)

        searchForDriverRecursively()
    }

    private fun searchForDriverRecursively() {
        val geoFire = GeoFire(driversGeoFireReference)
        searchDriverGeoQuery = geoFire.queryAtLocation(
            GeoLocation(
                _passengerCurrentLocation.value!!.latitude,
                _passengerCurrentLocation.value!!.longitude
            ), searchRadius
        )

        searchDriverGeoQueryEventListener = object : GeoQueryEventListener {
            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                if (!isDriverFound) {
                    isDriverFound = true
                    hireDriverIfPossible(key!!)
                }
            }

            override fun onKeyExited(key: String?) {
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {
            }

            override fun onGeoQueryReady() {
                if (searchRadius > 5) {
                    isDriverFound = false
                    _noDriversFound.postValue(true)

                    stopSearchingForDriver()
                    setDefaultUiValues()
                    return
                }

                if (!isDriverFound) {
                    searchRadius++
                    searchDriverGeoQuery!!.removeGeoQueryEventListener(this)
                    searchForDriverRecursively()
                }
            }

            override fun onGeoQueryError(error: DatabaseError?) {
            }
        }

        searchDriverGeoQuery!!.addGeoQueryEventListener(searchDriverGeoQueryEventListener)
    }

    private fun stopSearchingForDriver() {
        searchRadius = 1.0
        searchDriverGeoQueryEventListener?.let {
            searchDriverGeoQuery?.removeGeoQueryEventListener(it)
        }
        searchDriverGeoQueryEventListener = null
        searchDriverGeoQuery = null
    }

    private fun hireDriverIfPossible(driverId: String) {
        FirebaseDatabase.getInstance().reference
            .child(DatabaseNodeNames.USERS_NODE_NAME)
            .child(driverId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driver = snapshot.getValue(User::class.java)
                    driver?.let {
                        if (!driver.participatesInOrderNow) {
                            driverInfo = driver
                            stopSearchingForDriver()
                            setParticipationInOrderStatus(true)
                            orderIsActive = true
                            initializeOrder()
                            startGettingFoundDriverLocation()
                        } else {
                            isDriverFound = false
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun initializeOrder() {
        orderReference = FirebaseDatabase.getInstance()
            .reference.child(DatabaseNodeNames.ORDERS_NODE_NAME)
            .child(passengerId + driverInfo!!.id)

        val order = Order(
            orderReference!!.key.toString(),
            passengerId,
            driverInfo!!.id
        )

        orderReference!!.setValue(order)

        orderValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    closeOrderIfStarted()
                    _driverRefusedTheOrder.postValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        orderReference!!.addValueEventListener(orderValueEventListener!!)
    }

    private fun cleanOrderData() {
        orderValueEventListener?.let {
            orderReference?.removeEventListener(it)
        }
        orderReference?.removeValue()
        orderReference = null
    }

    private fun setParticipationInOrderStatus(status: Boolean) {
        val userOrderInfoUpdate = HashMap<String, Any?>()
        userOrderInfoUpdate["participatesInOrderNow"] = status
        passengerUserReference.updateChildren(userOrderInfoUpdate)
    }

    private fun startGettingFoundDriverLocation() {
        foundDriverLocationReference = driversGeoFireReference.child(driverInfo!!.id!!)
            .child(DatabaseNodeNames.LOCATION_GEOFIRE_NODE_NAME)

        foundDriverLocationValueEventListener = object : ValueEventListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists() && orderIsActive) {
                    val driverLocationParameters = snapshot.value as List<Any?>

                    val (latitude, longitude) = driverLocationParameters.map {
                        it?.toString()?.toDouble() ?: 0.0
                    }

                    val driverLocation = Location("").also {
                        it.latitude = latitude
                        it.longitude = longitude
                    }

                    _foundDriverCurrentLocation.postValue(driverLocation)

                    distanceToDriver = driverLocation.distanceTo(
                        _passengerCurrentLocation.value
                    ).roundToInt()

                    _bookTaxiButtonText.postValue(R.string.book_taxi_button_distance_to_driver_text)
                    _cancelCloseOrderButtonText.postValue(R.string.cancel_close_order_button_close_order_text)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        foundDriverLocationReference!!.addValueEventListener(
            foundDriverLocationValueEventListener!!
        )
    }

    private fun cleanFoundDriverData() {
        foundDriverLocationValueEventListener?.let {
            foundDriverLocationReference?.removeEventListener(it)
        }
        foundDriverLocationValueEventListener = null

        _foundDriverCurrentLocation.value = null
        distanceToDriver = null
    }

    fun closeOrderIfStarted() {
        stopSearchingForDriver()

        orderIsActive = false
        cleanOrderData()

        isDriverFound = false
        cleanFoundDriverData()

        setParticipationInOrderStatus(false)

        setDefaultUiValues()
    }

    private fun cleanSelfGeoData() {
        val geoFire = GeoFire(passengersGeoFireReference)
        geoFire.removeLocation(passengerId) { key, error ->
            /* An onCompleteListener just to make it work. */
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeOrderIfStarted()
        cleanSelfGeoData()
    }
}