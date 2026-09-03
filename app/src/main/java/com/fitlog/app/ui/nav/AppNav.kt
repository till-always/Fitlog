package com.fitlog.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fitlog.app.R
import com.fitlog.app.ui.calendar.CalendarScreen
import com.fitlog.app.ui.calendar.DayScreen
import com.fitlog.app.ui.home.HomeScreen
import com.fitlog.app.ui.library.CustomExerciseScreen
import com.fitlog.app.ui.library.ExerciseDetailScreen
import com.fitlog.app.ui.library.LibraryScreen
import com.fitlog.app.ui.plan.PlanDetailScreen
import com.fitlog.app.ui.plan.PlanEditorScreen
import com.fitlog.app.ui.profile.EditProfileScreen
import com.fitlog.app.ui.profile.ProfileScreen
import com.fitlog.app.ui.profile.SettingsScreen
import com.fitlog.app.ui.summary.SummaryScreen
import com.fitlog.app.ui.training.WorkoutScreen
import com.fitlog.app.util.todayStr

/** 训练库"选择模式"的回调总线：计划编辑 / 训练中添加动作时使用（支持多选） */
object PickerBus {
    private var onPick: ((List<Long>) -> Unit)? = null
    fun await(cb: (List<Long>) -> Unit) { onPick = cb }
    fun pick(ids: List<Long>) {
        val cb = onPick
        onPick = null
        cb?.invoke(ids)
    }
    fun cancel() { onPick = null }
}

private data class TabSpec(val route: String, val label: String, val icon: Int)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBar = route == "home" || route == "calendar" || route == "profile"

    val tabs = listOf(
        TabSpec("home", "训练", R.drawable.ic_nav_workout),
        TabSpec("calendar", "日历", R.drawable.ic_nav_calendar),
        TabSpec("profile", "我的", R.drawable.ic_nav_person)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painterResource(tab.icon), contentDescription = tab.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    tab.label, fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad),
            // 轻量转场：淡入 + 轻微上滑，观感流畅不夸张
            enterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(220)) { it / 24 }
            },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) },
            popExitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) +
                    androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { it / 24 }
            }
        ) {
            composable("home") { HomeScreen(nav) }
            composable("calendar") { CalendarScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable(
                "library?pick={pick}",
                arguments = listOf(navArgument("pick") { defaultValue = "0" })
            ) { LibraryScreen(nav, it.arguments?.getString("pick") == "1") }
            composable(
                "exdetail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { ExerciseDetailScreen(nav, it.arguments?.getLong("id") ?: 0L) }
            composable(
                "custom?exId={exId}",
                arguments = listOf(navArgument("exId") { type = NavType.LongType; defaultValue = -1L })
            ) { CustomExerciseScreen(nav, it.arguments?.getLong("exId") ?: -1L) }
            composable(
                "planEdit?planId={planId}&folderId={folderId}",
                arguments = listOf(
                    navArgument("planId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("folderId") { defaultValue = "none" }
                )
            ) {
                PlanEditorScreen(
                    nav,
                    it.arguments?.getLong("planId"),
                    it.arguments?.getString("folderId") ?: "none"
                )
            }
            composable(
                "planDetail/{planId}",
                arguments = listOf(navArgument("planId") { type = NavType.LongType })
            ) { PlanDetailScreen(nav, it.arguments?.getLong("planId") ?: 0L) }
            composable(
                "workout?planId={planId}",
                arguments = listOf(navArgument("planId") { type = NavType.LongType; defaultValue = -1L })
            ) { WorkoutScreen(nav, it.arguments?.getLong("planId")) }
            composable(
                "summary?sessionId={sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType; defaultValue = -1L })
            ) { SummaryScreen(nav, it.arguments?.getLong("sessionId") ?: -1L) }
            composable("day/{date}") { DayScreen(nav, it.arguments?.getString("date") ?: todayStr()) }
            composable("editProfile") { EditProfileScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
        }
    }
}
