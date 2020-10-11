package com.datafrey.taxiapp.model

data class Order(
    val orderId: String? = null,
    val passengerId: String? = null,
    var driverId: String? = null
)