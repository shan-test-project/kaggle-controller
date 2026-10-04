package com.kagglecontroller

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.kagglecontroller.core.network.KaggleRpcClient
import com.kagglecontroller.core.security.SecureTokenStore
import com.kagglecontroller.data.kaggle.AuthRepositoryImpl
import com.kagglecontroller.data.kaggle.NotebookRepositoryImpl
import com.kagglecontroller.data.kaggle.ResourceRepositoryImpl
import com.kagglecontroller.data.local.DraftStore
import com.kagglecontroller.data.local.RunStore
import com.kagglecontroller.data.local.SettingsStore
import com.kagglecontroller.domain.repository.KaggleAuthRepository
import com.kagglecontroller.domain.repository.KaggleNotebookRepository
import com.kagglecontroller.domain.repository.KaggleResourceRepository

/** Hand-rolled DI: fewer moving parts, faster cold start on low-end phones, no kapt/ksp. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    /** Outlives ViewModels: used to flush drafts to disk when a screen is closed. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val tokenStore = SecureTokenStore(context)
    val rpc = KaggleRpcClient(tokenProvider = { tokenStore.load() })
    val auth: KaggleAuthRepository = AuthRepositoryImpl(rpc, tokenStore)
    val notebooks: KaggleNotebookRepository = NotebookRepositoryImpl(rpc)
    val resources: KaggleResourceRepository = ResourceRepositoryImpl(rpc)
    val drafts = DraftStore(context)
    val runs = RunStore(context)
    val settings = SettingsStore(context)
}
