package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    username: String,
    onNavigateToCheckCoupon: () -> Unit,
    onNavigateToReport: () -> Unit,
    onLogout: () -> Unit
) {
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
            Text(
                text = "Staff Portal: $username",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Check Coupon Button
            Button(
                onClick = onNavigateToCheckCoupon,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Check Coupon")
            }
            
            // Report Button
            Button(
                onClick = onNavigateToReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Report, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Report Issue")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StaffHomeScreenPreview() {
    StaffHomeScreen(
        username = "Admin",
        onNavigateToCheckCoupon = { },
        onNavigateToReport = { },
        onLogout = { }
    )
} 