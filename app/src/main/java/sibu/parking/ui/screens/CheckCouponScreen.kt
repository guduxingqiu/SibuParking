package sibu.parking.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import sibu.parking.model.CouponUsage
import sibu.parking.model.ParkingCoupon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckCouponScreen(
    onSearchByVehicle: (String) -> Unit,
    onSearchByLocation: (String, String) -> Unit,
    couponResults: List<ParkingCoupon>,
    usageResults: List<CouponUsage>,
    isLoading: Boolean,
    onBackClick: () -> Unit
) {
    var vehicleNumber by remember { mutableStateOf("") }
    var parkingArea by remember { mutableStateOf("") }
    var parkingLotNumber by remember { mutableStateOf("") }
    var searchTab by remember { mutableStateOf(0) } // 0 for Vehicle, 1 for Location
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check Parking Coupon") },
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
                .padding(16.dp)
        ) {
            // Search Tabs
            TabRow(selectedTabIndex = searchTab) {
                Tab(
                    selected = searchTab == 0,
                    onClick = { searchTab = 0 },
                    text = { Text("Search by Vehicle") },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
                )
                
                Tab(
                    selected = searchTab == 1,
                    onClick = { searchTab = 1 },
                    text = { Text("Search by Location") },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Inputs
            when (searchTab) {
                0 -> {
                    // Vehicle Search
                    OutlinedTextField(
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        label = { Text("Vehicle Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = { 
                                    if (vehicleNumber.isNotBlank()) {
                                        onSearchByVehicle(vehicleNumber)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    )
                }
                1 -> {
                    // Location Search
                    Column {
                        OutlinedTextField(
                            value = parkingArea,
                            onValueChange = { parkingArea = it },
                            label = { Text("Parking Area") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = parkingLotNumber,
                            onValueChange = { parkingLotNumber = it },
                            label = { Text("Parking Lot Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        Button(
                            onClick = { 
                                if (parkingArea.isNotBlank() && parkingLotNumber.isNotBlank()) {
                                    onSearchByLocation(parkingArea, parkingLotNumber)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Search")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (couponResults.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No digital coupon found for this search",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn {
                        item {
                            Text(
                                text = "Coupon Results",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        items(couponResults) { coupon ->
                            CouponResultCard(coupon = coupon)
                        }
                        
                        if (usageResults.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Usage History",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(usageResults) { usage ->
                                UsageHistoryCard(usage = usage)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponResultCard(coupon: ParkingCoupon) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining Uses: ${coupon.remainingUses}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val dateString = sdf.format(Date(coupon.purchaseDate))
                
                Text(
                    text = "Purchased: $dateString",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageHistoryCard(usage: CouponUsage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Area: ${usage.parkingArea}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Lot: ${usage.parkingLotNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vehicle: ${usage.vehicleNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Uses: ${usage.usedCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dateString = sdf.format(Date(usage.timestamp))
            
            Text(
                text = "Used on: $dateString",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 