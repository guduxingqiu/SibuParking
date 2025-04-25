package sibu.parking.firebase

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import sibu.parking.model.User
import sibu.parking.model.UserType
import java.util.concurrent.TimeUnit

class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // 保存验证ID
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    // 获取当前登录用户
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    // 使用邮箱发送OTP验证码
    suspend fun sendEmailVerification(email: String): Result<Boolean> {
        return try {
            // 首先需要创建一个临时账户，然后发送验证邮件
            val authResult = auth.createUserWithEmailAndPassword(email, "temporary_password").await()
            val firebaseUser = authResult.user ?: throw Exception("用户创建失败")
            
            // 发送验证邮件
            firebaseUser.sendEmailVerification().await()
            
            // 注意：这里不会立即退出账户，因为我们需要在用户验证后更新账户信息
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 使用手机号发送OTP验证码
    fun sendPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }
            
            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed(e)
            }
            
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@FirebaseAuthService.verificationId = verificationId
                this@FirebaseAuthService.resendToken = token
                onCodeSent(verificationId, token)
            }
        }
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    // 验证手机验证码
    suspend fun verifyPhoneCode(code: String): Result<PhoneAuthCredential> {
        return try {
            val verificationId = this.verificationId ?: throw Exception("验证ID不存在")
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            Result.success(credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 重新发送手机验证码
    fun resendPhoneVerification(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val token = this.resendToken ?: return
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // 不处理，因为这是重新发送
            }
            
            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed(e)
            }
            
            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@FirebaseAuthService.verificationId = verificationId
                this@FirebaseAuthService.resendToken = token
                onCodeSent(verificationId, token)
            }
        }
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    
    // 使用邮箱和密码注册
    suspend fun registerWithEmail(
        username: String,
        email: String,
        phone: String,
        password: String,
        userType: UserType
    ): Result<FirebaseUser> {
        return try {
            // 检查是否已经有临时账户存在
            val currentUser = auth.currentUser
            
            val firebaseUser = if (currentUser != null) {
                // 如果有临时账户，更新密码
                val credential = EmailAuthProvider.getCredential(email, "temporary_password")
                currentUser.reauthenticate(credential).await()
                
                // 更新密码
                currentUser.updatePassword(password).await()
                currentUser
            } else {
                // 否则创建新账户
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                authResult.user ?: throw Exception("用户创建失败")
            }
            
            // 在Firestore中创建用户数据
            val user = User(
                id = firebaseUser.uid,
                email = email,
                phone = phone,
                username = username,
                userType = userType
            )
            
            // 保存用户数据到Firestore
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
                
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 使用手机凭证注册
    suspend fun registerWithPhone(
        username: String,
        email: String,
        phone: String,
        password: String,
        credential: PhoneAuthCredential,
        userType: UserType
    ): Result<FirebaseUser> {
        return try {
            // 使用电话认证登录
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw Exception("用户创建失败")
            
            // 链接邮箱和密码
            val emailCredential = EmailAuthProvider.getCredential(email, password)
            firebaseUser.linkWithCredential(emailCredential).await()
            
            // 在Firestore中创建用户数据
            val user = User(
                id = firebaseUser.uid,
                email = email,
                phone = phone,
                username = username,
                userType = userType
            )
            
            // 保存用户数据到Firestore
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
                
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 使用邮箱和密码登录
    suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("登录失败")
            
            // 从Firestore获取用户数据
            val userDocument = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
                
            // 转换为User对象
            val user = userDocument.toObject(User::class.java) 
                ?: throw Exception("用户数据不存在")
                
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 登出
    fun logout() {
        auth.signOut()
    }
} 