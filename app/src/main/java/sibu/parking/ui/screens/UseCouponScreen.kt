package sibu.parking.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import sibu.parking.model.ParkingArea
import sibu.parking.model.ParkingCoupon
import sibu.parking.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseCouponScreen(
    coupons: List<ParkingCoupon>,
    favoriteVehicles: List<Vehicle>,
    favoriteParkingAreas: List<ParkingArea>,
    onUseCoupon: (couponId: String, usedCount: Int, parkingArea: String, parkingLotNumber: String, vehicleNumber: String) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false
) {
    var selectedCoupon by remember { mutableStateOf<ParkingCoupon?>(null) }
    
    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var usedCount by remember { mutableStateOf("1") }
    var parkingArea by remember { mutableStateOf("") }
    var parkingLotNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    
    // Dropdown menu state
    var showVehicleDropdown by remember { mutableStateOf(false) }
    var showAreaDropdown by remember { mutableStateOf(false) }
    
    // Error states
    var usedCountError by remember { mutableStateOf<String?>(null) }
    var parkingAreaError by remember { mutableStateOf<String?>(null) }
    var parkingLotNumberError by remember { mutableStateOf<String?>(null) }
    var vehicleNumberError by remember { mutableStateOf<String?>(null) }
    
    // Reset dialog fields
    fun resetDialogFields() {
        usedCount = "1"
        parkingArea = ""
        parkingLotNumber = ""
        vehicleNumber = ""
        usedCountError = null
        parkingAreaError = null
        parkingLotNumberError = null
        vehicleNumberError = null
    }
    
    // Validate form
    fun validateForm(): Boolean {
        var isValid = true
        
        // Validate used count
        try {
            val count = usedCount.toInt()
            if (count <= 0) {
                usedCountError = "Count must be positive"
                isValid = false
            } else if (selectedCoupon != null && count > selectedCoupon!!.remainingUses) {
                usedCountError = "Exceeds remaining uses (${selectedCoupon!!.remainingUses})"
                isValid = false
            } else {
                usedCountError = null
            }
        } catch (e: NumberFormatException) {
            usedCountError = "Must be a number"
            isValid = false
        }
        
        // Validate parking area
        if (parkingArea.isBlank()) {
            parkingAreaError = "Required"
            isValid = false
        } else {
            parkingAreaError = null
        }
        
        // Validate parking lot number
        if (parkingLotNumber.isBlank()) {
            parkingLotNumberError = "Required"
            isValid = false
        } else {
            parkingLotNumberError = null
        }
        
        // Validate vehicle number
        if (vehicleNumber.isBlank()) {
            vehicleNumberError = "Required"
            isValid = false
        } else {
            vehicleNumberError = null
        }
        
        return isValid
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Use Parking Coupon") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (coupons.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You don't have any coupons",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = "Available Coupons",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    items(coupons) { coupon ->
                        CouponCard(
                            coupon = coupon,
                            onClick = {
                                selectedCoupon = coupon
                                showDialog = true
                                resetDialogFields()
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Use Coupon Dialog
    if (showDialog && selectedCoupon != null) {
        AlertDialog(
            onDismissRequest = { 
                showDialog = false
                resetDialogFields()
            },
            title = { Text("Use ${selectedCoupon!!.getDisplayName()}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Used Count
                    OutlinedTextField(
                        value = usedCount,
                        onValueChange = { 
                            usedCount = it
                            usedCountError = null
                        },
                        label = { Text("Number of Uses") },
                        singleLine = true,
                        isError = usedCountError != null,
                        supportingText = { usedCountError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    // Parking Area with dropdown
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = parkingArea,
                            onValueChange = { 
                                parkingArea = it
                                parkingAreaError = null
                            },
                            label = { Text("Parking Area") },
                            singleLine = true,
                            isError = parkingAreaError != null,
                            supportingText = { parkingAreaError?.let { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            trailingIcon = {
                                if (favoriteParkingAreas.isNotEmpty()) {
                                    IconButton(onClick = { showAreaDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Parking Area")
                                    }
                                }
                            }
                        )
                        
                        DropdownMenu(
                            expanded = showAreaDropdown,
                            onDismissRequest = { showAreaDropdown = false }
                        ) {
                            favoriteParkingAreas.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.name) },
                                    onClick = {
                                        parkingArea = area.name
                                        parkingAreaError = null
                                        showAreaDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Parking Lot Number
                    OutlinedTextField(
                        value = parkingLotNumber,
                        onValueChange = { 
                            parkingLotNumber = it
                            parkingLotNumberError = null
                        },
                        label = { Text("Parking Lot Number") },
                        singleLine = true,
                        isError = parkingLotNumberError != null,
                        supportingText = { parkingLotNumberError?.let { Text(it) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    // Vehicle Number with dropdown
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = { 
                                vehicleNumber = it
                                vehicleNumberError = null
                            },
                            label = { Text("Vehicle Number") },
                            singleLine = true,
                            isError = vehicleNumberError != null,
                            supportingText = { vehicleNumberError?.let { Text(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            trailingIcon = {
                                if (favoriteVehicles.isNotEmpty()) {
                                    IconButton(onClick = { showVehicleDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Vehicle")
                                    }
                                }
                            }
                        )
                        
                        DropdownMenu(
                            expanded = showVehicleDropdown,
                            onDismissRequest = { showVehicleDropdown = false }
                        ) {
                            favoriteVehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.licensePlate) },
                                    onClick = {
                                        vehicleNumber = vehicle.licensePlate
                                        vehicleNumberError = null
                                        showVehicleDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validateForm()) {
                            onUseCoupon(
                                selectedCoupon!!.id,
                                usedCount.toInt(),
                                parkingArea,
                                parkingLotNumber,
                                vehicleNumber
                            )
                            showDialog = false
                            resetDialogFields()
                        }
                    }
                ) {
                    Text("Use Coupon")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDialog = false
                        resetDialogFields()
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
fun CouponCard(
    coupon: ParkingCoupon,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = coupon.getDisplayName(),
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = coupon.getDescription(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 