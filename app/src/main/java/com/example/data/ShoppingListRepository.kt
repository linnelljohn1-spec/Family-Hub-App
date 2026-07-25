package com.example.data

import kotlinx.coroutines.flow.Flow

class ShoppingListRepository(private val shoppingListDao: ShoppingListDao) {
    val allShoppingLists: Flow<List<ShoppingList>> = shoppingListDao.getAllShoppingLists()
    val allShops: Flow<List<Shop>> = shoppingListDao.getAllShops()
    val allShoppingItems: Flow<List<ShoppingItem>> = shoppingListDao.getAllShoppingItems()

    suspend fun getShoppingListByFirestoreId(firestoreId: String): ShoppingList? =
        shoppingListDao.getShoppingListByFirestoreId(firestoreId)
    suspend fun getShopByFirestoreId(firestoreId: String): Shop? =
        shoppingListDao.getShopByFirestoreId(firestoreId)
    suspend fun getShoppingItemByFirestoreId(firestoreId: String): ShoppingItem? =
        shoppingListDao.getShoppingItemByFirestoreId(firestoreId)

    suspend fun insertShoppingList(list: ShoppingList): Long = shoppingListDao.insertShoppingList(list)
    suspend fun insertShop(shop: Shop): Long = shoppingListDao.insertShop(shop)
    suspend fun insertShoppingItem(item: ShoppingItem): Long = shoppingListDao.insertShoppingItem(item)

    suspend fun deleteShoppingListById(listId: Int) = shoppingListDao.deleteShoppingListById(listId)
    suspend fun deleteShopById(shopId: Int) = shoppingListDao.deleteShopById(shopId)
    suspend fun deleteShoppingItemById(itemId: Int) = shoppingListDao.deleteShoppingItemById(itemId)

    suspend fun clearAll() {
        shoppingListDao.clearShoppingItems()
        shoppingListDao.clearShops()
        shoppingListDao.clearShoppingLists()
    }
}
