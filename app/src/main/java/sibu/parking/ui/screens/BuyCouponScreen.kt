package sibu.parking.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sibu.parking.model.Cart
import sibu.parking.model.CartItem
import sibu.parking.model.CouponType
import sibu.parking.model.PaymentMethod
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponPackageCard(
    title: String,
    description: String,
    price: String,
    onAddToCart: () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Button(
                    onClick = onAddToCart
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add to Cart")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyCouponScreen(
    cart: Cart,
    onAddToCart: (CouponType) -> Unit,
    onRemoveFromCart: (Int) -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onCheckout: (PaymentMethod) -> Unit,
    onBackClick: () -> Unit
) {
    var showCartSheet by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buy Parking Coupon") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCartSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (cart.items.isNotEmpty()) {
                                    Badge {
                                        Text(cart.items.sumOf { it.quantity }.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
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
            Text(
                text = "Available Parking Coupons",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Coupon packages
            CouponPackageCard(
                title = "30 Minutes Coupon",
                description = "Valid for 10 uses",
                price = "RM 4.25",
                onAddToCart = { onAddToCart(CouponType.MINUTES_30) }
            )
            
            CouponPackageCard(
                title = "1 Hour Coupon",
                description = "Valid for 10 uses",
                price = "RM 8.50",
                onAddToCart = { onAddToCart(CouponType.HOUR_1) }
            )
            
            CouponPackageCard(
                title = "2 Hours Coupon",
                description = "Valid for 10 uses",
                price = "RM 16.95",
                onAddToCart = { onAddToCart(CouponType.HOURS_2) }
            )
            
            CouponPackageCard(
                title = "24 Hours Coupon",
                description = "Valid for 10 uses",
                price = "RM 63.60",
                onAddToCart = { onAddToCart(CouponType.HOURS_24) }
            )
        }
    }
    
    // Shopping Cart Bottom Sheet
    if (showCartSheet) {
        ShoppingCartBottomSheet(
            cart = cart,
            onDismiss = { showCartSheet = false },
            onRemoveItem = onRemoveFromCart,
            onUpdateQuantity = onUpdateQuantity,
            onCheckout = { paymentMethod -> 
                onCheckout(paymentMethod)
                showCartSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartBottomSheet(
    cart: Cart,
    onDismiss: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onUpdateQuantity: (Int, Int) -> Unit,
    onCheckout: (PaymentMethod) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
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
                    text = "Shopping Cart",
                    style = MaterialTheme.typography.titleLarge
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (cart.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(cart.items) { index, item ->
                        CartItemRow(
                            item = item,
                            onRemove = { onRemoveItem(index) },
                            onQuantityChange = { newQuantity -> onUpdateQuantity(index, newQuantity) }
                        )
                        
                        if (index < cart.items.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Total
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = cart.getFormattedTotalPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Payment method selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Select Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethodButton(
                            title = "Online Banking (FPX)",
                            selected = selectedPaymentMethod == PaymentMethod.ONLINE_BANKING,
                            onClick = { selectedPaymentMethod = PaymentMethod.ONLINE_BANKING }
                        )
                        
                        PaymentMethodButton(
                            title = "E-Wallet",
                            selected = selectedPaymentMethod == PaymentMethod.E_WALLET,
                            onClick = { selectedPaymentMethod = PaymentMethod.E_WALLET }
                        )
                    }
                }
                
                // Checkout button
                Button(
                    onClick = { 
                        selectedPaymentMethod?.let { onCheckout(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(vertical = 8.dp),
                    enabled = selectedPaymentMethod != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Proceed to Payment")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.getDisplayName(),
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = item.getFormattedUnitPrice(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) }
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            
            Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
            
            IconButton(
                onClick = { onQuantityChange(item.quantity + 1) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
        
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 