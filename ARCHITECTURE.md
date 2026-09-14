# ARCHITECTURE.md — MtG Card Tracker Rebuild Spec

> This document is a complete technical specification for rebuilding the MtG Card
> Tracker Android app from scratch. Every schema, API contract, navigation route,
> and UI behavior is documented here.

---

## 1. Project Identity

| Field | Value |
|-------|-------|
| **App name** | MtG Card Tracker |
| **Package** | `com.gitlab.abelnightroad` |
| **Language** | Kotlin |
| **UI framework** | Jetpack Compose + Material3 |
| **Database** | Room (SQLite) |
| **Serialization** | kotlinx.serialization |
| **Min SDK** | 26 |
| **Target/Compile SDK** | 35 |
| **Gradle** | 9.5.1 |
| **JDK** | 17 (system JDK, no toolchain pin) |

---

## 2. Build Configuration

### Plugins

| Plugin | Version |
|--------|---------|
| `com.android.application` | 8.10.1 |
| `org.jetbrains.kotlin.android` | 2.1.21 |
| `org.jetbrains.kotlin.plugin.compose` | 2.1.21 |
| `org.jetbrains.kotlin.plugin.serialization` | 2.1.21 |
| `com.google.devtools.ksp` | 2.1.21-2.0.1 |

### Dependencies (exact versions)

| Group:Artifact | Version |
|----------------|---------|
| `androidx.compose:compose-bom` | 2025.06.00 |
| `androidx.compose.material3:material3` | BOM |
| `androidx.compose.material:material-icons-extended` | BOM |
| `androidx.activity:activity-compose` | 1.10.1 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.9.0 |
| `androidx.navigation:navigation-compose` | 2.9.0 |
| `androidx.datastore:datastore-preferences` | 1.1.4 |
| `androidx.room:room-runtime` | 2.7.0 |
| `androidx.room:room-ktx` | 2.7.0 |
| `androidx.room:room-compiler` | 2.7.0 (KSP) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 |
| `io.coil-kt:coil-compose` | 2.7.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 |
| `org.jsoup:jsoup` | 1.18.1 |
| `br.com.devsrsouza.compose.icons:octicons` | 1.1.1 |
| `androidx.camera:camera-core` | 1.4.1 |
| `androidx.camera:camera-camera2` | 1.4.1 |
| `androidx.camera:camera-lifecycle` | 1.4.1 |
| `androidx.camera:camera-view` | 1.4.1 |
| `com.google.mlkit:text-recognition` | 16.0.1 |
| `com.google.mlkit:text-recognition-chinese` | 16.0.1 |
| `com.google.mlkit:text-recognition-japanese` | 16.0.1 |
| `com.google.mlkit:text-recognition-korean` | 16.0.1 |
| `com.google.android.gms:play-services-mlkit-text-recognition` | 19.0.1 |
| `com.google.android.gms:play-services-tasks` | 18.1.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.10.2 |

### Gradle Properties

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

### Fonts

Place `.ttf` files in `app/src/main/res/font/`:
- `comic_neue.ttf` (Comic Neue)
- `germania_one.ttf` (Germania One)
- Roboto is system default, no file needed.

---

## 3. Database Schema (Room v9)

Database name: `card_locator.db`
`fallbackToDestructiveMigration(true)` is enabled.

### 3.1 `cards` table

```sql
CREATE TABLE cards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    set_code TEXT NOT NULL,
    set_name TEXT NOT NULL,
    collector_number TEXT NOT NULL,
    foil TEXT NOT NULL,
    rarity TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    mana_box_id TEXT NOT NULL,
    scryfall_id TEXT NOT NULL,
    purchase_price REAL NOT NULL DEFAULT 0,
    misprint INTEGER NOT NULL DEFAULT 0,
    altered INTEGER NOT NULL DEFAULT 0,
    condition TEXT NOT NULL,
    language TEXT NOT NULL,
    purchase_price_currency TEXT NOT NULL,
    added TEXT NOT NULL,
    tag TEXT NOT NULL
);
CREATE INDEX index_cards_tag ON cards(tag);
CREATE INDEX index_cards_name_set_code ON cards(name, set_code);
```

### 3.2 `scryfall_cards` table

