package com.adam.habituator.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = HabitItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitItemId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("habitItemId")],
)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitItemId: Long,
    val loggedAt: Instant,
    val quantity: Double? = null,
)
