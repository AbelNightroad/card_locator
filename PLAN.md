# Card Scan Feature Plan

## Overview

Add a card scanning feature that uses the device camera to photograph MTG cards, performs OCR to extract the card name, looks up the card in the local Scryfall reference table, and presents results in a persistent temp list. Users can review, delete, and add confirmed cards to their collection.

## Architecture

### Flow

```
Camera Preview -> Capture Image -> ML Kit OCR -> Extract Card Name
    -> Local Scryfall DB lookup -> Show Match -> Add to Scan List
    -> User Reviews List -> Select Tag -> Add to Collection
```

### New Dependencies

| Library | Purpose |
|---|---|
| `androidx.camera:camera-core` | Camera abstraction |
| `androidx.camera:camera-camera2` | Camera2 implementation |
| `androidx.camera:camera-lifecycle` | Lifecycle-aware camera |
| `androidx.camera:camera-view` | `PreviewView` composable |
| `com.google.mlkit:text-recognition` | OCR (Latin script) |
| `com.google.mlkit:text-recognition-chinese` | OCR (CJK characters) |
| `com.google.mlkit:text-recognition-japanese` | OCR (Japanese hiragana/katakana) |
| `com.google.mlkit:text-recognition-korean` | OCR (Korean hangul) |
| `com.google.android.gms:play-services-mlkit-text-recognition` | Play Services wrapper |
| `androidx.test.ext:junit` | AndroidX test JUnit4 extensions |
| `androidx.test:runner` | AndroidX test runner |
| `androidx.test:rules` | AndroidX test rules (ActivityScenarioRule) |
| `androidx.test:core` | AndroidX test core (InputImage, etc.) |

### New DB Tables (migration 8 -> 9)

**`scan_sessions`**
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | |
| `created_at` | TEXT | ISO timestamp |

**`scanned_cards`**
| Column | Type | Notes |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | |
| `session_id` | INTEGER FK -> scan_sessions | CASCADE delete |
| `name` | TEXT | Card name |
| `set_code` | TEXT | e.g. "mh2" |
| `set_name` | TEXT | e.g. "Modern Horizons 2" |
| `collector_number` | TEXT | e.g. "242" |
| `rarity` | TEXT | |
| `mana_cost` | TEXT | |
| `type_line` | TEXT | |
| `oracle_text` | TEXT | |
| `color_identity` | TEXT | |
| `scryfall_id` | TEXT | For image display |
| `price_usd` | REAL | |
| `language` | TEXT | Detected language |

## Files to Create

### Data Layer

| File | Responsibility |
|---|---|
| `data/ScanRepository.kt` | CRUD for scan sessions + scanned cards |
| `db/ScanSessionEntity.kt` | `@Entity` for `scan_sessions` |
| `db/ScannedCardEntity.kt` | `@Entity` for `scanned_cards` |
| `db/ScanSessionDao.kt` | `@Dao` for scan operations |
| `data/TextRecognitionProcessor.kt` | ML Kit OCR wrapper + card name extraction |

### UI Layer

| File | Responsibility |
|---|---|
| `ui/ScanCameraScreen.kt` | CameraX preview + capture button |
| `ui/ScanResultsScreen.kt` | Scanned card list with delete/confirm actions |
| `ui/ScanViewModel.kt` | State management for scan flow |
| `ui/components/CameraPreview.kt` | Reusable CameraX `PreviewView` composable |

### Modified Files

| File | Change |
|---|---|
| `app/build.gradle.kts` | Add CameraX + ML Kit dependencies |
| `AndroidManifest.xml` | Add `android.permission.CAMERA` |
| `db/AppDatabase.kt` | Add new entities, DAO, migration 8->9 |
| `ui/navigation/Screen.kt` | Add `ScanCamera` and `ScanResults` screens |
| `ui/Screens.kt` | Add navigation cases for scan screens |
| `ui/MainScreen.kt` | Add scan entry point (FAB or menu item) |
| `ui/MainViewModel.kt` | Expose scan entry navigation |

## Task Checklist

### Phase 1: Dependencies & Permissions

- [x] Add CameraX dependencies to `app/build.gradle.kts`
- [x] Add ML Kit Text Recognition dependencies to `app/build.gradle.kts`
- [x] Add `android.permission.CAMERA` to `AndroidManifest.xml`
- [x] Add `uses-feature` declaration for camera hardware

### Phase 2: Database Layer

- [x] Create `ScanSessionEntity.kt` with `@Entity` annotation
- [x] Create `ScannedCardEntity.kt` with `@Entity` annotation and FK to scan_sessions
- [x] Create `ScanSessionDao.kt` with queries: insert session, insert card, get cards by session, delete card, delete session, get all sessions
- [x] Update `AppDatabase.kt`: add entities, DAO, bump version to 9
- [x] Write `MIGRATION_8_9` SQL statements

### Phase 3: OCR Processor

- [x] Create `TextRecognitionProcessor.kt`
- [x] Implement ML Kit text recognition setup (Latin + Chinese models)
- [x] Implement `processImage(InputImage): String` that returns recognized text
- [x] Implement `extractCardName(text: String): String` heuristic to extract card name from OCR output (first non-empty line, trimmed)
- [x] Handle Chinese/vertical text detection for CJK cards

### Phase 4: Scan Repository

- [x] Create `ScanRepository.kt` with `companion object { fun create(context) }`
- [x] Implement `createSession(): Long` (returns session ID)
- [x] Implement `addScannedCard(sessionId, ScannedCardEntity)`
- [x] Implement `getScannedCards(sessionId): Flow<List<ScannedCardEntity>>`
- [x] Implement `deleteScannedCard(cardId)`
- [x] Implement `deleteSession(sessionId)`
- [x] Implement `addToCollection(sessionId, tag, cardRepository)` — batch insert confirmed cards into collection

### Phase 5: Camera UI

- [x] Create `CameraPreview.kt` composable wrapping `PreviewView`
- [x] Create `ScanCameraScreen.kt` with camera preview + capture button
- [x] Implement camera permission request flow (rememberLauncherForActivityResult)
- [x] Handle permission denied state with explanation text
- [x] Implement image capture with CameraX `ImageCapture`
- [x] After capture, pass image to OCR processor and navigate to results

### Phase 6: Scan Results UI

- [x] Create `ScanResultsScreen.kt` showing list of scanned cards
- [x] Display card name, set, rarity, and Scryfall image thumbnail for each item
- [x] Implement swipe-to-delete or delete button per card
- [x] Implement "Add to Collection" button that opens tag picker
- [x] Show empty state when no cards in session
- [x] Show loading state during OCR + Scryfall lookup

### Phase 7: ViewModel

- [x] Create `ScanViewModel.kt` with state for: camera permission, captured image, OCR results, scanned cards, selected session
- [x] Expose `StateFlow` for all UI state
- [x] Handle OCR processing in `viewModelScope` with `Dispatchers.Default`
- [x] Handle Scryfall lookup using `ScryfallRepository.lookupByNameResilient()`
- [x] Handle collection import via `CardRepository`

### Phase 8: Navigation Integration

- [x] Add `Screen.Scan` to `Screen.kt`
- [x] Add `DeviceCamera16` icon to `NAV_ITEMS` (second position, after Collection)
- [x] Add navigation case in `Screens.kt` `when` block
- [x] Bottom nav now shows: Collection, Scan, Decks, Tags, Meta, Settings

