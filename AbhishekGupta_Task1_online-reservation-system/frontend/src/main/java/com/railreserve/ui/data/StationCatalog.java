package com.railreserve.ui.data;

import java.util.List;

/**
 * Static rail catalog for the landing page: station list for search
 * autocomplete plus popular routes shown as pre-fill cards. Sample data only —
 * train numbers reference the seeded trains table, so a pre-filled search can
 * actually be booked. No DB access at render time.
 */
public final class StationCatalog {

    /** A station: short code + display name. */
    public record Station(String code, String name) {
        @Override public String toString() {
            return "(" + code + ") " + name;
        }
    }

    /** A popular route card: stations + a train on that route (seeded). */
    public record PopularRoute(String fromCode, String fromName, String toCode, String toName,
                               int trainNumber, String trainName, String info) {
        @Override public String toString() {
            return fromCode + " → " + toCode;
        }
    }

    private static final List<Station> STATIONS = List.of(
            new Station("NDLS", "New Delhi"),
            new Station("DLI", "Old Delhi Junction"),
            new Station("HWH", "Howrah Junction"),
            new Station("SDAH", "Sealdah"),
            new Station("BCT", "Mumbai Central"),
            new Station("CSMT", "Mumbai CSMT"),
            new Station("MAS", "Chennai Central"),
            new Station("SBC", "Bengaluru City"),
            new Station("ADI", "Ahmedabad Junction"),
            new Station("LKO", "Lucknow (Charbagh)"),
            new Station("CNB", "Kanpur Central"),
            new Station("PNBE", "Patna Junction"),
            new Station("BPL", "Bhopal Junction"),
            new Station("NGP", "Nagpur Junction"),
            new Station("HYB", "Hyderabad Deccan"),
            new Station("PUNE", "Pune Junction"),
            new Station("ADI2", "Ahmedabad Sabarmati"),
            new Station("JP", "Jaipur Junction"),
            new Station("JU", "Jodhpur Junction"),
            new Station("BBS", "Bhubaneswar"),
            new Station("VSKP", "Visakhapatnam"),
            new Station("ERS", "Ernakulam Junction"),
            new Station("TVC", "Thiruvananthapuram Central"),
            new Station("GHY", "Guwahati"),
            new Station("ASR", "Amritsar Junction"),
            new Station("JAT", "Jammu Tawi"),
            new Station("CDG", "Chandigarh"),
            new Station("GKP", "Gorakhpur Junction"),
            new Station("KOTA", "Kota Junction"),
            new Station("TPT", "Tirupati"));

    private static final List<PopularRoute> POPULAR_ROUTES = List.of(
            new PopularRoute("NDLS", "New Delhi", "BCT", "Mumbai Central",
                    12951, "Mumbai Rajdhani Express", "Evening departure · overnight ride"),
            new PopularRoute("NDLS", "New Delhi", "HWH", "Howrah Junction",
                    12301, "Howrah Rajdhani Express", "Non-stop AC 2A/3A · ~16h"),
            new PopularRoute("ADI", "Ahmedabad Junction", "BCT", "Mumbai Central",
                    12009, "Ahmedabad-Mumbai Central Shatabdi Express", "Morning Shatabdi · ~6h"),
            new PopularRoute("BPL", "Bhopal Junction", "CNB", "Kanpur Central",
                    12621, "Tamil Nadu Express", "Through connection southbound"),
            new PopularRoute("VSKP", "Visakhapatnam", "BBS", "Bhubaneswar",
                    18507, "Visakhapatnam Intercity Express", "Coastal daytime intercity"),
            new PopularRoute("MAS", "Chennai Central", "TVC", "Thiruvananthapuram Central",
                    16526, "Kanyakumari Express", "Overnight to the southern tip"));

    private StationCatalog() {
    }

    /** All stations as display strings "[(CODE) Name]" for autocomplete. */
    public static List<String> allSuggestions() {
        return STATIONS.stream().map(Station::toString).toList();
    }

    /** Writable codes for matching a typed selection back to a catalog entry. */
    public static List<Station> stations() {
        return STATIONS;
    }

    public static List<PopularRoute> popularRoutes() {
        return POPULAR_ROUTES;
    }

    /** Station lookup from a typed value like "(NDLS) New Delhi", tolerant of partial text. */
    public static String codeOfDisplay(String display) {
        if (display != null && display.startsWith("(") && display.contains(")")) {
            return display.substring(1, display.indexOf(')'));
        }
        return display;
    }

    public static String nameOfDisplay(String display) {
        if (display != null && display.startsWith("(") && display.contains(")")) {
            return display.substring(display.indexOf(')') + 1).strip();
        }
        return display;
    }
}
