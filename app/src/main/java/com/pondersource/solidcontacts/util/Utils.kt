package com.pondersource.solidcontacts.util

fun isEmailValid(email: String): Boolean {
    val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
    return emailRegex.toRegex().matches(email)
}

fun isPhoneNumberValid(phoneNumber: String): Boolean {
    val phoneRegex = "^\\d{10}$"
    return phoneRegex.toRegex().matches(phoneNumber)
}