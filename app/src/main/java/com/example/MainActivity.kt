package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.ui.BrowserMainScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var isIncognitoActivity = false
    private var activityProfileId = "Default"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isIncognito = intent?.getBooleanExtra("is_incognito", false) ?: false
        isIncognitoActivity = isIncognito
        val profileId = intent?.getStringExtra("profile_id") ?: if (isIncognito) "incognito_" + java.util.UUID.randomUUID().toString() else "Default"
        activityProfileId = profileId

        // Cleanup orphaned profiles when starting main screen
        if (!isIncognito && androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
            try {
                val profileStore = androidx.webkit.ProfileStore.getInstance()
                for (p in profileStore.getAllProfileNames()) {
                    if (p.startsWith("incognito_")) {
                        profileStore.deleteProfile(p)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            MyApplicationTheme {
                val database = BrowserDatabase.getDatabase(applicationContext)
                val repository = BrowserRepository(database)
                val viewModel: BrowserViewModel = viewModel(
                    factory = BrowserViewModel.Factory(repository, isIncognito, profileId)
                )

                BrowserMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isIncognitoActivity && activityProfileId != "Default" && isFinishing) {
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MULTI_PROFILE)) {
                try {
                    androidx.webkit.ProfileStore.getInstance().deleteProfile(activityProfileId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
