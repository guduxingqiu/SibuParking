package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sibu.parking.model.UserType

enum class LoginType {
    Email, Username
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: (String) -> Unit = { }
) {
    var loginInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedLoginType by remember { mutableStateOf(LoginType.Email) }
    
    // For forgot password dialog
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Title
        Text(
            text = "SibuParking",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Login Type Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LoginTypeChip(
                type = LoginType.Email,
                icon = Icons.Default.Email,
                label = "Email",
                isSelected = selectedLoginType == LoginType.Email,
                onClick = { selectedLoginType = LoginType.Email }
            )
            
            LoginTypeChip(
                type = LoginType.Username,
                icon = Icons.Default.Person,
                label = "Username",
                isSelected = selectedLoginType == LoginType.Username,
                onClick = { selectedLoginType = LoginType.Username }
            )
        }

        // Input Field
        val keyboardType = when (selectedLoginType) {
            LoginType.Email -> KeyboardType.Email
            LoginType.Username -> KeyboardType.Text
        }
        
        val inputLabel = when (selectedLoginType) {
            LoginType.Email -> "Email"
            LoginType.Username -> "Username"
        }
        
        val inputIcon = when (selectedLoginType) {
            LoginType.Email -> Icons.Default.Email
            LoginType.Username -> Icons.Default.Person
        }

        OutlinedTextField(
            value = loginInput,
            onValueChange = { loginInput = it },
            label = { Text(inputLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            leadingIcon = {
                Icon(
                    imageVector = inputIcon,
                    contentDescription = null
                )
            }
        )

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
        
        // Forgot Password link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(
                onClick = { showForgotPasswordDialog = true },
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Forgot Password?")
            }
        }

        // Login Button
        Button(
            onClick = { onLoginClick(loginInput, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Login")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Register Button
        TextButton(
            onClick = onRegisterClick
        ) {
            Text("Don't have an account? Register")
        }
    }
    
    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false
                forgotPasswordEmail = ""
                emailError = null 
            },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it; emailError = null },
                        label = { Text("Email") },
                        singleLine = true,
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotPasswordEmail).matches()) {
                            emailError = "Please enter a valid email"
                        } else {
                            onForgotPasswordClick(forgotPasswordEmail)
                            showForgotPasswordDialog = false
                            forgotPasswordEmail = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showForgotPasswordDialog = false
                        forgotPasswordEmail = ""
                        emailError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginTypeChip(
    type: LoginType,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
} 