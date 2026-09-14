package com.example.posrf.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class PosRepository(context: Context) {
    private val dbHelper = PosDatabaseHelper(context)

    suspend fun addProduct(product: Product): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(PosDatabaseHelper.COL_PRODUCT_NAME, product.name)
            put(PosDatabaseHelper.COL_PRODUCT_SKU, product.sku)
            put(PosDatabaseHelper.COL_PRODUCT_COST_PRICE, product.costPrice)
            put(PosDatabaseHelper.COL_PRODUCT_PRICE, product.sellingPrice)
            put(PosDatabaseHelper.COL_PRODUCT_STOCK, product.currentStock)
        }
        db.insert(PosDatabaseHelper.TABLE_PRODUCTS, null, values)
    }

    suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(PosDatabaseHelper.TABLE_PRODUCTS, null, null, null, null, null, "${PosDatabaseHelper.COL_PRODUCT_NAME} ASC")
        
        while (cursor.moveToNext()) {
            products.add(cursorToProduct(cursor))
        }
        cursor.close()
        products
    }

    suspend fun updateProductPrice(productId: Long, newPrice: Double) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(PosDatabaseHelper.COL_PRODUCT_PRICE, newPrice)
        }
        db.update(PosDatabaseHelper.TABLE_PRODUCTS, values, "${PosDatabaseHelper.COL_PRODUCT_ID} = ?", arrayOf(productId.toString()))
    }

    suspend fun deleteProduct(productId: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(PosDatabaseHelper.TABLE_PRODUCTS, "${PosDatabaseHelper.COL_PRODUCT_ID} = ?", arrayOf(productId.toString()))
    }

    suspend fun recordTransaction(transaction: StockTransaction) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(PosDatabaseHelper.COL_TX_PRODUCT_ID, transaction.productId)
                put(PosDatabaseHelper.COL_TX_TYPE, transaction.type.name)
                put(PosDatabaseHelper.COL_TX_QUANTITY, transaction.quantity)
                put(PosDatabaseHelper.COL_TX_PRICE, transaction.pricePerUnit)
                put(PosDatabaseHelper.COL_TX_TIMESTAMP, transaction.timestamp)
            }
            db.insert(PosDatabaseHelper.TABLE_TRANSACTIONS, null, values)

            // Update Stock
            val stockChange = when (transaction.type) {
                TransactionType.IN -> transaction.quantity
                TransactionType.OUT, TransactionType.SELL -> -transaction.quantity
            }
            
            db.execSQL(
                "UPDATE ${PosDatabaseHelper.TABLE_PRODUCTS} SET ${PosDatabaseHelper.COL_PRODUCT_STOCK} = ${PosDatabaseHelper.COL_PRODUCT_STOCK} + ? WHERE ${PosDatabaseHelper.COL_PRODUCT_ID} = ?",
                arrayOf<Any>(stockChange, transaction.productId)
            )
            
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun getTransactionHistory(): List<StockTransactionDetail> = withContext(Dispatchers.IO) {
        val history = mutableListOf<StockTransactionDetail>()
        val db = dbHelper.readableDatabase
        
        val query = """
            SELECT t.*, p.${PosDatabaseHelper.COL_PRODUCT_NAME} 
            FROM ${PosDatabaseHelper.TABLE_TRANSACTIONS} t 
            JOIN ${PosDatabaseHelper.TABLE_PRODUCTS} p ON t.${PosDatabaseHelper.COL_TX_PRODUCT_ID} = p.${PosDatabaseHelper.COL_PRODUCT_ID} 
            ORDER BY t.${PosDatabaseHelper.COL_TX_TIMESTAMP} DESC
        """.trimIndent()
        
        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val productName = cursor.getString(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_NAME))
            val transaction = cursorToTransaction(cursor)
            history.add(StockTransactionDetail(transaction, productName))
        }
        cursor.close()
        history
    }

    suspend fun getSalesReport(startTime: Long): SalesReport = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT SUM(${PosDatabaseHelper.COL_TX_QUANTITY} * ${PosDatabaseHelper.COL_TX_PRICE}) as revenue, 
                   SUM(${PosDatabaseHelper.COL_TX_QUANTITY}) as count 
            FROM ${PosDatabaseHelper.TABLE_TRANSACTIONS} 
            WHERE ${PosDatabaseHelper.COL_TX_TYPE} = 'SELL' AND ${PosDatabaseHelper.COL_TX_TIMESTAMP} >= ?
        """.trimIndent()
        
        val cursor = db.rawQuery(query, arrayOf(startTime.toString()))
        var revenue = 0.0
        var count = 0
        if (cursor.moveToFirst()) {
            revenue = cursor.getDouble(cursor.getColumnIndexOrThrow("revenue"))
            count = cursor.getInt(cursor.getColumnIndexOrThrow("count"))
        }
        cursor.close()
        SalesReport(revenue, count)
    }

    fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun cursorToProduct(cursor: Cursor): Product {
        return Product(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_NAME)),
            sku = cursor.getString(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_SKU)),
            costPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_COST_PRICE)),
            sellingPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_PRICE)),
            currentStock = cursor.getInt(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_PRODUCT_STOCK))
        )
    }

    private fun cursorToTransaction(cursor: Cursor): StockTransaction {
        return StockTransaction(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_ID)),
            productId = cursor.getLong(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_PRODUCT_ID)),
            type = TransactionType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_TYPE))),
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_QUANTITY)),
            pricePerUnit = cursor.getDouble(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_PRICE)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(PosDatabaseHelper.COL_TX_TIMESTAMP))
        )
    }
}
