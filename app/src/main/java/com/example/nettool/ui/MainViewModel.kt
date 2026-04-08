package com.example.nettool.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nettool.data.Address
import com.example.nettool.data.AppDatabase
import com.example.nettool.net.Pinger
import com.example.nettool.net.PingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val ipInput: String = "",
    val noteInput: String = "",
    val pingResult: PingResult? = null,
    val isPinging: Boolean = false,
    val addresses: List<Address> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val addressDao = database.addressDao()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // 观察数据库变化，自动更新列表
        viewModelScope.launch {
            addressDao.getAllAddresses().collect { addresses ->
                _uiState.update { currentState ->
                    currentState.copy(addresses = addresses)
                }
            }
        }
    }

    fun updateIpInput(ip: String) {