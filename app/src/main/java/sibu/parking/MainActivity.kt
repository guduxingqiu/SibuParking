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
import kotlinx.coroutines.launch
import parking.firebase.FirebaseAuthService
import sibuparking.model.User
import sibuparking.model.UserType
import sibuparking.ui.screens.LoginScreen
import sibuparking.ui.screens.StaffHomeScreen
import sibuparking.ui.screens.UserHomeScreen
import sibuparking.ui.theme.SibuParkingTheme

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
                    
                    when {
                        isLoading -> {
                            // 可以在这里添加加载指示器
                        }
                        currentUser == null -> {
                            LoginScreen(
                                onLoginClick = { loginInput, password, userType ->
                                    isLoading = true
                                    loginUser(loginInput, password, userType) { user ->
                                        currentUser = user
                                        isLoading = false
                                    }
                                }
                            )
                        }
                        currentUser?.userType == UserType.STAFF -> {
                            StaffHomeScreen(
                                username = currentUser?.email ?:"",
                                onLogout = {
                                    authService.logout()
                                    currentUser = null
                                }
                            )
                        }
                        else -> {
                            UserHomeScreen(
                                username = currentUser?.email ?:"",
                                onLogout = {
                                    authService.logout()
                                    currentUser = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private fun loginUser(loginInput: String, password: String, userType: UserType, onResult: (User?) -> Unit) {
        // 在实际应用中，这里会调用Firebase登录API
        // 现在只是模拟登录过程
        
        lifecycleScope.launch {
            try {
                //在实际应用中会使用以下代码：
                val result = authService.loginWithEmail(loginInput, password)
                if (result.isSuccess) {
                    onResult(result.getOrNull())
                } else {
                    Toast.makeText(this@MainActivity, "登录失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
                
                /*// 模拟登录:
                Toast.makeText(
                    this@MainActivity,
                    "登录类型: $userType, 账号: $loginInput",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 模拟一个成功登录，创建一个用户对象
                val user = User(
                    id = "user_id_1",
                    email = loginInput,
                    userType = userType
                )
                
                onResult(user)*/
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }
    }
} 