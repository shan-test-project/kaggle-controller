package com.kaggle.controller.data.repository

import com.kaggle.controller.data.local.AppPreferences
import com.kaggle.controller.data.remote.api.KaggleApiService
import com.kaggle.controller.domain.model.Notebook
import com.kaggle.controller.domain.model.NotebookRun
import com.kaggle.controller.domain.model.Schedule
import com.kaggle.controller.domain.model.UserAccount
import com.kaggle.controller.domain.repository.KaggleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KaggleRepositoryImpl @Inject constructor(
    private val api: KaggleApiService,
    private val prefs: AppPreferences,
) : KaggleRepository {

    override suspend fun getAccount(): UserAccount? {
        return try {
            val user = api.getCurrentUser("Bearer ${prefs.getToken()}")
            UserAccount(
                username = user.userName,
                displayName = user.displayName,
                tier = "Standard"
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getNotebooks(): List<Notebook> {
        return try {
            val res = api.listNotebooks("Bearer ${prefs.getToken()}")
            res.items.map {
                Notebook(
                    id = it.id,
                    title = it.title,
                    owner = "You",
                    language = it.kernelType,
                    lastUpdated = "Recently updated",
                    lastRun = null,
                    runStatus = "None",
                    computeType = "None",
                    isFavorite = false,
                    hasLocalChanges = false,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getNotebookRuns(): List<NotebookRun> {
        return emptyList()
    }

    override suspend fun getSchedules(): List<Schedule> {
        return emptyList()
    }

    override suspend fun syncData() {
        // Background sync logic
    }
}
