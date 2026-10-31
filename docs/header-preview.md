# Fixed header preview

The home page has a separate status bar pinned to the screen's top edge. The original large clock and date remain visible for this first preview. The bar shows a textual date, time, the active network (Ethernet/Wi-Fi/offline) and media mute state.

From the first app row, press **Up** to select the bar. Press **OK** to open quick settings. **Down** from the selected bar returns to the app grid and never opens the panel. Arrow keys adjust the focused slider; Back closes the panel. Up from the volume slider also closes it. Back from the bar returns to the previously selected app.

Quick settings contain media volume, brightness, mute, Play Store and the TV's Settings. Volume and brightness are read again when the panel opens and after external changes. Volume follows Android's fixed-volume policy.

On SHARP AQUOS_TVE19A, `screen_brightness` is stored by Android without a visible panel change (confirmed by the user). Preview 3 used `Settings.Global.picture_brightness`, which the native service maps to picture processing (`g_video__brightness`). The user also observed no visible Home change with that path and clarified that the slider must control the physical light of the entire TV, including apps and HDMI.

Preview 4 therefore connects the existing slider to **`Settings.Global.picture_backlight`**, observed by the TV's existing `TVMenuSettingsService` and mapped to **`g_disp__disp_back_light`**. It uses the already authorized preview-only `WRITE_SECURE_SETTINGS` grant. This permission is broader than this one setting; this adapter writes only `picture_backlight`. It bundles no vendor binaries, uses no root, adds no software dimming overlay and does not open an external screen. It does not change picture processing, picture mode or automatic backlight mode. If permission is missing, it stays in the panel and reports that authorization is needed.

On **31 October 2026**, the user personally tested preview 4 on the SHARP AQUOS_TVE19A and confirmed correct operation. This is user-reported physical acceptance of the installed build from commit `21df61b`, following the negative results for the earlier brightness paths. No separate per-app/HDMI test matrix was recorded. The brief diagnostic change of the backlight from 0 to 30 was restored before delivery. Further physical tests and level changes remain user-controlled. Other devices retain the standard Android brightness path from the first preview; they have not received this device's physical acceptance.

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
