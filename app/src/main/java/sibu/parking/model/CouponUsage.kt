package sibu.parking.model

enum class CouponStatus {
    ACTIVE,
    EXPIRED
}

data class CouponUsage(
    val id: String = "",
    val couponId: String = "",
    val userId: String = "",
    val usedCount: Int = 1,
    val parkingArea: String = "",
    val parkingLotNumber: String = "",
    val vehicleNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: CouponStatus = CouponStatus.ACTIVE,
    val expirationTime: Long = 0 // 0 means no expiration
) {
    fun isExpired(): Boolean {
        if (expirationTime == 0L) return false
        return System.currentTimeMillis() > expirationTime
    }
} 