```sql
CREATE TABLE scryfall_cards (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    set_code TEXT NOT NULL,
    set_name TEXT NOT NULL,
    collector_number TEXT NOT NULL,
    rarity TEXT NOT NULL,
    mana_cost TEXT NOT NULL,
    type_line TEXT NOT NULL,
    oracle_text TEXT NOT NULL,
    price_usd REAL DEFAULT NULL,
    color_identity TEXT NOT NULL DEFAULT '',
    image_url TEXT DEFAULT NULL,
    cmc REAL NOT NULL DEFAULT 0.0,
    legalities TEXT NOT NULL DEFAULT '',
    reserved INTEGER NOT NULL DEFAULT 0,
    game_changer INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX index_scryfall_cards_name ON scryfall_cards(name);
```

### 3.3 `decks` table

```sql
CREATE TABLE decks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    format TEXT NOT NULL,
    source TEXT NOT NULL,
    cover_scryfall_id TEXT DEFAULT NULL,
    created_at INTEGER NOT NULL DEFAULT (current_timestamp)
);
```

### 3.4 `deck_cards` table

```sql
CREATE TABLE deck_cards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    deck_id INTEGER NOT NULL,
    scryfall_id TEXT NOT NULL,
    card_name TEXT NOT NULL,
    set_code TEXT NOT NULL,
    set_name TEXT NOT NULL,
    collector_number TEXT NOT NULL,
    rarity TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    mana_cost TEXT NOT NULL,
    type_line TEXT NOT NULL,
    slot TEXT NOT NULL DEFAULT 'mainboard',
    color_identity TEXT NOT NULL DEFAULT '',
    condition TEXT NOT NULL DEFAULT 'NM',
    price_usd REAL NOT NULL DEFAULT 0.0,
    FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE
);
CREATE INDEX index_deck_cards_deck_id ON deck_cards(deck_id);
```

### 3.5 `tags` table

```sql
CREATE TABLE tags (tag TEXT PRIMARY KEY NOT NULL);
```

### 3.6 `scan_sessions` table

```sql
CREATE TABLE scan_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    createdAt TEXT NOT NULL
);
```

### 3.7 `scanned_cards` table

```sql
CREATE TABLE scanned_cards (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sessionId INTEGER NOT NULL,
    name TEXT NOT NULL,
    setCode TEXT NOT NULL,
    setName TEXT NOT NULL,
    collectorNumber TEXT NOT NULL,
    rarity TEXT NOT NULL,
    manaCost TEXT NOT NULL,
    typeLine TEXT NOT NULL,
    oracleText TEXT NOT NULL,
    colorIdentity TEXT NOT NULL,
    scryfallId TEXT NOT NULL,
    priceUsd REAL NOT NULL,
    language TEXT NOT NULL,
    FOREIGN KEY (sessionId) REFERENCES scan_sessions(id) ON DELETE CASCADE
);
CREATE INDEX index_scanned_cards_sessionId ON scanned_cards(sessionId);
```

### Migrations

All migrations are applied in `AppDatabaseProvider`. `fallbackToDestructiveMigration(true)` handles any gaps.

| From→To | Changes |
|---------|---------|
| 3→4 | `CREATE TABLE tags(tag TEXT PK)`, populate from `DISTINCT tag FROM cards` |
| 4→5 | Add `slot`, `color_identity` to `deck_cards`; add `color_identity` to `scryfall_cards` |
| 5→6 | Add `image_url` to `scryfall_cards` |
| 6→7 | Add `cmc`, `legalities`, `reserved`, `game_changer` to `scryfall_cards` |
| 7→8 | Add `condition`, `price_usd` to `deck_cards` |
| 8→9 | Create `scan_sessions` and `scanned_cards` tables |

---

## 4. DAOs — Complete Query Reference

### CardDao

