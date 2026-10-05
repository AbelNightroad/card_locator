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
| **Gradle** | 9.7.1 |
| **JDK** | 17 (system JDK, no toolchain pin) |

### Feature Status

| Feature | Status |
|---------|--------|
| Core collection / decks / tags / meta | **Stable** — shipped |
| Card scan (CameraX + ML Kit OCR) | **Shipped** — see `PLAN.md` (10/10 phases) |
| LLM + vector deck analysis | **On hold — not essential.** Design only in `LLM_DECK_ANALYSIS_PLAN.md`; blocked on a trained model. No deps or code exist. |

---

## 2. Build Configuration

### Plugins

| Plugin | Version |
|--------|---------|
| `com.android.application` | 9.4.0 |
| `org.jetbrains.kotlin.plugin.compose` | 2.4.20 |
| `org.jetbrains.kotlin.plugin.serialization` | 2.4.20 |
| `com.google.devtools.ksp` | 2.3.12 |

AGP 9 built-in Kotlin compiles all Kotlin sources; `org.jetbrains.kotlin.android`
is intentionally NOT applied (it is incompatible with AGP 9's new DSL) and
`jvmTarget` defaults to `compileOptions.targetCompatibility` (17). Gradle
wrapper: 9.7.1. Builds on any JDK ≥ 17, verified on system JDK 26.

### Dependencies (exact versions)

| Group:Artifact | Version |
|----------------|---------|
| `androidx.compose:compose-bom` | 2025.06.00 |
| `androidx.compose.material3:material3` | BOM |
| `androidx.compose.material:material-icons-extended` | BOM |
| `androidx.activity:activity-compose` | 1.10.1 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.9.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.9.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.0 |
| `androidx.navigation:navigation-compose` | 2.9.0 |
| `androidx.datastore:datastore-preferences` | 1.1.4 |
| `androidx.room:room-runtime` | 2.7.0 |
| `androidx.room:room-ktx` | 2.7.0 |
| `androidx.room:room-compiler` | 2.7.0 (KSP) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.2 |
| `io.coil-kt:coil-compose` | 2.7.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 |
| `org.jsoup:jsoup` | 1.18.1 |
| `com.github.joaocsousa:font-awesome` | 2.0.0 (Font Awesome 7.3.x) |
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

### App Version

 `versionCode = 6`, `versionName = "1.3.2"` with `buildFeatures.buildConfig = true`;
`BuildConfig.VERSION_NAME` is shown in Settings → About. Versioning policy: PATCH
for fixes, MINOR for features, MAJOR for breaking changes (see §15).

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
@Query("UPDATE cards SET quantity = quantity + :delta WHERE id = :id") suspend fun addQuantity(id: Long, delta: Int)
@Query("SELECT * FROM cards WHERE tag = :tag AND name = :name COLLATE NOCASE AND set_code = :setCode COLLATE NOCASE AND collector_number = :collectorNumber AND foil = :foil LIMIT 25") suspend fun findByDuplicateKey(tag: String, name: String, setCode: String, collectorNumber: String, foil: String): List<CardEntity>
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

@Query("SELECT id, name, set_code, set_name, collector_number, foil, rarity, quantity, scryfall_id, tag FROM cards WHERE name LIKE '%' || :query || '%' COLLATE NOCASE AND (:colors IS NULL OR (instr(:colors, 'W') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%W%')) OR (instr(:colors, 'U') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%U%')) OR (instr(:colors, 'B') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%B%')) OR (instr(:colors, 'R') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%R%')) OR (instr(:colors, 'G') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity LIKE '%G%')) OR (instr(:colors, 'C') > 0 AND scryfall_id IN (SELECT id FROM scryfall_cards WHERE color_identity = ''))) AND (:type IS NULL OR scryfall_id IN (SELECT id FROM scryfall_cards WHERE type_line LIKE '%' || :type || '%')) AND (:rarity IS NULL OR rarity = :rarity) ORDER BY name COLLATE NOCASE ASC")
fun searchAdvanced(query: String, colors: String?, type: String?, rarity: String?): Flow<List<CardSearchResult>>

@Query("SELECT name, set_code as setCode, set_name as setName, scryfall_id as scryfallId, SUM(quantity) as totalQuantity FROM cards GROUP BY name, set_code HAVING SUM(quantity) > 4 ORDER BY name")
fun multiCopyCards(): Flow<List<MultiCopyCard>>
```

- `searchAdvanced.colors` is the selected color codes concatenated (`"WU"`, `"WC"`, `null` =
  no color filter). Each of the six static clauses is skipped unless its code is present, so a
  card matches **any** selected color (OR); `C` matches rows whose
  `scryfall_cards.color_identity` is empty. Rows without a `scryfall_cards` row are excluded
  while a color filter is active.
- `CardRepository.searchAdvanced()` encodes the UI's `Set<String>` into `colors`
  (`joinToString("")`, empty set → `null`).

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
@Query("SELECT * FROM scryfall_cards WHERE set_code = :setCode COLLATE NOCASE AND collector_number = :collectorNumber LIMIT 1") suspend fun bySetAndCollector(setCode: String, collectorNumber: String): ScryfallCardEntity?
@Query("SELECT * FROM scryfall_cards WHERE name = :name COLLATE NOCASE AND set_code = :setCode COLLATE NOCASE LIMIT 1") suspend fun byNameAndSet(name: String, setCode: String): ScryfallCardEntity?
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
| `CardRepository` | `CardRepository.create(context)` | `CardDao`, `TagDao` | Collection CRUD, CSV import, tag management, duplicate-merging import (`insertMergingDuplicates`) |
| `DeckRepository` | `DeckRepository.create(context)` | `DeckDao`, `ScryfallCardDao` | Deck CRUD, validation, clone, color identity |
| `ScryfallRepository` | `ScryfallRepository.create(context)` | `ScryfallCardDao`, `SettingsStore` | 15-day bulk sync, autocomplete, name lookup, set+collector lookup |
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

### Import Formats

| Parser | File | Used by | Notes |
|--------|------|---------|-------|
| `ThirdPartyImport` | `data/ThirdPartyImport.kt` | Settings → Import from 3rd-Party; Tag detail import | Detects format: 16-column CSV → `CsvImport`, else `QtyListImport`; delegates to the same enrich + `insertMergingDuplicates` flow; reports progress via `onProgress(processed, total)` |
| `CsvImport` | `data/CsvImport.kt` | via `ThirdPartyImport` | ManaBox 16-column CSV → `CardEntity` rows + skipped rows |
| `CollectionCsv` | `data/CollectionCsv.kt` | Settings export/restore | RFC4180 CSV encode/parse of the whole collection (header validation; replaces the deleted JSON `BackupStore`) |
| `QtyListImport` | `data/QtyListImport.kt` | Tag detail → Import into Tag; Settings (non-CSV) | `qty Name (SET) collector [*finish*]` line format |

`QtyListImport` keeps name/set/collector verbatim, maps the finish marker to
`foil` (`normal` / `foil` / `etched` / `etched foil`; markers `F`, `E`, `FE`/`EF`),
merges duplicate rows (same name + set + collector + finish → quantities summed)
and reports malformed rows as skipped with a reason. `CardListViewModel.importIntoTag()`
enriches each row from `scryfall_cards` (set name, rarity, image id — exact
set+collector first, then same-set name fallback); rows with no reference match
are still imported and counted as "imported without full details".
`CardRepository.insertMergingDuplicates()` folds rows into existing identical
cards in the same tag (any differing field keeps the cards separate).

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

Response (decoded at the **response root**, no `data` wrapper):
`MoxfieldDeckData { name, format, boards: { mainboard, sideboard, commanders, companions } }`
where each board is `{ cards: { <entryId>: { quantity, card: { name, set } } } }`

**URL extraction:** Regex `moxfield\.com/decks/([a-zA-Z0-9_-]+)`

### 6.3 EDHREC Average Decks + Deck Previews

```
GET https://json.edhrec.com/pages/average-decks/{commander-slug}.json   (JSON)
GET https://edhrec.com/deckpreview/{deckId}                             (HTML)
User-Agent: MtGCardTracker/1.0
```

Average-decks response: `EdhrecResponse { deck: { commander_v2: [[name, qty], ...], cards: { Creature: [[name, qty], ...], ... } } }`
(the `commander` field is a plain string list — the pairs live in `commander_v2`, mapped via `@SerialName`)

Deck preview: HTML embeds `<script id="__NEXT_DATA__">` → `props.pageProps.data.deck` (same shape), extracted by `EdhrecApiClient.parseNextDataDeck()`.

**URL extraction:** `edhrec\.com/average-decks/([a-z0-9\-]+)` or `edhrec\.com/deckpreview/([a-zA-Z0-9\-_]+)`

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

**Format page:** `https://mtgtop8.com/format?f=XX` (Commander → `https://mtgtop8.com/format?f=cEDH&meta=300`)
- Parse `div.hover_tr:has(div.S14 a[href*=archetype])` for archetype list.
- Extract: name, thumbnail (`/metas_thumbs/`), meta %, archetype ID.
- **XHR formats (cEDH / EDH):** the static page ships an empty deck container
  `div[id$=_decks]` (e.g. `#cEDH_decks`) and fills it from its own script via
  `RequestContent("cEDH_decks?f="+f+"&show="+show+"&cid="+color_id+"&meta="+meta+...)`.
  When the static page yields no archetypes, `MetaDecklistLoader.xhrDecksUrl(pageHtml, code)`
  reads the endpoint id and `meta=NNN;` from the script and POSTs to
  `https://mtgtop8.com/<endpoint>?f=<code>&show=pop&cid=&meta=<meta>&gamerid1=&gamerid2=&cEDH_cp=1`;
  the fragment is parsed with the same selector. Still empty → `IOException("No archetypes found for $format")`.

**Archetype page:** `https://mtgtop8.com/archetype?a=XX&meta=NN&f=XX`
- First deck link (`tr.hover_tr a[href*='/event?']`) → event page.

**Event page:** `MetaDecklistLoader.parseDecklist()` walks `div.O14` section headers together with `div[id^=md].deck_line` / `div[id^=sb].deck_line`: cards under a `COMMANDER` header → `slot = "commander"` (mtgtop8 marks them with `sb` ids), other `sb` ids → `sideboard`, `md` ids → `mainboard`. The list is stably ordered commander → mainboard → sideboard.
- Format codes: ST (Standard), MO (Modern), PI (Pioneer), PAU (Pauper), LE (Legacy), VI (Vintage), PREM (Premodern), cEDH (Commander).

### 6.6 MTG Goldfish (HTML scraping)

```
GET https://www.mtggoldfish.com/deck/{deckId}
User-Agent: MtGCardTracker/1.0
Accept: text/html,application/xhtml+xml
```

- **URL extraction:** Regex `mtggoldfish\.com/deck/(\d+)`
- **Decklist:** `<textarea id="deck_input_deck">` content (HTML-unescaped) → `UniversalDecklistParser`.
- **Cloudflare:** HTTP 403 or a "Just a moment" / `challenge-platform` body → error
  "Goldfish blocked the request — copy the deck list and paste it instead" (never silent).

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
| `FontAwesomeIcons.Solid.House` | Collection | `Screen.Main` |
| `FontAwesomeIcons.Solid.Book` | Decks | `Screen.Decks()` |
| `FontAwesomeIcons.Solid.ChartBar` | Meta | `Screen.Meta` |
| `FontAwesomeIcons.Solid.Camera` | Scan | `Screen.Scan` |
| `FontAwesomeIcons.Solid.Gear` | Settings | `Screen.Settings` |

All icons are Font Awesome 7 Solid (`compose.icons.FontAwesomeIcons.Solid.*`);
the Octicons library was removed. **Gotcha:** FA vectors are built with
`defaultWidth/Height = 512.dp`, so `Icon()` without an explicit
`Modifier.size(...)` renders at intrinsic size (huge). Every call site must
pass a size — 24.dp everywhere except the bottom bar (22.dp via `iconSize`)
and onboarding (56.dp).

`Screen.ManageTags` is no longer a bottom-nav item: it is opened from
Settings → Tags → "Manage Tags" (pushed onto the stack, back returns to
Settings). On screens that are not in `NAV_ITEMS` the bar renders with no
item selected (`indexOfFirst` → -1).

**Back handling:** Hardware back dismisses fullscreen overlays first, then pops stack. Bottom nav selection keeps a linear history: the current screen is pushed, and if the target screen already exists in history the stack truncates up to it (no back cycles), so back from a tab destination pops to the previous tab instead of exiting the app.

---

## 8. UI Screens — Complete Behavior

### 8.1 MainScreen (Collection)

- **Top bar:** Title "Card Tracker", magnifier icon (advanced filters), light/dark toggle. The magnifier tints to `primary` while any filter is active.
- **Tag list:** `LazyColumn` with `TagRow` per tag showing tag name, card count, total value.
- **Multi-copy toggle:** Filter for cards with >4 copies, grouped by card **name** across all sets/tags/finishes (`GROUP BY name`, representative printing via `MIN(set_code)`).
- **Search field:** Live name filter (leading magnifier, clear "✕" while non-blank). Results show when the query is non-blank **or** an advanced filter is set; filter-only queries use `searchAdvanced("")` (name `LIKE '%%'` → filter the whole collection).
- **Advanced filters:** Tapping the top-bar magnifier opens `AdvancedFilterDialog` (Color W/U/B/R/G/**Colorless**, Type, Rarity chip groups + "Clear all"). Color chips are multi-select (`MainViewModel.toggleColorFilter` toggles a code in a `Set<String>`, any selected color matches); Type/Rarity stay single-select via `setTypeFilter/setRarityFilter`. Chips apply immediately; `clearFilters()` resets only filters, `clearSearch()` resets query + filters.
- **FAB speed dial:** Tap unfolds two options — **Card** (`PenToSquare`) → `Screen.AddCard()`, **Tag** (`Tag`) → shared `CreateTagDialog`; tapping again or choosing an option collapses it (main FAB icon rotates 45°).
- **Tap tag:** Navigates to `Screen.Cards(tag)`.
- **Tap card:** Opens fullscreen `FullscreenOverlay` with Scryfall image.

### 8.2 CardListScreen (Tag Detail)

- **Top bar:** Tag name, back button, import button (upload icon), export button (download icon).
- **Import button:** Opens `AlertDialog` "Import into \"<tag>\"" with a file chooser (`OpenDocument`, TXT/CSV) and no tag field — cards always land in the open tag.
  - Parses with `QtyListImport`, enriches via `CardListViewModel.importIntoTag()`, inserts with `CardRepository.insertMergingDuplicates()`.
  - While importing the dialog stays open with a spinner and a live `x / y cards` counter (`ThirdPartyImport.import(onProgress)`); file read runs on `Dispatchers.IO`. Success → close + shared `ImportResultDialog`; failure → close + Toast.
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
  3. **Tags:** "Manage Tags" button → `Screen.ManageTags` (pushed screen; back returns here).
  4. **Backup & Restore:** Export CSV, Restore CSV, Import from 3rd-Party.
  5. **Scryfall Reference Data:** Last update date, sync status.
  6. **About:** App description, `Version ${BuildConfig.VERSION_NAME}`, "Crash logs" button.
- **Import from 3rd-Party:** Opens `ImportDialog` with tag name field + file chooser (CSV/TXT).
  - Format detection happens in `ThirdPartyImport.import()` (ManaBox CSV → `CsvImport`, anything else → `QtyListImport`), so pasted txt lists no longer fail with CSV column errors.
  - The dialog stays open with a spinner while importing (plus an `x / y cards` counter for txt/CSV rows); closes on completion.
  - After import: shows the shared `ImportResultDialog` with imported/skipped counts.
  - If skipped > 0: "View Skipped Cards" button opens scrollable list of skipped entries with reasons.
- **Crash logs dialog:** Shows `filesDir/crash.log` (written by `CrashLog`), with Copy (clipboard + Toast), Clear, and Close actions; empty state shows "No crashes recorded yet."

### 8.5 MetaScreen

- **Format chips:** FilterChips for Standard/MO/PI/PAU/LE/VI/PREM/Commander. Standard auto-loaded; Commander loads the cEDH metagame (`MetaDecklistLoader` XHR fallback — see §6.5).
- **Deck grid:** 2-column `LazyVerticalGrid` of archetypes from mtgtop8.
- **Tap deck:** Opens dialog with decklist + "Import to Decks" button.
- **Import:** Creates deck from parsed cards, validates Commander color identity (partner pair = 2 commanders, identity check uses their union).

### 8.6 DecksScreen

- **Format grid:** 2-column grid of formats with ≥1 deck (from `formatCounts()`); each format card gets a runtime-generated color: `DynamicColorGenerator.generateComplementaryColors(colorScheme.primary, count)` hue-shifts the active theme's primary around the color wheel (theme saturation, lightness clamped to 0.55–0.68 so cards stay mid-tone and readable in light and dark), `remember(baseColor, formatCounts.size)` keeps it stable across recompositions, `index % size` picks the color and `DynamicColorGenerator.onColor()` picks dark/light text by WCAG luminance. Never the theme's `background`/`surface`.
- **Long-press format:** Delete format (removes all decks with CASCADE).
- **Tap format:** Shows that format's decks.
- **Deck card:** Cover image (artCrop, `ContentScale.Crop` into a fixed 5:3 box so it always fills the card edges with no letterboxing), then a 8dp-padded text block with name, format, card count. Long-press: clone/rename/delete (rename = AlertDialog with `OutlinedTextField` prefilled, `DecksViewModel.renameDeck` → `DeckDao.renameDeck`).
- **FAB:** Create deck dialog (name + format dropdown).
- **Top bar:** Import button (download icon) → `Screen.UnifiedImport`.

### 8.7 DeckViewScreen

- **Cards grouped by slot:** commander → companion → mainboard (grouped by `primaryType()` in fixed order: Creature → Instant → Sorcery → Artifact → Enchantment → Planeswalker → Battle → Land → Other, unknown types last) → sideboard (flat list, always last).
- **Card row:** Mana cost, name, quantity controls (`QuantityStepper`), rarity/set info.
- **Cover button:** Sets card as deck cover (hidden for sideboard cards).
- **Top bar:** Export (`exportDeckToTxt` + `shareDeckFile` share intent; `FileProvider` authority `${applicationId}.fileprovider` with `res/xml/file_paths.xml` → `<cache-path name="decks">`; failures surface as a Toast instead of a crash), Add card button.
- **AddCardToDeckDialog:** Scryfall autocomplete. For Commander: slot selection (mainboard/commander/companion), color identity validation against existing commander.
- **TabRow + HorizontalPager:** Decklist tab + Statistics tab (`DeckStatisticsScreen`). Tab taps call `scope.launch { pagerState.animateScrollToPage(...) }`; the selected index is `pagerState.currentPage.coerceIn(0, 1)` (single source of truth, no sync effects).

### 8.8 DeckStatisticsScreen

- **Stats:** Total value, mana value distribution (bar chart, buckets 0–8 + `9+` for everything higher), type distribution (colored pie chart + legend with color swatches), color distribution (slim donut, no center total), rarity distribution.
- **Computation:** Pure top-level `computeDeckStats(cards)` in `ui/DeckStatistics.kt` (unit-tested; guards divide-by-zero, non-finite totals, cmc bucketed 0–8 with `9+` overflow).
- **Charts:** Custom Canvas composables (`BarChart`, `DonutChart`, `PieChartWithLegend`) in `ui/components/DeckStatsCharts.kt`; paint colors via `Color.toArgb()` (never `hashCode()`), all dimensions coerced ≥ 0.

### 8.9 UnifiedImportScreen

- **Sources:** FilterChips for Moxfield, EDHREC, Archidekt, MTG Goldfish, TappedOut — chips select the parser only for **pasted text/files**; URLs ignore them.
- **URL auto-detection** (`ImportSource.detect(url)` in `UnifiedImportViewModel.kt`): host-suffix match (tolerant of scheme-less input, `www.`, subdomains, `#fragment`/query) → picks the parser; the field shows a "Detected: <site>" label while a recognized URL is typed; unknown hosts error "Unsupported deck URL — paste the decklist text instead"; TappedOut URLs are detected but never fetched (probed Cloudflare 403) → explicit copy-paste guidance.
- **URL-aware sources** (Moxfield, EDHREC incl. deck previews, Archidekt, MTG Goldfish): fetch via API/scrape clients; invalid URLs report the expected format, HTTP failures report status + "may be private/deleted".
- **Paste/File sources** (TappedOut; also any URL-aware source): Text paste or file import (CSV/TXT).
- **Flow:** Parse → Preview dialog (card list with quantity/slot) → Import → Navigate to DeckView.
- **Format dropdown:** Defaults to Commander. Used for deck creation.
- **importDeckCards():** Shared function resolves cards via `ScryfallRepository.lookupByNameResilient()` (parallel lookups) and adds them to the deck. Commander: a `commander` slot declared by the source wins; otherwise card 0 becomes the commander. The identity filter skips cards outside the **union** of every commander-slot card's identity (`DeckRepository.mergeColorIdentities`), so partner pairs keep the whole list.

### 8.10 ScanCameraScreen

- **CameraX preview** with capture button; `CameraPreview` binds **preview + `ImageCapture` once** in a single `LaunchedEffect` (extras passed as `extraUseCases` — no competing rebinds, capture works first try).
- **Permission:** Runtime `CAMERA` request with graceful denied handling; `VIBRATE` permission declared in the manifest.
- **Haptic:** Short vibration on capture via `Vibrator`/`VibratorManager`, wrapped in try/catch (OEM-defensive).

### 8.11 ScanResultsScreen

- **Card list:** Thumbnails, names, detected metadata.
- **Actions:** Delete individual cards, "Add to Collection" with tag picker.
- **Batch add:** `ScanRepository.addToCollection()`.

### 8.12 OnboardingScreen

- **4 pages** shown on first launch only: Scan, Stats, Decks, Tags (Tags page explains that a tag = a physical storage location).
- **Completion:** `SettingsStore.setOnboardingComplete()`.

---

## 9. Reusable Components

| Component | File | Purpose |
|-----------|------|---------|
| `ScryfallAsyncImage` | `AsyncImage.kt` | Coil image loader with custom User-Agent |
| `FullscreenOverlay` | `FullscreenOverlay.kt` | Fullscreen card image dialog (uses `ScryfallImage.large()`) |
| `QuantityStepper` | `QuantityStepper.kt` | +/- quantity controls |
| `CreateTagDialog` | `CreateTagDialog.kt` | Shared "New Tag" dialog (name field + Random button) used by ManageTagsScreen and the Home FAB speed dial |
| `LoadingBox`, `ErrorBox`, `EmptyBox` | `LoadingBox.kt` | Loading/error/empty states |
| `CameraPreview` | `CameraPreview.kt` | CameraX `PreviewView` composable |
| `BarChart`, `DonutChart`, `PieChartWithLegend` | `DeckStatsCharts.kt` | Canvas chart composables |
| `AdvancedFilterDialog` | `AdvancedFilterDialog.kt` | Home advanced filter dialog (color/type/rarity chips) |
| `FilledBottomNavigationBar` | `BottomNavigationBar.kt` | Icon-only bottom nav with filled selected background |
| `ImportResultDialog`, `SkippedRowsDialog` | `ImportResultDialogs.kt` | Shared import result (imported/skipped/unresolved) + skipped-row list |

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

- **CommanderCountRule:** 1–2 cards with `slot == "commander"` (partner pair allowed).
- **ColorIdentityRule:** All non-commander/companion cards must have color identity ⊆ union of the commanders' identities (`DeckRepository.mergeColorIdentities`).
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

**Dynamic accent colors:** `ui/theme/DynamicColorGenerator.kt` derives off-theme
accent colors at runtime from a theme color (HSL hue rotation, theme saturation,
lightness clamped to 0.55–0.68) plus `onColor()` WCAG-luminance text selection.
Pure Kotlin (no `android.graphics`, so JVM-unit-testable); used by the Decks
format grid and covered by `DynamicColorGeneratorTest`.

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

On launch: `CrashLog.install(this)` chains an uncaught-exception handler that
persists the stack trace to `filesDir/crash.log` (trimmed to 64K, newest first)
before delegating to the system handler. Then `scryfall.syncIfNeeded()` runs on
`Dispatchers.IO`, shows Toast with status.

`setContent` observes `themeId`, `darkMode`, `fontId` from `MainViewModel` → applies `AppTheme` → renders `AppNavigation` (all repositories + settings passed as constructor params).

---

## 14. Build & Run

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
./gradlew test             # Unit tests
```

Requires JDK 17+ (AGP 9 minimum); verified on system JDK 26 with Gradle 9.7.1 —
no JDK pin needed since the AGP 9 upgrade.

---

## 15. Git Conventions

- Commit after every discrete change.
- Never add `Co-Authored-By` trailers.
- Follow conventional commits format: `type(scope): description`.
- Versioning: bump `versionName`/`versionCode` in `app/build.gradle.kts` per release — PATCH for fixes, MINOR for features, MAJOR for breaking changes.
