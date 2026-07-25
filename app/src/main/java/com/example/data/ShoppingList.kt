package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_lists",
    indices = [Index(value = ["firestoreId"], unique = true)]
)
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdByMemberId: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null
)
