package com.vinithreddybanda.whatsapstatus

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinithreddybanda.whatsapstatus.data.StatusRepository
import com.vinithreddybanda.whatsapstatus.model.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class StatusTab(val title: String) {
    object All : StatusTab("All")
    object Images : StatusTab("Images")
    object Videos : StatusTab("Videos")
    object Saved : StatusTab("Saved")
}

class MainViewModel : ViewModel() {
    private val repository = StatusRepository()
    private val pageSize = 24

    var statusList by mutableStateOf<List<Status>>(emptyList())
        private set
    var savedStatusList by mutableStateOf<List<Status>>(emptyList())
        private set

    val imagesList by derivedStateOf { statusList.filterNot { it.isVideo } }
    val videosList by derivedStateOf { statusList.filter { it.isVideo } }

    var isFetching by mutableStateOf(true)
        private set
    var isSavedFetching by mutableStateOf(false)
        private set
    var selectedTab by mutableStateOf<StatusTab>(StatusTab.All)
        private set

    private var visibleItemLimit by mutableStateOf(pageSize)
    private var savedVisibleItemLimit by mutableStateOf(pageSize)
    private var hasLoadedStatuses = false
    private var hasLoadedSavedStatuses = false
    private var refreshJob: Job? = null
    private var savedLoadJob: Job? = null

    fun getStatusesForTab(tab: StatusTab): List<Status> = when (tab) {
        StatusTab.All -> statusList
        StatusTab.Images -> imagesList
        StatusTab.Videos -> videosList
        StatusTab.Saved -> savedStatusList
    }

    fun getVisibleStatusesForTab(tab: StatusTab): List<Status> {
        val source = getStatusesForTab(tab)
        val limit = if (tab == StatusTab.Saved) savedVisibleItemLimit else visibleItemLimit
        return source.take(limit)
    }

    fun onTabSelected(tab: StatusTab) {
        selectedTab = tab
        if (tab == StatusTab.Saved && !hasLoadedSavedStatuses) {
            loadSavedStatuses()
        }
    }

    fun getStatuses(forceRefresh: Boolean = false) {
        if (refreshJob?.isActive == true) return
        if (hasLoadedStatuses && !forceRefresh) return

        refreshJob = viewModelScope.launch {
            isFetching = true
            if (forceRefresh) {
                statusList = emptyList()
                visibleItemLimit = pageSize
                hasLoadedStatuses = false
            }

            try {
                repository.streamStatuses().collect { batch ->
                    statusList = statusList + batch
                }
                hasLoadedStatuses = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                isFetching = false
            }
        }
    }

    fun loadMore(tab: StatusTab) {
        val sourceSize = getStatusesForTab(tab).size
        if (tab == StatusTab.Saved) {
            if (savedVisibleItemLimit < sourceSize) {
                savedVisibleItemLimit = (savedVisibleItemLimit + pageSize).coerceAtMost(sourceSize)
            }
        } else if (visibleItemLimit < sourceSize) {
            visibleItemLimit = (visibleItemLimit + pageSize).coerceAtMost(sourceSize)
        }
    }

    fun saveStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.saveStatus(status)
            if (success) {
                savedStatusList = repository.getSavedStatuses()
                hasLoadedSavedStatuses = true
                savedVisibleItemLimit = maxOf(savedVisibleItemLimit, pageSize)
            }
            onResult(success)
        }
    }

    fun deleteStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteStatus(status)
            if (success) {
                savedStatusList = savedStatusList.filterNot { it.path == status.path }
                savedVisibleItemLimit = savedVisibleItemLimit.coerceAtMost(savedStatusList.size)
            }
            onResult(success)
        }
    }

    private fun loadSavedStatuses(forceRefresh: Boolean = false) {
        if (savedLoadJob?.isActive == true) {
            if (!forceRefresh) return
            savedLoadJob?.cancel()
        }

        savedLoadJob = viewModelScope.launch {
            isSavedFetching = true
            try {
                savedStatusList = repository.getSavedStatuses()
                hasLoadedSavedStatuses = true
                savedVisibleItemLimit = pageSize
            } finally {
                isSavedFetching = false
            }
        }
    }
}
