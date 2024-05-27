package com.alvarez.furnivisionapp.data

data class DeliveryAddress(
    var name: String = "",
    var phone: String = "",
    var region: String = "",
    var barangay: String = "",
    var streetName: String = "",
    var postalCode: String = "",
    var isChecked: Boolean = false
) {
    // No-argument constructor
    constructor() : this("", "", "", "", "", "", false)
}