```kotlin
data class TagCount(val tag: String, val cardCount: Long, val totalValue: Double = 0.0)
data class CardSearchResult(
    val id: Long, val name: String, val setCode: String, val setName: String,
    val collectorNumber: String, val foil: String, val rarity: String,
    val quantity: Int, val scryfallId: String, val tag: String
)
data class MultiCopyCard(
    val name: String, val setCode: String, val setName: String,
    val scryfallId: String, val totalQuantity: Int
)

@Insert(onConflict = REPLACE) suspend fun insert(card: CardEntity)
@Insert(onConflict = REPLACE) suspend fun insertAll(cards: List<CardEntity>)
@Query("UPDATE cards SET quantity = quantity + 1 WHERE id = :id") suspend fun incrementQuantity(id: Long)
@Query("DELETE FROM cards WHERE id = :id AND quantity = 1") suspend fun deleteIfQuantityOne(id: Long)
@Query("UPDATE cards SET quantity = quantity - 1 WHERE id = :id AND quantity > 1") suspend fun decrementQuantity(id: Long)
@Query("DELETE FROM cards WHERE id = :id") suspend fun deleteById(id: Long)
@Query("DELETE FROM cards WHERE tag = :tag") suspend fun deleteByTag(tag: String)
@Query("UPDATE cards SET tag = :newTag WHERE tag = :oldTag") suspend fun renameTag(oldTag: String, newTag: String)
@Query("DELETE FROM cards") suspend fun clear()
@Query("SELECT * FROM cards") suspend fun getAll(): List<CardEntity>
@Query("SELECT COUNT(*) FROM cards") suspend fun count(): Int

@Query("SELECT tag, COUNT(*) as cardCount, COALESCE(SUM(quantity * purchase_price), 0.0) as totalValue FROM cards GROUP BY tag UNION ALL SELECT tag, 0 as cardCount, 0.0 as totalValue FROM tags WHERE tag NOT IN (SELECT DISTINCT tag FROM cards) ORDER BY tag")
fun tagCounts(): Flow<List<TagCount>>

@Query("SELECT id, name, set_code as setCode, set_name as setName, collector_number as collectorNumber, foil, rarity, quantity, scryfall_id as scryfallId, tag FROM cards WHERE tag = :tag ORDER BY name")
fun cardsByTag(tag: String): Flow<List<CardSearchResult>>

@Query("SELECT id, name, set_code as setCode, set_name as setName, collector_number as collectorNumber, foil, rarity, quantity, scryfall_id as scryfallId, tag FROM cards WHERE name LIKE '%' || :query || '%' ORDER BY name")
fun searchByName(query: String): Flow<List<CardSearchResult>>

@Query("SELECT id, name, set_code as setCode, set_name as setName, collector_number as collectorNumber, foil, rarity, quantity, scryfall_id as scryfallId, tag FROM cards WHERE name LIKE '%' || :query || '%' AND (:color IS NULL OR tag IN (SELECT tag FROM cards WHERE tag = tag AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%' || :color || '%'))) AND (:type IS NULL OR scryfall_id IN (SELECT id FROM scryfall_cards WHERE type_line LIKE '%' || :type || '%')) AND (:rarity IS NULL OR rarity = :rarity) ORDER BY name")
fun searchAdvanced(query: String, color: String?, type: String?, rarity: String?): Flow<List<CardSearchResult>>

@Query("SELECT name, set_code as setCode, set_name as setName, scryfall_id as scryfallId, SUM(quantity) as totalQuantity FROM cards GROUP BY name, set_code HAVING SUM(quantity) > 4 ORDER BY name")
fun multiCopyCards(): Flow<List<MultiCopyCard>>
```

### ScryfallCardDao

```kotlin
data class ScryfallCardLegality(val id: String, val legalities: String)

@Insert(onConflict = REPLACE) suspend fun insertAll(cards: List<ScryfallCardEntity>)
@Query("DELETE FROM scryfall_cards") suspend fun clear()
@Query("SELECT COUNT(*) FROM scryfall_cards") suspend fun count(): Int
@Query("SELECT * FROM scryfall_cards WHERE name LIKE :query || '%' LIMIT :limit")
fun autocomplete(query: String, limit: Int = 50): Flow<List<ScryfallCardEntity>>
@Query("SELECT * FROM scryfall_cards WHERE id = :id") suspend fun byId(id: String): ScryfallCardEntity?
@Query("SELECT * FROM scryfall_cards WHERE name = :name LIMIT 1") suspend fun byName(name: String): ScryfallCardEntity?
@Query("SELECT * FROM scryfall_cards WHERE name LIKE :name || '%' LIMIT 1") suspend fun byNamePrefix(name: String): ScryfallCardEntity?
@Query("SELECT id, legalities FROM scryfall_cards WHERE id IN (:ids)") suspend fun getLegalities(ids: List<String>): List<ScryfallCardLegality>
```

