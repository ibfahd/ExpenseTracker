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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Expense::class, Product::class, Supplier::class, Category::class, ShoppingListItem::class],
    version = 5,
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
                    .addCallback(AppDatabaseCallback(context)) // <-- ADDED THIS CALLBACK
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ShoppingListItem (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        unit TEXT,
                        quantity REAL NOT NULL, 
                        unitPrice REAL,
                        supplierId INTEGER,
                        shoppingDate INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (productId) REFERENCES Product(id) ON DELETE CASCADE,
                        FOREIGN KEY (supplierId) REFERENCES Supplier(id) ON DELETE SET NULL
                    )
                """.trimIndent())
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE ShoppingListItem_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        unit TEXT,
                        plannedQuantity REAL NOT NULL,
                        purchasedQuantity REAL NOT NULL,
                        unitPrice REAL,
                        supplierId INTEGER,
                        shoppingDate INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES Product(id) ON DELETE CASCADE,
                        FOREIGN KEY(supplierId) REFERENCES Supplier(id) ON DELETE SET NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO ShoppingListItem_new (id, productId, unit, plannedQuantity, purchasedQuantity, unitPrice, supplierId, shoppingDate)
                    SELECT id, productId, unit, quantity, 0.0, unitPrice, supplierId, shoppingDate 
                    FROM ShoppingListItem
                """)
                db.execSQL("DROP TABLE ShoppingListItem")
                db.execSQL("ALTER TABLE ShoppingListItem_new RENAME TO ShoppingListItem")
            }
        }
    }
    /**
     * Callback for creating the database. This is where we can insert initial data.
     */
    private class AppDatabaseCallback(private val context: Context) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialCategories(database.categoryDao())
                }
            }
        }
        suspend fun populateInitialCategories(categoryDao: CategoryDao) {
            val initialCategories = listOf(
                Category(name = context.getString(R.string.category_food_drinks)),
                Category(name = context.getString(R.string.category_housing)),
                Category(name = context.getString(R.string.category_transportation)),
                Category(name = context.getString(R.string.category_utilities)),
                Category(name = context.getString(R.string.category_healthcare)),
                Category(name = context.getString(R.string.category_personal_care)),
                Category(name = context.getString(R.string.category_shopping)),
                Category(name = context.getString(R.string.category_entertainment)),
                Category(name = context.getString(R.string.category_travel)),
                Category(name = context.getString(R.string.category_education)),
                Category(name = context.getString(R.string.category_savings_investments)),
                Category(name = context.getString(R.string.category_miscellaneous))
            )
            initialCategories.forEach { categoryDao.insertCategory(it) }
        }
    }
}