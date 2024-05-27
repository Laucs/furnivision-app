package com.alvarez.furnivisionapp.data

data class DeliveryAddress(
    val name: String,
    val phone: String,
    val region: String,
    val barangay: String,
    val streetName: String,
    val postalCode: String,
    val isDefault: Boolean
)