package sibu.parking.firebase

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import sibu.parking.MainActivity
import sibu.parking.firebase.FirebaseCouponService
import sibu.parking.model.Cart
import sibu.parking.model.PaymentMethod
import java.util.UUID

/**
 * Service to handle Stripe payment integration
 */
class StripeService(private val context: Context) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val couponService = FirebaseCouponService()
    
    // You should store these values securely - for demo purposes only
    private val stripePublishableKey = "pk_test_51RNAfPPaUeHHPSQZ601pTlS2XpXdfzlbGqgp2m7j8w5ONIC3XwIuhT6FM1yhkFzbotI3SzIskhO1zXtY0wQMuhhB002LWtnMmZ"
    
    // Backend API URL - Replace with your server URL
    private val backendUrl = "https://console.firebase.google.com/project/digital-parking-coupon/firestore/databases/create-payment-intent"
    
    // Stripe payment sheet
    private lateinit var paymentSheet: PaymentSheet
    private var paymentIntentClientSecret: String? = null
    private var currentCart: Cart? = null
    
    init {
        // Initialize PaymentConfiguration with publishable key
        PaymentConfiguration.init(context, stripePublishableKey)
    }
    
    /**
     * Initialize payment flow
     */
    fun initializePaymentSheet(cart: Cart) {
        paymentSheet = PaymentSheet(context as MainActivity, ::onPaymentSheetResult)
        currentCart = cart
    }
    
    /**
     * Create a payment intent via backend server or Firebase Functions
     */
    suspend fun createPaymentIntent(cart: Cart): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Payment intent data
            val amount = (cart.getTotalPrice() * 100).toInt() // Convert to cents
            val currency = "myr"
            val paymentMethodTypes = listOf("card", "fpx")
            
            val paymentData = PaymentIntentData(
                amount = amount,
                currency = currency,
                paymentMethodTypes = paymentMethodTypes,
                metadata = mapOf(
                    "userId" to userId,
                    "cartItemCount" to cart.items.size.toString()
                )
            )
            
            // For demo purposes, you can use a Firebase Function or a real backend
            // Option 1: Using Firebase Functions
            val transactionId = UUID.randomUUID().toString()
            val docRef = firestore.collection("payment_intents").document(transactionId)
            
            val paymentIntentData = hashMapOf(
                "id" to transactionId,
                "userId" to userId,
                "amount" to amount,
                "currency" to currency,
                "status" to "pending",
                "timestamp" to System.currentTimeMillis()
            )
            
            docRef.set(paymentIntentData).await()
            
            // Option 2: Using a backend server (commented out for now)
            /*
            val (_, _, result) = Fuel.post(backendUrl)
                .jsonBody(Json.encodeToString(paymentData))
                .awaitStringResponseResult()
                
            val clientSecret = result.fold(
                success = { data ->
                    val jsonData = JSONObject(data)
                    jsonData.getString("clientSecret")
                },
                failure = { error ->
                    return Result.failure(Exception("Payment intent creation failed: ${error.message}"))
                }
            )
            */
            
            // Mock client secret for demo (in a real app, this would come from your backend)
            val clientSecret = "pi_mock_secret_${UUID.randomUUID()}_secret_mock"
            paymentIntentClientSecret = clientSecret
            
            Result.success(clientSecret)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Present the payment sheet to the user
     */
    fun presentPaymentSheet() {
        val paymentIntentClientSecret = paymentIntentClientSecret ?: return
        
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "SibuParking",
            allowsDelayedPaymentMethods = true
        )
        
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret,
            configuration
        )
    }
    
    /**
     * Handle payment sheet result
     */
    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                // Payment completed, process the purchase
                processPurchase()
            }
            is PaymentSheetResult.Canceled -> {
                // User canceled the payment
                (context as MainActivity).onStripePaymentCanceled()
            }
            is PaymentSheetResult.Failed -> {
                // Payment failed
                (context as MainActivity).onStripePaymentFailed(result.error)
            }
        }
    }
    
    /**
     * Process purchase after successful payment
     */
    private fun processPurchase() {
        currentCart?.let { cart ->
            (context as MainActivity).lifecycleScope.launchWhenCreated {
                val result = couponService.purchaseCoupons(
                    cart = cart,
                    paymentMethod = PaymentMethod.ONLINE_BANKING
                )
                
                if (result.isSuccess) {
                    updatePaymentStatusInFirestore(true)
                    (context as MainActivity).onStripePaymentCompleted()
                } else {
                    updatePaymentStatusInFirestore(false)
                    (context as MainActivity).onStripePaymentFailed(
                        Exception(result.exceptionOrNull()?.message ?: "Purchase failed")
                    )
                }
            }
        }
    }
    
    /**
     * Update payment status in Firestore
     */
    private suspend fun updatePaymentStatusInFirestore(isSuccessful: Boolean) {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            // Create a payment record
            val paymentId = UUID.randomUUID().toString()
            val paymentData = hashMapOf(
                "id" to paymentId,
                "userId" to userId,
                "amount" to (currentCart?.getTotalPrice() ?: 0.0),
                "status" to if (isSuccessful) "completed" else "failed",
                "paymentMethod" to "stripe",
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("payments").document(paymentId)
                .set(paymentData)
                .await()
                
        } catch (e: Exception) {
            // Log error but continue
            e.printStackTrace()
        }
    }
    
    /**
     * Handle payment result from activity resume
     */
    fun handlePaymentResult(onSuccess: () -> Unit, onFailure: () -> Unit) {
        // Check if there's a pending payment that needs to be processed
        val pendingPaymentIntent = paymentIntentClientSecret
        if (pendingPaymentIntent != null && currentCart != null) {
            // Check payment status from Firestore or your backend
            checkPaymentStatus(pendingPaymentIntent) { isSuccessful ->
                if (isSuccessful) {
                    onSuccess()
                } else {
                    onFailure()
                }
                // Clear pending payment data
                paymentIntentClientSecret = null
            }
        }
    }
    
    /**
     * Check payment status from Firestore or backend
     */
    private fun checkPaymentStatus(clientSecret: String, onResult: (Boolean) -> Unit) {
        // In a real implementation, you would check with your backend or Stripe API
        // For demo purposes, we'll use the payment_intents collection in Firestore
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(false)
            return
        }
        
        firestore.collection("payment_intents")
            .whereEqualTo("userId", userId)
            .whereGreaterThan("timestamp", System.currentTimeMillis() - 3600000) // Last hour
            .get()
            .addOnSuccessListener { documents ->
                val recentPayments = documents.filter { doc ->
                    doc.getString("status") == "completed"
                }
                onResult(recentPayments.isNotEmpty())
            }
            .addOnFailureListener {
                onResult(false)
            }
    }
    
    /**
     * Data class for payment intent request
     */
    @Serializable
    data class PaymentIntentData(
        val amount: Int,
        val currency: String,
        val paymentMethodTypes: List<String>,
        val metadata: Map<String, String>
    )
    
    /**
     * Test Stripe connection
     * @return Result object with connection status
     */
    fun testStripeConnection(): Result<Boolean> {
        return try {
            // Verify that PaymentConfiguration is initialized
            val publishableKey = PaymentConfiguration.getInstance(context).publishableKey
            
            if (publishableKey.isNotBlank() && publishableKey == stripePublishableKey) {
                // Log success for debugging
                android.util.Log.d("StripeService", "Stripe SDK initialized successfully with key: ${publishableKey.take(10)}...")
                Result.success(true)
            } else {
                // Log error for debugging
                android.util.Log.e("StripeService", "Stripe SDK initialization issue - key mismatch or empty")
                Result.failure(Exception("Stripe SDK initialization issue - key mismatch or empty"))
            }
        } catch (e: Exception) {
            // Log error for debugging
            android.util.Log.e("StripeService", "Stripe connection test failed: ${e.message}")
            Result.failure(e)
        }
    }
} 