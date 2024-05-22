package com.alvarez.furnivisionapp.data

import java.util.Date

data class UserAccount(var userID: String, var name: String, var gender: String, var birthDate: Date, var phone: String, var email: String, val deliveryAddresses: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserAccount

        if (userID != other.userID) return false
        if (name != other.name) return false
        if (gender != other.gender) return false
        if (birthDate != other.birthDate) return false
        if (phone != other.phone) return false
        if (email != other.email) return false
        if (!deliveryAddresses.contentEquals(other.deliveryAddresses)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userID.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + birthDate.hashCode()
        result = 31 * result + phone.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + deliveryAddresses.contentHashCode()
        return result
    }
}