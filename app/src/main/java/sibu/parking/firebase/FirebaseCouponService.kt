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
import sibu.parking.model.CouponStatus
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
                .get()
                .await()
                
            android.util.Log.d("FirebaseCouponService", "Raw coupon documents count: ${snapshot.size()}")
            
            val coupons = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: ""
                    val docUserId = doc.getString("userId") ?: ""
                    val typeStr = doc.getString("type") ?: "HOUR_1"
                    val remainingUses = doc.getLong("remainingUses")?.toInt() ?: 0
                    val purchaseDate = doc.getLong("purchaseDate") ?: System.currentTimeMillis()
                    
                    android.util.Log.d("FirebaseCouponService", "Processing coupon: $id, type: $typeStr, remainingUses: $remainingUses")
                    
                    // 只返回还有剩余次数的优惠券
                    if (remainingUses > 0) {
                        val type = try {
                            CouponType.valueOf(typeStr)
                        } catch (e: Exception) {
                            android.util.Log.e("FirebaseCouponService", "Invalid coupon type: $typeStr, using default HOUR_1")
                            CouponType.HOUR_1
                        }
                        
                        ParkingCoupon(
                            id = id,
                            userId = docUserId,
                            type = type,
                            remainingUses = remainingUses,
                            purchaseDate = purchaseDate
                        )
                    } else {
                        android.util.Log.d("FirebaseCouponService", "Skipping used up coupon: $id (remainingUses=$remainingUses)")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseCouponService", "Error parsing coupon: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            emit(coupons)
            
            android.util.Log.d("FirebaseCouponService", "Emitting ${coupons.size} coupons for user $userId")
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCouponService", "Error loading coupons: ${e.message}")
            e.printStackTrace()
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
                    val couponId = UUID.randomUUID().toString()
                    
                    // 使用与Firestore兼容的字段结构
                    val couponData = hashMapOf(
                        "id" to couponId,
                        "userId" to userId,
                        "type" to item.couponType.name,
                        "remainingUses" to 10,
                        "purchaseDate" to System.currentTimeMillis(),
                        "usedCount" to 0
                    )
                    
                    android.util.Log.d("FirebaseCouponService", "Creating coupon: $couponId, type: ${item.couponType.name}")
                    val docRef = firestore.collection("coupons").document(couponId)
                    batch.set(docRef, couponData)
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
            
            android.util.Log.d("FirebaseCouponService", "Purchase completed for user $userId: ${cart.items.sumOf { it.quantity }} coupons")
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCouponService", "Error purchasing coupons: ${e.message}")
            e.printStackTrace()
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
            
            // 获取优惠券数据
            val couponDoc = firestore.collection("coupons").document(couponId).get().await()
            
            // 手动解析数据
            val docId = couponDoc.getString("id") ?: return Result.failure(Exception("Invalid coupon ID"))
            val docUserId = couponDoc.getString("userId") ?: return Result.failure(Exception("Invalid user ID"))
            val typeStr = couponDoc.getString("type") ?: return Result.failure(Exception("Invalid coupon type"))
            val remainingUses = couponDoc.getLong("remainingUses")?.toInt() ?: 0
            
            // 验证剩余使用次数
            if (remainingUses - usedCount < 0) {
                return Result.failure(Exception("Not enough remaining uses"))
            }
            
            // 计算过期时间
            val expirationTime = when (CouponType.valueOf(typeStr)) {
                CouponType.MINUTES_30 -> System.currentTimeMillis() + (30 * 60 * 1000 * usedCount) // 30分钟
                CouponType.HOUR_1 -> System.currentTimeMillis() + (60 * 60 * 1000 * usedCount) // 1小时
                CouponType.HOURS_2 -> System.currentTimeMillis() + (2 * 60 * 60 * 1000 * usedCount) // 2小时
                CouponType.HOURS_24 -> System.currentTimeMillis() + (24 * 60 * 60 * 1000 * usedCount) // 24小时
            }
            
            // 更新已使用次数和剩余次数
            val newUsedCount = couponDoc.getLong("usedCount")?.toInt()?.plus(usedCount) ?: usedCount
            val newRemainingUses = remainingUses - usedCount
            
            android.util.Log.d("FirebaseCouponService", "Updating coupon $couponId: usedCount to $newUsedCount, remainingUses to $newRemainingUses")
            
            // 更新Firestore文档
            firestore.collection("coupons").document(couponId)
                .update(
                    mapOf(
                        "usedCount" to newUsedCount,
                        "remainingUses" to newRemainingUses
                    )
                )
                .await()
            
            // 记录使用情况
            val usage = CouponUsage(
                id = UUID.randomUUID().toString(),
                couponId = couponId,
                userId = userId,
                usedCount = usedCount,
                parkingArea = parkingArea,
                parkingLotNumber = parkingLotNumber,
                vehicleNumber = vehicleNumber,
                timestamp = System.currentTimeMillis(),
                status = CouponStatus.ACTIVE,
                expirationTime = expirationTime
            )
            
            firestore.collection("couponUsages").document(usage.id)
                .set(usage)
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCouponService", "Error using coupon: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Check coupon by vehicle number (for staff)
    suspend fun checkCouponByVehicle(vehicleNumber: String): Result<Pair<List<ParkingCoupon>, List<CouponUsage>>> {
        return try {
            // Get usage history for this vehicle
            val usageSnapshot = firestore.collection("couponUsages")
                .whereEqualTo("vehicleNumber", vehicleNumber)
                .whereEqualTo("status", CouponStatus.ACTIVE.name)
                .get()
                .await()
                
            val usages = usageSnapshot.documents.mapNotNull { doc ->
                val usage = doc.toObject(CouponUsage::class.java)
                // 再次检查是否过期
                if (usage != null && !usage.isExpired()) {
                    usage
                } else {
                    // 如果过期了，更新状态
                    if (usage != null) {
                        firestore.collection("couponUsages").document(usage.id)
                            .update("status", CouponStatus.EXPIRED.name)
                            .await()
                    }
                    null
                }
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
    
    // 获取用户停车历史
    suspend fun getUserParkingHistory(): Flow<List<CouponUsage>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            
            val snapshot = firestore.collection("couponUsages")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            val usages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CouponUsage::class.java)
            }.sortedByDescending { it.timestamp }
            
            emit(usages)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseCouponService", "Error getting parking history: ${e.message}")
            emit(emptyList())
        }
    }
} 