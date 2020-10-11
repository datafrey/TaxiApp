package com.datafrey.taxiapp.drivermaps

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.datafrey.taxiapp.currentuserlocationreceiving.CurrentUserLocationReceiverViewModel
import com.datafrey.taxiapp.model.DatabaseNodeNames
import com.datafrey.taxiapp.model.Order
import com.datafrey.taxiapp.model.User
import com.datafrey.taxiapp.util.sendCustomerAppearedNotification
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverMapsViewModel(application: Application) :
    CurrentUserLocationReceiverViewModel(application) {

    private var showNotifications = false

    fun allowNotifications() {
        showNotifications = true
    }

    fun disableNotifications() {
        showNotifications = false
    }

    private val driverId = FirebaseAuth.getInstance().currentUser?.uid.toString()

    private val driverReference = FirebaseDatabase.getInstance()
        .reference.child(DatabaseNodeNames.USERS_NODE_NAME).child(driverId)

    private val driversGeoFireReference = FirebaseDatabase.getInstance()
        .reference.child(DatabaseNodeNames.DRIVERS_GEOFIRE_NODE_NAME)

    private var ordersReference = FirebaseDatabase.getInstance()
        .reference.child(DatabaseNodeNames.ORDERS_NODE_NAME)

    private var ordersChildEventListener: ChildEventListener? = null
    private var orderReference: DatabaseReference? = null

    private var passengerLocationReference: DatabaseReference? = null
    private var passengerLocationValueEventListener: ValueEventListener? = null

    var passengerInfo: User? = null
        private set

    private val _driverCurrentLocation = MutableLiveData<Location?>()
    val driverCurrentLocation: LiveData<Location?>
        get() = _driverCurrentLocation

    private val _passengerCurrentLocation = MutableLiveData<Location?>()
    val passengerCurrentLocation: LiveData<Location?>
        get() = _passengerCurrentLocation

    private val _orderExecutionEnded = MutableLiveData(false)
    val orderExecutionEnded: LiveData<Boolean>
        get() = _orderExecutionEnded

    fun orderExecutionEndedMessageShown() {
        _orderExecutionEnded.value = false
    }

    private val _refuseOrderButtonEnabled = MutableLiveData(false)
    val refuseOrderButtonEnabled: LiveData<Boolean>
        get() = _refuseOrderButtonEnabled

    init {
        startCheckingOrders()
    }

    override fun buildCurrentUserLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                _driverCurrentLocation.value = p0!!.lastLocation
                updateDriverLocationInDatabase()
            }
        }
    }

    private fun updateDriverLocationInDatabase() {
        if (_driverCurrentLocation.value != null) {
            val geoFire = GeoFire(driversGeoFireReference)
            geoFire.setLocation(
                driverId, GeoLocation(
                    _driverCurrentLocation.value!!.latitude,
                    _driverCurrentLocation.value!!.longitude
                )
            ) { key, error -> /* An onCompleteListener just to make it work. */ }
        }
    }

    private fun startCheckingOrders() {
        ordersChildEventListener = object : ChildEventListener {
            @Suppress("NAME_SHADOWING")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val order = snapshot.getValue(Order::class.java)
                order?.let { order ->
                    if (order.driverId == driverId) {
                        orderReference = snapshot.ref
                        _refuseOrderButtonEnabled.postValue(true)
                        setParticipationInOrderStatus(true)

                        if (showNotifications) {
                            val notificationManager = ContextCompat.getSystemService(
                                getApplication(), NotificationManager::class.java
                            ) as NotificationManager
                            notificationManager.sendCustomerAppearedNotification(getApplication())
                        } else {
                            notifyAboutStartedOrderWithoutNotification()
                        }

                        getPassengerInfo(order.passengerId!!)

                        passengerLocationReference = FirebaseDatabase.getInstance()
                            .reference.child(DatabaseNodeNames.PASSENGERS_GEOFIRE_NODE_NAME)
                            .child(order.passengerId)
                            .child(DatabaseNodeNames.LOCATION_GEOFIRE_NODE_NAME)

                        startGettingPassengerLocation()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Order::class.java)
                order?.let {
                    if (order.orderId == orderReference?.key) {
                        setParticipationInOrderStatus(false)
                        closeOrderIfStarted()
                        _orderExecutionEnded.postValue(true)
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        ordersReference.addChildEventListener(ordersChildEventListener!!)
    }

    private fun removeOrder() {
        orderReference?.removeValue()
        orderReference = null
    }

    private fun notifyAboutStartedOrderWithoutNotification() {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(getApplication(), notification)
        ringtone.play()

        val vibrator = (getApplication() as Context)
            .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect
                    .createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun getPassengerInfo(passengerId: String) {
        FirebaseDatabase.getInstance().reference
            .child(DatabaseNodeNames.USERS_NODE_NAME)
            .child(passengerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    passengerInfo = snapshot.getValue(User::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun setParticipationInOrderStatus(status: Boolean) {
        val userOrderInfoUpdate = HashMap<String, Any?>()
        userOrderInfoUpdate["participatesInOrderNow"] = status
        driverReference.updateChildren(userOrderInfoUpdate)
    }

    private fun startGettingPassengerLocation() {
        passengerLocationValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != null) {
                    val passengerLocationParameters = snapshot.value as List<Any?>

                    val (latitude, longitude) = passengerLocationParameters.map {
                        it?.toString()?.toDouble() ?: 0.0
                    }

                    val passengerLocation = Location("").also {
                        it.latitude = latitude
                        it.longitude = longitude
                    }

                    _passengerCurrentLocation.value = passengerLocation
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        passengerLocationReference!!.addValueEventListener(
            passengerLocationValueEventListener!!
        )
    }

    private fun cleanPassengerData() {
        passengerLocationValueEventListener?.let {
            passengerLocationReference?.removeEventListener(it)
        }
        passengerLocationReference = null
        _passengerCurrentLocation.value = null
    }

    fun closeOrderIfStarted() {
        cleanPassengerData()

        removeOrder()
        setParticipationInOrderStatus(false)

        _refuseOrderButtonEnabled.postValue(false)
    }

    private fun cleanSelfGeoData() {
        val geoFire = GeoFire(driversGeoFireReference)
        geoFire.removeLocation(driverId) { key, error ->
            /* An onCompleteListener just to make it work. */
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeOrderIfStarted()
        cleanSelfGeoData()
    }

}