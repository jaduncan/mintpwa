/*
 * Mint PWA — install web apps through Android's system Web App Installer.
 * Copyright (C) 2026 JD <https://jad.fyi>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package fyi.jad.mintpwa;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.webapp.WebAppInstallRequest;
import android.content.pm.webapp.WebAppManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.ObjIntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Share a page URL to this app (or VIEW it, or pass --es manifest via adb). It finds the page's
 * web app manifest, validates it, and asks Android's system Web App Installer
 * (android.content.pm.webapp.WebAppManager) to mint and install it. Logs under tag MintPWA.
 */
public class Main extends Activity {
    static final String T = "MintPWA";
    // A mainstream mobile-browser User-Agent: some sites send different (or no) <head> markup to
    // unknown clients. The exact version is not significant.
    static final String UA =
            "Mozilla/5.0 (Linux; Android 17) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/154.0.0.0 Mobile Safari/537.36";
    static final int MAX_BYTES = 1 << 20;

    private TextView tv;
    private final StringBuilder out = new StringBuilder();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Mint PWA");
        tv = new TextView(this);
        tv.setPadding(48, 32, 48, 48);
        tv.setTextIsSelectable(true);
        tv.setTextSize(14f);
        setContentView(tv);
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        out.setLength(0);
        handle(i);
    }

    private void say(final String s) {
        Log.i(T, s);
        runOnUiThread(new Runnable() {
            public void run() {
                out.append(s).append('\n');
                tv.setText(out.toString());
            }
        });
    }

    private void handle(Intent i) {
        final String manifestExtra = i.getStringExtra("manifest");
        String candidate = i.getStringExtra("android.intent.extra.TEXT");
        if (candidate == null) candidate = i.getDataString();
        final String pageUrl = firstUrl(candidate);

        if (manifestExtra == null && pageUrl == null) {
            say("Share a web page to \u201cMint PWA\u201d from Vanadium.\n\n"
                    + "(adb: am start -n fyi.jad.mintpwa/.Main --es manifest <url>)");
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                try {
                    resolveAndInstall(pageUrl, manifestExtra);
                } catch (Throwable t) {
                    Log.e(T, "failed", t);
                    say("\u2717 " + t);
                }
            }
        }).start();
    }

    private void resolveAndInstall(String pageUrl, String manifestUrl) throws Exception {
        if (manifestUrl == null) {
            say("Page: " + pageUrl);
            Fetched page = fetch(pageUrl);
            if (page.code != 200) {
                say("\u2717 Page returned HTTP " + page.code);
                return;
            }
            manifestUrl = findManifest(page.body, page.finalUrl);
            if (manifestUrl == null) {
                say("\u2717 No <link rel=\"manifest\"> on this page \u2014 it isn't an installable PWA.");
                return;
            }
        }
        say("Manifest: " + manifestUrl);
        if (!manifestUrl.startsWith("https://")) {
            say("\u2717 Manifest is not HTTPS.");
            return;
        }

        Fetched m = fetch(manifestUrl);
        if (m.code != 200) {
            say("\u2717 Manifest returned HTTP " + m.code);
            return;
        }
        if (!m.finalUrl.equals(manifestUrl)) {
            say("\u26a0 Manifest redirected to " + m.finalUrl + " \u2014 using that URL.");
            manifestUrl = m.finalUrl;
        }
        if (!manifestUrl.startsWith("https://")) {
            say("\u2717 Manifest is not HTTPS.");
            return;
        }
        String title;
        try {
            JSONObject j = new JSONObject(m.body);
            title = j.optString("name", "");
            if (title == null || title.isEmpty()) title = j.optString("short_name", "");
        } catch (Exception e) {
            say("\u2717 Manifest is not valid JSON.");
            return;
        }
        if (title == null || title.isEmpty()) title = new URL(manifestUrl).getHost();
        say("Name: " + title);

        final String fTitle = title;
        final String fManifest = manifestUrl;
        runOnUiThread(new Runnable() {
            public void run() {
                install(fTitle, fManifest);
            }
        });
    }

    private void install(String title, String manifestUrl) {
        try {
            WebAppManager wm = getSystemService(WebAppManager.class);
            if (wm == null) {
                say("\u2717 WebAppManager unavailable on this device.");
                return;
            }
            if (!wm.isAvailable()) {
                say("\u2717 Web App service not available (Developer option / flag off?).");
                return;
            }
            say("Asking system installer\u2026");
            wm.install(
                    new WebAppInstallRequest.Builder(title, manifestUrl).build(),
                    getMainExecutor(),
                    new ObjIntConsumer<String>() {
                        @Override
                        public void accept(String pkg, int code) {
                            String n = resultName(code);
                            if ("RESULT_SUCCESS".equals(n)) {
                                say("\u2713 Installed: " + pkg);
                            } else {
                                say("\u2717 " + n + " (" + code + ")" + (pkg != null ? " " + pkg : ""));
                            }
                        }
                    });
        } catch (Throwable t) {
            Log.e(T, "install failed", t);
            say("\u2717 " + t);
        }
    }

    /** Map a result code to its RESULT_* constant name by reflection on the real platform class. */
    static String resultName(int code) {
        try {
            for (Field f : WebAppInstallRequest.class.getFields()) {
                if (f.getName().startsWith("RESULT_") && f.getType() == int.class
                        && f.getInt(null) == code) {
                    return f.getName();
                }
            }
        } catch (Throwable ignored) {
        }
        return "RESULT_" + code;
    }

    // ---------- helpers ----------

    static final Pattern URL_RE = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    static final Pattern LINK_RE = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    static final Pattern BASE_RE = Pattern.compile("<base\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    static final Pattern ATTR_RE = Pattern.compile(
            "([a-zA-Z][\\w:-]*)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s\"'>]+)");

    static String firstUrl(String s) {
        if (s == null) return null;
        Matcher m = URL_RE.matcher(s);
        return m.find() ? m.group() : null;
    }

    static String attr(String tag, String name) {
        Matcher m = ATTR_RE.matcher(tag);
        while (m.find()) {
            if (m.group(1).equalsIgnoreCase(name)) {
                String v = m.group(2);
                if (v.startsWith("\"") || v.startsWith("'")) v = v.substring(1, v.length() - 1);
                return unescape(v.trim());
            }
        }
        return null;
    }

    static String unescape(String s) {
        return s.replace("&amp;", "&").replace("&#38;", "&").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&#x2F;", "/").replace("&#47;", "/");
    }

    static String findManifest(String html, String pageUrl) throws Exception {
        URL base = new URL(pageUrl);
        Matcher bm = BASE_RE.matcher(html);
        if (bm.find()) {
            String bh = attr(bm.group(), "href");
            if (bh != null && !bh.isEmpty()) base = new URL(base, bh);
        }
        Matcher lm = LINK_RE.matcher(html);
        while (lm.find()) {
            String tag = lm.group();
            String rel = attr(tag, "rel");
            if (rel == null) continue;
            for (String tok : rel.trim().split("\\s+")) {
                if (tok.equalsIgnoreCase("manifest")) {
                    String href = attr(tag, "href");
                    if (href != null && !href.isEmpty()) return new URL(base, href).toString();
                }
            }
        }
        return null;
    }

    static final class Fetched {
        int code;
        String finalUrl;
        String body;
    }

    static Fetched fetch(String url) throws Exception {
        Fetched f = new Fetched();
        String current = url;
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(current).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "text/html,application/manifest+json,application/json,*/*");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
                String next = new URL(new URL(current), c.getHeaderField("Location")).toString();
                c.disconnect();
                if (current.startsWith("https://") && !next.startsWith("https://")) {
                    throw new Exception("Refusing redirect from HTTPS to " + next);
                }
                current = next;
                continue;
            }
            f.code = code;
            f.finalUrl = current;
            if (code == 200) {
                InputStream in = c.getInputStream();
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0 && bo.size() < MAX_BYTES) bo.write(buf, 0, n);
                in.close();
                f.body = new String(bo.toByteArray(), "UTF-8");
            }
            c.disconnect();
            return f;
        }
        throw new Exception("Too many redirects");
    }
}
