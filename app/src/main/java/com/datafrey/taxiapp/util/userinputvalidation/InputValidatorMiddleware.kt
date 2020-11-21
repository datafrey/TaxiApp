package com.datafrey.taxiapp.util.userinputvalidation

abstract class InputValidatorMiddleware {

    private var next: InputValidatorMiddleware? = null

    fun linkWith(next: InputValidatorMiddleware): InputValidatorMiddleware {
        this.next = next
        return next
    }

    abstract fun check(input: String): InputValidationResult

    protected fun checkNext(input: String): InputValidationResult {
        if (next == null) {
            return InputValidationResult.OK
        }

        return next!!.check(input)
    }
}