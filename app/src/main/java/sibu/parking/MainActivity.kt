package sibu.parking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sibu.parking.firebase.FirebaseAuthService
import sibu.parking.firebase.FirebaseCouponService
import sibu.parking.firebase.StripeService
import sibu.parking.model.Cart
import sibu.parking.model.CouponType
import sibu.parking.model.CouponUsage
import sibu.parking.model.ParkingArea
import sibu.parking.model.ParkingCoupon
import sibu.parking.model.PaymentMethod
import sibu.parking.model.User
import sibu.parking.model.UserType
import sibu.parking.model.Vehicle
import sibu.parking.ui.screens.*
import sibu.parking.ui.theme.SibuParkingTheme

enum class AppScreen {
    LOGIN, REGISTER, USER_HOME, STAFF_HOME, USE_COUPON, BUY_COUPON, CHECK_COUPON, EMAIL_VERIFICATION, STRIPE_PAYMENT
}

enum class PaymentProvider {
    MANUAL, STRIPE
}

class MainActivity : ComponentActivity() {
    
    private val authService = FirebaseAuthService()
    private val couponService = FirebaseCouponService()
    private lateinit var stripeService: StripeService
    
    private var _checkCouponResults = mutableListOf<ParkingCoupon>()
    private var _checkUsageResults = mutableListOf<CouponUsage>()
    private var _isCheckingCoupon = false
    
    // Cart reference for payment processing
    private lateinit var _currentCart: Cart
    private var _selectedPaymentMethod: PaymentMethod? = null
    private var _isPaymentProcessing = false
    private var userCoupons by mutableStateOf<List<ParkingCoupon>>(emptyList())
    private var isLoadingCoupons by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Stripe Service
        stripeService = StripeService(this)
        
        // Test Stripe connection
        testStripeConnection()
        
