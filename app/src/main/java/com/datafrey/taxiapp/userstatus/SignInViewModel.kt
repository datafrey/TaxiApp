package com.datafrey.taxiapp.userstatus

import android.app.Application
import android.view.View
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.datafrey.taxiapp.R
import com.datafrey.taxiapp.model.DatabaseNodeNames
import com.datafrey.taxiapp.model.User
import com.datafrey.taxiapp.userinputvalidation.*
import com.datafrey.taxiapp.util.stringResourceToString
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignInViewModel(application: Application) : AndroidViewModel(application) {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val usersReference by lazy {
        FirebaseDatabase.getInstance().reference
            .child(DatabaseNodeNames.USERS_NODE_NAME)
    }

    private var signInModeActive = true

    private val _textInputConfirmPasswordVisibility = MutableLiveData(View.GONE)
    val textInputConfirmPasswordVisibility: LiveData<Int>
        get() = _textInputConfirmPasswordVisibility

    private val _textInputNameVisibility = MutableLiveData(View.GONE)
    val textInputNameVisibility: LiveData<Int>
        get() = _textInputNameVisibility

    private val _signInSignUpButtonText = MutableLiveData(R.string.sign_in_mode_button_text)
    val signInSignUpButtonText: LiveData<String>
        get() = Transformations.map(_signInSignUpButtonText) {
            stringResourceToString(it)
        }

    private val _toggleSignInSignUpButtonText =
        MutableLiveData(R.string.switch_to_sign_up_mode_button_text)
    val toggleSignInSignUpButtonText: LiveData<String>
        get() = Transformations.map(_toggleSignInSignUpButtonText) {
            stringResourceToString(it)
        }

    fun toggleAuthorizationMode() {
        if (signInModeActive) {
            activateSignUpMode()
        } else {
            activateSignInMode()
        }
    }

    private fun activateSignInMode() {
        signInModeActive = true
        _textInputConfirmPasswordVisibility.value = View.GONE
        _textInputNameVisibility.value = View.GONE
        _signInSignUpButtonText.value = R.string.sign_in_mode_button_text
        _toggleSignInSignUpButtonText.value = R.string.switch_to_sign_up_mode_button_text
    }

    private fun activateSignUpMode() {
        signInModeActive = false
        _textInputConfirmPasswordVisibility.value = View.VISIBLE
        _textInputNameVisibility.value = View.VISIBLE
        _signInSignUpButtonText.value = R.string.sign_up_mode_button_text
        _toggleSignInSignUpButtonText.value = R.string.switch_to_sign_in_mode_button_text
    }

    private val _signInSuccessful = MutableLiveData<Boolean>()
    val signInSuccessful: LiveData<Boolean>
        get() = _signInSuccessful

    fun uiReactedToAuthorizationAttempt() {
        _signInSuccessful.value = null
    }

    private val _userEmailInputErrorMessage = MutableLiveData(R.string.empty_error_message)
    val userEmailInputErrorMessage: LiveData<String>
        get() = Transformations.map(_userEmailInputErrorMessage) {
            stringResourceToString(it)
        }

    private val _userPasswordInputErrorMessage = MutableLiveData(R.string.empty_error_message)
    val userPasswordInputErrorMessage: LiveData<String>
        get() = Transformations.map(_userPasswordInputErrorMessage) {
            stringResourceToString(it)
        }

    private val _userConfirmPasswordInputErrorMessage =
        MutableLiveData(R.string.empty_error_message)
    val userConfirmPasswordInputErrorMessage: LiveData<String>
        get() = Transformations.map(_userConfirmPasswordInputErrorMessage) {
            stringResourceToString(it)
        }

    private val _userNameInputErrorMessage = MutableLiveData(R.string.empty_error_message)
    val userNameInputErrorMessage: LiveData<String>
        get() = Transformations.map(_userNameInputErrorMessage) {
            stringResourceToString(it)
        }

    private val _authorizationProgressBarVisibility = MutableLiveData(View.GONE)
    val authorizationProgressBarVisibility: LiveData<Int>
        get() = _authorizationProgressBarVisibility

    fun authorize(email: String, password: String, confirmPassword: String, name: String) {
        if (signInModeActive) {
            signInToFirebase(email, password)
        } else {
            signUpToFirebase(email, password, confirmPassword, name)
        }
    }

    private fun signInToFirebase(email: String, password: String) {
        if (signInInputIsValid(email, password)) {
            _authorizationProgressBarVisibility.postValue(View.VISIBLE)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _signInSuccessful.value = task.isSuccessful
                    _authorizationProgressBarVisibility.postValue(View.GONE)
                }
        }
    }

    private fun signUpToFirebase(
        email: String, password: String,
        confirmPassword: String, name: String
    ) {
        if (signUpInputIsValid(email, password, confirmPassword, name)) {
            _authorizationProgressBarVisibility.postValue(View.VISIBLE)

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val currentFirebaseUser = auth.currentUser!!
                        val newUser = User(currentFirebaseUser.uid, name)
                        usersReference.child(currentFirebaseUser.uid).setValue(newUser)
                    }

                    _signInSuccessful.value = task.isSuccessful
                    _authorizationProgressBarVisibility.postValue(View.GONE)
                }
        }
    }

    private fun signInInputIsValid(email: String, password: String): Boolean {
        val emailValidator = InputIsEmptyMiddleware()
        val passwordValidator = InputIsEmptyMiddleware()
        passwordValidator.linkWith(InputIsTooShortMiddleware(7))

        val emailInputValidationResult = emailValidator.check(email)
        val passwordInputValidationResult = passwordValidator.check(password)

        _userEmailInputErrorMessage.value = when (emailInputValidationResult) {
            InputValidationResult.OK -> R.string.empty_error_message
            InputValidationResult.INPUT_IS_EMPTY -> R.string.empty_email_field_error_message
            else -> null
        }

        _userPasswordInputErrorMessage.value = when (passwordInputValidationResult) {
            InputValidationResult.OK -> R.string.empty_error_message
            InputValidationResult.INPUT_IS_EMPTY -> R.string.empty_password_field_error_message
            InputValidationResult.INPUT_IS_TOO_SHORT -> R.string.too_short_password_error_message
            else -> null
        }

        return emailInputValidationResult == InputValidationResult.OK &&
                passwordInputValidationResult == InputValidationResult.OK
    }

    private fun signUpInputIsValid(
        email: String, password: String,
        confirmPassword: String, name: String
    ): Boolean {
        val emailAndPasswordAreValid = signInInputIsValid(email, password)

        val confirmPasswordValidator = InputIsEmptyMiddleware()
        confirmPasswordValidator.linkWith(InputIsNotEqualToStringMiddleware(password))
        val nameValidator = InputIsEmptyMiddleware()
        nameValidator.linkWith(InputIsTooLongMiddleware(15))

        val confirmPasswordInputValidationResult = confirmPasswordValidator.check(confirmPassword)
        val nameInputValidationResult = nameValidator.check(name)

        _userConfirmPasswordInputErrorMessage.value = when (confirmPasswordInputValidationResult) {
            InputValidationResult.OK -> R.string.empty_error_message
            InputValidationResult.INPUT_IS_EMPTY -> R.string.empty_confirm_password_field_error_message
            InputValidationResult.INPUT_IS_NOT_EQUAL_TO_STRING -> R.string.passwords_dont_match_error_message
            else -> null
        }

        _userNameInputErrorMessage.value = when (nameInputValidationResult) {
            InputValidationResult.OK -> R.string.empty_error_message
            InputValidationResult.INPUT_IS_EMPTY -> R.string.empty_name_field_error_message
            InputValidationResult.INPUT_IS_TOO_LONG -> R.string.too_long_name_error_message
            else -> null
        }

        return emailAndPasswordAreValid &&
                confirmPasswordInputValidationResult == InputValidationResult.OK &&
                nameInputValidationResult == InputValidationResult.OK
    }

}