package com.adam.habituator.notifications

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity

/**
 * Maps each category to a [NotificationChannelGroup] and each habit item to a
 * [NotificationChannel] inside its category's group, so Android's own notification
 * settings give the user per-item and per-category control over reminders.
 *
 * A channel's group can only be set when the channel is first created, so if a habit
 * item is later moved to a different category its existing channel stays listed under
 * its original category group in system settings — a minor cosmetic limitation.
 */
class NotificationChannelManager(context: Context) {

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
        val channel = NotificationChannel(
            itemChannelId(item.id),
            item.name,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            group = categoryGroupId(category.id)
            description = "Reminder to log \"${item.name}\""
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun deleteItemChannel(itemId: Long) {
        notificationManager.deleteNotificationChannel(itemChannelId(itemId))
    }

    companion object {
        fun categoryGroupId(categoryId: Long) = "category_$categoryId"
        fun itemChannelId(itemId: Long) = "habit_$itemId"
    }
}