### DeckDao

```kotlin
data class FormatCount(val format: String, val deckCount: Int)
data class SlotCount(val slot: String, val cnt: Int)

@Transaction
data class DeckWithCards(
    @Embedded val deck: DeckEntity,
    @Relation(parentColumn = "id", entityColumn = "deck_id")
    val cards: List<DeckCardEntity>
)

@Insert suspend fun insertDeck(deck: DeckEntity): Long
@Delete suspend fun deleteDeck(deck: DeckEntity)
@Query("SELECT * FROM decks ORDER BY name") fun allDecks(): Flow<List<DeckEntity>>
@Query("SELECT * FROM decks WHERE format = :format ORDER BY name") fun decksByFormat(format: String): Flow<List<DeckEntity>>
@Transaction @Query("SELECT * FROM decks WHERE id = :deckId") fun getDeckWithCards(deckId: Long): Flow<DeckWithCards?>
@Insert suspend fun insertCard(card: DeckCardEntity): Long
@Query("DELETE FROM deck_cards WHERE id = :cardId") suspend fun deleteCard(cardId: Long)
@Query("DELETE FROM deck_cards WHERE deck_id = :deckId") suspend fun deleteAllCards(deckId: Long)
@Query("UPDATE deck_cards SET quantity = :quantity WHERE id = :cardId") suspend fun updateCardQuantity(cardId: Long, quantity: Int)
@Query("UPDATE decks SET cover_scryfall_id = :scryfallId WHERE id = :deckId") suspend fun updateDeckCover(deckId: Long, scryfallId: String?)
@Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId") suspend fun cardCount(deckId: Long): Int
@Query("SELECT COALESCE(SUM(quantity), 0) FROM deck_cards WHERE deck_id = :deckId") fun cardCountFlow(deckId: Long): Flow<Int>
@Query("SELECT slot, COUNT(*) as cnt FROM deck_cards WHERE deck_id = :deckId GROUP BY slot") fun slotCounts(deckId: Long): Flow<List<SlotCount>>
@Query("SELECT format, COUNT(*) as deckCount FROM decks GROUP BY format HAVING COUNT(*) > 0") fun formatCounts(): Flow<List<FormatCount>>
@Query("DELETE FROM deck_cards WHERE deck_id = :deckId AND slot = :slot") suspend fun deleteCardsBySlot(deckId: Long, slot: String)
@Query("SELECT * FROM deck_cards WHERE deck_id = :deckId AND slot = :slot LIMIT 1") suspend fun findCardBySlot(deckId: Long, slot: String): DeckCardEntity?
@Query("SELECT * FROM deck_cards WHERE deck_id = :deckId") suspend fun getCardsForDeck(deckId: Long): List<DeckCardEntity>
@Query("DELETE FROM decks WHERE format = :format") suspend fun deleteDecksByFormat(format: String)
```

### TagDao

```kotlin
@Insert(onConflict = IGNORE) suspend fun insert(tag: TagEntity)
@Query("DELETE FROM tags WHERE tag = :tag") suspend fun delete(tag: String)
@Query("DELETE FROM cards WHERE tag = :tag") suspend fun deleteCardsByTag(tag: String)
@Query("UPDATE tags SET tag = :newTag WHERE tag = :oldTag") suspend fun rename(oldTag: String, newTag: String)
```

### ScanSessionDao

```kotlin
@Insert suspend fun insertSession(session: ScanSessionEntity): Long
@Insert suspend fun insertCard(card: ScannedCardEntity): Long
@Query("SELECT * FROM scanned_cards WHERE sessionId = :sessionId") fun getCardsBySession(sessionId: Long): Flow<List<ScannedCardEntity>>
@Query("SELECT * FROM scanned_cards WHERE sessionId = :sessionId") suspend fun getCardsBySessionOnce(sessionId: Long): List<ScannedCardEntity>
@Query("DELETE FROM scanned_cards WHERE id = :cardId") suspend fun deleteCard(cardId: Long)
@Query("DELETE FROM scan_sessions WHERE id = :sessionId") suspend fun deleteSession(sessionId: Long)
@Query("SELECT * FROM scan_sessions ORDER BY createdAt DESC") fun getAllSessions(): Flow<List<ScanSessionEntity>>
```

