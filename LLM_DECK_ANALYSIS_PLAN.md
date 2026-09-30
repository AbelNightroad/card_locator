# LLM + Vector DB Integration Plan: MTG Deck Analysis Engine

> **STATUS: ON HOLD — not essential.** Future feature, blocked on having a trained
> model available. No work scheduled; no dependencies added; no code exists.
> Revisit only after the core app is stable. All phases below remain unchecked.

**Target**: On-device RAG system for Magic: The Gathering deck refinement using local LLM + sqlite-vec
**Constraints**: Fully offline, 2-10s latency, privacy-first, Android (min SDK 26), minimal dependencies

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android App                              │
├─────────────────────────────────────────────────────────────────┤
│  UI Layer (Compose)                                             │
│  ├─ DeckAnalysisScreen: User selects deck → sees suggestions   │
│  ├─ MetagameInsightsScreen: Format archetypes, winrates        │
│  └─ Settings: Model download, data refresh controls            │
├─────────────────────────────────────────────────────────────────┤
│  Domain Layer                                                   │
│  ├─ DeckAnalyzer: Orchestrates analysis pipeline               │
│  ├─ MetagameIngestor: Fetches & processes external sources     │
│  ├─ EmbeddingService: Generates vectors for cards/decks        │
│  ├─ VectorRepository: sqlite-vec CRUD + ANN search             │
│  └─ LLMInferenceEngine: MediaPipe GenAI wrapper                │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                     │
│  ├─ Room DB: cards, decks, scryfall_cards (existing)           │
│  ├─ sqlite-vec virtual table: embeddings (NEW)                 │
│  ├─ Metagame cache tables: archetypes, tournament results      │
│  └─ Model assets: GGUF model + tokenizer (downloaded on-demand)│
└─────────────────────────────────────────────────────────────────┘
```

**Data Flow (Deck Analysis Request)**:
```
User Deck → EmbeddingService → Vector Search (sqlite-vec)
    → Top-K Similar Decks + Metagame Context + MTG Rules Chunks
    → Prompt Constructor → LLMInferenceEngine (MediaPipe)
    → Structured Suggestions (add/remove/replace cards, reasoning)
    → UI Rendering
```

---

## 2. Vector Database Schema (sqlite-vec)

### 2.1 Virtual Table Definition

```sql
-- Virtual table for HNSW index (sqlite-vec v0.1.6+)
CREATE VIRTUAL TABLE deck_embeddings USING vec0(
    deck_id INTEGER PRIMARY KEY,      -- FK to decks.id
    embedding BLOB,                   -- 384-dim float32 (bge-small-en-v1.5)
    format TEXT,                      -- Commander, Modern, etc.
    archetype TEXT,                   -- "Rhinos", "4C Control", etc.
    winrate REAL,                     -- From metagame source
    last_updated INTEGER              -- Unix timestamp
);

-- Card-level embeddings for fine-grained retrieval
CREATE VIRTUAL TABLE card_embeddings USING vec0(
    scryfall_id TEXT PRIMARY KEY,     -- FK to scryfall_cards.id
    embedding BLOB,                   -- 384-dim
    name TEXT,
    type_line TEXT,
    oracle_text TEXT,
    color_identity TEXT,
    cmc REAL
);

