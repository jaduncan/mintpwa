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
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * App-drawer entry: Material 3-styled list of web apps minted by Android's system Web App
 * Installer (installs initiated by com.android.webapp.service). Framework widgets only; colours
 * come from the system's dynamic (Material You) palette. Tap opens; ⋮ or long-press opens app info.
 */
public class Apps extends Activity {
    static final String T = "MintPWA";
    static final String MINTER = "com.android.webapp.service";
    static final int GET_META_DATA = 0x80;
    static final int THEME_DARK = 16974121;  // Theme_DeviceDefault_NoActionBar
    static final int THEME_LIGHT = 16974124; // Theme_DeviceDefault_Light_NoActionBar
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private boolean night;
    private float dp;
    private LinearLayout content;

    // Material 3 roles, resolved from the system dynamic palette (fallbacks = M3 baseline).
    private int bg, surfaceItem, onSurface, onSurfaceVariant, primaryContainer, onPrimaryContainer;

    @Override
    protected void onCreate(Bundle b) {
        night = (getResources().getConfiguration().uiMode & 0x30) == 0x20;
        setTheme(night ? THEME_DARK : THEME_LIGHT);
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        resolveColours();

        try { // dark icons on light bars in light mode
            int light = 8 | 16; // APPEARANCE_LIGHT_STATUS_BARS | APPEARANCE_LIGHT_NAVIGATION_BARS
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.setSystemBarsAppearance(night ? 0 : light, light);
        } catch (Throwable ignored) {
        }

        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        sv.setBackground(solid(bg, 0));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(16), px(8), px(16), px(32));
        sv.addView(content);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    // ---------- colours ----------

    private int sys(String name, int fallback) {
        try {
            int id = Class.forName("android.R$color").getField(name).getInt(null);
            return getColor(id);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private void resolveColours() {
        if (night) {
            bg = sys("system_neutral1_900", 0xFF141218);
            surfaceItem = sys("system_neutral2_800", 0xFF2B2930);
            onSurface = sys("system_neutral1_100", 0xFFE6E0E9);
            onSurfaceVariant = sys("system_neutral2_200", 0xFFCAC4D0);
            primaryContainer = sys("system_accent1_700", 0xFF4F378B);
            onPrimaryContainer = sys("system_accent1_100", 0xFFEADDFF);
        } else {
            bg = sys("system_neutral1_10", 0xFFFEF7FF);
            surfaceItem = sys("system_neutral2_50", 0xFFF3EDF7);
            onSurface = sys("system_neutral1_900", 0xFF1D1B20);
            onSurfaceVariant = sys("system_neutral2_700", 0xFF49454F);
            primaryContainer = sys("system_accent1_100", 0xFFEADDFF);
            onPrimaryContainer = sys("system_accent1_900", 0xFF21005D);
        }
    }

    private static int alpha(int colour, int a) { return (colour & 0x00FFFFFF) | (a << 24); }

    // ---------- drawing helpers ----------

    private int px(float d) { return Math.round(d * dp); }

    private GradientDrawable solid(int colour, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(colour);
        g.setCornerRadius(px(radiusDp));
        return g;
    }

    private GradientDrawable shaped(int colour, float top, float bottom) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(colour);
        float t = px(top), bt = px(bottom);
        g.setCornerRadii(new float[] {t, t, t, t, bt, bt, bt, bt});
        return g;
    }

    private RippleDrawable ripple(GradientDrawable shape, GradientDrawable mask) {
        return new RippleDrawable(ColorStateList.valueOf(alpha(onSurface, 0x1F)), shape, mask);
    }

    private TextView text(String s, float sp, int colour, String family) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(colour);
        v.setTypeface(Typeface.create(family, Typeface.NORMAL));
        return v;
    }

    // ---------- data ----------

    static final class Minted {
        ApplicationInfo ai;
        String label;
        String site;
    }

