package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterClick: (
        username: String,
        email: String, 
        password: String
    ) -> Unit = { _, _, _ -> },
    onBackToLogin: () -> Unit = {},
    checkUsernameExists: suspend (String) -> Boolean = { false }
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Username verification status
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    
    // Form validation
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    var usernameCheckJob: Job? = remember { null }
    
    // Debounced username check
    fun checkUsername(input: String) {
        if (input.length < 1) {
            isUsernameAvailable = null
            return
        }
        
        usernameCheckJob?.cancel()
        usernameCheckJob = coroutineScope.launch {
            isCheckingUsername = true
            delay(500) // Debounce
            try {
                val exists = checkUsernameExists(input)
                isUsernameAvailable = !exists
            } catch (e: Exception) {
                isUsernameAvailable = null
            } finally {
                isCheckingUsername = false
            }
        }
    }
    
    // Validate form
    fun validateForm(): Boolean {
        var isValid = true
        
        // Validate username
        if (username.isEmpty()) {
            usernameError = "Username cannot be empty"
            isValid = false
        } else if (isUsernameAvailable == false) {
            usernameError = "Username already exists"
            isValid = false
        } else {
            usernameError = null
        }
        
        // Validate email
        if (email.isEmpty()) {
            emailError = "Email cannot be empty"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            isValid = false
        } else {
            emailError = null
        }
        
        // Validate password
        if (password.isEmpty()) {
            passwordError = "Password cannot be empty"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }
        
        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "Confirm password cannot be empty"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }
        
        return isValid
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "Create New Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )
        
        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                checkUsername(it)
            },
            label = { Text("Username") },
            singleLine = true,
            isError = usernameError != null,
            supportingText = { usernameError?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            trailingIcon = {
                when {
                    isCheckingUsername -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    isUsernameAvailable == true -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Username available",
                            tint = Color.Green
                        )
                    }
                    isUsernameAvailable == false -> {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Username unavailable",
                            tint = Color.Red
                        )
                    }
                }
            }
        )
        
        // Email input
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null
                )
            }
        )
        
        // Password input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                val image = if (passwordVisible)
                    Icons.Default.VisibilityOff
                else Icons.Default.Visibility

                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        
        // Confirm password input
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            isError = confirmPasswordError != null,
            supportingText = { confirmPasswordError?.let { Text(it) } },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null
                )
            },
            trailingIcon = {
                val image = if (confirmPasswordVisible)
                    Icons.Default.VisibilityOff
                else Icons.Default.Visibility

                val description = if (confirmPasswordVisible) "Hide password" else "Show password"

                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Register Button
        Button(
            onClick = { 
                if (validateForm()) {
                    onRegisterClick(username, email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Register")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back to login
        TextButton(
            onClick = onBackToLogin
        ) {
            Text("Already have an account? Login")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Note about email verification
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Email Verification",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "After registration, a verification link will be sent to your email. Please verify your email to complete the registration process."
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
} 