package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarColorHex: String,
    val unallocatedBalance: Double = 0.0,
    val isAdmin: Boolean = false,
    val password: String = "1234"
)
