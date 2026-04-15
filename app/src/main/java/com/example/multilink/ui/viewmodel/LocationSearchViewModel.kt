package com.example.multilink.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.multilink.model.SearchResult
import com.example.multilink.repo.LocationSearchRepository
import com.mapbox.geojson.Point
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LocationSearchViewModel : ViewModel() {
    private val repository = LocationSearchRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _showSearchResults = MutableStateFlow(false)
    val showSearchResults = _showSearchResults.asStateFlow()

    private val _moveToPlaceTrigger = MutableStateFlow<Point?>(null)
    val moveToPlaceTrigger = _moveToPlaceTrigger.asStateFlow()

    private val _selectedPlace = MutableStateFlow<SearchResult?>(null)
    val selectedPlace = _selectedPlace.asStateFlow()

    // ⭐ ADDED: A SharedFlow to act as a trigger for Debouncing the search queries
    private val searchTrigger = MutableSharedFlow<Pair<String, String>>()

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            searchTrigger
                .debounce(500) // ⭐ ADDED: Wait 500ms after user stops typing before calling API
                .collectLatest { (query, proximity) ->
                    if (query.isBlank()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                        return@collectLatest
                    }
                    _isSearching.value = true
                    _searchResults.value = repository.getSuggestions(query, proximity)
                    _isSearching.value = false
                }
        }
    }

    fun onSearchQueryChanged(query: String, proximity: String) {
        _searchQuery.value = query
        _showSearchResults.value = true
        viewModelScope.launch { searchTrigger.emit(Pair(query, proximity)) }
    }

    fun forceSearch(query: String, proximity: String) = viewModelScope.launch {
        if (query.isNotBlank()) {
            _isSearching.value = true
            _searchResults.value = repository.getSuggestions(query, proximity)
            _isSearching.value = false
            _showSearchResults.value = true
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _showSearchResults.value = false
    }

    fun hideResults() {
        _showSearchResults.value = false
    }

    fun selectResult(result: SearchResult) = viewModelScope.launch {
        result.mapboxId?.let { id ->
            val point = repository.getPlaceDetails(id)
            if (point != null) {
                _searchQuery.value = result.name
                _selectedPlace.value = result.copy(point = point)
                _moveToPlaceTrigger.value = point
                _showSearchResults.value = false
            }
        }
    }

    fun clearMoveToTrigger() {
        _moveToPlaceTrigger.value = null
    }
}