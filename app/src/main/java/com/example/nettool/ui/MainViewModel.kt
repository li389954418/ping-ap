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
        _uiState.update { it.copy(ipInput = ip) }
    }

    fun updateNoteInput(note: String) {
        _uiState.update { it.copy(noteInput = note) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        // 实时搜索
        if (query.isBlank()) {
            viewModelScope.launch {
                addressDao.getAllAddresses().collect { addresses ->
                    _uiState.update { currentState ->
                        currentState.copy(addresses = addresses)
                    }
                }
            }
        } else {
            viewModelScope.launch {
                addressDao.searchAddresses("%$query%").collect { addresses ->
                    _uiState.update { currentState ->
                        currentState.copy(addresses = addresses)
                    }
                }
            }
        }
    }

    fun ping(host: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true, errorMessage = null) }
            
            try {
                val result = Pinger.ping(host)
                _uiState.update { 
                    it.copy(
                        pingResult = result,
                        isPinging = false,
                        errorMessage = if (result.success) null else result.errorMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isPinging = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun saveAddress(ip: String, note: String) {
        viewModelScope.launch {
            val address = Address(ip = ip, note = note)
            addressDao.insert(address)
            // 清空输入
            _uiState.update { it.copy(noteInput = "") }
        }
    }

    fun deleteAddress(address: Address) {
        viewModelScope.launch {
            addressDao.delete(address)
        }
    }

    fun clearPingResult() {
        _uiState.update { it.copy(pingResult = null) }
    }
}