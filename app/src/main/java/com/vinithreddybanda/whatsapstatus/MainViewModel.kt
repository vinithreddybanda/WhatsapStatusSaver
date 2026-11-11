package com.vinithreddybanda.whatsapstatus

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinithreddybanda.whatsapstatus.data.StatusRepository
import com.vinithreddybanda.whatsapstatus.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StatusTab(val title: String) {
    object All : StatusTab("All")
    object Images : StatusTab("Images")
    object Videos : StatusTab("Videos")
    object Saved : StatusTab("Saved")
}

class MainViewModel : ViewModel() {
    private val repository = StatusRepository()

    var statusList by mutableStateOf<List<Status>>(emptyList())
        private set
    var savedStatusList by mutableStateOf<List<Status>>(emptyList())
        private set

    var isFetching by mutableStateOf(true)
        private set
    var loadError by mutableStateOf(false)
        private set
    var selectedTab by mutableStateOf<StatusTab>(StatusTab.All)
        private set

    private var statusRefreshJob: Job? = null
    private var savedRefreshJob: Job? = null
    private var lastStatusRefreshAt = 0L
    private var lastSavedRefreshAt = 0L

    fun getStatusesForTab(tab: StatusTab): List<Status> = when (tab) {
        StatusTab.All -> statusList
        StatusTab.Images -> statusList.filterNot { it.isVideo }
        StatusTab.Videos -> statusList.filter { it.isVideo }
        StatusTab.Saved -> savedStatusList
    }

    fun onTabSelected(tab: StatusTab) {
        selectedTab = tab
        if (tab == StatusTab.Saved) refreshSavedStatuses()
    }

    /**
     * Refreshes once per request. A refresh already in progress is shared rather
     * than starting another directory walk from a second lifecycle callback.
     */
    fun refreshStatuses(force: Boolean = false) {
        if (statusRefreshJob?.isActive == true) return

        val now = SystemClock.elapsedRealtime()
        if (!force && statusList.isNotEmpty() && now - lastStatusRefreshAt < REFRESH_COOLDOWN_MS) {
            return
        }

        statusRefreshJob = viewModelScope.launch {
            if (statusList.isEmpty()) isFetching = true
            loadError = false
            try {
                val latest = withContext(Dispatchers.IO) { repository.getStatuses() }
                statusList = latest
                lastStatusRefreshAt = SystemClock.elapsedRealtime()
                refreshSavedStatuses(force = true)
            } catch (_: Exception) {
                loadError = true
            } finally {
                isFetching = false
            }
        }
    }

    fun refreshSavedStatuses(force: Boolean = false) {
        if (savedRefreshJob?.isActive == true) return

        val now = SystemClock.elapsedRealtime()
        if (!force && savedStatusList.isNotEmpty() && now - lastSavedRefreshAt < REFRESH_COOLDOWN_MS) {
            return
        }

        savedRefreshJob = viewModelScope.launch {
            try {
                val latest = withContext(Dispatchers.IO) { repository.getSavedStatuses() }
                savedStatusList = latest
                lastSavedRefreshAt = SystemClock.elapsedRealtime()
            } catch (_: Exception) {
                // Keep the last known saved list visible if storage is temporarily unavailable.
            }
        }
    }

    fun saveStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) { repository.saveStatus(status) }
            if (success) refreshSavedStatuses(force = true)
            onResult(success)
        }
    }

    fun deleteStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) { repository.deleteStatus(status) }
            if (success) {
                savedStatusList = savedStatusList.filterNot { it.path == status.path }
                refreshSavedStatuses(force = true)
            }
            onResult(success)
        }
    }

    private companion object {
        const val REFRESH_COOLDOWN_MS = 15_000L
    }
}
