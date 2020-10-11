package com.datafrey.taxiapp.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, duration).show()

fun Context.toast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, duration).show()

val EditText.data get() = text.toString().trim()

inline fun <reified A : Activity> Context.startActivity(intent: Intent.() -> Unit = {}) {
    startActivity(Intent(this, A::class.java).apply(intent))
}

inline fun <reified A : Activity> Fragment.startActivity(intent: Intent.() -> Unit = {}) {
    startActivity(Intent(requireContext(), A::class.java).apply(intent))
}

fun AndroidViewModel.stringResourceToString(
    @StringRes stringResource: Int,
    additionalParameter: Any? = null
) = (getApplication() as Context).getString(stringResource, additionalParameter)