---

## 5. Data Layer

### Repositories

| Repository | Created via | Dependencies | Purpose |
|------------|-------------|--------------|---------|
| `CardRepository` | `CardRepository.create(context)` | `CardDao`, `TagDao` | Collection CRUD, CSV import, tag management |
| `DeckRepository` | `DeckRepository.create(context)` | `DeckDao`, `ScryfallCardDao` | Deck CRUD, validation, clone, color identity |
| `ScryfallRepository` | `ScryfallRepository.create(context)` | `ScryfallCardDao`, `SettingsStore` | 15-day bulk sync, autocomplete, name lookup |
| `ScanRepository` | `ScanRepository.create(context)` | `ScanSessionDao` | Scan sessions, add-to-collection |
| `SettingsStore` | `SettingsStore(context)` | DataStore | Preferences |

Each `create(context)` method obtains the Room database via `AppDatabaseProvider.get(context)`.

### AppDatabaseProvider

Singleton pattern. Returns `AppDatabase` instance. Applies all migrations (3→9) + `fallbackToDestructiveMigration(true)`.

### SettingsStore (DataStore Preferences)

| Key | Type | Default | Exposed as Flow |
|-----|------|---------|-----------------|
| `theme_id` | String | `"nord"` | `themeId` |
| `dark_mode` | Boolean | `true` | `darkMode` |
| `font_id` | String | `"roboto"` | `fontId` |
| `scryfall_updated_at` | String | `null` | `scryfallUpdatedAt` |
| `scryfall_last_check` | Long | `0L` | No |
| `onboarding_complete` | Boolean | `false` | `onboardingComplete` |
| `haptic_feedback` | Boolean | `true` | `hapticFeedback` |

---

## 6. External APIs

### 6.1 Scryfall Bulk Data

**Sync flow:** On launch, `ScryfallRepository.syncIfNeeded()` checks:
1. `scryfall_cards` table empty? → sync.
2. `scryfall_last_check` older than 15 days? → check `updated_at` from Scryfall API.
3. If changed → re-download.

**Endpoints:**
```
GET https://api.scryfall.com/bulk-data
→ { "data": [{ "type": "default_cards", "download_uri": "...jsonl.gz", "updated_at": "..." }] }

GET {download_uri}
→ gzipped JSON array or JSON Lines
```

**User-Agent:** `MtGCardTracker/1.0` (required — Scryfall returns 400 for generic okhttp UA).

**Parsing:** `ScryfallBulkImport` handles gzip transparently. Auto-detects format (JSON array `[{...}]` vs JSON Lines `{...}\n{...}`). Maps each card to `ScryfallCardEntity`. Batches inserts (500 per batch). Always deletes downloaded file after import.

**Image URLs:** Constructed deterministically from Scryfall UUID:
```
https://cards.scryfall.io/normal/front/{a}/{b}/{uuid}.jpg
https://cards.scryfall.io/large/front/{a}/{b}/{uuid}.jpg
https://cards.scryfall.io/art_crop/front/{a}/{b}/{uuid}.jpg
```
Where `{a}` = first char, `{b}` = second char of UUID.

### 6.2 Moxfield API v3

```
GET https://api2.moxfield.com/v3/decks/all/{deckId}
User-Agent: MtGCardTracker/1.0
Accept: application/json
```

Response: `MoxfieldDeckResponse { data: MoxfieldDeckData { name, mainboard, sideboard, commanders, companions } }`
Each entry: `{ quantity, card: { name, set } }`

**URL extraction:** Regex `moxfield\.com/decks/([a-zA-Z0-9]+)`

### 6.3 EDHREC Average Decks

```
GET https://json.edhrec.com/pages/average-decks/{commander-slug}.json
User-Agent: MtGCardTracker/1.0
Accept: application/json
```

Response: `EdhrecResponse { deck: { commander: [[name, qty], ...], cards: { Creature: [[name, qty], ...], ... } } }`

**URL extraction:** Regex `edhrec\.com/average-decks/([a-z0-9\-]+)`

### 6.4 Archidekt API

```
GET https://archidekt.com/api/decks/{deckId}/
User-Agent: MtGCardTracker/1.0
Accept: application/json
```

