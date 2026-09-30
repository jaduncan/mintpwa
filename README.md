# Mint PWA

Install web apps as real Android apps through **Android 17's system Web App Installer**: share a
page from your browser, and Android itself builds and installs the app.

> **Experimental.** The system installer is new and switched off by default. This app does nothing
> until you enable it (see below). Tested on GrapheneOS, Android 17 (`CP3A.260905.009`), with
> Vanadium 154 as the browser.

<!-- Screenshots: add docs/list.png and docs/share.png -->

## What it does

1. **Share a page to Mint PWA** from your browser. It fetches the page, finds its
   `<link rel="manifest">`, checks the manifest is reachable over HTTPS and is valid JSON, then calls
   `WebAppManager.install()` (public API, level 37).
2. **Android's Web App service** (`com.android.webapp.service`, in the `com.android.webapp` APEX)
   fetches the manifest, shows its own install prompt, generates an app from it, and installs it.
3. The **new app** appears in your launcher like any other. It opens the site through your default
   browser as a Trusted Web Activity.
4. **Open Mint PWA from the app drawer** to see every system-minted web app. Tap to open one; use ⋮
   or long-press for app info and uninstall.

Minted apps are ordinary installed packages: they survive clearing the browser's data, and follow
your choice of default browser.

## Requirements

- Android 17 (API 37) with the Web App Installer enabled:
  1. **Developer options → the Web App minter toggle** (global setting `enable_webapp_minter`).
  2. The module's feature flag, via ADB:

         adb shell device_config put lse_desktop_experience com.android.webapp.flags.enable_web_app_service_v2 true

  Check with `adb shell settings get global enable_webapp_minter` (expect `1`). These are
  experimental switches and may be reset by system updates.
- A browser to share pages from, set as the default browser for opening the minted apps.

## Install

Download the APK from Releases, or build it (below), then:

    adb install mintpwa-1.0.0.apk

## Why it appears as a browser

Android only accepts install requests from apps *eligible* for the browser role. Mint PWA declares
an `http`/`https` link handler for that reason alone, so it shows up under Default apps → Browser and
in "Open with" choices. Choosing it for a link simply tries to install that page.

## Limitations

- **Browser bar.** Minted apps open as a Trusted Web Activity. The site can't vouch for an app built
  on your device (Digital Asset Links), so the browser shows its URL bar. On sites that scroll the
  whole page it slides away; app-style sites with a fixed layout keep it.
- **Green bar in dark mode.** The system installer fills in the light-mode toolbar colour from the
  manifest's `theme_color`, but leaves the dark-mode colours at template placeholders
  (`#00FF00`). This is an Android bug, not something an installing app can influence.
- **Sites without a manifest** can't be installed this way (the system installer needs a manifest
  URL). Your browser's own *Add to Home screen* still works for them. Manifests injected by
  JavaScript after load aren't found either.
- **Browser integration.** Minted apps run as custom tabs of your browser: sites see a browser rather
  than standalone display mode, and notifications and permissions belong to the browser.
- **Logged-in manifests.** Mint PWA and the system installer fetch without your browser's cookies.

## Privacy

Mint PWA's only network access is fetching the page you share and its manifest. It sends nothing
anywhere else and collects no data. The app list reads your installed packages locally.

## Building

No Gradle or Android SDK needed; Debian/Ubuntu packages suffice:

    sudo apt install openjdk-21-jdk-headless dalvik-exchange aapt zipalign apksigner android-sdk-platform-23
    ./build.sh --debug          # throwaway debug key
    ./tools/check-api.sh        # verify every framework call against a real android.jar

Release builds need your own key:

    MINTPWA_KEYSTORE=~/keys/mintpwa.jks MINTPWA_KEY_ALIAS=mintpwa \
    MINTPWA_KS_PASS_FILE=~/keys/mintpwa.pass ./build.sh

Sources compile against small stubs in `build-support/stubs` (see its README). The icon is generated
by `python3 tools/genleaf.py`.

## Licence

Copyright (C) 2026 JD — <https://jad.fyi>

GNU General Public License v3.0 or later. See [LICENSE](LICENSE).
