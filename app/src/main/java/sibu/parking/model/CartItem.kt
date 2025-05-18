package sibu.parking.model

import java.text.NumberFormat
import java.util.Locale

data class CartItem(
    val couponType: CouponType,
    var quantity: Int = 1
) {
    fun getDisplayName(): String {
        return when(couponType) {
            CouponType.MINUTES_30 -> "30 Minutes Coupon"
            CouponType.HOUR_1 -> "1 Hour Coupon"
            CouponType.HOURS_2 -> "2 Hours Coupon"
            CouponType.HOURS_24 -> "24 Hours Coupon"
        }
    }
    
    fun getUnitPrice(): Double {
        return when(couponType) {
            CouponType.MINUTES_30 -> 4.25
            CouponType.HOUR_1 -> 8.50
            CouponType.HOURS_2 -> 16.95
            CouponType.HOURS_24 -> 63.60
        }
    }
    
    fun getTotalPrice(): Double {
        return getUnitPrice() * quantity
    }
    
    fun getFormattedUnitPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        return format.format(getUnitPrice())
    }
    
    fun getFormattedTotalPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        return format.format(getTotalPrice())
    }
}

data class Cart(
    val items: MutableList<CartItem> = mutableListOf()
) {
    fun getTotalPrice(): Double {
        return items.sumOf { it.getTotalPrice() }
    }
    
    fun getFormattedTotalPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        return format.format(getTotalPrice())
    }
    
    fun isEmpty(): Boolean {
        return items.isEmpty()
    }
    
    fun addItem(couponType: CouponType) {
        val existingItem = items.find { it.couponType == couponType }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            items.add(CartItem(couponType))
        }
    }
    
    fun removeItem(index: Int) {
        if (index in items.indices) {
            items.removeAt(index)
        }
    }
    
    fun updateQuantity(index: Int, quantity: Int) {
        if (index in items.indices && quantity > 0) {
            items[index].quantity = quantity
        }
    }
    
    fun clear() {
        items.clear()
    }
} 