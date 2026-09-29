package com.sdkm.manager.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkm.manager.utils.DisplayState
import com.sdkm.manager.utils.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DisplayViewModel : ViewModel() {
    private val _state = MutableStateFlow(DisplayState())
    val state: StateFlow<DisplayState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { _state.value = DisplayUtils.readState() }
    }
}
