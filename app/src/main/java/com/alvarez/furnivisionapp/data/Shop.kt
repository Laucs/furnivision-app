package com.alvarez.furnivisionapp.data

data class Shop(var id: String? = null, val logo: String? = null, val name: String? = null, val description: String? = null,val slogan: String? = null,  val reviews: Double? = null, val views: Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Shop

        if (id != other.id) return false
        if (name != other.name) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}