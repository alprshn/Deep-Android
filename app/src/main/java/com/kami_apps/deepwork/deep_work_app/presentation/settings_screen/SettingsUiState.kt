package com.kami_apps.deepwork.deep_work_app.presentation.settings_screen

import com.kami_apps.deepwork.deep_work_app.domain.data.AppIcon
import com.kami_apps.deepwork.deep_work_app.domain.data.InstalledApp

data class SettingsUiState(
    // App installation related
    val installedApps: List<InstalledApp> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = true,
    val currentPage: Int = 0,
    val error: String? = null,
    
    // App icon related
    val availableIcons: List<AppIcon> = emptyList(),
    val currentAppIcon: AppIcon? = null,
    val isIconLoading: Boolean = false,
    val iconChangeSuccess: Boolean = false,
    val iconError: String? = null,
    
    // App blocking related
    val blockedApps: List<String> = emptyList()
)