package com.kaggle.controller.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "kaggle_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("kaggle_token")
    }

    private val dataStore = context.dataStore

    fun getToken(): String = "test_token_placeholder"

    suspend fun saveToken(token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
        }
    }
}
