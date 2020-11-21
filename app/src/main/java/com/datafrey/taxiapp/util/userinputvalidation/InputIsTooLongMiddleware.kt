package com.datafrey.taxiapp.util.userinputvalidation

class InputIsTooLongMiddleware(
    private val maximumLength: Int
) : InputValidatorMiddleware() {

    override fun check(input: String): InputValidationResult {
        if (input.length > maximumLength) {
            return InputValidationResult.INPUT_IS_TOO_LONG
        }

        return checkNext(input)
    }
}