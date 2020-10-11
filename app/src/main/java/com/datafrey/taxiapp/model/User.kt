package com.datafrey.taxiapp.model

import com.google.firebase.database.Exclude

data class User(
    val id: String? = null,
    val name: String? = null,
    val participatesInOrderNow: Boolean = false
) {

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "participatesInOrderNow" to participatesInOrderNow
        )
    }

}