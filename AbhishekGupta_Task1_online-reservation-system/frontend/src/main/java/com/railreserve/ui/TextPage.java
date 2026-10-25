package com.railreserve.ui;

import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Simple information page (About / Help / Privacy) on a white card. */
public class TextPage extends JPanel {

    /** @param paragraphs body text; empty lines become visual spacing */
    public TextPage(String title, String... paragraphs) {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        SecondaryButton back = SecondaryButton.link("\u2190 Back to Home");
        back.addActionListener(e -> {
            var frame = getTopLevelAncestor();
            if (frame instanceof com.railreserve.ui.AppFrame appFrame) {
                appFrame.show(AppFrame.HOME);
            }
        });
        back.setAlignmentX(0f);
        column.add(back);
        column.add(Box.createVerticalStrut(Theme.SP16));

        Card card = new Card(new BorderLayout(0, Theme.SP12));
        card.setAlignmentX(0f);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        JLabel heading = new JLabel(title);
        heading.setFont(Theme.H1);
        heading.setForeground(Theme.TEXT);
        card.add(heading, BorderLayout.NORTH);

        JTextArea body = new JTextArea(String.join("\n\n", paragraphs));
        body.setFont(Theme.BODY);
        body.setForeground(Theme.MUTED);
        body.setBackground(Theme.SURFACE);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setCaretPosition(0);
        body.setBorder(BorderFactory.createEmptyBorder(Theme.SP8, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        card.add(scroll, BorderLayout.CENTER);

        column.add(card);
        add(column, BorderLayout.CENTER);
    }
}
