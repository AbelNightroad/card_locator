package com.gitlab.abelnightroad

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.gitlab.abelnightroad.data.CardRepository
import com.gitlab.abelnightroad.data.ScryfallRepository
import com.gitlab.abelnightroad.data.SettingsStore
import com.gitlab.abelnightroad.ui.AppNavigation
import com.gitlab.abelnightroad.ui.MainViewModel
import com.gitlab.abelnightroad.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = CardRepository.create(this)
        val scryfall = ScryfallRepository.create(this)
        val settings = SettingsStore(this)
        val mainViewModel = MainViewModel(repository, settings)

        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) { scryfall.syncIfNeeded(this@MainActivity) }
            val msg = when (status) {
                is ScryfallRepository.SyncStatus.Populated ->
                    "Imported ${status.inserted} reference cards"
                is ScryfallRepository.SyncStatus.Updated ->
                    "Updated ${status.inserted} reference cards"
                ScryfallRepository.SyncStatus.UpToDate ->
                    "Scryfall reference data is up to date"
                ScryfallRepository.SyncStatus.Skipped -> null
                is ScryfallRepository.SyncStatus.Error ->
                    "Scryfall sync failed: ${status.message}"
            }
            msg?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
        }

        setContent {
            val themeId by mainViewModel.themeId.collectAsState(initial = "nord")
            val dark by mainViewModel.darkMode.collectAsState(initial = true)
            val fontId by mainViewModel.fontId.collectAsState(initial = "roboto")
            AppTheme(themeId = themeId, dark = dark, fontId = fontId) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        mainViewModel = mainViewModel,
                        repository = repository,
                        scryfall = scryfall,
                        settings = settings
                    )
                }
            }
        }
    }
}
