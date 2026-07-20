# Card Tracker

A native Android app for **Magic: The Gathering** collectors to locate cards stored
across physical boxes and binders by tag.

- **Package:** `com.github.abelnightroad`
- **Min SDK:** 26 · **Target / Compile SDK:** 35
- **Stack:** Kotlin, Jetpack Compose, Room, kotlinx.serialization
- **Default theme:** Nord (dark) — Catppuccin and Shades of Purple also included

## Features

- **Tag-based collection** — each storage box/binder is a *Tag* holding many cards.
- **CSV import** — import a ManaBox export and tag every row as a storage location.
- **Global search** by card name and a "more than 4 copies" filter across all tags.
- **Fullscreen card image overlay** (images served by Scryfall).
- **Reference data** — import Scryfall's *Default Cards* bulk file (~532 MB) into a
  local `scryfall_cards` table.
- **Manual entry with autocomplete** — add a card by name; suggestions are pulled
  live from `scryfall_cards`, so set, collector number, rarity and Scryfall ID are
  filled in automatically.

## Build & run

The build uses the **JDK installed on the system** (developed and tested on JDK 26)
and the Android SDK (platform 35). Configure `local.properties` with your signing
key (see the existing `local.properties`).

```bash
./gradlew :app:assembleDebug      # debug APK
make apk                          # release APK -> CardTracker-<commit>.apk
./gradlew :app:testDebugUnitTest  # JVM unit tests
```

## Scryfall reference table (auto-sync)

The `scryfall_cards` table powers **Add Card** autocomplete. It is maintained
automatically:

- **On first launch**, if the table is empty, the app downloads Scryfall's
  *Default Cards* `jsonl.gz` (~557 MB) and streams it into the table.
- **Every 15 days** (checked on launch), the app queries the Scryfall Bulk Data
  API; if `updated_at` changed, it re-downloads and re-imports.
- The downloaded `jsonl.gz` is **always deleted** after import.

Manual import is also available: drawer → **Import CSV** → **Choose Scryfall bulk
file** (select a `jsonl.gz` or JSON export). The importer handles both the
gzip JSON-array and JSON-Lines formats.

> The initial import needs network access and a few minutes of background time.

Once populated, **Add Card** (drawer) autocompletes card names against this table.

## Manual card entry

Open the drawer → **Add Card**. Type a name (≥ 2 chars) to see live suggestions from
the reference table, pick one, then set quantity / foil / condition / tag and save.
The card is written to the `cards` table.

## Themes

Three palettes (Catppuccin, Nord, Shades of Purple), each with light/dark variants,
selectable in Settings. Default is **Nord (dark)**.

## Credits

Card images and reference metadata are sourced from
[Scryfall](https://scryfall.com). This app does not modify or redistribute Scryfall
data.
