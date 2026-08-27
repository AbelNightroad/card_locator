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

- **Test runner:** `./gradlew :app:testDebugUnitTest` (JVM, no device needed)
- **TextRecognitionProcessorTest:** 16 tests for `extractCardName()` — English, Portuguese, Japanese, Chinese, double-faced cards, watermark stripping, mana cost skipping, type line filtering
- **ScryfallApiLookupTest:** 8 tests hitting Scryfall API — exact name lookup, color identity, price, special characters, fake card rejection, multi-result search, UUID format validation
- **Test class locations:**
  - `app/src/test/java/com/gitlab/abelnightroad/data/TextRecognitionProcessorTest.kt`
  - `app/src/test/java/com/gitlab/abelnightroad/data/ScryfallApiLookupTest.kt`

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
