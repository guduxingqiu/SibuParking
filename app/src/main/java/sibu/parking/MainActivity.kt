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
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import sibu.parking.firebase.FirebaseAuthService
import sibu.parking.model.User
import sibu.parking.model.UserType
import sibu.parking.ui.screens.LoginScreen
import sibu.parking.ui.screens.RegisterScreen
import sibu.parking.ui.screens.StaffHomeScreen
import sibu.parking.ui.screens.UserHomeScreen
import sibu.parking.ui.screens.VerificationType
import sibu.parking.ui.theme.SibuParkingTheme

enum class AppScreen {
    LOGIN, REGISTER, USER_HOME, STAFF_HOME
}

class MainActivity : ComponentActivity() {
    
    private val authService = FirebaseAuthService()
    
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
                    
                    // 临时存储注册信息
                    var registerUsername by remember { mutableStateOf("") }
                    var registerEmail by remember { mutableStateOf("") }
                    var registerPhone by remember { mutableStateOf("") }
                    var registerPassword by remember { mutableStateOf("") }
                    var registerUserType by remember { mutableStateOf(UserType.USER) }
                    var verificationId by remember { mutableStateOf("") }
                    var phoneAuthCredential by remember { mutableStateOf<PhoneAuthCredential?>(null) }
                    
                    when {
                        isLoading -> {
                            // 可以在这里添加加载指示器
                        }
                        currentScreen == AppScreen.LOGIN && currentUser == null -> {
                            LoginScreen(
                                onLoginClick = { loginInput, password, userType ->
                                    isLoading = true
                                    loginUser(loginInput, password) { user ->
                                        currentUser = user
                                        isLoading = false
                                        if (user != null) {
                                            if (user.userType == UserType.STAFF) {
                                                currentScreen = AppScreen.STAFF_HOME
                                            } else {
                                                currentScreen = AppScreen.USER_HOME
                                            }
                                        }
                                    }
                                },
                                onRegisterClick = {
                                    currentScreen = AppScreen.REGISTER
                                }
                            )
                        }
                        currentScreen == AppScreen.REGISTER -> {
                            RegisterScreen(
                                onRegisterClick = { username, email, phone, password, userType ->
                                    registerUsername = username
                                    registerEmail = email
                                    registerPhone = phone
                                    registerPassword = password
                                    registerUserType = userType
                                    
                                    isLoading = true
                                    registerUser(username, email, phone, password, userType, phoneAuthCredential) { success ->
                                        isLoading = false
                                        if (success) {
                                            currentScreen = AppScreen.LOGIN
                                            Toast.makeText(
                                                this@MainActivity,
                                                "注册成功，请登录",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onBackToLogin = {
                                    currentScreen = AppScreen.LOGIN
                                },
                                /*onSendVerification = { verificationType, input ->
                                    isLoading = true
                                    if (verificationType == VerificationType.EMAIL) {
                                        sendEmailVerification(input) { success ->
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "验证邮件已发送，请查收",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        sendPhoneVerification(
                                            phoneNumber = input,
                                            onVerificationCompleted = { credential ->
                                                phoneAuthCredential = credential
                                                isLoading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "自动验证成功",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onVerificationFailed = { e ->
                                                isLoading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "验证失败: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onCodeSent = { vId, _ ->
                                                verificationId = vId
                                                isLoading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "验证码已发送",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                },
                                onVerifyOtp = { code ->
                                    isLoading = true
                                    verifyPhoneCode(code) { credential ->
                                        phoneAuthCredential = credential
                                        isLoading = false
                                        if (credential != null) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "验证成功",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onResendCode = { verificationType, input ->
                                    isLoading = true
                                    if (verificationType == VerificationType.EMAIL) {
                                        sendEmailVerification(input) { success ->
                                            isLoading = false
                                            if (success) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "验证邮件已重新发送，请查收",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        resendPhoneVerification(
                                            input,
                                            onVerificationFailed = { e ->
                                                isLoading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "重新发送失败: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onCodeSent = { vId, _ ->
                                                verificationId = vId
                                                isLoading = false
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "验证码已重新发送",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                                }*/
                            )
                        }
                        currentScreen == AppScreen.STAFF_HOME || currentUser?.userType == UserType.STAFF -> {
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 登录用户
    private fun loginUser(loginInput: String, password: String, onResult: (User?) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = authService.loginWithEmail(loginInput, password)
                if (result.isSuccess) {
                    onResult(result.getOrNull())
                } else {
                    Toast.makeText(this@MainActivity, "登录失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
    }
    
    // 注册用户
    private fun registerUser(
        username: String,
        email: String,
        phone: String,
        password: String,
        userType: UserType,
        phoneAuthCredential: PhoneAuthCredential?,
        onResult: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val result = if (phoneAuthCredential != null) {
                    // 使用手机验证注册
                    authService.registerWithPhone(username, email, phone, password, phoneAuthCredential, userType)
                } else {
                    // 使用邮箱密码注册
                    authService.registerWithEmail(username, email, phone, password, userType)
                }
                
                if (result.isSuccess) {
                    onResult(true)
                } else {
                    Toast.makeText(this@MainActivity, "注册失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "注册失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }
    
    // 发送邮箱验证
    private fun sendEmailVerification(email: String, onResult: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = authService.sendEmailVerification(email)
                if (result.isSuccess) {
                    onResult(true)
                } else {
                    Toast.makeText(this@MainActivity, "发送验证邮件失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "发送验证邮件失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }
    
    // 发送手机验证码
    private fun sendPhoneVerification(
        phoneNumber: String,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit
    ) {
        authService.sendPhoneVerification(
            phoneNumber,
            this,
            onCodeSent,
            onVerificationCompleted,
            onVerificationFailed
        )
    }
    
    // 重新发送手机验证码
    private fun resendPhoneVerification(
        phoneNumber: String,
        onVerificationFailed: (Exception) -> Unit,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit
    ) {
        authService.resendPhoneVerification(
            phoneNumber,
            this,
            onCodeSent,
            onVerificationFailed
        )
    }
    
    // 验证手机验证码
    private fun verifyPhoneCode(code: String, onResult: (PhoneAuthCredential?) -> Unit) {
        lifecycleScope.launch {
            try {
                val result = authService.verifyPhoneCode(code)
                if (result.isSuccess) {
                    onResult(result.getOrNull())
                } else {
                    Toast.makeText(this@MainActivity, "验证失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "验证失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
    }
} 