### Phase 9: Polish & Edge Cases

- [x] Handle OCR failure gracefully (show "Could not detect card name" toast)
- [x] Handle Scryfall no-match (show "Card not found" error)
- [x] Add haptic feedback on successful capture
- [x] Improve permission denied state with explanation text
- [x] Test with non-English cards (Portuguese, Japanese, Chinese in unit tests)

### Phase 10: Unit Tests (JVM, no emulator)

- [x] Create `TextRecognitionProcessorTest.kt` — 16 tests for `extractCardName()` heuristic
- [x] Create `ScryfallApiLookupTest.kt` — 8 tests hitting Scryfall API for card lookup verification
- [x] Refactor `extractCardName()` to companion object for JVM testability

### Test Data Convention

Tests use simulated OCR text strings (no image files needed):

```kotlin
// English
"Lightning Bolt\nInstant\n{R}\nDamage"
// Portuguese
"Relâmpago\nInstantâneo\n{R}"
// Japanese
"雷撃\nインスタント\n{R}"
// Chinese
"闪电击\n瞬间\n{R}"
// Double-faced
"Delver of Secrets // Insectile Aberration\nCreature — Human Wizard\n{1}{U}"
// Messy OCR with watermark numbers
"Sol Ring234\nArtifact\n{T}"
```

### Test Infrastructure

- **Test runner:** `./gradlew :app:testDebugUnitTest` (JVM, no device needed) — or `make test`
- **TextRecognitionProcessorTest:** 17 tests for `extractCardName()`
- **ScryfallApiLookupTest:** 7 tests hitting Scryfall API
- **QtyListImportTest:** 17 tests for the 3rd-party list parser (finish markers, duplicate merging, malformed rows, CRLF, sample rows)
- **Test class locations:**
  - `app/src/test/java/com/gitlab/abelnightroad/data/TextRecognitionProcessorTest.kt`
  - `app/src/test/java/com/gitlab/abelnightroad/data/ScryfallApiLookupTest.kt`
  - `app/src/test/java/com/gitlab/abelnightroad/data/QtyListImportTest.kt`
  - `app/src/test/java/com/gitlab/abelnightroad/data/ScryfallBulkClientTest.kt`
  - `app/src/test/java/com/gitlab/abelnightroad/data/ScryfallBulkImportTest.kt`

## Implementation Notes

### OCR Name Extraction Heuristic

MTG cards have the card name as the first line of text at the top of the card. The extraction logic:

1. Split OCR text by newlines
2. Take the first non-empty line
3. Trim whitespace
4. Handle double-faced cards (contains " // " separator)
5. Strip any trailing numbers or watermarks

### Scryfall Lookup Strategy

1. Use `ScryfallRepository.lookupByNameResilient()` for local DB match
2. If no local match, fall back to Scryfall API `https://api.scryfall.com/cards/named?fuzzy=<name>`
3. If still no match, show "Card not found" with manual search option

### Multi-Language Support

- **Recognition strategy:** Chinese → Japanese → Korean → Latin (ordered by coverage, best result wins by length)
- Chinese model covers: Simplified + Traditional Chinese, kanji (shared with Japanese)
- Japanese model covers: hiragana, katakana, kanji (handles full Japanese cards)
- Korean model covers: hangul characters
- Latin model covers: English, Portuguese, Spanish, French, German, Italian, etc.
- `recognizeText()` runs the two most likely CJK models, picks the longer result, falls back to Korean then Latin

---

# Plan: Settings Tags + 3rd-Party Tag Import

## Overview

Move tag management into Settings, and let a Tag detail screen (from Home)
import a 3rd-party scanner list directly into that tag — no tag prompt, as much
card info preserved as possible, failures reported like every other import.

### Input format (per line)

```
qty Name (SET) collector_number [*finish*]
1 Jyoti, Moag Ancient (M3C) 8 *F*
2 Many Partings (LTR) 176
```

- **Finish marker:** absent → `normal`, `F` → `foil`, `E` → `etched`,
  `FE`/`EF` → `etched foil`; any other marker → skipped with reason.
- **Collector number** identifies the printing (same-set art variants differ).
- **Duplicates** = same Name + Set + Number + Finish → quantities are summed.
  Rows differing in any of those stay separate (all other info identical
  by construction in this format; a differing field anywhere else would too).
- **No reference match** → row is imported with the parsed data and reported
  as "imported without full details".

## Task Checklist

### A. Parser

- [x] Create `data/QtyListImport.kt` with end-anchored line regex (name keeps commas, apostrophes, `//`)
- [x] Map finish markers → `normal` / `foil` / `etched` / `etched foil`
- [x] Merge duplicate rows by summing quantity (case-insensitive name+set+collector+finish)
- [x] Report malformed rows as `SkippedRow(row, reason)`, ignore blank lines

### B. Reference enrichment

- [x] `ScryfallCardDao.bySetAndCollector(set, collector)` (case-insensitive)
- [x] `ScryfallCardDao.byNameAndSet(name, set)` fallback within the same set
- [x] `CardListViewModel.importIntoTag(tag, text)` — parse → enrich (`setName`, `rarity`, `scryfallId`) → insert
- [x] Count unresolved rows (no reference match) for the result dialog

### C. Duplicate merging on insert

- [x] `CardDao.findByDuplicateKey(tag, name, set, collector, foil)` + `addQuantity(id, delta)`
- [x] `CardRepository.insertMergingDuplicates()` — sums quantity when every
      other field matches; any difference keeps the cards separate

### D. Tag detail import UI

- [x] Import icon (`Octicons.Upload24`) beside export in `CardListScreen` top bar
- [x] "Import into \"<tag>\"" dialog with file chooser (`OpenDocument`, TXT/CSV), no tag field
- [x] Result dialog: imported / skipped / imported-without-full-details + skipped list
- [x] Extract shared `ui/components/ImportResultDialogs.kt` (`ImportResultDialog`, `SkippedRowsDialog`)

### E. Move Manage Tags to Settings

- [x] Remove Tags entry from `NAV_ITEMS` (bottom nav: Collection, Scan, Decks, Meta, Settings)
- [x] Settings → new "Tags" card with "Manage Tags" button → `Screen.ManageTags`
- [x] Pass `onManageTags` from `Screens.kt`
- [x] Stop coercing `selectedNavIndex` so screens outside `NAV_ITEMS` show no selection

### F. Tests & Docs

- [x] `QtyListImportTest` — 17 tests (finish markers, merging, split cards, CRLF, malformed rows, sample rows)
- [x] Full suite green: `make test` (45 tests)
- [x] Update `ARCHITECTURE.md` (DAOs, import formats, navigation, §8.2, §8.4, components)
- [x] Update this file

---

# Plan: UX Polish — Onboarding Tags, FAB Speed Dial, Import Progress

## Overview

Three small UX improvements. No DB, API, or navigation-structure changes.

1. Onboarding explains what Tags are.
2. Home FAB unfolds into two actions: **Card** (Add Card screen) and **Tag**
   (the existing "New Tag" dialog) — one tap less to create a tag.
3. Tag import and Settings 3rd-party import show a progress indicator instead
   of silently closing the dialog (currently the app looks idle for seconds),
   including a `#processed / #total` card counter while rows are imported.

