package sibu.parking.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import sibu.parking.model.User
import sibu.parking.model.Cart
import sibu.parking.model.CartItem
import sibu.parking.model.CouponType
import sibu.parking.model.CouponUsage
import sibu.parking.model.ParkingArea
import sibu.parking.model.ParkingCoupon
import sibu.parking.model.PaymentMethod
import sibu.parking.model.Vehicle
import java.util.UUID

class FirebaseCouponService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Get current user's coupons
    suspend fun getUserCoupons(): Flow<List<ParkingCoupon>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            
            val snapshot = firestore.collection("coupons")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("remainingUses", 0)
                .get()
                .await()
                
            val coupons = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ParkingCoupon::class.java)
            }
            
            emit(coupons)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Purchase coupons
    suspend fun purchaseCoupons(
        cart: Cart,
        paymentMethod: PaymentMethod
    ): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Simulate payment processing - in a real app, you would integrate with payment gateway
            // For now, we'll just create the coupons
            
            val batch = firestore.batch()
            
            // Add each coupon to the batch
            for (item in cart.items) {
                repeat(item.quantity) {
                    val coupon = ParkingCoupon(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        type = item.couponType,
                        remainingUses = 10,
                        purchaseDate = System.currentTimeMillis()
                    )
                    
                    val docRef = firestore.collection("coupons").document(coupon.id)
                    batch.set(docRef, coupon)
                }
            }
            
            // Record the purchase transaction
            val transactionId = UUID.randomUUID().toString()
            val transaction = hashMapOf(
                "id" to transactionId,
                "userId" to userId,
                "items" to cart.items.map { 
                    hashMapOf(
                        "couponType" to it.couponType.name,
                        "quantity" to it.quantity,
                        "unitPrice" to it.getUnitPrice(),
                        "totalPrice" to it.getTotalPrice()
                    )
                },
                "totalAmount" to cart.getTotalPrice(),
                "paymentMethod" to paymentMethod.name,
                "timestamp" to System.currentTimeMillis()
            )
            
            val transactionRef = firestore.collection("transactions").document(transactionId)
            batch.set(transactionRef, transaction)
            
            // Commit all operations
            batch.commit().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Use a coupon
    suspend fun useCoupon(
        couponId: String, 
        usedCount: Int,
        parkingArea: String,
        parkingLotNumber: String,
        vehicleNumber: String
    ): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            // Get the coupon
            val couponDoc = firestore.collection("coupons").document(couponId).get().await()
            val coupon = couponDoc.toObject(ParkingCoupon::class.java) 
                ?: return Result.failure(Exception("Coupon not found"))
                
            if (coupon.remainingUses < usedCount) {
                return Result.failure(Exception("Not enough remaining uses"))
            }
            
            // Update the coupon
            val newRemainingUses = coupon.remainingUses - usedCount
            firestore.collection("coupons").document(couponId)
                .update("remainingUses", newRemainingUses)
                .await()
                
            // Record the usage
            val usage = CouponUsage(
                id = UUID.randomUUID().toString(),
                couponId = couponId,
                userId = userId,
                usedCount = usedCount,
                parkingArea = parkingArea,
                parkingLotNumber = parkingLotNumber,
                vehicleNumber = vehicleNumber,
                timestamp = System.currentTimeMillis()
            )
            
            firestore.collection("couponUsages").document(usage.id)
                .set(usage)
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check coupon by vehicle number (for staff)
    suspend fun checkCouponByVehicle(vehicleNumber: String): Result<Pair<List<ParkingCoupon>, List<CouponUsage>>> {
        return try {
            // Get usage history for this vehicle
            val usageSnapshot = firestore.collection("couponUsages")
                .whereEqualTo("vehicleNumber", vehicleNumber)
                .get()
                .await()
                
            val usages = usageSnapshot.documents.mapNotNull { doc ->
                doc.toObject(CouponUsage::class.java)
            }.sortedByDescending { it.timestamp }
            
            // Get all coupon IDs associated with this vehicle
            val couponIds = usages.map { it.couponId }.distinct()
            
            // Get coupons that match those IDs
            val coupons = mutableListOf<ParkingCoupon>()
            for (couponId in couponIds) {
                val couponDoc = firestore.collection("coupons").document(couponId).get().await()
                couponDoc.toObject(ParkingCoupon::class.java)?.let { coupons.add(it) }
            }
            
            Result.success(Pair(coupons, usages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check coupon by parking area and lot (for staff)
    suspend fun checkCouponByLocation(parkingArea: String, lotNumber: String): Result<Pair<List<ParkingCoupon>, List<CouponUsage>>> {
        return try {
            // Get usage history for this location
            val usageSnapshot = firestore.collection("couponUsages")
                .whereEqualTo("parkingArea", parkingArea)
                .whereEqualTo("parkingLotNumber", lotNumber)
                .get()
                .await()
                
            val usages = usageSnapshot.documents.mapNotNull { doc ->
                doc.toObject(CouponUsage::class.java)
            }.sortedByDescending { it.timestamp }
            
            // Get all coupon IDs associated with this location
            val couponIds = usages.map { it.couponId }.distinct()
            
            // Get coupons that match those IDs
            val coupons = mutableListOf<ParkingCoupon>()
            for (couponId in couponIds) {
                val couponDoc = firestore.collection("coupons").document(couponId).get().await()
                couponDoc.toObject(ParkingCoupon::class.java)?.let { coupons.add(it) }
            }
            
            Result.success(Pair(coupons, usages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get user's favorite vehicles
    suspend fun getFavoriteVehicles(): Flow<List<Vehicle>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            
            val snapshot = firestore.collection("vehicles")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
                .get()
                .await()
                
            val vehicles = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Vehicle::class.java)
            }
            
            emit(vehicles)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Get user's favorite parking areas
    suspend fun getFavoriteParkingAreas(): Flow<List<ParkingArea>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            
            val snapshot = firestore.collection("parkingAreas")
                .whereEqualTo("isFavorite", true)
                .get()
                .await()
                
            val areas = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ParkingArea::class.java)
            }
            
            emit(areas)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Add a vehicle
    suspend fun addVehicle(licensePlate: String, isFavorite: Boolean): Result<Vehicle> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val vehicle = Vehicle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                licensePlate = licensePlate,
                isFavorite = isFavorite
            )
            
            firestore.collection("vehicles").document(vehicle.id)
                .set(vehicle)
                .await()
                
            Result.success(vehicle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 