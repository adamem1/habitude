package com.adam.habituator.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adam.habituator.data.AppContainer
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.db.LogEntryEntity
import com.adam.habituator.data.repository.CategoryRepository
import com.adam.habituator.data.repository.HabitRepository
import com.adam.habituator.data.repository.LogRepository
import com.adam.habituator.domain.GamificationCalculator
import com.adam.habituator.domain.GamificationConfig
import com.adam.habituator.domain.WeekUtils
import com.adam.habituator.notifications.NotificationChannelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class HabitItemUiState(
    val item: HabitItemEntity,
    val sessionsThisWeek: Int,
    val quantityThisWeek: Double,
    val sessionsToday: Int,
)

data class CategoryWithItems(
    val category: CategoryEntity,
    val items: List<HabitItemUiState>,
    val totalPoints: Int,
    val level: Int,
    val sessionsThisWeek: Int,
)

data class TrackUiState(
    val categories: List<CategoryWithItems> = emptyList(),
)

/** Describes what just happened as a result of a [TrackViewModel.logSession] call, for UI feedback. */
data class LogFeedback(
    val itemName: String,
    val categoryName: String,
    val weeklyGoalJustMet: Boolean,
    /** Non-null if this log pushed the category's points into a new level. */
    val newCategoryLevel: Int? = null,
)

class TrackViewModel(
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val logRepository: LogRepository,
    private val notificationChannelManager: NotificationChannelManager,
) : ViewModel() {

    val uiState: StateFlow<TrackUiState> = combine(
        categoryRepository.observeCategories(),
        habitRepository.observeItems(),
        logRepository.observeAll(),
    ) { categories, items, logs ->
        val now = Instant.now()
        val weekStart = WeekUtils.startOfWeek(now)
        val weekEnd = WeekUtils.endOfWeek(now)
        val dayStart = WeekUtils.startOfDay(now)
        val dayEnd = WeekUtils.endOfDay(now)
        val logsByItem = logs.groupBy { it.habitItemId }

        val itemStates = items.map { item ->
            val itemLogs = logsByItem[item.id].orEmpty()
            val thisWeekLogs = itemLogs.filter { it.loggedAt >= weekStart && it.loggedAt < weekEnd }
            val todayLogs = itemLogs.filter { it.loggedAt >= dayStart && it.loggedAt < dayEnd }
            HabitItemUiState(
                item = item,
                sessionsThisWeek = thisWeekLogs.size,
                quantityThisWeek = thisWeekLogs.sumOf { it.quantity ?: 0.0 },
                sessionsToday = todayLogs.size,
            )
        }
        val itemsByCategory = itemStates.groupBy { it.item.categoryId }

        TrackUiState(
            categories = categories.map { category ->
                val categoryItems = itemsByCategory[category.id].orEmpty()
                val totalPoints = GamificationCalculator.pointsForCategory(
                    items = categoryItems.map { it.item },
                    logsByItem = logsByItem,
                )
                CategoryWithItems(
                    category = category,
                    items = categoryItems.sortedBy { it.item.name },
                    totalPoints = totalPoints,
                    level = GamificationCalculator.levelForPoints(totalPoints),
                    sessionsThisWeek = categoryItems.sumOf { it.sessionsThisWeek },
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    /**
     * Logs a new session and predicts the resulting [LogFeedback] from the current [uiState],
     * so the UI can react immediately without waiting for the database write to round-trip.
     */
    fun logSession(habitItemId: Long, quantity: Double? = null): LogFeedback? {
        val categoryWithItems = uiState.value.categories
            .firstOrNull { cwi -> cwi.items.any { it.item.id == habitItemId } }
        val itemState = categoryWithItems?.items?.firstOrNull { it.item.id == habitItemId }

        notificationChannelManager.cancelItemNotification(habitItemId)
        viewModelScope.launch { logRepository.logSession(habitItemId, quantity) }

        if (categoryWithItems == null || itemState == null) return null
        val item = itemState.item

        val quantityBonus = if (item.tracksQuantity) {
            (quantity ?: 0.0).coerceAtMost(GamificationConfig.MAX_QUANTITY_BONUS.toDouble()).toInt()
        } else {
            0
        }
        val newSessionsThisWeek = itemState.sessionsThisWeek + 1
        val goal = item.weeklyGoalCount
        val weeklyGoalJustMet = goal != null &&
            itemState.sessionsThisWeek < goal &&
            newSessionsThisWeek >= goal

        var pointsFromThisLog = GamificationConfig.BASE_POINTS_PER_LOG + quantityBonus
        if (weeklyGoalJustMet) pointsFromThisLog += GamificationConfig.WEEKLY_GOAL_BONUS

        val newLevel = GamificationCalculator.levelForPoints(categoryWithItems.totalPoints + pointsFromThisLog)
        val leveledUp = newLevel > categoryWithItems.level

        return LogFeedback(
            itemName = item.name,
            categoryName = categoryWithItems.category.name,
            weeklyGoalJustMet = weeklyGoalJustMet,
            newCategoryLevel = if (leveledUp) newLevel else null,
        )
    }

    fun observeLogsForItem(habitItemId: Long): Flow<List<LogEntryEntity>> =
        logRepository.observeForItem(habitItemId)

    fun updateLogEntry(entry: LogEntryEntity) {
        viewModelScope.launch { logRepository.update(entry) }
    }

    fun deleteLogEntry(entry: LogEntryEntity) {
        viewModelScope.launch { logRepository.delete(entry) }
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val id = categoryRepository.save(category)
            notificationChannelManager.syncCategoryGroup(category.copy(id = id))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.delete(category)
            notificationChannelManager.deleteCategoryGroup(category.id)
        }
    }

    fun saveHabitItem(item: HabitItemEntity) {
        viewModelScope.launch {
            val id = habitRepository.save(item)
            val category = uiState.value.categories
                .firstOrNull { it.category.id == item.categoryId }
                ?.category
            if (category != null) {
                notificationChannelManager.syncItemChannel(item.copy(id = id), category)
            }
        }
    }

    fun deleteHabitItem(item: HabitItemEntity) {
        viewModelScope.launch {
            habitRepository.delete(item)
            notificationChannelManager.deleteItemChannel(item.id)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TrackViewModel(
                    container.categoryRepository,
                    container.habitRepository,
                    container.logRepository,
                    container.notificationChannelManager,
                ) as T
        }
    }
}
