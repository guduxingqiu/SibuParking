package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import sibu.parking.model.Vehicle
import sibu.parking.model.ParkingArea

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMenuScreen(
    username: String,
    email: String,
    favoriteVehicles: List<Vehicle>,
    favoriteParkingAreas: List<ParkingArea>,
    onBackClick: () -> Unit,
    onUpdatePassword: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
    onSignOut: () -> Unit,
    onUpdateUsername: (newUsername: String) -> Unit,
    onAddFavoriteVehicle: (licensePlate: String) -> Unit,
    onRemoveFavoriteVehicle: (vehicleId: String) -> Unit,
    onAddFavoriteParkingArea: (areaName: String) -> Unit,
    onRemoveFavoriteParkingArea: (areaId: String) -> Unit,
    checkUsernameExists: suspend (username: String) -> Boolean
) {
    var showUpdatePasswordDialog by remember { mutableStateOf(false) }
    var showUpdateUsernameDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddParkingAreaDialog by remember { mutableStateOf(false) }
    
    // Username verification status
    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    
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
        ) {
            // Wrap the content in a scrollable column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                
                // Favorite Vehicles Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Favorite Vehicles",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showAddVehicleDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (favoriteVehicles.isEmpty()) {
                            Text("No favorite vehicles added yet")
                        } else {
                            favoriteVehicles.forEach { vehicle ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${vehicle.licensePlate}")
                                    IconButton(
                                        onClick = { onRemoveFavoriteVehicle(vehicle.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Vehicle")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Favorite Parking Areas Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Favorite Parking Areas",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showAddParkingAreaDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Parking Area")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (favoriteParkingAreas.isEmpty()) {
                            Text("No favorite parking areas added yet")
                        } else {
                            favoriteParkingAreas.forEach { area ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${area.name}")
                                    IconButton(
                                        onClick = { onRemoveFavoriteParkingArea(area.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Parking Area")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Update Username Button
                Button(
                    onClick = { showUpdateUsernameDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Username")
                }
                
                // Update Password Button
                Button(
                    onClick = { showUpdatePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Password")
                }
                
                // Sign Out Button
                Button(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
    
    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        var licensePlate by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Add Favorite Vehicle") },
            text = {
                Column {
                    OutlinedTextField(
                        value = licensePlate,
                        onValueChange = { licensePlate = it },
                        label = { Text("License Plate") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (licensePlate.isNotBlank()) {
                            onAddFavoriteVehicle(licensePlate)
                            showAddVehicleDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Add Parking Area Dialog
    if (showAddParkingAreaDialog) {
        var areaName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddParkingAreaDialog = false },
            title = { Text("Add Favorite Parking Area") },
            text = {
                Column {
                    OutlinedTextField(
                        value = areaName,
                        onValueChange = { areaName = it },
                        label = { Text("Area Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (areaName.isNotBlank()) {
                            onAddFavoriteParkingArea(areaName)
                            showAddParkingAreaDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParkingAreaDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Update Password Dialog
    if (showUpdatePasswordDialog) {
        var currentPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showUpdatePasswordDialog = false },
            title = { Text("Update Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword == confirmPassword) {
                            onUpdatePassword(currentPassword, newPassword, confirmPassword)
                            showUpdatePasswordDialog = false
                        }
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdatePasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Update Username Dialog
    if (showUpdateUsernameDialog) {
        var newUsername by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showUpdateUsernameDialog = false },
            title = { Text("Update Username") },
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
                        if (validateForm()) {
                            onUpdateUsername(newUsername)
                            showUpdateUsernameDialog = false
                        }
                    },
                    enabled = !isCheckingUsername && isUsernameAvailable == true
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateUsernameDialog = false },
                    enabled = !isCheckingUsername
                ) {
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