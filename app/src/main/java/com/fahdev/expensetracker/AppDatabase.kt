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
    version = 5, // Assuming this is the latest version after your MIGRATION_3_4
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
                    // Add all migrations here. Ensure they are in order.
                    .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2 (existing)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
            }
        }

        // Migration from version 3 to 4 (existing - for initial ShoppingListItem table)
        // Note: If your previous ShoppingListItem table was created in version 3, this migration path is correct.
        // If ShoppingListItem was added in a different version, adjust the startVersion of MIGRATION_4_5.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This migration should create the ShoppingListItem table as it was BEFORE your new fields.
                // The schema here should match the 'ShoppingListItem' table structure at version 4.
                // Based on your provided code, the 'quantity' field existed.
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ShoppingListItem (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        unit TEXT,
                        quantity REAL NOT NULL, 
                        unitPrice REAL,
                        supplierId INTEGER,
                        shoppingDate INTEGER NOT NULL DEFAULT 0, /* Added shoppingDate as it's in your entity */
                        FOREIGN KEY (productId) REFERENCES Product(id) ON DELETE CASCADE,
                        FOREIGN KEY (supplierId) REFERENCES Supplier(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                // If shoppingDate was not in version 4, you'd add it in MIGRATION_4_5 or a separate migration.
                // For simplicity, I'm assuming shoppingDate was part of the table structure at version 4.
                // If not, MIGRATION_3_4 would not have shoppingDate, and MIGRATION_4_5 would add it along with plannedQuantity.
            }
        }


        // NEW MIGRATION: From version 4 to 5 (to modify ShoppingListItem table)
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Rename the old 'quantity' column to 'purchasedQuantity'
                //    We need to create a new table and copy data because SQLite has limited ALTER TABLE support.
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

                // 2. Copy data from the old table to the new table.
                //    Set 'plannedQuantity' to the value of the old 'quantity'.
                //    Set 'purchasedQuantity' to 0.0 initially (or copy old 'quantity' if it represented purchased).
                //    Based on the problem, old 'quantity' was mixed, let's assume it was closer to planned.
                db.execSQL("""
                    INSERT INTO ShoppingListItem_new (id, productId, unit, plannedQuantity, purchasedQuantity, unitPrice, supplierId, shoppingDate)
                    SELECT id, productId, unit, quantity, 0.0, unitPrice, supplierId, shoppingDate 
                    FROM ShoppingListItem
                """)
                // If 'quantity' was meant to be 'purchasedQuantity' and 'plannedQuantity' is new:
                // SELECT id, productId, unit, 0.0, quantity, unitPrice, supplierId, shoppingDate

                // 3. Drop the old table
                db.execSQL("DROP TABLE ShoppingListItem")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE ShoppingListItem_new RENAME TO ShoppingListItem")
            }
        }
    }
}