        setContent {
            SibuParkingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentUser by remember { mutableStateOf<User?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
                    
                    // State for coupons
                    var userCoupons by remember { mutableStateOf(this@MainActivity.userCoupons) }
                    var favoriteVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
                    var favoriteParkingAreas by remember { mutableStateOf<List<ParkingArea>>(emptyList()) }
                    var isLoadingCoupons by remember { mutableStateOf(this@MainActivity.isLoadingCoupons) }
                    
                    // Shopping cart state
                    val cart = remember { Cart() }
                    _currentCart = cart
                    
                    // Payment state
                    var isPaymentProcessing by remember { mutableStateOf(_isPaymentProcessing) }
                    var selectedPaymentProvider by remember { mutableStateOf(PaymentProvider.MANUAL) }
                    
                    // 使用LaunchedEffect来观察外部状态变化
                    LaunchedEffect(this@MainActivity.userCoupons) {
                        userCoupons = this@MainActivity.userCoupons
                    }
                    
                    LaunchedEffect(this@MainActivity.isLoadingCoupons) {
                        isLoadingCoupons = this@MainActivity.isLoadingCoupons
                    }
                    
                    // Load favorite vehicles
                    fun loadFavoriteVehicles() {
                        lifecycleScope.launch {
                            try {
                                couponService.getFavoriteVehicles().collectLatest { vehicles ->
                                    favoriteVehicles = vehicles
                                }
                            } catch (e: Exception) {
                                // Silent fail
                            }
                        }
                    }
                    
                    // Load favorite parking areas
                    fun loadFavoriteParkingAreas() {
                        lifecycleScope.launch {
                            try {
                                couponService.getFavoriteParkingAreas().collectLatest { areas ->
                                    favoriteParkingAreas = areas
                                }
                            } catch (e: Exception) {
                                // Silent fail
                            }
                        }
                    }
                    
                    // 使用状态对象来保存和观察变量的变化
                    var checkCouponResults by remember { mutableStateOf(_checkCouponResults.toList()) }
                    var checkUsageResults by remember { mutableStateOf(_checkUsageResults.toList()) }
                    var isCheckingCoupon by remember { mutableStateOf(_isCheckingCoupon) }
                    
                    // 添加状态更新函数
                    fun updateCheckCouponState() {
                        checkCouponResults = _checkCouponResults.toList()
                        checkUsageResults = _checkUsageResults.toList()
                        isCheckingCoupon = _isCheckingCoupon
                    }
                    
                    fun updatePaymentState() {
                        isPaymentProcessing = _isPaymentProcessing
                    }
                    
                    when {
                        isLoading -> {
                            // Add loading indicator here if needed
                        }
                        currentScreen == AppScreen.LOGIN -> {
                            LoginScreen(
                                onLoginClick = { loginInput, password ->
                                    isLoading = true
                                    loginUser(loginInput, password) { user ->
                                        currentUser = user
                                        isLoading = false
                                        if (user != null) {
                                            if (user.userType == UserType.STAFF) {
                                                currentScreen = AppScreen.STAFF_HOME
                                            } else {
                                                currentScreen = AppScreen.USER_HOME
                                                loadCoupons()
                                                loadFavoriteVehicles()
                                                loadFavoriteParkingAreas()
                                            }
                                        }
                                    }
                                },
                                onRegisterClick = {
                                    currentScreen = AppScreen.REGISTER
                                },
                                onForgotPasswordClick = { email ->
                                    isLoading = true
                                    forgotPassword(email) {
                                        isLoading = false
                                    }
                                }
                            )
                        }
                        currentScreen == AppScreen.REGISTER -> {
                            RegisterScreen(
                                onRegisterClick = { username, email, password ->
                                    isLoading = true
                                    registerUser(username, email, password) { success ->
                                        isLoading = false
                                        if (success) {
                                            currentScreen = AppScreen.LOGIN
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Registration successful! A verification email has been sent.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                onBackToLogin = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                checkUsernameExists = { username ->
                                    !authService.isUsernameUnique(username)
                                }
                            )
                        }
                        currentScreen == AppScreen.USE_COUPON -> {
                            UseCouponScreen(
                                coupons = userCoupons,
                                favoriteVehicles = favoriteVehicles,
                                favoriteParkingAreas = favoriteParkingAreas,
                                isLoading = isLoadingCoupons,
                                onBackClick = {
                                    currentScreen = AppScreen.USER_HOME
                                    loadCoupons()
                                },
                                onUseCoupon = { couponId, usedCount, parkingArea, parkingLotNumber, vehicleNumber ->
                                    isLoading = true
                                    useCoupon(couponId, usedCount, parkingArea, parkingLotNumber, vehicleNumber) { success ->
                                        isLoading = false
                                        if (success) {
                                            loadCoupons() // Reload coupons after use
                                        }
                                    }
                                }
                            )
                        }
                        currentScreen == AppScreen.BUY_COUPON -> {
                            BuyCouponScreen(
                                cart = cart,
                                onAddToCart = { couponType ->
                                    cart.addItem(couponType)
                                    Toast.makeText(this@MainActivity, "Added to cart", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveFromCart = { index ->
                                    cart.removeItem(index)
                                },
                                onUpdateQuantity = { index, quantity ->
                                    cart.updateQuantity(index, quantity)
                                },
                                onCheckout = { paymentMethod ->
                                    if (cart.isEmpty()) {
                                        Toast.makeText(this@MainActivity, "Cart is empty", Toast.LENGTH_SHORT).show()
                                        return@BuyCouponScreen
                                    }
                                    
                                    // Save payment method for later use
                                    _selectedPaymentMethod = paymentMethod
                                    
                                    // Choose between payment providers
                                    when (selectedPaymentProvider) {
                                        PaymentProvider.MANUAL -> {
                                            isLoading = true
                                            // Process traditional payment
                                            purchaseCoupons(cart, paymentMethod) { success ->
                                                isLoading = false
                                                if (success) {
                                                    // Clear cart after successful purchase
                                                    cart.clear()
                                                    loadCoupons() // Reload coupons to reflect new purchases
                                                    currentScreen = AppScreen.USER_HOME
                                                    
                                                    // In a real app, you would redirect to payment gateway
                                                    // For now, we'll just show a success message
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Purchase successful! Coupons added to your account.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                        PaymentProvider.STRIPE -> {
                                            // Use Stripe for payment
                                            _isPaymentProcessing = true
                                            updatePaymentState()
                                            
                                            // Initialize Stripe payment
                                            initializeStripePayment(cart) { success ->
                                                if (success) {
                                                    // Show Stripe payment sheet
                                                    stripeService.presentPaymentSheet()
                                                } else {
                                                    _isPaymentProcessing = false
                                                    updatePaymentState()
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Failed to initialize payment. Please try again.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.USER_HOME
                                },
                                onChangePaymentProvider = { provider ->
                                    selectedPaymentProvider = provider
                                },
                                selectedPaymentProvider = selectedPaymentProvider,
                                isProcessingPayment = isPaymentProcessing
                            )
                        }
                        currentScreen == AppScreen.CHECK_COUPON -> {
                            // 确保显示最新状态
                            updateCheckCouponState()
                            
                            CheckCouponScreen(
                                couponResults = checkCouponResults,
                                usageResults = checkUsageResults,
                                isLoading = isCheckingCoupon,
                                onSearchByVehicle = { vehicleNumber ->
                                    checkCouponByVehicle(vehicleNumber) {
                                        updateCheckCouponState()
                                    }
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.STAFF_HOME
                                }
                            )
                        }
                        currentUser?.userType == UserType.STAFF -> {
                            StaffHomeScreen(
                                username = currentUser?.username ?: currentUser?.email ?: "",
                                onLogout = {
                                    authService.logout()
                                    currentUser = null
                                    currentScreen = AppScreen.LOGIN
                                },
                                onCheckCouponClick = {
                                    // Reset results before navigating to check screen
                                    _checkCouponResults = mutableListOf()
                                    _checkUsageResults = mutableListOf()
                                    currentScreen = AppScreen.CHECK_COUPON
                                }
                            )
                        }
                        else -> {
                            UserHomeScreen(
                                username = currentUser?.username ?: currentUser?.email ?: "",
                                onLogout = {
                                    authService.logout()
                                    currentUser = null
                                    currentScreen = AppScreen.LOGIN
                                },
                                onUseCouponClick = {
                                    loadCoupons() // Refresh coupons
                                    currentScreen = AppScreen.USE_COUPON
                                },
                                onBuyCouponClick = {
                                    currentScreen = AppScreen.BUY_COUPON
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // 设置Stripe支付结果处理
        setupStripeResultHandling()
    }
    
    private fun loadCoupons() {
        isLoadingCoupons = true
        android.util.Log.d("MainActivity", "开始加载优惠券...")
        
        lifecycleScope.launch {
            try {
                couponService.getUserCoupons().collectLatest { coupons ->
                    userCoupons = coupons
                    isLoadingCoupons = false
                    android.util.Log.d("MainActivity", "成功加载 ${coupons.size} 张优惠券")
                    
                    // 显示加载到的优惠券详情
                    if (coupons.isEmpty()) {
                        android.util.Log.d("MainActivity", "没有可用的优惠券")
                        Toast.makeText(this@MainActivity, "您没有可用的优惠券", Toast.LENGTH_SHORT).show()
                    } else {
                        android.util.Log.d("MainActivity", "优惠券列表:")
                        coupons.forEachIndexed { index, coupon ->
                            android.util.Log.d("MainActivity", "[$index] ID: ${coupon.id}, 类型: ${coupon.type}, 剩余次数: ${coupon.remainingUses}")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "加载优惠券失败: ${e.message}")
                e.printStackTrace()
                isLoadingCoupons = false
                Toast.makeText(this@MainActivity, "加载优惠券失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loginUser(loginInput: String, password: String, onResult: (User?) -> Unit) {
        lifecycleScope.launch {
            try {
                // Check if login input is an email
                val result = if (android.util.Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                    authService.loginWithEmail(loginInput, password)
                } else {
                    // Try to login with username
                    authService.loginWithUsername(loginInput, password)
                }
                
                if (result.isSuccess) {
                    onResult(result.getOrNull())
                } else {
                    Toast.makeText(this@MainActivity, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
    }
    
    private fun registerUser(username: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = authService.registerWithEmail(username, email, password)
                
                if (result.isSuccess) {
                    onResult(true)
                } else {
                    Toast.makeText(this@MainActivity, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }
    
    private fun forgotPassword(email: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                val result = authService.sendPasswordResetEmail(email)
                if (result.isSuccess) {
                    Toast.makeText(
                        this@MainActivity,
                        "Password reset mail has been sent, please check your mailbox",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to send password reset email: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to send password reset email: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                onComplete()
            }
        }
    }
    
    private fun useCoupon(
        couponId: String, 
        usedCount: Int, 
        parkingArea: String, 
        parkingLotNumber: String, 
        vehicleNumber: String, 
        onComplete: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val result = couponService.useCoupon(
                    couponId = couponId,
                    usedCount = usedCount,
                    parkingArea = parkingArea,
                    parkingLotNumber = parkingLotNumber,
                    vehicleNumber = vehicleNumber
                )
                
                if (result.isSuccess) {
                    Toast.makeText(
                        this@MainActivity,
                        "Coupon used successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(true)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to use coupon: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to use coupon: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(false)
            }
        }
    }
    
    private fun purchaseCoupons(
        cart: Cart,
        paymentMethod: PaymentMethod,
        onComplete: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                // Simulate payment gateway redirect
                // In a real app, you would redirect to the payment gateway here
                // After returning from the payment gateway, you would process the purchase
                
                // For demo purposes, let's just make a direct purchase
                val result = couponService.purchaseCoupons(cart, paymentMethod)
                
                if (result.isSuccess) {
                    onComplete(true)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Purchase failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Purchase failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(false)
            }
        }
    }
    
    private fun initializeStripePayment(cart: Cart, onComplete: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                // Initialize Stripe payment sheet
                stripeService.initializePaymentSheet(cart)
                
                // Create payment intent
                val result = stripeService.createPaymentIntent(cart)
                
                if (result.isSuccess) {
                    onComplete(true)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to create payment: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onComplete(false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to initialize payment: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onComplete(false)
            }
        }
    }
    
    // Helper method for payment gateway redirect (in a real app)
    private fun redirectToPaymentGateway(paymentMethod: PaymentMethod, amount: Double) {
        // This is a placeholder. In a real app, you would redirect to the payment gateway.
        val url = when (paymentMethod) {
            PaymentMethod.ONLINE_BANKING -> "https://example.com/fpx-payment?amount=$amount"
            PaymentMethod.E_WALLET -> "https://example.com/ewallet-payment?amount=$amount"
            else -> "https://example.com/default-payment?amount=$amount"
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    // Check coupon by vehicle number
    private fun checkCouponByVehicle(vehicleNumber: String, onComplete: () -> Unit) {
        _isCheckingCoupon = true
        lifecycleScope.launch {
            try {
                val result = couponService.checkCouponByVehicle(vehicleNumber)
                if (result.isSuccess) {
                    val (coupons, usages) = result.getOrNull() ?: Pair(emptyList(), emptyList())
                    _checkCouponResults = coupons.toMutableList()
                    _checkUsageResults = usages.toMutableList()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to check coupon: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to check coupon: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                _isCheckingCoupon = false
                onComplete()
            }
        }
    }
    
    // Stripe payment callback methods
    fun onStripePaymentCompleted() {
        _isPaymentProcessing = false
        
        // Clear cart and show success message
        _currentCart.clear()
        
        // 确保重新加载优惠券
        loadCoupons()
        
        Toast.makeText(
            this,
            "Payment successful! Coupons added to your account.",
            Toast.LENGTH_LONG
        ).show()
        
        // 返回主界面并触发重组
        lifecycleScope.launch {
            setContent {
                SibuParkingTheme {
                    // This will trigger a recomposition with updated state
                }
            }
        }
    }
    
    fun onStripePaymentCanceled() {
        _isPaymentProcessing = false
        
        Toast.makeText(
            this,
            "Payment was canceled",
            Toast.LENGTH_SHORT
        ).show()
        
        // Refresh the UI state
        setContent {
            SibuParkingTheme {
                // This will trigger a recomposition with updated state
            }
        }
    }
    
    fun onStripePaymentFailed(error: Throwable) {
        _isPaymentProcessing = false
        
        Toast.makeText(
            this,
            "Payment failed: ${error.message}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Refresh the UI state
        setContent {
            SibuParkingTheme {
                // This will trigger a recomposition with updated state
            }
        }
    }
    
    private fun setupStripeResultHandling() {
        // 这个方法会在Activity重新创建时调用，处理支付结果回调
        if (::stripeService.isInitialized) {
            stripeService.handlePaymentResult(
                onSuccess = {
                    handlePaymentSuccess()
                },
                onFailure = {
                    handlePaymentFailure()
                }
            )
        }
    }
    
    private fun handlePaymentSuccess() {
        runOnUiThread {
            _isPaymentProcessing = false
            purchaseCoupons(_currentCart, PaymentMethod.STRIPE) { success ->
                if (success) {
                    _currentCart.clear()
                    loadCoupons()
                    // 确保购买成功后导航回主页
                    setContent {
                        SibuParkingTheme {
                            // 这会触发界面重新组合
                        }
                    }
                    Toast.makeText(this@MainActivity, "Payment successful! Coupons added to your account.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Payment failed. Please try again later.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun handlePaymentFailure() {
        runOnUiThread {
            _isPaymentProcessing = false
            Toast.makeText(this@MainActivity, "Payment failed. Please try again later.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Test if Stripe is properly connected
     */
    private fun testStripeConnection() {
        lifecycleScope.launch {
            try {
                val result = stripeService.testStripeConnection()
                if (result.isSuccess) {
                    // Connection successful
                    android.util.Log.d("MainActivity", "Stripe connection test successful")
                    // Uncomment the next line to show a toast message
                    // Toast.makeText(this@MainActivity, "Stripe connection successful", Toast.LENGTH_SHORT).show()
                } else {
                    // Connection failed
                    android.util.Log.e("MainActivity", "Stripe connection test failed: ${result.exceptionOrNull()?.message}")
                    // Uncomment the next line to show a toast message
                    // Toast.makeText(this@MainActivity, "Failed to connect to Stripe: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error testing Stripe connection: ${e.message}")
            }
        }
    }
} 