package com.railreserve.ui.dashboard;

import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.EmptyState;
import com.railreserve.ui.components.FormLabel;
import com.railreserve.ui.components.Icons;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.StatusChip;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * My Bookings: the existing history table with filters (status, PNR, train
 * number), status color chips per row, a View-details dialog and a Cancel
 * action that reuses the confirm flow. All queries run in SwingWorkers.
 */
public class MyBookingsPanel extends JPanel {

    private static final String[] COLUMNS =
            {"PNR", "Train", "Route", "Journey date", "Class", "Status"};

    private final DashboardHost host;
    private final int userId;
    private final ReservationService service = new ReservationService();

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final List<Reservation> rows = new java.util.ArrayList<>();
    private final RoundedTextField pnrFilter = new RoundedTextField(9);
    private final RoundedTextField trainFilter = new RoundedTextField(7);
    private final javax.swing.JComboBox<String> statusFilter =
            new javax.swing.JComboBox<>(new String[]{"All", "CONFIRMED", "CANCELLED"});
    private final JPanel body = new JPanel(new BorderLayout());
    private JPanel actionsBarPanel;
    private final PrimaryButton refreshButton = new PrimaryButton("Refresh");
    private final SecondaryButton viewDetails = new SecondaryButton("View details");
    private final SecondaryButton cancelSelected = new SecondaryButton("Cancel booking");
    private final JLabel note = new JLabel(" ");

