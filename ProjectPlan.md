# Stoic Launcher — Implementation Plan

## Project Overview

Stoic is a custom Android launcher featuring a futuristic Iron Man HUD-inspired dual-ring interface as the primary interaction hub. The launcher replaces the default home screen with a cyberpunk aesthetic centered around a circular control ring, semi-circular arc panels for app browsing and quick settings, and gesture-based navigation.

---

## Design Decisions Summary

| Element | Decision |
|---------|----------|
| Ring | Dual concentric circles (outer + inner), items as flat-ended arc blocks between them |
| Ring slots | Max 8, evenly distributed (360/n), customizable |
| Center gestures | Swipe down → notifications, swipe left → app drawer, swipe right → quick settings |
| App drawer | Left-edge semi-circle (top-left to bottom-left), A-Z, vertical scroll |
| Quick settings | Right-edge semi-circle (top-right to bottom-right), standard Android quick tiles |
| Navigation (home/back/recents) | Follows device system configuration |
| Block shape | Flat-ended (straight radial cuts) |

---

## Architecture

**Pattern**: MVVM + Clean Architecture (lightweight)

```
UI Layer (Compose)
  └─ ViewModels
       └─ UseCases (Domain)
            └─ Repositories (Data)
                 └─ DataStore / PackageManager / Settings API
```

**Dependency Injection**: Hilt
**UI Framework**: Jetpack Compose + Custom Canvas drawing
**Persistence**: DataStore (ring config), Room (folders/app groups)
**Image Loading**: Coil (app icons)

---

## Package Structure

```
com.rudy.stoic/
├── di/                          # Hilt modules
│   ├── AppModule.kt
│   └── RepositoryModule.kt
│
├── data/
│   ├── repository/
│   │   ├── AppRepositoryImpl.kt
│   │   ├── RingConfigRepositoryImpl.kt
│   │   ├── QuickSettingsRepositoryImpl.kt
│   │   └── SearchRepositoryImpl.kt
│   ├── local/
│   │   ├── dao/
│   │   │   └── FolderDao.kt
│   │   ├── database/
│   │   │   └── StoicDatabase.kt
│   │   ├── datastore/
│   │   │   └── RingConfigDataStore.kt
│   │   └── model/
│   │       ├── RingItemEntity.kt
│   │       └── FolderEntity.kt
│   └── mapper/
│       └── AppMapper.kt
│
├── domain/
│   ├── model/
│   │   ├── RingItem.kt
│   │   ├── InstalledApp.kt
│   │   ├── AppFolder.kt
│   │   ├── QuickSettingTile.kt
│   │   └── GestureDirection.kt
│   ├── repository/
│   │   ├── AppRepository.kt
│   │   ├── RingConfigRepository.kt
│   │   ├── QuickSettingsRepository.kt
│   │   └── SearchRepository.kt
│   └── usecase/
│       ├── GetInstalledAppsUseCase.kt
│       ├── GetRingItemsUseCase.kt
│       ├── UpdateRingConfigUseCase.kt
│       ├── GetQuickSettingsUseCase.kt
│       ├── ToggleQuickSettingUseCase.kt
│       ├── SearchAppsUseCase.kt
│       └── ManageFoldersUseCase.kt
│
├── ui/
│   ├── home/
│   ��   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── ring/
│   │   ├── DualRingView.kt
│   │   ├── RingBlock.kt
│   │   ├── RingGestureDetector.kt
│   │   └── RingMath.kt
│   ├── drawer/
│   │   ├── ArcAppDrawer.kt
│   │   ├── ArcAppItem.kt
│   │   ├── ArcScrollState.kt
│   │   └── DrawerViewModel.kt
│   ├── quicksettings/
│   │   ├── ArcQuickSettings.kt
│   │   ├── ArcToggleTile.kt
│   │   └── QuickSettingsViewModel.kt
│   ├── config/
│   │   ├── RingConfigScreen.kt
│   │   └── RingConfigViewModel.kt
│   ├── search/
│   │   ├── SearchOverlay.kt
│   │   └── SearchViewModel.kt
│   ├── components/
│   │   ├── ArcLayout.kt
│   │   ├── GlowEffect.kt
│   │   └── CyberText.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       ├── Type.kt
│       └── StoicColors.kt
│
├── util/
│   ├── ArcMath.kt
│   ├── SystemActionHelper.kt
│   └── PackageUtils.kt
│
├── receiver/
│   └── PackageChangeReceiver.kt
│
├── StoicApplication.kt
└── MainActivity.kt
```

