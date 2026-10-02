package com.kaggle.controller.domain.repository

import com.kaggle.controller.domain.model.Notebook
import com.kaggle.controller.domain.model.NotebookRun
import com.kaggle.controller.domain.model.Schedule
import com.kaggle.controller.domain.model.UserAccount

interface KaggleRepository {
    suspend fun getAccount(): UserAccount?
    suspend fun getNotebooks(): List<Notebook>
    suspend fun getNotebookRuns(): List<NotebookRun>
    suspend fun getSchedules(): List<Schedule>
    suspend fun syncData()
}
