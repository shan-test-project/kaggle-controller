package com.kaggle.controller.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "kaggle_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        const val KEY_TOKEN = "kaggle_token"
    }

    private val dataStore = context.dataStore

    fun getToken(): String = "test_token_placeholder"

    suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
        }
    }
}
