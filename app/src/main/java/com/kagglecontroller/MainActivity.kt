package com.kagglecontroller

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kagglecontroller.core.ui.components.MessageState
import com.kagglecontroller.core.ui.containerViewModel
import com.kagglecontroller.core.ui.rememberContainer
import com.kagglecontroller.core.ui.theme.KaggleControllerTheme
import com.kagglecontroller.feature.auth.AuthUi
import com.kagglecontroller.feature.auth.AuthViewModel
import com.kagglecontroller.feature.auth.LoginScreen
import com.kagglecontroller.feature.editor.EditorScreen
import com.kagglecontroller.feature.explore.ExploreScreen
import com.kagglecontroller.feature.files.FilesScreen
import com.kagglecontroller.feature.home.HomeScreen
import com.kagglecontroller.feature.more.MoreScreen
import com.kagglecontroller.feature.notebooks.NotebooksScreen
import com.kagglecontroller.feature.runs.RunScreen
import com.kagglecontroller.feature.runs.RunsListScreen
import com.kagglecontroller.feature.schedules.SchedulesScreen
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private object Route {
    const val HOME = "home"; const val NOTEBOOKS = "notebooks"; const val SCHEDULES = "schedules"
    const val EXPLORE = "explore"; const val FILES = "files"; const val MORE = "more"; const val RUNS = "runs"
    const val EDITOR = "editor/{id}"; const val RUN = "run/{ref}"
    fun editor(id: String) = "editor/$id"
    fun run(ref: String) = "run/" + URLEncoder.encode(ref, "UTF-8")
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Route.HOME, "Home", Icons.Outlined.Home),
    Tab(Route.NOTEBOOKS, "Notebooks", Icons.Outlined.Code),
    Tab(Route.SCHEDULES, "Schedules", Icons.Outlined.Schedule),
    Tab(Route.EXPLORE, "Explore", Icons.Outlined.Explore),
    Tab(Route.FILES, "Files", Icons.Outlined.Folder),
)

class MainActivity : ComponentActivity() {
    private val pendingLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingLink.value = extractNotebookRef(intent)
        setContent {
            val container = rememberContainer()
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            KaggleControllerTheme(settings.themeMode, settings.dynamicColor) {
                Surface(Modifier.fillMaxSize(), color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    Root(pendingLink.value) { pendingLink.value = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingLink.value = extractNotebookRef(intent)
    }

    /** kaggle.com/code/{owner}/{slug} either opened as a link or shared as text (spec 34). */
    private fun extractNotebookRef(intent: Intent?): String? {
        val text = intent?.dataString ?: intent?.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        val m = Regex("kaggle\\.com/code/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)").find(text) ?: return null
        return m.groupValues[1] + "/" + m.groupValues[2]
    }
}

@Composable
private fun Root(link: String?, onLinkHandled: () -> Unit) {
    val auth = containerViewModel { AuthViewModel(it) }
    val ui by auth.ui.collectAsStateWithLifecycle()
    val busy by auth.busy.collectAsStateWithLifecycle()
    val error by auth.error.collectAsStateWithLifecycle()

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(ui) {
        if (ui is AuthUi.SignedIn && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    when (val s = ui) {
        AuthUi.Checking -> Box(Modifier.fillMaxSize()) { MessageState(Icons.Outlined.Home, "Starting up", "Checking your Kaggle connection...") }
        AuthUi.SignedOut -> LoginScreen(busy, error, auth::signIn)
        is AuthUi.SignedIn -> AppShell(s, link, onLinkHandled, onSignOut = auth::signOut)
    }
}

@Composable
private fun AppShell(s: AuthUi.SignedIn, link: String?, onLinkHandled: () -> Unit, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val showBars = tabs.any { t -> current?.hierarchy?.any { it.route == t.route } == true } || current?.route == Route.MORE || current?.route == Route.RUNS

    LaunchedEffect(link) {
        if (link != null) { nav.navigate(Route.run(link)); onLinkHandled() }
    }

    fun go(route: String) = nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true; restoreState = true
    }

    val reauth = onSignOut

    val content: @Composable (Modifier) -> Unit = { mod ->
        NavHost(nav, startDestination = Route.HOME, modifier = mod) {
            composable(Route.HOME) {
                HomeScreen(
                    s.account.username, s.verified,
                    onNewNotebook = { go(Route.NOTEBOOKS) },
                    onOpenRun = { nav.navigate(Route.run(it)) }, onOpenDraft = { nav.navigate(Route.editor(it)) },
                    onAllRuns = { nav.navigate(Route.RUNS) }, onSearch = { go(Route.EXPLORE) }, onMore = { nav.navigate(Route.MORE) },
                )
            }
            composable(Route.NOTEBOOKS) {
                NotebooksScreen(
                    onOpenEditor = { nav.navigate(Route.editor(it)) }, onOpenNotebook = { nav.navigate(Route.run(it)) },
                    onReauth = reauth, username = s.account.username,
                )
            }
            composable(Route.SCHEDULES) { SchedulesScreen() }
            composable(Route.EXPLORE) { ExploreScreen(onReauth = reauth) }
            composable(Route.FILES) { FilesScreen(onOpenEditor = { nav.navigate(Route.editor(it)) }) }
            composable(Route.RUNS) { RunsListScreen { nav.navigate(Route.run(it)) } }
            composable(Route.MORE) { MoreScreen(s.account.username, s.verified, onSignOut) }
            composable(Route.EDITOR) { e ->
                EditorScreen(
                    draftId = e.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenRun = { ref -> nav.navigate(Route.run(ref)) },
                )
            }
            composable(Route.RUN) { e ->
                val ref = URLDecoder.decode(e.arguments?.getString("ref").orEmpty(), "UTF-8")
                RunScreen(ref, onBack = { nav.popBackStack() }, onEdit = null)
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            if (showBars) NavigationRail {
                tabs.forEach { t ->
                    NavigationRailItem(
                        selected = current?.hierarchy?.any { it.route == t.route } == true,
                        onClick = { go(t.route) }, icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                    )
                }
                NavigationRailItem(selected = current?.route == Route.RUNS, onClick = { go(Route.RUNS) }, icon = { Icon(Icons.Outlined.PlayCircleOutline, null) }, label = { Text("Runs") })
                NavigationRailItem(selected = current?.route == Route.MORE, onClick = { go(Route.MORE) }, icon = { Icon(Icons.Outlined.MoreHoriz, null) }, label = { Text("More") })
            }
            content(Modifier.weight(1f).fillMaxSize())
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showBars) NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current?.hierarchy?.any { it.route == t.route } == true,
                            onClick = { go(t.route) }, icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding -> content(Modifier.padding(padding)) }
    }
}
