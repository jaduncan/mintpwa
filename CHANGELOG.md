# Changelog

## 1.0.0 — 2026-09-30

First public release (package `fyi.jad.mintpwa`).

- Share a web page from your browser to **Mint PWA**: it finds the page's web app manifest, validates
  it, and asks Android's system Web App Installer (`WebAppManager.install()`) to build and install
  the app.
- App-drawer entry listing every system-minted web app (installs initiated by
  `com.android.webapp.service`), in a Material 3 style using the system's dynamic colours. Tap to
  open; ⋮ or long-press for app info / uninstall.
- Manifest discovery handles relative and absolute `href`s, `<base href>`, attribute order and
  quoting, multi-token `rel`, and HTML entities; redirects are followed but HTTPS→HTTP downgrades are
  refused, and the manifest must be HTTPS.
- Result codes are reported by their platform `RESULT_*` names.
