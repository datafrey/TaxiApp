package com.datafrey.taxiapp.userinputvalidation

class InputIsEmptyMiddleware : InputValidatorMiddleware() {

    override fun check(input: String): InputValidationResult {
        if (input.isEmpty()) {
            return InputValidationResult.INPUT_IS_EMPTY
        }

        return checkNext(input)
    }

}