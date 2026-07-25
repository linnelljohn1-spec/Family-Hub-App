package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = Shop::class,
            parentColumns = ["id"],
            childColumns = ["shopId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val name: String,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null
)
