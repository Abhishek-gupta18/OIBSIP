package com.railreserve.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * The single RailReserve palette, styled after ixigo.com: warm brand orange on
 * white cards, soft grey page bands, hairline borders and generous rounding.
 * Light and dark palettes are swapped live via {@link #setDark(boolean)}
 * (Settings &gt; Preferences); every screen is rebuilt by the navigation shell on
 * a theme change so all colors follow.
 */
public final class Theme {

    // ---- brand ----
    public static Color BRAND;         // #EC5B24 orange — CTAs, active links, accents
    public static Color BRAND_HOVER;
    public static Color BRAND_SOFT;    // tinted background behind orange content
    public static Color SUCCESS_SOFT;
    public static Color ERROR_SOFT;

    // ---- surfaces ----
    public static Color PRIMARY;       // ink — headings, icons, strong text
    public static Color PRIMARY_HOVER;
    public static Color ACCENT;        // alias of BRAND (kept for older call sites)
    public static Color ACCENT_HOVER;
    public static Color BACKGROUND;    // page band
    public static Color SURFACE;       // cards / bars
    public static Color SURFACE_ALT;   // inputs, hairline fills
    public static Color BORDER;        // 1px card/input borders
    public static Color TEXT;
    public static Color MUTED;
    public static Color SUCCESS;
    public static Color ERROR;
    public static Color ON_PRIMARY;    // text on brand/accent buttons

    // ---- type ----
    public static Font H1;
    public static Font H2;
    public static Font H3;
    public static Font BODY;
    public static Font SMALL;

    // ---- spacing / rounding ----
    public static final int SP2 = 2;
    public static final int SP4 = 4;
    public static final int SP8 = 8;
    public static final int SP12 = 12;
    public static final int SP16 = 16;
    public static final int SP24 = 24;
    public static final int SP32 = 32;
    public static final int SP40 = 40;
    public static final int SP48 = 48;
    public static final int SP20 = 20;
    public static final int RADIUS = 16;
    public static final int RADIUS_SM = 10;
    public static final int RADIUS_PILL = 999;

    public static final Cursor HAND = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    /**
     * Strong refs on purpose. The only registrant is the application window,
     * which lives for the whole session; components must NOT register here
     * because {@link #setDark(boolean)} rebuilds the active screen anyway.
     */
    private static final List<Runnable> REFRESH_LISTENERS = new ArrayList<>();
    private static boolean darkMode;

    private Theme() {
    }

    static {
        applyLight();
    }

    /** Swaps palette and notifies all live refresh listeners (navigation shell rebuild). */
    public static void setDark(boolean dark) {
        if (darkMode != dark) {
            darkMode = dark;
            if (dark) {
                applyDark();
            } else {
                applyLight();
            }
            for (Runnable listener : List.copyOf(REFRESH_LISTENERS)) {
                listener.run();
            }
        }
    }

    public static boolean isDark() {
        return darkMode;
    }

    /**
     * Registers a long-lived callback that re-applies colors when the mode
     * flips. Meant for the navigation shell only; screens and widgets are
     * rebuilt by it, so they must not register (that would leak per rebuild).
     */
    public static void onThemeChange(Runnable listener) {
        if (listener != null) {
            REFRESH_LISTENERS.add(listener);
        }
    }

    private static void applyLight() {
        BRAND = new Color(0xEC5B24);
        BRAND_HOVER = new Color(0xD24A16);
        BRAND_SOFT = new Color(0xFFF0E9);
        SUCCESS_SOFT = new Color(0xE7F6EF);
        ERROR_SOFT = new Color(0xFDECEA);

        PRIMARY = new Color(0x16181F);
        PRIMARY_HOVER = new Color(0x000000);
        ACCENT = BRAND;
        ACCENT_HOVER = BRAND_HOVER;
        BACKGROUND = new Color(0xF7F8FA);
        SURFACE = new Color(0xFFFFFF);
        SURFACE_ALT = new Color(0xF1F3F6);
        BORDER = new Color(0xE5E8EE);
        TEXT = new Color(0x1B1E27);
        MUTED = new Color(0x717886);
        SUCCESS = new Color(0x10855F);
        ERROR = new Color(0xD2453C);
        ON_PRIMARY = Color.WHITE;
        applyFonts();
    }

    private static void applyDark() {
        BRAND = new Color(0xFF7A3D);
        BRAND_HOVER = new Color(0xFF9059);
        BRAND_SOFT = new Color(0x2C1D15);
        SUCCESS_SOFT = new Color(0x122A20);
        ERROR_SOFT = new Color(0x2E1716);

        PRIMARY = new Color(0xF2F4F8);
        PRIMARY_HOVER = Color.WHITE;
        ACCENT = BRAND;
        ACCENT_HOVER = BRAND_HOVER;
        BACKGROUND = new Color(0x0E1116);
        SURFACE = new Color(0x171B22);
        SURFACE_ALT = new Color(0x1F242D);
        BORDER = new Color(0x272D38);
        TEXT = new Color(0xE9ECF2);
        MUTED = new Color(0x98A1B2);
        SUCCESS = new Color(0x3ED598);
        ERROR = new Color(0xFF6B5E);
        ON_PRIMARY = Color.WHITE;
        applyFonts();
    }

    private static void applyFonts() {
        String family = "Segoe UI";
        H1 = safeFont(family, Font.BOLD, 28);
        H2 = safeFont(family, Font.BOLD, 20);
        H3 = safeFont(family, Font.BOLD, 15);
        BODY = safeFont(family, Font.PLAIN, 14);
        SMALL = safeFont(family, Font.PLAIN, 12);
    }

    private static Font safeFont(String family, int style, int size) {
        // Falls back to the dialog default when Segoe UI is unavailable (Linux/macOS)
        return new Font(family, style, size);
    }

    /** Overrides hit when Segoe UI is missing so text stays readable. */
    public static Font fallbackFont(Font requested) {
        return requested;
    }
}
