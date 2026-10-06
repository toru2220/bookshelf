package com.toru2220.bookshelf

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.serverDataStore by preferencesDataStore(name = "bookshelf")

class ServerStore(context: Context) {
    private val dataStore = context.applicationContext.serverDataStore

    val baseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: ""
    }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = url.trim().trimEnd('/')
        }
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
    }
}
