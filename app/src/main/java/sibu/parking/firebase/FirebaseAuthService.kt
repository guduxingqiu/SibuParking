package sibu.parking.firebase

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import sibu.parking.model.User
import sibu.parking.model.UserType

class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // 获取当前登录用户
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    // 使用邮箱和密码注册
    suspend fun registerWithEmail(email: String, password: String, userType: UserType): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("用户创建失败")
            
            // 在Firestore中创建用户数据
            val user = User(
                id = firebaseUser.uid,
                email = email,
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