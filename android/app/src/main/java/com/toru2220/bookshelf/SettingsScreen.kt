package com.toru2220.bookshelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private const val EMULATOR_HINT = "http://10.0.2.2:50080"

@Composable
fun SettingsScreen(
    current: String,
    onSave: suspend (String) -> Unit,
) {
    var text by remember(current) { mutableStateOf(current.ifBlank { EMULATOR_HINT }) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("サーバー", style = MaterialTheme.typography.headlineSmall)
        Text(
            "エミュレータからこの Mac へ繋ぐときは $EMULATOR_HINT です。",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                error = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ベース URL") },
            singleLine = true,
        )
        if (error != null) {
            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                val url = text.trim().trimEnd('/')
                val parsed = url.toHttpUrlOrNull()
                if (parsed == null || (parsed.scheme != "http" && parsed.scheme != "https")) {
                    error = "http または https の URL を入力してください"
                    return@Button
                }
                scope.launch { onSave(url) }
            },
        ) {
            Text("保存")
        }
    }
}
