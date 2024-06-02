package com.alvarez.furnivisionapp.data

data class CartItem(val furniture: Furniture?, var quantity: Int? = 0)
{
    constructor() : this (null, 0)
}
