package com.adam.habituator.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adam.habituator.data.AppContainer
import com.adam.habituator.data.db.CategoryEntity
import com.adam.habituator.data.db.HabitItemEntity
import com.adam.habituator.data.repository.CategoryRepository
import com.adam.habituator.data.repository.HabitRepository
import com.adam.habituator.data.repository.LogRepository
import com.adam.habituator.domain.AnalyticsCalculator
import com.adam.habituator.domain.GamificationCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RadarAxisUiState(
    val category: CategoryEntity,
    val ratio: Float,
)

data class ItemAnalyticsUiState(
    val item: HabitItemEntity,
    val category: CategoryEntity,
    val totalPoints: Int,
    val level: Int,
    val weeklyVolumes: List<AnalyticsCalculator.WeeklyVolume>,
)

data class AnalyticsUiState(
    val radarAxes: List<RadarAxisUiState> = emptyList(),
    val items: List<ItemAnalyticsUiState> = emptyList(),
    val timeInvestment: List<AnalyticsCalculator.CategoryTimeInvestment> = emptyList(),
)

class AnalyticsViewModel(
    private val categoryRepository: CategoryRepository,
    private val habitRepository: HabitRepository,
    private val logRepository: LogRepository,
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        categoryRepository.observeCategories(),
        habitRepository.observeItems(),
        logRepository.observeAll(),
    ) { categories, items, logs ->
        val logsByItem = logs.groupBy { it.habitItemId }
        val itemsByCategory = items.groupBy { it.categoryId }
        val categoriesById = categories.associateBy { it.id }

        val radarAxes = categories.map { category ->
            RadarAxisUiState(
                category = category,
                ratio = AnalyticsCalculator.categoryGoalMetRatio(
                    items = itemsByCategory[category.id].orEmpty(),
                    logsByItem = logsByItem,
                ),
            )
        }

        val itemStates = items.mapNotNull { item ->
            val category = categoriesById[item.categoryId] ?: return@mapNotNull null
            val itemLogs = logsByItem[item.id].orEmpty()
            val totalPoints = GamificationCalculator.pointsForItem(item, itemLogs)
            ItemAnalyticsUiState(
                item = item,
                category = category,
                totalPoints = totalPoints,
                level = GamificationCalculator.levelForPoints(totalPoints),
                weeklyVolumes = AnalyticsCalculator.weeklyVolumes(item, itemLogs),
            )
        }.sortedWith(compareBy({ it.category.name }, { it.item.name }))

        val timeInvestment = AnalyticsCalculator.medianWeeklySessionsByCategory(
            categories = categories,
            items = items,
            logsByItem = logsByItem,
        )

        AnalyticsUiState(radarAxes = radarAxes, items = itemStates, timeInvestment = timeInvestment)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AnalyticsViewModel(
                    container.categoryRepository,
                    container.habitRepository,
                    container.logRepository,
                ) as T
        }
    }
}
