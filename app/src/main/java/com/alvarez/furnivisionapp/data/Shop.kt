package com.alvarez.furnivisionapp.data

data class Shop(val id: String?, val logo: Int, val name: String?, val description: String?, val furnitures: Array<Furniture>, val reviews: Double, val views: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shop

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false
        if (!furnitures.contentEquals(other.furnitures)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + furnitures.contentHashCode()
        return result
    }
}