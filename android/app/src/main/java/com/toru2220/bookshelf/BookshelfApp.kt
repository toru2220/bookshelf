package com.toru2220.bookshelf

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

private sealed interface Route {
    data object Settings : Route
    data class Library(val folder: String) : Route
    data class Reader(val path: String, val title: String, val position: String) : Route
}

@Composable
fun BookshelfApp(store: ServerStore, api: ShelfApi) {
    val baseUrl by store.baseUrl.collectAsState(initial = null)
    val stack = remember { mutableStateListOf<Route>() }

    LaunchedEffect(baseUrl) {
        if (baseUrl != null && stack.isEmpty()) {
            stack.add(if (baseUrl.isNullOrBlank()) Route.Settings else Route.Library(""))
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val route = stack.lastOrNull()
            if (baseUrl == null || route == null) {
                Box(Modifier.fillMaxSize())
                return@Surface
            }
            BackHandler(enabled = stack.size > 1) {
                stack.removeAt(stack.lastIndex)
            }
            when (route) {
                Route.Settings -> SettingsScreen(
                    current = baseUrl.orEmpty(),
                    onSave = { url ->
                        store.setBaseUrl(url)
                        if (stack.size == 1) {
                            stack[0] = Route.Library("")
                        } else {
                            stack.removeAt(stack.lastIndex)
                        }
                    },
                )
                is Route.Library -> LibraryScreen(
                    api = api,
                    baseUrl = baseUrl.orEmpty(),
                    folder = route.folder,
                    onOpenFolder = { stack.add(Route.Library(it)) },
                    onOpenBook = { book ->
                        stack.add(Route.Reader(book.path, book.title, book.currentPosition))
                    },
                    onOpenSettings = { stack.add(Route.Settings) },
                )
                is Route.Reader -> ReaderScreen(
                    api = api,
                    baseUrl = baseUrl.orEmpty(),
                    bookPath = route.path,
                    title = route.title,
                    initialPosition = route.position,
                    onBack = { stack.removeAt(stack.lastIndex) },
                )
            }
        }
    }
}
