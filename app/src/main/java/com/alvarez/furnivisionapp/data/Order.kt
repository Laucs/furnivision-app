package com.alvarez.furnivisionapp.data

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

data class Order(var id: String, var userId: String, var items: MutableList<ShopCart>, var date: Date)