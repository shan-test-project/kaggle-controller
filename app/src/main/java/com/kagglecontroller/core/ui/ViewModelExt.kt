package com.kagglecontroller.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kagglecontroller.AppContainer
import com.kagglecontroller.KaggleControllerApp

@Composable
fun rememberContainer(): AppContainer =
    (LocalContext.current.applicationContext as KaggleControllerApp).container

@Composable
inline fun <reified VM : ViewModel> containerViewModel(key: String? = null, crossinline build: (AppContainer) -> VM): VM {
    val container = rememberContainer()
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = build(container) as T
        },
    )
}
