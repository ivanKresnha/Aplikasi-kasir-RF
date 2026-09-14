package com.example.posrf.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.posrf.data.PosRepository
import com.example.posrf.data.Product
import com.example.posrf.data.SalesReport
import com.example.posrf.data.StockTransaction
import com.example.posrf.data.StockTransactionDetail
import com.example.posrf.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository(application)

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _cart = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cart: StateFlow<Map<Product, Int>> = _cart.asStateFlow()

    private val _todaySales = MutableStateFlow(SalesReport(0.0, 0))
    val todaySales: StateFlow<SalesReport> = _todaySales.asStateFlow()

    private val _weeklySales = MutableStateFlow(SalesReport(0.0, 0))
    val weeklySales: StateFlow<SalesReport> = _weeklySales.asStateFlow()

    private val _monthlySales = MutableStateFlow(SalesReport(0.0, 0))
    val monthlySales: StateFlow<SalesReport> = _monthlySales.asStateFlow()

    private val _transactionHistory = MutableStateFlow<List<StockTransactionDetail>>(emptyList())
    val transactionHistory: StateFlow<List<StockTransactionDetail>> = _transactionHistory.asStateFlow()

    // Stock Summary Stats
    private val _totalUniqueProducts = MutableStateFlow(0)
    val totalUniqueProducts: StateFlow<Int> = _totalUniqueProducts.asStateFlow()

    private val _totalStockQuantity = MutableStateFlow(0)
    val totalStockQuantity: StateFlow<Int> = _totalStockQuantity.asStateFlow()

    private val _totalInventoryValue = MutableStateFlow(0.0)
    val totalInventoryValue: StateFlow<Double> = _totalInventoryValue.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val productList = repository.getAllProducts()
            _products.value = productList
            
            // Calculate Stock Summary
            _totalUniqueProducts.value = productList.size
            _totalStockQuantity.value = productList.sumOf { it.currentStock }
            _totalInventoryValue.value = productList.sumOf { it.currentStock * it.costPrice }

            _transactionHistory.value = repository.getTransactionHistory()

            updateReports()
        }
    }

    private suspend fun updateReports() {
        _todaySales.value = repository.getSalesReport(repository.getStartOfDay())
        _weeklySales.value = repository.getSalesReport(repository.getStartOfWeek())
        _monthlySales.value = repository.getSalesReport(repository.getStartOfMonth())
    }

    fun addProduct(name: String, sku: String, costPrice: Double, sellingPrice: Double, initialStock: Int) {
        viewModelScope.launch {
            val newProduct = Product(name = name, sku = sku, costPrice = costPrice, sellingPrice = sellingPrice, currentStock = initialStock)
            val id = repository.addProduct(newProduct)
            if (initialStock > 0 && id > 0) {
                repository.recordTransaction(
                    StockTransaction(
                        productId = id,
                        type = TransactionType.IN,
                        quantity = initialStock,
                        pricePerUnit = sellingPrice
                    )
                )
            }
            loadData()
        }
    }

    fun stockIn(product: Product, quantity: Int) {
        viewModelScope.launch {
            repository.recordTransaction(
                StockTransaction(
                    productId = product.id,
                    type = TransactionType.IN,
                    quantity = quantity,
                    pricePerUnit = product.sellingPrice
                )
            )
            loadData()
        }
    }

    fun stockOut(product: Product, quantity: Int) {
        if (product.currentStock - quantity < 0) return // Abort if it goes negative
        viewModelScope.launch {
            repository.recordTransaction(
                StockTransaction(
                    productId = product.id,
                    type = TransactionType.OUT,
                    quantity = quantity,
                    pricePerUnit = product.sellingPrice
                )
            )
            loadData()
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            // Remove from cart if present
            val currentCart = _cart.value.toMutableMap()
            val cartItem = currentCart.keys.find { it.id == product.id }
            if (cartItem != null) {
                currentCart.remove(cartItem)
                _cart.value = currentCart
            }
            loadData()
        }
    }

    fun updatePrice(product: Product, newPrice: Double) {
        viewModelScope.launch {
            repository.updateProductPrice(product.id, newPrice)
            loadData()
        }
    }

    fun addToCart(product: Product) {
        val currentCart = _cart.value.toMutableMap()
        // Find existing match by ID since currentStock properties could change
        val existingEntry = currentCart.entries.find { it.key.id == product.id }
        val countInCart = existingEntry?.value ?: 0
        
        if (countInCart < product.currentStock) {
            if (existingEntry != null) {
                currentCart[existingEntry.key] = countInCart + 1
            } else {
                currentCart[product] = 1
            }
            _cart.value = currentCart
        }
    }

    fun removeFromCart(product: Product) {
        val currentCart = _cart.value.toMutableMap()
        val existingEntry = currentCart.entries.find { it.key.id == product.id } ?: return
        val countInCart = existingEntry.value
        
        if (countInCart > 1) {
            currentCart[existingEntry.key] = countInCart - 1
        } else {
            currentCart.remove(existingEntry.key)
        }
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun checkout() {
        viewModelScope.launch {
            val currentCart = _cart.value
            for ((product, quantity) in currentCart) {
                repository.recordTransaction(
                    StockTransaction(
                        productId = product.id,
                        type = TransactionType.SELL,
                        quantity = quantity,
                        pricePerUnit = product.sellingPrice
                    )
                )
            }
            clearCart()
            loadData()
        }
    }
}
