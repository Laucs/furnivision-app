package com.alvarez.furnivisionapp.data

data class Shop(val id: String?, val shopName: String?, val location: String?)
data class Furniture(val id: String?, val name: String?, val type: String?, val price: Double, val shop: Shop)