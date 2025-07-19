package com.fahdev.expensetracker.di

import android.content.Context
import com.fahdev.expensetracker.AppDatabase
import com.fahdev.expensetracker.data.CategoryDao
import com.fahdev.expensetracker.data.ExpenseDao
import com.fahdev.expensetracker.data.ExpenseRepository
import com.fahdev.expensetracker.data.ProductDao
import com.fahdev.expensetracker.data.ShoppingListItemDao
import com.fahdev.expensetracker.data.ShoppingRepository
import com.fahdev.expensetracker.data.SupplierDao
import com.fahdev.expensetracker.data.UserPreferencesRepository
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
    fun provideExpenseDao(appDatabase: AppDatabase): ExpenseDao {
        return appDatabase.expenseDao()
    }

    @Provides
    fun provideProductDao(appDatabase: AppDatabase): ProductDao {
        return appDatabase.productDao()
    }

    @Provides
    fun provideSupplierDao(appDatabase: AppDatabase): SupplierDao {
        return appDatabase.supplierDao()
    }

    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }

    @Provides
    fun provideShoppingListItemDao(appDatabase: AppDatabase): ShoppingListItemDao {
        return appDatabase.shoppingListItemDao()
    }

    // --- Repository Providers ---

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        productDao: ProductDao,
        supplierDao: SupplierDao,
        categoryDao: CategoryDao
    ): ExpenseRepository {
        return ExpenseRepository(expenseDao, productDao, supplierDao, categoryDao)
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