-- MTG Rules/Rulings chunks for RAG
CREATE VIRTUAL TABLE rules_embeddings USING vec0(
    chunk_id INTEGER PRIMARY KEY AUTOINCREMENT,
    embedding BLOB,                   -- 384-dim
    source TEXT,                      -- "comp_rules", "ruling", "faq"
    section TEXT,                     -- "701.14", "608.2g", etc.
    content TEXT                      -- Full text chunk (~512 tokens)
);
```

### 2.2 Index Configuration

```sql
-- HNSW parameters for ANN search (balance recall/speed)
-- M=16, ef_construction=200, ef_search=50 (tunable)
```

### 2.3 Storage Estimates

| Table | Rows | Dim | Size (est.) |
|-------|------|-----|-------------|
| `deck_embeddings` | 50,000 | 384 | ~75 MB |
| `card_embeddings` | 30,000 | 384 | ~45 MB |
| `rules_embeddings` | 5,000 | 384 | ~7.5 MB |
| **Total** | | | **~130 MB** |

---

## 3. Embedding Model Selection

### 3.1 Recommended: `bge-small-en-v1.5` (384-dim)

| Property | Value |
|----------|-------|
| Parameters | 33M |
| Dimension | 384 |
| Max Seq Len | 512 |
| MTEB Score | 62.3 (retrieval) |
| Quantized Size | ~28 MB (INT8) / ~14 MB (INT4) |
| License | MIT |

**Why**: Best quality/size for on-device, multilingual support (MTG cards in JP/CN), fast inference via ONNX Runtime.

### 3.2 Integration: **ONNX Runtime Android**

```kotlin
// app/build.gradle.kts
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.0")
```

- Mature, GPU/NPU delegates, Apache 2.0
- Single AAR (~15 MB), no JNI compilation needed
- Model bundled in `res/raw/bge_small_en_v15.onnx` (~14 MB INT4)

---

## 4. LLM Inference Engine: MediaPipe GenAI (Minimal Dependencies)

### 4.1 Why MediaPipe GenAI

| Factor | MediaPipe GenAI | llama.cpp JNI |
|--------|-----------------|---------------|
| **Dependencies** | 1 AAR (`tasks-genai`) | Custom NDK build + JNI |
| **Setup time** | ~10 lines | ~1 week |
| **Model support** | Gemma-2-2B, Phi-3-mini, Llama-3.2-1B | Any GGUF |
| **Tokenization** | Built-in | Manual |
| **Binary size** | ~5 MB | ~3 MB (but +NDK toolchain) |
| **Maintenance** | Google-maintained | Self-maintained |

**Single dependency**:
```kotlin
// app/build.gradle.kts
implementation("com.google.mediapipe:tasks-genai:0.10.14")
```

### 4.2 Supported Models (all < 2 GB, Apache 2.0 / MIT)

| Model | Size (Q4) | Context | Best For |
|-------|-----------|---------|----------|
| **Gemma-2-2B-it** | 1.6 GB | 8K | Structured JSON output, instruction following |
| **Phi-3-mini-4k-instruct** | 2.3 GB | 4K | Reasoning, MTG rules knowledge |
| **Llama-3.2-1B-Instruct** | 0.9 GB | 8K | Lowest RAM, fastest |

**Top Pick**: **Gemma-2-2B-it** (1.6 GB)
- Smallest high-quality model with strong JSON mode
- Google-maintained, optimized for MediaPipe
- 8K context > RAG needs (deck + 10 chunks)

### 4.3 Implementation

```kotlin
// ai/llm/MediaPipeLlmEngine.kt
class MediaPipeLlmEngine(private val context: Context) {
    private var llm: LlmInference? = null
    private val modelName = "gemma-2-2b-it-q4"  // MediaPipe model ID
    
    suspend fun ensureModel(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val modelPath = LlmInference.downloadModel(context, modelName)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTemperature(0.2f)
                .setTopK(40)
                .build()
            llm = LlmInference.createFromOptions(context, options)
            Result.success(modelPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generate(prompt: String): Result<String> = withContext(Dispatchers.Default) {
        llm?.let { Result.success(it.generateResponse(prompt)) }
            ?: Result.failure(IllegalStateException("Model not loaded"))
    }
    
    fun close() { llm?.close(); llm = null }
}
```

### 4.4 Model Distribution

- **No bundling** — MediaPipe downloads on first use to app cache
- **WorkManager** triggers download on WiFi + charging
- **Progress notification** via `LlmInference.addModelDownloadProgressListener()`
- **No Play Store size impact** — model stored in app-specific cache

### 4.5 User-Facing Download Guards

Before any model download, the app must enforce three safeguards:

#### 4.5.1 Explicit User Consent Dialog
When the user first opens **DeckAnalysisScreen** and no model is cached, show a modal dialog:
```kotlin
@Composable
fun ModelDownloadConsentDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text("Download AI Model") },
        text = { Text("""
            This feature requires downloading a ~1.6 GB AI model (Gemma-2-2B) 
            to analyze your deck locally on your device. The model enables 
            offline deck suggestions without sending your data anywhere.
            
            Download happens once. You can delete it anytime in Settings.
        """.trimIndent()) },
        confirmButton = { 
            Button(onClick = { onAccept(); onDismissRequest() }) { Text("Download") }
        },
        dismissButton = { 
            TextButton(onClick = { onDecline(); onDismissRequest() }) { Text("Not Now") }
        }
    )
}
```
- **Persist consent** in `SettingsStore` (`llm_model_consent: Boolean = false`)
- **Gate** `DeckAnalysisViewModel.analyze()` behind this flag

#### 4.5.2 Disk Space Pre-Check
Before starting download, verify sufficient free space:
```kotlin
// ai/llm/ModelManager.kt
suspend fun checkDiskSpace(context: Context, requiredBytes: Long = 2_000_000_000L): Boolean {
    val statFs = StatFs(context.cacheDir.path)
    val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong
    return availableBytes >= requiredBytes
}

