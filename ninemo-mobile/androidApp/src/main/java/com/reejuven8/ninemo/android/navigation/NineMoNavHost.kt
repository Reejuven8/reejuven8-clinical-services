package com.reejuven8.ninemo.android.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.reejuven8.ninemo.android.ui.screens.auth.AbhaLinkScreen
import com.reejuven8.ninemo.android.ui.screens.community.ChannelChatScreen
import com.reejuven8.ninemo.android.ui.screens.community.CommunityScreen
import com.reejuven8.ninemo.android.ui.screens.community.ContentFeedScreen
import com.reejuven8.ninemo.android.ui.screens.profile.ProfileScreen
import com.reejuven8.ninemo.android.ui.screens.auth.LoginScreen
import com.reejuven8.ninemo.android.ui.screens.auth.RegisterScreen
import com.reejuven8.ninemo.android.ui.screens.auth.SplashScreen
import com.reejuven8.ninemo.android.ui.screens.child.ChildDashboardScreen
import com.reejuven8.ninemo.android.ui.screens.child.GrowthChartScreen
import com.reejuven8.ninemo.android.ui.screens.child.MilestoneScreen
import com.reejuven8.ninemo.android.ui.screens.child.ModeTransitionScreen
import com.reejuven8.ninemo.android.ui.screens.child.VaccinationScreen
import com.reejuven8.ninemo.android.ui.screens.locker.ConsentManagerScreen
import com.reejuven8.ninemo.android.ui.screens.locker.DocumentDetailScreen
import com.reejuven8.ninemo.android.ui.screens.locker.HealthLockerScreen
import com.reejuven8.ninemo.android.ui.screens.onboarding.OnboardingScreen
import com.reejuven8.ninemo.android.ui.screens.timeline.TimelineScreen
import com.reejuven8.ninemo.android.ui.screens.tools.ContractionTimerScreen
import com.reejuven8.ninemo.android.ui.screens.tools.DietScreen
import com.reejuven8.ninemo.android.ui.screens.tools.KickCounterScreen
import com.reejuven8.ninemo.android.ui.screens.tools.SummaryCardScreen
import com.reejuven8.ninemo.android.ui.screens.tools.SymptomLogScreen
import com.reejuven8.ninemo.android.ui.screens.tools.ToolsHubScreen
import com.reejuven8.ninemo.android.ui.screens.tools.VitalsScreen
import com.reejuven8.ninemo.shared.model.VitalType
import com.reejuven8.ninemo.shared.viewmodel.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NineMoNavHost() {
    val session: SessionViewModel = koinViewModel()
    val isAuthenticated by session.isAuthenticated.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by session.hasCompletedOnboarding.collectAsStateWithLifecycle()

    if (isAuthenticated && hasCompletedOnboarding) MainShell(session) else AuthNavHost(session)
}

/** Auth + onboarding stack: P0 Splash routes to P1/P2 Login/Register or P4 Onboarding. */
@Composable
private fun AuthNavHost(session: SessionViewModel) {
    val nav = rememberNavController()
    val isAuthenticated by session.isAuthenticated.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by session.hasCompletedOnboarding.collectAsStateWithLifecycle()

    NavHost(
        navController = nav,
        startDestination = Routes.Splash,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<Routes.Splash> {
            SplashScreen(
                isAuthenticated = isAuthenticated,
                hasCompletedOnboarding = hasCompletedOnboarding,
                onNavigateToLogin = { nav.navigate(Routes.Login) { popUpTo(Routes.Splash) { inclusive = true } } },
                onNavigateToOnboarding = { nav.navigate(Routes.Onboarding) { popUpTo(Routes.Splash) { inclusive = true } } },
            )
        }
        composable<Routes.Login> {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.Onboarding) { popUpTo(Routes.Login) { inclusive = true } }
                },
                onGoToRegister = { nav.navigate(Routes.Register) },
            )
        }
        composable<Routes.Register> {
            RegisterScreen(
                onRegistered = {
                    nav.navigate(Routes.Onboarding) { popUpTo(Routes.Register) { inclusive = true } }
                },
                onGoToLogin = { nav.popBackStack() },
            )
        }
        composable<Routes.AbhaLink> {
            AbhaLinkScreen(
                maskedPhone = "",
                onDone = { nav.popBackStack() },
                onSkip = { nav.popBackStack() },
            )
        }
        composable<Routes.Onboarding> {
            OnboardingScreen(onDone = { /* top-level gate swaps to MainShell once onboarding flag flips */ })
        }
    }
}

