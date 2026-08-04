package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Bookmark
import com.example.data.BrowserRepository
import com.example.data.HistoryEntry
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q=")
}

sealed interface BrowserCommand {
    object GoBack : BrowserCommand
    object GoForward : BrowserCommand
    object Reload : BrowserCommand
    object StopLoading : BrowserCommand
    data class LoadUrl(val url: String) : BrowserCommand
}

class BrowserViewModel(private val repository: BrowserRepository) : ViewModel() {

    // Address and page state
    private val _currentUrl = MutableStateFlow("local:home")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _typedUrl = MutableStateFlow("")
    val typedUrl: StateFlow<String> = _typedUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Home")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    // Loading indicator
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    // Navigation back/forward availability
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    // Search Engine
    private val _searchEngine = MutableStateFlow(SearchEngine.GOOGLE)
    val searchEngine: StateFlow<SearchEngine> = _searchEngine.asStateFlow()

    // Dialogs / Overlays
    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    // Reactive streams from Repository
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarked status of the current page
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _currentUrl
        .flatMapLatest { url ->
            if (url.isEmpty() || url == "local:home") {
                flowOf(false)
            } else {
                repository.isUrlBookmarked(url)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Unidirectional action flow to WebView
    private val _commands = MutableSharedFlow<BrowserCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<BrowserCommand> = _commands.asSharedFlow()

    init {
        // Load settings
        viewModelScope.launch {
            repository.getSettingFlow("pref_search_engine").collect { engineName ->
                if (engineName != null) {
                    try {
                        _searchEngine.value = SearchEngine.valueOf(engineName)
                    } catch (e: Exception) {
                        _searchEngine.value = SearchEngine.GOOGLE
                    }
                }
            }
        }
    }

    // Input handlers
    fun onUrlTyped(url: String) {
        _typedUrl.value = url
    }

    fun selectSearchEngine(engine: SearchEngine) {
        _searchEngine.value = engine
        viewModelScope.launch {
            repository.saveSetting("pref_search_engine", engine.name)
        }
    }

    // Overlays toggles
    fun setShowBookmarks(show: Boolean) {
        _showBookmarks.value = show
        if (show) {
            _showHistory.value = false
            _showSettings.value = false
        }
    }

    fun setShowHistory(show: Boolean) {
        _showHistory.value = show
        if (show) {
            _showBookmarks.value = false
            _showSettings.value = false
        }
    }

    fun setShowSettings(show: Boolean) {
        _showSettings.value = show
        if (show) {
            _showBookmarks.value = false
            _showHistory.value = false
        }
    }

    // Action execution
    fun navigateToTyped() {
        val rawInput = _typedUrl.value.trim()
        if (rawInput.isEmpty()) return

        val finalUrl = normalizeUrl(rawInput)
        loadUrl(finalUrl)
    }

    fun loadUrl(url: String) {
        _currentUrl.value = url
        _typedUrl.value = url
        if (url == "local:home") {
            _pageTitle.value = "Home"
            _isLoading.value = false
            _progress.value = 0
        } else {
            viewModelScope.launch {
                _commands.emit(BrowserCommand.LoadUrl(url))
            }
        }
        // Dismiss overlays when navigating
        _showBookmarks.value = false
        _showHistory.value = false
    }

    fun navigateBack() {
        if (_currentUrl.value != "local:home") {
            viewModelScope.launch {
                _commands.emit(BrowserCommand.GoBack)
            }
        }
    }

    fun navigateForward() {
        if (_currentUrl.value != "local:home") {
            viewModelScope.launch {
                _commands.emit(BrowserCommand.GoForward)
            }
        }
    }

    fun triggerReload() {
        if (_currentUrl.value != "local:home") {
            viewModelScope.launch {
                _commands.emit(BrowserCommand.Reload)
            }
        }
    }

    fun triggerStop() {
        viewModelScope.launch {
            _commands.emit(BrowserCommand.StopLoading)
        }
    }

    fun navigateHome() {
        loadUrl("local:home")
    }

    // Bookmarking current page
    fun toggleCurrentPageBookmark() {
        val url = _currentUrl.value
        val title = _pageTitle.value.ifEmpty { url }
        if (url.isEmpty() || url == "local:home") return

        viewModelScope.launch {
            if (isCurrentPageBookmarked.value) {
                repository.removeBookmarkByUrl(url)
            } else {
                repository.addBookmark(title, url)
            }
        }
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun addManualBookmark(title: String, url: String) {
        if (url.trim().isEmpty()) return
        val finalUrl = normalizeUrl(url)
        val finalTitle = title.trim().ifEmpty { finalUrl }
        viewModelScope.launch {
            repository.addBookmark(finalTitle, finalUrl)
        }
    }

    // History management
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryEntry(id: Int) {
        viewModelScope.launch {
            repository.removeHistoryEntry(id)
        }
    }

    // WebView event bridges
    fun onPageStarted(url: String) {
        _currentUrl.value = url
        _typedUrl.value = url
        _isLoading.value = true
        _progress.value = 0
    }

    fun onPageFinished(webView: android.webkit.WebView, url: String) {
        _currentUrl.value = url
        _typedUrl.value = url
        _isLoading.value = false
        _progress.value = 100
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
        
        val title = webView.title ?: ""
        _pageTitle.value = title.ifEmpty { url }

        // Add history entry
        if (url.isNotEmpty() && url != "local:home" && url != "about:blank") {
            viewModelScope.launch {
                repository.addHistoryEntry(_pageTitle.value, url)
            }
        }
    }

    fun onProgressChanged(progress: Int) {
        _progress.value = progress
    }

    fun onNavigationStateChanged(webView: android.webkit.WebView) {
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
    }

    // URL Normalization Logic
    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        
        // 1. Check if it's already a full URL
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)) {
            return trimmed
        }

        // 2. Check if it has a valid TLD or looks like an IP address
        val domainPattern = "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(/.*)?$".toRegex()
        val ipPattern = "^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(:\\d+)?(/.*)?$".toRegex()
        val localhostPattern = "^localhost(:\\d+)?(/.*)?$".toRegex()

        if (domainPattern.matches(trimmed) || ipPattern.matches(trimmed) || localhostPattern.matches(trimmed)) {
            return "https://$trimmed"
        }

        // 3. Otherwise treat as a search query
        return _searchEngine.value.searchUrl + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    // Factory
    class Factory(private val repository: BrowserRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BrowserViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
