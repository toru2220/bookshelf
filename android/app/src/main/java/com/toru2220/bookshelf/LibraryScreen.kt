package com.toru2220.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    api: ShelfApi,
    baseUrl: String,
    folder: String,
    onOpenFolder: (String) -> Unit,
    onOpenBook: (BookEntry) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var books by remember(folder, baseUrl) { mutableStateOf<List<BookEntry>>(emptyList()) }
    var nextPage by remember(folder, baseUrl) { mutableIntStateOf(1) }
    var hasMore by remember(folder, baseUrl) { mutableStateOf(true) }
    var loading by remember(folder, baseUrl) { mutableStateOf(false) }
    var error by remember(folder, baseUrl) { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    fun loadNext() {
        if (loading || !hasMore) return
        loading = true
        scope.launch {
            var requested = nextPage
            var more = hasMore
            while (more) {
                val result = withContext(Dispatchers.IO) {
                    runCatching { api.list(baseUrl, folder, requested) }
                }
                val loaded = result.getOrElse { cause ->
                    loading = false
                    error = cause.message ?: "一覧を取得できません"
                    hasMore = false
                    return@launch
                }
                val visible = loaded.books.filter { it.isFolder || it.openable }
                books = books + visible
                more = loaded.hasMore
                requested += 1
                nextPage = requested
                hasMore = more
                error = null
                if (visible.isNotEmpty() || !more) break
            }
            loading = false
        }
    }

    LaunchedEffect(folder, baseUrl) {
        loadNext()
    }

    LaunchedEffect(gridState, books.size, hasMore, loading) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { last ->
                if (!loading && last != null && last >= books.size - 4) {
                    loadNext()
                }
            }
    }

    val title = folder.trim('/').substringAfterLast('/').ifBlank { "本棚" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "サーバー設定")
                    }
                },
            )
        },
    ) { padding ->
        when {
            books.isEmpty() && loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            books.isEmpty() && error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(error.orEmpty(), modifier = Modifier.padding(24.dp))
                }
            }
            books.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("ZIP / CBZ がありません", modifier = Modifier.padding(24.dp))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(140.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(books, key = { it.path + it.type }) { book ->
                        BookCard(
                            book = book,
                            coverUrl = api.coverUrl(baseUrl, book.cover),
                            onClick = {
                                if (book.isFolder) onOpenFolder(book.path) else onOpenBook(book)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookEntry,
    coverUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box {
            if (coverUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (book.isFolder) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                    }
                }
            } else {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f),
                )
            }
            if (!book.isFolder && book.progress > 0.0) {
                LinearProgressIndicator(
                    progress = { book.progress.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
        Text(
            text = book.title.ifBlank { book.path.substringAfterLast('/') },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
