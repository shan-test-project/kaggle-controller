package com.kaggle.controller.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface KaggleApiService {
    @GET("me")
    suspend fun getCurrentUser(
        @Header("Authorization") auth: String,
    ): UserResponse

    @GET("kernels/list")
    suspend fun listNotebooks(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("sortBy") sort: String = "updated",
    ): NotebooksResponse

    @GET("kernels/status/")
    suspend fun listRuns(
        @Header("Authorization") auth: String,
        @Query("kernelId") kernelId: String? = null,
    ): RunsResponse
}

data class UserResponse(val userName: String, val displayName: String)
data class NotebookItem(val id: String, val title: String, val kernelType: String)
data class NotebooksResponse(val items: List<NotebookItem> = emptyList(), val count: Int = 0)
data class RunsResponse(val runs: List<RunItem> = emptyList())
data class RunItem(val id: String, val status: String, val startTime: String)
