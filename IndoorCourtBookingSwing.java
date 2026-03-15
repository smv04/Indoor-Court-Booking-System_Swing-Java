import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class IndoorCourtBookingSwing extends JFrame {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> COURTS = Arrays.asList(
            "Badminton Court A",
            "Badminton Court B",
            "Basketball Court",
            "Tennis Court",
            "Futsal Court"
    );
    private static final List<String> TIME_SLOTS = Arrays.asList(
            "06:00 - 07:00",
            "07:00 - 08:00",
            "08:00 - 09:00",
            "09:00 - 10:00",
            "16:00 - 17:00",
            "17:00 - 18:00",
            "18:00 - 19:00",
            "19:00 - 20:00",
            "20:00 - 21:00"
    );
    private static final Path STORAGE_PATH = Path.of("court_bookings.csv");

    private static final Color BG_MAIN = new Color(238, 245, 252);
    private static final Color BG_CARD = new Color(255, 255, 255);
    private static final Color BORDER = new Color(212, 224, 238);
    private static final Color STATUS_OK = new Color(21, 92, 131);
    private static final Color STATUS_ERR = new Color(183, 28, 28);

    private final JComboBox<String> courtCombo = new JComboBox<>(COURTS.toArray(new String[0]));
    private final JTextField dateField = new JTextField(10);
    private final JButton pickDateButton = new JButton("Select Date");
    private final JPanel slotsPanel = new JPanel(new GridLayout(3, 3, 8, 8));
    private final JTextField nameField = new JTextField();
    private final JLabel nameErrorLabel = new JLabel(" ");
    private final JTextField registerNoField = new JTextField();
    private final JLabel registerNoErrorLabel = new JLabel(" ");
    private final JTextField emailField = new JTextField();
    private final JLabel emailErrorLabel = new JLabel(" ");
    private final JTextField phoneField = new JTextField();
    private final JLabel phoneErrorLabel = new JLabel(" ");
    private final JTextField notesField = new JTextField();
    private final JLabel statusLabel = new JLabel("Select a date, court, and time slot.");
    private final JLabel totalBookingsValue = new JLabel("0");
    private final JLabel todayBookingsValue = new JLabel("0");
    private final JLabel availableSlotsValue = new JLabel(String.valueOf(TIME_SLOTS.size()));
    private final JLabel selectedCourtValue = new JLabel();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Date", "Court", "Slot", "Name", "Register No", "Email", "Phone", "Notes"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable bookingsTable = new JTable(tableModel);

    private final Map<String, JButton> slotButtons = new LinkedHashMap<>();
    private final List<Booking> bookings = new ArrayList<>();
    private LocalDate selectedDate = LocalDate.now();
    private String selectedSlot;

    public IndoorCourtBookingSwing() {
        super("Indoor Court Booking System");
        setupUi();
        installFieldValidation();
        loadBookings();
        refreshTable();
        refreshSlotButtons();
        refreshStats();
    }

    private void setupUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 760));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(BG_MAIN);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildHeaderPanel(), BorderLayout.NORTH);

        JPanel main = new JPanel(new GridLayout(1, 2, 12, 12));
        main.setBackground(BG_MAIN);
        main.add(buildBookingPanel());
        main.add(buildListPanel());
        root.add(main, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(8, 94, 124));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(6, 73, 97)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel title = new JLabel("Indoor Court Booking");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Interactive scheduling with live availability");
        subtitle.setForeground(new Color(219, 240, 250));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(subtitle, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBookingPanel() {
        JPanel panel = makeCardPanel(new BorderLayout(0, 12));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(BG_CARD);

        JLabel title = new JLabel("Create Booking");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        top.add(title);
        top.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Pick date + court, choose a slot, and confirm.");
        subtitle.setForeground(new Color(87, 99, 117));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(12));

        JPanel topControls = new JPanel(new GridLayout(2, 2, 8, 8));
        topControls.setBackground(BG_CARD);
        topControls.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), "Schedule"));
        topControls.add(new JLabel("Date"));
        topControls.add(new JLabel("Court"));

        dateField.setEditable(false);
        dateField.setText(selectedDate.format(DATE_FORMAT));
        styleInput(dateField);
        stylePaddedSecondaryButton(pickDateButton);
        pickDateButton.addActionListener(e -> openCalendarDialog());

        JPanel datePanel = new JPanel(new BorderLayout(6, 0));
        datePanel.setBackground(BG_CARD);
        datePanel.add(dateField, BorderLayout.CENTER);
        datePanel.add(pickDateButton, BorderLayout.EAST);
        topControls.add(datePanel);

        styleInput(courtCombo);
        topControls.add(courtCombo);
        top.add(topControls);
        panel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG_CARD);

        JLabel slotLabel = new JLabel("Available Slots");
        slotLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        center.add(slotLabel);
        center.add(Box.createVerticalStrut(8));

        slotsPanel.setBackground(BG_CARD);
        for (String slot : TIME_SLOTS) {
            JButton slotButton = new JButton(slot);
            slotButton.setMargin(new Insets(8, 8, 8, 8));
            slotButton.setFocusPainted(false);
            slotButton.addActionListener(e -> selectSlot(slot));
            slotButtons.put(slot, slotButton);
            slotsPanel.add(slotButton);
        }
        center.add(slotsPanel);
        center.add(Box.createVerticalStrut(12));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), "Player Details"));
        form.add(new JLabel("Name"));
        styleInlineErrorLabel(nameErrorLabel);
        form.add(nameErrorLabel);
        styleInput(nameField);
        form.add(nameField);
        form.add(Box.createVerticalStrut(4));
        form.add(new JLabel("Register No"));
        styleInlineErrorLabel(registerNoErrorLabel);
        form.add(registerNoErrorLabel);
        styleInput(registerNoField);
        form.add(registerNoField);
        form.add(Box.createVerticalStrut(4));
        form.add(new JLabel("Email"));
        styleInlineErrorLabel(emailErrorLabel);
        form.add(emailErrorLabel);
        styleInput(emailField);
        form.add(emailField);
        form.add(Box.createVerticalStrut(4));
        form.add(new JLabel("Phone (10 digits)"));
        styleInlineErrorLabel(phoneErrorLabel);
        form.add(phoneErrorLabel);
        styleInput(phoneField);
        form.add(phoneField);
        form.add(Box.createVerticalStrut(4));
        form.add(new JLabel("Notes (optional)"));
        styleInput(notesField);
        form.add(notesField);

        JScrollPane playerDetailsScroll = new JScrollPane(form);
        playerDetailsScroll.setBorder(null);
        playerDetailsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        playerDetailsScroll.getVerticalScrollBar().setUnitIncrement(16);
        playerDetailsScroll.setPreferredSize(new Dimension(0, 210));
        center.add(playerDetailsScroll);

        panel.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBackground(BG_CARD);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setBackground(BG_CARD);

        JButton bookButton = new JButton("Confirm Booking");
        stylePrimaryButton(bookButton);
        bookButton.addActionListener(e -> handleBooking());

        JButton resetButton = new JButton("Reset");
        stylePaddedSecondaryButton(resetButton);
        resetButton.addActionListener(e -> clearInput(false));

        actionRow.add(bookButton);
        actionRow.add(resetButton);
        bottom.add(actionRow);
        bottom.add(Box.createVerticalStrut(8));

        statusLabel.setForeground(STATUS_OK);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        bottom.add(statusLabel);
        panel.add(bottom, BorderLayout.SOUTH);

        courtCombo.addActionListener(e -> {
            selectedSlot = null;
            refreshTable();
            refreshSlotButtons();
            refreshStats();
        });

        return panel;
    }

    private JPanel buildListPanel() {
        JPanel panel = makeCardPanel(new BorderLayout(0, 8));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(BG_CARD);

        JLabel heading = new JLabel("Bookings Dashboard");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 21));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(heading);
        top.add(Box.createVerticalStrut(8));

        JPanel selectedCourtRow = new JPanel(new BorderLayout(8, 0));
        selectedCourtRow.setBackground(new Color(248, 252, 255));
        selectedCourtRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 228, 240)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        JLabel selectedCourtLabel = new JLabel("Court");
        selectedCourtLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectedCourtLabel.setForeground(new Color(89, 105, 122));
        selectedCourtRow.add(selectedCourtLabel, BorderLayout.WEST);
        selectedCourtValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        selectedCourtValue.setForeground(new Color(18, 92, 144));
        selectedCourtRow.add(selectedCourtValue, BorderLayout.CENTER);
        selectedCourtRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedCourtRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, selectedCourtRow.getPreferredSize().height));
        top.add(selectedCourtRow);
        top.add(Box.createVerticalStrut(8));

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 8, 8));
        statsRow.setBackground(BG_CARD);
        statsRow.add(makeStatCard("Total Bookings", totalBookingsValue, new Color(18, 92, 144)));
        statsRow.add(makeStatCard("Today's Bookings", todayBookingsValue, new Color(20, 125, 98)));
        statsRow.add(makeStatCard("Available Slots", availableSlotsValue, new Color(146, 88, 27)));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, statsRow.getPreferredSize().height));
        top.add(statsRow);

        panel.add(top, BorderLayout.NORTH);

        bookingsTable.getColumnModel().getColumn(0).setMinWidth(0);
        bookingsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        bookingsTable.setRowHeight(28);
        bookingsTable.setGridColor(new Color(228, 235, 244));
        bookingsTable.setSelectionBackground(new Color(219, 241, 252));
        bookingsTable.setSelectionForeground(new Color(20, 35, 52));
        bookingsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bookingsTable.getTableHeader().setBackground(new Color(227, 239, 250));
        bookingsTable.getTableHeader().setForeground(new Color(31, 71, 103));
        bookingsTable.setDefaultRenderer(Object.class, new ZebraRenderer());

        JScrollPane scrollPane = new JScrollPane(bookingsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottom.setBackground(BG_CARD);

        JButton cancelSelected = new JButton("Cancel Selected");
        stylePaddedSecondaryButton(cancelSelected);
        cancelSelected.addActionListener(e -> cancelSelectedBooking());

        JButton clearAll = new JButton("Clear All");
        stylePaddedDangerButton(clearAll);
        clearAll.addActionListener(e -> clearAllBookings());

        bottom.add(cancelSelected);
        bottom.add(clearAll);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel makeStatCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(new Color(248, 252, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(216, 228, 240)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(89, 105, 122));
        card.add(titleLabel, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(valueColor);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel makeCardPanel(BorderLayout layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    private void styleInput(Component input) {
        if (input instanceof JTextField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(204, 215, 229)),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        } else if (input instanceof JComboBox<?> combo) {
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        }
    }

    private void stylePrimaryButton(JButton button) {
        button.setBackground(new Color(0, 102, 204));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 72, 144)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setFocusPainted(false);
    }

    private void styleSecondaryButton(JButton button) {
        button.setBackground(new Color(241, 246, 252));
        button.setForeground(new Color(44, 77, 105));
        button.setBorder(BorderFactory.createLineBorder(new Color(182, 201, 221)));
        button.setFocusPainted(false);
    }

    private void stylePaddedSecondaryButton(JButton button) {
        styleSecondaryButton(button);
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(182, 201, 221)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void stylePaddedDangerButton(JButton button) {
        button.setFocusPainted(false);
        button.setForeground(new Color(165, 18, 45));
        button.setBackground(new Color(255, 243, 245));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(236, 168, 182)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void styleInlineErrorLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(STATUS_ERR);
        label.setVisible(false);
    }

    private void installFieldValidation() {
        addBlurValidation(nameField, this::validateName);
        addBlurValidation(registerNoField, this::validateRegisterNo);
        addBlurValidation(emailField, this::validateEmail);
        addBlurValidation(phoneField, this::validatePhone);
    }

    private void addBlurValidation(JTextField field, Function<String, String> validator) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String error = validator.apply(field.getText().trim());
                if (error == null) {
                    clearFieldError(field);
                } else {
                    showFieldError(field, error);
                }
            }
        });
    }

    private String validateName(String value) {
        return value.isEmpty() ? "Name is required." : null;
    }

    private String validateRegisterNo(String value) {
        return value.isEmpty() ? "Register No is required." : null;
    }

    private String validateEmail(String value) {
        if (value.isEmpty()) {
            return "Email is required.";
        }
        return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$") ? null : "Enter a valid email address.";
    }

    private String validatePhone(String value) {
        if (value.isEmpty()) {
            return "Phone is required.";
        }
        return value.matches("\\d{10}") ? null : "Phone must be exactly 10 digits.";
    }

    private void showFieldError(JTextField field, String errorMessage) {
        JLabel errorLabel = getErrorLabel(field);
        field.setToolTipText(errorMessage);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(183, 28, 28)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        errorLabel.setText(errorMessage);
        errorLabel.setVisible(true);
    }

    private void clearFieldError(JTextField field) {
        JLabel errorLabel = getErrorLabel(field);
        field.setToolTipText(null);
        styleInput(field);
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private JLabel getErrorLabel(JTextField field) {
        if (field == nameField) {
            return nameErrorLabel;
        }
        if (field == registerNoField) {
            return registerNoErrorLabel;
        }
        if (field == emailField) {
            return emailErrorLabel;
        }
        if (field == phoneField) {
            return phoneErrorLabel;
        }
        return nameErrorLabel;
    }

    private void selectSlot(String slot) {
        if (isSlotBooked(selectedDate, getSelectedCourt(), slot)) {
            setStatus("Slot is already booked.", true);
            return;
        }
        selectedSlot = slot;
        refreshSlotButtons();
        setStatus("Selected slot: " + slot, false);
    }

    private void refreshSlotButtons() {
        String selectedCourt = getSelectedCourt();
        int bookedCount = 0;
        for (Map.Entry<String, JButton> entry : slotButtons.entrySet()) {
            String slot = entry.getKey();
            JButton button = entry.getValue();
            boolean booked = isSlotBooked(selectedDate, selectedCourt, slot);

            if (booked) {
                bookedCount++;
                button.setEnabled(false);
                button.setBackground(new Color(232, 236, 242));
                button.setForeground(new Color(125, 133, 145));
                button.setBorder(BorderFactory.createLineBorder(new Color(220, 227, 237)));
            } else {
                button.setEnabled(true);
                boolean isSelected = Objects.equals(slot, selectedSlot);
                button.setBackground(isSelected ? new Color(218, 246, 234) : new Color(250, 252, 255));
                button.setForeground(isSelected ? new Color(13, 112, 76) : new Color(32, 44, 60));
                button.setBorder(BorderFactory.createLineBorder(isSelected ? new Color(91, 193, 151) : new Color(209, 220, 232)));
            }
        }
        availableSlotsValue.setText(String.valueOf(TIME_SLOTS.size() - bookedCount));
    }

    private void handleBooking() {
        String name = nameField.getText().trim();
        String registerNo = registerNoField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String notes = notesField.getText().trim();
        LocalDate date = selectedDate;
        String court = getSelectedCourt();

        if (selectedSlot == null) {
            setStatus("Please select a time slot.", true);
            return;
        }
        String nameError = validateName(name);
        if (nameError != null) {
            showFieldError(nameField, nameError);
            setStatus(nameError, true);
            return;
        }
        clearFieldError(nameField);
        String registerNoError = validateRegisterNo(registerNo);
        if (registerNoError != null) {
            showFieldError(registerNoField, registerNoError);
            setStatus(registerNoError, true);
            return;
        }
        clearFieldError(registerNoField);
        String emailError = validateEmail(email);
        if (emailError != null) {
            showFieldError(emailField, emailError);
            setStatus(emailError, true);
            return;
        }
        clearFieldError(emailField);
        String phoneError = validatePhone(phone);
        if (phoneError != null) {
            showFieldError(phoneField, phoneError);
            setStatus(phoneError, true);
            return;
        }
        clearFieldError(phoneField);
        if (date.isBefore(LocalDate.now())) {
            setStatus("You cannot book a court before today.", true);
            return;
        }
        if (isSlotBooked(date, court, selectedSlot)) {
            setStatus("This slot was just booked. Pick another.", true);
            refreshSlotButtons();
            return;
        }

        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                date,
                court,
                selectedSlot,
                name,
                registerNo,
                email,
                phone,
                notes
        );
        bookings.add(booking);
        saveBookings();
        refreshTable();
        clearInput(true);
        refreshStats();
        setStatus("Booking confirmed for " + date + " (" + booking.slot + ").", false);
    }

    private void cancelSelectedBooking() {
        int selectedRow = bookingsTable.getSelectedRow();
        if (selectedRow < 0) {
            setStatus("Select a booking to cancel.", true);
            return;
        }
        String bookingId = String.valueOf(tableModel.getValueAt(selectedRow, 0));
        bookings.removeIf(b -> b.id.equals(bookingId));
        saveBookings();
        refreshTable();
        refreshSlotButtons();
        refreshStats();
        setStatus("Selected booking cancelled.", false);
    }

    private void clearAllBookings() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete all bookings?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        bookings.clear();
        saveBookings();
        refreshTable();
        refreshSlotButtons();
        refreshStats();
        setStatus("All bookings cleared.", false);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        String selectedCourt = getSelectedCourt();
        bookings.stream()
                .filter(b -> b.court.equals(selectedCourt))
                .sorted((a, b) -> {
                    int byDate = a.date.compareTo(b.date);
                    if (byDate != 0) {
                        return byDate;
                    }
                    int byCourt = a.court.compareToIgnoreCase(b.court);
                    if (byCourt != 0) {
                        return byCourt;
                    }
                    return a.slot.compareToIgnoreCase(b.slot);
                })
                .forEach(b -> tableModel.addRow(new Object[]{
                        b.id, b.date.format(DATE_FORMAT), b.court, b.slot, b.name, b.registerNo, b.email, b.phone, b.notes
                }));
        refreshStats();
    }

    private void clearInput(boolean resetSlot) {
        nameField.setText("");
        registerNoField.setText("");
        emailField.setText("");
        phoneField.setText("");
        notesField.setText("");
        if (resetSlot) {
            selectedSlot = null;
            refreshSlotButtons();
        }
    }

    private boolean isSlotBooked(LocalDate date, String court, String slot) {
        return bookings.stream().anyMatch(b ->
                b.date.equals(date) &&
                b.court.equals(court) &&
                b.slot.equals(slot)
        );
    }

    private String getSelectedCourt() {
        return String.valueOf(courtCombo.getSelectedItem());
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? STATUS_ERR : STATUS_OK);
    }

    private void refreshStats() {
        String selectedCourt = getSelectedCourt();
        selectedCourtValue.setText(selectedCourt);
        long totalForCourt = bookings.stream()
                .filter(b -> b.court.equals(selectedCourt))
                .count();
        totalBookingsValue.setText(String.valueOf(totalForCourt));
        long todaysBookings = bookings.stream()
                .filter(b -> b.court.equals(selectedCourt))
                .filter(b -> b.date.equals(LocalDate.now()))
                .count();
        todayBookingsValue.setText(String.valueOf(todaysBookings));
    }

    private void openCalendarDialog() {
        LocalDate picked = showCalendarDialog(selectedDate);
        if (picked != null) {
            if (picked.isBefore(LocalDate.now())) {
                setStatus("Please select today or a future date.", true);
                return;
            }
            selectedDate = picked;
            dateField.setText(selectedDate.format(DATE_FORMAT));
            selectedSlot = null;
            refreshSlotButtons();
            refreshStats();
        }
    }

    private LocalDate showCalendarDialog(LocalDate initialDate) {
        JDialog dialog = new JDialog(this, "Choose Date", true);
        dialog.setSize(340, 360);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(BG_CARD);

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        styleSecondaryButton(prev);
        styleSecondaryButton(next);

        JPanel nav = new JPanel(new BorderLayout(8, 0));
        nav.setBackground(BG_CARD);
        nav.add(prev, BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        root.add(nav, BorderLayout.NORTH);

        JPanel daysGrid = new JPanel(new GridLayout(7, 7, 4, 4));
        daysGrid.setBackground(BG_CARD);
        root.add(daysGrid, BorderLayout.CENTER);

        LocalDate[] chosenDate = new LocalDate[]{null};
        YearMonth[] shownMonth = new YearMonth[]{YearMonth.from(initialDate)};
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy");

        Runnable render = () -> {
            daysGrid.removeAll();
            String[] headers = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String h : headers) {
                JLabel lbl = new JLabel(h, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(new Color(80, 97, 115));
                daysGrid.add(lbl);
            }

            YearMonth ym = shownMonth[0];
            monthLabel.setText(ym.atDay(1).format(monthFormat));

            int firstDayOffset = ym.atDay(1).getDayOfWeek().getValue() % 7;
            for (int i = 0; i < firstDayOffset; i++) {
                daysGrid.add(new JLabel(""));
            }

            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate candidate = ym.atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                dayButton.setFocusPainted(false);
                dayButton.setMargin(new Insets(4, 4, 4, 4));
                dayButton.setBackground(Color.WHITE);
                dayButton.setBorder(BorderFactory.createLineBorder(new Color(211, 220, 232)));
                boolean inPast = candidate.isBefore(LocalDate.now());

                if (candidate.equals(LocalDate.now())) {
                    dayButton.setBorder(BorderFactory.createLineBorder(new Color(112, 186, 145), 2));
                }
                if (candidate.equals(initialDate)) {
                    dayButton.setBackground(new Color(217, 240, 255));
                }

                if (inPast) {
                    dayButton.setEnabled(false);
                    dayButton.setForeground(new Color(156, 164, 176));
                    dayButton.setBackground(new Color(244, 247, 251));
                } else {
                    dayButton.addActionListener(e -> {
                        chosenDate[0] = candidate;
                        dialog.dispose();
                    });
                }
                daysGrid.add(dayButton);
            }

            int usedCells = 7 + firstDayOffset + ym.lengthOfMonth();
            for (int i = usedCells; i < 49; i++) {
                daysGrid.add(new JLabel(""));
            }

            daysGrid.revalidate();
            daysGrid.repaint();
        };

        prev.addActionListener(e -> {
            shownMonth[0] = shownMonth[0].minusMonths(1);
            render.run();
        });
        next.addActionListener(e -> {
            shownMonth[0] = shownMonth[0].plusMonths(1);
            render.run();
        });

        render.run();
        dialog.setContentPane(root);
        dialog.setVisible(true);
        return chosenDate[0];
    }

    private void loadBookings() {
        bookings.clear();
        if (!Files.exists(STORAGE_PATH)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(STORAGE_PATH)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 7 && parts.length != 9) {
                    continue;
                }
                try {
                    if (parts.length == 9) {
                        bookings.add(new Booking(
                                parts[0],
                                LocalDate.parse(parts[1], DATE_FORMAT),
                                parts[2],
                                parts[3],
                                parts[4],
                                parts[5],
                                parts[6],
                                parts[7],
                                parts[8]
                        ));
                    } else {
                        bookings.add(new Booking(
                                parts[0],
                                LocalDate.parse(parts[1], DATE_FORMAT),
                                parts[2],
                                parts[3],
                                parts[4],
                                "",
                                "",
                                parts[5],
                                parts[6]
                        ));
                    }
                } catch (DateTimeParseException ignored) {
                    // Ignore malformed rows.
                }
            }
        } catch (IOException ex) {
            setStatus("Failed to load saved bookings.", true);
        }
    }

    private void saveBookings() {
        try (BufferedWriter writer = Files.newBufferedWriter(STORAGE_PATH)) {
            for (Booking b : bookings) {
                writer.write(safe(b.id) + "|" + safe(b.date.format(DATE_FORMAT)) + "|" + safe(b.court) + "|"
                        + safe(b.slot) + "|" + safe(b.name) + "|" + safe(b.registerNo) + "|" + safe(b.email) + "|"
                        + safe(b.phone) + "|" + safe(b.notes));
                writer.newLine();
            }
        } catch (IOException ex) {
            setStatus("Failed to save bookings.", true);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", "/");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use default look and feel.
            }
            new IndoorCourtBookingSwing().setVisible(true);
        });
    }

    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 251, 255));
                c.setForeground(new Color(30, 42, 58));
            }
            return c;
        }
    }

    private static class Booking {
        private final String id;
        private final LocalDate date;
        private final String court;
        private final String slot;
        private final String name;
        private final String registerNo;
        private final String email;
        private final String phone;
        private final String notes;

        private Booking(
                String id,
                LocalDate date,
                String court,
                String slot,
                String name,
                String registerNo,
                String email,
                String phone,
                String notes
        ) {
            this.id = id;
            this.date = date;
            this.court = court;
            this.slot = slot;
            this.name = name;
            this.registerNo = registerNo;
            this.email = email;
            this.phone = phone;
            this.notes = notes;
        }
    }
}