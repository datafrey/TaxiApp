package com.datafrey.taxiapp.util.userinputvalidation

class InputIsNotEqualToStringMiddleware(
    private val mustBeEqualTo: String
) : InputValidatorMiddleware() {

    override fun check(input: String): InputValidationResult {
        if (input != mustBeEqualTo) {
            return InputValidationResult.INPUT_IS_NOT_EQUAL_TO_STRING
        }

        return checkNext(input)
    }
}