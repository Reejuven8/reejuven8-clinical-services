package com.reejuven8.ninemo.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/** The five bottom-tab roots of the main shell. */
enum class BottomTab(val route: Routes, val label: String, val icon: ImageVector) {
    HOME(Routes.Home, "Home", Icons.Outlined.Home),
    LOCKER(Routes.Locker, "Locker", Icons.Outlined.Folder),
    TOOLS(Routes.Tools, "Tools", Icons.Outlined.Widgets),
    COMMUNITY(Routes.Community, "Community", Icons.Outlined.Groups),
    PROFILE(Routes.Profile, "Profile", Icons.Outlined.Person),
}
