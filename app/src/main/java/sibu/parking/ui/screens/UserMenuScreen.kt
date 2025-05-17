package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMenuScreen(
    username: String,
    email: String,
    onBackClick: () -> Unit,
    onUpdatePassword: (String, String, String) -> Unit,
    onSignOut: () -> Unit,
    onUpdateUsername: (String) -> Unit,
    checkUsernameExists: suspend (String) -> Boolean = { false }
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    
    // Username verification status
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    var usernameCheckJob: Job? = remember { null }
    
    // Debounced username check
    fun checkUsername(input: String) {
        if (input.length >= 3) {
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

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Menu") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "User Information",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Username: $username")
                    Text("Email: $email")
                }
            }
            
            // Change Username Button
            Button(
                onClick = { showUsernameDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Username")
            }
            
            // Change Password Button
            Button(
                onClick = { showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Password")
            }
            
            // Sign Out Button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out")
            }
        }
    }
    
    // Username Update Dialog
    if (showUsernameDialog) {
        var newUsername by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Change Username") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { 
                            newUsername = it
                            checkUsername(it)
                        },
                        label = { Text("New Username") },
                        singleLine = true,
                        isError = usernameError != null,
                        supportingText = { usernameError?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth(),
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        var isValid = true
                        if (newUsername.isEmpty()) {
                            usernameError = "Username cannot be empty"
                            isValid = false
                            return@TextButton
                        }
                        if (isUsernameAvailable == false) {
                            usernameError = "Username already exists"
                            isValid = false
                            return@TextButton
                        }
                        onUpdateUsername(newUsername)
                        showUsernameDialog = false
                    },
                    enabled = !isCheckingUsername && isUsernameAvailable == true
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUsernameDialog = false },
                    enabled = !isCheckingUsername
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Password Update Dialog
    if (showPasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        
        var showCurrentPassword by remember { mutableStateOf(false) }
        var showNewPassword by remember { mutableStateOf(false) }
        var showConfirmPassword by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { 
                            currentPassword = it
                            error = null
                        },
                        label = { Text("Current Password") },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showCurrentPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { 
                            newPassword = it
                            error = null
                        },
                        label = { Text("New Password") },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            error = null
                        },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                )
                            }
                        }
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (currentPassword.isBlank()) {
                            error = "Please enter current password"
                            return@TextButton
                        }
                        if (newPassword.isBlank()) {
                            error = "Please enter new password"
                            return@TextButton
                        }
                        if (newPassword != confirmPassword) {
                            error = "Passwords do not match"
                            return@TextButton
                        }
                        onUpdatePassword(currentPassword, newPassword, confirmPassword)
                        showPasswordDialog = false
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSignOut()
                        showSignOutDialog = false
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 