    public MyBookingsPanel(DashboardHost host, int userId) {
        this.host = host;
        this.userId = userId;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        add(header(), BorderLayout.NORTH);

        body.setOpaque(false);
        body.add(loading(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        actionsBarPanel = actionsBar();
        add(actionsBarPanel, BorderLayout.SOUTH);

        BookPanel.styleControl(statusFilter);
        refreshButton.addActionListener(e -> reload());
        viewDetails.addActionListener(e -> showDetails());
        cancelSelected.addActionListener(e -> cancelSelectedBooking());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncButtons();
            }
        });
        reload();
        syncButtons();
    }

    private JComponent header() {
        JPanel head = new JPanel(new BorderLayout(0, Theme.SP8));
        head.setOpaque(false);
        JLabel title = new JLabel("My Bookings");
        title.setFont(Theme.H1);
        title.setForeground(Theme.PRIMARY);
        head.add(title, BorderLayout.NORTH);
        head.add(filters(), BorderLayout.CENTER);
        return head;
    }

    private JComponent filters() {
        Card card = new Card(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Theme.SP12, Theme.SP8));
        card.setAlignmentX(0f);
        card.add(labeled("PNR", pnrFilter));
        card.add(labeled("Train number", trainFilter));
        card.add(labeled("Status", statusFilter));
        refreshButton.setFont(Theme.BODY);
        card.add(refreshButton);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.WEST);
        return wrap;
    }

    private JComponent labeled(String caption, JComponent control) {
        JPanel box = new JPanel(new java.awt.BorderLayout(0, Theme.SP2));
        box.setOpaque(false);
        FormLabel cap = new FormLabel(caption);
        box.add(cap, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(140, 32));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    private JComponent loading() {
        JPanel p = new JPanel(new java.awt.GridBagLayout());
        p.setOpaque(false);
        LoadingPlaceholder spinner = new LoadingPlaceholder("Loading your bookings…");
        p.add(spinner, new java.awt.GridBagConstraints());
        return p;
    }

    private JPanel actionsBar() {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, Theme.SP12, Theme.SP8));
        bar.setOpaque(false);
        viewDetails.setEnabled(false);
        cancelSelected.setEnabled(false);
        bar.add(viewDetails);
        bar.add(cancelSelected);
        note.setFont(Theme.SMALL);
        note.setForeground(Theme.MUTED);
        bar.add(note);
        return bar;
    }

    private void reload() {
        String statusText = String.valueOf(statusFilter.getSelectedItem());
        String status = "All".equals(statusText) ? null : statusText;
        Integer trainNo = trainFilter.getText().strip().matches("\\d{3,6}")
                ? Integer.valueOf(trainFilter.getText().strip()) : null;
        String pnr = pnrFilter.getText().strip().isBlank() ? null : pnrFilter.getText().strip();

        SwingWorker<List<Reservation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Reservation> doInBackground() throws Exception {
                return service.historyFiltered(userId, status, null, trainNo, pnr);
            }

            @Override
            protected void done() {
                try {
                    render(get());
                    note.setText(" ");
                } catch (Exception ex) {
                    body.removeAll();
                    body.add(new EmptyState(Icons.cancel(40), "Could not load bookings",
                            "The database is not reachable right now. Check the connection and press Refresh."));
                    body.revalidate();
                    body.repaint();
                }
            }
        };
        worker.execute();
    }

    private void render(List<Reservation> data) {
        rows.clear();
        rows.addAll(data);
        model.setRowCount(0);
        for (Reservation r : data) {
            model.addRow(new Object[]{
                    r.getPnr(),
                    r.getTrainNumber() + " · " + r.getTrainName(),
                    r.getSourceStation() + " → " + r.getDestinationStation(),
                    r.getJourneyDate() == null ? "—" : r.getJourneyDate(),
                    r.getClassType(),
                    r.getStatus()
            });
        }
        body.removeAll();
        if (data.isEmpty()) {
            SecondaryButton goBook = new SecondaryButton("Book a ticket");
            goBook.addActionListener(e -> host.showSection("book"));
            body.add(new EmptyState(Icons.ticket(40), "No bookings match",
                    data.isEmpty() && rows.isEmpty() && statusFilter.getSelectedIndex() == 0
                            ? "You haven't booked anything yet in this account."
                            : "No bookings match the current filters — widen them or press Refresh.",
                    goBook));
        } else {
            table.setRowHeight(42);
            table.setFont(Theme.BODY);
            table.setBackground(Theme.SURFACE);
            table.setForeground(Theme.TEXT);
            table.setSelectionBackground(Theme.BRAND_SOFT);
            table.setSelectionForeground(Theme.TEXT);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(0, 1));
            table.setGridColor(Theme.BORDER);
            table.getTableHeader().setFont(Theme.SMALL.deriveFont(Font.BOLD, 11.5f));
            table.getTableHeader().setBackground(Theme.SURFACE_ALT);
            table.getTableHeader().setForeground(Theme.MUTED);
            table.getTableHeader().setPreferredSize(new Dimension(0, 40));
            table.getTableHeader().setReorderingAllowed(false);
            rightAlignStatusColumn();
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            scroll.getViewport().setBackground(Theme.SURFACE);
            scroll.setPreferredSize(new Dimension(920, Math.min(420, data.size() * 40 + 40)));
            body.add(scroll, BorderLayout.CENTER);
        }
        body.revalidate();
        body.repaint();
        syncButtons();
    }

    /** Status column paints a colored pill like the chips elsewhere. */
    private void rightAlignStatusColumn() {
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value,
                                                                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(String.valueOf(value));
                label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                label.setOpaque(true);
                label.setFont(Theme.SMALL.deriveFont(Font.BOLD, 11f));
                String status = String.valueOf(value);
                if (isSelected) {
                    label.setBackground(t.getSelectionBackground());
                    label.setForeground(t.getSelectionForeground());
                } else if ("CONFIRMED".equalsIgnoreCase(status)) {
                    label.setBackground(Theme.SUCCESS_SOFT);
                    label.setForeground(Theme.SUCCESS);
                } else if ("CANCELLED".equalsIgnoreCase(status)) {
                    label.setBackground(Theme.ERROR_SOFT);
                    label.setForeground(Theme.ERROR);
                } else {
                    label.setBackground(t.getBackground());
                    label.setForeground(Theme.MUTED);
                }
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return label;
            }
        });
    }

    private void syncButtons() {
        int selected = table.getSelectedRow();
        boolean usable = selected >= 0 && selected < rows.size();
        viewDetails.setEnabled(usable);
        boolean cancellable = usable && !rows.get(selected).isCancelled();
        cancelSelected.setEnabled(cancellable);
        note.setText(cancellable ? " " : usable ? "This booking is already CANCELLED." : " ");
        if (actionsBarPanel != null) {
            actionsBarPanel.setVisible(!rows.isEmpty());
        }
    }

    private Reservation selected() {
        int i = table.getSelectedRow();
        return i >= 0 && i < rows.size() ? rows.get(i) : null;
    }

    private void showDetails() {
        Reservation r = selected();
        if (r == null) {
            return;
        }
        Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Booking " + r.getPnr(), java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.BACKGROUND);
        holder.setBorder(BorderFactory.createEmptyBorder(Theme.SP16, Theme.SP16, Theme.SP16, Theme.SP16));
        holder.add(new TicketCard(r), BorderLayout.CENTER);
        SecondaryButton close = new SecondaryButton("Close");
        close.addActionListener(e -> dialog.dispose());
        holder.add(close, BorderLayout.SOUTH);
        dialog.setContentPane(holder);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(460, 380));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private void cancelSelectedBooking() {
        Reservation r = selected();
        if (r == null || r.isCancelled()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + r.getPnr() + "?\n"
                        + r.getTrainNumber() + " " + r.getTrainName() + "\n"
                        + r.getSourceStation() + " → " + r.getDestinationStation()
                        + " on " + (r.getJourneyDate() == null ? "—" : r.getJourneyDate())
                        + "\n\nCancellation is final in this demo.",
                "Confirm cancellation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        String message = service.cancel(r.getPnr());
        if (message == null) {
            host.toast("PNR " + r.getPnr() + " cancelled.", com.railreserve.ui.components.Toast.Kind.SUCCESS);
        } else {
            host.toast(message, com.railreserve.ui.components.Toast.Kind.ERROR);
        }
        reload();
    }

    /** Convenience used by other sections after actions on bookings. */
    void refreshSoon() {
        reload();
    }
}
