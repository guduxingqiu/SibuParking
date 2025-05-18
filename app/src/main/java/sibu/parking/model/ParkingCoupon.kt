package sibu.parking.model

enum class CouponType {
    MINUTES_30,
    HOUR_1,
    HOURS_2,
    HOURS_24
}

data class ParkingCoupon(
    val id: String = "",
    val userId: String = "",
    val type: CouponType = CouponType.HOUR_1,
    val remainingUses: Int = 10,
    val purchaseDate: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String {
        return when(type) {
            CouponType.MINUTES_30 -> "30 Minutes Coupon"
            CouponType.HOUR_1 -> "1 Hour Coupon"
            CouponType.HOURS_2 -> "2 Hours Coupon"
            CouponType.HOURS_24 -> "24 Hours Coupon"
        }
    }
    
    fun getDescription(): String {
        return "Remaining uses: $remainingUses"
    }
}

data class Vehicle(
    val id: String = "",
    val userId: String = "",
    val licensePlate: String = "",
    val isFavorite: Boolean = false
)

data class ParkingArea(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val isFavorite: Boolean = false
)