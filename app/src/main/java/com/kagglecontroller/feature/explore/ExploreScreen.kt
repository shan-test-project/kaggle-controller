package com.kagglecontroller.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kagglecontroller.AppContainer
import com.kagglecontroller.core.common.ApiState
import com.kagglecontroller.core.common.toApiState
import com.kagglecontroller.core.ui.components.EmptyState
import com.kagglecontroller.core.ui.components.StateHost
import com.kagglecontroller.core.ui.components.openInKaggle
import com.kagglecontroller.core.ui.containerViewModel
import com.kagglecontroller.domain.model.ResourceItem
import com.kagglecontroller.domain.repository.ResourceKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

data class ResourcePage(val items: List<ResourceItem>, val next: String?)

class ExploreViewModel(private val c: AppContainer) : ViewModel() {
    private val _kind = MutableStateFlow(ResourceKind.DATASETS)
    val kind: StateFlow<ResourceKind> = _kind.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _state = MutableStateFlow<ApiState<ResourcePage>>(ApiState.Idle)
    val state: StateFlow<ApiState<ResourcePage>> = _state.asStateFlow()
    private var job: Job? = null

    init { load(true, false) }

    fun setKind(k: ResourceKind) { _kind.value = k; load(true, false) }
    fun setQuery(q: String) { _query.value = q; job?.cancel(); job = viewModelScope.launch { delay(400); fetch(true, false) } }
    fun reload() = load(true, true)
    fun loadMore() { if ((_state.value as? ApiState.Success)?.data?.next != null) load(false, false) }

    private fun load(reset: Boolean, force: Boolean) { job?.cancel(); job = viewModelScope.launch { fetch(reset, force) } }

    private suspend fun fetch(reset: Boolean, force: Boolean) {
        val prev = (_state.value as? ApiState.Success)?.data
        if (reset) _state.value = ApiState.Loading
        try {
            val p = c.resources.list(_kind.value, _query.value.ifBlank { null }, if (reset) null else prev?.next, force = force)
            _state.value = ApiState.Success(ResourcePage(if (reset || prev == null) p.items else prev.items + p.items, p.nextPageToken))
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { _state.value = if (!reset && prev != null) ApiState.Success(prev) else e.toApiState() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onReauth: () -> Unit) {
    val vm = containerViewModel { ExploreViewModel(it) }
    val kind by vm.kind.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Text("Explore", Modifier.padding(16.dp), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(query, vm::setQuery, singleLine = true, label = { Text("Search ${kind.title.lowercase()}") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourceKind.entries.forEach { k -> FilterChip(selected = k == kind, onClick = { vm.setKind(k) }, label = { Text(k.title) }) }
        }
        PullToRefreshBox(isRefreshing = state is ApiState.Loading, onRefresh = vm::reload, modifier = Modifier.fillMaxSize()) {
            StateHost(state, onRetry = vm::reload, onReauth = onReauth) { page ->
                if (page.items.isEmpty()) EmptyState("Nothing found", "Try a different search.")
                else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(page.items, key = { it.ref }) { item ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                                .clickable { openInKaggle(ctx, item.webUrl) }.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                            item.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                    item { LaunchedEffect(page.next) { vm.loadMore() } }
                }
            }
        }
    }
}
