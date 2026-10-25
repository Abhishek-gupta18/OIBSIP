package com.railreserve.ui.dashboard;

import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.EmptyState;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JOptionPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * PNR Status section: type a PNR, get a ticket-style result card; CONFIRMED
 * bookings can be cancelled right from the card via a confirm dialog.
 */
public class PnrPanel extends JPanel {

    private final DashboardHost host;
    private final ReservationService service = new ReservationService();
    private final JSpinner pnrSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9_999_999_999L, 1));
    private final RoundedTextField pnrField = new RoundedTextField(12);
    private final PrimaryButton check = new PrimaryButton("Check status");
    private final JPanel resultSlot = new JPanel();
    private final JLabel hint = new JLabel(" ");

    public PnrPanel(DashboardHost host) {
        this.host = host;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        add(heading(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        center.add(searchCard(), c);

        resultSlot.setOpaque(false);
        resultSlot.setLayout(new BoxLayout(resultSlot, BoxLayout.Y_AXIS));
        resultSlot.setBorder(BorderFactory.createEmptyBorder(Theme.SP16, 0, 0, 0));
        resultSlot.add(new EmptyState(Icons.search(40), "Check any PNR",
                "Enter a 10-digit PNR above — CONFIRMED tickets can even be cancelled from here."));
        c.gridy = 1;
        c.weighty = 1.0;
        center.add(resultSlot, c);

        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.ERROR);
        c.gridy = 2;
        c.weighty = 0;
        c.insets = new Insets(Theme.SP8, 0, 0, 0);
        center.add(hint, c);
        add(center, BorderLayout.CENTER);
        check.addActionListener(e -> lookup());
    }

    private JComponent heading() {
        JPanel head = new JPanel(new BorderLayout(0, Theme.SP4));
        head.setOpaque(false);
        JLabel title = new JLabel("PNR Status");
        title.setFont(Theme.H1);
        title.setForeground(Theme.PRIMARY);
        JLabel sub = new JLabel("Live status for any booking in this database.");
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        return head;
    }

    private JComponent searchCard() {
        Card card = new Card(new GridBagLayout());
        card.setPreferredSize(new Dimension(560, 70));
        card.setAlignmentX(0f);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        pnrField.setPreferredSize(new Dimension(300, 42));
        card.add(pnrField, c);
        PrimaryButton btn = check;
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        card.add(btn, c);
        btn.addActionListener(e -> lookup());
        pnrField.addActionListener(e -> lookup());
        return card;
    }

    private void lookup() {
        String pnr = pnrField.getText().strip();
        if (!pnr.matches("\\d{10}")) {
            hint.setText("A PNR is exactly 10 digits.");
            return;
        }
        hint.setText(" ");
        check.setBusy(true);
        SwingWorker<Optional<Reservation>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<Reservation> doInBackground() throws Exception {
                return service.findByPnr(pnr);
            }

            @Override
            protected void done() {
                check.setBusy(false);
                try {
                    render(get());
                } catch (Exception ex) {
                    hint.setText("Lookup failed — please try again in a moment.");
                }
            }
        };
        worker.execute();
    }

    private void render(Optional<Reservation> result) {
        resultSlot.removeAll();
        if (result.isEmpty()) {
            resultSlot.add(new EmptyState(Icons.search(40), "No booking found",
                    "That PNR is not in this demo database — double-check the digits."));
        } else {
            Reservation r = result.get();
            TicketCard card = new TicketCard(r, this::maybeCancel);
            card.setPreferredSize(new Dimension(560, 340));
            card.setAlignmentX(0f);
            resultSlot.add(card);
        }
        resultSlot.revalidate();
        resultSlot.repaint();
    }

    /** Trailing action from the ticket card. */
    private void maybeCancel(Reservation r) {
        if (r.isCancelled()) {
            host.toast("This booking is already CANCELLED.", com.railreserve.ui.components.Toast.Kind.INFO);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + r.getPnr() + "? Cancellation is final in this demo.",
                "Confirm cancellation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String error = service.cancel(r.getPnr());
        if (error == null) {
            host.toast("PNR " + r.getPnr() + " cancelled.", com.railreserve.ui.components.Toast.Kind.SUCCESS);
        } else {
            host.toast(error, com.railreserve.ui.components.Toast.Kind.ERROR);
        }
        lookup(); // refresh the card from the DB
    }
}
