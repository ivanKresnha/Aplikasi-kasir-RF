package com.example.posrf.data

/**
 * Represents a product in the store.
 */
data class Product(
    val id: Long = 0,
    val name: String,
    val sku: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val currentStock: Int
)

/**
 * Represents a stock movement (Restock, Sale, or Adjustment).
 */
data class StockTransaction(
    val id: Long = 0,
    val productId: Long,
    val type: TransactionType,
    val quantity: Int,
    val pricePerUnit: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    IN,   // Restocking
    OUT,  // Damage or Adjustment
    SELL  // Customer Purchase
}

/**
 * Aggregated sales data for reports.
 */
data class SalesReport(
    val totalRevenue: Double,
    val itemsSold: Int
)

/**
 * Detailed transaction info for history list.
 */
data class StockTransactionDetail(
    val transaction: StockTransaction,
    val productName: String
)
