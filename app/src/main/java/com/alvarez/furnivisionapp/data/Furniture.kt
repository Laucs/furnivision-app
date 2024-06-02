package com.alvarez.furnivisionapp.data

data class Furniture(
    var id: String? = null,
    val img: String? = null,
    val name: String? = null,
    val description: String? = null,
    val price: Double? = 0.00,
    val dimensions: String? = null,
    val stocks: Int? = null,
    val rating: Double? = 0.0,
    val sold: Int? = 0,
    val shopID: String? = null,
    val colors: List<String>? = null) {
    constructor() : this(null, null, null, null, 0.00, null, null, 0.0, 0, null, null)
}
