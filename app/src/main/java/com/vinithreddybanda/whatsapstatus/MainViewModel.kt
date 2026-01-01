package com.vinithreddybanda.whatsapstatus

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinithreddybanda.whatsapstatus.data.StatusRepository
import com.vinithreddybanda.whatsapstatus.model.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.os.Environment

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

    // Cache filtered lists to avoid recalculating on every frame
    val imagesList by derivedStateOf { statusList.filter { !it.isVideo } }
    val videosList by derivedStateOf { statusList.filter { it.isVideo } }

    var isFetching by mutableStateOf(true)
        private set

    var selectedTab by mutableStateOf<StatusTab>(StatusTab.All)
        private set

    val filteredStatuses by derivedStateOf {
        getStatusesForTab(selectedTab)
    }

    fun getStatusesForTab(tab: StatusTab): List<Status> {
        return when (tab) {
            StatusTab.All -> statusList
            StatusTab.Images -> imagesList
            StatusTab.Videos -> videosList
            StatusTab.Saved -> savedStatusList
        }
    }

    fun onTabSelected(tab: StatusTab) {
        selectedTab = tab
        if (tab == StatusTab.Saved) {
            getSavedStatuses()
        }
    }

    fun getStatuses() {
        viewModelScope.launch {
            isFetching = true
            val statuses = withContext(Dispatchers.IO) {
                repository.getStatuses()
            }
            statusList = statuses

            // Refresh saved statuses in case new ones were added
            getSavedStatuses()

            isFetching = false
        }
    }

    fun saveStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveStatus(status)
            onResult(result)
            // Refresh saved list if save was successful
            if (result) {
                getSavedStatuses()
            }
        }
    }

    fun deleteStatus(status: Status, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteStatus(status)
            onResult(result)
            if (result) {
                 // Update the local list immediately to reflect UI change
                 getSavedStatuses()
            }
        }
    }

    private fun getSavedStatuses() {
        viewModelScope.launch {
             val savedStatuses = withContext(Dispatchers.IO) {
                repository.getSavedStatuses()
            }
            savedStatusList = savedStatuses
        }
    }
}