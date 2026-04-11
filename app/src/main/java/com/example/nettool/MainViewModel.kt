package com.example.nettool

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val allEntries = db.ipDao().getAllEntries()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries = combine(allEntries, _searchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.address.contains(query, ignoreCase = true)
        }
    }

    private val _pingResult = MutableStateFlow("")
    val pingResult: StateFlow<String> = _pingResult

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addEntry(name: String, address: String) {
        viewModelScope.launch {
            db.ipDao().insert(IpEntry(name = name, address = address))
        }
    }

    fun deleteEntry(entry: IpEntry) {
        viewModelScope.launch {
            db.ipDao().delete(entry)
        }
    }

    fun pingAddress(address: String) {
        viewModelScope.launch {
            _pingResult.value = "正在 Ping $address ...\n"
            try {
                IcmpPing.ping(address, count = 4).collect { line ->
                    _pingResult.value = _pingResult.value + line + "\n"
                }
            } catch (e: Exception) {
                _pingResult.value = _pingResult.value + "Ping 失败: ${e.message}\n"
            }
        }
    }
}