## Feature 1 — Onboarding: Tags explanation

File: `ui/OnboardingScreen.kt` (page list at line 42, pager uses `pages.size`).

- [x] Add a 4th `OnboardingPage` after "Track your stats":
  - icon: `FontAwesomeIcons.Solid.Tag`
  - title: `Organize with tags`
  - description: `Tags are the physical location where cards are stored — a
    binder, a box, a deck, or any custom group you create.`
- [x] Nothing else — page count, dots, and "Skip/Next" adapt from `pages.size`

## Feature 2 — Home FAB unfolds (Card / Tag)

Files: `ui/MainScreen.kt`, `ui/MainViewModel.kt`, `ui/components/CreateTagDialog.kt`
(new), `ui/ManageTagsScreen.kt`. `Screens.kt` needs no signature change.

### Behavior

- [x] `fabExpanded: Boolean` state in `MainScreen` (`rememberSaveable`)
- [x] Main FAB: `Plus` icon rotates 45° when expanded (reads as an X) via
      `animateFloatAsState`
- [x] When expanded, two options appear above the FAB with
      `AnimatedVisibility` (slide-up + fade), each a `Row(label, SmallFloatingActionButton)`:
  - **Card** (icon `PenToSquare`) → `onAddCard()` (existing → `Screen.AddCard`)
  - **Tag** (icon `Tag`) → open create-Tag dialog, collapse FAB
- [x] Collapse when: main FAB tapped again or an option chosen

### Dialog reuse

- [x] Extract the "New Tag" `AlertDialog` from `ManageTagsScreen.kt` (lines
      148–185) into `ui/components/CreateTagDialog.kt`:
      `CreateTagDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit)` —
      dialog owns its name state, label field, and "Random" button
- [x] `ManageTagsScreen` calls it with `onCreate = { vm.createTag(it) }`
- [x] `MainViewModel`: add `fun createTag(name: String)` delegating to
      `repository.createTag(name)` — MainViewModel already holds the
      repository, and the Home tag list updates reactively via `tagCounts`
- [x] `MainScreen` hosts `CreateTagDialog(onCreate = viewModel::createTag)`;
      after create the new tag appears on Home with no navigation

## Feature 3 — Import progress indicators

Both flows close their dialog immediately and run the import asynchronously
with zero feedback: Settings (`SettingsScreen.kt` 279–333, `scope.launch(Dispatchers.IO)`
then `showImportDialog = false` right away) and Tag import (`CardListScreen.kt`
105–141, `scope.launch` **without a dispatcher** — file read + enrichment on
the main thread). Shared pattern:

`importing: Boolean` state → keep dialog open → confirm button swaps to
`CircularProgressIndicator(20.dp)` and disables → close dialog only when the
result dialog or the error toast fires.

### A. Tag import (`CardListScreen.kt`)

- [x] Add `isImporting` state; set on Import tap, dialog stays open
- [x] Add `importProgress: Pair<Int, Int>?` state (processed, total)
- [x] Move file read + `vm.importIntoTag` into `Dispatchers.IO`
- [x] Confirm button shows spinner, `enabled = !isImporting`, and
      `"$processed / $total cards"` when `importProgress != null`
- [x] `onDismissRequest` becomes a no-op while importing (no orphan runs)
- [x] Success → close + existing `ImportResultDialog`; failure → close + the
      existing Toast; both paths reset `isImporting` and `importProgress`

### B. Settings 3rd-party import (`SettingsScreen.kt` + private `ImportDialog` ~448)

- [x] Add `isImporting` + `importProgress: Pair<Int, Int>?` state; pass both
      into `ImportDialog`
- [x] Delete the immediate `showImportDialog = false` (line 322); close on completion
- [x] Import button: spinner while importing, otherwise label; counter line
      under/near the spinner when progress is known
- [x] Success (JSON + txt paths) → close + `ImportResultDialog`; failure →
      close + existing `backupStatus` message; both reset `isImporting` and
      `importProgress`

### C. Progress statistics (`data/ThirdPartyImport.kt`)

- [x] Add trailing parameter `onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }`
      to `ThirdPartyImport.import(...)` — default no-op keeps the 2 existing
      test suites (`ThirdPartyImportTest`, Settings callers) source-compatible
- [x] Quantity-list path (the slow one — per-row Scryfall enrichment): `total`
      = `parsed.cards.size`, call `onProgress(i + 1, total)` after each card
- [x] ManaBox CSV path (fast bulk insert): `onProgress(0, n)` after parse and
      `onProgress(n, n)` after insert — counter appears and completes instantly
- [x] `CardListViewModel.importIntoTag` forwards the callback to `import(...)`
- [x] Settings JSON path (`BackupStore.decodeToTag`) stays indeterminate
      spinner only — it is one bulk decode + insert with no row loop
- [x] Callers assign the callback into Compose snapshot state directly
      (safe from IO threads)
- [x] New test: callback receives monotonically increasing counts ending at
      `total` (extend `ThirdPartyImportTest`)

### Notes

- 2 UI call sites only → duplicate the small logic locally, no shared component
  yet (rule of three applies if a third importer appears)
- Counter granularity: per enriched card (qty-list) — matches the actual slow
  work; bulk paths (CSV insert, JSON decode) show start/end or stay
  indeterminate

## Verification

- [x] `make test` — full suite stays green
- [x] `make build` — installable APK
- [ ] Manual: onboarding shows the 4th Tags page; FAB Card → ManualAdd, Tag →
      dialog → tag visible on Home; both imports show a spinner with a live
      `x / y cards` counter that reaches the total, then the result dialog
      (or error), never a dead-looking app

## Commits

1. `feat: explain tags in onboarding`
2. `feat: unfold home fab into card and tag actions`
3. `feat: show progress during tag and settings imports`
4. `docs: update architecture for onboarding, fab speed dial, import progress`

---

# Plan: Post-Testing Fixes — Camera, Colors, CSV Backup, URL Parsers, Multi-Copy

## Overview

Six findings from live testing (Xiaomi 12, HyperOS, Android 15):

1. Scan capture fails: "Not bound to a valid camera" → **F1**
2. Deck format cards need distinct, non-theme-clashing colors → **F2**
3. Collection backup should be CSV-only (JSON import/export removed) → **F3**
4. Camera capture crashes when haptic feedback is on → **F1**
5. Site-URL decklist parsers broken (404 / invalid URL) → **F4**
6. Multi-copy filter must group by card **name**, not name+set → **F5**

## F1 — Scan capture "Not bound to a valid camera" + haptic crash

### Root causes (confirmed)

- **Two racing `bindToLifecycle` calls**, each doing `unbindAll()` on the same
  `ProcessCameraProvider`:
  - `CameraPreview.kt:35-51` — binding lives in the AndroidView **`update`
    lambda**, which re-runs on *every recomposition*, rebinding preview-only
    and wiping the `ImageCapture` bound by `ScanCameraScreen.kt:126-133`.
    Result: preview looks fine, `takePicture()` → "Not bound to a valid camera".
