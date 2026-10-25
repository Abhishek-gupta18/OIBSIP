package com.railreserve.ui;

import com.railreserve.ui.theme.Theme;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A theme flip has to rebuild the active screen. That only happens when the
 * shell's listener survives garbage collection, which regressed once while the
 * registration was an unrooted lambda.
 */
class AppFrameThemeTest {

    @AfterEach
    void restoreLightPalette() {
        Theme.setDark(false);
    }

    @Test
    void themeFlipRebuildsTheActiveScreen() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "A JFrame needs a display; skipped in headless CI");

        boolean startDark = Theme.isDark();
        AppFrame frame = new AppFrame();
        AtomicInteger builds = new AtomicInteger();
        frame.register(AppFrame.HOME, () -> {
            builds.incrementAndGet();
            return new JPanel();
        });

        frame.show(AppFrame.HOME);
        assertEquals(1, builds.get(), "first visit builds the screen once");

        Theme.setDark(!startDark);
        assertEquals(2, builds.get(), "flipping the theme rebuilds the active screen");

        Theme.setDark(startDark);
        assertEquals(3, builds.get(), "flipping back rebuilds it again");
        assertEquals(AppFrame.HOME, frame.activeScreen());

        frame.dispose();
    }

    @Test
    void themeListenerSurvivesGarbageCollection() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "A JFrame needs a display; skipped in headless CI");

        boolean startDark = Theme.isDark();
        AppFrame frame = new AppFrame();
        AtomicInteger builds = new AtomicInteger();
        frame.register(AppFrame.HOME, () -> {
            builds.incrementAndGet();
            return new JPanel();
        });
        frame.show(AppFrame.HOME);

        System.gc();
        Thread.sleep(50);

        Theme.setDark(!startDark);
        assertEquals(2, builds.get(), "the shell listener must still be reachable");

        frame.dispose();
    }

    @Test
    void missingScreenDoesNotThrow() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "A JFrame needs a display; skipped in headless CI");

        AppFrame frame = new AppFrame();
        frame.show("nope");
        assertEquals(null, frame.activePanel());
        assertNotNull(frame.layeredPane());
        frame.dispose();
    }
}
