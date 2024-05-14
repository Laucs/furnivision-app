package com.alvarez.furnivisionapp.data

data class CartItem(val furniture: Furniture, var count: Int, var isSelected: Boolean = true)
