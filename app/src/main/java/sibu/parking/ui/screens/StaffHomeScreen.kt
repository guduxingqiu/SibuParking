package sibuparking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    username: String,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SibuParking - Staff") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan") },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Staff Portal: $username",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when(selectedTabIndex) {
                            0 -> "Parking lot occupancy overview"
                            1 -> "Scan vehicle QR codes"
                            2 -> "Manage parking settings"
                            else -> ""
                        },
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    when (selectedTabIndex) {
                        0 -> {
                            // Dashboard content
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatCard(title = "Available", value = "42", color = MaterialTheme.colorScheme.primary)
                                StatCard(title = "Occupied", value = "78", color = MaterialTheme.colorScheme.error)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Detailed Report")
                            }
                        }
                        1 -> {
                            // Scan content
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Start Scanning")
                            }
                        }
                        2 -> {
                            // Settings content
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Manage Pricing")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Parking Zones")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = { },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Staff Management")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StaffHomeScreenPreview() {
    StaffHomeScreen(
        username = "Admin",
        onLogout = { }
    )
} 