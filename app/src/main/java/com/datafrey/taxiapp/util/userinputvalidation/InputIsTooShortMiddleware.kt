package com.datafrey.taxiapp.util.userinputvalidation

class InputIsTooShortMiddleware(
    private val minimumLength: Int
) : InputValidatorMiddleware() {

    override fun check(input: String): InputValidationResult {
        if (input.length < minimumLength) {
            return InputValidationResult.INPUT_IS_TOO_SHORT
        }

        return checkNext(input)
    }
}