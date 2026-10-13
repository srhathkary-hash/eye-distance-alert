package com.eyedistance.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribers_table")
data class Subscriber(
    @PrimaryKey val username: String,
    val fullName: String,
    val phoneNumber: String,
    val currentDebt: Double,
    val totalActivations: Int
)
