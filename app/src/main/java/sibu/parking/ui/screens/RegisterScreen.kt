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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import sibu.parking.model.UserType

enum class VerificationType {
    EMAIL, PHONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterClick: (
        username: String,
        email: String, 
        phone: String, 
        password: String, 
        userType: UserType
    ) -> Unit = { _, _, _, _, _ -> },
    onBackToLogin: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var selectedUserType by remember { mutableStateOf(UserType.USER) }
    
    // 验证状态
    var verificationType by remember { mutableStateOf(VerificationType.EMAIL) }
    var isVerificationSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    
    // 表单验证
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    
    // 验证表单
    fun validateForm(): Boolean {
        var isValid = true
        
        // 验证用户名
        if (username.isEmpty()) {
            usernameError = "用户名不能为空"
            isValid = false
        } else {
            usernameError = null
        }
        
        // 验证邮箱
        if (email.isEmpty()) {
            emailError = "邮箱不能为空"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "邮箱格式不正确"
            isValid = false
        } else {
            emailError = null
        }
        
        // 验证手机号
        if (phone.isEmpty()) {
            phoneError = "手机号不能为空"
            isValid = false
        } else if (!android.util.Patterns.PHONE.matcher(phone).matches()) {
            phoneError = "手机号格式不正确"
            isValid = false
        } else {
            phoneError = null
        }
        
        // 验证密码
        if (password.isEmpty()) {
            passwordError = "密码不能为空"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "密码长度不能少于6位"
            isValid = false
        } else {
            passwordError = null
        }
        
        // 验证确认密码
        if (confirmPassword.isEmpty()) {
            confirmPasswordError = "确认密码不能为空"
            isValid = false
        } else if (confirmPassword != password) {
            confirmPasswordError = "两次输入的密码不一致"
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
        // 标题
        Text(
            text = "注册新账户",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )
        
        // 用户类型选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = selectedUserType == UserType.USER,
                onClick = { selectedUserType = UserType.USER },
                label = { Text("用户") },
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
                label = { Text("工作人员") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        
        // 用户名输入框
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("用户名") },
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
            }
        )
        
        // 邮箱输入框
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("电子邮箱") },
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
        
        // 手机号输入框
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号码") },
            singleLine = true,
            isError = phoneError != null,
            supportingText = { phoneError?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null
                )
            }
        )
        
        // 密码输入框
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("密码") },
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

                val description = if (passwordVisible) "隐藏密码" else "显示密码"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        
        // 确认密码输入框
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("确认密码") },
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

                val description = if (confirmPasswordVisible) "隐藏密码" else "显示密码"

                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )
        
        // 选择验证方式
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = verificationType == VerificationType.EMAIL,
                onClick = { verificationType = VerificationType.EMAIL },
                label = { Text("邮箱验证") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            
            FilterChip(
                selected = verificationType == VerificationType.PHONE,
                onClick = { verificationType = VerificationType.PHONE },
                label = { Text("手机验证") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
        
        // OTP验证码部分
        if (isVerificationSent) {
            // 验证码输入
            OutlinedTextField(
                value = otpCode,
                onValueChange = { otpCode = it },
                label = { Text("验证码") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = null
                    )
                }
            )
            
            Button(
                onClick = { /* 验证OTP */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("验证")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = { /* 重新发送OTP */ }
            ) {
                Text("重新发送验证码")
            }
        } else {
            // 发送验证码按钮
            Button(
                onClick = { 
                    if (validateForm()) {
                        isVerificationSent = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("发送验证码")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 注册按钮，只有验证成功后才能点击
        Button(
            onClick = { 
                if (isVerificationSent && otpCode.isNotEmpty()) {
                    onRegisterClick(username, email, phone, password, selectedUserType)
                }
            },
            enabled = isVerificationSent && otpCode.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("注册")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 返回登录页面
        TextButton(
            onClick = onBackToLogin
        ) {
            Text("已有账号？返回登录")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen()
} 