- **Haptic crash**: `AndroidManifest.xml` declares only `INTERNET` + `CAMERA` —
  **`VIBRATE` is missing**, so `vibrator.vibrate()` throws `SecurityException`
  exactly when `hapticFeedback` is on (that's why toggling it stops the crash).

### Tasks

- [x] Add `<uses-permission android:name="android.permission.VIBRATE" />`
- [x] Wrap `performHapticFeedback` body in `try/catch` (OEM-defensive)
- [x] `CameraPreview`: accept `extraUseCases: List<UseCase> = emptyList()`;
      single `LaunchedEffect(Unit)` binds preview + extras once (one
      `unbindAll`); remove all provider work from `update` (factory-only view)
- [x] `ScanCameraScreen`: create `imageCapture` eagerly
      (`remember { ImageCapture.Builder()…build() }`, non-null), pass it as
      extra use case; delete its own competing bind block and nullable state
- [ ] Manual: preview stays live, capture works first try, haptic on/off both
      crash-free

## F2 — Distinct format card colors (`DecksScreen.kt:155-169`)

Two confirmed defects:

- **Repeats**: `abs(format.hashCode()) % palette.size` collides — different
  formats get the same index (and several palette entries are near-identical
  theme tones anyway).
- **Invisible cards**: palette includes `background`, `surface`,
  `surfaceBright`, … — the card renders in the same color as the screen it
  sits on, so the widget disappears (no contrast against its background).

Current palette is 9 MaterialTheme colors — all of them theme-dependent and
several mutually indistinguishable.

### Tasks

- [x] Replace with a static `FormatColor(bg, fg)` palette: ~12 curated muted
      tinted colors, **each guaranteed to contrast against the screen
      background in both light and dark themes** (mid-tone bg + explicit
      near-black/white fg) — no MaterialTheme refs, never `background`/
      `surface`
- [x] Guarantee uniqueness across visible formats: color by position in the
      rendered `formatCounts` list (`index % palette.size`) instead of
      hashCode — repeats only possible with more formats than palette entries
- [x] Card `containerColor = bg`; title `color = fg`; deck-count line uses
      `fg` at reduced alpha instead of `onSurfaceVariant`
- [ ] Manual: every format card visibly distinct and distinct from the
      screen background, in dark + light themes

## F3 — Collection backup: CSV only

`BackupStore.kt` (JSON) is used only by SettingsScreen (export, restore,
3rd-party `.json` branch) — safe to delete; kotlinx-serialization stays
(Scryfall/API models).

- [x] New `data/CollectionCsv.kt`:
  - `encode(cards: List<CardEntity>): String` — header row + every column
    (`tag`, name, set, collector, quantity, foil, rarity, mana cost, type
    line, prices, condition, language, …), RFC4180 quoting (names contain
    commas/quotes), `\r\n`-tolerant `parse`
  - `parse(text: String): List<CardEntity>` — round-trip inverse
- [x] Settings export: `CreateDocument("text/csv")`, filename
      `card_tracker_collection.csv`, `CollectionCsv.encode`; button label
      "Export collection as CSV"
- [x] Settings restore: `OpenDocument("text/csv")` → `CollectionCsv.parse` →
      `repository.replaceAll` (same semantics as the JSON restore)
- [x] 3rd-party `ImportDialog`: mime `text/csv` + `text/plain` only; delete
      the `.json` branch (`BackupStore.decodeToTag`) and JSON from the
      "Choose file" label
- [x] Delete `data/BackupStore.kt`
- [x] Test: `CollectionCsvTest` — full-field round-trip, names with
      commas/quotes/apostrophes, empty tag, CRLF input, header validation

## F4 — Broken site-URL decklist parsers (live-probed 2026-10-01)

User-tested links — all three must import after this fix:

| Link (from site) | Current error | Root cause (probed) |
|---|---|---|
| `https://www.moxfield.com/decks/aSGf97rAHkKrxR_5OfihbA` | `Moxfield API error: HTTP 404` | publicId contains `_`/`-`; `extractDeckId` regex `[a-zA-Z0-9]+` truncates at `_` → fetches `aSGf97rAHkKrxR` → 404 (verified: full id → 200, truncated → 404). Bonus bug: v3 returns the deck **at the root**, wrapper model decodes `data = null` → "Empty response" would follow |
| `https://edhrec.com/deckpreview/4Rm5X7VN7c_H1V33ojqsAw` | `Invalid EDHREC URL` | `extractSlug` only matches `/average-decks/{slug}`. Probe: deckpreview HTML (200) embeds `<script id="__NEXT_DATA__">` → `props.pageProps.data.deck` = `{cards, commander, commander_v2}` — same shape as the average-decks JSON |
| `https://www.mtggoldfish.com/deck/2016013` | `No cards found in MTG_GOLDFISH` | Goldfish has **no URL branch** — the URL falls through to `parseText` → no cards. Probe: every goldfish path returns Cloudflare "Just a moment" (403) to desktop curl; the app's Android `HttpURLConnection` uses OkHttp TLS fingerprint and may pass — must verify on device |

Cross-cutting decode bug (both EDHREC forms): response `commander` is
`["Name"]` (strings) but the model expects pairs → decode fails; the pairs
field is **`commander_v2`**. `cards` map already matches.
Archidekt probed healthy (`/api/decks/{id}/` 200, `cards[].card.oracleCard.name`
present) — re-verify with one real public deck URL.

### Tasks

- [x] Moxfield: regex → `([a-zA-Z0-9_-]+)`; decode deck from response root
      (drop/unwrap `MoxfieldDeckResponse`); keep v3 route — **implemented as
      root decode + nested `boards` map** (v3 returns card lists under
      `boards.mainboard/sideboard/commanders/companions`, verified live)
- [x] EDHREC: accept both `/average-decks/{slug}` (existing JSON endpoint) and
      `/deckpreview/{id}` (fetch HTML → extract `__NEXT_DATA__` →
      `props.pageProps.data.deck` → existing `EdhrecDeck` model);
      map `commander_v2` into the pairs field (`@SerialName("commander_v2")`
      or field + fallback) for both
- [x] MTG Goldfish (new URL support): `extractDeckId` for `/deck/(\d+)` +
      fetch deck page and extract the decklist (inspect real on-device HTML;
      candidate: `#deck_input_deck` textarea or deck table) → feed through
      `UniversalDecklistParser`; add `MTG_GOLDFISH` branch to
      `parseUrl` and show the fetch button for Goldfish
- [x] Goldfish fallback: if Cloudflare still challenges on device, surface
      "Goldfish blocked the request — copy the deck list and paste it instead"
      (no silent failure)
- [x] Archidekt: verify with one real public deck URL; fix only if broken
      (re-verified live 2026-10-01: `/api/decks/1/` 200, `oracleCard.name`
      present — model matches, no fix needed)
- [x] Errors: distinguish "Invalid <Site> URL — expected:
      https://www.moxfield.com/decks/<id>" from HTTP failures
      ("<Site> returned 404 — deck may be private/deleted")
- [x] Tests: `extractDeckId`/`extractSlug` cases (user's exact 3 links, ids
      with `_`/`-`, trailing deck name, `www.` prefix, query params); decode
      tests against **recorded JSON/HTML fixtures** saved from live responses
      (`SiteUrlImportTest` + `fixtures/moxfield_deck.json`,
      `fixtures/edhrec_deckpreview.html`, `fixtures/goldfish_blocked.html`)
- [ ] Manual acceptance: the 3 links above import successfully

## F5 — Multi-copy filter groups by name (`CardDao.kt:131-138`)

Current: `GROUP BY name, set_code HAVING SUM(quantity) > 4` — same name in
different sets/tags/finishes counts separately ("only counts cards exactly
the same").

- [x] `GROUP BY name HAVING SUM(quantity) > 4`; make display columns
      deterministic with aggregates (`MIN(set_code)`, `MIN(set_name)` as
      representative printing; total = sum across **all** sets/tags/finishes)
- [x] Keep `ORDER BY total_quantity DESC, name COLLATE NOCASE ASC`
- [x] `MainScreen` synthetic-row mapping (line 128-135) unchanged
- [ ] Manual: 5 copies across 2 sets/tags → listed once with total 5;
      4 copies anywhere → not listed

## Verification

- [x] `make test` green (+ new `CollectionCsvTest`, Moxfield/EDHREC fixture tests)
- [x] `make build` → installable APK
- [ ] Device pass: capture with haptic on, format card colors (both themes),
      CSV export→reimport round-trip, 3 URL imports, multi-copy filter

## Commits

1. `fix: bind camera preview and capture together, add vibrate permission`
2. `fix: use distinct palette for deck format cards`
3. `feat: replace json collection backup with csv`
4. `fix: site url deck imports (moxfield, edhrec, mtggoldfish)`
5. `fix: multi-copy filter groups cards by name`
6. `docs: update architecture for testing fixes`

---

# Plan: Polish — Decklist Order, Deck Rename, Stats Charts, Lifecycle Owner

## Overview

Four small requests after the 1.2.0 batch: decklist group ordering like MTG
sites, a Rename option on the deck long-press menu, statistics chart
restyling, and following the `LocalLifecycleOwner` deprecation advice.

## 1 — Decklist type group order

File: `ui/DeckViewScreen.kt` (`DecklistTab`).

- [x] Sort mainboard type groups by fixed `MAINBOARD_TYPE_ORDER`:
      Creature → Instant → Sorcery → Artifact → Enchantment → Planeswalker →
      Battle → Land → Other (unknown types last, stable order)
- [x] Slots already ordered Commander → Companion → mainboard → Sideboard
      (sideboard flat, always last) — no change needed

## 2 — LocalLifecycleOwner deprecation

- [x] `CameraPreview.kt`: import `androidx.lifecycle.compose.LocalLifecycleOwner`
      (verified bytecode: the deprecated ui.platform getter delegates to the
      same CompositionLocal — zero runtime change)
- [x] `build.gradle.kts`: explicit `androidx.lifecycle:lifecycle-runtime-compose:2.9.0`
      (was already on the classpath transitively)

## 3 — Deck rename

- [x] `DeckDao.renameDeck(deckId, name)` (`UPDATE decks SET name = …`)
- [x] `DeckRepository.renameDeck` + `DecksViewModel.renameDeck`
- [x] `DecksScreen`: "Rename" menu item between Clone and Delete
      (`PenToSquare` icon) → `AlertDialog` with prefilled `OutlinedTextField`,
      Rename disabled while blank, trims input

## 4 — Statistics charts

- [x] Mana Value: buckets `0…8` + single `9+` overflow — `CmcStat(cmc, count)`
      → `CmcStat(label, count)`; `computeDeckStats` buckets `coerceIn(0, 9)`;
      `DeckStatsTest` assertions updated
- [x] Card Types: `HorizontalBarChart` (deleted — no other callers) →
      `PieChartWithLegend` solid-slice pie + legend rows (color swatch,
      `type: count`, %) using the shared `chartColors` palette
- [x] Color Distribution: `DonutChart` 200dp → 160dp, center total removed

## Verification

- [x] `make test` — full suite green (updated `DeckStatsTest`)
- [x] `make build` → `CardTracker-20bc350.apk`
- [ ] Manual: decklist groups in site order; rename dialog renames and the
      grid updates; stats tabs show 9+ bucket, pie + legend, smaller donut
      without center total; camera preview still binds (import change)

## Commits

1. `feat: order decklist type groups by mtg convention`
2. `refactor: follow lifecycle local lifecycle owner migration`
3. `feat: rename deck from long press menu`
4. `feat: restyle deck statistics charts`
5. `docs: update architecture for rename and charts`

---

# Plan: URL Source Auto-Detection

## Overview

Make decklist URL import pick the parser automatically from the URL host —
the user pastes a link and taps Fetch, no source chip needed. Chips stay for
pasted text/files (parser format still matters: Goldfish CSV, TappedOut `.dck`,
universal).

## Tasks

- [x] `ImportSource` gains `urlHost` + `companion detect(url)` — host-suffix
      match, tolerant of scheme-less input, `www.`, subdomains, `#fragment`/
      query, surrounding whitespace, any case (plain strings, JVM-testable —
      no `android.net.Uri`)
- [x] `parseUrl(url)` drops the `source` param: detect → dispatch; unknown
      host → "Unsupported deck URL — paste the decklist text instead"
- [x] TappedOut URLs: detected but not fetched — probed live, Cloudflare 403
      ("Just a moment") for page and `?fmt=txt` → explicit error "TappedOut
      URLs are blocked (Cloudflare) — copy the deck list and paste it instead"
- [x] Screen: URL path ignores the selected chip; input field shows
      "Detected: <site>" above the button while a recognized URL is typed;
      scheme-less URLs now count as URLs for the button label
- [x] Tests: `ImportSourceDetectTest` — the 4 URLs supplied for this batch
      (archidekt, edhrec average-decks, tappedout, mtggoldfish `#paper`) +
      the F4 links + scheme-less/uppercase/unknown/plain-text cases

## Verification

- [x] `make test` green (78 tests)
- [x] `make build` → installable APK
- [ ] Manual: paste the 4 test URLs → correct "Detected" label + successful
      import (tappedout → paste guidance); unknown site → explicit error;
      chips still drive text/file parsing

## Commits

1. `feat: auto detect decklist source from url`
2. `docs: update architecture for url auto detection`
3. `docs: record url auto detection batch in plan`
4. `build: bump version to 1.3.0 (versionCode 5)`

---

# Plan: Next Batch — Standard Theme, Tag Card Sheet, Covers, Export Filename

Research-only plan for the next batch. Seven features (decisions marked in
each section):

1. **Standard theme** — Material You dynamic color on API 31+, Material 3
   baseline fallback below that. Just another option in the theme dropdown —
   Nord stays the default/fallback (`SettingsStore` `"nord"`, `themeById`).
2. **Tag card tap** — ModalBottomSheet with card image + info + icon actions
   (Move / Edit / Delete). Only inside a Tag (`Screen.Cards`); Home search
   results and DeckView keep `FullscreenOverlay`. Move = existing tags only.
3. **Default deck cover** — commander card (first) / random non-land
   mainboard card when no cover was set.
4. **Metagame covers** — replace stretched 80×40 mtgtop8 thumbs with
   lazily-resolved Scryfall art.
5. **Export CSV filename** — include a ddMMyyyy timestamp.
6. **Scan screen stays open** — capture no longer navigates away; overlays
   show the session count (tap → list) and the last scanned card name.
7. **Language-independent recognition** — identify JP/CN/etc. cards from the
   bottom-left metadata (rarity · collector number · set · language) instead
   of the localized name (root cause of the current JP/CN lookup failures).

## Research Findings

### Current state

- Theme: `ui/theme/ThemeModel.kt` `AppTheme(id, label, light, dark)` — pure
  static `ColorScheme` data; `THEMES` list feeds the Settings dropdown
  automatically (`SettingsScreen.kt:170`). `Theme.kt:AppTheme()` picks
  `theme.dark/light` from the `darkMode` toggle (default `true`).
  4 themes: Catppuccin, Nord, ShadesOfPurple, Cobalt2 — each hand-picked
  palettes, none is a vanilla Android look.
- Tag screen: `CardListScreen.kt` — rows are `Card(...).clickable {
  onCardClick(card) }`; `Screens.kt:119` maps that to `selectedCard` →
  `FullscreenOverlay` (image only, no info/actions). Swipe-left deletes
  (no confirm). No bottom sheet exists anywhere in the app yet.
- Data: `CardEntity` has everything the sheet needs (condition, language,
  added, tag, quantity, foil, scryfall_id) but the list projection
  `CardSearchResult` does **not** (no condition/language/added) → sheet must
  read the full entity, not the list row.
- `CardDao` has no per-card UPDATE for tag/condition/foil/set and no
  `byId(id)`; `TagDao` has no SELECT but `tagCounts()` already unions the
  `tags` table with distinct `cards.tag` (includes empty tags) → Move dialog
  can reuse it.
- `added` = `LocalDateTime.now().toString()` (ISO) from app paths, but CSV
  import copies ManaBox's raw string → date display must be best-effort.
- Finish (`foil`) values are heterogeneous: canonical set from
  `QtyListImport` is `normal / foil / etched / "etched foil"` (note the doc
  comment says `foil etched` but the code stores `etched foil`), plus free
  text from ManualAdd and raw CSV values.
- Market price: only local `scryfall_cards.price_usd` (bulk snapshot, keyed
  by scryfall_id via `ScryfallRepository.lookupById`). No price history →
  the screenshot's `+11.30 (+3.85%)` delta cannot be shown (out of scope).
- Set editing: local `scryfall_cards` keeps one printing per name, so
  alternate printings need the live API. `ScryfallApiLookupTest` already
  proves the pattern: `HttpURLConnection` +
  `https://api.scryfall.com/cards/search?q=...`, User-Agent `MtGCardTracker/1.0`,
  kotlinx.serialization with `ignoreUnknownKeys`.
- Compose BOM `2025.06.00` → Material3 has `ModalBottomSheet`,
  `dynamicLightColorScheme/dynamicDarkColorScheme` (API 31+). Both icons
  families available: FontAwesome Solid (used everywhere) +
  `material-icons-extended` (for a Move icon if FA lacks one).

### Key pitfalls (design responses)

1. **Sheet content is clipped to the sheet's rounded shape** — the
   screenshot's card image floating *above* the sheet edge would be cut off.
   Baseline: image lives *inside* the sheet as the first block
   (`aspectRatio(63f/88f)`, `ContentScale.Fit`, never `Crop` — cards must
   not be cropped). Optional polish: `sheetContainerColor = Transparent` +
   inner `Surface` with rounded top so the image can visually sit higher;
   only if it does not clip.
2. **Back/scrim**: `ModalBottomSheet` is a dialog window → back and scrim
   dismiss it via `onDismissRequest`; `Screens.kt` `BackHandler` (activity
   window) must not also fire. Verify back closes the sheet only, never
   pops the screen.
3. **Read the full `CardEntity`** for the sheet (`SELECT * FROM cards WHERE
   id = :id` as `Flow`) so the sheet stays correct after Move/Edit and closes
   itself naturally after Delete (`entity == null`).
4. **Move must merge, not duplicate**: destination may already have the
   identical row (same key `tag,name,set,cn,foil` + `sameCardInfo`) →
   `addQuantity` + delete source, else `UPDATE cards SET tag`. Reuse the
   private merge semantics already in `CardRepository.insertMergingDuplicates`.
5. **Edit set needs network** — explicit loading + explicit error text on
   failure/HTTP 429/empty (fail fast, no silent empty list). Scryfall
   etiquette: User-Agent required, 1 request here (pagination `has_more` is
   unlikely for one card name; if set, take the first 175 results).
6. **Dynamic color ignores the theme file's colors** — that is the point;
   the `darkMode` toggle still switches `dynamicDark/LightColorScheme`.
   Below API 31 fall back to baseline `lightColorScheme()/darkColorScheme()`.
7. **Icon for Move**: FA Solid has no proven move/exchange icon in use yet →
   verify at compile time (`ArrowRightArrowLeft`/`ShareNodes`), fallback
   `Icons.Filled.Move` from `material-icons-extended`.
8. **Empty/blank `scryfall_id`** (manual rows) → placeholder glyph like the
   deck cards (`♠`), hide the price line.

## Feature 1 — Standard theme (Material You)

- [x] `ui/theme/ThemeModel.kt`: add `dynamic: Boolean = false` to `AppTheme`;
      append `StandardTheme` to `THEMES` (dropdown order; Nord stays default)
- [x] `ui/theme/Standard.kt`: `id = "standard"`, `label = "Standard"`,
      `dynamic = true`, `light/dark` = plain `lightColorScheme()` /
      `darkColorScheme()` (baseline, doubles as the pre-31 fallback)
- [x] `ui/theme/Theme.kt`: branch when `theme.dynamic && SDK_INT >= 31` →
      `dynamicLightColorScheme(LocalContext.current)` / `dynamicDark…`,
      else existing static path (keep `remember(themeId, dark)`)
- [x] No DataStore/Settings changes — theme id is just `"standard"` now
      offered by the existing dropdown
- [x] Test: pure helper for the branch decision if extracted; otherwise
      covered by build + manual check (Compose-only)

## Feature 2 — Tag card bottom sheet

**Data layer**

- [x] `db/CardDao.kt`:
      - `byId(id): Flow<CardEntity?>`
      - `updateAttributes(id, condition, foil)`
      - `updatePrinting(id, setCode, setName, collectorNumber, scryfallId, rarity)`
      - `moveTag(id, newTag)`
- [x] `data/CardRepository.kt`: `cardById`, `moveCard(card, newTag)` (merge
      via `findByDuplicateKey` + `sameCardInfo`), `updateCardAttributes`,
      `updateCardPrinting`
- [x] `data/ScryfallPrintings.kt` (new, JVM-testable):
      `printingsUrl(name)` → `api.scryfall.com/cards/search?q=<!"name">`,
      `parsePrintings(json): List<PrintingInfo>` (set, set_name,
      collector_number, rarity, id, prices.usd, released_at — sort newest
      first), `HttpURLConnection` fetch with the app's User-Agent
- [x] `ui/CardListViewModel.kt`: `moveCard`, `updateAttributes`,
      `updatePrinting`, `deleteCard` (exists), `printings(name)` suspend

**UI layer**

- [x] `ui/components/CardBottomSheet.kt` (new, `@OptIn(ExperimentalMaterial3Api)`):
      - local `selectedCardId` state in `CardListScreen` + `byId` flow
      - content: image block (`ScryfallImage.normal`, `ScryfallAsyncImage`,
        `Fit` + 63/88 box) → `"${quantity}x ${name}"` → `"${setName} #${cn}"`
        → chips (language, condition display via `CardConditions.displayName`,
        tag, rarity) → `"Added on …"` (best-effort ISO parse, raw fallback)
        → `"Market $x.xx"` from `lookupById(scryfallId).priceUsd` (hidden if
        null) → action row of **icon** `IconButton`s: Move, Edit, Delete
      - `rememberModalBottomSheetState(skipPartiallyExpanded = true)`,
        scrollable info column
- [x] Move: `AlertDialog` list-picker of **existing tags only** — rows come
      from `tagCounts` (tags table ∪ distinct `cards.tag`) minus the current
      one; **no text field, no "create tag" path** (creation stays in
      ManageTags/Onboarding), tap a row → `vm.moveCard` → dismiss sheet;
      empty state "No other tags"
- [x] Edit: `ui/components/EditCardDialog.kt` (new):
      - Condition dropdown (`CardConditions.ALL` + current raw value)
      - Finish dropdown (canonical `normal/foil/etched/"etched foil"` +
        current raw value if different)
      - Set row → "Change printings…" → fetch list → picker dialog
        (`setName (SET) #cn · rarity · $price`, newest first) → on pick
        `updatePrinting`
      - Save / Cancel (text buttons — only the sheet's entry actions are
        icon buttons, per spec)
- [x] Delete: `AlertDialog` confirm (error color) → `vm.deleteCard` → sheet
      closes when the flow emits `null`
- [x] `ui/CardListScreen.kt`: own the sheet state; **remove**
      `onCardClick` param; `ui/Screens.kt`: drop the Cards-route wiring
      (line 119) — `selectedCard`/`FullscreenOverlay` stays for Home search
- [x] Tests (JVM): `ScryfallPrintingsParseTest` (fixture JSON: sort order,
      exact fields, malformed → explicit error), added-date formatting,
      finish/condition display formatting

## Feature 3 — Default deck cover (Decks screen)

**Findings:** `DeckEntity.coverScryfallId` is null → DecksScreen draws the `♠`
placeholder (`DecksScreen.kt:319-328`). Manual covers come from DeckView's
"Set as cover" (`DeckViewViewModel.setCover` → `updateDeckCover`) and must
keep winning. `DeckDao.findCardBySlot` exists but has no `ORDER BY` (not
deterministic for "first commander"). `DeckCardEntity.typeLine` is populated
on import → land detection is a simple `NOT LIKE '%Land%'`.

- [x] `db/DeckDao.kt` + `DeckRepository`:
      - `firstCommander(deckId)` → `slot = 'commander' ORDER BY id LIMIT 1`
      - `randomNonLandMain(deckId)` → `slot = 'mainboard' AND type_line
        NOT LIKE '%Land%' ORDER BY RANDOM() LIMIT 1`
- [x] `ui/DecksScreen.kt` `DeckGridCard`: when `coverScryfallId == null`,
      `LaunchedEffect(deck.id)` → pick candidate (commander slot present →
      first commander, else random non-land mainboard) → **persist** via
      `updateDeckCover` (stable cover from then on; no re-roll on every
      visit) → placeholder only when no candidate (empty deck)
- [x] Commander detection by **slot presence**, not the format string
      (format casing/labels vary across import paths; only commander decks
      ever get `slot = 'commander'` rows)

**Pitfalls:** persist freezes the random pick (intended; "Set as cover"
still overrides); cover may go stale if that card is later removed from the
deck (same as manual covers — out of scope); resolve must be async/off the
composition hot path.

## Feature 4 — Metagame screen cover images

**Findings (probed live):** mtgtop8 covers are
`/metas_thumbs/{archetypeId}.jpg` = **80×40 JPEG** (verified), stretched into
a `fillMaxWidth × 80.dp` box (`MetaScreen.kt:155-168`) → blurry 2× upscale.
mtgtop8 has **no larger archetype image anywhere** (archetype/event pages are
text-only). Only real source = Scryfall art. Archetype names are often exact
card names (cEDH probed: `Kinnan, Bonder Prodigy`, `Sisay, Weatherlight
Captain` → local `scryfall_cards.byName` hit; generic ones like
`Partner WUBR` miss). Grid is `LazyVerticalGrid` → lazy work only touches
composed items.

Options:

1. **Lazy two-stage cover (recommended):** A) local `byName(archetypeName)`
   → `ScryfallImage.artCrop` (zero network); B) on miss, fetch the archetype
   page → first deck event page → first card name (COMMANDER section
   preferred, else first non-land mainboard) → local `byName` → artCrop.
   Session cache `Map<archetypeId, coverUrl>` in `MetaViewModel`; mtgtop8
   calls throttled (sequential queue, ~300-500 ms gap, UA already set);
   thumb/placeholder renders first, swaps when resolved; any failure keeps
   the thumb.