    private List<Minted> findMinted() {
        PackageManager pm = getPackageManager();
        List<Minted> out = new ArrayList<Minted>();
        for (ApplicationInfo ai : pm.getInstalledApplications(GET_META_DATA)) {
            try {
                InstallSourceInfo src = pm.getInstallSourceInfo(ai.packageName);
                if (src == null || !MINTER.equals(src.getInitiatingPackageName())) continue;
            } catch (Exception e) {
                continue;
            }
            Minted m = new Minted();
            m.ai = ai;
            m.label = String.valueOf(pm.getApplicationLabel(ai));
            String manifest = ai.metaData != null ? ai.metaData.getString("web_manifest_url") : null;
            try {
                m.site = manifest != null ? new URL(manifest).getHost() : ai.packageName;
            } catch (Exception e) {
                m.site = manifest;
            }
            out.add(m);
        }
        Collections.sort(out, new Comparator<Minted>() {
            public int compare(Minted a, Minted c) { return a.label.compareToIgnoreCase(c.label); }
        });
        return out;
    }

    // ---------- UI ----------

    private void render() {
        content.removeAllViews();
        List<Minted> apps;
        String error = null;
        try {
            apps = findMinted();
        } catch (Throwable t) {
            Log.e(T, "listing failed", t);
            apps = new ArrayList<Minted>();
            error = String.valueOf(t);
        }

        // Large top app bar: headline + supporting count
        TextView title = text("Mint PWA", 32f, onSurface, "sans-serif");
        title.setPadding(px(8), px(56), px(8), px(4));
        content.addView(title);
        int n = apps.size();
        TextView count = text(n == 0 ? "No web apps yet" : n + (n == 1 ? " web app" : " web apps"),
                14f, onSurfaceVariant, "sans-serif");
        count.setPadding(px(8), 0, px(8), px(20));
        content.addView(count);

        // Tonal hint card
        TextView hint = text("To add one, open the site in Vanadium, then Share \u2192 Mint PWA.",
                14f, onPrimaryContainer, "sans-serif");
        hint.setBackground(solid(primaryContainer, 20));
        hint.setPadding(px(20), px(16), px(20), px(16));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(MATCH, WRAP);
        hlp.setMargins(0, 0, 0, px(20));
        content.addView(hint, hlp);

        if (error != null) {
            TextView e = text("Couldn\u2019t list apps: " + error, 14f, onSurfaceVariant, "sans-serif");
            e.setPadding(px(8), 0, px(8), 0);
            content.addView(e);
            return;
        }

        // Segmented list: 24dp outer corners, 4dp inner, 2dp gaps
        PackageManager pm = getPackageManager();
        for (int i = 0; i < n; i++) {
            final Minted m = apps.get(i);
            float top = i == 0 ? 24 : 4, bottom = i == n - 1 ? 24 : 4;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(16); // CENTER_VERTICAL
            row.setMinimumHeight(px(72));
            row.setPadding(px(16), px(12), px(4), px(12));
            row.setBackground(ripple(shaped(surfaceItem, top, bottom), shaped(0xFFFFFFFF, top, bottom)));

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(pm.getApplicationIcon(m.ai));
            row.addView(icon, new LinearLayout.LayoutParams(px(40), px(40)));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(px(16), 0, px(8), 0);
            TextView name = text(m.label, 16f, onSurface, "sans-serif-medium");
            name.setSingleLine(true);
            TextView site = text(m.site, 14f, onSurfaceVariant, "sans-serif");
            site.setSingleLine(true);
            col.addView(name);
            col.addView(site);
            row.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));

            TextView more = text("\u22ee", 22f, onSurfaceVariant, "sans-serif");
            more.setGravity(17); // CENTER
            more.setContentDescription("App info");
            more.setBackground(new RippleDrawable(
                    ColorStateList.valueOf(alpha(onSurface, 0x1F)), null, null)); // unbounded
            row.addView(more, new LinearLayout.LayoutParams(px(48), px(48)));

            final View.OnClickListener info = new View.OnClickListener() {
                public void onClick(View v) { openInfo(m.ai.packageName); }
            };
            more.setOnClickListener(info);
            row.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) { openInfo(m.ai.packageName); return true; }
            });
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(m.ai.packageName);
                    if (launch != null) startActivity(launch);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.setMargins(0, i == 0 ? 0 : px(2), 0, 0);
            content.addView(row, lp);
        }
    }

    private void openInfo(String pkg) {
        Intent i = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse("package:" + pkg));
        i.addFlags(0x10000000);
        startActivity(i);
    }
}
