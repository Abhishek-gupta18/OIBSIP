package com.railreserve.ui.dashboard;

import com.railreserve.dao.TrainDAO;
import com.railreserve.model.Reservation;
import com.railreserve.service.ReservationService;
import com.railreserve.ui.SearchIntent;
import com.railreserve.ui.components.Card;
import com.railreserve.ui.components.FormLabel;
import com.railreserve.ui.components.PrimaryButton;
import com.railreserve.ui.components.RoundedTextField;
import com.railreserve.ui.components.SecondaryButton;
import com.railreserve.ui.components.TicketCard;
import com.railreserve.ui.components.Toast;
import com.railreserve.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Book Ticket section: one polished form (passenger, train number with
 * name auto-fill, class, date, stations). Reuses ReservationService
 * validation + booking (duplicate-PNR retry inside the service). On success
 * the view flips to a confirmation ticket card with "Book another" and
 * "View my bookings".
 */
public class BookPanel extends JPanel {

    private final DashboardHost host;
    private final int userId;
    private final ReservationService service = new ReservationService();
    private final TrainDAO trainDAO = new TrainDAO();
    private final Map<Integer, String> trainNames = new HashMap<>();

    private final RoundedTextField trainNumber = new RoundedTextField(8);
    private final RoundedTextField passenger = new RoundedTextField(18);
    private final RoundedTextField from = new RoundedTextField(12);
    private final RoundedTextField to = new RoundedTextField(12);
    private final JComboBox<String> classCombo = new JComboBox<>(ReservationService.CLASS_TYPES);
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
    private final JLabel trainNameLabel = new JLabel("Enter a train number — the name fills itself");
    private final PrimaryButton confirmButton = new PrimaryButton("Confirm booking");

    private final FormLabel passengerRow = new FormLabel("Passenger name");
    private final FormLabel trainRow = new FormLabel("Train number");
    private final FormLabel classRow = new FormLabel("Class");
    private final FormLabel dateRow = new FormLabel("Journey date");
    private final FormLabel fromRow = new FormLabel("From");
    private final FormLabel toRow = new FormLabel("To");

    private final JPanel formView = form();
    private final JPanel confirmationView = new JPanel(new GridBagLayout());
    private final JPanel cardHolder = new JPanel(new GridBagLayout());

    public BookPanel(DashboardHost host, int userId, SearchIntent prefill) {
        this.host = host;
        this.userId = userId;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        add(buildHeading("Book a ticket",
                "Every confirmed booking gets a unique 10-digit PNR."), BorderLayout.NORTH);

        cardHolder.setOpaque(false);
        cardHolder.add(formView, new GridBagConstraints());
        add(cardHolder, BorderLayout.CENTER);

        loadTrains();
        applyPreferenceDefaults();
        if (prefill != null) {
            from.setText(prefill.fromDisplay());
            to.setText(prefill.toDisplay());
            if (prefill.classType() != null) {
                classCombo.setSelectedItem(prefill.classType());
            }
            trainNumber.requestFocusInWindow();
        }
        confirmButton.addActionListener(e -> submit());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd MMM yyyy"));
        styleControl(classCombo);
        styleControl(dateSpinner);
    }

    /** Rounded, surface-colored combo/spinner to match the text inputs. */
    static void styleControl(JComponent control) {
        control.setFont(Theme.BODY);
        control.setBackground(Theme.SURFACE);
        control.setForeground(Theme.TEXT);
        control.setBorder(BorderFactory.createCompoundBorder(
                new com.railreserve.ui.components.RoundedBorder(Theme.RADIUS_SM, Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, Theme.SP4, 0, 0)));
        if (control instanceof JSpinner spinner
                && spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setBackground(Theme.SURFACE);
            editor.getTextField().setForeground(Theme.TEXT);
            editor.getTextField().setBorder(BorderFactory.createEmptyBorder(Theme.SP4,
                    Theme.SP8, Theme.SP4, Theme.SP8));
        }
    }

