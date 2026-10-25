package com.railreserve.ui.dashboard;

import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.EmptyState;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Optional;

/**
 * Cancel Ticket section: fetch a booking by PNR, show it read-only, then a
 * two-step cancel (fetch → confirm). Cancelled bookings refuse a second time
 * with a clear message.
 */
public class CancelPanel extends JPanel {

    private final DashboardHost host;
    private final ReservationService service = new ReservationService();
    private final RoundedTextField pnrField = new RoundedTextField(12);
    private final PrimaryButton fetch = new PrimaryButton("Fetch booking");
    private final JPanel resultSlot = new JPanel();
    private SecondaryButton cancelNow = new SecondaryButton("Cancel this ticket");
    private Reservation loaded;

    public CancelPanel(DashboardHost host) {
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
        resultSlot.add(new EmptyState(Icons.ticket(40), "Nothing selected",
                "Fetch a booking by PNR first, then cancel it in one confirmation."));
        c.gridy = 1;
        c.weighty = 1.0;
        center.add(resultSlot, c);
        add(center, BorderLayout.CENTER);
    }

    private JComponent heading() {
        JPanel head = new JPanel(new BorderLayout(0, Theme.SP4));
        head.setOpaque(false);
        JLabel title = new JLabel("Cancel Ticket");
        title.setFont(Theme.H1);
        title.setForeground(Theme.PRIMARY);
        JLabel sub = new JLabel("Cancellations are final in this demo — the status flips to CANCELLED.");
        sub.setFont(Theme.BODY);
        sub.setForeground(Theme.MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.CENTER);
        return head;
    }

    private JComponent searchCard() {
        Card card = new Card(new GridBagLayout());
        card.setPreferredSize(new Dimension(560, 84));
        card.setAlignmentX(0f);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        pnrField.setPreferredSize(new Dimension(300, 42));
        card.add(pnrField, c);
        fetch.addActionListener(buildFetchAction());
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        card.add(fetch, c);
        pnrField.addActionListener(buildFetchAction());
        return card;
    }

    private ActionListener buildFetchAction() {
        return e -> fetchBooking();
    }

    private void fetchBooking() {
        String pnr = pnrField.getText().strip();
        if (!pnr.matches("\\d{10}")) {
            renderMessage("A PNR is exactly 10 digits.", false);
            return;
        }
        fetch.setBusy(true);
        SwingWorker<Optional<Reservation>, Void> worker = new SwingWorker<>() {
            @Override
            protected Optional<Reservation> doInBackground() throws Exception {
                return service.findByPnr(pnr);
            }

            @Override
            protected void done() {
                fetch.setBusy(false);
                try {
                    show(get());
                } catch (Exception ex) {
                    renderMessage("Lookup failed — please try again in a moment.", false);
                }
            }
        };
        worker.execute();
    }

    private void show(Optional<Reservation> result) {
        if (result.isEmpty()) {
            renderMessage("No booking found for that PNR.", true);
            return;
        }
        loaded = result.get();
        resultSlot.removeAll();
        TicketCard card = new TicketCard(loaded);
        card.setPreferredSize(new Dimension(560, 330));
        card.setAlignmentX(0f);
        resultSlot.add(card);
        cancelNow = new SecondaryButton(loaded.isCancelled()
                ? "This booking is already CANCELLED"
                : "Cancel this ticket");
        cancelNow.setEnabled(!loaded.isCancelled());
        cancelNow.addActionListener(e -> confirmCancel());
        cancelNow.setAlignmentX(0f);
        resultSlot.add(javax.swing.Box.createVerticalStrut(Theme.SP12));
        resultSlot.add(cancelNow);
        resultSlot.revalidate();
        resultSlot.repaint();
    }

    private void renderMessage(String message, boolean notFound) {
        resultSlot.removeAll();
        resultSlot.add(notFound
                ? new EmptyState(Icons.search(40), "No booking found", message)
                : new JLabel(message));
        resultSlot.revalidate();
        resultSlot.repaint();
    }

    private void confirmCancel() {
        if (loaded == null || loaded.isCancelled()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + loaded.getPnr() + "?\n"
                        + loaded.getTrainNumber() + " " + loaded.getTrainName() + "\n"
                        + loaded.getSourceStation() + " → " + loaded.getDestinationStation()
                        + "\n\nCancellation is final in this demo.",
                "Confirm cancellation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        fetch.setBusy(true);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return service.cancel(loaded.getPnr());
            }

            @Override
            protected void done() {
                fetch.setBusy(false);
                try {
                    String error = get();
                    if (error == null) {
                        host.toast("PNR " + loaded.getPnr() + " cancelled.",
                                com.railreserve.ui.components.Toast.Kind.SUCCESS);
                    } else {
                        host.toast(error, com.railreserve.ui.components.Toast.Kind.ERROR);
                    }
                } catch (Exception ex) {
                    host.toast("Cancellation failed — please retry.",
                            com.railreserve.ui.components.Toast.Kind.ERROR);
                }
                fetchBooking(); // restore the fresh state so the user sees CANCELLED
            }
        };
        worker.execute();
    }
}
