package com.alvarez.furnivisionapp.data

import java.util.Date

data class Order(var userId: String, var furnitures: MutableList<String>, var date: Date)
