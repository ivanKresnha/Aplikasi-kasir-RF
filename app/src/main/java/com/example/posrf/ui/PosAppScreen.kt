package com.example.posrf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.posrf.data.Product
import com.example.posrf.data.SalesReport
import com.example.posrf.data.StockTransactionDetail
import com.example.posrf.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosAppScreen(viewModel: PosViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val weeklySales by viewModel.weeklySales.collectAsState()
    val monthlySales by viewModel.monthlySales.collectAsState()
    val history by viewModel.transactionHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("POS OFFLINE", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 0.5.sp)
                        Text("Aman & Mandiri", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple(0, Icons.Rounded.Storefront, "Jual"),
                    Triple(1, Icons.Rounded.Inventory2, "Stok"),
                    Triple(2, Icons.Rounded.ReceiptLong, "Riwayat"),
                    Triple(3, Icons.Rounded.BarChart, "Laporan")
                )
                tabs.forEach { (index, icon, label) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp)) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> SalesScreen(products, cart, viewModel)
                1 -> StockScreen(products, viewModel)
                2 -> HistoryScreen(history)
                3 -> ReportsScreen(todaySales, weeklySales, monthlySales)
            }
        }
    }
}

@Composable
fun SalesScreen(products: List<Product>, cart: Map<Product, Int>, viewModel: PosViewModel) {
    var showCartView by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple sub-tab switcher for absolute phone screens responsiveness
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { showCartView = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showCartView) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!showCartView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null
            ) {
                Icon(Icons.Rounded.GridView, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pilih Barang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            
            Button(
                onClick = { showCartView = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showCartView) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (showCartView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = null
            ) {
                BadgedBox(badge = {
                    if (cart.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${cart.values.sum()}", color = Color.White)
                        }
                    }
                }) {
                    Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Keranjang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        if (!showCartView) {
            // Responsive list for phone screen size
            if (products.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada barang. Silakan tambah di menu Stok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addToCart(product) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                              ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        product.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Rp ${formatPrice(product.sellingPrice)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (product.currentStock < 5) Color(0xFF5C1D1D) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Stok: ${product.currentStock}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.currentStock < 5) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Cart View panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (cart.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Keranjang masih kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(cart.toList()) { (product, qty) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(product.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Rp ${formatPrice(product.sellingPrice)} x $qty", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(product) },
                                    modifier = Modifier.background(Color(0xFF3B2424), CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFFFCA5A5))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                    
                    val total = cart.entries.sumOf { it.key.sellingPrice * it.value }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Belanja", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rp ${formatPrice(total)}", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.checkout(); showCartView = false },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("KONFIRMASI BAYAR", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun StockScreen(products: List<Product>, viewModel: PosViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForStock by remember { mutableStateOf<Product?>(null) }
    var stockAction by remember { mutableStateOf("") }

    val totalUnique by viewModel.totalUniqueProducts.collectAsState()
    val totalQty by viewModel.totalStockQuantity.collectAsState()
    val totalValue by viewModel.totalInventoryValue.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.White)
            }
        }
    ) { p ->
        LazyColumn(modifier = Modifier.padding(p).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                StockSummaryHeader(totalUnique, totalQty, totalValue)
                Spacer(Modifier.height(12.dp))
                Text("Daftar Barang & Stok", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            
            if (products.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada barang terdaftar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(product.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        
                        // Adaptive horizontal row wraps safely or sizes proportionally
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Modal: Rp ${formatPrice(product.costPrice)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Jual: Rp ${formatPrice(product.sellingPrice)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }
                            Text("Stok: ${product.currentStock} pcs", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { selectedProductForStock = product; stockAction = "IN" }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" TAMBAH", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { selectedProductForStock = product; stockAction = "OUT" }, 
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(" KURANG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { viewModel.deleteProduct(product) }, 
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(" HAPUS BARANG", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(onDismiss = { showAddDialog = false }, onConfirm = { name, sku, costPrice, sellingPrice, stock ->
            viewModel.addProduct(name, sku, costPrice, sellingPrice, stock)
            showAddDialog = false
        })
    }

    if (selectedProductForStock != null) {
        StockUpdateDialog(
            product = selectedProductForStock!!,
            action = stockAction,
            onDismiss = { selectedProductForStock = null },
            onConfirm = { qty ->
                if (stockAction == "IN") viewModel.stockIn(selectedProductForStock!!, qty)
                else viewModel.stockOut(selectedProductForStock!!, qty)
                selectedProductForStock = null
            }
        )
    }
}

@Composable
fun StockSummaryHeader(totalUnique: Int, totalQty: Int, totalValue: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Vertical stacking or well-padded responsive grids prevent side cutting on 300dp-360dp phone screens
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ringkasan Inventaris Toko", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Jenis Barang", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalUnique", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Total Stok", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalQty pcs", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            Column {
                Text("Total Nilai Aset Modal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Rp ${formatPrice(totalValue)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun HistoryScreen(history: List<StockTransactionDetail>) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Riwayat Transaksi", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        
        if (history.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat aktivitas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        items(history) { detail ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val pair = when (detail.transaction.type) {
                        TransactionType.IN -> Icons.Rounded.ArrowCircleUp to Color(0xFF34D399)
                        TransactionType.SELL -> Icons.Rounded.ShoppingBag to Color(0xFFF87171)
                        TransactionType.OUT -> Icons.Rounded.ChangeCircle to Color(0xFFFBBF24)
                    }
                    val icon = pair.first
                    val tint = pair.second
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(tint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(detail.productName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(formatDateTime(detail.transaction.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            when(detail.transaction.type) {
                                TransactionType.IN -> "Masuk"
                                TransactionType.SELL -> "Terjual"
                                TransactionType.OUT -> "Keluar"
                            },
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = tint
                        )
                        Text(
                            "${if (detail.transaction.type == TransactionType.IN) "+" else "-"}${detail.transaction.quantity}", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Barang", fontSize = 20.sp, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = costPrice, onValueChange = { costPrice = it }, label = { Text("Harga Modal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = sellingPrice, onValueChange = { sellingPrice = it }, label = { Text("Harga Jual (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stok Awal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, "", costPrice.toDoubleOrNull() ?: 0.0, sellingPrice.toDoubleOrNull() ?: 0.0, stock.toIntOrNull() ?: 0) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun StockUpdateDialog(product: Product, action: String, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var qty by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (action == "IN") "Restok: ${product.name}" else "Kurangi: ${product.name}", 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qty, 
                    onValueChange = { 
                        qty = it
                        errorMessage = null 
                    }, 
                    label = { Text("Jumlah (pcs)") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = errorMessage != null
                )
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val inputQty = qty.toIntOrNull() ?: 0
                    if (action == "OUT" && product.currentStock - inputQty < 0) {
                        errorMessage = "Gagal: Stok tidak boleh kurang dari 0!"
                    } else {
                        onConfirm(inputQty)
                    }
                }, 
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("BATAL") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ReportsScreen(today: SalesReport, weekly: SalesReport, monthly: SalesReport) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Laporan Penjualan", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        item { ReportCard("HARI INI", today, Icons.Rounded.Today, MaterialTheme.colorScheme.primary) }
        item { ReportCard("MINGGU INI", weekly, Icons.Rounded.DateRange, Color(0xFF818CF8)) }
        item { ReportCard("BULAN INI", monthly, Icons.Rounded.CalendarMonth, Color(0xFFA78BFA)) }
    }
}

@Composable
fun ReportCard(title: String, report: SalesReport, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                Text("Rp ${formatPrice(report.totalRevenue)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Text("${report.itemsSold} item terjual", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun formatPrice(amount: Double): String {
    val formatter = NumberFormat.getInstance(Locale("id", "ID"))
    return formatter.format(amount)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}
