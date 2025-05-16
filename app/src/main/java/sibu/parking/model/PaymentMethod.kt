package sibu.parking.model

enum class PaymentMethod {
    ONLINE_BANKING,
    E_WALLET,
    CREDIT_CARD,
    STRIPE
}

fun PaymentMethod.getDisplayName(): String {
    return when (this) {
        PaymentMethod.ONLINE_BANKING -> "ONLINE BANKING (FPX)"
        PaymentMethod.E_WALLET -> "E-WALLET"
        PaymentMethod.CREDIT_CARD -> "CREDIT/DEBIT CARD"
        PaymentMethod.STRIPE -> "STRIPE PAYMENT"
    }
} 