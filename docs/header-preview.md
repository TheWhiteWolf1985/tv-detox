# Fixed header and input-sidebar preview

The home page has a separate status bar pinned to the screen's top edge. The bar shows a textual date, time, the active network (Ethernet/Wi-Fi/offline) and media mute state. Preview 5 removes the old large clock, date and network shortcut from the page, so those indicators appear only in the header.

From the first app row, press **Up** to select the bar. Press **OK** to open quick settings. **Down** from the selected bar returns to the app grid and never opens the panel. Arrow keys adjust the focused slider; Back closes the panel. Up from the volume slider also closes it. Back from the bar returns to the previously selected app.

## Input sidebar (preview 5)

All available registered TV inputs now appear as vertical tiles in a fixed right-hand sidebar. Its scrolling is independent of the app grid and header. There is no four-input cap or bottom-row capacity limit. HDMI ports, their detected CEC devices, composite/component and tuners are included; inputs explicitly hidden in Android settings or the existing Home menu remain hidden. A connected CEC device is ordered immediately after its parent HDMI port. Input additions, removals and label changes refresh the sidebar through `TvInputCallback`.

The main area uses four app columns, with the existing Play Store, Settings and Add app utilities below. The default automatic app selection still tops up to six apps, as before; saved app order is preserved. Inputs are no longer rendered in that utility row.

Right from the end of an app/utility row enters the input sidebar. Up/Down traverse the input tiles and scroll the sidebar. Left returns to the previous main-area tile. Up from the first input reaches the header. OK opens an input; holding OK/Menu keeps the existing rename/hide menu. Tuners open their channel URI, while hardware/CEC inputs use the passthrough URI.

The SHARP registered nine inputs at preparation time: four HDMI ports, PlayStation 5 via CEC on HDMI 3, component, composite (with its existing custom label), and two tuners. This is discovery, not proof that each physical port or input launch has been tested. User acceptance of this layout/input routing is separate from the accepted preview-4 backlight control.

Preview 5 was built, signed and installed on the SHARP on 31 October 2026. Directional navigation reached all nine sidebar entries, scrolled vertically through the last tuner, returned to the app area with Left and preserved header Up/Down navigation. Screenshots verified removal of the duplicate status elements and the fixed header. The app launched successfully with no crash-buffer entries. These checks did not activate inputs or change volume/backlight levels; physical input activation and user acceptance of the new layout remain pending.

Later on 31 October 2026, the user personally confirmed that the sidebar worked well and that inputs opened correctly. This supplies user-reported acceptance of the preview-5 input navigation and launch behavior.

## Glass chrome (preview 6)

Preview 6 introduced the same `GlassSurface` palette for the closed header and input sidebar: a translucent grey tint, a faint diagonal sheen and a one-pixel edge at 1080p. Initially each surface used one native `GradientDrawable`. The header keeps a brighter focused state for remote navigation. The sidebar starts below the header so their translucent backgrounds never overlap and darken the top-right corner. Input tiles, navigation and quick-setting behavior were unchanged.

Preview 6 was built, signed and installed on the SHARP. Directional checks reached the last input and returned to the main area/header; screenshots confirmed the normal and focused surfaces. Backlight and retained preview permissions were unchanged. An idle two-second sample added no rendered frames. A navigation sample showed slow frames, so it is not evidence of a frame-rate improvement or a comparative performance result. Visual acceptance of this finish remains with the user.

Quick settings contain media volume, brightness, mute, Play Store and the TV's Settings. Volume and brightness are read again when the panel opens and after external changes. Volume follows Android's fixed-volume policy.

On SHARP AQUOS_TVE19A, `screen_brightness` is stored by Android without a visible panel change (confirmed by the user). Preview 3 used `Settings.Global.picture_brightness`, which the native service maps to picture processing (`g_video__brightness`). The user also observed no visible Home change with that path and clarified that the slider must control the physical light of the entire TV, including apps and HDMI.

Preview 4 therefore connects the existing slider to **`Settings.Global.picture_backlight`**, observed by the TV's existing `TVMenuSettingsService` and mapped to **`g_disp__disp_back_light`**. It uses the already authorized preview-only `WRITE_SECURE_SETTINGS` grant. This permission is broader than this one setting; this adapter writes only `picture_backlight`. It bundles no vendor binaries, uses no root, adds no software dimming overlay and does not open an external screen. It does not change picture processing, picture mode or automatic backlight mode. If permission is missing, it stays in the panel and reports that authorization is needed.

On **31 October 2026**, the user personally tested preview 4 on the SHARP AQUOS_TVE19A and confirmed correct operation. This is user-reported physical acceptance of the installed build from commit `21df61b`, following the negative results for the earlier brightness paths. No separate per-app/HDMI test matrix was recorded. The brief diagnostic change of the backlight from 0 to 30 was restored before delivery. Further physical tests and level changes remain user-controlled. Other devices retain the standard Android brightness path from the first preview; they have not received this device's physical acceptance.

