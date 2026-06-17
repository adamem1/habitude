package com.adam.habituator.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity

/**
 * Maps each category to a [NotificationChannelGroup] and each habit item to a silent
 * [NotificationChannel] (IMPORTANCE_LOW — no sound or vibration) inside its category's group.
 *
 * Channels are versioned ("habit_v2_<id>"). When [syncItemChannel] runs it deletes any legacy
 * "habit_<id>" channel so old loud notifications are replaced by the silent ones.
 *
 * A channel's group can only be set on first creation, so moving a habit to a different category
 * leaves its channel listed under the original category group in system settings.
 */
class NotificationChannelManager(private val context: Context) {

    private val notificationManager = context.getSystemService<NotificationManager>()!!

    fun syncAll(categories: List<CategoryEntity>, items: List<HabitItemEntity>) {
        val categoriesById = categories.associateBy { it.id }
        categories.forEach(::syncCategoryGroup)
        items.forEach { item ->
            categoriesById[item.categoryId]?.let { category -> syncItemChannel(item, category) }
        }
    }

    fun syncCategoryGroup(category: CategoryEntity) {
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(categoryGroupId(category.id), category.name)
        )
    }

    fun deleteCategoryGroup(categoryId: Long) {
        notificationManager.deleteNotificationChannelGroup(categoryGroupId(categoryId))
    }

    fun syncItemChannel(item: HabitItemEntity, category: CategoryEntity) {
        // Remove legacy loud channel if present
        notificationManager.deleteNotificationChannel("habit_${item.id}")

        val channel = NotificationChannel(
            itemChannelId(item.id),
            item.name,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            group = categoryGroupId(category.id)
            description = "Reminder to log \"${item.name}\""
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun deleteItemChannel(itemId: Long) {
        notificationManager.deleteNotificationChannel(itemChannelId(itemId))
        notificationManager.deleteNotificationChannel("habit_$itemId")
    }

    fun cancelItemNotification(itemId: Long) {
        NotificationManagerCompat.from(context).cancel(itemId.toInt())
    }

    companion object {
        fun categoryGroupId(categoryId: Long) = "category_$categoryId"
        fun itemChannelId(itemId: Long) = "habit_v2_$itemId"
    }
}
