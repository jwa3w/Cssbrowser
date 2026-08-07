package com.example.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Bookmark
import com.example.data.HistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMainScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val typedUrl by viewModel.typedUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsStateWithLifecycle()

    // Overlay triggers
    val showBookmarks by viewModel.showBookmarks.collectAsStateWithLifecycle()
    val showHistory by viewModel.showHistory.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        topBar = {
            // Static Address / Search Bar (Strictly no auto-hide!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    .testTag("address_bar_container")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // URL/Search input
                    OutlinedTextField(
                        value = typedUrl,
                        onValueChange = { viewModel.onUrlTyped(it) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 52.dp)
                            .testTag("url_input_field"),
                        placeholder = { 
                            Text(
                                "Enter website URL", 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 14.sp
                            ) 
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                viewModel.navigateToTyped()
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Web",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            Row {
                                if (typedUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onUrlTyped("") },
                                        modifier = Modifier.size(36.dp).testTag("clear_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (currentUrl != "about:blank") {
                                    IconButton(
                                        onClick = { viewModel.toggleCurrentPageBookmark() },
                                        modifier = Modifier.size(36.dp).testTag("bookmark_star_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Bookmark this page",
                                            tint = if (isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Go / Navigation Button
                    Button(
                        onClick = {
                            viewModel.navigateToTyped()
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("go_button"),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Go",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            val uniqueProfileName = "incognito_" + java.util.UUID.randomUUID().toString().replace("-", "").take(8)
                            val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                putExtra("is_incognito", true)
                                putExtra("incognito_profile_name", uniqueProfileName)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("incognito_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "New Incognito Instance",
                            tint = if (viewModel.isIncognito) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }



                // Highly precise real progress bar (no auto-hide!)
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .testTag("webview_progress_indicator"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        },
        bottomBar = {
            // Static Bottom Control bar (no auto-hide!)
            BottomAppBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_control_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        enabled = canGoBack,
                        modifier = Modifier.testTag("back_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Forward
                    IconButton(
                        onClick = { viewModel.navigateForward() },
                        enabled = canGoForward,
                        modifier = Modifier.testTag("forward_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // Stop / Refresh
                    IconButton(
                        onClick = {
                            if (isLoading) viewModel.triggerStop() else viewModel.triggerReload()
                        },
                        modifier = Modifier.testTag("refresh_stop_button")
                    ) {
                        Icon(
                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isLoading) "Stop" else "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Home
                    IconButton(
                        onClick = { viewModel.navigateHome() },
                        modifier = Modifier.testTag("home_navigation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (currentUrl == "about:blank") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bookmarks
                    IconButton(
                        onClick = { viewModel.setShowBookmarks(!showBookmarks) },
                        modifier = Modifier.testTag("bookmarks_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showBookmarks) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Bookmarks",
                            tint = if (showBookmarks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // History
                    IconButton(
                        onClick = { viewModel.setShowHistory(!showHistory) },
                        modifier = Modifier.testTag("history_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showHistory) Icons.Filled.History else Icons.Default.History,
                            contentDescription = "History",
                            tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main body content: Always WebPage
            BrowserWebView(
                url = currentUrl,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Bookmarks Sliding Overlay Sheet
            AnimatedVisibility(
                visible = showBookmarks,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                BookmarksOverlay(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowBookmarks(false) }
                )
            }

            // History Sliding Overlay Sheet
            AnimatedVisibility(
                visible = showHistory,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                HistoryOverlay(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowHistory(false) }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    url: String,
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            if (viewModel.isIncognito && viewModel.incognitoProfileName != null) {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
                    try {
                        val profileStore = androidx.webkit.ProfileStore.getInstance()
                        profileStore.getOrCreateProfile(viewModel.incognitoProfileName)
                        androidx.webkit.WebViewCompat.setProfile(this, viewModel.incognitoProfileName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                builtInZoomControls = true
                displayZoomControls = false // Keep pinch-to-zoom manual, no cluttering UI controls
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
                
                if (viewModel.isIncognito) {
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                }
            }
            
            if (viewModel.isIncognito) {
                clearCache(true)
                clearHistory()
                clearFormData()
                
                // Clear cookies
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                
                // Clear web storage (localStorage, databases)
                val webStorage = android.webkit.WebStorage.getInstance()
                webStorage.deleteAllData()
            }

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    url?.let { viewModel.onPageStarted(it) }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let { viewModel.onPageFinished(this@apply, it) }
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Manual loading override, let the webview load internal links normally
                    return false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    viewModel.onProgressChanged(newProgress)
                    viewModel.onNavigationStateChanged(this@apply)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    title?.let { viewModel.onNavigationStateChanged(this@apply) }
                }
            }
        }
    }

    // Bind commands to the WebView instance
    LaunchedEffect(viewModel.commands) {
        viewModel.commands.collect { command ->
            when (command) {
                is BrowserCommand.GoBack -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    }
                }
                is BrowserCommand.GoForward -> {
                    if (webView.canGoForward()) {
                        webView.goForward()
                    }
                }
                is BrowserCommand.Reload -> {
                    webView.reload()
                }
                is BrowserCommand.StopLoading -> {
                    webView.stopLoading()
                }
                is BrowserCommand.LoadUrl -> {
                    webView.loadUrl(command.url)
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            if (viewModel.isIncognito) {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
                
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                
                val webStorage = android.webkit.WebStorage.getInstance()
                webStorage.deleteAllData()
            }
            webView.stopLoading()
            webView.destroy()

            if (viewModel.isIncognito && viewModel.incognitoProfileName != null) {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
                    try {
                        val profileStore = androidx.webkit.ProfileStore.getInstance()
                        profileStore.deleteProfile(viewModel.incognitoProfileName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.testTag("browser_webview_container")
    )
}



@Composable
fun BookmarksOverlay(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    var showAddManualDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {}, // Prevent closing when tapping card itself
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Bookmarks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        IconButton(
                            onClick = { showAddManualDialog = true },
                            modifier = Modifier.testTag("add_manual_bookmark_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Bookmark Manually",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StarOutline,
                                contentDescription = "No bookmarks",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No bookmarks saved yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Star a page to bookmark it!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("bookmarks_list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.loadUrl(bookmark.url)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Bookmark Star",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = bookmark.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteBookmark(bookmark.id) },
                                    modifier = Modifier.testTag("delete_bookmark_${bookmark.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Bookmark",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    if (showAddManualDialog) {
        AddManualBookmarkDialog(
            onDismiss = { showAddManualDialog = false },
            onSave = { title, url ->
                viewModel.addManualBookmark(title, url)
                showAddManualDialog = false
            }
        )
    }
}

@Composable
fun AddManualBookmarkDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_manual_bookmark_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Bookmark",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. My Favorite Site") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_bookmark_title_input")
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    placeholder = { Text("e.g. example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("manual_bookmark_url_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, url) },
                        enabled = url.trim().isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOverlay(
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    var showConfirmClear by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Surfing History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        if (history.isNotEmpty()) {
                            IconButton(
                                onClick = { showConfirmClear = true },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Clear All History",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryToggleOff,
                                contentDescription = "No history",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No history recorded",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Your visited sites will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("history_list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(history, key = { it.id }) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.loadUrl(entry.url)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = "History Item",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = entry.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHistoryEntry(entry.id) },
                                    modifier = Modifier.testTag("delete_history_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete history item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to completely clear your browsing history? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showConfirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("confirm_clear_history_dialog")
        )
    }
}


