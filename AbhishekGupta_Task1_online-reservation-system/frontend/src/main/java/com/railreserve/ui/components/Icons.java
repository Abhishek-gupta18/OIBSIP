package com.railreserve.ui.components;

import com.railreserve.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Simple vector icons drawn with Java2D — bundled in code, so no image assets
 * and no network access are needed. Colors are read from {@link Theme} at paint
 * time, so icons follow light/dark switches automatically.
 */
public final class Icons {

    private Icons() {
    }

    /** Train front — brand mark. */
    public static Icon train(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.6f, s / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Float(s * .12f, s * .10f, s * .76f, s * .60f, s * .22f, s * .22f));
            g.fill(new RoundRectangle2D.Float(s * .22f, s * .22f, s * .24f, s * .20f, s * .06f, s * .06f));
            g.fill(new RoundRectangle2D.Float(s * .54f, s * .22f, s * .24f, s * .20f, s * .06f, s * .06f));
            g.draw(new Line2D.Float(s * .18f, s * .70f, s * .82f, s * .70f));
            g.fill(new Ellipse2D.Float(s * .24f, s * .76f, s * .14f, s * .14f));
            g.fill(new Ellipse2D.Float(s * .62f, s * .76f, s * .14f, s * .14f));
        });
    }

    /** Magnifier — search / simple booking. */
    public static Icon search(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.8f, s / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Ellipse2D.Float(s * .16f, s * .12f, s * .52f, s * .52f));
            g.draw(new Line2D.Float(s * .62f, s * .62f, s * .86f, s * .86f));
        });
    }

    /** Ticket with a perforated notch — PNR / bookings. */
    public static Icon ticket(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.6f, s / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Float(s * .10f, s * .24f, s * .80f, s * .52f, s * .16f, s * .16f));
            float dash = Math.max(2f, s / 12f);
            g.setStroke(new BasicStroke(Math.max(1.2f, s / 16f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{dash, dash}, 0f));
            g.draw(new Line2D.Float(s * .38f, s * .26f, s * .38f, s * .74f));
            g.fill(new Ellipse2D.Float(s * .55f, s * .40f, s * .16f, s * .16f));
        });
    }

    /** Lock — secure login. */
    public static Icon lock(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.8f, s / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Float(s * .26f, s * .12f, s * .48f, s * .48f, 0, 180, Arc2D.OPEN));
            g.fill(new RoundRectangle2D.Float(s * .18f, s * .42f, s * .64f, s * .46f, s * .14f, s * .14f));
            Color hole = Theme.SURFACE;
            g.setColor(hole);
            g.fill(new Ellipse2D.Float(s * .45f, s * .55f, s * .10f, s * .16f));
        });
    }

    /** Crossed circle — cancellation. */
    public static Icon cancel(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.8f, s / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Ellipse2D.Float(s * .12f, s * .12f, s * .76f, s * .76f));
            g.draw(new Line2D.Float(s * .32f, s * .32f, s * .68f, s * .68f));
            g.draw(new Line2D.Float(s * .68f, s * .32f, s * .32f, s * .68f));
        });
    }

    /** Calendar page — journey date. */
    public static Icon calendar(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.6f, s / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Float(s * .14f, s * .18f, s * .72f, s * .66f, s * .16f, s * .16f));
            g.draw(new Line2D.Float(s * .14f, s * .40f, s * .86f, s * .40f));
            g.draw(new Line2D.Float(s * .32f, s * .10f, s * .32f, s * .24f));
            g.draw(new Line2D.Float(s * .68f, s * .10f, s * .68f, s * .24f));
            g.fill(new Ellipse2D.Float(s * .30f, s * .52f, s * .10f, s * .10f));
            g.fill(new Ellipse2D.Float(s * .60f, s * .52f, s * .10f, s * .10f));
        });
    }

    /** Shield with a tick — trust / security strip. */
    public static Icon shield(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.5f, s / 13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D.Float shield = new java.awt.geom.Path2D.Float();
            shield.moveTo(s * .5f, s * .08f);
            shield.lineTo(s * .86f, s * .24f);
            shield.lineTo(s * .86f, s * .52f);
            shield.curveTo(s * .86f, s * .74f, s * .70f, s * .86f, s * .5f, s * .94f);
            shield.curveTo(s * .30f, s * .86f, s * .14f, s * .74f, s * .14f, s * .52f);
            shield.lineTo(s * .14f, s * .24f);
            shield.closePath();
            g.draw(shield);
            java.awt.geom.Path2D.Float tick = new java.awt.geom.Path2D.Float();
            tick.moveTo(s * .34f, s * .50f);
            tick.lineTo(s * .46f, s * .63f);
            tick.lineTo(s * .68f, s * .38f);
            g.draw(tick);
        });
    }

    /** Lightning bolt — instant confirmation. */
    public static Icon bolt(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            java.awt.geom.Path2D.Float bolt = new java.awt.geom.Path2D.Float();
            bolt.moveTo(s * .56f, s * .08f);
            bolt.lineTo(s * .24f, s * .54f);
            bolt.lineTo(s * .46f, s * .54f);
            bolt.lineTo(s * .40f, s * .92f);
            bolt.lineTo(s * .76f, s * .42f);
            bolt.lineTo(s * .53f, s * .42f);
            bolt.closePath();
            g.fill(bolt);
        });
    }

    /** Small chevron used by the account dropdown. */
    public static Icon chevronDown(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.6f, s / 5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Float(s * .22f, s * .40f, s * .5f, s * .68f));
            g.draw(new Line2D.Float(s * .78f, s * .40f, s * .5f, s * .68f));
        });
    }

    /** Person silhouette — account menu. */
    public static Icon user(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.fill(new Ellipse2D.Float(s * .34f, s * .12f, s * .32f, s * .32f));
            java.awt.geom.Path2D.Float body = new java.awt.geom.Path2D.Float();
            body.moveTo(s * .16f, s * .90f);
            body.curveTo(s * .18f, s * .60f, s * .34f, s * .52f, s * .50f, s * .52f);
            body.curveTo(s * .66f, s * .52f, s * .82f, s * .60f, s * .84f, s * .90f);
            body.closePath();
            g.fill(body);
        });
    }

    /** Crescent moon — dark mode. */
    public static Icon moon(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            java.awt.geom.Area crescent = new java.awt.geom.Area(
                    new Ellipse2D.Float(s * .16f, s * .12f, s * .68f, s * .68f));
            crescent.subtract(new java.awt.geom.Area(
                    new Ellipse2D.Float(s * .34f, s * .02f, s * .66f, s * .66f)));
            g.setColor(color);
            g.fill(crescent);
        });
    }

    /** Sun — light mode. */
    public static Icon sun(int size) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.3f, s / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.fill(new Ellipse2D.Float(s * .34f, s * .34f, s * .32f, s * .32f));
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                float x1 = (float) (s * .5f + Math.cos(a) * s * .30f);
                float y1 = (float) (s * .5f + Math.sin(a) * s * .30f);
                float x2 = (float) (s * .5f + Math.cos(a) * s * .42f);
                float y2 = (float) (s * .5f + Math.sin(a) * s * .42f);
                g.draw(new Line2D.Float(x1, y1, x2, y2));
            }
        });
    }

    /** Eye for the password reveal toggle; closed draws a slash. */
    public static Icon eye(int size, boolean open) {
        return new VectorIcon(size, (g, s, color) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(Math.max(1.4f, s / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D.Float lid = new java.awt.geom.Path2D.Float();
            lid.moveTo(s * .10f, s * .50f);
            lid.quadTo(s * .50f, s * .10f, s * .90f, s * .50f);
            lid.quadTo(s * .50f, s * .90f, s * .10f, s * .50f);
            lid.closePath();
            g.draw(lid);
            if (open) {
                g.fill(new Ellipse2D.Float(s * .40f, s * .40f, s * .20f, s * .20f));
            } else {
                g.draw(new Line2D.Float(s * .18f, s * .82f, s * .82f, s * .18f));
            }
        });
    }

    @FunctionalInterface
    private interface Painter {
        void paint(Graphics2D g2, int size, Color color);
    }

    private static final class VectorIcon implements Icon {
        private final int size;
        private final Painter painter;

        VectorIcon(int size, Painter painter) {
            this.size = size;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            painter.paint(g2, size, Theme.BRAND);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
