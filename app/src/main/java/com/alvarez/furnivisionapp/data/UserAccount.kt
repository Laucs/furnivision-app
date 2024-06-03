import com.alvarez.furnivisionapp.data.DeliveryAddress

data class UserAccount(
    var id: String? = null,
    var name: String? = null,
    var image: String? = null,
    var gender: String? = null,
    var birthday: String? = null,
    var phoneNumber: String? = null,
    var email: String? = null,
    val deliveryAddresses: MutableList<DeliveryAddress>? = null
) {
    constructor() : this(null, null, null, null, null, null, null, null)
}