2. Local-only (stage A) + show the thumb without upscaling (native size /
   smaller box).
3. Drop the raster cover: generated header from the row's mana-symbol colors
   + monogram (the fragment already carries W/U/B/R/G per row).

Decision (user-confirmed): **Option 1**, degrading to Option 2 if mtgtop8
throttling proves flaky in practice.

**Pitfalls:** extra mtgtop8 traffic (throttle + session-only cache, never
persist — thumbs/art are volatile); `byName` `LIMIT 1` may pick a different
printing than the deck's (harmless for art); wrong-image risk for fuzzy name
matches (keep Stage A to exact-name queries only).

## Feature 5 — Export Collection CSV filename

**Finding:** `SettingsScreen.kt:249` launches SAF with the static name
`card_tracker_collection.csv`.

- [x] Suggested name = `collection_ddMMyyyy.csv` (user-confirmed: 4-digit
      year), built at tap time with
      `SimpleDateFormat("ddMMyyyy", Locale.getDefault())`
- [x] Extract a tiny pure `exportFileName(now: Date)` helper + JVM test
      (matches repo's testable-helper style)

**Pitfall:** SAF lets the user rename and appends `(1)` on same-day
re-exports — acceptable, no dedupe logic.

## Feature 6 — Scan screen: stay on camera + overlays

**Findings:** today the capture callback (`Screens.kt:100-107`) fires
`scanViewModel.processImage(path)` **and immediately navigates** to
`Screen.ScanResults(sid)` — the camera screen leaves composition, the preview
unbinds, and the user must go back to scan the next card. The Scan ViewModel
is `remember`ed in `AppNavigation` (lives for the whole session) and already
exposes `isProcessing` / `lastError`; it does **not** expose the count or the
last successful name. `ScanResultsScreen` already renders the session list
(reuse it as-is). Reference layout (`cam_overlays.jpeg`): overlay 1 =
top-right vertical pill, overlay 2 = bottom-center pill above the nav bar.

- [x] `ui/ScanViewModel.kt`:
      - `scannedCount: StateFlow<Int>` — `sessionId.flatMapLatest {
        getScannedCards(it) }.map { it.size }` (0 before first scan)
      - `lastScan: StateFlow<ScanOverlayState?>` — sealed:
        `Scanning` / `Found(name)` / `Error(message)`; set in `processImage`
        (keep `Found` until the next capture; `Error` reuses `lastError`
        values like `"Card not found: X"` / `"No text detected"`)
      - delete the `SCAN_*.jpg` capture file once OCR finishes (finally) —
        continuous scanning would otherwise fill `filesDir`
- [x] `ui/ScanCameraScreen.kt` new params
      `scannedCount`, `lastScan`, `onOpenList`:
      - **Overlay 1** (top-right, under the TopAppBar): rounded vertical
        pill — list icon + count badge; tap → `onOpenList()`
      - **Overlay 2** (bottom-center, above the shutter): rounded horizontal
        pill — `Scanning…` while processing, card name on success (single
        line, ellipsized), error text (error color) on failure
      - shutter **disabled while `isProcessing`** (today two rapid captures
        run two OCR jobs concurrently and race into the session list)
      - shutter icon: replace the `●` text glyph (reads as a record button)
        with `FontAwesomeIcons.Solid.Camera` (already proven in `NAV_ITEMS`),
        white/`onPrimary`, same 72 dp circle
- [x] `ui/Screens.kt` Scan case: `onImageCaptured = { scanViewModel
      .processImage(it) }` (drop the navigation + `ensureSession` launch);
      `onOpenList = { scope.launch { navigate(Screen.ScanResults(scanViewModel.ensureSession())) } }`

**Pitfalls:** returning from the list re-creates the preview (CameraX
rebind, ~1 s) — accepted, "always open" means while on the Scan screen;
`processImage` is fire-and-forget from a camera callback → keep all state
writes on the VM (they already are); repeated taps on the same card still
create one row per scan (current semantics — count = rows; an optional
`quantity` column + merge would need a DB migration, decide at
implementation if the reference app's "+1" behavior is wanted).

## Feature 7 — Language-independent recognition (name + metadata regions)

**Root cause (matches the user's observation):** OCR is fine — the CJK
models return correct Japanese/Chinese characters (`TextRecognitionProcessor`
runs Chinese → Japanese → Korean → Latin, `TextRecognitionProcessor.kt:27-47`).
The failure is the **lookup**: `extractCardName()` takes the first non-type
line → the localized name → `lookupByNameResilient()` hits `scryfall_cards`,
which is Scryfall's *English* "Default Cards" bulk → miss → fuzzy API search
by a foreign name → Scryfall's name search is English-oriented →
`Card not found`. A card's identity does not depend on language: **set code +
collector number** is printed in Latin on every modern card and is unique per
set (all languages of a card share the collector number).

**Reference regions (`bolt-to-ai.png`):** top box = card name (localized on
foreign cards); bottom-left box = two lines —
`U 0040` (rarity letter + collector number) and `FCA • EN` (set code ·
language).

- [x] `data/TextRecognitionProcessor.kt`: stop discarding structure — new
      `recognizeTextRegions(inputImage): OcrPage` that keeps ML Kit
      `textBlocks` (text + `boundingBox` ints); keep `recognizeText()`
      delegating to it for any other caller
- [x] New `data/CardMetadataParser.kt` (pure, JVM-testable — takes
      `List<OcrBlock(text, top, bottom, left, right)]` + image w/h as plain
      ints, **never touches `android.graphics.Rect`**):
      - `nameRegion(blocks, h, w)` → text of blocks with `bottom <= ~0.20h`
        and not in the top-right mana-cost corner (`right > 0.70w` excluded)
      - `metadataRegion(blocks, h, w)` → blocks with `top >= ~0.82h` and
        `right <= ~0.45w` (left column only — artist/copyright is right-aligned)
      - `parseCollector(line)` → rarity letter + digits
        (`^\s*[CUMBRSFT]\s*[-.]?\s*0*(\d{1,4})\s*$`, tolerate OCR `O/0`
        confusion); return raw and leading-zero-stripped variants
      - `parseSetLang(line)` → `setCode` + `language`
        (`^\s*([A-Z0-9]{2,6})\s*[•·・\-–]?\s*([A-Z]{2,3})\s*$`)
- [x] `ui/ScanViewModel.processImage` lookup order:
      1. metadata parse → `ScryfallCardDao.bySetAndCollector(set, cn)` with
         both collector variants (`"0040"`/`"40"` — local table's padding
         varies by set) → hit = exact card regardless of language
      2. miss → current name path, but fed with the **name-region text**
         (better than "first line of everything" for every language) →
         local `byName` → API fuzzy (unchanged fallback)
      3. still nothing → explicit `"Card not found"` as today
- [x] Language: prefer the metadata code (`EN`, `JP`, `KR`, `ZHS`, …),
      normalized to the existing scheme (`en`/`ja`/`ko`/`zh` — mapping table);
      keep the script-based `detectLanguage()` as fallback when metadata is
      missing
- [x] Model coverage note: the CJK recognizers also read the Latin collector
      line; if metadata parsing fails on a JP/CN card, one extra Latin-model
      pass on the same image is the cheap retry (only on miss — keeps the
      common path single-pass)
- [x] Tests (JVM): parser fixtures — EN/JP/CN OCR samples (name + metadata
      lines), leading-zero / no-zero collectors, `O/0` noise, metadata block
      mixed with artist line (position filter), missing-metadata → name
      fallback; name-region extraction beats "first line" on a card whose
      first line is a watermark

**Pitfalls:** region thresholds are fractional (rotation/perspective shifts
boxes — keep generous bands and never hard-fail: metadata is a *fast path*,
name lookup stays as fallback); pre-Exodus frames / promos / tokens without
the metadata line → fallback path; DFC backs have no name band → fallback;
`bySetAndCollector` needs lowercase set (query is case-insensitive — verify);
foreign-language cards store the DB's English `name` in `scanned_cards` (good
— the list/results show the canonical name + image).

## Verification

- [x] `make test` green (98 existing + new)
- [x] `make build` → installable APK
- [ ] Manual: Settings → Standard theme → wallpaper-derived colors on API 31+,
      baseline below; dark toggle still flips dark/light; other 4 themes intact
- [ ] Manual: Tag → tap card → sheet (image, chips, date, price); back/scrim
      dismiss sheet without leaving the tag; Move picker shows only existing
      tags (no typing) and merges into an identical row in the destination
      tag; Edit changes condition/finish offline and
      set via picker online (explicit error offline); Delete confirms then
      removes; swipe-delete and quantity stepper still work; Home/DeckView
      taps still open the fullscreen overlay
- [ ] Manual: deck without cover shows commander (first of partner pair) or
      a random non-land card after first render, stable on revisit, manual
      cover still wins, empty deck keeps ♠
- [ ] Manual: Metagame covers load Scryfall art for card-named archetypes
      (thumb while loading / on failure), mtgtop8 traffic throttled
- [ ] Manual: export CSV suggests `collection_ddMMyyyy.csv` (today's date)
- [ ] Manual: Scan → capture stays on camera; overlay 1 count increments and
      tap opens the existing list (back returns to a live preview); overlay 2
      shows `Scanning…` → card name (or red error); shutter ignores taps
      while processing; shutter shows a camera icon (no more ● record look);
      capture files don't pile up in `filesDir`
- [ ] Manual: JP and CN cards scan to the correct card (English name +
      image in the list) via metadata; English cards still match; an old
      card without the metadata line still resolves via the name path

## Commits (when implemented)

1. `feat: add material you standard theme`
2. `feat: tag card bottom sheet with move edit delete`
3. `feat: default deck cover from deck cards`
4. `feat: scryfall art covers for metagame archetypes`
5. `feat: timestamped collection export filename`
6. `feat: keep scan camera open with count and name overlays`
7. `feat: identify cards from collector metadata for any language`
8. `docs: update architecture for standard theme and card sheet`
9. `docs: record next batch plan`
