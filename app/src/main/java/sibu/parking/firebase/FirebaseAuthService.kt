package sibu.parking.firebase

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import sibu.parking.model.User
import sibu.parking.model.UserType
/*import com.google.firebase.firestore.Source
import android.util.Log*/

class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Get current logged in user
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    // Check if username is unique
    suspend fun isUsernameUnique(username: String): Boolean {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username.trim().lowercase())
                .get()
                .await()
                
            querySnapshot.isEmpty
        } catch (e: Exception) {
            // If there's an error, conservatively return false
            false
            /*// 记录详细错误
            Log.e("Auth", "Username check failed", e)
            // 改为抛出异常而不是返回false
            throw e*/
        }
    }
    
    // Register with email and password
    suspend fun registerWithEmail(username: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            // Check if username is unique first
            if (!isUsernameUnique(username)) {
                return Result.failure(Exception("Username already exists"))
            }
            
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")
            
            // Send email verification
            firebaseUser.sendEmailVerification().await()
            
            // Create user data in Firestore
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                userType = UserType.USER
            )
            
            // Save user data to Firestore
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
                
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Login with email and password
    suspend fun loginWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")
            
            // Get user data from Firestore
            val userDocument = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()
                
            // Convert to User object
            val user = userDocument.toObject(User::class.java) 
                ?: throw Exception("User data not found")
                
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Login with username
    suspend fun loginWithUsername(username: String, password: String): Result<User> {
        return try {
            // First find the user by username
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username.trim().lowercase())
                .get()
                .await()
                
            if (querySnapshot.isEmpty) {
                throw Exception("User not found")
            }
            
            // Get the user document
            val userDoc = querySnapshot.documents.first()
            val email = userDoc.getString("email") ?: throw Exception("User email not found")
            
            // Now login with email and password
            loginWithEmail(email, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check if email is verified
    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }
    
    // Send email verification again
    suspend fun sendEmailVerificationAgain(): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user is logged in")
            user.sendEmailVerification().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Send password reset email
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Logout
    fun logout() {
        auth.signOut()
    }
} 