    /** Enter submits the form once the panel is mounted in the window. */
    @Override
    public void addNotify() {
        super.addNotify();
        javax.swing.SwingUtilities.getRootPane(this).setDefaultButton(confirmButton);
    }

    /** Default class / source station come from the saved preferences file. */
    private void applyPreferenceDefaults() {
        com.railreserve.service.PreferencesService.Prefs prefs =
                new com.railreserve.service.PreferencesService().load();
        classCombo.setSelectedItem(prefs.defaultClass());
        if (prefs.defaultSource() != null && !prefs.defaultSource().isBlank()) {
            from.setText(prefs.defaultSource());
        }
    }

    private JComponent buildHeading(String title, String subtitle) {
        JPanel head = new JPanel(new BorderLayout(0, Theme.SP4));
        head.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.H1);
        t.setForeground(Theme.PRIMARY);
        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.BODY);
        s.setForeground(Theme.MUTED);
        head.add(t, BorderLayout.NORTH);
        head.add(s, BorderLayout.CENTER);
        return head;
    }

    private JPanel form() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);

        Card card = new Card(new GridBagLayout());
        card.setPreferredSize(new Dimension(720, 400));
        card.setBorder(BorderFactory.createEmptyBorder(Theme.SP24, Theme.SP32, Theme.SP24, Theme.SP32));

        trainNameLabel.setFont(Theme.SMALL.deriveFont(Font.PLAIN, 12f));
        trainNameLabel.setForeground(Theme.BRAND);

        card.add(field(passengerRow, passenger), cell(0, 0));
        card.add(trainField(), cell(1, 0));
        card.add(comboBox(classRow, classCombo), cell(0, 1));
        card.add(comboField(dateRow, dateSpinner), cell(1, 1));
        card.add(field(fromRow, from), cell(0, 2));
        card.add(field(toRow, to), cell(1, 2));

        GridBagConstraints c = cell(0, 3);
        c.gridwidth = 2;
        c.insets = new Insets(Theme.SP16, Theme.SP8, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        confirmButton.setPreferredSize(new Dimension(200, 44));
        card.add(confirmButton, c);

        outer.add(card, new GridBagConstraints());
        return outer;
    }

    /** Two-column form cell. */
    private GridBagConstraints cell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(y == 0 ? 0 : Theme.SP12, x == 0 ? 0 : Theme.SP16, 0, 0);
        return c;
    }

    private JComponent field(FormLabel label, JComponent control) {
        JPanel box = new JPanel(new BorderLayout(0, Theme.SP4));
        box.setOpaque(false);
        box.add(label, BorderLayout.NORTH);
        control.setPreferredSize(new Dimension(300, 42));
        box.add(control, BorderLayout.CENTER);
        return box;
    }

    private JComponent trainField() {
        JComponent base = field(trainRow, trainNumber);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        base.setAlignmentX(0f);
        trainNameLabel.setAlignmentX(0f);
        box.add(base);
        box.add(trainNameLabel);
        return box;
    }

    private JComponent comboField(FormLabel label, JComponent control) {
        return field(label, control);
    }

    private JComponent comboBox(FormLabel label, JComponent control) {
        return field(label, control);
    }

    /** Loads the seeded trains once so the number field can auto-name itself. */
    private void loadTrains() {
        SwingWorker<Map<Integer, String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<Integer, String> doInBackground() {
                Map<Integer, String> map = new HashMap<>();
                try {
                    for (var t : trainDAO.findAll()) {
                        map.put(t.getTrainNumber(), t.getTrainName());
                    }
                } catch (SQLException ignored) {
                    // handled at submit time with a friendly message
                }
                return map;
            }

            @Override
            protected void done() {
                try {
                    trainNames.putAll(get());
                    trainNameLabel.setText(trainNames.isEmpty()
                            ? "Could not preload the train list — the name fills after you type a number."
                            : "Enter a train number — the name fills itself");
                } catch (Exception ignored) {
                    // keep the placeholder text
                }
            }
        };
        worker.execute();
        trainNumber.onTextChange(() -> {
            String typed = trainNumber.getText().strip();
            Integer key = null;
            try {
                if (typed.matches("\\d{3,6}")) {
                    key = Integer.valueOf(typed);
                }
            } catch (NumberFormatException ignored) {
                // out of range — leave the hint as-is
            }
            String name = key == null ? null : trainNames.get(key);
            trainNameLabel.setText(name != null ? "✓  " + name : "No seeded train matches that number yet");
        });
    }

    private void submit() {
        LocalDate date = ((Date) dateSpinner.getValue())
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String name = passenger.getText().strip();
        String number = trainNumber.getText().strip();
        String trainName = trainNameFor(number);
        String classType = String.valueOf(classCombo.getSelectedItem());
        String source = (from.getText().startsWith("(") && from.getText().contains(")"))
                ? from.getText().substring(from.getText().indexOf(')') + 1).strip()
                : from.getText().strip();
        String destination = (to.getText().startsWith("(") && to.getText().contains(")"))
                ? to.getText().substring(to.getText().indexOf(')') + 1).strip()
                : to.getText().strip();

        String error = service.validate(name, number, trainName, classType, date, source, destination);
        if (error != null) {
            showInlineError(error);
            host.toast(error, Toast.Kind.ERROR);
            return;
        }

        confirmButton.setBusy(true);
        SwingWorker<ReservationService.BookingResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ReservationService.BookingResult doInBackground() {
                return service.book(userId, name, number, trainName, classType, date, source, destination);
            }

            @Override
            protected void done() {
                confirmButton.setBusy(false);
                try {
                    ReservationService.BookingResult result = get();
                    if (result.success()) {
                        showConfirmation(result.reservation());
                        host.toast("Booking confirmed — PNR " + result.reservation().getPnr(),
                                Toast.Kind.SUCCESS);
                    } else {
                        showInlineError(result.errorMessage());
                        host.toast(result.errorMessage(), Toast.Kind.ERROR);
                    }
                } catch (Exception ex) {
                    host.toast("Booking failed unexpectedly — please retry.", Toast.Kind.ERROR);
                }
            }
        };
        worker.execute();
    }

    private String trainNameFor(String number) {
        Integer key;
        try {
            key = number.matches("\\d{3,6}") ? Integer.valueOf(number) : null;
        } catch (NumberFormatException ignored) {
            key = null; // cannot happen after matches(), defensive
        }
        String name = key == null ? null : trainNames.get(key);
        return name == null ? "" : name;
    }

    private void showInlineError(String message) {
        trainNameLabel.setText(message);
        trainNameLabel.setForeground(Theme.ERROR);
        passengerRow.setError(null);
    }

    private void showConfirmation(Reservation r) {
        confirmationView.removeAll();
        confirmationView.setLayout(new GridBagLayout());
        confirmationView.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel done = new JLabel("Your ticket is confirmed");
        done.setFont(Theme.H1);
        done.setForeground(Theme.SUCCESS);
        confirmationView.add(done, c);

        TicketCard ticket = new TicketCard(r);
        ticket.setPreferredSize(new Dimension(560, 330));
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        confirmationView.add(ticket, c);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, Theme.SP16, 0));
        actions.setOpaque(false);
        PrimaryButton another = new PrimaryButton("Book another");
        another.addActionListener(e -> backToForm());
        SecondaryButton seeBookings = new SecondaryButton("View my bookings");
        seeBookings.addActionListener(e -> host.showSection("bookings"));
        actions.add(another);
        actions.add(seeBookings);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(Theme.SP16, 0, 0, 0);
        confirmationView.add(actions, c);

        cardHolder.removeAll();
        cardHolder.add(confirmationView, new GridBagConstraints());
        revalidate();
        repaint();
    }

    private void backToForm() {
        passenger.setText("");
        trainNumber.setText("");
        trainNameLabel.setForeground(Theme.BRAND);
        trainNameLabel.setText("Enter a train number — the name fills itself");
        cardHolder.removeAll();
        cardHolder.add(formView, new GridBagConstraints());
        revalidate();
        repaint();
    }
}
