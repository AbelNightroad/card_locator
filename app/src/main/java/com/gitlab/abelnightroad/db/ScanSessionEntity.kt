package com.gitlab.abelnightroad.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: String
)