`QuickHeader` owns its receiver, network callback and settings observer only while the activity is resumed. It is placed outside the existing page's ScrollView. Preview 5 keeps that header and the accepted SHARP backlight adapter. Upstream PRs #92 (inputs), #93 (fixed clock/scroller) and #99 (focus/layout) remain separate work to reconcile before any upstream contribution.

## Collapsible input drawer (preview 7)

The right input sidebar starts hidden. Only a compact glass tab labeled **Inputs / Ingressi** remains beside the wider four-column app grid. Focusing the tab does not open anything: press **OK** to show the vertical input list and focus its first tile. Up/Down navigate the inputs. **Back** or **Left** closes the drawer and returns to the tab; Left from that tab returns to the last main-area tile. The header keeps its existing focus/OK behavior.

The drawer overlays the right side when open. Main-area tiles are temporarily excluded from focus, so directional navigation cannot land on a covered app. The main scroller is clipped at the drawer edge to keep app banners from showing through its translucent background. Opening/closing only toggles visibility, clipping and focus; it does not rebuild the app grid or run an opening animation. Selecting an input closes the drawer before using the existing input launch route. The glass palette and persistent brightness grants are retained.

An identical short navigation sample on the SHARP measured preview-5 median/p95 frame times of 18/23 ms and preview-6 times of 20/31 ms. This is one sample per build, affected by execution order and TV load. It motivated replacing the per-frame gradient with two shared 64x64 textures (normal/focused, 32 KiB total pixel storage) in preview 7. The textures are generated once and reused by header, tab and drawer. The effect uses no live blur, wallpaper capture or continuous animation.

Preview 7 was built, signed, updated in place and launched on the SHARP on 31 October 2026. Device checks verified the hidden initial state, focus-only remaining closed, OK opening the first input, traversal to the ninth input, Left/Back closing to the tab, return to apps and the header's existing OK panel. Screenshots confirmed the drawer clipping and wider app grid. Brightness grants were retained; no input was activated or audio/backlight level intentionally changed. The crash buffer had no entries. A separate Home-only navigation sample measured median/p95 11/19 ms and an idle two-second sample added zero frames; its different layout/path prevents a direct performance comparison with the earlier sidebar samples. Final visual and input-activation acceptance remains with the user.

## Narrow vertical tab (preview 8)

The input tab remains on the explicitly confirmed right side. Its label is rotated **90 degrees counterclockwise**, inside a 48-by-160 design-pixel focus target at 1080p. The tab previously occupied 160 pixels horizontally; it now reserves only 48, returning another 112 pixels to the main area. Only the text is rotated, so the glass background and focus bounds remain a narrow vertical tab. Its accessible label remains Inputs / Ingressi. OK/open, Back/Left/close and the input launch route are unchanged.

Preview 8 was built, signed and installed in place on the SHARP. Screenshots verified the complete counterclockwise label and wider Home. Device checks confirmed the 48-by-160 focus bounds, focus alone leaving the drawer closed, OK opening HDMI 1, Back restoring the tab and Up/Down navigation to/from the header. Brightness grants were retained and the crash buffer was empty. No input was activated or audio/backlight level changed.

On 31 October 2026 the user personally confirmed the narrow tab and correct opening of the input panel, then requested that this Home be installed as the TV's default.

## Default Home installation (1.5.7-custom.1)

```powershell
./tools/build-preview.ps1 -AsHome
./tools/install-preview.ps1 -Serial <authorized-TV-serial> -Apk ./out/home/tvhome-custom-home.apk -MakeDefaultHome
```

The Home variant is labeled **TV Detox**, is not debuggable and includes the HOME/DEFAULT intent filter. It intentionally keeps `com.thewhitewolf1985.tvhome.preview` and the same signing key: updating in place retains the already tested app's identity, configuration and brightness permissions. The internal package suffix is retained for compatibility. The original upstream manifest and the official installed app remain separate.

Before promotion, the packaged runtime code and resources were compared byte-for-byte with the accepted preview 8. Only the manifest and APK signature metadata differed. On the SHARP the updated app retained UID 10064, `WRITE_SECURE_SETTINGS` and AppOps `WRITE_SETTINGS: allow`. The installer selected the Home with Android's `set-home-activity` command and verified the resulting HOME intent resolution. Launching TV settings and pressing Home returned to this package; a fresh UI dump and screenshot confirmed it. The crash buffer was empty. No input was activated, volume/backlight level changed or reboot performed.

For subsequent updates to the selected Home, build with `-AsHome` and use that APK. The installer refuses an ordinary preview without a HOME filter when this app is already the default launcher; that rejection was verified before installation. `-MakeDefaultHome` changes the default explicitly and is needed only when selecting it. There is no uninstall fallback, system-app conversion, disabled launcher or additional accessibility service. The local signing key must be preserved for future updates.

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
