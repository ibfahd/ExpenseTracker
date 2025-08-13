package com.fahdev.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fahdev.expensetracker.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Expense::class,
        Product::class,
        Supplier::class,
        Category::class,
        ShoppingListItem::class,
        CategorySupplierCrossRef::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun categoryDao(): CategoryDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun categorySupplierDao(): CategorySupplierDao

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
                    .addCallback(AppDatabaseCallback(context))
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN iconName TEXT")
                db.execSQL("ALTER TABLE categories ADD COLUMN colorHex TEXT")
                db.execSQL("ALTER TABLE suppliers ADD COLUMN colorHex TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN iconName TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN colorHex TEXT")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE suppliers_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO suppliers_new (id, name, colorHex)
                    SELECT id, name, colorHex FROM suppliers
                """)
                db.execSQL("DROP TABLE suppliers")
                db.execSQL("ALTER TABLE suppliers_new RENAME TO suppliers")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_supplier_cross_ref` (
                        `categoryId` INTEGER NOT NULL, 
                        `supplierId` INTEGER NOT NULL, 
                        PRIMARY KEY(`categoryId`, `supplierId`), 
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                // Create indices for the foreign keys for better performance
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_category_supplier_cross_ref_supplierId` ON `category_supplier_cross_ref` (`supplierId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add the new 'date' and 'creationTimestamp' columns with default values
                db.execSQL("ALTER TABLE expenses ADD COLUMN date INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN creationTimestamp INTEGER NOT NULL DEFAULT 0")

                // 2. Copy the data from the old 'timestamp' column to both new columns
                db.execSQL("UPDATE expenses SET date = timestamp, creationTimestamp = timestamp")

                // 3. Re-create the table without the old 'timestamp' column
                db.execSQL("""
                    CREATE TABLE expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        productId INTEGER NOT NULL,
                        supplierId INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        creationTimestamp INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE RESTRICT,
                        FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON DELETE RESTRICT
                    )
                """)
                db.execSQL("""
                    INSERT INTO expenses_new (id, amount, productId, supplierId, date, creationTimestamp)
                    SELECT id, amount, productId, supplierId, date, creationTimestamp FROM expenses
                """)
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")

                // 4. Re-create indices on the new table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_productId` ON `expenses` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_supplierId` ON `expenses` (`supplierId`)")
            }
        }
    }

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