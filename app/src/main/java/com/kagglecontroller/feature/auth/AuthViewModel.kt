package com.kagglecontroller.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kagglecontroller.AppContainer
import com.kagglecontroller.core.network.KaggleException
import com.kagglecontroller.domain.model.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUi {
    data object Checking : AuthUi
    data object SignedOut : AuthUi
    /** verified = false means we started offline and have not confirmed the token with Kaggle yet. */
    data class SignedIn(val account: UserAccount, val verified: Boolean) : AuthUi
}

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow<AuthUi>(AuthUi.Checking)
    val ui: StateFlow<AuthUi> = _ui.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { restore() }

    fun restore() {
        viewModelScope.launch {
            if (!c.auth.hasToken()) { _ui.value = AuthUi.SignedOut; return@launch }
            try {
                val acc = c.auth.restore()
                _ui.value = if (acc == null) AuthUi.SignedOut else AuthUi.SignedIn(acc, true)
                acc?.let { c.settings.lastUsername = it.username }
            } catch (e: KaggleException.Offline) {
                // Offline start: let the user work with local drafts and cached data.
                _ui.value = AuthUi.SignedIn(UserAccount(c.settings.lastUsername ?: "offline", null, false), false)
            } catch (e: Exception) {
                _ui.value = AuthUi.SignedOut
                _error.value = e.message
            }
        }
    }

    fun signIn(token: String) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val acc = c.auth.signIn(token)
                c.settings.lastUsername = acc.username
                _ui.value = AuthUi.SignedIn(acc, true)
            } catch (e: IllegalArgumentException) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't verify the token."
            } finally {
                _busy.value = false
            }
        }
    }

    fun signOut() {
        c.auth.signOut()
        _ui.value = AuthUi.SignedOut
    }
}
