package com.example.nammasantheledger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val name: String,
    val phoneNumber: String? = null,
    val photoUri: String? = null
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val amount: Double,
    val type: String, // "GIVE" or "TAKE"
    val timestamp: Long = System.currentTimeMillis()
)
