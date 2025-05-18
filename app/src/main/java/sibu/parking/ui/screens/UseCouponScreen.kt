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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseCouponScreen(
    coupons: List<ParkingCoupon>,
    favoriteVehicles: List<Vehicle>,
    favoriteParkingAreas: List<ParkingArea>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onUseCoupon: (couponId: String, usedCount: Int, parkingArea: String, parkingLotNumber: String, vehicleNumber: String, startTime: Long) -> Unit
) {
    // 添加调试日志
    LaunchedEffect(coupons) {
        android.util.Log.d("UseCouponScreen", "优惠券列表更新: ${coupons.size}张券")
        coupons.forEachIndexed { index, coupon ->
            android.util.Log.d("UseCouponScreen", "券[$index]: ${coupon.id}, 类型: ${coupon.type}, 剩余: ${coupon.remainingUses}")
        }
    }
    
    var showUseDialog by remember { mutableStateOf(false) }
    var selectedCoupon by remember { mutableStateOf<ParkingCoupon?>(null) }
    var usedCount by remember { mutableStateOf("1") }
    var parkingArea by remember { mutableStateOf("") }
    var parkingLotNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var useCurrentTime by remember { mutableStateOf(true) }
    var customTime by remember { mutableStateOf("") }
    var timeError by remember { mutableStateOf<String?>(null) }
    
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
        useCurrentTime = true
        customTime = ""
        timeError = null
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
        
        // Validate custom time
        if (!useCurrentTime) {
            try {
                if (customTime.length != 4) {
                    timeError = "Time must be in HHmm format"
                    isValid = false
                } else {
                    val hours = customTime.take(2).toInt()
                    val minutes = customTime.takeLast(2).toInt()
                    
                    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                        timeError = "Invalid time format"
                        isValid = false
                    } else {
                        val calendar = Calendar.getInstance()
                        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                        val currentMinute = calendar.get(Calendar.MINUTE)
                        
                        if (hours < currentHour || (hours == currentHour && minutes < currentMinute)) {
                            timeError = "Time cannot be earlier than current time"
                            isValid = false
                        } else {
                            timeError = null
                        }
                    }
                }
            } catch (e: Exception) {
                timeError = "Invalid time format"
                isValid = false
            }
        }
        
        return isValid
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Use Parking Coupon") },
                navigationIcon = {
                    IconButton(onClick = {
                        android.util.Log.d("UseCouponScreen", "返回按钮点击")
                        onBackClick()
                    }) {
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
                android.util.Log.d("UseCouponScreen", "显示加载中...")
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
                android.util.Log.d("UseCouponScreen", "显示无券状态")
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
                                android.util.Log.d("UseCouponScreen", "点击优惠券: ${coupon.id}, 类型: ${coupon.type}")
                                selectedCoupon = coupon
                                showUseDialog = true
                                resetDialogFields()
                            }
                        )
                    }
                }
                android.util.Log.d("UseCouponScreen", "显示券列表")
            }
        }
    }
    
    // Use Coupon Dialog
    if (showUseDialog && selectedCoupon != null) {
        AlertDialog(
            onDismissRequest = { showUseDialog = false },
            title = { Text("Use Coupon") },
            text = {
                Column {
                    OutlinedTextField(
                        value = usedCount,
                        onValueChange = { usedCount = it },
                        label = { Text("Number of Uses") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parkingArea,
                        onValueChange = { parkingArea = it },
                        label = { Text("Parking Area") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showAreaDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Area")
                            }
                        }
                    )
                    if (showAreaDropdown) {
                        DropdownMenu(
                            expanded = showAreaDropdown,
                            onDismissRequest = { showAreaDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            favoriteParkingAreas.forEach { area ->
                                DropdownMenuItem(
                                    text = { Text(area.name) },
                                    onClick = {
                                        parkingArea = area.name
                                        showAreaDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = parkingLotNumber,
                        onValueChange = { parkingLotNumber = it },
                        label = { Text("Parking Lot Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        label = { Text("Vehicle Number") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showVehicleDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Vehicle")
                            }
                        }
                    )
                    if (showVehicleDropdown) {
                        DropdownMenu(
                            expanded = showVehicleDropdown,
                            onDismissRequest = { showVehicleDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            favoriteVehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = { Text(vehicle.licensePlate) },
                                    onClick = {
                                        vehicleNumber = vehicle.licensePlate
                                        showVehicleDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Time selection section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Current Time")
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = useCurrentTime,
                            onCheckedChange = { useCurrentTime = it }
                        )
                    }
                    
                    if (!useCurrentTime) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customTime,
                            onValueChange = { 
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    customTime = it
                                    timeError = null
                                }
                            },
                            label = { Text("Custom Time (HHmm)") },
                            placeholder = { Text("e.g., 0800 or 1530") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            isError = timeError != null,
                            supportingText = {
                                if (timeError != null) {
                                    Text(timeError!!)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (validateForm()) {
                            val count = usedCount.toIntOrNull() ?: 1
                            val startTime = if (useCurrentTime) {
                                System.currentTimeMillis()
                            } else {
                                try {
                                    val hours = customTime.take(2).toInt()
                                    val minutes = customTime.takeLast(2).toInt()
                                    val calendar = Calendar.getInstance()
                                    calendar.set(Calendar.HOUR_OF_DAY, hours)
                                    calendar.set(Calendar.MINUTE, minutes)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    calendar.timeInMillis
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                            }
                            onUseCoupon(selectedCoupon!!.id, count, parkingArea, parkingLotNumber, vehicleNumber, startTime)
                            showUseDialog = false
                        }
                    }
                ) {
                    Text("Use")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUseDialog = false }) {
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