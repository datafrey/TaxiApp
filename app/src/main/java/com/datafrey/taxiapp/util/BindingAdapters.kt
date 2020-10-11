package com.datafrey.taxiapp.util

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("errorMessage")
fun setErrorMessage(textInputLayout: TextInputLayout, message: String?) {
    textInputLayout.error = message
}