// Usage in ModelDownloadWorker:
override suspend fun doWork(): Result {
    if (!ModelManager.checkDiskSpace(applicationContext)) {
        // Notify user via notification channel
        NotificationManager.showLowSpaceWarning(applicationContext)
        return Result.failure()
    }
    // ... proceed with download
}
```
- **Threshold**: 2 GB free (model 1.6 GB + overhead)
- **Show actionable toast/notification**: "Free up 2 GB to download AI model"

#### 4.5.3 Network Type Warning (Cellular vs WiFi)
MediaPipe's `LlmInference.downloadModel()` respects `WorkManager` constraints, but add a user-visible warning if they manually trigger download on cellular:
```kotlin
// In DeckAnalysisViewModel when user taps "Analyze" without model:
private fun assertWiFiOrWarn(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetwork ?: return true // no network, let WorkManager handle
    val capabilities = cm.getNetworkCapabilities(activeNetwork)
        ?: return true
    
    val isMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED).not()
    if (isMetered) {
        // Show inline warning banner in UI, not blocking
        _uiState.update { it.copy(showCellularWarning = true) }
        return false
    }
    return true
}

// Compose banner:
@Composable
fun CellularWarningBanner(onDismiss: () -> Unit) {
    Banner(
        modifier = Modifier.fillMaxWidth(),
        leading = { Icon(Icons.Default.Warning, contentDescription = null) },
        content = { Text("Downloading ~1.6 GB over cellular may incur charges. Connect to Wi-Fi or continue anyway.") },
        actions = { 
            TextButton(onClick = onDismiss) { Text("Dismiss") }
            Button(onClick = { /* proceed anyway */ }) { Text("Continue") }
        }
    )
}
```
- **Non-blocking** — user can proceed, but warned
- **WorkManager constraint** still enforces `UNMETERED` for background auto-download

---

## 5. Metagame Data Ingestion Pipeline

### 5.1 Sources & Refresh Cadence

| Source | Format | Frequency | Data Type |
|--------|--------|-----------|-----------|
| **mtgtop8.com** | HTML scrape | Weekly | Tournament decks, archetypes, meta % |
| **MTGMelee/Melee.gg** | GraphQL API | Daily | Event decks, standings |
| **MTGGoldfish** | JSON/CSV | Daily | Format metagame %, decklists |
| **17Lands** | Public API | Weekly | Limited archetypes, winrates by color pair |

### 5.2 Ingestion Architecture

```kotlin
// Data/MetagameIngestor.kt
class MetagameIngestor(
    private val mtgTop8Scraper: MtgTop8Scraper,
    private val meleeClient: MeleeGraphQLClient,
    private val goldfishClient: MTGGoldfishClient,
    private val landsClient: SeventeenLandsClient,
    private val vectorRepo: VectorRepository,
    private val embeddingService: EmbeddingService
) {
    suspend fun refreshAll() = coroutineScope {
        launch { ingestMtgTop8() }
        launch { ingestMelee() }
        launch { ingestGoldfish() }
        launch { ingest17Lands() }
    }
    
    private suspend fun ingestMtgTop8() {
        val archetypes = mtgTop8Scraper.fetchAllFormats()
        for (arch in archetypes) {
            val deck = mtgTop8Scraper.fetchSampleDeck(arch.archetypeId)
            val embedding = embeddingService.embedDeck(deck)
            vectorRepo.upsertDeckEmbedding(DeckEmbedding(
                deckId = -arch.archetypeId,
                embedding = embedding,
                format = arch.format,
                archetype = arch.name,
                winrate = arch.metaPercentage,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }
}
```

### 5.3 Deck Representation for Embedding

```kotlin
data class DeckTextRepresentation(
    val name: String,
    val format: String,
    val commander: String?,
    val mainboard: List<CardSlot>,
    val sideboard: List<CardSlot>,
    val colorIdentity: String
) {
    fun toEmbeddingText(): String = buildString {
        append("Format: $format\n")
        commander?.let { append("Commander: $it\n") }
        append("Mainboard:\n")
        mainboard.forEach { append("  ${it.quantity}x ${it.name}\n") }
        if (sideboard.isNotEmpty()) {
            append("Sideboard:\n")
            sideboard.forEach { append("  ${it.quantity}x ${it.name}\n") }
        }
    }
}
```

---

## 6. RAG Pipeline Design

### 6.1 Retrieval Strategy

```kotlin
class DeckAnalysisRAG(
    private val vectorRepo: VectorRepository,
    private val embeddingService: EmbeddingService,
    private val scryfallRepo: ScryfallRepository
) {
    data class RetrievedContext(
        val similarDecks: List<DeckEmbedding>,
        val metagameArchetypes: List<DeckEmbedding>,
        val relevantRules: List<RulesChunk>,
        val keyCards: List<CardEmbedding>
    )
    
    suspend fun retrieveContext(userDeck: DeckWithCards): RetrievedContext {
        val deckEmb = embeddingService.embedDeck(userDeck.toRepresentation())
        
        return coroutineScope {
            val similarDecks = async { vectorRepo.searchDecks(deckEmb, k = 5, filterFormat = userDeck.format) }
            val metaDecks = async { vectorRepo.searchArchetypes(userDeck.format, k = 3) }
            val rules = async { retrieveRules(userDeck) }
            val keyCards = async { retrieveKeyCards(userDeck) }
            
            RetrievedContext(
                similarDecks = similarDecks.await(),
                metagameArchetypes = metaDecks.await(),
                relevantRules = rules.await(),
                keyCards = keyCards.await()
            )
        }
    }
    
    private suspend fun retrieveRules(deck: DeckWithCards): List<RulesChunk> {
        val queryText = buildRulesQuery(deck)
        val queryEmb = embeddingService.embedText(queryText)
        return vectorRepo.searchRules(queryEmb, k = 3)
    }
}
```

### 6.2 Prompt Template (Structured JSON Output)

```kotlin
const val DECK_ANALYSIS_PROMPT = """
You are an expert Magic: The Gathering deck analyst. Analyze the user's deck against the metagame and provide specific, actionable suggestions.

=== USER DECK ===
Format: {format}
Commander: {commander}
Mainboard ({mainboardCount} cards):
{mainboardList}
Sideboard ({sideboardCount} cards):
{sideboardList}

=== METAGAME CONTEXT ===
Top Archetypes in {format}:
{archetypeSummaries}

Similar Successful Decks:
{similarDeckSummaries}

=== RELEVANT RULES ===
{rulesChunks}

=== YOUR TASK ===
Provide suggestions as JSON:
{
  "overallAssessment": "Brief 2-3 sentence assessment",
  "strengths": ["strength1", "strength2"],
  "weaknesses": ["weakness1", "weakness2"],
  "suggestions": [
    {
      "type": "ADD" | "REMOVE" | "REPLACE" | "SIDEBOARD",
      "cardName": "Card Name",
      "quantity": 1,
      "reasoning": "Specific reason citing metagame/rules",
      "priority": 1-5,
      "slot": "mainboard" | "sideboard" | "commander"
    }
  ],
  "metagamePositioning": "How this deck sits vs top archetypes",
  "ruleClarifications": ["Any relevant rules notes"]
}

Respond ONLY with valid JSON. No markdown, no extra text.
"""
```

### 6.3 Response Parsing & Validation

```kotlin
@Serializable
data class DeckSuggestion(
    @SerialName("type") val type: SuggestionType,
    @SerialName("cardName") val cardName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("reasoning") val reasoning: String,
    @SerialName("priority") val priority: Int,
    @SerialName("slot") val slot: String
)

enum class SuggestionType { ADD, REMOVE, REPLACE, SIDEBOARD }

@Serializable
data class DeckAnalysisResult(
    val overallAssessment: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val suggestions: List<DeckSuggestion>,
    val metagamePositioning: String,
    val ruleClarifications: List<String>
)
```

---

## 7. Android Integration Architecture

### 7.1 New Dependencies (Minimal)

```kotlin
// app/build.gradle.kts

// Vector DB - single AAR
implementation("com.github.asg017:sqlite-vec:0.1.6")

// ONNX Runtime for embeddings
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.0")

// MediaPipe GenAI for LLM (single AAR, no JNI)
implementation("com.google.mediapipe:tasks-genai:0.10.14")

// Background work
implementation("androidx.work:work-runtime-ktx:2.9.1")

// Serialization (already present)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
```

**Total new deps: 3 AARs** (sqlite-vec, ONNX Runtime, MediaPipe GenAI)

### 7.2 Module Structure

```
app/src/main/java/com/gitlab/abelnightroad/
├── ai/
│   ├── embedding/
│   │   ├── EmbeddingService.kt
│   │   ├── OnnxEmbeddingModel.kt
│   │   └── Tokenizer.kt
│   ├── llm/
│   │   ├── LlmEngine.kt              // Interface
│   │   ├── MediaPipeLlmEngine.kt     // MediaPipe implementation
│   │   ├── ModelManager.kt           // Download, cache via MediaPipe
│   │   └── PromptTemplates.kt
│   ├── rag/
│   │   ├── DeckAnalysisRAG.kt
│   │   ├── VectorRepository.kt
│   │   └── RetrievalResult.kt
│   └── analysis/
│       ├── DeckAnalyzer.kt
│       ├── MetagameIngestor.kt
│       └── AnalysisResult.kt
├── db/vec/
│   ├── VecDbHelper.kt
│   ├── DeckEmbeddingDao.kt
│   ├── CardEmbeddingDao.kt
│   └── RulesEmbeddingDao.kt
├── db/metagame/
│   ├── ArchetypeEntity.kt
│   ├── TournamentResultEntity.kt
│   └── MetagameDao.kt
├── data/metagame/
│   ├── MtgTop8Scraper.kt
│   ├── MeleeGraphQLClient.kt
│   ├── MTGGoldfishClient.kt
│   └── SeventeenLandsClient.kt
├── data/ai/
│   └── ModelDownloadWorker.kt
└── ui/
    ├── deckanalysis/
    │   ├── DeckAnalysisScreen.kt
    │   ├── DeckAnalysisViewModel.kt
    │   └── components/
    │       ├── SuggestionCard.kt
    │       ├── MetagamePositioningChart.kt
    │       └── RuleClarificationChip.kt
    └── metagame/
        ├── MetagameInsightsScreen.kt
        └── MetagameViewModel.kt
```

### 7.3 sqlite-vec Initialization

```kotlin
// db/vec/VecDbHelper.kt
class VecDbHelper(context: Context) {
    private val db: SupportSQLiteDatabase = SQLiteOpenHelper(context, "card_locator.db", null, 9) {}.writableDatabase
    
    init {
        db.execSQL("SELECT vec_load_extension()")
        db.execSQL("""
            CREATE VIRTUAL TABLE IF NOT EXISTS deck_embeddings USING vec0(
                deck_id INTEGER PRIMARY KEY,
                embedding BLOB,
                format TEXT,
                archetype TEXT,
                winrate REAL,
                last_updated INTEGER
            )
        """)
        // ... card_embeddings, rules_embeddings
    }
    
    fun getDatabase(): SupportSQLiteDatabase = db
}
```

### 7.4 Background Processing (WorkManager)

```kotlin
// Model download worker (MediaPipe handles actual download)
class ModelDownloadWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        MediaPipeLlmEngine(applicationContext).ensureModel().getOrThrow()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

// Metagame refresh worker (weekly)
class MetagameRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        MetagameIngestor.create(context).refreshAll()
        return Result.success()
    }
}
```

---

## 8. UI/UX Design

### 8.1 New Navigation Entry

Add to `Screen.kt`:
```kotlin
sealed interface Screen {
    data object DeckAnalysis : Screen
    data object MetagameInsights : Screen
}
```

### 8.2 DeckAnalysisScreen Flow

```
DeckAnalysisScreen
├─ Deck Selector (dropdown of user's decks by format)
├─ "Analyze" Button (disabled during analysis)
├─ Progress Indicator (Embedding → Retrieval → LLM → Parsing)
├─ Results Section:
│  ├─ Overall Assessment Card
│  ├─ Strengths/Weaknesses Chips
│  ├─ Suggestions List (priority-sorted, color-coded by type)
│  │  └─ Each: Type badge, Card name, Reasoning, Accept/Reject
│  ├─ Metagame Positioning (text + small bar chart)
│  └─ Rule Clarifications (expandable chips)
└─ "Apply to Deck" FAB → Opens DeckView with suggestions pre-filled
```

### 8.3 Suggestion Card Component

```kotlin
@Composable
fun SuggestionCard(
    suggestion: DeckSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (suggestion.type) {
                SuggestionType.ADD -> MaterialTheme.colorScheme.tertiaryContainer
                SuggestionType.REMOVE -> MaterialTheme.colorScheme.errorContainer
                SuggestionType.REPLACE -> MaterialTheme.colorScheme.primaryContainer
                SuggestionType.SIDEBOARD -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(text = suggestion.type.name)
                    Text(suggestion.cardName, style = MaterialTheme.typography.titleSmall)
                    Text("×${suggestion.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(suggestion.reasoning, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                Row {
                    PriorityIndicator(suggestion.priority)
                    Text("Slot: ${suggestion.slot}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Row {
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Apply") }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Dismiss") }
            }
        }
    }
}
```

---

## 9. Implementation Phases

### Phase 1: Foundation (Week 1-2)
- [ ] Add sqlite-vec + ONNX Runtime + MediaPipe GenAI dependencies
- [ ] Create `VecDbHelper` + virtual table migrations
- [ ] Implement `EmbeddingService` with `bge-small-en-v1.5.onnx`
- [ ] Unit test: Embed known cards, verify vector dimensions

### Phase 2: Vector Population (Week 2-3)
- [ ] Create `CardEmbeddingDao`, `DeckEmbeddingDao`, `RulesEmbeddingDao`
- [ ] Populate `card_embeddings` from existing `scryfall_cards` table
- [ ] Build `rules_embeddings` from Comprehensive Rules + Gatherer rulings
- [ ] Implement `DeckEmbedding` generation from `DeckWithCards`
- [ ] Benchmark ANN search latency on device

### Phase 3: Metagame Ingestion (Week 3-4)
- [ ] Implement `MeleeGraphQLClient`, `MTGGoldfishClient`, `SeventeenLandsClient`
- [ ] Extend `MtgTop8Scraper` for winrate/archetype metadata
- [ ] Create `MetagameIngestor` orchestrating all sources
- [ ] Schedule weekly `MetagameRefreshWorker`

### Phase 4: LLM Integration (Week 4)
- [ ] Implement `MediaPipeLlmEngine` wrapper (minimal, ~50 lines)
- [ ] Implement `ModelManager` using MediaPipe downloader
- [ ] Build prompt templates with JSON schema enforcement
- [ ] Test inference latency on target devices

### Phase 5: RAG Pipeline (Week 5)
- [ ] Implement `DeckAnalysisRAG` retrieval logic
- [ ] Build `DeckAnalyzer` orchestrating full pipeline
- [ ] Add response parsing + validation
- [ ] Handle edge cases: empty retrieval, JSON parse failure, OOM

### Phase 6: UI Integration (Week 6)
- [ ] Add `DeckAnalysisScreen` + `DeckAnalysisViewModel`
- [ ] Create suggestion components with accept/reject
- [ ] Add "Apply to Deck" flow
- [ ] Enhance `MetagameScreen` with archetype winrate charts
- [ ] Settings: Model management, data refresh, analysis depth

### Phase 7: Polish & Evaluation (Week 7)
- [ ] Create evaluation dataset (50 decks with expert annotations)
- [ ] Measure: Suggestion relevance, rule accuracy, latency
- [ ] Optimize: HNSW params, prompt length, context window
- [ ] Fix OOM crashes, add memory monitoring
- [ ] Documentation + README update

---

## 10. Testing Strategy

### 10.1 Unit Tests (JVM)
| Component | Test Focus |
|-----------|------------|
| `EmbeddingService` | Vector dimensions, normalization, batch processing |
| `VectorRepository` | ANN recall@k, filter by format, upsert idempotency |
| `DeckAnalysisRAG` | Retrieval diversity, context window sizing |
| `PromptTemplates` | JSON validity, token count < 3500 |
| `ModelManager` | Download resume, cache management |

### 10.2 Integration Tests (Device)
- End-to-end: User deck → suggestions → apply
- Metagame refresh → vector upsert → search consistency
- Model download → inference → memory profile

### 10.3 Evaluation Dataset
```
eval/
├── decks/
│   ├── commander_ces.yaml
│   ├── modern_rhinos.yaml
│   ├── pioneer_lotus.yaml
│   └── pauper_burn.yaml
├── expected_suggestions/
│   └── *.json
└── run_eval.py
```

**Metrics**: Suggestion Precision, Rule Accuracy, Latency P50/P95, Memory Peak

---

## 11. Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Model too large for 4GB RAM | Medium | Crash | Gemma-2-2B (1.6 GB) fits; fallback to Llama-3.2-1B (0.9 GB) |
| sqlite-vec JNI crashes | Low | Crash | Fallback to brute-force linear scan |
| Inference > 10s on low-end | Medium | UX fail | Timeout + partial results, allow cancellation |
| Metagame scraping breaks | Medium | Stale data | Multiple sources, graceful degradation |
| LLM hallucinates card names | Low | Bad suggestions | Post-validate vs Scryfall DB, constrained JSON |

---

## 12. File Tree Summary (New Files)

```
app/src/main/
├── assets/models/.gitkeep          # Empty - models downloaded at runtime
├── java/com/gitlab/abelnightroad/
│   ├── ai/
│   │   ├── embedding/
│   │   │   ├── EmbeddingService.kt
│   │   │   ├── OnnxEmbeddingModel.kt
│   │   │   └── Tokenizer.kt
│   │   ├── llm/
│   │   │   ├── LlmEngine.kt
│   │   │   ├── MediaPipeLlmEngine.kt
│   │   │   ├── ModelManager.kt
│   │   │   └── PromptTemplates.kt
│   │   ├── rag/
│   │   │   ├── DeckAnalysisRAG.kt
│   │   │   ├── VectorRepository.kt
│   │   │   └── RetrievalResult.kt
│   │   └── analysis/
│   │       ├── DeckAnalyzer.kt
│   │       ├── MetagameIngestor.kt
│   │       └── AnalysisResult.kt
│   ├── db/vec/
│   │   ├── VecDbHelper.kt
│   │   ├── DeckEmbeddingDao.kt
│   │   ├── CardEmbeddingDao.kt
│   │   └── RulesEmbeddingDao.kt
│   ├── db/metagame/
│   │   ├── ArchetypeEntity.kt
│   │   ├── TournamentResultEntity.kt
│   │   └── MetagameDao.kt
│   ├── data/metagame/
│   │   ├── MeleeGraphQLClient.kt
│   │   ├── MTGGoldfishClient.kt
│   │   └── SeventeenLandsClient.kt
│   ├── data/ai/
│   │   └── ModelDownloadWorker.kt
│   └── ui/
│       ├── deckanalysis/
│       │   ├── DeckAnalysisScreen.kt
│       │   ├── DeckAnalysisViewModel.kt
│       │   └── components/
│       │       ├── SuggestionCard.kt
│       │       ├── MetagamePositioningChart.kt
│       │       └── RuleClarificationChip.kt
│       └── metagame/
│           ├── MetagameInsightsScreen.kt
│           └── MetagameViewModel.kt
└── res/raw/
    └── bge_small_en_v15.onnx       # Embedding model (~14 MB INT4)
```

---

## 13. Next Steps

1. **Add 3 dependencies** to `app/build.gradle.kts` (sqlite-vec, ONNX Runtime, MediaPipe GenAI)
2. **Prototype** Phase 1: embeddings + sqlite-vec on device
3. **Acquire** MTG rules/rulings corpus for `rules_embeddings`
4. **Start** Phase 1 implementation

---

*Generated for card_locator project — MTG Deck Analysis RAG System (MediaPipe GenAI + sqlite-vec + ONNX)*