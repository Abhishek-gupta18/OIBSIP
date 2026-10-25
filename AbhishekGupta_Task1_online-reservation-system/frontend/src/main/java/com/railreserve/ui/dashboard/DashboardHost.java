package com.railreserve.ui.dashboard;

import com.railreserve.ui.components.Toast;

/** Callback surface each dashboard section uses to navigate + report toasts. */
public interface DashboardHost {

    /** Supported sections: overview, book, bookings, pnr, cancel; settings via openSettings(). */
    void showSection(String section);

    void toast(String message, Toast.Kind kind);

    /** Leaves the dashboard for the Settings screen. */
    void openSettings();
}
