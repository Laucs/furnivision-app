package com.alvarez.furnivisionapp.data

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Order(var id: String?, var userId: String?, var itemShops: MutableList<ShopCart>?, var totalPrice: Double?, var date: Date?, var arrival: Date?) {
    constructor() : this(null, null, mutableListOf(), 0.0, Date(), Date())
}