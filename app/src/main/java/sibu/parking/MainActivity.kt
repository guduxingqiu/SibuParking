package sibu.parking

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import sibu.parking.firebase.FirebaseAuthService
import sibu.parking.firebase.FirebaseCouponService
import sibu.parking.model.ParkingArea
import sibu.parking.model.ParkingCoupon
import sibu.parking.model.User
import sibu.parking.model.UserType
import sibu.parking.model.Vehicle
import sibu.parking.ui.screens.LoginScreen
import sibu.parking.ui.screens.RegisterScreen
import sibu.parking.ui.screens.StaffHomeScreen
import sibu.parking.ui.screens.UseCouponScreen
import sibu.parking.ui.screens.UserHomeScreen
import sibu.parking.ui.theme.SibuParkingTheme

enum class AppScreen {
    LOGIN, REGISTER, USER_HOME, STAFF_HOME, USE_COUPON, EMAIL_VERIFICATION
}

class MainActivity : ComponentActivity() {
    
    private val authService = FirebaseAuthService()
    private val couponService = FirebaseCouponService()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                    var userCoupons by remember { mutableStateOf<List<ParkingCoupon>>(emptyList()) }
                    var favoriteVehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
                    var favoriteParkingAreas by remember { mutableStateOf<List<ParkingArea>>(emptyList()) }
                    var isLoadingCoupons by remember { mutableStateOf(false) }
                    
                    // Load coupons when needed
                    fun loadCoupons() {
                        isLoadingCoupons = true
                        lifecycleScope.launch {
                            try {
                                couponService.getUserCoupons().collectLatest { coupons ->
                                    userCoupons = coupons
                                    isLoadingCoupons = false
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Failed to load coupons: ${e.message}", Toast.LENGTH_SHORT).show()
                                isLoadingCoupons = false
                            }
                        }
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
                        currentUser?.userType == UserType.STAFF -> {
                            StaffHomeScreen(
                                username = currentUser?.username ?: currentUser?.email ?: "",
                                onLogout = {
                                    authService.logout()
                                    currentUser = null
                                    currentScreen = AppScreen.LOGIN
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
                                }
                            )
                        }
                    }
                }
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
} 