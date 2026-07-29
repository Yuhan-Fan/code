import java.sql.*;
import java.util.Scanner;
import java.math.BigDecimal;

class ConnectDatabase {
    static final String URL  = "jdbc:mysql://localhost:3306/mydb";
    static final String USER = "root";
    static final String PASS = "whoeverfyh";

    static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

public class Mytix {
    private static final double DEFAULT_RADIUS = 4.0;

    public static void main(String[] args) throws SQLException {

        try (Connection conn = ConnectDatabase.getConnection()) {

            System.out.println("Connected!\n");

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("1 Operations   2 Queries   3 Reports   4 Exit\n");
                System.out.print("Choose by entering a number.\n");
                String choice = scanner.nextLine();

                if (choice.equals("1"))
                    operations(conn, scanner);
                else if (choice.equals("2"))
                    queries(conn, scanner);
                else if (choice.equals("3"))
                    reports(conn, scanner);
                else if (choice.equals("4"))
                    break;
                else
                    System.out.print("Invalid choice.\n");
            }
        }
    }

    private static boolean helper_genre_exists(Connection conn, String genre, String segment) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Genres WHERE genre = ? AND segment = ?")) {
            stmt.setString(1, genre);
            stmt.setString(2, segment);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_segment_exists(Connection conn, String segment) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Segments WHERE segment = ?")) {
            stmt.setString(1, segment);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_organizer_exists(Connection conn, int uid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Organizers JOIN Users ON Organizers.uid = Users.uid " +
            "WHERE Organizers.uid = ? AND Users.deleted = false")) {
            stmt.setInt(1, uid);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_customer_exists(Connection conn, int uid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Customers JOIN Users ON Customers.uid = Users.uid " +
            "WHERE Customers.uid = ? AND Users.deleted = false")) {
            stmt.setInt(1, uid);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_event_exists(Connection conn, int eid, int uid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Events WHERE eid = ? AND uid = ?")) {
            stmt.setInt(1, eid);
            stmt.setInt(2, uid);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_venue_exists(Connection conn, String venue) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Venues WHERE name = ?")) {
            stmt.setString(1, venue);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_performance_exists_witheid(Connection conn, int pid, int eid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Performances WHERE pid = ? AND eid = ? and cancel_datetime IS NULL")) {
            stmt.setInt(1, pid);
            stmt.setInt(2, eid);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_performance_exists(Connection conn, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Performances WHERE pid = ? and cancel_datetime IS NULL")) {
            stmt.setInt(1, pid);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_pricetier_exists(Connection conn, String pricetierName, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) " +
                "FROM Price_tiers " +
                "WHERE name = ? AND pid = ?")) {
            stmt.setString(1, pricetierName);
            stmt.setInt(2, pid);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_general_section_exists(Connection conn, String section, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) " +
                "FROM General_sections JOIN Performances ON General_sections.venue_name = Performances.venue_name " +
                "WHERE Performances.pid = ? AND General_sections.name = ?" )) {
            stmt.setInt(1, pid);
            stmt.setString(2, section);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_seat_exists(Connection conn, int seat, int row, String section, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) " +
                "FROM Seats JOIN Performances ON Seats.venue_name = Performances.venue_name " +
                "WHERE Seats.seat = ? AND Seats.row = ? AND Seats.section_name = ? AND Performances.pid = ?")) {
            stmt.setInt(1, seat);
            stmt.setInt(2, row);
            stmt.setString(3, section);
            stmt.setInt(4, pid);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_seat_sold(Connection conn, int seat, int row, String section, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) " +
                "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
                "             JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                "WHERE Orders.pid = ? AND Tickets.cancel_datetime IS NULL AND Reserved_tickets.seat = ? " +
                "                     AND Reserved_tickets.row = ? AND Reserved_tickets.section_name = ?")) {
            stmt.setInt(1, pid);
            stmt.setInt(2, seat);
            stmt.setInt(3, row);
            stmt.setString(4, section);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt(1) > 0;
            }
    }

    private static boolean helper_seat_blocked(Connection conn, int seat, int row, String section, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) " +
                "FROM Block " +
                "WHERE seat = ? AND `row` = ? AND section_name = ? AND pid = ?")) {
            stmt.setInt(1, seat);
            stmt.setInt(2, row);
            stmt.setString(3, section);
            stmt.setInt(4, pid);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_general_ticket_available(Connection conn, String section, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT General_sections.total_capacity - COUNT(General_tickets.tid) AS available " +
            "FROM General_sections JOIN Performances ON General_sections.venue_name = Performances.venue_name " +
            "                      LEFT JOIN Orders ON Orders.pid = Performances.pid " +
            "                      LEFT JOIN Tickets ON Tickets.oid = Orders.oid AND Tickets.cancel_datetime IS NULL " +
            "                      LEFT JOIN General_tickets ON General_tickets.tid = Tickets.tid " +
            "                                                AND General_tickets.section_name = General_sections.name " +
            "WHERE Performances.pid = ? " +
            "  AND General_sections.name = ? " +
            "GROUP BY General_sections.total_capacity")) {

            stmt.setInt(1, pid);
            stmt.setString(2, section);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("available") > 0;
            }
        }
    }

    private static boolean helper_listing_available(Connection conn, int lid, int pid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT Listings.lid, Listings.seller_id, Listings.price " +
            "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
            "              JOIN Orders ON Tickets.oid = Orders.oid " +
            "              JOIN Performances ON Orders.pid = Performances.pid " +
            "WHERE Listings.lid = ? AND Performances.pid = ? " +
            "  AND Listings.withdraw_datetime IS NULL " +
            "  AND Listings.buyer_id IS NULL")) {
            
            stmt.setInt(1, lid);
            stmt.setInt(2, pid);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean helper_reserved_ticket_can_cancel(Connection conn, int tid, int uid) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
        "SELECT COUNT(*) " +
        "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
        "             JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
        "             JOIN Performances ON Orders.pid = Performances.pid " +
        "WHERE Tickets.tid = ? " +
        "  AND Orders.uid = ? " +
        "  AND Tickets.cancel_datetime IS NULL AND Performances.datetime >= CURRENT_TIMESTAMP + INTERVAL 7 DAY" +
        "  AND NOT EXISTS (SELECT * " +
        "                  FROM Listings " +
        "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL)"
        )) {
            ps.setInt(1, tid);
            ps.setInt(2, uid);
            ps.setInt(3, uid);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_general_ticket_can_cancel(Connection conn, int tid, int uid) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
        "SELECT COUNT(*) " +
        "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
        "             JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
        "             JOIN Performances ON Orders.pid = Performances.pid " +
        "WHERE Tickets.tid = ? " +
        "  AND Orders.uid = ? " +
        "  AND Tickets.cancel_datetime IS NULL AND Performances.datetime >= CURRENT_TIMESTAMP + INTERVAL 7 DAY" +
        "  AND NOT EXISTS (SELECT * " +
        "                  FROM Listings " +
        "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL)"
        )) {
            ps.setInt(1, tid);
            ps.setInt(2, uid);
            ps.setInt(3, uid);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_reserved_ticket_can_list(Connection conn, int tid, int uid) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
            "             JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
            "             JOIN Performances ON Orders.pid = Performances.pid " +
            "WHERE Tickets.tid = ? " +
            "  AND Performances.datetime >= CURRENT_TIMESTAMP " +
            "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
            "                                 FROM Listings " +
            "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
            "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
            "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
            "                  FROM Listings " +
            "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL)"
        )) {
            ps.setInt(1, tid);
            ps.setInt(2, uid);
            ps.setInt(3, uid);
            ps.setInt(4, uid);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_general_ticket_can_list(Connection conn, int tid, int uid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Orders JOIN Tickets on Orders.oid = Tickets.oid " +
            "            JOIN Performances ON Orders.pid = Performances.pid " +
            "            JOIN Events ON Performances.eid = Events.eid " +
            "            JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
            "WHERE Tickets.tid = ? " +
            "  AND Performances.datetime >= CURRENT_TIMESTAMP " +
            "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
            "                                 FROM Listings " +
            "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
            "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
            "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
            "                  FROM Listings " +
            "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL)"
        )) {
            ps.setInt(1, tid);
            ps.setInt(2, uid);
            ps.setInt(3, uid);
            ps.setInt(4, uid);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static BigDecimal helper_highest_resale_price(Connection conn, int tid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
        "SELECT Tickets.face_value, Events.resale_cap " +
        "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
        "             JOIN Performances ON Orders.pid = Performances.pid " +
        "             JOIN Events ON Performances.eid = Events.eid " +
        "WHERE Tickets.tid = ?"
        )) {
            ps.setInt(1, tid);

            ResultSet rs = ps.executeQuery();
            rs.next();
            BigDecimal face_value = rs.getBigDecimal("face_value");
            BigDecimal resale_cap = rs.getBigDecimal("resale_cap");
            return face_value.multiply(resale_cap).divide(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    private static boolean helper_listing_can_withdraw(Connection conn, int lid, int uid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * " +
            "FROM Listings " +
            "WHERE Listings.lid = ? AND Listings.seller_id = ? " +
            "  AND Listings.buyer_id IS NULL " +
            "  AND Listings.withdraw_datetime IS NULL "
        )) {
            ps.setInt(1, lid);
            ps.setInt(2, uid);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean helper_performance_reviewable(Connection conn, int pid, int uid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT DISTINCT Performances.pid, Events.name " +
            "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
            "             JOIN Performances ON Orders.pid = Performances.pid " +
            "             JOIN Events ON Performances.eid = Events.eid " +
            "WHERE Performances.pid = ? " +
            "  AND Performances.datetime <= CURRENT_TIMESTAMP " +
            "  AND Performances.datetime >= CURRENT_TIMESTAMP - INTERVAL 1 MONTH " +
            "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
            "                                 FROM Listings " +
            "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
            "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
            "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
            "                  FROM Listings " +
            "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL)")) {

            stmt.setInt(1, pid);
            stmt.setInt(2, uid);
            stmt.setInt(3, uid);
            stmt.setInt(4, uid);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void print_operations_options () {
        System.out.print("1 Create a profile.\n");
        System.out.print("2 Delete a profile.\n");
        System.out.print("3 Create an event.\n");
        System.out.print("4 Create a performance.\n");
        System.out.print("5 Define pricetiers and assign to sections.\n");
        System.out.print("6 Set resale cap of an event.\n");
        System.out.print("7 Update price tier price.\n");
        System.out.print("8 Block or unblock seat.\n");
        System.out.print("9 Book tickets.\n");
        System.out.print("10 Cancel performance.\n");
        System.out.print("11 Cancel ticket.\n");
        System.out.print("12 List ticket.\n");
        System.out.print("13 Withdraw listing.\n");
        System.out.print("14 Purchase listing of another customer.\n");
        System.out.print("15 Review performance.\n");
        System.out.print("Choose by entering a number.\n");
    }

    private static void operations (Connection conn, Scanner scanner) throws SQLException {
        print_operations_options ();
        String choice = scanner.nextLine();

        if (choice.equals("1"))
            o1_create_profile(conn, scanner);
        else if (choice.equals("2"))
            o2_delete_profile(conn, scanner);
        else if (choice.equals("3"))
            o3_create_event(conn, scanner);
        else if (choice.equals("4"))
            o4_create_performance(conn, scanner);
        else if (choice.equals("5"))
            o5_define_pricetier_and_assign_to_section(conn, scanner);
        else if (choice.equals("6"))
            o6_set_event_resale_cap(conn, scanner);
        else if (choice.equals("7"))
            o7_update_pricetier(conn, scanner);
        else if (choice.equals("8"))
            o8_block_or_unblock_seat(conn, scanner);
        else if (choice.equals("9"))
            o9_book_ticket(conn, scanner);
        else if (choice.equals("10"))
            o10_cancel_performance(conn, scanner);
        else if (choice.equals("11"))
            o11_cancel_ticket(conn, scanner);
        else if (choice.equals("12"))
            o12_list_ticket(conn, scanner);
        else if (choice.equals("13"))
            o13_withdraw_listing(conn, scanner);
        else if (choice.equals("14"))
            o14_purchase_listing(conn, scanner);
        else if (choice.equals("15"))
            o15_review_performance(conn, scanner);
        else
            System.out.println("Invalid choice.\n");
    }

    private static void print_queries_options () {
        System.out.print("1 Search for upcoming performances by latitude & longitude.\n");
        System.out.print("2 Search for upcoming performances by postal code.\n");
        System.out.print("3 Search for upcoming performances by address.\n");
        System.out.print("4.1 Temporal and availability refinement of 1.\n");
        System.out.print("4.2                                         2.\n");
        System.out.print("4.3                                         3.\n");
        System.out.print("5 Search by filters.\n");
        System.out.print("6 Seat summary of a performance.\n");
        System.out.print("Choose by entering a number.\n");
    }

    private static void queries (Connection conn, Scanner scanner) throws SQLException {
        print_queries_options ();
        String choice = scanner.nextLine();

        if (choice.equals("1"))
            q1_search_performances_by_lat_lon(conn, scanner);
        else if (choice.equals("2"))
            q2_search_performances_by_postalcode(conn, scanner);
        else if (choice.equals("3"))
            q3_search_performances_by_address(conn, scanner);
        else if (choice.equals("4.1"))
            q4_refine_q1(conn, scanner);
        else if (choice.equals("4.2"))
            q4_refine_q2(conn, scanner);
        else if (choice.equals("4.3"))
            q4_refine_q3(conn, scanner);
        else if (choice.equals("5"))
            q5_filters(conn, scanner);
        else if (choice.equals("6"))
            q6_seat_map_summary(conn, scanner);
        else
            System.out.println("Invalid choice.\n");

        return;
    }

    private static void print_reports_options () {
        System.out.print("1.1 Rank number of tickets sold & gross revenue by city.\n");
        System.out.print("1.2                                             by venue within a city.\n");
        System.out.print("2.1 Report number of events & performances by segment and genre.\n");
        System.out.print("2.2                                        by country.\n");
        System.out.print("2.3                                        by country and city.\n");
        System.out.print("2.4                                        by country, city and venue.\n");
        System.out.print("3.1 Rank organizers by overall revenue.\n");
        System.out.print("3.2                 by country revenue.\n");
        System.out.print("3.3                 by country, city revenue.\n");
        System.out.print("4   Report ticket scalpers.\n");
        System.out.print("5.1 Rank customers by number of orders.\n");
        System.out.print("5.2                by number of orders per city.\n");
        System.out.print("6.1 Rank customers by number of tickets cancelled.\n");
        System.out.print("6.2 Rank organizers by number of performances cancelled.\n");
        System.out.print("7   Report performance sell-through rates.\n");
        System.out.print("8.1 Report event number of completed resales.\n");
        System.out.print("8.2              average markup over face value.\n");
        System.out.print("8.3              fraction of listings priced exactly at the cap.\n");
        System.out.print("8.4 Report top 10 events by resale volume in a given period.\n");
        System.out.print("Choose by entering a choice, e.g. 1.1\n");
    }

    private static void reports (Connection conn, Scanner scanner) throws SQLException {
        print_reports_options ();
        String choice = scanner.nextLine();

        if (choice.equals("1.1"))
            r1_1_city_ticket_or_revenue_rank(conn, scanner);
        else if (choice.equals("1.2"))
            r1_2_venue_ticket_or_revenue_rank(conn, scanner);
        else if (choice.equals("2.1"))
            r2_1_genre_report(conn, scanner);
        else if (choice.equals("2.2"))
            r2_2_country_report(conn, scanner);
        else if (choice.equals("2.3"))
            r2_3_country_city_report(conn, scanner);
        else if (choice.equals("2.4"))
            r2_4_country_city_venue_report(conn, scanner);
        else if (choice.equals("3.1"))
            r3_1_organizer_overall_revenue_rank(conn, scanner);
        else if (choice.equals("3.2"))
            r3_2_organizer_country_revenue_rank(conn, scanner);
        else if (choice.equals("3.3"))
            r3_3_organizer_country_city_revenue_rank(conn, scanner);
        else if (choice.equals("4"))
            r4_ticket_scalpers_eport(conn, scanner);
        else if (choice.equals("5.1"))
            r5_1_customer_order_rank(conn, scanner);
        else if (choice.equals("5.2"))
            r5_2_customer_order_city_rank(conn, scanner);
        else if (choice.equals("6.1"))
            r6_1_customer_cancel_ticket_rank(conn, scanner);
        else if (choice.equals("6.2"))
            r6_2_organizer_cancel_performance_rank(conn, scanner);
        else if (choice.equals("7"))
            r7_performance_sell_through_rate(conn, scanner);
        else if (choice.equals("8.1"))
            r8_1_resales_report_completed_resale(conn, scanner);
        else if (choice.equals("8.2"))
            r8_2_resales_report_average_markup(conn, scanner);
        else if (choice.equals("8.3"))
            r8_3_resales_report_fraction(conn, scanner);
        else if (choice.equals("8.4"))
            r8_4_resales_report_top_10(conn, scanner);
        else
            System.out.println("Invalid choice.\n");
    }

    private static void o1_create_profile(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter name:\n");
        String name = scanner.nextLine();
        System.out.print("Enter address:\n");
        String address = scanner.nextLine();
        System.out.print("Enter email:\n");
        String email = scanner.nextLine();
        System.out.print("Enter date-of-birth: YYYY-MM-DD\n");
        String dob = scanner.nextLine();
        java.sql.Date dob_date;
        try {
            dob_date = java.sql.Date.valueOf(dob);
        }
        catch (IllegalArgumentException e) {
            System.out.print("Invalid date format.\n");
            return;
        }
        if (dob_date.after(java.sql.Date.valueOf(java.time.LocalDate.now().minusYears(18)))) {
            System.out.print("You have to be at least 18 years old to create a profile.\n");
            return;
        }

        System.out.print("Are you a customer or organizer? c/o\n");
        String role = scanner.nextLine();

        try {
            conn.setAutoCommit(false);
            int uid;
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Users (name, address, email, date_of_birth) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, email);
            ps.setDate(4, dob_date);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();

            if (!keys.next())
                throw new SQLException("Creating user failed: generating UID failed");
            uid = keys.getInt(1);
            keys.close();

            if (role.equals("c")) {
                System.out.print("Enter payment card number:\n");
                String card = scanner.nextLine();
                PreparedStatement customerps = conn.prepareStatement(
                    "INSERT INTO Customers (uid, payment_card) VALUES (?, ?)"
                );
                customerps.setInt(1, uid);
                customerps.setString(2, card);
                customerps.executeUpdate();
            }
            else if (role.equals("o")) {
                PreparedStatement organizerps = conn.prepareStatement(
                    "INSERT INTO Organizers (uid) VALUES (?)"
                );
                organizerps.setInt(1, uid);
                organizerps.executeUpdate();
            }
            else {
                System.out.print("Invalid choice.\n");
                conn.rollback();
                return;
            }

            conn.commit();
            System.out.printf("Your profile is created. UID: %d\n", uid);
        }
        catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o2_delete_profile(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement listingps = conn.prepareStatement(
                "UPDATE Listings " +
                "SET withdraw_datetime = CURRENT_TIMESTAMP " +
                "WHERE seller_id = ? " +
                "  AND buyer_id IS NULL " +
                "  AND withdraw_datetime IS NULL")) {
            listingps.setInt(1, uid);
            listingps.executeUpdate();
        }
            PreparedStatement userps = conn.prepareStatement(
                "UPDATE Users " +
                "SET deleted = true " +
                "WHERE uid = ? AND deleted = false"
            );
            userps.setInt(1, uid);

            int success = userps.executeUpdate();
            if (success == 0) {
                conn.rollback();
                System.out.print("Failed to delete profile.\n");
            }
            else {
                conn.commit();
                System.out.println("Profile deleted successfully.\n");
            }
        }
        catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o3_create_event(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }
        System.out.print("Enter event name:\n");
        String name = scanner.nextLine();
        System.out.print("Enter event resale-cap: in percentage, e.g. 120\n");
        BigDecimal resaleCap = new BigDecimal(scanner.nextLine());
        if (resaleCap.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Resale cap must be greater than 0.\n");
            return;
        }
        System.out.print("Enter event segment:\n");
        String segment = scanner.nextLine();
        if (!helper_segment_exists(conn, segment)) {
            System.out.print("Invalid segment.\n");
            return;
        }
        System.out.print("Enter event genre:\n");
        String genre = scanner.nextLine();
        if (!helper_genre_exists(conn, genre, segment)) {
            System.out.print("Invalid genre.\n");
            return;
        }

        conn.setAutoCommit(false);

        try {
            int eid;
            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Events (name, resale_cap, uid, genre, segment) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )) {

                ps.setString(1, name);
                ps.setBigDecimal(2, resaleCap);
                ps.setInt(3, uid);
                ps.setString(4, genre);
                ps.setString(5, segment);
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();

                if (!keys.next())
                    throw new SQLException("Creating event failed: generating EID failed");
                eid = keys.getInt(1);
                keys.close();

                System.out.print("Here are all the artists:\n");
                try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                    "SELECT * " +
                    "FROM Artists "
                )) {

                System.out.printf(
                    "%-10s %-30s%n",
                    "AID", "Artist name"
                );
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                        "%-10s %-30s %n",
                        rs.getInt("aid"),
                        rs.getString("name")
                    );
                }
            }

                System.out.print("How many artists are you planning to have?\n");
                int a = Integer.parseInt(scanner.nextLine());
                if (a < 1) {
                    throw new SQLException("There should be at least 1 artist.");
                }
                int aid;

                for (int i=1; i<=a; i++) {
                    System.out.printf("Enter AID for the number %d artist:\n", i);
                    aid = Integer.parseInt(scanner.nextLine());

                    try (PreparedStatement aps = conn.prepareStatement(
                        "INSERT INTO Feature (aid, eid, billing_order) VALUES (?, ?, ?)"
                    )) {
                        aps.setInt(1, aid);
                        aps.setInt(2, eid);
                        aps.setInt(3, i);
                        aps.executeUpdate();
                    }
                }

                conn.commit();
                System.out.printf("Your event is created. EID: %d\n", eid);
            }
        }
        catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o4_create_performance(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }
        System.out.print("Here are the events you have created:\n");
        try (PreparedStatement eventps = conn.prepareStatement(
            "SELECT eid, name " +
            "FROM Events " +
            "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf("%-10s %-30s%n", "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf("%-10d %-30s%n",
                        rs.getInt("eid"),
                        rs.getString("name"));
                }
            }
        }

        System.out.print("Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());
        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print("Enter datetime: YYYY-MM-DD HH:MM:SS\n");
        String datetime = scanner.nextLine();
        Timestamp datetime_time;
        try {
            datetime_time = Timestamp.valueOf(datetime);
        }
        catch (IllegalArgumentException e) {
            System.out.print("Invalid datetime format.\n");
            return;
        }

        if (!datetime_time.after(new Timestamp(System.currentTimeMillis()))) {
            System.out.println("Only future performances can be created.");
            return;
        }

        System.out.print("Enter venue name:\n");
        String venue = scanner.nextLine();
        if (!helper_venue_exists(conn, venue)) {
            System.out.print("Invalid venue.\n");
            return;
        }

        conn.setAutoCommit(false);

        try {
            int pid;
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Performances (datetime, eid, venue_name) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setTimestamp(1, datetime_time);
            ps.setInt(2, eid);
            ps.setString(3, venue);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();

            if (!keys.next())
                throw new SQLException("Creating performance failed: generating PID failed");
            pid = keys.getInt(1);
            keys.close();

            conn.commit();
            System.out.printf("Performance is created. PID: %d\n", pid);
        }
        catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o5_define_pricetier_and_assign_to_section(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());

        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }

        System.out.print("Here are the events you have created:\n");

        try (PreparedStatement eventps = conn.prepareStatement(
                "SELECT eid, name " +
                "FROM Events " +
                "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf(
                        "%-10s %-30s%n",
                        "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-30s%n",
                            rs.getInt("eid"),
                            rs.getString("name"));
                }
            }
        }

        System.out.print("Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());

        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print(
                "Here are the performances of this event:\n");

        try (PreparedStatement performanceps = conn.prepareStatement(
                "SELECT pid, datetime, venue_name " +
                "FROM Performances " +
                "WHERE eid = ? AND cancel_datetime IS NULL")) {
            performanceps.setInt(1, eid);

            try (ResultSet rs = performanceps.executeQuery()) {
                System.out.printf(
                        "%-10s %-25s %-30s%n",
                        "PID", "Datetime", "Venue");
                System.out.println("-".repeat(65));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-25s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"));
                }
            }
        }

        System.out.print("Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());

        if (!helper_performance_exists_witheid(conn, pid, eid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        System.out.print("How many price tiers?\n");
        int num = Integer.parseInt(scanner.nextLine());

        if (num <= 0) {
            System.out.print(
                    "Number of price tiers must be greater than 0.\n");
            return;
        }

        conn.setAutoCommit(false);

        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Price_tiers (name, price, pid) " +
                    "VALUES (?, ?, ?)")) {

                for (int i = 1; i <= num; i++) {
                    System.out.printf("Enter name of price tier number %d:\n", i);
                    String pricetierName = scanner.nextLine();

                    System.out.printf("Enter price of price tier number %d:\n", i);
                    BigDecimal pricetierPrice =
                            new BigDecimal(scanner.nextLine());

                    if (pricetierPrice.compareTo(
                            BigDecimal.ZERO) <= 0) {
                        System.out.print("Price must be greater than 0.\n");
                        conn.rollback();
                        return;
                    }

                    ps.setString(1, pricetierName);
                    ps.setBigDecimal(2, pricetierPrice);
                    ps.setInt(3, pid);
                    ps.executeUpdate();
                }
            }

            System.out.println(
                    "Here are the price tiers of this performance:");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT name, price " +
                    "FROM Price_tiers " +
                    "WHERE pid = ? " +
                    "ORDER BY price")) {
                ps.setInt(1, pid);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf(
                            "%-30s %-15s%n",
                            "Price tier", "Price");
                    System.out.println("-".repeat(45));

                    while (rs.next()) {
                        System.out.printf(
                                "%-30s %-15s%n",
                                rs.getString("name"),
                                rs.getBigDecimal("price"));
                    }
                }
            }

            String venueName;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT venue_name " +
                    "FROM Performances " +
                    "WHERE pid = ?")) {
                ps.setInt(1, pid);

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    venueName = rs.getString("venue_name");
                }
            }

            System.out.println("Here are the sections of this venue:");

            try (PreparedStatement sectionps = conn.prepareStatement(
                    "SELECT name " +
                    "FROM Sections " +
                    "WHERE venue_name = ?")) {
                sectionps.setString(1, venueName);

                try (ResultSet rs = sectionps.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getString("name"));
                    }
                }
            }

            try (PreparedStatement sectionps = conn.prepareStatement(
                    "SELECT name " +
                    "FROM Sections " +
                    "WHERE venue_name = ?");
                 PreparedStatement insertps = conn.prepareStatement(
                    "INSERT INTO Section_pricetier " +
                    "(section_name, pid, pricetier_name) " +
                    "VALUES (?, ?, ?)")) {

                sectionps.setString(1, venueName);

                try (ResultSet rs = sectionps.executeQuery()) {
                    while (rs.next()) {
                        String sectionName = rs.getString("name");

                        System.out.printf("Enter the price tier for section %s:\n", sectionName);
                        String pricetierName = scanner.nextLine();

                        if (!helper_pricetier_exists(conn, pricetierName, pid)) {
                            System.out.print("Invalid price tier.\n");
                            conn.rollback();
                            return;
                        }

                        insertps.setString(1, sectionName);
                        insertps.setInt(2, pid);
                        insertps.setString(3, pricetierName);
                        insertps.executeUpdate();
                    }
                }
            }

            conn.commit();
            System.out.print(
                    "Price tiers created and assigned successfully.\n");
        }
        catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o6_set_event_resale_cap(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }
        System.out.print("Here are the events you have created:\n");
        try (PreparedStatement eventps = conn.prepareStatement(
            "SELECT eid, name " +
            "FROM Events " +
            "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf("%-10s %-30s%n", "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf("%-10d %-30s%n",
                        rs.getInt("eid"),
                        rs.getString("name"));
                }
            }
        }

        System.out.print("Which one do you want to set resale cap? Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());
        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print("Enter the new resale cap: in percentage, e.g. 120\n");
        BigDecimal resaleCap = new BigDecimal(scanner.nextLine());

        if (resaleCap.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.print("Resale cap must be greater than 0.\n");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Events " +
                "SET resale_cap = ? " +
                "WHERE eid = ?")) {
            ps.setBigDecimal(1, resaleCap);
            ps.setInt(2, eid);

            int success = ps.executeUpdate();

            if (success == 0)
                System.out.print("Failed to update resale cap.\n");
            else
                System.out.print("Resale cap updated successfully.\n");
        }
    }

    private static void o7_update_pricetier(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());

        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }

        System.out.print("Here are the events you have created:\n");

        try (PreparedStatement eventps = conn.prepareStatement(
                "SELECT eid, name " +
                "FROM Events " +
                "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf(
                        "%-10s %-30s%n",
                        "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-30s%n",
                            rs.getInt("eid"),
                            rs.getString("name"));
                }
            }
        }

        System.out.print("Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());

        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print(
                "Here are the performances of this event:\n");

        try (PreparedStatement performances = conn.prepareStatement(
                "SELECT pid, datetime, venue_name " +
                "FROM Performances " +
                "WHERE eid = ?  AND cancel_datetime IS NULL")) {
            performances.setInt(1, eid);

            try (ResultSet rs = performances.executeQuery()) {
                System.out.printf(
                        "%-10s %-25s %-30s%n",
                        "PID", "Datetime", "Venue");
                System.out.println("-".repeat(65));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-25s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"));
                }
            }
        }

        System.out.print("Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_exists_witheid(conn, pid, eid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        try (PreparedStatement date = conn.prepareStatement(
            "SELECT datetime " +
            "FROM Performances " +
            "WHERE pid = ?"
        )) {
            date.setInt(1, pid);

            try (ResultSet rs = date.executeQuery()) {
                rs.next();
                Timestamp time = rs.getTimestamp("datetime");

                if (!time.after(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("Only future performances can be updated.");
                    return;
                }
            }
        }

        System.out.print("Here are the price tiers of this performance:\n");

        try (PreparedStatement sections = conn.prepareStatement(
            "SELECT Price_tiers.name, Price_tiers.price " +
            "FROM Price_tiers JOIN Performances ON Price_tiers.pid = Performances.pid " +
            "WHERE Performances.pid = ?"
        )) {
            sections.setInt(1, pid);

            try (ResultSet rs = sections.executeQuery()) {
                while (rs.next()) {
                    System.out.printf(
                            "%-20s %-10s%n",
                            "Price tier name", "Price");
                    System.out.println("-".repeat(30));

                    System.out.printf(
                            "%-20s %-10s%n",
                            rs.getString("name"),
                            rs.getBigDecimal("price"));
                }
            }
        }

        System.out.print("Which price tier do you want to update? Enter the name:\n");
        String name = scanner.nextLine();
        if (!helper_pricetier_exists(conn, name, pid)) {
            System.out.print("Invalid price tier.\n");
            return;
        }

        System.out.print("Enter the new price:\n");
        BigDecimal price = new BigDecimal(scanner.nextLine());
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.print("Price must be greater than 0.\n");
            return;
        }

        int sold = 0;

        try (PreparedStatement pricetier1ps = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Price_tiers JOIN Section_pricetier ON Section_pricetier.pid = Price_tiers.pid " +
            "                       AND Section_pricetier.pricetier_name = Price_tiers.name " +
            "                 JOIN Reserved_tickets ON Section_pricetier.section_name = Reserved_tickets.section_name " +
            "                 JOIN Tickets ON Reserved_tickets.tid = Tickets.tid " +
            "                 JOIN Orders ON Tickets.oid = Orders.oid " +
            "                             AND Orders.pid = Price_tiers.pid " +
            "WHERE Price_tiers.name = ? AND Price_tiers.pid = ? AND Tickets.cancel_datetime IS NULL"
        )) {
            pricetier1ps.setString(1, name);
            pricetier1ps.setInt(2, pid);

            ResultSet rs = pricetier1ps.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0)
                sold = 1;
        }

        if (sold == 0) {
        try (PreparedStatement pricetier2ps = conn.prepareStatement(
            "SELECT COUNT(*) " +
            "FROM Price_tiers JOIN Section_pricetier ON Section_pricetier.pid = Price_tiers.pid " +
            "                       AND Section_pricetier.pricetier_name = Price_tiers.name " +
            "                 JOIN General_tickets ON Section_pricetier.section_name = General_tickets.section_name " +
            "                 JOIN Tickets ON General_tickets.tid = Tickets.tid " +
            "                 JOIN Orders ON Tickets.oid = Orders.oid " +
            "                             AND Orders.pid = Price_tiers.pid " +
            "WHERE Price_tiers.name = ? AND Price_tiers.pid = ? AND Tickets.cancel_datetime IS NULL"
        )) {
            pricetier2ps.setString(1, name);
            pricetier2ps.setInt(2, pid);

            ResultSet rs = pricetier2ps.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0)
                sold = 1;
        }
        }

        if (sold == 1) {
            System.out.print("Sorry, since there are tickets sold in this price tier, you cannot update the price.\n");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Price_tiers " +
                "SET price = ? " +
                "WHERE name = ? AND pid = ?"
        )) {
            ps.setBigDecimal(1, price);
            ps.setString(2, name);
            ps.setInt(3, pid);
            int rows = ps.executeUpdate();
            if (rows == 1)
                System.out.println("Price updated successfully.");
            else
                System.out.println("Failed to update price.");
        }
    }

    private static void o8_block_or_unblock_seat(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());

        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }

        System.out.print("Here are the events you have created:\n");

        try (PreparedStatement eventps = conn.prepareStatement(
                "SELECT eid, name " +
                "FROM Events " +
                "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf(
                        "%-10s %-30s%n",
                        "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-30s%n",
                            rs.getInt("eid"),
                            rs.getString("name"));
                }
            }
        }

        System.out.print("Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());

        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print(
                "Here are the performances of this event:\n");

        try (PreparedStatement performances = conn.prepareStatement(
                "SELECT pid, datetime, venue_name " +
                "FROM Performances " +
                "WHERE eid = ? AND cancel_datetime IS NULL")) {
            performances.setInt(1, eid);

            try (ResultSet rs = performances.executeQuery()) {
                System.out.printf(
                        "%-10s %-25s %-30s%n",
                        "PID", "Datetime", "Venue");
                System.out.println("-".repeat(65));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-25s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"));
                }
            }
        }

        System.out.print("Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_exists_witheid(conn, pid, eid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        try (PreparedStatement date = conn.prepareStatement(
            "SELECT datetime " +
            "FROM Performances " +
            "WHERE pid = ?"
        )) {
            date.setInt(1, pid);

            try (ResultSet rs = date.executeQuery()) {
                rs.next();
                Timestamp time = rs.getTimestamp("datetime");

                if (!time.after(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("Only future performances can be updated.");
                    return;
                }
            }
        }

        System.out.print("Here are the reserved sections of this performance:\n");

        try (PreparedStatement sections = conn.prepareStatement(
            "SELECT Reserved_sections.name " +
            "FROM Reserved_sections JOIN Venues ON Reserved_sections.venue_name = Venues.name " +
            "                       JOIN Performances ON Venues.name = Performances.venue_name " +
            "WHERE Performances.pid = ?"
        )) {
            sections.setInt(1, pid);

            try (ResultSet rs = sections.executeQuery()) {
                while (rs.next()) {
                    System.out.printf(
                            "%-20s%n",
                            rs.getString("name"));
                }
            }
        }

        System.out.print("Enter section name:\n");
        String section = scanner.nextLine();
        System.out.print("Enter row number:\n");
        int row = Integer.parseInt(scanner.nextLine());
        System.out.print("Enter seat number:\n");
        int seat = Integer.parseInt(scanner.nextLine());
        System.out.print("Would you like to block or unblock this seat? b/u\n");
        String choice = scanner.nextLine();

        if (choice.equals("b")) {
            if (!helper_seat_exists(conn, seat, row, section, pid)) {
                System.out.print("The seat doesn't exist.\n");
                return;
            }
            if (helper_seat_sold(conn, seat, row, section, pid)) {
                System.out.print("The seat cannot be blocked because it is sold.\n");
                return;
            }
            try (PreparedStatement bps2 = conn.prepareStatement(
                "INSERT INTO Block (pid, seat, `row`, section_name) VALUES (?, ?, ?, ?)"
            )) {
                bps2.setInt(1, pid);
                bps2.setInt(2, seat);
                bps2.setInt(3, row);
                bps2.setString(4, section);
                int rows = bps2.executeUpdate();
                if (rows == 1)
                    System.out.println("Seat blocked successfully.");
                else
                    System.out.println("Failed to block seat.");
            }
        }
        else if (choice.equals("u")) {
            try (PreparedStatement ups = conn.prepareStatement(
                "DELETE FROM Block WHERE pid = ? AND seat = ? AND `row` = ? AND section_name = ?"
            )) {
                ups.setInt(1, pid);
                ups.setInt(2, seat);
                ups.setInt(3, row);
                ups.setString(4, section);
                int rows = ups.executeUpdate();
                if (rows == 1)
                    System.out.println("Seat unblocked successfully.");
                else
                    System.out.println("Operation failed. The seat is currently not blocked.");
            }
        }
        else {
            System.out.println("Invalid choice.");
        }
    }

    private static void o9_book_ticket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }

        System.out.print("Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_exists(conn, pid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        try (PreparedStatement date = conn.prepareStatement(
            "SELECT datetime " +
            "FROM Performances " +
            "WHERE pid = ?"
        )) {
            date.setInt(1, pid);

            try (ResultSet rs = date.executeQuery()) {
                rs.next();
                Timestamp time = rs.getTimestamp("datetime");

                if (!time.after(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("Only future performances tickets can be booked.");
                    return;
                }
            }
        }

        System.out.print("How many tickets are you going to book?\n");
        int num = Integer.parseInt(scanner.nextLine());
        if (num <= 0) {
            System.out.print("Number of tickets must be greater than 0.\n");
            return;
        }

        try{
            conn.setAutoCommit(false);

            String venue;
            int oid;

            try (PreparedStatement orderps = conn.prepareStatement(
                "INSERT INTO Orders (uid, pid, datetime) VALUES (?, ?, CURRENT_TIMESTAMP)",
                Statement.RETURN_GENERATED_KEYS
            )) {
                orderps.setInt(1, uid);
                orderps.setInt(2, pid);
                orderps.executeUpdate();

                ResultSet keys = orderps.getGeneratedKeys();
                if (!keys.next())
                    throw new SQLException("Creating order failed: generating OID failed");
                oid = keys.getInt(1);
                keys.close();
            }

            try (PreparedStatement venueps = conn.prepareStatement(
                "SELECT venue_name " +
                "FROM Performances " +
                "WHERE pid = ?"
            )) {
                venueps.setInt(1, pid);
                ResultSet rs = venueps.executeQuery();
                rs.next();
                venue = rs.getString(1);
            }

            for (int i=0; i<num;) {
                System.out.print("Would you like to book reserved or general ticket? r/g\n");
                String type = scanner.nextLine();
                if (type.equals("r")) {
                    System.out.print("Enter section name:\n");
                    String section = scanner.nextLine();
                    System.out.print("Enter row number:\n");
                    int row = Integer.parseInt(scanner.nextLine());
                    System.out.print("Enter seat number:\n");
                    int seat = Integer.parseInt(scanner.nextLine());
                    if (!helper_seat_exists(conn, seat, row, section, pid)) {
                        System.out.print("The seat doesn't exist. Ticket not booked.\n");
                        continue;
                    }
                    else if (helper_seat_sold(conn, seat, row, section, pid)) {
                        System.out.print("The seat is sold. Ticket not booked.\n");
                        continue;
                    }
                    else if (helper_seat_blocked(conn, seat, row, section, pid)) {
                        System.out.print("The seat is blocked. Ticket not booked.\n");
                        continue;
                    }
                    int tid;
                    try (PreparedStatement tps = conn.prepareStatement(
                        "INSERT INTO Tickets (face_value, oid) VALUES (" +
                        "(SELECT Price_tiers.price " +
                        "FROM Section_pricetier JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name" +
                        "                                        AND Section_pricetier.pid = Price_tiers.pid " +
                        "WHERE Section_pricetier.section_name = ? AND Section_pricetier.pid = ?), " +
                        "?)",
                        Statement.RETURN_GENERATED_KEYS
                    )) {
                        tps.setString(1, section);
                        tps.setInt(2, pid);
                        tps.setInt(3, oid);
                        tps.executeUpdate();
                        ResultSet keys = tps.getGeneratedKeys();
                        if (!keys.next())
                            throw new SQLException("Creating ticket failed: generating TID failed.");
                        tid = keys.getInt(1);
                        keys.close();
                    }
                    try (PreparedStatement rtps = conn.prepareStatement(
                        "INSERT INTO Reserved_tickets (tid, seat, `row`, section_name, venue_name) VALUES (?, ?, ?, ?, ?) "
                    )) {
                        rtps.setInt(1, tid);
                        rtps.setInt(2, seat);
                        rtps.setInt(3, row);
                        rtps.setString(4, section);
                        rtps.setString(5, venue);
                        rtps.executeUpdate();
                        System.out.println("Ticket booked successfully.");
                        i++;
                    }
                }
                else if (type.equals("g")) {
                    System.out.print("Enter section name:\n");
                    String section = scanner.nextLine();
                    if (!helper_general_section_exists(conn, section, pid)) {
                        System.out.print("Section doesn't exist. Ticket not booked.\n");
                        continue;
                    }
                    if (!helper_general_ticket_available(conn, section, pid)) {
                        System.out.print("Section tickets sold out. Ticket not booked.\n");
                        continue;
                    }
                    int tid;
                    try (PreparedStatement tps = conn.prepareStatement(
                        "INSERT INTO Tickets (face_value, oid) VALUES (" +
                        "(SELECT Price_tiers.price " +
                        "FROM Section_pricetier JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name" +
                        "                                        AND Section_pricetier.pid = Price_tiers.pid " +
                        "WHERE Section_pricetier.section_name = ? AND Section_pricetier.pid = ?), " +
                        "?)",
                        Statement.RETURN_GENERATED_KEYS
                    )) {
                        tps.setString(1, section);
                        tps.setInt(2, pid);
                        tps.setInt(3, oid);
                        tps.executeUpdate();
                        ResultSet keys = tps.getGeneratedKeys();
                        if (!keys.next())
                            throw new SQLException("Creating ticket failed: generating TID failed.");
                        tid = keys.getInt(1);
                        keys.close();
                    }
                    try (PreparedStatement rtps = conn.prepareStatement(
                        "INSERT INTO General_tickets (tid, section_name, venue_name) VALUES (?, ?, ?) "
                    )) {
                        rtps.setInt(1, tid);
                        rtps.setString(2, section);
                        rtps.setString(3, venue);
                        rtps.executeUpdate();
                        System.out.println("Ticket booked successfully.");
                        i++;
                    }
                }
                else {
                    System.out.print("Invalid choice.\n");
                }
            }
            conn.commit();
        }
        catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o10_cancel_performance(Connection conn, Scanner scanner) throws SQLException {
         System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());

        if (!helper_organizer_exists(conn, uid)) {
            System.out.print("Invalid UID.\n");
            return;
        }

        System.out.print("Here are the events you have created:\n");

        try (PreparedStatement eventps = conn.prepareStatement(
                "SELECT eid, name " +
                "FROM Events " +
                "WHERE uid = ?")) {
            eventps.setInt(1, uid);

            try (ResultSet rs = eventps.executeQuery()) {
                System.out.printf(
                        "%-10s %-30s%n",
                        "EID", "Event name");
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-30s%n",
                            rs.getInt("eid"),
                            rs.getString("name"));
                }
            }
        }

        System.out.print("Enter EID:\n");
        int eid = Integer.parseInt(scanner.nextLine());

        if (!helper_event_exists(conn, eid, uid)) {
            System.out.print("Invalid EID.\n");
            return;
        }

        System.out.print(
                "Here are the performances of this event:\n");

        try (PreparedStatement performances = conn.prepareStatement(
                "SELECT pid, datetime, venue_name " +
                "FROM Performances " +
                "WHERE eid = ?  AND cancel_datetime IS NULL")) {
            performances.setInt(1, eid);

            try (ResultSet rs = performances.executeQuery()) {
                System.out.printf(
                        "%-10s %-25s %-30s%n",
                        "PID", "Datetime", "Venue");
                System.out.println("-".repeat(65));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-25s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"));
                }
            }
        }

        System.out.print("Which performance are you going to cancel? Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_exists_witheid(conn, pid, eid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement( // cancel performance
            "UPDATE Performances " +
            "SET cancel_datetime = CURRENT_TIMESTAMP " +
            "WHERE pid = ? and cancel_datetime IS NULL"
            )) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            try (PreparedStatement tps = conn.prepareStatement( // cancel tickets
            "UPDATE Tickets " +
            "JOIN Orders ON Tickets.oid = Orders.oid " +
            "SET Tickets.cancel_datetime = CURRENT_TIMESTAMP " +
            "WHERE Orders.pid = ? and Tickets.cancel_datetime IS NULL"
            )) {
                tps.setInt(1, pid);
                tps.executeUpdate();
            }

            try (PreparedStatement lps = conn.prepareStatement( // withdraw active listings
            "UPDATE Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
            "                JOIN Orders ON Tickets.oid = Orders.oid " +
            "                JOIN Performances ON Orders.pid = Performances.pid " +
            "SET Listings.withdraw_datetime = CURRENT_TIMESTAMP " +
            "WHERE Performances.pid = ? AND Listings.buyer_id IS NULL AND Listings.withdraw_datetime IS NULL"
            )) {
                lps.setInt(1, pid);
                lps.executeUpdate();
                System.out.print("Performance cancelled successfully.\n");
            }

            conn.commit();
        }
        catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
        }
    }

    private static void o11_cancel_ticket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }

        System.out.print("Would you like to cancel a reserved ticket or general ticket? r/g\n");
        String type = scanner.nextLine();
        
        if (type.equals("r")) {
            System.out.print("Here are the reserved tickets you booked (that are still cancellable):\n");

            try (PreparedStatement tps = conn.prepareStatement(
                "SELECT Tickets.tid, Reserved_tickets.seat, Reserved_tickets.`row`, Reserved_tickets.section_name, Events.name AS event_name, Performances.pid " +
                "FROM Orders JOIN Tickets on Orders.oid = Tickets.oid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Events ON Performances.eid = Events.eid " +
                "            JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                "WHERE Orders.uid = ? AND Tickets.cancel_datetime IS NULL " + // The user bought the ticket from system, not listing.
                                                                              // The user hasn't cancelled the ticket.
                "  AND Performances.datetime >= CURRENT_TIMESTAMP + INTERVAL 7 DAY " + // 7 days before the performance
                "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed, he has withdrawn.
                "                  FROM Listings " +
                "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL) " +
                "ORDER BY Orders.datetime DESC"
            )) {
                tps.setInt(1, uid);
                tps.setInt(2, uid);
                try (ResultSet rs = tps.executeQuery()) {
                    System.out.printf("%-8s %-25s %-8s %-12s %-8s %-8s%n",
                                    "TID", "Event", "PID", "Section", "Row", "Seat");
                    System.out.println("-".repeat(75));

                    while (rs.next()) {
                    System.out.printf("%-8d %-25s %-8d %-12s %-8d %-8d%n",
                        rs.getInt("tid"),
                        rs.getString("event_name"),
                        rs.getInt("pid"),
                        rs.getString("section_name"),
                        rs.getInt("row"),
                        rs.getInt("seat"));
                    }
                }
            }

            System.out.print("Which ticket are you going to cancel? Enter TID:\n");
            int tid = Integer.parseInt(scanner.nextLine());
            if (!helper_reserved_ticket_can_cancel(conn, tid, uid)) {
                System.out.print("Invalid TID.\n");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Tickets " +
                "SET cancel_datetime = CURRENT_TIMESTAMP " +
                "WHERE Tickets.tid = ?"
            )) {
                ps.setInt(1, tid);
                ps.executeUpdate();
                System.out.print("Successfully cancelled ticket.\n");
            }
        }
        else if (type.equals("g")) {
            System.out.print("Here are the general tickets you booked (that are still cancellable):\n");

            try (PreparedStatement tps = conn.prepareStatement(
                "SELECT Tickets.tid, General_tickets.section_name, Events.name AS event_name, Performances.pid " +
                "FROM Orders JOIN Tickets on Orders.oid = Tickets.oid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Events ON Performances.eid = Events.eid " +
                "            JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                "WHERE Orders.uid = ? AND Tickets.cancel_datetime IS NULL " +
                "  AND Performances.datetime >= CURRENT_TIMESTAMP + INTERVAL 7 DAY " +
                "  AND NOT EXISTS (SELECT * " +
                "                  FROM Listings " +
                "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL) " +
                "ORDER BY Orders.datetime DESC"
            )) {
                tps.setInt(1, uid);
                tps.setInt(2, uid);
                try (ResultSet rs = tps.executeQuery()) {
                    System.out.printf("%-8s %-25s %-8s %-12s%n",
                                    "TID", "Event", "PID", "Section");
                    System.out.println("-".repeat(60));

                    while (rs.next()) {
                    System.out.printf("%-8d %-25s %-8d %-12s%n",
                        rs.getInt("tid"),
                        rs.getString("event_name"),
                        rs.getInt("pid"),
                        rs.getString("section_name"));
                    }
                }
            }

            System.out.print("Which ticket are you going to cancel? Enter TID:\n");
            int tid = Integer.parseInt(scanner.nextLine());
            if (!helper_general_ticket_can_cancel(conn, tid, uid)) {
                System.out.print("Invalid TID.\n");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Tickets " +
                "SET cancel_datetime = CURRENT_TIMESTAMP " +
                "WHERE Tickets.tid = ?"
            )) {
                ps.setInt(1, tid);
                ps.executeUpdate();
                System.out.print("Successfully cancelled ticket.\n");
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void o12_list_ticket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }
        
        System.out.print("Would you like to list a reserved ticket or general ticket? r/g\n");
        String type = scanner.nextLine();
        
        if (type.equals("r")) {
            System.out.print("Here are the reserved tickets you booked:\n");

            try (PreparedStatement tps = conn.prepareStatement(
                "SELECT Tickets.tid, Reserved_tickets.seat, Reserved_tickets.`row`, Reserved_tickets.section_name, Events.name AS event_name, Performances.pid " +
                "FROM Orders JOIN Tickets ON Orders.oid = Tickets.oid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Events ON Performances.eid = Events.eid " +
                "            JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                "WHERE Performances.datetime >= CURRENT_TIMESTAMP " +
                "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
                "                                 FROM Listings " +
                "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
                "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
                "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
                "                  FROM Listings " +
                "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL) " +
                "ORDER BY Orders.datetime DESC"
            )) {
                tps.setInt(1, uid);
                tps.setInt(2, uid);
                tps.setInt(3, uid);
                try (ResultSet rs = tps.executeQuery()) {
                    System.out.printf("%-8s %-25s %-8s %-12s %-8s %-8s%n",
                                    "TID", "Event", "PID", "Section", "Row", "Seat");
                    System.out.println("-".repeat(75));

                    while (rs.next()) {
                    System.out.printf("%-8d %-25s %-8d %-12s %-8d %-8d%n",
                        rs.getInt("tid"),
                        rs.getString("event_name"),
                        rs.getInt("pid"),
                        rs.getString("section_name"),
                        rs.getInt("row"),
                        rs.getInt("seat"));
                    }
                }
            }

            System.out.print("Which ticket are you going to list? Enter TID:\n");
            int tid = Integer.parseInt(scanner.nextLine());
            if (!helper_reserved_ticket_can_list(conn, tid, uid)) {
                System.out.print("Invalid TID.\n");
                return;
            }

            BigDecimal max = helper_highest_resale_price(conn, tid);
            System.out.printf("You can list it at a maximum price %s dollars. How much are you listing?\n", max);
            BigDecimal price = new BigDecimal(scanner.nextLine());
            if (price.compareTo(max) > 0) {
                System.out.print("You are listing higher than the maximum price.\n");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Listings (tid, seller_id, list_datetime, price) VALUES (?, ?, CURRENT_TIMESTAMP, ?)"
            )) {
                ps.setInt(1, tid);
                ps.setInt(2, uid);
                ps.setBigDecimal(3, price);
                ps.executeUpdate();
                System.out.print("Successfully listed ticket.\n");
            }
        }
        else if (type.equals("g")) {
            System.out.print("Here are the general tickets you booked:\n");

            try (PreparedStatement tps = conn.prepareStatement(
                "SELECT Tickets.tid, General_tickets.section_name, Events.name AS event_name, Performances.pid " +
                "FROM Orders JOIN Tickets on Orders.oid = Tickets.oid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Events ON Performances.eid = Events.eid " +
                "            JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                "WHERE Performances.datetime >= CURRENT_TIMESTAMP " +
                "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
                "                                 FROM Listings " +
                "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
                "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
                "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
                "                  FROM Listings " +
                "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL) " +
                "ORDER BY Orders.datetime DESC"
            )) {
                tps.setInt(1, uid);
                tps.setInt(2, uid);
                tps.setInt(3, uid);
                try (ResultSet rs = tps.executeQuery()) {
                    System.out.printf("%-8s %-25s %-8s %-12s%n",
                                    "TID", "Event", "PID", "Section");
                    System.out.println("-".repeat(60));

                    while (rs.next()) {
                    System.out.printf("%-8d %-25s %-8d %-12s%n",
                        rs.getInt("tid"),
                        rs.getString("event_name"),
                        rs.getInt("pid"),
                        rs.getString("section_name"));
                    }
                }
            }

            System.out.print("Which ticket are you going to list? Enter TID:\n");
            int tid = Integer.parseInt(scanner.nextLine());
            if (!helper_general_ticket_can_list(conn, tid, uid)) {
                System.out.print("Invalid TID.\n");
                return;
            }

            BigDecimal max = helper_highest_resale_price(conn, tid);
            System.out.printf("You can list it at a maximum price %s dollars. How much are you listing?\n", max);
            BigDecimal price = new BigDecimal(scanner.nextLine());
            if (price.compareTo(max) > 0) {
                System.out.print("You are listing higher than the maximum price.\n");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Listings (tid, seller_id, list_datetime, price) VALUES (?, ?, CURRENT_TIMESTAMP, ?)"
            )) {
                ps.setInt(1, tid);
                ps.setInt(2, uid);
                ps.setBigDecimal(3, price);
                ps.executeUpdate();
                System.out.print("Successfully listed ticket.\n");
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void o13_withdraw_listing(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }

        System.out.print("Here are all your listings (that are withdrawable):\n");

        try (PreparedStatement pps = conn.prepareStatement(
            "SELECT Listings.lid, Listings.price, Listings.list_datetime, Events.name, Performances.pid " +
            "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
            "              JOIN Orders ON Tickets.oid = Orders.oid " +
            "              JOIN Performances ON Orders.pid = Performances.pid " +
            "              JOIN Events ON Performances.eid = Events.eid " +
            "WHERE Listings.seller_id = ? " +
            "  AND Listings.buyer_id IS NULL " +
            "  AND Listings.withdraw_datetime IS NULL " +
            "ORDER BY Listings.list_datetime DESC"
        )) {
            pps.setInt(1, uid);

            try (ResultSet rs = pps.executeQuery()) {
                System.out.printf("%-10s %-20s %-20s %-20s %-10s%n",
                    "LID", "List price", "List datetime", "Event name", "PID");
                System.out.println("-".repeat(80));

                while (rs.next()) {
                System.out.printf("%-10d %-20s %-20s %-20s %-10d%n",
                    rs.getInt("lid"),
                    rs.getBigDecimal("price"),
                    rs.getTimestamp("list_datetime"),
                    rs.getString("name"),
                    rs.getInt("pid"));
                }
            }
        }

        System.out.print("Which listing are you going to withdraw? Enter LID:\n");
        int lid = Integer.parseInt(scanner.nextLine());
        if (!helper_listing_can_withdraw(conn, lid, uid)) {
            System.out.print("Invalid LID.\n");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
        "UPDATE Listings " +
        "SET withdraw_datetime = CURRENT_TIMESTAMP " +
        "WHERE lid = ? " +
        "  AND seller_id = ? " +
        "  AND buyer_id IS NULL " +
        "  AND withdraw_datetime IS NULL"
        )) {
            ps.setInt(1, lid);
            ps.setInt(2, uid);

            int rows = ps.executeUpdate();

            if (rows == 1)
                System.out.print("Successfully withdrawn listing.\n");
            else
                System.out.print("Failed to withdraw listing.\n");
        }
    }

    private static void o14_purchase_listing(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }

        System.out.print("Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_exists(conn, pid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        try (PreparedStatement date = conn.prepareStatement(
            "SELECT datetime " +
            "FROM Performances " +
            "WHERE pid = ?"
        )) {
            date.setInt(1, pid);

            try (ResultSet rs = date.executeQuery()) {
                rs.next();
                Timestamp time = rs.getTimestamp("datetime");

                if (!time.after(new Timestamp(System.currentTimeMillis()))) {
                    System.out.println("Only future performances listings can be purchased.");
                    return;
                }
            }
        }

        System.out.print("Here are the available listings:\n");

        try (PreparedStatement lps = conn.prepareStatement(
            "SELECT Listings.lid, Listings.seller_id, Listings.price " +
            "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
            "              JOIN Orders ON Tickets.oid = Orders.oid " +
            "              JOIN Performances ON Orders.pid = Performances.pid " +
            "WHERE Performances.pid = ? " +
            "  AND Listings.withdraw_datetime IS NULL " +
            "  AND Listings.buyer_id IS NULL"
        )) {
            lps.setInt(1, pid);

            try (ResultSet rs = lps.executeQuery()) {
                System.out.printf(
                        "%-10s %-15s %-15s%n",
                        "LID", "Seller UID", "Price"
                );
                System.out.println("-".repeat(40));

                while (rs.next()) {
                    System.out.printf(
                            "%-10d %-15d %-15s%n",
                            rs.getInt("lid"),
                            rs.getInt("seller_id"),
                            rs.getBigDecimal("price")
                    );
                }
            }
        }

        System.out.print("Which listing are you going to purchase?\n");
        int lid = Integer.parseInt(scanner.nextLine());
        if (!helper_listing_available(conn, lid, pid)) {
            System.out.print("Invalid LID.\n");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
            "UPDATE Listings " +
            "SET buyer_id = ?, trans_datetime = CURRENT_TIMESTAMP " +
            "WHERE lid = ?"
        )) {
            ps.setInt(1, uid);
            ps.setInt(2, lid);
            ps.executeUpdate();
            System.out.print("Successfully purchased lisitng.\n");
        }
    }

    private static void o15_review_performance(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your UID:\n");
        int uid = Integer.parseInt(scanner.nextLine());
        if (!helper_customer_exists(conn, uid)) {
            System.out.print("You don't have an account yet.\n");
            return;
        }

        System.out.print("Here are the performances you attended in a month:\n");

        try (PreparedStatement pps = conn.prepareStatement(
            "SELECT DISTINCT Performances.pid, Events.name, Performances.datetime " +
            "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
            "             JOIN Performances ON Orders.pid = Performances.pid " +
            "             JOIN Events ON Performances.eid = Events.eid " +
            "WHERE Performances.datetime <= CURRENT_TIMESTAMP " +
            "  AND Performances.datetime >= CURRENT_TIMESTAMP - INTERVAL 1 MONTH " +
            "  AND (Orders.uid = ? OR EXISTS (SELECT * " + // The user either bought it from system or a listing of another user.
            "                                 FROM Listings " +
            "                                 WHERE Listings.tid = Tickets.tid AND Listings.buyer_id = ?)) " +
            "  AND Tickets.cancel_datetime IS NULL " + // The ticket is not cancelled.
            "  AND NOT EXISTS (SELECT * " + // The user hasn't listed the ticket, or if he listed it, he has withdrawn.
            "                  FROM Listings " +
            "                  WHERE Listings.tid = Tickets.tid AND Listings.seller_id = ? AND Listings.withdraw_datetime IS NULL) " +
            "ORDER BY Performances.datetime DESC"
        )) {
            pps.setInt(1, uid);
            pps.setInt(2, uid);
            pps.setInt(3, uid);

            try (ResultSet rs = pps.executeQuery()) {
                System.out.printf("%-8s %-25s %-20s%n",
                    "PID", "Event name", "datetime");
                System.out.println("-".repeat(55));

                while (rs.next()) {
                System.out.printf("%-8d %-25s %-20s%n",
                    rs.getInt("pid"),
                    rs.getString("name"),
                    rs.getTimestamp("datetime"));
                }
            }
        }

        System.out.print("Which performance are you going to review? Enter PID:\n");
        int pid = Integer.parseInt(scanner.nextLine());
        if (!helper_performance_reviewable(conn, pid, uid)) {
            System.out.print("Invalid PID.\n");
            return;
        }

        System.out.print("Score the event: 1-5\n");
        int event_score = Integer.parseInt(scanner.nextLine());
        System.out.print("Score the venue: 1-5\n");
        int venue_score = Integer.parseInt(scanner.nextLine());
        System.out.print("Provide your comment on the performance:\n");
        String comment = scanner.nextLine();

        try (PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO Review (uid, pid, comment, event_score, venue_score) VALUES (?, ?, ?, ?, ?)"
        )) {
            ps.setInt(1, uid);
            ps.setInt(2, pid);
            ps.setString(3, comment);
            ps.setInt(4, event_score);
            ps.setInt(5, venue_score);
            int rows = ps.executeUpdate();
                    if (rows == 1)
                        System.out.println("Review created successfully.");
                    else
                        System.out.println("You have reviewed this performance before.");
        }
    }

    private static void q1_search_performances_by_lat_lon(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter latitude:");
        String latStr = scanner.nextLine();
        double latitude = Double.parseDouble(latStr);
        System.out.print("Enter longitude:");
        String lonStr = scanner.nextLine();
        double longitude = Double.parseDouble(lonStr);

        if (latitude > 90 || latitude < -90) {
            System.out.println("Latitude is out of range [-90, 90].\n");
            return;
        }
        if (longitude > 180 || longitude < -180) {
            System.out.println("Longitude is out of range [-180, 180].\n");
            return;
        }

        System.out.printf(
            "Enter search distance in kilometres, or press Enter to use the default %.1f km:\n",
            DEFAULT_RADIUS
        );
        String distanceStr = scanner.nextLine();
        double distance;
        if (distanceStr.equals(""))
            distance = DEFAULT_RADIUS;
        else
            distance = Double.parseDouble(distanceStr);

        if (distance <= 0) {
            System.out.println("Distance must be greater than 0.\n");
            return;
        }

        System.out.print("Would you like the results be ordered by distance, or cheapest available ticket price ascending, or descending? 1/2/3\n");
        String order = scanner.nextLine();

        if (order.equals("1")) {
            String sql =
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                "      AND Performances.datetime >= CURRENT_TIMESTAMP " +
                "      AND Performances.cancel_datetime IS NULL " +
                "ORDER BY POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "         POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) ";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, latitude);
                ps.setDouble(4, distance);
                ps.setDouble(5, latitude);
                ps.setDouble(6, longitude);
                ps.setDouble(7, latitude);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-5s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                    System.out.println("-".repeat(90));
                    while (rs.next()) {
                        System.out.printf("%-5d %-30s %-30s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("event_name"),
                            rs.getString("datetime"),
                            rs.getString("venue_name")
                        );
                    }
                }
            }
        }
        else if (order.equals("2") || order.equals("3")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Pid_price");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_capacity");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_sold");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_blocked");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_capacity");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_sold");

                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_sold AS " +
                    "SELECT Orders.pid, Reserved_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Tickets JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                    "             JOIN Orders ON Tickets.oid = Orders.oid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Orders.pid, Reserved_tickets.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_capacity AS " +
                    "SELECT Performances.pid, Seats.section_name, COUNT(*) AS capacity " +
                    "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                    "GROUP BY Performances.pid, Seats.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_blocked AS " +
                    "SELECT Performances.pid, Block.section_name, COUNT(*) AS blocked " +
                    "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                    "GROUP BY Performances.pid, Block.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section AS " +
                    "SELECT Reserved_pid_section_capacity.pid, Reserved_pid_section_capacity.section_name " +
                    "FROM Reserved_pid_section_capacity LEFT JOIN Reserved_pid_section_sold ON Reserved_pid_section_capacity.pid = Reserved_pid_section_sold.pid " +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_sold.section_name " +
                    "                                   LEFT JOIN Reserved_pid_section_blocked ON Reserved_pid_section_capacity.pid = Reserved_pid_section_blocked.pid " +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_blocked.section_name " +
                    "WHERE IFNULL(Reserved_pid_section_sold.sold, 0) < Reserved_pid_section_capacity.capacity - IFNULL(Reserved_pid_section_blocked.blocked, 0) "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section_sold AS " +
                    "SELECT Orders.pid, General_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Tickets JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                    "             JOIN Orders ON Tickets.oid = Orders.oid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Orders.pid, General_tickets.section_name "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section_capacity AS " +
                    "SELECT Performances.pid, General_sections.name AS section_name, " +
                    "       General_sections.total_capacity AS capacity " +
                    "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section AS " +
                    "SELECT General_pid_section_capacity.pid, General_pid_section_capacity.section_name " +
                    "FROM General_pid_section_capacity LEFT JOIN General_pid_section_sold ON General_pid_section_capacity.pid = General_pid_section_sold.pid " +
                    "                                                                     AND General_pid_section_capacity.section_name = General_pid_section_sold.section_name " +
                    "WHERE IFNULL(General_pid_section_sold.sold, 0) < General_pid_section_capacity.capacity "
                );
                stmt.executeUpdate( // useful for cheapest available ticket
                    "CREATE TEMPORARY TABLE Pid_price AS " +
                    "SELECT Available.pid, MIN(Price_tiers.price) AS cheapest_available_ticket " +
                    "FROM ((SELECT pid, section_name FROM Reserved_pid_section) UNION (SELECT pid, section_name FROM General_pid_section)) AS Available " +
                    "                          NATURAL JOIN Section_pricetier " +
                    "                          JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                                           AND Section_pricetier.pid = Price_tiers.pid " +
                    "GROUP BY Available.pid"
                );
            }

            if (order.equals("2")) {
                try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, Pid_price.cheapest_available_ticket AS price " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                "      AND Performances.datetime >= CURRENT_TIMESTAMP " +
                "      AND Performances.cancel_datetime IS NULL " +
                "ORDER BY Pid_price.cheapest_available_ticket ASC"
            )) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, latitude);
                ps.setDouble(4, distance);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-5s %-30s %-25s %-30s %-10s%n", "PID", "Event", "Datetime", "Venue", "Cheapest available ticket price");
                    System.out.println("-".repeat(130));
                    while (rs.next()) {
                        System.out.printf("%-5d %-30s %-25s %-30s %-10s%n",
                            rs.getInt("pid"),
                            rs.getString("event_name"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"),
                            rs.getBigDecimal("price")
                        );
                    }
                }
            }
            }
            else {
                try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, Pid_price.cheapest_available_ticket AS price " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                "      AND Performances.datetime >= CURRENT_TIMESTAMP " +
                "      AND Performances.cancel_datetime IS NULL " +
                "ORDER BY Pid_price.cheapest_available_ticket DESC"
            )) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, latitude);
                ps.setDouble(4, distance);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-5s %-30s %-25s %-30s %-10s%n", "PID", "Event", "Datetime", "Venue", "Cheapest available ticket price");
                    System.out.println("-".repeat(130));
                    while (rs.next()) {
                        System.out.printf("%-5d %-30s %-25s %-30s %-10s%n",
                            rs.getInt("pid"),
                            rs.getString("event_name"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"),
                            rs.getBigDecimal("price")
                        );
                    }
                }
            }
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void q2_search_performances_by_postalcode(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter postal code:");
        String postal = scanner.nextLine();

        postal = postal.replace(" ", "").toUpperCase();
        if (!postal.matches("[A-Za-z0-9]{5,6}")) {
            System.out.println("Postal doesn't have right format.\n");
            return;
        }

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE LEFT(Venues.postal_code, 3) = LEFT(?, 3) " +
            "      AND Performances.cancel_datetime IS NULL " +
            "      AND Performances.datetime >= CURRENT_TIMESTAMP ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postal);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-10s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                System.out.println("-".repeat(90));
                while (rs.next()) {
                    System.out.printf("%-10d %-30s %-30s %-30s%n",
                        rs.getInt("pid"),
                        rs.getString("event_name"),
                        rs.getString("datetime"),
                        rs.getString("venue_name")
                    );
                }
            }
        }
    }

    private static void q3_search_performances_by_address(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter country:");
        String country = scanner.nextLine();
        System.out.print("Enter city:");
        String city = scanner.nextLine();
        System.out.print("Enter postal code:");
        String postal = scanner.nextLine();

        postal = postal.replace(" ", "").toUpperCase();

        System.out.print("Here are the venues:\n");

        try(PreparedStatement vps = conn.prepareStatement(
            "SELECT name " +
            "FROM Venues " +
            "WHERE country = ? AND city = ? AND postal_code = ?"
        )) {
            vps.setString(1, country);
            vps.setString(2, city);
            vps.setString(3, postal);
            try (ResultSet vrs = vps.executeQuery()) {
                while (vrs.next()) {
                    System.out.printf("%s", vrs.getString("name"));
                }
            }
        }

        System.out.print("Here are the performances:\n");

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE Venues.country = ? AND Venues.city = ? AND Venues.postal_code = ? " +
            "      AND Performances.cancel_datetime IS NULL " +
            "      AND Performances.datetime >= CURRENT_TIMESTAMP ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, country);
            ps.setString(2, city);
            ps.setString(3, postal);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-10s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                System.out.println("-".repeat(90));
                while (rs.next()) {
                    System.out.printf("%-10d %-30s %-30s %-30s%n",
                        rs.getInt("pid"),
                        rs.getString("event_name"),
                        rs.getString("datetime"),
                        rs.getString("venue_name")
                    );
                }
            }
        }
    }

    private static void q4_refine_q1(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter latitude:");
        String latStr = scanner.nextLine();
        double latitude = Double.parseDouble(latStr);
        System.out.print("Enter longitude:");
        String lonStr = scanner.nextLine();
        double longitude = Double.parseDouble(lonStr);

        if (latitude > 90 || latitude < -90) {
            System.out.println("Latitude is out of range [-90, 90].\n");
            return;
        }
        if (longitude > 180 || longitude < -180) {
            System.out.println("Longitude is out of range [-180, 180].\n");
            return;
        }

        System.out.printf(
            "Enter search distance in kilometres, or press Enter to use the default %.1f km: ",
            DEFAULT_RADIUS
        );
        String distanceStr = scanner.nextLine();
        double distance;
        if (distanceStr.equals(""))
            distance = DEFAULT_RADIUS;
        else
            distance = Double.parseDouble(distanceStr);

        if (distance <= 0) {
            System.out.println("Distance must be greater than 0.\n");
            return;
        }

        System.out.print("Would you like the results be ordered by distance, or cheapest available ticket price ascending, or descending? 1/2/3\n");
        String order = scanner.nextLine();

        System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS ");
        String start = scanner.nextLine();
        System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS ");
        String end = scanner.nextLine();
        System.out.print("Enter availability: ");
        int available = Integer.parseInt(scanner.nextLine());

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Sold_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_blocked_table");

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Sold_table AS " +
                "SELECT Orders.pid, COUNT(*) AS sold " +
                "FROM Orders JOIN Tickets ON Orders.oid = Tickets.oid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Orders.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_capacity_table AS " +
                "SELECT Performances.pid, COUNT(*) AS reserved_capacity " +
                "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_capacity_table AS " +
                "SELECT Performances.pid, SUM(General_sections.total_capacity) AS general_capacity " +
                "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_blocked_table AS " +
                "SELECT Performances.pid, COUNT(*) AS blocked " +
                "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                "GROUP BY Performances.pid"
            );
        }

        if (order.equals("1")) {
            String sql =
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
                "                  LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
                "                  LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
                "                  LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
                "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                "  AND Performances.datetime BETWEEN ? AND ? " +
                "  AND Performances.cancel_datetime IS NULL " +
                "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) " +
                "      - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ? " +
                "ORDER BY POWER((Venues.latitude - ?) * 111.0, 2) + " +
                "         POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) ";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, latitude);
                ps.setDouble(4, distance);
                ps.setString(5, start);
                ps.setString(6, end);
                ps.setInt(7, available);
                ps.setDouble(8, latitude);
                ps.setDouble(9, longitude);
                ps.setDouble(10, latitude);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-5s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                    System.out.println("-".repeat(90));
                    while (rs.next()) {
                        System.out.printf("%-5d %-30s %-30s %-30s%n",
                            rs.getInt("pid"),
                            rs.getString("event_name"),
                            rs.getString("datetime"),
                            rs.getString("venue_name")
                        );
                    }
                }
            }
        }
        else if (order.equals("2") || order.equals("3")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Pid_price");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_capacity");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_sold");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_blocked");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_capacity");
                stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_sold");

                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_sold AS " +
                    "SELECT Orders.pid, Reserved_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Tickets JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                    "             JOIN Orders ON Tickets.oid = Orders.oid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Orders.pid, Reserved_tickets.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_capacity AS " +
                    "SELECT Performances.pid, Seats.section_name, COUNT(*) AS capacity " +
                    "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                    "GROUP BY Performances.pid, Seats.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section_blocked AS " +
                    "SELECT Performances.pid, Block.section_name, COUNT(*) AS blocked " +
                    "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                    "GROUP BY Performances.pid, Block.section_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_pid_section AS " +
                    "SELECT Reserved_pid_section_capacity.pid, Reserved_pid_section_capacity.section_name " +
                    "FROM Reserved_pid_section_capacity LEFT JOIN Reserved_pid_section_sold ON Reserved_pid_section_capacity.pid = Reserved_pid_section_sold.pid " +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_sold.section_name " +
                    "                                   LEFT JOIN Reserved_pid_section_blocked ON Reserved_pid_section_capacity.pid = Reserved_pid_section_blocked.pid " +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_blocked.section_name " +
                    "WHERE IFNULL(Reserved_pid_section_sold.sold, 0) < Reserved_pid_section_capacity.capacity - IFNULL(Reserved_pid_section_blocked.blocked, 0) "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section_sold AS " +
                    "SELECT Orders.pid, General_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Tickets JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                    "             JOIN Orders ON Tickets.oid = Orders.oid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Orders.pid, General_tickets.section_name "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section_capacity AS " +
                    "SELECT Performances.pid, General_sections.name AS section_name, " +
                    "       General_sections.total_capacity AS capacity " +
                    "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name"
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section AS " +
                    "SELECT General_pid_section_capacity.pid, General_pid_section_capacity.section_name " +
                    "FROM General_pid_section_capacity LEFT JOIN General_pid_section_sold ON General_pid_section_capacity.pid = General_pid_section_sold.pid " +
                    "                                                                     AND General_pid_section_capacity.section_name = General_pid_section_sold.section_name " +
                    "WHERE IFNULL(General_pid_section_sold.sold, 0) < General_pid_section_capacity.capacity "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Pid_price AS " +
                    "SELECT Available.pid, MIN(Price_tiers.price) AS cheapest_available_ticket " +
                    "FROM ((SELECT pid, section_name FROM Reserved_pid_section) UNION (SELECT pid, section_name FROM General_pid_section)) AS Available " +
                    "                          NATURAL JOIN Section_pricetier " +
                    "                          JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                                           AND Section_pricetier.pid = Price_tiers.pid " +
                    "GROUP BY Available.pid"
                );
            }

            if (order.equals("2")) {
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, Pid_price.cheapest_available_ticket AS price " +
                    "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                    "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                    "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                    "                  LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
                    "                  LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
                    "                  LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
                    "                  LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
                    "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                    "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                    "  AND Performances.datetime BETWEEN ? AND ? " +
                    "  AND Performances.cancel_datetime IS NULL " +
                    "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) " +
                    "      - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ? " +
                    "ORDER BY Pid_price.cheapest_available_ticket ASC"
                )) {
                    ps.setDouble(1, latitude);
                    ps.setDouble(2, longitude);
                    ps.setDouble(3, latitude);
                    ps.setDouble(4, distance);
                    ps.setString(5, start);
                    ps.setString(6, end);
                    ps.setInt(7, available);

                    try (ResultSet rs = ps.executeQuery()) {
                        System.out.printf("%-5s %-30s %-25s %-30s %-10s%n", "PID", "Event", "Datetime", "Venue", "Cheapest available ticket price");
                        System.out.println("-".repeat(130));
                        while (rs.next()) {
                            System.out.printf("%-5d %-30s %-25s %-30s %-10s%n",
                                rs.getInt("pid"),
                                rs.getString("event_name"),
                                rs.getString("datetime"),
                                rs.getString("venue_name"),
                                rs.getBigDecimal("price")
                            );
                        }
                    }
                }
            }
            else {
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, Pid_price.cheapest_available_ticket AS price " +
                    "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                    "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                    "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                    "                  LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
                    "                  LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
                    "                  LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
                    "                  LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
                    "WHERE POWER((Venues.latitude - ?) * 111.0, 2) + " +
                    "      POWER((Venues.longitude - ?) * 111.0 * COS(RADIANS(?)), 2) <= POWER(?, 2) " +
                    "  AND Performances.datetime BETWEEN ? AND ? " +
                    "  AND Performances.cancel_datetime IS NULL " +
                    "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) " +
                    "      - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ? " +
                    "ORDER BY Pid_price.cheapest_available_ticket DESC"
                )) {
                    ps.setDouble(1, latitude);
                    ps.setDouble(2, longitude);
                    ps.setDouble(3, latitude);
                    ps.setDouble(4, distance);
                    ps.setString(5, start);
                    ps.setString(6, end);
                    ps.setInt(7, available);

                    try (ResultSet rs = ps.executeQuery()) {
                        System.out.printf("%-5s %-30s %-25s %-30s %-10s%n", "PID", "Event", "Datetime", "Venue", "Cheapest available ticket price");
                        System.out.println("-".repeat(130));
                        while (rs.next()) {
                            System.out.printf("%-5d %-30s %-25s %-30s %-10s%n",
                                rs.getInt("pid"),
                                rs.getString("event_name"),
                                rs.getString("datetime"),
                                rs.getString("venue_name"),
                                rs.getBigDecimal("price")
                            );
                        }
                    }
                }
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void q4_refine_q2(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter postal code: ");
        String postal = scanner.nextLine();

        postal = postal.replace(" ", "").toUpperCase();
        if (!postal.matches("[A-Za-z0-9]{5,6}")) {
            System.out.println("Postal doesn't have right format.\n");
            return;
        }

        System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS ");
        String start = scanner.nextLine();
        System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS ");
        String end = scanner.nextLine();
        System.out.print("Enter availability: ");
        int available = Integer.parseInt(scanner.nextLine());

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Sold_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_blocked_table");

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Sold_table AS " +
                "SELECT Orders.pid, COUNT(*) AS sold " +
                "FROM Orders JOIN Tickets ON Orders.oid = Tickets.oid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Orders.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_capacity_table AS " +
                "SELECT Performances.pid, COUNT(*) AS reserved_capacity " +
                "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_capacity_table AS " +
                "SELECT Performances.pid, SUM(General_sections.total_capacity) AS general_capacity " +
                "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_blocked_table AS " +
                "SELECT Performances.pid, COUNT(*) AS blocked " +
                "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                "GROUP BY Performances.pid"
            );
        }

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "                  LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
            "                  LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
            "                  LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
            "                  LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
            "WHERE LEFT(Venues.postal_code, 3) = LEFT(?, 3) " +
            "  AND Performances.datetime BETWEEN ? AND ? " +
            "  AND Performances.cancel_datetime IS NULL " +
            "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) " +
            "      - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postal);
            ps.setString(2, start);
            ps.setString(3, end);
            ps.setInt(4, available);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-10s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                System.out.println("-".repeat(90));
                while (rs.next()) {
                    System.out.printf("%-10d %-30s %-30s %-30s%n",
                        rs.getInt("pid"),
                        rs.getString("event_name"),
                        rs.getString("datetime"),
                        rs.getString("venue_name")
                    );
                }
            }
        }
    }

    private static void q4_refine_q3(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter country: ");
        String country = scanner.nextLine();
        System.out.print("Enter city: ");
        String city = scanner.nextLine();
        System.out.print("Enter postal code: ");
        String postal = scanner.nextLine();

        postal = postal.replace(" ", "").toUpperCase();

        System.out.print("Here are the venues:\n");

        try(PreparedStatement vps = conn.prepareStatement(
            "SELECT name " +
            "FROM Venues " +
            "WHERE country = ? AND city = ? AND postal_code = ?"
        )) {
            vps.setString(1, country);
            vps.setString(2, city);
            vps.setString(3, postal);
            try (ResultSet vrs = vps.executeQuery()) {
                while (vrs.next()) {
                    System.out.printf("%s%n", vrs.getString("name"));
                }
            }
        }

        System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS ");
        String start = scanner.nextLine();
        System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS ");
        String end = scanner.nextLine();
        System.out.print("Enter availability: ");
        int available = Integer.parseInt(scanner.nextLine());

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Sold_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_capacity_table");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_blocked_table");

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Sold_table AS " +
                "SELECT Orders.pid, COUNT(*) AS sold " +
                "FROM Orders JOIN Tickets ON Orders.oid = Tickets.oid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Orders.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_capacity_table AS " +
                "SELECT Performances.pid, COUNT(*) AS reserved_capacity " +
                "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_capacity_table AS " +
                "SELECT Performances.pid, SUM(General_sections.total_capacity) AS general_capacity " +
                "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name " +
                "GROUP BY Performances.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_blocked_table AS " +
                "SELECT Performances.pid, COUNT(*) AS blocked " +
                "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                "GROUP BY Performances.pid"
            );
        }

        System.out.print("Here are the performances:\n");

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "                  LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
            "                  LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
            "                  LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
            "                  LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
            "WHERE Venues.country = ? AND Venues.city = ? AND Venues.postal_code = ? " +
            "  AND Performances.datetime BETWEEN ? AND ? " +
            "  AND Performances.cancel_datetime IS NULL " +
            "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) " +
            "      - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, country);
            ps.setString(2, city);
            ps.setString(3, postal);
            ps.setString(4, start);
            ps.setString(5, end);
            ps.setInt(6, available);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-10s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue");
                System.out.println("-".repeat(90));
                while (rs.next()) {
                    System.out.printf("%-10d %-30s %-30s %-30s%n",
                        rs.getInt("pid"),
                        rs.getString("event_name"),
                        rs.getString("datetime"),
                        rs.getString("venue_name")
                    );
                }
            }
        }
    }

    private static void q5_filters(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section_blocked");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_section");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_section");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Pid_price");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_pid_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_pid_blocked");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Num_available_tickets");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Filter_table");
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_section_sold AS " +
                "SELECT Orders.pid, Reserved_tickets.section_name, COUNT(*) AS sold " +
                "FROM Tickets JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                "             JOIN Orders ON Tickets.oid = Orders.oid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Orders.pid, Reserved_tickets.section_name"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_section_capacity AS " +
                "SELECT Performances.pid, Seats.section_name, COUNT(*) AS capacity " +
                "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                "GROUP BY Performances.pid, Seats.section_name"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_section_blocked AS " +
                "SELECT Performances.pid, Block.section_name, COUNT(*) AS blocked " +
                "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                "GROUP BY Performances.pid, Block.section_name"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_section AS " + 
                "SELECT Reserved_pid_section_capacity.pid, Reserved_pid_section_capacity.section_name " +
                "FROM Reserved_pid_section_capacity LEFT JOIN Reserved_pid_section_sold ON Reserved_pid_section_capacity.pid = Reserved_pid_section_sold.pid " +
                "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_sold.section_name " +
                "                                   LEFT JOIN Reserved_pid_section_blocked ON Reserved_pid_section_capacity.pid = Reserved_pid_section_blocked.pid " +
                "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_blocked.section_name " +
                "WHERE IFNULL(Reserved_pid_section_sold.sold, 0) < Reserved_pid_section_capacity.capacity - IFNULL(Reserved_pid_section_blocked.blocked, 0)"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_section_sold AS " +
                "SELECT Orders.pid, General_tickets.section_name, COUNT(*) AS sold " +
                "FROM Tickets JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                "             JOIN Orders ON Tickets.oid = Orders.oid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Orders.pid, General_tickets.section_name "
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_section_capacity AS " +
                "SELECT Performances.pid, General_sections.name AS section_name, " +
                "       General_sections.total_capacity AS capacity " +
                "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_section AS " +
                "SELECT General_pid_section_capacity.pid, General_pid_section_capacity.section_name " +
                "FROM General_pid_section_capacity LEFT JOIN General_pid_section_sold ON General_pid_section_capacity.pid = General_pid_section_sold.pid " +
                "                                                                     AND General_pid_section_capacity.section_name = General_pid_section_sold.section_name " +
                "WHERE IFNULL(General_pid_section_sold.sold, 0) < General_pid_section_capacity.capacity "
            );
            stmt.executeUpdate( // useful for cheapest available ticket
                "CREATE TEMPORARY TABLE Pid_price AS " +
                "SELECT Available.pid, MIN(Price_tiers.price) AS cheapest_available_ticket " +
                "FROM ((SELECT pid, section_name FROM Reserved_pid_section) UNION (SELECT pid, section_name FROM General_pid_section)) AS Available " +
                "                          NATURAL JOIN Section_pricetier " +
                "                          JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name "+
                    "                                           AND Section_pricetier.pid = Price_tiers.pid " +
                "GROUP BY Available.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_sold AS " +
                "SELECT Reserved_pid_section_sold.pid, SUM(Reserved_pid_section_sold.sold) AS reserved_sold " +
                "FROM Reserved_pid_section_sold " +
                "GROUP BY Reserved_pid_section_sold.pid"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_sold AS " +
                "SELECT General_pid_section_sold.pid, SUM(General_pid_section_sold.sold) AS general_sold " +
                "FROM General_pid_section_sold " +
                "GROUP BY General_pid_section_sold.pid"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_capacity AS " +
                "SELECT Reserved_pid_section_capacity.pid, SUM(Reserved_pid_section_capacity.capacity) AS reserved_capacity " +
                "FROM Reserved_pid_section_capacity " +
                "GROUP BY Reserved_pid_section_capacity.pid"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_capacity AS " +
                "SELECT General_pid_section_capacity.pid, SUM(General_pid_section_capacity.capacity) AS general_capacity " +
                "FROM General_pid_section_capacity " +
                "GROUP BY General_pid_section_capacity.pid"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Reserved_pid_blocked AS " +
                "SELECT pid, SUM(blocked) AS reserved_blocked " +
                "FROM Reserved_pid_section_blocked " +
                "GROUP BY pid"
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Num_available_tickets AS " +
                "SELECT Performances.pid, " +
                "       IFNULL(Reserved_pid_capacity.reserved_capacity, 0) - IFNULL(Reserved_pid_sold.reserved_sold, 0) - IFNULL(Reserved_pid_blocked.reserved_blocked, 0) AS reserved_availability, " +
                "       IFNULL(General_pid_capacity.general_capacity, 0) - IFNULL(General_pid_sold.general_sold, 0) AS general_availability, " +
                "       IFNULL(Reserved_pid_capacity.reserved_capacity, 0) - IFNULL(Reserved_pid_sold.reserved_sold, 0) - IFNULL(Reserved_pid_blocked.reserved_blocked, 0) + " +
                "       IFNULL(General_pid_capacity.general_capacity, 0) - IFNULL(General_pid_sold.general_sold, 0) AS availability " +
                "FROM Performances LEFT JOIN Reserved_pid_sold ON Performances.pid = Reserved_pid_sold.pid " +
                "                  LEFT JOIN Reserved_pid_capacity ON Performances.pid = Reserved_pid_capacity.pid " +
                "                  LEFT JOIN Reserved_pid_blocked ON Performances.pid = Reserved_pid_blocked.pid " +
                "                  LEFT JOIN General_pid_sold ON Performances.pid = General_pid_sold.pid " +
                "                  LEFT JOIN General_pid_capacity ON Performances.pid = General_pid_capacity.pid"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE Filter_table AS " +
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, " +
                "       Venues.city, Events.genre, Events.segment, Pid_price.cheapest_available_ticket, " +
                "       Num_available_tickets.reserved_availability, Num_available_tickets.general_availability, Num_available_tickets.availability " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                "                  JOIN Num_available_tickets ON Performances.pid = Num_available_tickets.pid " +
                "WHERE Performances.cancel_datetime IS NULL " +
                "  AND Performances.datetime >= CURRENT_TIMESTAMP"
            );

            System.out.print("Filters: city, segment, genre, datetime range, cheapest available ticket\n");
            System.out.print("         number of available tickets, reserved, general\n");
            System.out.print("Would you like to filter by city? y/n \n");
            String yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter city: ");
                String city = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE city <> ?")) {
                    ps.setString(1, city);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by segment? y/n \n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter segment: ");
                String seg = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE segment <> ?")) {
                    ps.setString(1, seg);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by genre? y/n \n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter genre: ");
                String genre = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE genre <> ?")) {
                    ps.setString(1, genre);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by datetime range? y/n \n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS ");
                String start = scanner.nextLine();
                System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS ");
                String end = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE datetime NOT BETWEEN ? AND ?")) {
                    ps.setString(1, start);
                    ps.setString(2, end);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by cheapest available ticket price range? y/n \n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter price bottom line: ");
                String start = scanner.nextLine();
                System.out.print("Enter price top line: ");
                String end = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE cheapest_available_ticket NOT BETWEEN ? AND ?")) {
                    ps.setString(1, start);
                    ps.setString(2, end);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by number of available tickets y/n \n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter at least how many available tickets: ");
                String least = scanner.nextLine();
                System.out.print("Do you want reserved seating, general admission, or any? (r/g/a): ");
                String seatType = scanner.nextLine();
                if (seatType.equals("r")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE reserved_availability < ?")) {
                        ps.setString(1, least);
                        ps.executeUpdate();
                    }
                }
                else if (seatType.equals("g")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE general_availability < ?")) {
                        ps.setString(1, least);
                        ps.executeUpdate();
                    }
                }
                else if (seatType.equals("a")) {
                    try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE availability < ?")) {
                        ps.setString(1, least);
                        ps.executeUpdate();
                    }
                }
                else {
                    System.out.print("Invalid choice.\n");
                    return;
                }
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT * " +
                    "FROM Filter_table")) {
                System.out.printf("%-5s %-30s %-20s %-25s %-10s %-20s %-20s %-20s %-10s %-10s %-10s%n",
                        "PID", "Event", "Datetime", "Venue", "City", "Genre", "Segment", "Cheapest available price", "Reserved", "General", "Total");
                System.out.println("-".repeat(190));
                while (rs.next()) {
                    System.out.printf("%-5d %-30s %-20s %-25s %-10s %-20s %-20s %-20.2f %-10d %-10d %-10d%n",
                            rs.getInt("pid"),
                            rs.getString("event_name"),
                            rs.getString("datetime"),
                            rs.getString("venue_name"),
                            rs.getString("city"),
                            rs.getString("genre"),
                            rs.getString("segment"),
                            rs.getDouble("cheapest_available_ticket"),
                            rs.getInt("reserved_availability"),
                            rs.getInt("general_availability"),
                            rs.getInt("availability"));
                }
            }
        }
    }

    private static void q6_seat_map_summary(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter performance PID:\n");
        System.out.print("You should get PID from other queries, e.g. using filters.\n");
        int pid = Integer.parseInt(scanner.nextLine());

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_blocked");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General");

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE Reserved_capacity AS " +
                    "SELECT Seats.section_name, COUNT(*) AS capacity " +
                    "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                    "WHERE Performances.pid = ? " +
                    "GROUP BY Seats.section_name")) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE Reserved_sold AS " +
                    "SELECT Reserved_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Performances JOIN Orders ON Performances.pid = Orders.pid " +
                    "                  JOIN Tickets ON Orders.oid = Tickets.oid " +
                    "                  JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                    "WHERE Performances.pid = ? AND Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Reserved_tickets.section_name")) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE Reserved_blocked AS " +
                    "SELECT Block.section_name, COUNT(*) AS blocked " +
                    "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                    "WHERE Performances.pid = ? " +
                    "GROUP BY Block.section_name")) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved AS " +
                    "SELECT Reserved_capacity.section_name, (Reserved_capacity.capacity - IFNULL(Reserved_sold.sold, 0) - IFNULL(Reserved_blocked.blocked, 0)) AS available, " +
                    "       IFNULL(Reserved_sold.sold, 0) AS sold, IFNULL(Reserved_blocked.blocked, 0) as blocked " +
                    "FROM Reserved_capacity LEFT JOIN Reserved_sold ON Reserved_capacity.section_name = Reserved_sold.section_name " +
                    "                       LEFT JOIN Reserved_blocked ON Reserved_capacity.section_name = Reserved_blocked.section_name");

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE General_capacity AS " +
                    "SELECT General_sections.name AS section_name, General_sections.total_capacity AS capacity " +
                    "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name " +
                    "WHERE Performances.pid = ?")) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TEMPORARY TABLE General_sold AS " +
                    "SELECT General_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Performances JOIN Orders ON Performances.pid = Orders.pid " +
                    "                  JOIN Tickets ON Orders.oid = Tickets.oid " +
                    "                  JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                    "WHERE Performances.pid = ? AND Tickets.cancel_datetime IS NULL " +
                    "GROUP BY General_tickets.section_name")) {
                ps.setInt(1, pid);
                ps.executeUpdate();
            }

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General AS " +
                    "SELECT General_capacity.section_name, " +
                    "       (General_capacity.capacity - IFNULL(General_sold.sold, 0)) AS available, " +
                    "       IFNULL(General_sold.sold, 0) AS sold, 0 AS blocked " +
                    "FROM General_capacity LEFT JOIN General_sold ON General_capacity.section_name = General_sold.section_name");

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Combined.section_name, Combined.available, Combined.sold, Combined.blocked, Price_tiers.name, Price_tiers.price " +
                    "FROM ((SELECT * FROM Reserved) UNION ALL (SELECT * FROM General)) AS Combined " +
                    "     JOIN Section_pricetier ON Combined.section_name = Section_pricetier.section_name " +
                    "     JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                         AND Section_pricetier.pid = Price_tiers.pid " +
                    "WHERE Section_pricetier.pid = ? " +
                    "ORDER BY Price_tiers.name"
                )) {
                ps.setInt(1, pid);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-30s %-10s %-10s %-10s %-30s %-10s%n", "Section", "Available", "Sold", "Blocked", "Pricetier name", "Price");
                    System.out.println("-".repeat(100));
                    while (rs.next()) {
                        System.out.printf("%-30s %-10s %-10s %-10s %-30s %-10s%n",
                                rs.getString("section_name"),
                                rs.getInt("available"),
                                rs.getInt("sold"),
                                rs.getInt("blocked"),
                                rs.getString("name"),
                                rs.getBigDecimal("price"));
                    }
                }
            }
        }
    }

    private static void r1_1_city_ticket_or_revenue_rank(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter the start datetime: YYYY-MM-DD HH:MM:SS\n");
        String start = scanner.nextLine();
        System.out.print("Enter the end datetime: YYYY-MM-DD HH:MM:SS\n");
        String end = scanner.nextLine();

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Unordered_list");
        }
        try (PreparedStatement ps = conn.prepareStatement(
            "CREATE TEMPORARY TABLE Unordered_list AS " +
            "SELECT Venues.city, Venues.country, COUNT(*) tickets_sold, SUM(Tickets.face_value) AS gross_revenue " +
            "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
            "             JOIN Performances ON Orders.pid = Performances.pid " +
            "             JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE Tickets.cancel_datetime IS NULL " +
            "  AND Orders.datetime >= ? " +
            "  AND Orders.datetime <= ? " +
            "GROUP BY Venues.city, Venues.country"
        )) {
            ps.setString(1, start);
            ps.setString(2, end);
            ps.executeUpdate();
        }

        System.out.print("Would you like to rank by tickets sold or gross revenue? 1/2\n");
        String choice = scanner.nextLine();

                if (choice.equals("1")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT * " +
                     "FROM Unordered_list " +
                     "ORDER BY tickets_sold DESC"
                 )) {

                System.out.printf(
                    "%-25s %-25s %-15s %-15s%n",
                    "City", "Country", "Tickets Sold", "Gross Revenue"
                );
                System.out.println("-".repeat(85));

                while (rs.next()) {
                    System.out.printf(
                        "%-25s %-25s %-15d %-15s%n",
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getInt("tickets_sold"),
                        rs.getBigDecimal("gross_revenue")
                    );
                }
            }
        }
        else if (choice.equals("2")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT * " +
                     "FROM Unordered_list " +
                     "ORDER BY gross_revenue DESC"
                 )) {

                System.out.printf(
                    "%-25s %-25s %-15s %-15s%n",
                    "City", "Country", "Tickets Sold", "Gross Revenue"
                );
                System.out.println("-".repeat(85));

                while (rs.next()) {
                    System.out.printf(
                        "%-25s %-25s %-15d %-15s%n",
                        rs.getString("city"),
                        rs.getString("country"),
                        rs.getInt("tickets_sold"),
                        rs.getBigDecimal("gross_revenue")
                    );
                }
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void r1_2_venue_ticket_or_revenue_rank(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter the start datetime: YYYY-MM-DD HH:MM:SS\n");
        String start = scanner.nextLine();
        System.out.print("Enter the end datetime: YYYY-MM-DD HH:MM:SS\n");
        String end = scanner.nextLine();
        System.out.print("Enter the city:\n");
        String city = scanner.nextLine();
        System.out.print("Enter country of the city:\n");
        String country = scanner.nextLine();

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Unordered_list");
        }
        try (PreparedStatement ps = conn.prepareStatement(
            "CREATE TEMPORARY TABLE Unordered_list AS " +
            "SELECT Venues.name, COUNT(*) tickets_sold, SUM(Tickets.face_value) AS gross_revenue " +
            "FROM Tickets JOIN Orders ON Tickets.oid = Orders.oid " +
            "             JOIN Performances ON Orders.pid = Performances.pid " +
            "             JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE Tickets.cancel_datetime IS NULL " +
            "  AND Orders.datetime >= ? " +
            "  AND Orders.datetime <= ? " +
            "  AND Venues.country = ? " +
            "  AND Venues.city = ? " +
            "GROUP BY Venues.name"
        )) {
            ps.setString(1, start);
            ps.setString(2, end);
            ps.setString(3, country);
            ps.setString(4, city);
            ps.executeUpdate();
        }

        System.out.print("Would you like to rank by tickets sold or gross revenue? 1/2\n");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT * " +
                     "FROM Unordered_list " +
                     "ORDER BY tickets_sold DESC"
                 )) {

                System.out.printf(
                    "%-25s %-15s %-15s%n",
                    "Venue", "Tickets Sold", "Gross Revenue"
                );
                System.out.println("-".repeat(85));

                while (rs.next()) {
                    System.out.printf(
                        "%-25s %-15d %-15s%n",
                        rs.getString("name"),
                        rs.getInt("tickets_sold"),
                        rs.getBigDecimal("gross_revenue")
                    );
                }
            }
        }
        else if (choice.equals("2")) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT * " +
                     "FROM Unordered_list " +
                     "ORDER BY gross_revenue DESC"
                 )) {

                System.out.printf(
                    "%-25s %-15s %-15s%n",
                    "Venue", "Tickets Sold", "Gross Revenue"
                );
                System.out.println("-".repeat(85));

                while (rs.next()) {
                    System.out.printf(
                        "%-25s %-15d %-15s%n",
                        rs.getString("name"),
                        rs.getInt("tickets_sold"),
                        rs.getBigDecimal("gross_revenue")
                    );
                }
            }
        }
        else {
            System.out.print("Invalid choice.\n");
        }
    }

    private static void r2_1_genre_report(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.segment, Events.genre, COUNT(DISTINCT Events.eid) AS num_of_events, COUNT(DISTINCT Performances.pid) AS num_of_performances " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "WHERE Performances.cancel_datetime IS NULL " +
                "GROUP BY Events.segment, Events.genre"
            )) {

            System.out.printf(
                "%-25s %-25s %-20s %-20s%n",
                "Segment", "Genre", "Number of events", "Number of performances"
            );
            System.out.println("-".repeat(90));

            while (rs.next()) {
                System.out.printf(
                    "%-25s %-25s %-20s %-20s%n",
                    rs.getString("segment"),
                    rs.getString("genre"),
                    rs.getInt("num_of_events"),
                    rs.getInt("num_of_performances")
                );
            }
        }
    }

    private static void r2_2_country_report(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Venues.country, COUNT(DISTINCT Events.eid) AS num_of_events, COUNT(DISTINCT Performances.pid) AS num_of_performances " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Performances.cancel_datetime IS NULL " +
                "GROUP BY Venues.country"
            )) {

            System.out.printf(
                "%-25s %-20s %-20s%n",
                "Country", "Number of events", "Number of performances"
            );
            System.out.println("-".repeat(65));

            while (rs.next()) {
                System.out.printf(
                    "%-25s %-20s %-20s%n",
                    rs.getString("country"),
                    rs.getInt("num_of_events"),
                    rs.getInt("num_of_performances")
                );
            }
        }
    }

    private static void r2_3_country_city_report(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Venues.country, Venues.city, COUNT(DISTINCT Events.eid) AS num_of_events, COUNT(DISTINCT Performances.pid) AS num_of_performances " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Performances.cancel_datetime IS NULL " +
                "GROUP BY Venues.country, Venues.city"
            )) {

            System.out.printf(
                "%-25s %-25s %-20s %-20s%n",
                "Country", "City", "Number of events", "Number of performances"
            );
            System.out.println("-".repeat(85));

            while (rs.next()) {
                System.out.printf(
                    "%-25s %-25s %-20s %-20s%n",
                    rs.getString("country"),
                    rs.getString("city"),
                    rs.getInt("num_of_events"),
                    rs.getInt("num_of_performances")
                );
            }
        }
    }

    private static void r2_4_country_city_venue_report(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Venues.country, Venues.city, Venues.name AS venue, COUNT(DISTINCT Events.eid) AS num_of_events, COUNT(DISTINCT Performances.pid) AS num_of_performances " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Performances.cancel_datetime IS NULL " +
                "GROUP BY Venues.country, Venues.city, Venues.name"
            )) {

            System.out.printf(
                "%-25s %-25s %-25s %-20s %-20s%n",
                "Country", "City", "Venue", "Number of events", "Number of performances"
            );
            System.out.println("-".repeat(115));

            while (rs.next()) {
                System.out.printf(
                    "%-25s %-25s %-25s %-20s %-20s%n",
                    rs.getString("country"),
                    rs.getString("city"),
                    rs.getString("venue"),
                    rs.getInt("num_of_events"),
                    rs.getInt("num_of_performances")
                );
            }
        }
    }

    private static void r3_1_organizer_overall_revenue_rank(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.uid AS organizer, Users.name, SUM(Tickets.face_value) AS revenue " +
                "FROM Events JOIN Performances ON Events.eid = Performances.eid " +
                "            JOIN Orders ON Performances.pid = Orders.pid " +
                "            JOIN Tickets ON Orders.oid = Tickets.oid " +
                "            JOIN Users ON Events.uid = Users.uid " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Events.uid, Users.name " +
                "ORDER BY revenue DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-10s%n",
                "UID", "Organizer name", "Revenue"
            );
            System.out.println("-".repeat(50));

            while (rs.next()) {
                System.out.printf(
                    "%-10s %-30s %-10s%n",
                    rs.getInt("organizer"),
                    rs.getString("name"),
                    rs.getBigDecimal("revenue")
                );
            }
        }
    }

    private static void r3_2_organizer_country_revenue_rank(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.uid AS organizer, Venues.country, Users.name, SUM(Tickets.face_value) AS revenue " +
                "FROM Events JOIN Performances ON Events.eid = Performances.eid " +
                "            JOIN Orders ON Performances.pid = Orders.pid " +
                "            JOIN Tickets ON Orders.oid = Tickets.oid " +
                "            JOIN Users ON Events.uid = Users.uid " +
                "            JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Events.uid, Users.name, Venues.country " +
                "ORDER BY country, revenue DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-30s %-10s%n",
                "UID", "Organizer name", "Country", "Revenue"
            );
            System.out.println("-".repeat(80));

            while (rs.next()) {
                System.out.printf(
                    "%-10s %-30s %-30s %-10s%n",
                    rs.getInt("organizer"),
                    rs.getString("name"),
                    rs.getString("country"),
                    rs.getBigDecimal("revenue")
                );
            }
        }
    }

    private static void r3_3_organizer_country_city_revenue_rank(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.uid AS organizer, Venues.country, Venues.city, Users.name, SUM(Tickets.face_value) AS revenue " +
                "FROM Events JOIN Performances ON Events.eid = Performances.eid " +
                "            JOIN Orders ON Performances.pid = Orders.pid " +
                "            JOIN Tickets ON Orders.oid = Tickets.oid " +
                "            JOIN Users ON Events.uid = Users.uid " +
                "            JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Tickets.cancel_datetime IS NULL " +
                "GROUP BY Events.uid, Users.name, Venues.country, Venues.city " +
                "ORDER BY country, city, revenue DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-30s %-30s %-10s%n",
                "UID", "Organizer name", "Country", "City", "Revenue"
            );
            System.out.println("-".repeat(110));

            while (rs.next()) {
                System.out.printf(
                    "%-10s %-30s %-30s %-30s %-10s%n",
                    rs.getInt("organizer"),
                    rs.getString("name"),
                    rs.getString("country"),
                    rs.getString("city"),
                    rs.getBigDecimal("revenue")
                );
            }
        }
    }

    private static void r4_ticket_scalpers_eport(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "DROP TEMPORARY TABLE IF EXISTS User_purchase_detail"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE User_purchase_detail AS " +
                "SELECT Orders.uid, Tickets.tid, Venues.country, Venues.city " +
                "FROM Orders JOIN Tickets ON Orders.oid = Tickets.oid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Orders.datetime >= CURRENT_TIMESTAMP - INTERVAL 1 YEAR " +
                "  AND Orders.datetime <= CURRENT_TIMESTAMP " +

                "UNION ALL " +

                "SELECT Listings.buyer_id AS uid, Listings.tid, Venues.country, Venues.city " +
                "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
                "              JOIN Orders ON Tickets.oid = Orders.oid " +
                "              JOIN Performances ON Orders.pid = Performances.pid " +
                "              JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Listings.trans_datetime IS NOT NULL " +
                "  AND Listings.buyer_id IS NOT NULL " +
                "  AND Listings.trans_datetime >= CURRENT_TIMESTAMP - INTERVAL 1 YEAR " +
                "  AND Listings.trans_datetime <= CURRENT_TIMESTAMP"
            );

            stmt.executeUpdate(
                "DROP TEMPORARY TABLE IF EXISTS User_purchase"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE User_purchase AS " +
                "SELECT country, city, uid, COUNT(DISTINCT tid) AS purchased " +
                "FROM User_purchase_detail " +
                "GROUP BY country, city, uid"
            );

            stmt.executeUpdate(
                "DROP TEMPORARY TABLE IF EXISTS User_list"
            );

            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE User_list AS " +
                "SELECT User_purchase_detail.country, " +
                "       User_purchase_detail.city, " +
                "       User_purchase_detail.uid, " +
                "       COUNT(DISTINCT User_purchase_detail.tid) AS listed " +
                "FROM User_purchase_detail JOIN Listings ON Listings.tid = User_purchase_detail.tid " +
                "                                        AND Listings.seller_id = User_purchase_detail.uid " +
                "GROUP BY User_purchase_detail.country, " +
                "         User_purchase_detail.city, " +
                "         User_purchase_detail.uid"
            );
        }
        try (Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery(
                "SELECT * " +
                "FROM User_purchase NATURAL JOIN User_list " +
                "WHERE User_list.listed * 2 > User_purchase.purchased " +
                "  AND User_purchase.purchased >= 10"
            )) {

            System.out.printf(
                "%-30s %-30s %-10s %-10s %-10s%n",
                "Country", "City", "UID", "Purchased", "Listed"
            );
            System.out.println("-".repeat(90));

            while (rs.next()) {
                System.out.printf(
                    "%-30s %-30s %-10s %-10s %-10s%n",
                    rs.getString("country"),
                    rs.getString("city"),
                    rs.getInt("uid"),
                    rs.getInt("purchased"),
                    rs.getInt("listed")
                );
            }
        }
    }

    private static void r5_1_customer_order_rank(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter the start datetime: YYYY-MM-DD HH:MM:SS ");
        String start = scanner.nextLine();
        System.out.print("Enter the end datetime: YYYY-MM-DD HH:MM:SS ");
        String end = scanner.nextLine();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Users.uid, Users.name, COUNT(Orders.oid) AS num_of_orders " +
                "FROM Orders JOIN Users ON Orders.uid = Users.uid " +
                "WHERE Orders.datetime >= ? AND Orders.datetime <= ? " +
                "GROUP BY Users.uid " +
                "ORDER BY num_of_orders DESC "
            )) {
                ps.setString(1, start);
                ps.setString(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf(
                    "%-10s %-30s %-15s%n",
                    "UID", "Customer name", "Number of orders"
                );
                System.out.println("-".repeat(55));

                while (rs.next()) {
                    System.out.printf(
                        "%-10d %-30s %-15d%n",
                        rs.getInt("uid"),
                        rs.getString("name"),
                        rs.getInt("num_of_orders")
                    );
                }
            }
        }
    }

    private static void r5_2_customer_order_city_rank(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter the start datetime: YYYY-MM-DD HH:MM:SS ");
        String start = scanner.nextLine();
        System.out.print("Enter the end datetime: YYYY-MM-DD HH:MM:SS ");
        String end = scanner.nextLine();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Users.uid, Users.name, Venues.country, Venues.city, COUNT(Orders.oid) AS num_of_orders " +
                "FROM Orders JOIN Users ON Orders.uid = Users.uid " +
                "            JOIN Performances ON Orders.pid = Performances.pid " +
                "            JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Orders.datetime >= ? AND Orders.datetime <= ? " +
                "GROUP BY Venues.country, Venues.city, Users.uid, Users.name " +
                "HAVING num_of_orders >= 2 " +
                "ORDER BY Venues.country, Venues.city, num_of_orders DESC "
            )) {
                ps.setString(1, start);
                ps.setString(2, end);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf(
                    " %-20s %-20s %-10s %-30s %-15s%n",
                    "Country", "City", "UID", "Customer name", "Number of orders"
                );
                System.out.println("-".repeat(95));

                while (rs.next()) {
                    System.out.printf(
                        " %-20s %-20s %-10d %-30s %-15d%n",
                        rs.getString("country"),
                        rs.getString("city"),
                        rs.getInt("uid"),
                        rs.getString("name"),
                        rs.getInt("num_of_orders")
                    );
                }
            }
        }
    }

    private static void r6_1_customer_cancel_ticket_rank(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Users.uid, Users.name, COUNT(*) AS cancelled " +
                "FROM Users JOIN Orders ON Users.uid = Orders.uid " +
                "           JOIN Tickets ON Orders.oid = Tickets.oid " +
                "           JOIN Performances ON Orders.pid = Performances.pid " +
                "WHERE Tickets.cancel_datetime IS NOT NULL " +
                "  AND Tickets.cancel_datetime >= CURRENT_TIMESTAMP - INTERVAL 1 YEAR " +
                "  AND Tickets.cancel_datetime <= CURRENT_TIMESTAMP " +
                "  AND Performances.cancel_datetime IS NULL " +
                "GROUP BY Users.uid, Users.name " +
                "ORDER BY cancelled DESC"
            )) {

            System.out.printf(
                "%-10s %-20s %-30s%n",
                "UID", "Customer name", "Number of tickets cancelled"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-20s %-30d%n",
                    rs.getInt("uid"),
                    rs.getString("name"),
                    rs.getInt("cancelled")
                );
            }
        }
    }

    private static void r6_2_organizer_cancel_performance_rank(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Users.uid, Users.name, COUNT(*) AS cancelled " +
                "FROM Users JOIN Events ON Users.uid = Events.uid " +
                "           JOIN Performances ON Events.eid = Performances.eid " +
                "WHERE Performances.cancel_datetime IS NOT NULL " +
                "  AND Performances.cancel_datetime >= CURRENT_TIMESTAMP - INTERVAL 1 YEAR " +
                "  AND Performances.cancel_datetime <= CURRENT_TIMESTAMP " +
                "GROUP BY Users.uid, Users.name " +
                "ORDER BY cancelled DESC"
            )) {

            System.out.printf(
                "%-10s %-20s %-30s%n",
                "UID", "Organizer name", "Number of performances cancelled"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-20s %-30d%n",
                    rs.getInt("uid"),
                    rs.getString("name"),
                    rs.getInt("cancelled")
                );
            }
        }
    }

    private static void r7_performance_sell_through_rate(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved_blocked");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS Reserved");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_capacity");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General_sold");
            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS General");

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_capacity AS " +
                    "SELECT Performances.pid, Seats.section_name, COUNT(*) AS capacity " +
                    "FROM Performances JOIN Seats ON Performances.venue_name = Seats.venue_name " +
                    "GROUP BY Performances.pid, Seats.section_name"
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_sold AS " +
                    "SELECT Performances.pid, Reserved_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Performances JOIN Orders ON Performances.pid = Orders.pid " +
                    "                  JOIN Tickets ON Orders.oid = Tickets.oid " +
                    "                  JOIN Reserved_tickets ON Tickets.tid = Reserved_tickets.tid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Performances.pid, Reserved_tickets.section_name"
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved_blocked AS " +
                    "SELECT Performances.pid, Block.section_name, COUNT(*) AS blocked " +
                    "FROM Performances JOIN Block ON Performances.pid = Block.pid " +
                    "GROUP BY Performances.pid, Block.section_name"
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE Reserved AS " +
                    "SELECT Reserved_capacity.pid, Reserved_capacity.section_name, (Reserved_capacity.capacity - IFNULL(Reserved_blocked.blocked, 0)) AS sellable, IFNULL(Reserved_sold.sold, 0) AS sold " +
                    "FROM Reserved_capacity LEFT JOIN Reserved_sold ON Reserved_capacity.pid = Reserved_sold.pid " +
                    "                                               AND Reserved_capacity.section_name = Reserved_sold.section_name " +
                    "                       LEFT JOIN Reserved_blocked ON Reserved_capacity.pid = Reserved_blocked.pid " +
                    "                                               AND Reserved_capacity.section_name = Reserved_blocked.section_name"
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_capacity AS " +
                    "SELECT Performances.pid, General_sections.name AS section_name, General_sections.total_capacity AS capacity " +
                    "FROM Performances JOIN General_sections ON Performances.venue_name = General_sections.venue_name "
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_sold AS " +
                    "SELECT Performances.pid, General_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Performances JOIN Orders ON Performances.pid = Orders.pid " +
                    "                  JOIN Tickets ON Orders.oid = Tickets.oid " +
                    "                  JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                    "WHERE Tickets.cancel_datetime IS NULL " +
                    "GROUP BY Performances.pid, General_tickets.section_name"
            );

            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General AS " +
                    "SELECT General_capacity.pid, General_capacity.section_name, General_capacity.capacity AS sellable, IFNULL(General_sold.sold, 0) AS sold " +
                    "FROM General_capacity LEFT JOIN General_sold ON General_capacity.pid = General_sold.pid " +
                    "                                             AND General_capacity.section_name = General_sold.section_name"
            );
            
            System.out.print("Do you want per performance or per price-tier report? 1/2\n");
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT Price_tiers.pid, SUM(Combined.sellable) AS sellable, SUM(Combined.sold) AS sold, SUM(Combined.sold) * 1.0 / NULLIF(SUM(Combined.sellable), 0) AS sell_through_rate " +
                    "FROM ((SELECT * FROM Reserved) UNION ALL (SELECT * FROM General)) AS Combined " +
                    "     JOIN Section_pricetier ON Combined.pid = Section_pricetier.pid " +
                    "                            AND Combined.section_name = Section_pricetier.section_name " +
                    "     JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                         AND Section_pricetier.pid = Price_tiers.pid " +
                    "GROUP BY Price_tiers.pid"
                )) {
                    System.out.printf("%-10s %-10s %-10s %-20s%n", "PID", "Sellable", "Sold", "Sell-through rate");
                    System.out.println("-".repeat(50));
                    while (rs.next()) {
                        System.out.printf("%-10d %-10d %-10d %-20.4f%n",
                                rs.getInt("pid"),
                                rs.getInt("sellable"),
                                rs.getInt("sold"),
                                rs.getDouble("sell_through_rate"));
                    }
                }
            }
            else if (choice.equals("2")) {
                try (ResultSet rs = stmt.executeQuery(
                    "SELECT Price_tiers.pid, Price_tiers.name, SUM(Combined.sellable) AS sellable, SUM(Combined.sold) AS sold, SUM(Combined.sold) * 1.0 / NULLIF(SUM(Combined.sellable), 0) AS sell_through_rate " +
                    "FROM ((SELECT * FROM Reserved) UNION ALL (SELECT * FROM General)) AS Combined " +
                    "     JOIN Section_pricetier ON Combined.pid = Section_pricetier.pid " +
                    "                            AND Combined.section_name = Section_pricetier.section_name " +
                    "     JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                         AND Section_pricetier.pid = Price_tiers.pid " +
                    "GROUP BY Price_tiers.pid, Price_tiers.name"
                )) {
                    System.out.printf("%-10s %-30s %-10s %-10s %-20s%n", "PID", "Price_tier name", "Sellable", "Sold", "Sell-through rate");
                    System.out.println("-".repeat(80));
                    while (rs.next()) {
                        System.out.printf("%-10d %-30s %-10d %-10d %-20.4f%n",
                                rs.getInt("pid"),
                                rs.getString("name"),
                                rs.getInt("sellable"),
                                rs.getInt("sold"),
                                rs.getDouble("sell_through_rate"));
                    }
                }
            }

            System.out.print("Enter month: YYYY-MM\n");
            String month = scanner.nextLine();
            java.time.YearMonth yearMonth;
            try {
            yearMonth = java.time.YearMonth.parse(month);
            }
            catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid month format.");
                return;
            }

            Timestamp start = Timestamp.valueOf(yearMonth.atDay(1).atStartOfDay());
            Timestamp end = Timestamp.valueOf(yearMonth.plusMonths(1).atDay(1).atStartOfDay());

            System.out.print("Here are the performances that sold out or sold below 1/4:\n");
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Venues.country, Venues.city, Combined.pid, " +
                "       SUM(Combined.sellable) AS sellable, " +
                "       SUM(Combined.sold) AS sold, " +
                "       SUM(Combined.sold) * 1.0 / " +
                "           NULLIF(SUM(Combined.sellable), 0) AS sell_through_rate, " +
                "       CASE " +
                "           WHEN SUM(Combined.sold) = SUM(Combined.sellable) " +
                "               THEN 'Sold out' " +
                "           WHEN SUM(Combined.sold) * 4 < SUM(Combined.sellable) " +
                "               THEN 'Below one quarter' " +
                "       END AS status " +
                "FROM ((SELECT * FROM Reserved) " +
                "      UNION ALL " +
                "      (SELECT * FROM General)) AS Combined " +
                "JOIN Performances ON Combined.pid = Performances.pid " +
                "JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE Performances.datetime >= ? " +
                "  AND Performances.datetime < ? " +
                "  AND Performances.cancel_datetime IS NULL " +
                "GROUP BY Venues.country, Venues.city, Combined.pid " +
                "HAVING SUM(Combined.sellable) > 0 " +
                "   AND (SUM(Combined.sold) = SUM(Combined.sellable) " +
                "        OR SUM(Combined.sold) * 4 < SUM(Combined.sellable)) " +
                "ORDER BY Venues.country, Venues.city, Combined.pid"
            )) {
                ps.setTimestamp(1, start);
                ps.setTimestamp(2, end);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf(
                        "%-20s %-20s %-10s %-10s %-10s %-20s %-20s%n",
                        "Country", "City", "PID", "Sellable", "Sold",
                        "Sell-through rate", "Status"
                    );
                    System.out.println("-".repeat(120));

                    while (rs.next()) {
                        System.out.printf(
                            "%-20s %-20s %-10d %-10d %-10d %-20.4f %-20s%n",
                            rs.getString("country"),
                            rs.getString("city"),
                            rs.getInt("pid"),
                            rs.getInt("sellable"),
                            rs.getInt("sold"),
                            rs.getDouble("sell_through_rate"),
                            rs.getString("status")
                        );
                    }
                }
            }
        }
    }

    private static void r8_1_resales_report_completed_resale(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.eid, Events.name, COUNT(Listings.lid) AS num " +
                "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
                "              JOIN Orders ON Tickets.oid = Orders.oid " +
                "              JOIN Performances ON Orders.pid = Performances.pid " +
                "              JOIN Events ON Performances.eid = Events.eid " +
                "WHERE Listings.buyer_id IS NOT NULL " +
                "GROUP BY Events.eid, Events.name " +
                "ORDER BY num DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-20s%n",
                "EID", "Event name", "Number of completed resales"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-30s %-20d%n",
                    rs.getInt("eid"),
                    rs.getString("name"),
                    rs.getInt("num")
                );
            }
        }
    }

    private static void r8_2_resales_report_average_markup(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.eid, Events.name, AVG(Listings.price - Tickets.face_value) AS markup " +
                "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
                "              JOIN Orders ON Tickets.oid = Orders.oid " +
                "              JOIN Performances ON Orders.pid = Performances.pid " +
                "              JOIN Events ON Performances.eid = Events.eid " +
                "WHERE Listings.buyer_id IS NOT NULL " +
                "GROUP BY Events.eid, Events.name " +
                "ORDER BY markup DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-20s%n",
                "EID", "Event name", "Average markup"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-30s %-20s%n",
                    rs.getInt("eid"),
                    rs.getString("name"),
                    rs.getBigDecimal("markup")
                );
            }
        }
    }

    private static void r8_3_resales_report_fraction(Connection conn, Scanner scanner) throws SQLException {
        try (Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT Events.eid, Events.name, " +
                "       1.0 * SUM(" +
                "           CASE " +
                "               WHEN Listings.price = " +
                "                    ROUND(Tickets.face_value * Events.resale_cap / 100, 2) " +
                "               THEN 1 " +
                "               ELSE 0 " +
                "           END" +
                "       ) / COUNT(Listings.lid) AS fraction_at_cap " +
                "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
                "              JOIN Orders ON Tickets.oid = Orders.oid " +
                "              JOIN Performances ON Orders.pid = Performances.pid " +
                "              JOIN Events ON Performances.eid = Events.eid " +
                "WHERE Listings.buyer_id IS NOT NULL " +
                "GROUP BY Events.eid, Events.name " +
                "ORDER BY fraction_at_cap DESC"
            )) {

            System.out.printf(
                "%-10s %-30s %-20s%n",
                "EID", "Event name", "Fraction at cap"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-30s %-20s%n",
                    rs.getInt("eid"),
                    rs.getString("name"),
                    rs.getBigDecimal("fraction_at_cap")
                );
            }
        }
    }

    private static void r8_4_resales_report_top_10(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter the start datetime: YYYY-MM-DD HH:MM:SS\n");
        String start_s = scanner.nextLine();
        System.out.print("Enter the end datetime: YYYY-MM-DD HH:MM:SS\n");
        String end_s = scanner.nextLine();

        Timestamp start;
        Timestamp end;

        try {
            start = Timestamp.valueOf(start_s);
            end = Timestamp.valueOf(end_s);
        }
        catch (IllegalArgumentException e) {
            System.out.println("Invalid datetime format.");
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT Events.eid, Events.name, COUNT(Listings.lid) AS num " +
                "FROM Listings JOIN Tickets ON Listings.tid = Tickets.tid " +
                "              JOIN Orders ON Tickets.oid = Orders.oid " +
                "              JOIN Performances ON Orders.pid = Performances.pid " +
                "              JOIN Events ON Performances.eid = Events.eid " +
                "WHERE Listings.buyer_id IS NOT NULL " +
                "  AND Listings.trans_datetime >= ? AND Listings.trans_datetime <= ? " +
                "GROUP BY Events.eid, Events.name " +
                "ORDER BY num DESC " +
                "LIMIT 10"
            )) {
                ps.setTimestamp(1, start);
                ps.setTimestamp(2, end);

        try (ResultSet rs = ps.executeQuery()) {
            System.out.printf(
                "%-10s %-30s %-20s%n",
                "EID", "Event name", "Resale volume"
            );
            System.out.println("-".repeat(60));

            while (rs.next()) {
                System.out.printf(
                    "%-10d %-30s %-20d%n",
                    rs.getInt("eid"),
                    rs.getString("name"),
                    rs.getInt("num")
                );
            }
        }
        }
    }
}