# build-support/stubs

Compile-only stand-ins for the handful of Android framework classes Mint PWA uses. They let
`build.sh` compile with a plain JDK, without the Android SDK or Gradle. **They are never packaged:**
only classes under `src/` are dexed, and at runtime every reference resolves to the device's real
framework classes.

A wrong stub signature would compile but fail at runtime, so after building run:

    ./tools/check-api.sh

It extracts every `android.*` method and field the compiled app references and checks each one,
by exact signature, against a real platform `android.jar`. Members newer than that jar are listed
for review; members added to already-existing classes are allow-listed, with their API level, in
`tools/check-api/newer-members.txt` once checked against the published API reference.

If you add a framework call: add or extend the stub with the exact published signature, rebuild,
and run the check.
