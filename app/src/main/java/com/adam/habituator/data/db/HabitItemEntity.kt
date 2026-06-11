package com.adam.habituator.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "habit_items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("categoryId")],
)
data class HabitItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    /** Optional per-habit weekly target. May be left unset for habits whose category has its own weekly goal. */
    val weeklyGoalCount: Int? = null,
    val tracksQuantity: Boolean = false,
    val quantityUnit: String? = null,
    val quantityGoalPerSession: Double? = null,
    val reminderEnabled: Boolean = false,
    /** Bitmask of [java.time.DayOfWeek] values to check for reminders, bit (value - 1) per day. Default: every day. */
    val reminderDaysOfWeek: Int = ALL_DAYS_MASK,
    /** Minutes since midnight at which reminders may start firing, or null for any time of day. */
    val reminderTimeMinutes: Int? = null,
    val archived: Boolean = false,
    val createdAt: Instant,
) {
    companion object {
        const val ALL_DAYS_MASK = 0b1111111
    }
}