Response: `ArchidektResponse { name, cards: [{ quantity, card: { oracleCard: { name } } }] }`

**URL extraction:** Regex `archidekt\.com/decks/(\d+)`

### 6.5 mtgtop8.com (Jsoup scraping)

**User-Agent:** Mozilla/5.0 with Accept, Accept-Language, Referer headers.

**Format page:** `https://www.mtgtop8.com/format?f=XX`
- Parse `div.hover_tr:has(div.S14 a[href*=archetype])` for archetype list.
- Extract: name, thumbnail (`/metas_thumbs/`), meta %, archetype ID.

**Archetype page:** `https://www.mtgtop8.com/archetype?aid=XX`
- First deck link → event page.

**Event page:** Parse `div[id^=md].deck_line` (mainboard) and `div[id^=sb].deck_line` (sideboard).
- Format codes: ST (Standard), MO (Modern), PI (Pioneer), PAU (Pauper), LE (Legacy), VI (Vintage), PREM (Premodern), EDH (Commander).

---

## 7. Navigation

**Type:** Manual backstack (`mutableListOf<Screen>()`), NOT Navigation Compose router.

**State:**
```kotlin
var screen by remember { mutableStateOf<Screen>(Screen.Main) }
val backStack = remember { mutableListOf<Screen>() }
```

**Routes:**
```kotlin
sealed interface Screen {
    data object Main : Screen
    data object Scan : Screen
    data class ScanResults(val sessionId: Long) : Screen
    data class Cards(val tag: String) : Screen
    data class AddCard(val initialTag: String = "") : Screen
    data object ManageTags : Screen
    data object Settings : Screen
    data object Meta : Screen
    data class Decks(val format: String? = null) : Screen
    data class DeckView(val deckId: Long) : Screen
    data object UnifiedImport : Screen
}
```

**Bottom nav items:**
| Icon | Label | Route |
|------|-------|-------|
| `Octicons.Home24` | Collection | `Screen.Main` |
| `Octicons.DeviceCamera16` | Scan | `Screen.Scan` |
| `Octicons.Book24` | Decks | `Screen.Decks()` |
| `Octicons.Tag24` | Tags | `Screen.ManageTags` |
| `Octicons.Graph24` | Meta | `Screen.Meta` |
| `Octicons.Gear24` | Settings | `Screen.Settings` |

**Back handling:** Hardware back dismisses fullscreen overlays first, then pops stack. Bottom nav clears stack on selection.

---

## 8. UI Screens — Complete Behavior

### 8.1 MainScreen (Collection)

- **Top bar:** Title "Collection", search bar, filter icon.
- **Tag list:** `LazyColumn` with `TagRow` per tag showing tag name, card count, total value.
- **Multi-copy toggle:** Filter for cards with >4 copies across all tags.
- **Advanced search:** `SearchFilterChips` for color (W/U/B/R/G), type (Creature/Instant/Sorcery/...), rarity.
- **FAB:** Opens `Screen.AddCard()`.
- **Tap tag:** Navigates to `Screen.Cards(tag)`.
- **Tap card:** Opens fullscreen `FullscreenOverlay` with Scryfall image.

### 8.2 CardListScreen (Tag Detail)

- **Top bar:** Tag name, back button, export button (download icon).
- **Export button:** Opens `AlertDialog` with "Copy to Clipboard" and "Save to File".
  - Both output format: `"{quantity} {name}"` per line (e.g., "4 Golos, Tireless Pilgrim").
  - Clipboard uses `ClipboardManager.setPrimaryClip()`.
  - File writes to `Downloads/{tag}.txt`.
- **Card list:** `LazyColumn` with `SwipeToDismissBox` per card.
  - Each card: name (titleSmall), set name + rarity (bodySmall), `QuantityStepper`.
  - Swipe left: delete with red "X Delete" background.
- **Tap card:** Opens fullscreen image overlay.

### 8.3 ManualAddScreen

- **Autocomplete:** `OutlinedTextField` with debounced query → `ScryfallCardDao.autocomplete` → dropdown suggestions.
- **Fields:** Name (autocomplete), Set, Collector Number, Rarity, Scryfall ID, Quantity, Foil toggle, Condition dropdown, Tag (autocomplete from existing tags).
- **Save:** `CardRepository.addCard()`. Form resets after save for batch entry.
- **Tag suggestions:** Shows existing tags as chips below tag field.

