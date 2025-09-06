package com.example.taskcommadmin.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskcommadmin.data.repository.SearchRepository
import com.example.taskcommadmin.data.repository.SearchResults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    
    private val repository = SearchRepository()
    private var searchJob: Job? = null
    
    private val _searchResults = MutableStateFlow<SearchResults?>(null)
    val searchResults: StateFlow<SearchResults?> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun search(query: String, context: Context) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        
        // Cancel previous search
        searchJob?.cancel()
        
        // Debounce search to avoid too many API calls
        searchJob = viewModelScope.launch {
            delay(300) // Wait 300ms after user stops typing
            
            _isLoading.value = true
            _error.value = null
            
            try {
                val results = repository.searchAll(query, context)
                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchUsers(query: String, context: Context) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            
            _isLoading.value = true
            _error.value = null
            
            try {
                val users = repository.searchUsers(query, context)
                _searchResults.value = SearchResults(
                    users = users,
                    tasks = emptyList(),
                    instructions = emptyList(),
                    messages = emptyList()
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "User search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchTasks(query: String, context: Context) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            
            _isLoading.value = true
            _error.value = null
            
            try {
                val tasks = repository.searchTasks(query, context)
                _searchResults.value = SearchResults(
                    users = emptyList(),
                    tasks = tasks,
                    instructions = emptyList(),
                    messages = emptyList()
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Task search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchInstructions(query: String, context: Context) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            
            _isLoading.value = true
            _error.value = null
            
            try {
                val instructions = repository.searchInstructions(query, context)
                _searchResults.value = SearchResults(
                    users = emptyList(),
                    tasks = emptyList(),
                    instructions = instructions,
                    messages = emptyList()
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Instruction search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchMessages(query: String, context: Context) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            
            _isLoading.value = true
            _error.value = null
            
            try {
                val messages = repository.searchMessages(query, context)
                _searchResults.value = SearchResults(
                    users = emptyList(),
                    tasks = emptyList(),
                    instructions = emptyList(),
                    messages = messages
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Message search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearResults() {
        _searchResults.value = null
        _error.value = null
    }
}
