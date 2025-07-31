package com.fahdev.expensetracker.di

import android.content.Context
import com.fahdev.expensetracker.AppDatabase
import com.fahdev.expensetracker.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Database and DAO Providers ---

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideExpenseDao(appDatabase: AppDatabase): ExpenseDao = appDatabase.expenseDao()

    @Provides
    fun provideProductDao(appDatabase: AppDatabase): ProductDao = appDatabase.productDao()

    @Provides
    fun provideSupplierDao(appDatabase: AppDatabase): SupplierDao = appDatabase.supplierDao()

    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao = appDatabase.categoryDao()

    @Provides
    fun provideShoppingListItemDao(appDatabase: AppDatabase): ShoppingListItemDao = appDatabase.shoppingListItemDao()

    @Provides
    fun provideCategorySupplierDao(appDatabase: AppDatabase): CategorySupplierDao = appDatabase.categorySupplierDao() // <-- ADDED

    // --- Repository Providers ---

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        productDao: ProductDao,
        supplierDao: SupplierDao,
        categoryDao: CategoryDao,
        categorySupplierDao: CategorySupplierDao // <-- ADDED
    ): ExpenseRepository {
        return ExpenseRepository(expenseDao, productDao, supplierDao, categoryDao, categorySupplierDao) // <-- ADDED
    }

    @Provides
    @Singleton
    fun provideShoppingRepository(
        shoppingListItemDao: ShoppingListItemDao,
        expenseDao: ExpenseDao,
        productDao: ProductDao,
        supplierDao: SupplierDao
    ): ShoppingRepository {
        return ShoppingRepository(shoppingListItemDao, expenseDao, productDao, supplierDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository.getInstance(context)
    }
}
