package com.embedsuite.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.embedsuite.app.connection.DeviceConnectionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RfHubViewModel(
    connectionManager: DeviceConnectionManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val lastDecoded = connectionManager.lastDecoded

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
}
