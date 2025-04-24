package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
    Email, Phone, Username
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String, UserType) -> Unit = { _, _, _ -> }
) {
    var loginInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedLoginType by remember { mutableStateOf(LoginType.Email) }
    var selectedUserType by remember { mutableStateOf(UserType.USER) }

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

        // User Type Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = selectedUserType == UserType.USER,
                onClick = { selectedUserType = UserType.USER },
                label = { Text("User") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            
            FilterChip(
                selected = selectedUserType == UserType.STAFF,
                onClick = { selectedUserType = UserType.STAFF },
                label = { Text("Staff") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

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
                type = LoginType.Phone,
                icon = Icons.Default.Phone,
                label = "Phone",
                isSelected = selectedLoginType == LoginType.Phone,
                onClick = { selectedLoginType = LoginType.Phone }
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
            LoginType.Phone -> KeyboardType.Phone
            LoginType.Username -> KeyboardType.Text
        }
        
        val inputLabel = when (selectedLoginType) {
            LoginType.Email -> "Email"
            LoginType.Phone -> "Phone"
            LoginType.Username -> "Username"
        }
        
        val inputIcon = when (selectedLoginType) {
            LoginType.Email -> Icons.Default.Email
            LoginType.Phone -> Icons.Default.Phone
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
                .padding(bottom = 24.dp),
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

        // Login Button
        Button(
            onClick = { onLoginClick(loginInput, password, selectedUserType) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Login")
        }
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