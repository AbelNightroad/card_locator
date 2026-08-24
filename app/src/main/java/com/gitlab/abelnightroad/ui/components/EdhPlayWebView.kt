package com.gitlab.abelnightroad.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gitlab.abelnightroad.data.MetaDeckCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

private const val JS_BRIDGE_NAME = "EdhPlayBridge"

private val jsonDecoder = Json { ignoreUnknownKeys = true; isLenient = true }

private const val EXTRACT_SCRIPT = """
(function() {
    function extractDeck() {
        var cards = [];
        var zones = document.querySelectorAll('[class*="board"], [class*="zone"], [data-zone]');
        if (zones.length === 0) {
            zones = document.querySelectorAll('div');
        }

        var currentZone = 'mainboard';
        var allText = document.body.innerText;

        var lines = allText.split('\n');
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            if (!line) continue;

            var lower = line.toLowerCase().replace(/^\/\/\s*/, '').replace(/:$/, '').trim();
            if (/^(commander|mainboard|main deck|sideboard|companion|maybeboard|considering)$/.test(lower)) {
                if (lower.indexOf('command') >= 0) currentZone = 'commander';
                else if (lower.indexOf('side') >= 0) currentZone = 'sideboard';
                else if (lower.indexOf('companion') >= 0) currentZone = 'companion';
                else currentZone = 'mainboard';
                continue;
            }

            var match = line.match(/^(\d+)\s*x?\s+(.+)/);
            if (match) {
                cards.push({
                    quantity: parseInt(match[1]),
                    name: match[2].trim(),
                    slot: currentZone
                });
            }
        }

        if (cards.length > 0) {
            EdhPlayBridge.onDeckExtracted(JSON.stringify(cards));
        } else {
            EdhPlayBridge.onDeckExtracted('[]');
        }
    }

    if (document.readyState === 'complete') {
        setTimeout(extractDeck, 2000);
    } else {
        window.addEventListener('load', function() {
            setTimeout(extractDeck, 2000);
        });
    }
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdhPlayWebView(
    deckUrl: String,
    onBack: () -> Unit,
    onDeckParsed: (List<MetaDeckCard>) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var extractionDone by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EDH Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onDeckExtracted(json: String) {
                                if (extractionDone) return
                                extractionDone = true
                                val cards = parseExtractedJson(json)
                                onDeckParsed(cards)
                            }
                        }, JS_BRIDGE_NAME)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.evaluateJavascript(EXTRACT_SCRIPT, null)
                            }
                        }

                        loadUrl(deckUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "Loading EDH Play...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseExtractedJson(json: String): List<MetaDeckCard> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        jsonDecoder.decodeFromString<List<MetaDeckCard>>(json)
    } catch (_: Exception) {
        emptyList()
    }
}
