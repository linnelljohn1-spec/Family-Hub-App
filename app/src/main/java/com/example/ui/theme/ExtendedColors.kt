package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LightAppExtendedColors = AppExtendedColors(
    success = SuccessLight,
    onSuccess = OnSuccessLight,
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
)

val DarkAppExtendedColors = AppExtendedColors(
    success = SuccessDark,
    onSuccess = OnSuccessDark,
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
)

val LocalAppExtendedColors = staticCompositionLocalOf { LightAppExtendedColors }

object AppTheme {
    val extendedColors: AppExtendedColors
        @Composable get() = LocalAppExtendedColors.current
}

/** Section accent + soft-container colors for Hub quick-action tiles. */
object FeatureColors {
    val calendar = CalendarAccent
    val calendarContainer = CalendarAccentContainer
    val tasks = TasksAccent
    val tasksContainer = TasksAccentContainer
    val chat = ChatAccent
    val chatContainer = ChatAccentContainer
    val savings = SavingsAccent
    val savingsContainer = SavingsAccentContainer
}

/** Colors for Calendar event-category badges, keyed by category name. Falls back to [EventOther]. */
val eventCategoryColors = mapOf(
    "Family Outing" to EventFamilyOuting,
    "Chore" to EventChore,
    "Birthday" to EventBirthday,
    "Reminder" to EventReminder,
)
