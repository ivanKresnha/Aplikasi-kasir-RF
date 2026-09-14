package com.example.posrf.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PosDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pos_offline.db"
        private const val DATABASE_VERSION = 2

        // Table Products
        const val TABLE_PRODUCTS = "products"
        const val COL_PRODUCT_ID = "id"
        const val COL_PRODUCT_NAME = "name"
        const val COL_PRODUCT_SKU = "sku"
        const val COL_PRODUCT_COST_PRICE = "cost_price"
        const val COL_PRODUCT_PRICE = "selling_price"
        const val COL_PRODUCT_STOCK = "current_stock"

        // Table Transactions
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TX_ID = "id"
        const val COL_TX_PRODUCT_ID = "product_id"
        const val COL_TX_TYPE = "type" // IN, OUT, SELL
        const val COL_TX_QUANTITY = "quantity"
        const val COL_TX_PRICE = "price_per_unit"
        const val COL_TX_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProductsTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COL_PRODUCT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PRODUCT_NAME TEXT NOT NULL,
                $COL_PRODUCT_SKU TEXT UNIQUE,
                $COL_PRODUCT_COST_PRICE REAL DEFAULT 0.0,
                $COL_PRODUCT_PRICE REAL NOT NULL,
                $COL_PRODUCT_STOCK INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COL_TX_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TX_PRODUCT_ID INTEGER NOT NULL,
                $COL_TX_TYPE TEXT NOT NULL,
                $COL_TX_QUANTITY INTEGER NOT NULL,
                $COL_TX_PRICE REAL NOT NULL,
                $COL_TX_TIMESTAMP INTEGER NOT NULL,
                FOREIGN KEY($COL_TX_PRODUCT_ID) REFERENCES $TABLE_PRODUCTS($COL_PRODUCT_ID)
            )
        """.trimIndent()

        db.execSQL(createProductsTable)
        db.execSQL(createTransactionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_PRODUCTS ADD COLUMN $COL_PRODUCT_COST_PRICE REAL DEFAULT 0.0")
        }
    }
}
