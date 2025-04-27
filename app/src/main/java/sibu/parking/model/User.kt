    package sibu.parking.model

enum class UserType {
    USER,
    STAFF
}

data class User(
    val id: String = "",
    val email: String = "",
    val phone: String? = null,
    val username: String? = null,
    val userType: UserType = UserType.USER
) 