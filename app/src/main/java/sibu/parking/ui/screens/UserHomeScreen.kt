package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    username: String,
    email: String,
    onLogout: () -> Unit,
    onNavigateToBuyCoupon: () -> Unit,
    onNavigateToUseCoupon: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToParkingHistory: () -> Unit,
    onNavigateToUserMenu: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, $username") },
                actions = {
                    IconButton(onClick = onNavigateToUserMenu) {
                        Icon(Icons.Default.Person, contentDescription = "User Menu")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onNavigateToBuyCoupon,
                modifier = Modifier.fillMaxWidth()
                ) {
                Text("Buy Coupon")
            }
            
            Button(
                onClick = onNavigateToUseCoupon,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Coupon")
            }
            
            Button(
                onClick = onNavigateToReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Report Issue")
            }
                    
                    Button(
                onClick = onNavigateToParkingHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                Text("Parking History")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserHomeScreenPreview() {
    UserHomeScreen(
        username = "John Doe",
        email = "john.doe@example.com",
        onLogout = { },
        onNavigateToBuyCoupon = { },
        onNavigateToUseCoupon = { },
        onNavigateToReport = { },
        onNavigateToParkingHistory = { },
        onNavigateToUserMenu = { }
    )
} 