---

## Implementation Steps

Steps are ordered by **priority (highest first)** and respect **dependency chains**. Each step lists what it depends on and what it unblocks.

---

### Step 1: Project Foundation & Launcher Registration

**Priority**: P0 (Critical — nothing works without this)
**Depends on**: Nothing
**Unblocks**: Steps 2, 3, 4, 5, 6, 7, 8, 9 (everything)

**Objective**: Transform the app from a regular app into a launcher that Android recognizes as a home screen replacement.

**Tasks**:

1.1. **Update AndroidManifest.xml** to declare the app as a launcher:
   - Add `android.intent.category.HOME` and `android.intent.category.DEFAULT` intent filters to `MainActivity`
   - Remove the existing `LAUNCHER` category (launcher itself shouldn't appear in app drawer)
   - Set `android:launchMode="singleTask"` to prevent multiple instances
   - Set `android:stateNotNeeded="true"` since launcher doesn't need saved state on restart
   - Set `android:clearTaskOnLaunch="true"` to reset to home on return
   - Set `android:screenOrientation="portrait"` (launchers are typically portrait-locked)

1.2. **Update MainActivity.kt**:
   - Enable edge-to-edge display (already done)
   - Set up transparent system bars for immersive feel
   - Make status bar and navigation bar transparent
   - Handle `onNewIntent()` for when user presses home while already on launcher
   - Handle `onBackPressed()` — launcher should not exit on back press

1.3. **Set up wallpaper display**:
   - Use `WallpaperManager` to get the current system wallpaper
   - Display it as the background behind all launcher content
   - Handle wallpaper changes via broadcast receiver

1.4. **Add core dependencies to build.gradle.kts**:
   - Hilt (dependency injection)
   - DataStore (preferences persistence)
   - Room (database for folders)
   - Coil (image/icon loading)
   - Compose Navigation (minimal, for config screen)
   - Lifecycle ViewModel Compose

1.5. **Create `StoicApplication.kt`** with `@HiltAndroidApp` annotation

1.6. **Create base Hilt modules** (`di/AppModule.kt`, `di/RepositoryModule.kt`)

1.7. **Verify**: Install on device → Android offers Stoic as a home screen option → selecting it shows wallpaper on full screen

**Estimated Complexity**: Low-Medium

---

### Step 2: Stoic Theme & Color System

**Priority**: P0 (Required before any UI work)
**Depends on**: Step 1 (project compiles and runs)
**Unblocks**: Steps 3, 4, 5, 6, 7, 8

**Objective**: Replace the default Material3 purple theme with the Iron Man / cyberpunk HUD aesthetic.

**Tasks**:

2.1. **Define the Stoic color palette** in `StoicColors.kt`:
   - Primary cyan/electric blue: `#00E5FF` (arc lines, active glow)
   - Secondary amber/gold: `#FFD740` (accents, highlights)
   - Ring background: `#0A1929` at 80% opacity (dark translucent)
   - Block background: `#0D2137` at 85% opacity
   - Block border: `#00E5FF` at 60% opacity
   - Text primary: `#E0F7FA` (light cyan-white)
   - Text secondary: `#80DEEA` (muted cyan)
   - Active/ON state: `#00E5FF`
   - Inactive/OFF state: `#37474F`
   - Glow color: `#00E5FF` at 30% opacity (for blur/shadow effects)

2.2. **Update `Theme.kt`**:
   - Replace dynamic color with Stoic dark-only color scheme
   - Launcher is always dark theme (fits the HUD aesthetic)
   - Override Material3 color slots with Stoic colors

2.3. **Update `Type.kt`**:
   - Choose a monospace or tech font (e.g., JetBrains Mono, or bundled custom font)
   - Define typography scale for ring labels, drawer items, section headers

2.4. **Create `GlowEffect.kt`** utility:
   - Reusable glow/neon effect modifier using `drawBehind` with blurred shadows
   - Parameterized: color, radius, intensity

**Estimated Complexity**: Low

---

### Step 3: Dual-Ring — Static Rendering

**Priority**: P0 (Core visual identity)
**Depends on**: Step 1 (launcher shell), Step 2 (theme/colors)
**Unblocks**: Step 4 (ring interaction), Step 6 (ring customization)

**Objective**: Render the dual concentric ring with flat-ended arc blocks on the home screen.

**Tasks**:

3.1. **Create `RingMath.kt`** utility:
   - Function to calculate arc start/sweep angles for N items with gaps
   - Function to calculate block midpoint (for icon/label positioning)
   - Function to convert screen tap coordinates to polar coordinates
   - Function to determine which block (if any) a tap falls within
   - Constants: default gap angle between blocks (e.g., 4 degrees)

3.2. **Define `RingItem` domain model**:
   ```
   RingItem:
     id: String
     label: String
     type: APP | FOLDER | SYSTEM_ACTION
     packageName: String? (for apps)
     actionId: String? (for system actions like SEARCH, SETTINGS, CALLS, MESSAGES)
     appList: List<String>? (for folders — list of package names)
     iconUri: String?
     position: Int (0 to N-1, clockwise)
   ```

3.3. **Create `DualRingView.kt`** composable using Canvas:
   - Accept parameters: `items: List<RingItem>`, `outerRadius: Dp`, `innerRadius: Dp`
   - Calculate ring size relative to screen (outer radius ~40% of screen width)
   - Inner radius = outer radius * 0.65 (adjustable)
   - Draw outer circle: thin stroke with cyan glow
   - Draw inner circle: thin stroke with cyan glow
   - For each item:
     - Calculate start angle and sweep angle (360/n evenly divided)
     - Draw flat-ended arc block using `Path`:
       - Outer arc (from startAngle for sweepAngle at outerRadius)
       - Straight line radially inward to inner circle
       - Inner arc (reversed, from endAngle back to startAngle at innerRadius)
       - Straight line radially outward to close the path
     - Fill with semi-transparent dark color
     - Stroke border with cyan
     - Draw app icon at the block's angular midpoint, radial midpoint
     - Draw label text below the icon (or below the block)

3.4. **Create default ring items** (6 default slots):
   - Search (system action)
   - Settings (system action)
   - Work (folder — empty initially)
   - Favourites (folder — empty initially)
   - Calls (app — dialer)
   - Messages (app — SMS)

3.5. **Integrate into `HomeScreen.kt`**:
   - Center the dual ring on screen
   - Ring overlays on top of wallpaper

3.6. **Verify**: Launcher shows wallpaper with the dual ring centered, 6 blocks visible with icons and labels

**Estimated Complexity**: High (custom Canvas drawing)

---

### Step 4: Ring Interaction — Tap & Center Gestures

**Priority**: P0 (Ring must be interactive to be useful)
**Depends on**: Step 3 (ring is rendered)
**Unblocks**: Step 5 (panels that gestures open), Step 7 (search)

**Objective**: Make ring blocks tappable to launch their targets, and detect swipe gestures in the center zone.

**Tasks**:

4.1. **Implement tap detection on ring blocks**:
   - Use `pointerInput` modifier on the DualRingView canvas
   - On tap: convert (x, y) to polar coordinates (distance, angle)
   - If distance is between innerRadius and outerRadius → determine which block by angle
   - Trigger the block's action:
     - APP type → launch app via `packageManager.getLaunchIntentForPackage()`
     - FOLDER type → expand folder (deferred to Step 6)
     - SYSTEM_ACTION type → handle accordingly:
       - SEARCH → open search overlay (Step 7)
       - SETTINGS → launch system settings intent
       - CALLS → launch dialer app
       - MESSAGES → launch SMS app

4.2. **Add tap visual feedback**:
   - On press: highlight the block (brighter border, slight scale)
   - Animate back on release
   - Haptic feedback on tap (`HapticFeedbackType.LightTap`)

4.3. **Create `RingGestureDetector.kt`**:
   - Detect touch events only within the inner circle (center zone)
   - Track pointer down → move → up
   - Calculate dominant swipe direction based on:
     - Horizontal delta (dx) vs vertical delta (dy)
     - Minimum velocity threshold to avoid accidental triggers
     - Minimum distance threshold
   - Emit gesture events:
     - Swipe Down → `GestureDirection.DOWN` (notifications)
     - Swipe Left → `GestureDirection.LEFT` (app drawer)
     - Swipe Right → `GestureDirection.RIGHT` (quick settings)

4.4. **Wire gestures to actions in `HomeViewModel.kt`**:
   - `GestureDirection.DOWN` → expand notification panel (use `StatusBarManager.expandNotificationsPanel()`)
   - `GestureDirection.LEFT` → set UI state to show app drawer
   - `GestureDirection.RIGHT` → set UI state to show quick settings

4.5. **Implement notification panel expansion**:
   - Add `EXPAND_STATUS_BAR` permission to manifest
   - Use reflection to call `StatusBarManager.expandNotificationsPanel()`
   - Fallback: accessibility service approach (if OEM blocks reflection)

4.6. **Verify**: Tap ring blocks to launch apps, swipe down in center opens notification shade, swipe left/right changes UI state (panels built in next step)

**Estimated Complexity**: Medium-High

---

### Step 5: Arc Panels — App Drawer & Quick Settings

**Priority**: P0 (Core navigation — user needs to access all apps)
**Depends on**: Step 4 (gestures trigger panel open)
**Unblocks**: Step 8 (polish/animations)

**Objective**: Build the semi-circular arc panels — app drawer on the left edge and quick settings on the right edge.

**Tasks**:

5.1. **Create `ArcMath.kt`** shared utility:
   - Calculate item positions along a semi-circle arc
   - Parameterized for both left and right edge arcs:
     - **Left arc** (App Drawer): center off-screen left (x = -radius * 0.3, y = screenHeight / 2), spans top-left to bottom-left
     - **Right arc** (Quick Settings): center off-screen right (x = screenWidth + radius * 0.3, y = screenHeight / 2), spans top-right to bottom-right
   - Function to map vertical scroll offset to angular position
   - Function to calculate (x, y) position for item at a given angle
   - Function to calculate rotation angle for tangent-aligned rendering
   - Accept `ArcSide.LEFT` or `ArcSide.RIGHT` enum to flip geometry

5.2. **Create `ArcScrollState.kt`**:
   - Custom scroll state that maps vertical finger drag to angular movement
   - Support fling with deceleration
   - Clamp scroll to valid range (first item to last item)
   - Expose current scroll offset as angular position

5.3. **Create `AppRepository`** (data layer):
   - Query all installed apps via `PackageManager` or `LauncherApps` API
   - Return `List<InstalledApp>` sorted alphabetically
   - Cache the list, refresh on package install/uninstall broadcasts
   - Each `InstalledApp`: name, packageName, icon (as `Drawable` or loaded via Coil)

5.4. **Create `PackageChangeReceiver.kt`**:
   - BroadcastReceiver for `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_CHANGED`
   - Notify `AppRepository` to refresh cached app list

5.5. **Create `ArcAppDrawer.kt`** composable:
   - Semi-circle background: dark translucent arc shape drawn via Canvas `Path`
   - Position each app along the arc using `ArcMath`
   - Each app item: icon + label, tangent-aligned to the arc
   - Alphabet section headers along the arc edge (A, B, C...)
   - Vertical drag gesture → scroll through apps on the arc
   - Tap on app → launch it
   - Tap outside arc → dismiss drawer
   - Slide-in animation from left edge on open

5.6. **Create `ArcAppItem.kt`** composable:
   - Renders a single app entry at a given arc position
   - Icon (loaded via Coil from app's package) + label
   - Rotated to be tangent to the arc
   - Highlight on press

5.7. **Create `QuickSettingsRepository`** (data layer):
   - Query available quick setting tiles
   - Standard tiles: WiFi, Bluetooth, Mobile Data, Airplane Mode, Brightness, Auto-Rotate, DND, Flashlight, Location, NFC, Battery Saver, Hotspot
   - Read current state of each toggle via `Settings.System` / `Settings.Global` / `ConnectivityManager` / `WifiManager` etc.
   - Provide toggle functions for each setting

5.8. **Create `ArcQuickSettings.kt`** composable:
   - Uses `ArcMath` with `ArcSide.RIGHT` — mirrored arc geometry anchored to right edge
   - Arc center: off-screen to the right, semi-circle spans top-right corner to bottom-right corner
   - Each tile: icon + label + on/off state indicator
   - ON state: cyan glow, OFF state: dim gray
   - Tap tile → toggle the setting
   - Brightness: special handling — render as a value indicator (not a simple toggle)
   - Slide-in animation from right edge on open (matches swipe-right gesture direction)

5.9. **Create `ArcToggleTile.kt`** composable:
   - Renders a single quick setting tile on the arc
   - Icon + label + state dot/glow
   - Tap handler → calls toggle use case

5.10. **Wire into `HomeScreen.kt`**:
   - State machine: `HOME` (ring visible) | `DRAWER_OPEN` | `QUICK_SETTINGS_OPEN`
   - Swipe left in center → transition to `DRAWER_OPEN` (left-edge arc appears)
   - Swipe right in center → transition to `QUICK_SETTINGS_OPEN` (right-edge arc appears)
   - Tap outside / back press → transition back to `HOME`
   - Only one panel open at a time
   - Both panels can coexist on screen spatially (left vs right) but only one opens at a time for UX clarity

5.11. **Required permissions** (add to manifest):
   - `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` (WiFi toggle)
   - `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT` (Bluetooth toggle)
   - `WRITE_SETTINGS` (brightness, auto-rotate) — requires user grant via `Settings.ACTION_MANAGE_WRITE_SETTINGS`
   - `ACCESS_NETWORK_STATE` (mobile data state)
   - `CAMERA` or `FLASHLIGHT` (flashlight toggle)

5.12. **Verify**:
   - Swipe left → app drawer slides in from LEFT edge as semi-circle with all apps A-Z
   - Scroll vertically to browse apps, tap to launch
   - Swipe right → quick settings slides in from RIGHT edge as mirrored semi-circle
   - Toggle WiFi/Bluetooth/Flashlight and verify state changes
   - Tap outside to dismiss either panel

**Estimated Complexity**: Very High (most complex step — custom arc layout + system APIs)

---

### Step 6: Ring Customization

**Priority**: P1 (Important but ring works with defaults without this)
**Depends on**: Step 3 (ring rendering), Step 5 (app drawer — to pick apps for ring)
**Unblocks**: Step 9 (edge cases)

**Objective**: Let users add, remove, reorder ring items and create folders.

**Tasks**:

6.1. **Create `RingConfigDataStore.kt`**:
   - Persist ring items configuration using Proto DataStore or Preferences DataStore
   - Store: list of `RingItem` with position, type, target info
   - Default config: the 6 default items from Step 3

6.2. **Create Room database for folders**:
   - `FolderEntity`: id, name, iconUri
   - `FolderAppCrossRef`: folderId, packageName
   - `FolderDao`: CRUD operations
   - `StoicDatabase`: Room database class

6.3. **Create `RingConfigScreen.kt`**:
   - Visual preview of the ring with current items
   - "Add item" button (visible if < 8 items)
   - Each item shows: icon, label, type, remove button
   - Drag-to-reorder support (reorder changes angular position)
   - Add item flow:
     - Choose type: App | Folder | System Action
     - If App → show app picker (list of installed apps)
     - If Folder → name the folder, then pick apps to include
     - If System Action → show available actions (Search, Settings, Calls, Messages)
   - Remove item: long-press or swipe on item in config list
   - Enforce max 8 items limit
   - Ring preview updates live as user configures

6.4. **Create `RingConfigViewModel.kt`**:
   - Load current config from DataStore
   - Handle add/remove/reorder operations
   - Validate constraints (min 1, max 8 items)
   - Save changes to DataStore

6.5. **Folder expansion on ring tap**:
   - When a FOLDER type ring block is tapped, expand to show contained apps
   - Expansion UI: small arc of app icons that fans out from the block's position
   - Tap an app in the expanded folder → launch it
   - Tap outside → collapse folder

6.6. **Navigation to config screen**:
   - Long-press on empty area of home screen (outside ring) → open ring config
   - Or: add a small settings gear icon somewhere accessible

6.7. **Verify**:
   - Open config, add/remove apps, create folder, reorder items
   - Ring updates with changes
   - Folder tap expands and shows contained apps
   - Config persists across launcher restarts

**Estimated Complexity**: Medium-High

---

### Step 7: Search Functionality

**Priority**: P1 (Important feature, but launcher is usable without it)
**Depends on**: Step 4 (ring tap triggers search), Step 5 (app repository exists)
**Unblocks**: Nothing directly

**Objective**: Unified search for phone apps and internet.

**Tasks**:

7.1. **Create `SearchOverlay.kt`** composable:
   - Overlay that appears when Search ring block is tapped
   - Text input field at top with auto-focus and keyboard open
   - Results displayed below in two sections:
     - "Apps" — matching installed apps (filtered from AppRepository)
     - "Search Web" — option to open browser with search query
   - Styled with Stoic theme (dark, cyan accents)

7.2. **Create `SearchRepository.kt`**:
   - Filter installed apps by name matching query
   - Generate web search URL (Google): `https://www.google.com/search?q={query}`

7.3. **Create `SearchViewModel.kt`**:
   - Debounced query input (300ms)
   - Combine app results + web search option
   - Handle app launch and web search launch

7.4. **Verify**:
   - Tap Search on ring → overlay appears with keyboard
   - Type query → matching apps appear
   - Tap app result → launches app
   - Tap "Search Web" → opens browser with query

**Estimated Complexity**: Medium

---

### Step 8: Visual Polish & Animations

**Priority**: P2 (Enhances UX significantly but functionally complete without it)
**Depends on**: Steps 3, 4, 5 (all core UI built)
**Unblocks**: Nothing

**Objective**: Apply the Iron Man HUD aesthetic with animations, glow effects, and haptics.

**Tasks**:

8.1. **Ring glow effects**:
   - Outer and inner circle: pulsing cyan glow (animated shadow radius)
   - Block borders: subtle neon glow
   - Selected/pressed block: intensified glow + slight scale up
   - Idle animation: slow rotating scan line or pulse wave on the ring

8.2. **Ring entrance animation**:
   - On launcher start: ring draws itself in (arcs animate from 0 to full sweep)
   - Blocks fade in sequentially with slight delay per block
   - Circles draw with a "tracing" animation

8.3. **Panel animations**:
   - App drawer: slides in from LEFT edge with an arc-reveal animation
   - Quick settings: slides in from RIGHT edge with a mirrored arc-reveal animation
   - Items along each arc stagger-animate into position
   - Dismiss: reverse animation (drawer retreats left, quick settings retreats right)

8.4. **Haptic feedback**:
   - Light tap on ring block tap
   - Medium tap on gesture recognition
   - Tick on scroll through drawer items (subtle)

8.5. **Center zone visual**:
   - Subtle animated pattern inside inner circle (grid lines, scan effect)
   - Visual hint arrows for gesture directions (very subtle, fade out after first use)

8.6. **Icon rendering**:
   - App icons with slight glow halo
   - System action icons: custom vector icons matching the HUD style

8.7. **Transition between states**:
   - Home → Drawer: ring fades/shrinks, drawer slides in from left
   - Home → Quick Settings: ring fades/shrinks, quick settings slides in from right
   - Back to Home: panel retreats to its edge, ring re-entrance animation plays

**Estimated Complexity**: Medium-High

---

### Step 9: Edge Cases, Stability & Accessibility

**Priority**: P2 (Required for production quality)
**Depends on**: All previous steps
**Unblocks**: Nothing (final step)

**Objective**: Handle all edge cases that make a launcher reliable for daily use.

**Tasks**:

9.1. **Launcher reliability**:
   - Handle crash recovery — if launcher crashes, Android restarts it
   - Ensure `onCreate` is fast (< 300ms to first frame)
   - Handle configuration changes (rotation lock already set, but test)
   - Handle low memory scenarios — launcher should be last to be killed

9.2. **App install/uninstall handling**:
   - `PackageChangeReceiver` updates app list in real-time
   - If a ring item's app is uninstalled → show placeholder or remove from ring
   - If a folder's app is uninstalled → remove from folder

9.3. **Default app handling**:
   - When user selects Stoic as default launcher, handle it gracefully
   - When user wants to switch away, don't block — they can change in system settings
   - Handle `ACTION_MAIN` + `CATEGORY_HOME` intent properly

9.4. **Permission handling**:
   - `WRITE_SETTINGS` requires explicit user grant — show clear prompt explaining why
   - Handle permission denied gracefully (gray out affected quick settings)
   - `EXPAND_STATUS_BAR` handling across OEM variants

9.5. **Accessibility**:
   - Add content descriptions to all ring blocks
   - TalkBack support: announce block labels on focus
   - Sufficient contrast ratios for text
   - Touch target sizes meet 48dp minimum

9.6. **Performance optimization**:
   - App list loading: load icons lazily, cache in memory
   - Ring drawing: avoid unnecessary recompositions
   - Profile with Android Studio profiler — target 60fps

9.7. **Widget support** (optional, lower priority):
   - Allow placing widgets on the home screen behind/around the ring
   - Use `AppWidgetHost` and `AppWidgetManager`
   - This is a significant addition — can be deferred to v2

**Estimated Complexity**: Medium

---

## Dependency Graph

```
Step 1 (Launcher Shell)
  │
  ├──→ Step 2 (Theme)
  │       │
  │       ├──→ Step 3 (Ring Rendering)
  │       │       │
  │       │       ├──→ Step 4 (Ring Interaction + Gestures)
  │       │       │       │
  │       │       │       ├──→ Step 5 (Arc Panels: Drawer + Quick Settings)
  │       │       │       │       │
  │       │       │       │       ├──→ Step 8 (Visual Polish)
  │       │       │       │       │
  │       │       │       │       └──→ Step 9 (Edge Cases)
  │       │       │       │
  │       │       │       └──→ Step 7 (Search)
  │       │       │
  │       │       └──→ Step 6 (Ring Customization)
  │       │               │
  │       │               └──→ Step 9 (Edge Cases)
  │       │
  │       └──→ Step 8 (Visual Polish)
  │
  └──→ Step 5 (needs launcher + theme for system APIs)
```

## Summary Table

| Step | Name | Priority | Depends On | Complexity |
|------|------|----------|------------|------------|
| 1 | Launcher Shell & Foundation | P0 | — | Low-Medium |
| 2 | Theme & Color System | P0 | Step 1 | Low |
| 3 | Dual-Ring Static Rendering | P0 | Steps 1, 2 | High |
| 4 | Ring Interaction & Gestures | P0 | Step 3 | Medium-High |
| 5 | Arc Panels (Drawer + Quick Settings) | P0 | Step 4 | Very High |
| 6 | Ring Customization | P1 | Steps 3, 5 | Medium-High |
| 7 | Search Functionality | P1 | Steps 4, 5 | Medium |
| 8 | Visual Polish & Animations | P2 | Steps 3, 4, 5 | Medium-High |
| 9 | Edge Cases & Stability | P2 | All | Medium |
