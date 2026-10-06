package com.toru2220.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    api: ShelfApi,
    baseUrl: String,
    bookPath: String,
    title: String,
    initialPosition: String,
    onBack: () -> Unit,
) {
    var total by remember(bookPath) { mutableIntStateOf(0) }
    var page by remember(bookPath) {
        mutableIntStateOf(initialPosition.toIntOrNull()?.takeIf { it >= 1 } ?: 1)
    }
    var rtl by remember(bookPath) { mutableStateOf(false) }
    var loading by remember(bookPath) { mutableStateOf(true) }
    var error by remember(bookPath) { mutableStateOf<String?>(null) }

    LaunchedEffect(bookPath) {
        withContext(Dispatchers.IO) { api.reportAccess(baseUrl, bookPath) }
        val result = withContext(Dispatchers.IO) {
            runCatching { api.pageCount(baseUrl, bookPath) }
        }
        loading = false
        result.onSuccess { count ->
            if (count < 1) {
                error = "画像が見つかりません"
            } else {
                total = count
                page = page.coerceIn(1, count)
            }
        }.onFailure { cause ->
            error = if (cause is ShelfException && cause.code >= 500) {
                "このアーカイブは開けません。パスワード付きの ZIP は対象外です。"
            } else {
                cause.message ?: "開けません"
            }
        }
    }

    LaunchedEffect(page, total) {
        if (total > 0) {
            val progress = page.toDouble() / total.toDouble()
            withContext(Dispatchers.IO) {
                api.reportProgress(baseUrl, bookPath, page.toString(), progress)
            }
        }
    }

    fun step(forward: Boolean) {
        if (total < 1) return
        val delta = if (forward) 1 else -1
        page = (page + delta).coerceIn(1, total)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (total > 0) "$title  $page / $total" else title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    TextButton(onClick = { rtl = !rtl }) {
                        Text(if (rtl) "右から" else "左から")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null -> Text(
                    error.orEmpty(),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                else -> {
                    SubcomposeAsyncImage(
                        model = api.pageUrl(baseUrl, bookPath, page),
                        contentDescription = title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("ページを読み込めません")
                            }
                        },
                    )
                    Row(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { step(forward = rtl) },
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { step(forward = !rtl) },
                        )
                    }
                }
            }
        }
    }
}
