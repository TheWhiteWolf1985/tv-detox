# Fixed header and input-sidebar preview

The home page has a separate status bar pinned to the screen's top edge. The bar shows a textual date, time, the active network (Ethernet/Wi-Fi/offline) and media mute state. Preview 5 removes the old large clock, date and network shortcut from the page, so those indicators appear only in the header.

From the first app row, press **Up** to select the bar. Press **OK** to open quick settings. **Down** from the selected bar returns to the app grid and never opens the panel. Arrow keys adjust the focused slider; Back closes the panel. Up from the volume slider also closes it. Back from the bar returns to the previously selected app.

## Input sidebar (preview 5)

All available registered TV inputs now appear as vertical tiles in a fixed right-hand sidebar. Its scrolling is independent of the app grid and header. There is no four-input cap or bottom-row capacity limit. HDMI ports, their detected CEC devices, composite/component and tuners are included; inputs explicitly hidden in Android settings or the existing Home menu remain hidden. A connected CEC device is ordered immediately after its parent HDMI port. Input additions, removals and label changes refresh the sidebar through `TvInputCallback`.

The main area uses four app columns, with the existing Play Store, Settings and Add app utilities below. The default automatic app selection still tops up to six apps, as before; saved app order is preserved. Inputs are no longer rendered in that utility row.

Right from the end of an app/utility row enters the input sidebar. Up/Down traverse the input tiles and scroll the sidebar. Left returns to the previous main-area tile. Up from the first input reaches the header. OK opens an input; holding OK/Menu keeps the existing rename/hide menu. Tuners open their channel URI, while hardware/CEC inputs use the passthrough URI.

The SHARP registered nine inputs at preparation time: four HDMI ports, PlayStation 5 via CEC on HDMI 3, component, composite (with its existing custom label), and two tuners. This is discovery, not proof that each physical port or input launch has been tested. User acceptance of this layout/input routing is separate from the accepted preview-4 backlight control.

Preview 5 was built, signed and installed on the SHARP on 31 October 2026. Directional navigation reached all nine sidebar entries, scrolled vertically through the last tuner, returned to the app area with Left and preserved header Up/Down navigation. Screenshots verified removal of the duplicate status elements and the fixed header. The app launched successfully with no crash-buffer entries. These checks did not activate inputs or change volume/backlight levels; physical input activation and user acceptance of the new layout remain pending.

Quick settings contain media volume, brightness, mute, Play Store and the TV's Settings. Volume and brightness are read again when the panel opens and after external changes. Volume follows Android's fixed-volume policy.

On SHARP AQUOS_TVE19A, `screen_brightness` is stored by Android without a visible panel change (confirmed by the user). Preview 3 used `Settings.Global.picture_brightness`, which the native service maps to picture processing (`g_video__brightness`). The user also observed no visible Home change with that path and clarified that the slider must control the physical light of the entire TV, including apps and HDMI.

Preview 4 therefore connects the existing slider to **`Settings.Global.picture_backlight`**, observed by the TV's existing `TVMenuSettingsService` and mapped to **`g_disp__disp_back_light`**. It uses the already authorized preview-only `WRITE_SECURE_SETTINGS` grant. This permission is broader than this one setting; this adapter writes only `picture_backlight`. It bundles no vendor binaries, uses no root, adds no software dimming overlay and does not open an external screen. It does not change picture processing, picture mode or automatic backlight mode. If permission is missing, it stays in the panel and reports that authorization is needed.

On **31 October 2026**, the user personally tested preview 4 on the SHARP AQUOS_TVE19A and confirmed correct operation. This is user-reported physical acceptance of the installed build from commit `21df61b`, following the negative results for the earlier brightness paths. No separate per-app/HDMI test matrix was recorded. The brief diagnostic change of the backlight from 0 to 30 was restored before delivery. Further physical tests and level changes remain user-controlled. Other devices retain the standard Android brightness path from the first preview; they have not received this device's physical acceptance.

`QuickHeader` owns its receiver, network callback and settings observer only while the activity is resumed. It is placed outside the existing page's ScrollView. Preview 5 keeps that header and the accepted SHARP backlight adapter. Upstream PRs #92 (inputs), #93 (fixed clock/scroller) and #99 (focus/layout) remain separate work to reconcile before any upstream contribution.

## Separate preview package

The Windows adapter builds `com.thewhitewolf1985.tvhome.preview`, labeled **TV Detox Anteprima**, with a persistent local signing key. The generated manifest keeps the Java class names explicit and omits the HOME intent filter. Installing the preview therefore preserves the official package, its preferences and the default Home selection. The preview has its own app list. The key and build outputs are ignored by Git.

```powershell
./tools/build-preview.ps1
```

The adapter uses Android platform 34 and build-tools 36.0.0 already installed locally. It does not alter the upstream POSIX build.

## Persistent brightness authorization

```powershell
./tools/install-preview.ps1 -Serial <authorized-TV-serial>
```

The installer checks the expected TV model and the separate preview APK identity, then uses `adb install -r`. The stable package/signing key preserves app data, UID and existing permission grants. It checks that `WRITE_SECURE_SETTINGS` survived the update before granting it only if missing, keeps `WRITE_SETTINGS` in AppOps `allow` mode, and flushes pending AppOps settings to storage when the OS supports that command. It verifies both grants at the end. There is no signing-mismatch uninstall fallback, automatic input activation, reboot or brightness change.

These grants are stored by Android rather than limited to an ADB session. They remain tied to the installed app identity; uninstall, an explicit revoke or a factory reset requires granting them again. No system-app conversion, root or background permission-restoration service is added. Persistence across the in-place preview-5 update is checked on the TV; a reboot test has not been performed.

Host checks:

```powershell
New-Item -ItemType Directory -Force out/header-tests
javac -encoding UTF-8 -d out/header-tests src/com/ansub/tvhome/HeaderValues.java tests/com/ansub/tvhome/HeaderValuesTest.java
java -cp out/header-tests com.ansub.tvhome.HeaderValuesTest
```

`tools/smoke-header.py` runs authorized physical-device checks with an explicit serial and expected model. It operates only on the preview package, briefly adjusts volume/brightness, tests mute restoration, checks that Home and disabled packages remain unchanged, and leaves the panel open. Do not run it while someone is independently changing TV controls. Audible output and perceived panel brightness are separate manual acceptance checks.
