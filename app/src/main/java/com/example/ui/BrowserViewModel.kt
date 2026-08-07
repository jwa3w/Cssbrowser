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

sealed interface BrowserCommand {
    object GoBack : BrowserCommand
    object GoForward : BrowserCommand
    object Reload : BrowserCommand
    object StopLoading : BrowserCommand
    data class LoadUrl(val url: String) : BrowserCommand
}

class BrowserViewModel(
    private val repository: BrowserRepository,
    val isIncognito: Boolean = false,
    val incognitoProfileName: String? = null
) : ViewModel() {

    // Address and page state
    private val _currentUrl = MutableStateFlow("about:blank")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _typedUrl = MutableStateFlow("")
    val typedUrl: StateFlow<String> = _typedUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Blank Page")
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

    // Dialogs / Overlays
    private val _showBookmarks = MutableStateFlow(false)
    val showBookmarks: StateFlow<Boolean> = _showBookmarks.asStateFlow()

    private val _showHistory = MutableStateFlow(false)
    val showHistory: StateFlow<Boolean> = _showHistory.asStateFlow()

    // Reactive streams from Repository
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarked status of the current page
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _currentUrl
        .flatMapLatest { url ->
            if (url.isEmpty() || url == "about:blank") {
                flowOf(false)
            } else {
                repository.isUrlBookmarked(url)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Unidirectional action flow to WebView
    private val _commands = MutableSharedFlow<BrowserCommand>(extraBufferCapacity = 16)
    val commands: SharedFlow<BrowserCommand> = _commands.asSharedFlow()

    // Input handlers
    fun onUrlTyped(url: String) {
        _typedUrl.value = url
    }

    // Overlays toggles
    fun setShowBookmarks(show: Boolean) {
        _showBookmarks.value = show
        if (show) {
            _showHistory.value = false
        }
    }

    fun setShowHistory(show: Boolean) {
        _showHistory.value = show
        if (show) {
            _showBookmarks.value = false
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
        _typedUrl.value = if (url == "about:blank") "" else url
        if (url == "about:blank") {
            _pageTitle.value = "Blank Page"
            _isLoading.value = false
            _progress.value = 0
            viewModelScope.launch {
                _commands.emit(BrowserCommand.LoadUrl("about:blank"))
            }
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
        viewModelScope.launch {
            _commands.emit(BrowserCommand.GoBack)
        }
    }

    fun navigateForward() {
        viewModelScope.launch {
            _commands.emit(BrowserCommand.GoForward)
        }
    }

    fun triggerReload() {
        viewModelScope.launch {
            _commands.emit(BrowserCommand.Reload)
        }
    }

    fun triggerStop() {
        viewModelScope.launch {
            _commands.emit(BrowserCommand.StopLoading)
        }
    }

    fun navigateHome() {
        loadUrl("about:blank")
    }

    // Bookmarking current page
    fun toggleCurrentPageBookmark() {
        val url = _currentUrl.value
        val title = _pageTitle.value.ifEmpty { url }
        if (url.isEmpty() || url == "about:blank") return

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
        _typedUrl.value = if (url == "about:blank") "" else url
        _isLoading.value = true
        _progress.value = 0
    }

    fun onPageFinished(webView: android.webkit.WebView, url: String) {
        _currentUrl.value = url
        _typedUrl.value = if (url == "about:blank") "" else url
        _isLoading.value = false
        _progress.value = 100
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
        
        val title = webView.title ?: ""
        _pageTitle.value = title.ifEmpty { url }

        // Add history entry
        if (!isIncognito && url.isNotEmpty() && url != "about:blank") {
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

    // URL Normalization Logic (pure website address parsing, no search engine)
    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        
        // 1. Check if it's already a full URL
        if (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("about:", ignoreCase = true)) {
            return trimmed
        }

        // 2. Otherwise treat directly as a website by prepending https://
        return "https://$trimmed"
    }

    // Factory
    class Factory(
        private val repository: BrowserRepository,
        private val isIncognito: Boolean = false,
        private val incognitoProfileName: String? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BrowserViewModel(repository, isIncognito, incognitoProfileName) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