### 8.4 SettingsScreen

- **Sections (Card wrappers):**
  1. **Appearance:** Theme dropdown (Catppuccin/Nord/Cobalt2/Shades of Purple), Font dropdown (Roboto/Comic Neue/Germania One).
  2. **Card Scan:** Haptic feedback toggle.
  3. **Backup & Restore:** Export JSON, Restore JSON, Import from 3rd-Party.
  4. **Scryfall Reference Data:** Last update date, sync status.
  5. **About:** App description.
- **Import from 3rd-Party:** Opens `ImportDialog` with tag name field + file chooser (CSV/JSON/TXT).
  - After CSV import: shows `ImportResultDialog` with imported/skipped counts.
  - If skipped > 0: "View Skipped Cards" button opens scrollable list of `SkippedRow` entries with reasons.

### 8.5 MetaScreen

- **Format chips:** FilterChips for Standard/MO/PI/PAU/LE/VI/PREM/EDH. Standard auto-loaded.
- **Deck grid:** 2-column `LazyVerticalGrid` of archetypes from mtgtop8.
- **Tap deck:** Opens dialog with decklist + "Import to Decks" button.
- **Import:** Creates deck from parsed cards, validates Commander color identity.

### 8.6 DecksScreen

- **Format grid:** 2-column grid of formats with ≥1 deck (from `formatCounts()`).
- **Long-press format:** Delete format (removes all decks with CASCADE).
- **Tap format:** Shows that format's decks.
- **Deck card:** Cover image (artCrop 5:3), name, format, card count. Long-press: clone/delete.
- **FAB:** Create deck dialog (name + format dropdown).
- **Top bar:** Import button (download icon) → `Screen.UnifiedImport`.

### 8.7 DeckViewScreen

- **Cards grouped by slot:** commander → companion → mainboard (grouped by `primaryType()`) → sideboard (flat list).
- **Card row:** Mana cost, name, quantity controls (`QuantityStepper`), rarity/set info.
- **Cover button:** Sets card as deck cover (hidden for sideboard cards).
- **Top bar:** Export (`exportDeckToTxt` + `shareDeckFile` share intent), Add card button.
- **AddCardToDeckDialog:** Scryfall autocomplete. For Commander: slot selection (mainboard/commander/companion), color identity validation against existing commander.
- **TabRow:** Decklist tab + Statistics tab (`DeckStatisticsScreen`).

### 8.8 DeckStatisticsScreen

- **Stats:** Total value, mana value distribution (bar chart), type distribution (donut chart), color distribution, rarity distribution.
- **Charts:** Custom Canvas composables (`BarChart`, `DonutChart`, `HorizontalBarChart`).

### 8.9 UnifiedImportScreen

- **Sources:** FilterChips for Moxfield, EDHREC, Archidekt, MTG Goldfish, TappedOut.
- **URL-aware sources** (Moxfield, EDHREC, Archidekt): Detect URLs via regex, fetch via API clients.
- **Paste/File sources** (MTG Goldfish, TappedOut): Text paste or file import (CSV/TXT).
- **Flow:** Parse → Preview dialog (card list with quantity/slot) → Import → Navigate to DeckView.
- **Format dropdown:** Defaults to Commander. Used for deck creation.
- **importDeckCards():** Shared function resolves cards via `ScryfallRepository.lookupByNameResilient()`, enforces Commander color identity, adds to deck. Parallel Scryfall lookups.

### 8.10 ScanCameraScreen

- **CameraX preview** with capture button.
- **Permission:** Runtime `CAMERA_EXTERNAL` request with graceful denied handling.
- **Haptic:** Short vibration on capture via `Vibrator`/`VibratorManager`.

### 8.11 ScanResultsScreen

- **Card list:** Thumbnails, names, detected metadata.
- **Actions:** Delete individual cards, "Add to Collection" with tag picker.
- **Batch add:** `ScanRepository.addToCollection()`.

### 8.12 OnboardingScreen

- **3 pages** shown on first launch only.
- **Completion:** `SettingsStore.setOnboardingComplete()`.

---

