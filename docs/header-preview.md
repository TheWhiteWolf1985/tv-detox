# Fixed header preview

The home page has a separate status bar pinned to the screen's top edge. The original large clock and date remain visible for this first preview. The bar shows a textual date, time, the active network (Ethernet/Wi-Fi/offline) and media mute state.

From the first app row, press **Up** to select the bar. Press **OK** to open quick settings. **Down** from the selected bar returns to the app grid and never opens the panel. Arrow keys adjust the focused slider; Back closes the panel. Up from the volume slider also closes it. Back from the bar returns to the previously selected app.

Quick settings contain media volume, Android screen brightness, mute, Play Store and the TV's Settings. Volume and brightness are read again when the panel opens and after external changes. Volume follows Android's fixed-volume policy. Brightness requests the special system-settings permission when needed and falls back to the TV's own picture settings when Android exposes no brightness value or automatic brightness is enabled. It never changes automatic mode itself. A successful Android value readback does not prove that a vendor TV panel applies that setting; this still needs a visual check on the TV.

`QuickHeader` owns its receiver, network callback and settings observer only while the activity is resumed. It is placed outside the existing page's ScrollView. No input selection or HDMI layout code is changed. Upstream PRs #92 (inputs) and #93 (fixed clock/scroller) have not been incorporated. Any eventual upstream contribution needs reconciliation with those branches and #99's focus/layout changes.

## Separate preview package

The Windows adapter builds `com.thewhitewolf1985.tvhome.preview`, labeled **TV Detox Anteprima**, with a persistent local signing key. The generated manifest keeps the Java class names explicit and omits the HOME intent filter. Installing the preview therefore preserves the official package, its preferences and the default Home selection. The preview has its own app list. The key and build outputs are ignored by Git.

```powershell
./tools/build-preview.ps1
```

The adapter uses Android platform 34 and build-tools 36.0.0 already installed locally. It does not alter the upstream POSIX build.

Host checks:

```powershell
New-Item -ItemType Directory -Force out/header-tests
javac -encoding UTF-8 -d out/header-tests src/com/ansub/tvhome/HeaderValues.java tests/com/ansub/tvhome/HeaderValuesTest.java
java -cp out/header-tests com.ansub.tvhome.HeaderValuesTest
```

`tools/smoke-header.py` runs authorized physical-device checks with an explicit serial and expected model. It operates only on the preview package, briefly adjusts volume/brightness, tests mute restoration, checks that Home and disabled packages remain unchanged, and leaves the panel open. Do not run it while someone is independently changing TV controls. Audible output and perceived panel brightness are separate manual acceptance checks.
