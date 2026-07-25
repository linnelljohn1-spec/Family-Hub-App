package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt ASC")
    fun getAllShoppingLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shops ORDER BY createdAt ASC")
    fun getAllShops(): Flow<List<Shop>>

    @Query("SELECT * FROM shopping_items ORDER BY createdAt ASC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_lists WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getShoppingListByFirestoreId(firestoreId: String): ShoppingList?

    @Query("SELECT * FROM shops WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getShopByFirestoreId(firestoreId: String): Shop?

    @Query("SELECT * FROM shopping_items WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getShoppingItemByFirestoreId(firestoreId: String): ShoppingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingList): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: Shop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem): Long

    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteShoppingListById(listId: Int)

    @Query("DELETE FROM shops WHERE id = :shopId")
    suspend fun deleteShopById(shopId: Int)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteShoppingItemById(itemId: Int)

    @Query("DELETE FROM shopping_lists")
    suspend fun clearShoppingLists()

    @Query("DELETE FROM shops")
    suspend fun clearShops()

    @Query("DELETE FROM shopping_items")
    suspend fun clearShoppingItems()
}
