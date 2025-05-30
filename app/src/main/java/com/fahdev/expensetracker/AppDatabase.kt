package com.fahdev.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fahdev.expensetracker.data.Category
import com.fahdev.expensetracker.data.CategoryDao
import com.fahdev.expensetracker.data.Expense
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.Product
import com.fahdev.expensetracker.data.ProductDao
import com.fahdev.expensetracker.data.ShoppingListItem
import com.fahdev.expensetracker.data.ShoppingListItemDao
import com.fahdev.expensetracker.data.Supplier
import com.fahdev.expensetracker.data.SupplierDao

@Database(
    entities = [Expense::class, Product::class, Supplier::class, Category::class, ShoppingListItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ShoppingListItem (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        productId INTEGER NOT NULL,
                        unit TEXT,
                        quantity REAL NOT NULL,
                        unitPrice REAL,
                        supplierId INTEGER,
                        FOREIGN KEY (productId) REFERENCES Product(id) ON DELETE CASCADE,
                        FOREIGN KEY (supplierId) REFERENCES Supplier(id) ON DELETE SET NULL
                    )
                """.trimIndent())
            }
        }
    }
}