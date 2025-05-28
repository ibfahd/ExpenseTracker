package com.fahdev.expensetracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Add new entities and increment the version number!
@Database(
    entities = [Expense::class, Product::class, Supplier::class, Category::class], // Added Category entity
    version = 2, // IMPORTANT: Increment database version again! (from 1 to 2)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun categoryDao(): CategoryDao // New DAO method

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
                    // !!! IMPORTANT: Add a migration strategy if you don't want to lose data
                    // For now, we'll continue to use fallbackToDestructiveMigration for simplicity in learning.
                    // In a real app, you'd write a proper Migration.
                    .fallbackToDestructiveMigration(true) // THIS WILL WIPE ALL OLD DATA on schema change!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}