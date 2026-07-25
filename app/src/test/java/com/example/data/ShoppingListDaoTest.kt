package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShoppingListDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ShoppingListDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.shoppingListDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `deleting a shopping list cascades through shop to items`() = runBlocking {
        val listId = dao.insertShoppingList(ShoppingList(name = "Weekly groceries", createdByMemberId = 1)).toInt()
        val shopId = dao.insertShop(Shop(listId = listId, name = "Tesco")).toInt()
        dao.insertShoppingItem(ShoppingItem(shopId = shopId, name = "Milk"))

        dao.deleteShoppingListById(listId)

        assertTrue(dao.getAllShops().first().isEmpty())
        assertTrue(dao.getAllShoppingItems().first().isEmpty())
    }
}