## 9. Reusable Components

| Component | File | Purpose |
|-----------|------|---------|
| `ScryfallAsyncImage` | `AsyncImage.kt` | Coil image loader with custom User-Agent |
| `FullscreenOverlay` | `FullscreenOverlay.kt` | Fullscreen card image dialog (uses `ScryfallImage.large()`) |
| `QuantityStepper` | `QuantityStepper.kt` | +/- quantity controls |
| `LoadingBox`, `ErrorBox`, `EmptyBox` | `LoadingBox.kt` | Loading/error/empty states |
| `CameraPreview` | `CameraPreview.kt` | CameraX `PreviewView` composable |
| `BarChart`, `DonutChart`, `HorizontalBarChart` | `DeckStatsCharts.kt` | Canvas chart composables |
| `SearchFilterChips` | `SearchFilterChips.kt` | Color/type/rarity filter chips |
| `FilledBottomNavigationBar` | `BottomNavigationBar.kt` | Icon-only bottom nav with filled selected background |

---

## 10. Domain Logic

### primaryType()

Extracts the primary type from a `type_line` string. Filters out supertypes (Legendary, Snow, World, Basic). Priority: Land > Creature > Planeswalker > Artifact > Battle > non-Tribal Enchantment > other.

### FormatValidator

```kotlin
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

fun interface FormatRule {
    fun validate(format: String, cards: List<DeckCardEntity>, legalitiesMap: Map<String, String>): List<String>
}
```

**Registered rules:**
| Format | Rules |
|--------|-------|
| Commander, Brawl | CommanderCountRule + ColorIdentityRule + LegalityRule |
| All others | LegalityRule only |

- **CommanderCountRule:** Exactly 1 card with `slot == "commander"`.
- **ColorIdentityRule:** All non-commander/companion cards must have color identity ⊆ commander's.
- **LegalityRule:** Each card's `legalities` JSON must contain `"legal"` or `"restricted"` for the deck's format.

### Card Conditions

`NM` (Near Mint), `LP` (Lightly Played), `MP` (Moderately Played), `HP` (Heavily Played), `DM` (Damaged).

### Color Identity

Stored as comma-separated sorted string (e.g., `"W,U,B"`). Parsed from Scryfall's `color_identity` JSON array. Denormalized onto both `scryfall_cards` and `deck_cards` for fast validation without joins.

---

## 11. Constants

```kotlin
ALL_FORMATS = ["Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage", "Premodern", "Commander"]
DECK_FORMATS = ["All"] + ALL_FORMATS
FORMATS = ["All", "Standard", "Modern", "Pioneer", "Pauper", "Legacy", "Vintage", "Premodern", "Commander"]
SUPERTYPES = listOf("Legendary", "Snow", "World", "Basic")
```

---

## 12. Theming

**4 themes** (each in separate file with `lightColorScheme()` + `darkColorScheme()`):
| ID | Label |
|----|-------|
| `catppuccin` | Catppuccin |
| `nord` | Nord (default, dark) |
| `shades_of_purple` | Shades of Purple |
| `cobalt2` | Cobalt2 |

**3 fonts:**
| ID | Label | Source |
|----|-------|--------|
| `roboto` | Roboto | System default |
| `comic_neue` | Comic Neue | `R.font.comic_neue` |
| `germania_one` | Germania One | `R.font.germania_one` |

Typography applies the selected font family to all 15 Material3 text styles.

---

## 13. MainActivity Wiring

```kotlin
val repository = CardRepository.create(this)
val deckRepository = DeckRepository.create(this)
val scryfall = ScryfallRepository.create(this)
val scanRepository = ScanRepository.create(this)
val settings = SettingsStore(this)
val mainViewModel = MainViewModel(repository, settings)
```

On launch: `scryfall.syncIfNeeded()` runs on `Dispatchers.IO`, shows Toast with status.

`setContent` observes `themeId`, `darkMode`, `fontId` from `MainViewModel` → applies `AppTheme` → renders `AppNavigation` (all repositories + settings passed as constructor params).

---

## 14. Build & Run

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
./gradlew test             # Unit tests
```

---

## 15. Git Conventions

- Commit after every discrete change.
- Never add `Co-Authored-By` trailers.
- Follow conventional commits format: `type(scope): description`.
