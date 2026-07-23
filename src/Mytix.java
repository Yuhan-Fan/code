import java.sql.*;
import java.util.Scanner;
import java.math.BigDecimal;

/**
 * Compile:  javac -cp mysql-connector-java-8.0.29.jar mytix.java
 * Run:      java  -cp .:mysql-connector-java-8.0.29.jar mytix
 * Windows:  java  -cp .;mysql-connector-java-8.0.29.jar mytix
 */

class ConnectDatabase {
    static final String URL  = "jdbc:mysql://localhost:3306/mytix";
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
                System.out.print("1. Operations   2. Queries   3. Reports\n");
                System.out.print("Choose by entering a number.\n");
                String choice = scanner.nextLine();

                if (choice.equals("1"))
                    operations(conn, scanner);
                else if (choice.equals("2"))
                    queries(conn, scanner);
                else if (choice.equals("3"))
                    reports(conn, scanner);
                else
                    System.out.print("Invalid choice.\n");
            }
        }
    }

    private static boolean helper_genre_exists(Connection conn, String genre) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Genres WHERE genre = ?")) {
            stmt.setString(1, genre);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static boolean helper_organizer_exists(Connection conn, int uid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Organizers WHERE uid = ?")) {
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

    private static boolean helper_performance_exists(Connection conn, int pid, int eid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM Performances WHERE pid = ? AND eid = ?")) {
            stmt.setInt(1, pid);
            stmt.setInt(2, eid);

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

    private static void print_operations_options () {
        System.out.print("1. Create a profile.\n");
        System.out.print("2. Delete a profile.\n");
        System.out.print("3. Create an event.\n");
        System.out.print("4. Create a performance.\n");
        System.out.print("5. Define pricetiers and assign to sections.\n");
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
        else
            System.out.println("Invalid choice.\n");
    }

    private static void print_queries_options () {
        System.out.print("1. Search for upcoming performances by latitude & longitude.\n");
        System.out.print("2. Search for upcoming performances by postal code.\n");
        System.out.print("3. Search for upcoming performances by address.\n");
        System.out.print("4. Search for upcoming performances by date range & available ticket number.\n");
        System.out.print("5. Search by filters.\n");
        System.out.print("6. Seat summary of a performance.\n");
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
        else if (choice.equals("4"))
            q4_search_date_range_available_tickets(conn, scanner);
        else if (choice.equals("5"))
            q5_filters(conn, scanner);
        else if (choice.equals("6"))
            q6_seat_map_summary(conn, scanner);
        else
            System.out.println("Invalid choice.\n");

        return;
    }

    private static void print_reports_options () {
        System.out.print("");
    }

    private static void reports (Connection conn, Scanner scanner) {
        print_reports_options ();
        String choice = scanner.nextLine();
    }

    private static void o1_create_profile(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter your name:\n");
        String name = scanner.nextLine();
        System.out.print("Enter your address:\n");
        String address = scanner.nextLine();
        System.out.print("Enter your email:\n");
        String email = scanner.nextLine();
        System.out.print("Enter your date-of-birth: YYYY-MM-DD\n");
        String dob = scanner.nextLine();
        java.sql.Date date_of_birth = java.sql.Date.valueOf(dob);
        if (date_of_birth.after(java.sql.Date.valueOf(java.time.LocalDate.now().minusYears(18)))) {
            System.out.println("You have to be at least 18 years old to create a profile.\n");
            return;
        }

        System.out.print("Are you a customer or an organizer? c/o\n");
        String role = scanner.nextLine();
        if (!role.equals("c") && !role.equals("o")) {
            System.out.print("Invalid choice.\n");
            return;
        }

        conn.setAutoCommit(false);

        try {
            int uid;
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Users (name, address, email, date_of_birth) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, email);
            ps.setDate(4, date_of_birth);
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
            else {
                PreparedStatement organizerps = conn.prepareStatement(
                    "INSERT INTO Organizers (uid) VALUES (?)"
                );
                organizerps.setInt(1, uid);
                organizerps.executeUpdate();
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

        conn.setAutoCommit(false);

        try {
            PreparedStatement customerps = conn.prepareStatement(
                "DELETE FROM Customers WHERE uid = ?"
            );
            customerps.setInt(1, uid);
            customerps.executeUpdate();
            PreparedStatement organizerps = conn.prepareStatement(
                "DELETE FROM Organizers WHERE uid = ?"
            );
            organizerps.setInt(1, uid);
            organizerps.executeUpdate();
            PreparedStatement userps = conn.prepareStatement(
                "DELETE FROM Users WHERE uid = ?"
            );
            userps.setInt(1, uid);

            int success = userps.executeUpdate();
            if (success == 0) {
                conn.rollback();
                System.out.print("Invalid UID.\n");
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
        System.out.print("Enter event genre:\n");
        String genre = scanner.nextLine();
        if (!helper_genre_exists(conn, genre)) {
            System.out.print("Invalid genre.\n");
            return;
        }

        conn.setAutoCommit(false);

        try {
            int eid;
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Events (name, resale_cap, uid, genre) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, name);
            ps.setBigDecimal(2, resaleCap);
            ps.setInt(3, uid);
            ps.setString(4, genre);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();

            if (!keys.next())
                throw new SQLException("Creating event failed: generating EID failed");
            eid = keys.getInt(1);
            keys.close();

            conn.commit();
            System.out.printf("Your event is created. EID: %d\n", eid);
        }
        catch (SQLException e) {
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
                "INSERT INTO Performances (cancelled, datetime, eid, venue_name) VALUES (false, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, datetime);
            ps.setInt(2, eid);
            ps.setString(3, venue);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();

            if (!keys.next())
                throw new SQLException("Creating performance failed: generating PID failed");
            pid = keys.getInt(1);
            keys.close();

            conn.commit();
            System.out.printf("Your performance is created. PID: %d\n", pid);
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
                "WHERE eid = ?")) {
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

        if (!helper_performance_exists(conn, pid, eid)) {
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
                    "(section_name, venue_name, pid, pricetier_name) " +
                    "VALUES (?, ?, ?, ?)")) {

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
                        insertps.setString(2, venueName);
                        insertps.setInt(3, pid);
                        insertps.setString(4, pricetierName);
                        insertps.executeUpdate();
                    }
                }
            }

            conn.commit();
            System.out.print(
                    "Price tiers created and assigned successfully.\n");
        }
        catch (SQLException e | RuntimeException e) {
            conn.rollback();
            throw e;
        }
        finally {
            conn.setAutoCommit(true);
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

        System.out.print("Would you like the results be ordered by distance, or cheapest available ticket price? 1/2\n");
        String order = scanner.nextLine();

        if (order.equals("1")) {
            String sql =
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "WHERE POWER(Venues.latitude - ?, 2) + POWER(Venues.longitude - ?, 2) <= POWER(?, 2) " +
                "      AND Performances.datetime >= CURRENT_TIMESTAMP " +
                "ORDER BY POWER(Venues.latitude - ?, 2) + POWER(Venues.longitude - ?, 2) ";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, DEFAULT_RADIUS);
                ps.setDouble(4, latitude);
                ps.setDouble(5, longitude);

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
        else if (order.equals("2")) {
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
                    "WHERE Tickets.cancelled = false " +
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
                    "FROM Reserved_pid_section_capacity LEFT JOIN Reserved_pid_section_sold ON Reserved_pid_section_capacity.pid = Reserved_pid_section_sold.pid" +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_sold.section_name" +
                    "                                   LEFT JOIN Reserved_pid_section_blocked ON Reserved_pid_section_capacity.pid = Reserved_pid_section_blocked.pid" +
                    "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_blocked.section_name" +
                    "WHERE IFNULL(Reserved_pid_section_sold.sold, 0) < Reserved_pid_section_capacity.capacity - IFNULL(Reserved_pid_section_blocked.blocked, 0) "
                );
                stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE General_pid_section_sold AS " +
                    "SELECT Orders.pid, General_tickets.section_name, COUNT(*) AS sold " +
                    "FROM Tickets JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                    "             JOIN Orders ON Tickets.oid = Orders.oid " +
                    "WHERE Tickets.cancelled = false " +
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

            String sql =
                "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name, Pid_price.cheapest_available_ticket AS price " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                "WHERE POWER(Venues.latitude - ?, 2) + POWER(Venues.longitude - ?, 2) <= POWER(?, 2) " +
                "      AND Performances.datetime >= CURRENT_TIMESTAMP " +
                "ORDER BY Pid_price.cheapest_available_ticket ";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, latitude);
                ps.setDouble(2, longitude);
                ps.setDouble(3, DEFAULT_RADIUS);

                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-10s %-30s %-30s %-30s %-30s%n", "PID", "Event", "Datetime", "Venue", "Cheapest available ticket price");
                    System.out.println("-".repeat(130));
                    while (rs.next()) {
                        System.out.printf("%-10d %-30s %-30s %-30s %-30s%n",
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
            System.out.print("Invalid choice.\n");
        }
    }

    private static void q2_search_performances_by_postalcode(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter postal code:");
        String postal = scanner.nextLine();

        postal = postal.replace(" ", "").toUpperCase();
        if (!postal.matches("[A-Za-z0-9]{6}")) {
            System.out.println("Postal doesn't have right format.\n");
            return;
        }

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE LEFT(Venues.postal_code, 3) = LEFT(?, 3) " +
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
        } catch (SQLException e) {
            System.out.println("Query error (q2): " + e.getMessage());
        }
    }

    private static void q3_search_performances_by_address(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter address:");
        String address = scanner.nextLine();

        String sql =
            "SELECT Performances.pid, Events.name AS event_name, Performances.datetime, Venues.name AS venue_name " +
            "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
            "                  JOIN Venues ON Performances.venue_name = Venues.name " +
            "WHERE Venues.address = ? " +
            "      AND Performances.datetime >= CURRENT_TIMESTAMP ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, address);
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
        } catch (SQLException e) {
            System.out.println("Query error (q3): " + e.getMessage());
        }
    }

    private static void q4_search_date_range_available_tickets(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS");
        String start = scanner.nextLine();
        System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS");
        String end = scanner.nextLine();
        System.out.print("Enter availability:");
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
                "WHERE Tickets.cancelled = false " +
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
            "                 JOIN Venues ON Performances.venue_name = Venues.name " +
            "                 LEFT JOIN Reserved_capacity_table ON Performances.pid = Reserved_capacity_table.pid " +
            "                 LEFT JOIN General_capacity_table ON Performances.pid = General_capacity_table.pid " +
            "                 LEFT JOIN Sold_table ON Performances.pid = Sold_table.pid " +
            "                 LEFT JOIN Reserved_blocked_table ON Performances.pid = Reserved_blocked_table.pid " +
            "WHERE Performances.datetime BETWEEN ? AND ? " +
            "  AND IFNULL(Reserved_capacity_table.reserved_capacity, 0) + IFNULL(General_capacity_table.general_capacity, 0) - IFNULL(Sold_table.sold, 0) - IFNULL(Reserved_blocked_table.blocked, 0) >= ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, start);
            ps.setString(2, end);
            ps.setInt(3, available);
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
        } catch (SQLException e) {
            System.out.println("Query error (q4): " + e.getMessage());
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
                "WHERE Tickets.cancelled = false " +
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
                "FROM Reserved_pid_section_capacity LEFT JOIN Reserved_pid_section_sold ON Reserved_pid_section_capacity.pid = Reserved_pid_section_sold.pid" +
                "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_sold.section_name" +
                "                                   LEFT JOIN Reserved_pid_section_blocked ON Reserved_pid_section_capacity.pid = Reserved_pid_section_blocked.pid" +
                "                                                                       AND Reserved_pid_section_capacity.section_name = Reserved_pid_section_blocked.section_name" +
                "WHERE IFNULL(Reserved_pid_section_sold.sold, 0) < Reserved_pid_section_capacity.capacity - IFNULL(Reserved_pid_section_blocked.blocked, 0) "
            );
            stmt.executeUpdate(
                "CREATE TEMPORARY TABLE General_pid_section_sold AS " +
                "SELECT Orders.pid, General_tickets.section_name, COUNT(*) AS sold " +
                "FROM Tickets JOIN General_tickets ON Tickets.tid = General_tickets.tid " +
                "             JOIN Orders ON Tickets.oid = Orders.oid " +
                "WHERE Tickets.cancelled = false " +
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
                "       Venues.city, Events.genre, Genres.segment, Pid_price.cheapest_available_ticket, " +
                "       Num_available_tickets.reserved_availability, Num_available_tickets.general_availability, Num_available_tickets.availability " +
                "FROM Performances JOIN Events ON Performances.eid = Events.eid " +
                "                  JOIN Venues ON Performances.venue_name = Venues.name " +
                "                  JOIN Genres ON Events.genre = Genres.genre " +
                "                  JOIN Pid_price ON Performances.pid = Pid_price.pid " +
                "                  JOIN Num_available_tickets ON Performances.pid = Num_available_tickets.pid"
            );

            System.out.print("Filters: city, segment, genre, datetime range, cheapest available ticket\n");
            System.out.print("         number of available tickets, reserved, general\n");
            System.out.print("Would you like to filter by city? y/n\n");
            String yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter city:");
                String city = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE city <> ?")) {
                    ps.setString(1, city);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by segment? y/n\n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter segment:");
                String seg = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE segment <> ?")) {
                    ps.setString(1, seg);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by genre? y/n\n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter genre:");
                String genre = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE genre <> ?")) {
                    ps.setString(1, genre);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by datetime range? y/n\n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter start datetime: YYYY-MM-DD HH:MM:SS");
                String start = scanner.nextLine();
                System.out.print("Enter end datetime: YYYY-MM-DD HH:MM:SS");
                String end = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE datetime NOT BETWEEN ? AND ?")) {
                    ps.setString(1, start);
                    ps.setString(2, end);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by cheapest available ticket price range? y/n\n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter price bottom line:");
                String start = scanner.nextLine();
                System.out.print("Enter price top line:");
                String end = scanner.nextLine();
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Filter_table WHERE cheapest_available_ticket NOT BETWEEN ? AND ?")) {
                    ps.setString(1, start);
                    ps.setString(2, end);
                    ps.executeUpdate();
                }
            }

            System.out.print("Would you like to filter by number of available tickets y/n\n");
            yesno = scanner.nextLine();
            if (yesno.equals("y")) {
                System.out.print("Enter at least how many available tickets:");
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
                System.out.printf("%-6s %-25s %-20s %-20s %-15s %-10s %-12s %-24s %-10s %-10s %-10s%n",
                        "PID", "Event", "Datetime", "Venue", "City", "Genre", "Segment", "Cheapest available price", "Reserved", "General", "Total");
                System.out.println("-".repeat(160));
                while (rs.next()) {
                    System.out.printf("%-6d %-25s %-20s %-20s %-15s %-10s %-12s %-10.2f %-10d %-10d %-10d%n",
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
            stmt.executeUpdate("DROP TABLE IF EXISTS Reserved_capacity");
            stmt.executeUpdate("DROP TABLE IF EXISTS Reserved_sold");
            stmt.executeUpdate("DROP TABLE IF EXISTS Reserved_blocked");
            stmt.executeUpdate("DROP TABLE IF EXISTS Reserved");
            stmt.executeUpdate("DROP TABLE IF EXISTS General_capacity");
            stmt.executeUpdate("DROP TABLE IF EXISTS General_sold");
            stmt.executeUpdate("DROP TABLE IF EXISTS General");

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
                    "WHERE Performances.pid = ? AND Tickets.cancelled = false " +
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
                    "FROM Reserved_capacity LEFT JOIN Reserved_sold ON Reserved_capacity.section_name = Reserved_sold.section_name" +
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
                    "WHERE Performances.pid = ? AND Tickets.cancelled = false " +
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
                    "SELECT Both.section_name, Both.available, Both.sold, Both.blocked, Price_tiers.name, Price_tiers.price " +
                    "FROM ((SELECT * FROM Reserved) UNION ALL (SELECT * FROM General)) AS Both " +
                    "     JOIN Section_pricetier ON Both.section_name = Section_pricetier.section_name " +
                    "     JOIN Price_tiers ON Section_pricetier.pricetier_name = Price_tiers.name " +
                    "                         AND Section_pricetier.pid = Price_tiers.pid " +
                    "WHERE Section_pricetier.pid = ? " +
                    "ORDER BY Price_tiers.name"
                )) {
                ps.setInt(1, pid);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf("%-30s %-10s %-10s %-10s %-30s %-10s%n", "Section", "Available", "Sold", "Blocked", "Pricetier name", "Price");
                    System.out.println("-".repeat(32));
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
}