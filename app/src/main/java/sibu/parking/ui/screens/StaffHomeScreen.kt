package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffHomeScreen(
    username: String,
    onNavigateToCheckCoupon: () -> Unit,
    onNavigateToReportManagement: () -> Unit,
    onNavigateToStaffMenu: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome, $username") },
                actions = {
                    IconButton(onClick = onNavigateToStaffMenu) {
                        Icon(Icons.Default.Person, contentDescription = "Staff Menu")
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
            // Check Coupon Button
            Button(
                onClick = onNavigateToCheckCoupon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.width(32.dp))
                Text("Check Coupon", style = MaterialTheme.typography.headlineMedium)
            }
            
            // Check Report Button
            Button(
                onClick = onNavigateToReportManagement,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.width(32.dp))
                Text("Report", style = MaterialTheme.typography.headlineMedium)
            }
        }
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