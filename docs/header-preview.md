# Fixed header preview

The home page has a separate status bar pinned to the screen's top edge. The original large clock and date remain visible for this first preview. The bar shows a textual date, time, the active network (Ethernet/Wi-Fi/offline) and media mute state.

From the first app row, press **Up** to select the bar. Press **OK** to open quick settings. **Down** from the selected bar returns to the app grid and never opens the panel. Arrow keys adjust the focused slider; Back closes the panel. Up from the volume slider also closes it. Back from the bar returns to the previously selected app.

Quick settings contain media volume, brightness, mute, Play Store and the TV's Settings. Volume and brightness are read again when the panel opens and after external changes. Volume follows Android's fixed-volume policy.

On SHARP AQUOS_TVE19A, `screen_brightness` is stored by Android without a visible panel change (confirmed by the user). The corrected adapter uses `Settings.Global.picture_brightness`: the TV's existing `TVMenuSettingsService` observes that URI and maps it to its native `g_video__brightness` picture control. This needs a one-time explicit `WRITE_SECURE_SETTINGS` grant to the preview app. The permission is broader than this one setting; the adapter only writes the exact `picture_brightness` key. No vendor binaries are bundled, no root is used, and the slider does not open an external screen. It does not change backlight, picture mode, automatic brightness or other picture controls. If the permission is missing, it stays in the panel and reports that authorization is needed.

Other devices retain the standard Android brightness path from the first preview. The corrected SHARP path was tested on the TV after the preview-only permission grant: the slider changed the value from 52 to 57 and back to 52; TVMenuSettingsService applied both native commands with return code 0, and its subsequent native read reported 57 before restoration. The old Android screen_brightness setting was unchanged. Visual acceptance remains with the user. Do not treat settings readback alone as physical acceptance.

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
