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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isIncognito = intent?.getBooleanExtra("is_incognito", false) ?: false
        setContent {
            MyApplicationTheme {
                val database = BrowserDatabase.getDatabase(applicationContext)
                val repository = BrowserRepository(database)
                val viewModel: BrowserViewModel = viewModel(
                    factory = BrowserViewModel.Factory(repository, isIncognito)
                )

                BrowserMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