/** Main shell: 5 bottom tabs, mode-aware Home. */
@Composable
private fun MainShell(session: SessionViewModel) {
    val nav = rememberNavController()
    val isChildMode by session.isChildMode.collectAsStateWithLifecycle()
    val userId by session.userId.collectAsStateWithLifecycle()
    val activeChildId by session.activeChildId.collectAsStateWithLifecycle()
    val activeChildName by session.activeChildName.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            val backStack by nav.currentBackStackEntryAsState()
            val current = backStack?.destination
            NavigationBar {
                BottomTab.entries.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable<Routes.Home> {
                val childId = activeChildId
                if (isChildMode && childId != null) {
                    ChildDashboardScreen(
                        childId = childId,
                        childName = activeChildName,
                        onLogGrowth = { nav.navigate(Routes.GrowthChart(childId)) },
                        onMilestones = { nav.navigate(Routes.Milestones(childId)) },
                        onVaccines = { nav.navigate(Routes.Vaccination(childId)) },
                    )
                } else {
                    TimelineScreen(
                        onLogSymptom = { nav.navigate(Routes.SymptomLog) },
                        onLogVitals = { nav.navigate(Routes.VitalsWeight) },
                        onSummaryCard = { nav.navigate(Routes.SummaryCard(userId ?: "")) },
                        onBabyArrived = { nav.navigate(Routes.ModeTransition) },
                    )
                }
            }
            composable<Routes.Locker> {
                HealthLockerScreen(
                    onOpenRecord = { recordId -> nav.navigate(Routes.DocumentDetail(recordId)) },
                    onOpenConsents = { nav.navigate(Routes.Consent) },
                )
            }
            composable<Routes.Tools> {
                ToolsHubScreen(
                    currentWeek = null,
                    onSymptomLog = { nav.navigate(Routes.SymptomLog) },
                    onVitalsWeight = { nav.navigate(Routes.VitalsWeight) },
                    onKickCounter = { nav.navigate(Routes.KickCounter) },
                    onContractionTimer = { nav.navigate(Routes.ContractionTimer) },
                    onDiet = { nav.navigate(Routes.Diet) },
                    onSummaryCard = { nav.navigate(Routes.SummaryCard(userId ?: "")) },
                )
            }
            composable<Routes.Community> {
                CommunityScreen(
                    onOpenChannel = { clubId, channelId, channelName, alias ->
                        nav.navigate(Routes.ChannelChat(clubId, channelId, channelName, alias))
                    },
                    onOpenContent = { nav.navigate(Routes.ContentFeed) },
                )
            }
            composable<Routes.Profile> {
                ProfileScreen(
                    onAbhaLink = { nav.navigate(Routes.AbhaLink) },
                    onConsents = { nav.navigate(Routes.Consent) },
                    onLoggedOut = { /* top-level gate swaps to AuthNavHost once session clears */ },
                )
            }
            composable<Routes.ChannelChat> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.ChannelChat>()
                ChannelChatScreen(
                    clubId = route.clubId,
                    channelId = route.channelId,
                    channelName = route.channelName,
                    alias = route.alias,
                    onBack = { nav.popBackStack() },
                )
            }
            composable<Routes.ContentFeed> { ContentFeedScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.AbhaLink> {
                AbhaLinkScreen(maskedPhone = "", onDone = { nav.popBackStack() }, onSkip = { nav.popBackStack() })
            }

            composable<Routes.SymptomLog> { SymptomLogScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.VitalsWeight> { VitalsScreen(initialTab = VitalType.WEIGHT, onBack = { nav.popBackStack() }) }
            composable<Routes.VitalsBP> { VitalsScreen(initialTab = VitalType.BLOOD_PRESSURE, onBack = { nav.popBackStack() }) }
            composable<Routes.KickCounter> { KickCounterScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.ContractionTimer> { ContractionTimerScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.Diet> { DietScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.SummaryCard> { SummaryCardScreen(onBack = { nav.popBackStack() }) }
            composable<Routes.DocumentDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.DocumentDetail>()
                DocumentDetailScreen(recordId = route.recordId, onBack = { nav.popBackStack() })
            }
            composable<Routes.Consent> { ConsentManagerScreen(onBack = { nav.popBackStack() }) }

            // F5 — Child mode
            composable<Routes.GrowthChart> { backStackEntry ->
                GrowthChartScreen(childId = backStackEntry.toRoute<Routes.GrowthChart>().childId, onBack = { nav.popBackStack() })
            }
            composable<Routes.Vaccination> { backStackEntry ->
                VaccinationScreen(childId = backStackEntry.toRoute<Routes.Vaccination>().childId, onBack = { nav.popBackStack() })
            }
            composable<Routes.Milestones> { backStackEntry ->
                MilestoneScreen(childId = backStackEntry.toRoute<Routes.Milestones>().childId, onBack = { nav.popBackStack() })
            }
            composable<Routes.ModeTransition> {
                ModeTransitionScreen(
                    onCancel = { nav.popBackStack() },
                    onSwitched = { nav.popBackStack() }, // top-level gate swaps Home to child dashboard
                )
            }
        }
    }
}
