package com.kagglecontroller.feature.runs

import androidx.lifecycle.ViewModel
import com.kagglecontroller.AppContainer
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.common.toApiState
import com.kagglecontroller.domain.model.OutputFile
import com.kagglecontroller.domain.model.RunStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RunUi(
    val status: RunStatus = RunStatus.UNKNOWN,
    val failureMessage: String? = null,
    val log: String = "",
    val files: List<OutputFile> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val lastChecked: Long? = null,
    val error: ApiState<Nothing>? = null,
)

class RunViewModel(private val c: AppContainer, val ref: String) : ViewModel() {
    private val _ui = MutableStateFlow(RunUi())
    val ui: StateFlow<RunUi> = _ui.asStateFlow()

    /** Pull one snapshot of status + log + outputs. */
    suspend fun refresh() {
        try {
            val st = c.notebooks.status(ref)
            val out = c.notebooks.output(ref)
            _ui.update {
                it.copy(
                    status = st.status, failureMessage = st.failureMessage,
                    log = out.log.takeLast(MAX_LOG_CHARS), files = out.files,
                    lastChecked = System.currentTimeMillis(), error = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(error = e.toApiState()) }
        }
    }

    /**
     * Intelligent polling (spec 12, 39): starts at 4s, backs off x1.6 up to 60s, doubles again after
     * a failure, and stops by itself as soon as the run reaches a terminal state. The screen runs this
     * only while STARTED, so it pauses in the background.
     */
    suspend fun pollUntilDone() {
        var wait = 4_000L
        while (true) {
            refresh()
            val s = _ui.value
            if (s.status.terminal) {
                finalizeRecord(s)
                return
            }
            val err = s.error
            wait = when {
                err is ApiState.Unauthorized -> return              // never loop on auth failure
                err is ApiState.RateLimited -> maxOf(wait * 2, ((err.retryAfterSeconds ?: 30) * 1000L)).coerceAtMost(120_000L)
                err != null -> (wait * 2).coerceAtMost(120_000L)
                else -> (wait * 16 / 10).coerceAtMost(60_000L)
            }
            delay(wait)
        }
    }

    private suspend fun finalizeRecord(s: RunUi) {
        c.runs.refresh()
        val rec = c.runs.runs.value.firstOrNull { it.ref == ref && !it.runStatus.terminal } ?: return
        c.runs.upsert(rec.copy(status = s.status.name, endedAt = System.currentTimeMillis(), failureMessage = s.failureMessage))
    }

    fun errorLineIndex(): Int {
        val lines = _ui.value.log.lines()
        return lines.indexOfLast { l -> ERROR_MARKERS.any { l.contains(it, ignoreCase = true) } }
    }

    companion object {
        const val MAX_LOG_CHARS = 200_000
        private val ERROR_MARKERS = listOf("Traceback", "Error:", "Exception", "CUDA out of memory", "ModuleNotFoundError", "Killed")
    }
}
