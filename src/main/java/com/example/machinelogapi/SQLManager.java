/**
 *  This class is what interacts with the PostgreSQL database.
 *   It contains methods to get data from the database and return it to the DataService class.
 *   The methods in this class are annotated with @Component to make it a Spring bean.
 *   Username and password are loaded from a properties file.
 */

package com.example.machinelogapi;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


import org.springframework.stereotype.Component;


@Component
public class SQLManager {
    String dbURL = "jdbc:postgresql://10.0.0.85:5432/fault_log";

    String username;
    String password;

    SQLManager() {

        try {
            // Load username and password from properties file
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            username = props.getProperty("psql.username");
            password = props.getProperty("psql.password");

            // Load PostgreSQL driver
            Class.forName("org.postgresql.Driver");


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public int[] getMachineNumbers() {
        return new int[] {1, 2, 3, 17, 19, 26, 27, 28};
    }

    public Map<String, Object> getOverviewData(String date, String shift) {
        /**
         * This method returns an overview of data for a given shift
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @return A map containing the following keys:
         *         - "machines": A map containing the machine numbers and their percent running time
         *         Example: { "machines": { "numbers": [1, 2, 3], "percentRun": [90.0, 80.0, 70.0] } }
         *         - "error": An error message if an error occurred
         *         Example: { "error": "Failed to connect to the database" }
         *
         */
        Timestamp start;
        Timestamp end;
        double shiftHours;

        // Day shift starts at 00:06:00 and ends at 17:30:00
        // Night shift starts at 17:30:00 and ends at 00:06:00 the next day
        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 06:00:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 06:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
        }

        // If we are currently in the shift, the shift hours should be calculated up to the current time
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (now.after(start) && now.before(end)) {
            shiftHours = (now.getTime() - start.getTime()) / 3600000.0;
        }

        Map<String, Object> response = new HashMap<>();
        Map<Integer, Double> machinePercentRun = new HashMap<>();
        int totalMachines = 0; // To count total machines for average calculation

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT machine_number, SUM(fault_time) FROM faults WHERE date >= ?::timestamp AND date < ?::timestamp AND visible = TRUE GROUP BY machine_number";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, start);
                pstmt.setTimestamp(2, end);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int machineNumber = rs.getInt(1);
                        String faultTimeString = rs.getString(2);

                        // Parse fault time string into hours, minutes, seconds
                        String[] parts = faultTimeString.split(":");
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        int seconds = Integer.parseInt(parts[2]);

                        // Calculate total fault time in seconds
                        long faultTimeSeconds = hours * 3600 + minutes * 60 + seconds;

                        // Calculate percent running time
                        double percentRunning = ((shiftHours * 3600 - faultTimeSeconds) / (shiftHours * 3600)) * 100.0;
                        machinePercentRun.put(machineNumber, percentRunning);

                        totalMachines++;
                    }
                }

                // Construct response map
                int[] machineNumbers = new int[totalMachines];
                double[] percentRun = new double[totalMachines];
                int index = 0;
                for (Map.Entry<Integer, Double> entry : machinePercentRun.entrySet()) {
                    machineNumbers[index] = entry.getKey();
                    percentRun[index] = entry.getValue();
                    index++;
                }

                Map<String, Object> machinesMap = new HashMap<>();
                machinesMap.put("numbers", machineNumbers);
                machinesMap.put("percentRun", percentRun);

                response.put("machines", machinesMap);
            } catch (SQLException e) {
                response.put("error", "Failed to execute query");
                e.printStackTrace();
            }
        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }

        return response;
    }

    public Map<String, Object> getMachineCardData(String machineNumber, String date, String shift) {
        /**
         * This method returns data for a machine card for a given shift
         *
         * @param machineNumber: The machine number
         *                     Example: "3"
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @return A map containing the following
         *        - "machineNumber": The machine number
         *        - "downTime": A map containing the fault codes and their downtime
         *        - "totalDownTime": The total downtime for the machine
         *        - "error": An error message if an error occurred
         */
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();

        Timestamp start;
        Timestamp end;
        double shiftHours;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 06:00:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 06:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
        }

        // If we are currently in the shift, the shift hours should be calculated up to the current time
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        if (now.after(start) && now.before(end)) {
            shiftHours = (now.getTime() - start.getTime()) / 3600000.0;
        }

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT\n" +
                    "    fc.code AS fault_code,\n" +
                    "    fc.description AS fault_description,\n" +
                    "    SUM(f.fault_time) AS total_fault_time\n" +
                    "FROM\n" +
                    "    faults f\n" +
                    "JOIN\n" +
                    "    fault_codes fc ON f.fault_code = fc.code\n" +
                    "JOIN\n" +
                    "    operators o ON f.operator_code = o.code\n" +
                    "WHERE\n" +
                    "    f.machine_number = ?\n" +
                    "    AND date >= ?::timestamp AND date < ?::timestamp\n" +
                    "AND visible = TRUE GROUP BY\n" +
                    "    fc.code, fc.description;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(2, start);
                pstmt.setTimestamp(3, end);
                pstmt.setInt(1, Integer.parseInt(machineNumber));

                try (ResultSet rs = pstmt.executeQuery()) {
                    double totalFaultTime = 0;
                    while (rs.next()) {

                        String fault = rs.getString(2);
                        String faultTimeString = rs.getString(3); // Assuming SUM(fault_time) returns hh:mm:ss
                        String parts[] = faultTimeString.split(":");
                        long hours = Integer.parseInt(parts[0]);
                        long minutes = Integer.parseInt(parts[1]);
                        long seconds = Integer.parseInt(parts[2]);

                        // Calculate total fault time in seconds
                        double faultTimeHours = hours + (double) minutes / 60 + (double) seconds / 3600;

                        faultDownTime.put(fault, faultTimeHours);
                        totalFaultTime += faultTimeHours;
                    }

                    response.put("machineNumber", machineNumber);
                    response.put("downTime", faultDownTime);
                    response.put("totalDownTime", totalFaultTime);
                    response.put("shiftHours", shiftHours);

                }
            }

        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }
        return response;
    }

    public Map<String, Object> getFaultLog(String machineNumber, String date, String shift) {
        /**
         * This method returns the fault log for a given machine number, date, and shift
         * Almost exactly how it is displayed in the database
         *
         * @param machineNumber: The machine number
         *                     Example: "3"
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @return A map containing the following
         *       - "machineNumber": The machine number
         *       - "faultLog": A list of maps containing the fault log data
         *       - "headers": An array of headers for the fault log
         *       - "error": An error message if an error occurred
         */
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> faultLog = new ArrayList<>();

        Timestamp start;
        Timestamp end;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 06:00:00");
            end = Timestamp.valueOf(date + " 17:30:00");
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 06:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT date, fc.description AS Fault, o.name AS Operator, fault_time FROM faults f JOIN fault_codes fc ON f.fault_code = fc.code JOIN operators o ON f.operator_code = o.code WHERE machine_number = ? AND date >= ?::timestamp AND date < ?::timestamp AND visible = TRUE ORDER BY date;";


            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(machineNumber));
                pstmt.setTimestamp(2, start);
                pstmt.setTimestamp(3, end);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {

                        LocalDateTime localDateTime = rs.getTimestamp(1).toLocalDateTime();
                        // Define the formatter
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        // Format the LocalDateTime to string
                        String formattedDateTime = localDateTime.format(formatter);

                        Map<String, Object> faultData = new HashMap<>();
                        faultData.put("Date", formattedDateTime);
                        faultData.put("Fault", rs.getString(2));
                        faultData.put("Fault Time", rs.getString(4));
                        faultData.put("Operator", rs.getString(3));

                        faultLog.add(faultData);
                    }

                    String[] headers = {"Date", "Fault", "Fault Time", "Operator"};

                    response.put("machineNumber", machineNumber);
                    response.put("faultLog", faultLog);
                    response.put("headers", headers);
                }
            }

        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }

        if (response.isEmpty()) {
            response.put("error", "No data found");
        }

        return response;
    }

    public Map<String, Object> getFaultReport(String machineNumber, String date, String shift) {
        /**
         * This method returns a fault report for a given machine number, date, and shift
         *
         * @param machineNumber: The machine number
         *                     Example: "3"
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @return A map containing the following
         *        - "machineNumber": The machine number
         *        - "totalDownTime": The total down time for the machine
         *        - "faultReport": A list of maps containing the fault report data
         *           Example: [ { "Fault": "Fault 1", "Number of Faults": 2, "percentage / count": 50.0, "Fault Down Time": 1.5, "percentage / time": 50.0 } ]
         *        - "error": An error message if an error occurred
         */
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();
        Map<String, Double> faultTimePercentage = new HashMap<>();
        Map<String, Integer> faultCount = new HashMap<>();
        Map<String, Double> faultCountPercentage = new HashMap<>();
        int totalFaults = 0;

        Timestamp start;
        Timestamp end;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 06:00:00");
            end = Timestamp.valueOf(date + " 17:30:00");
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 06:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT fc.description AS Fault, SUM(f.fault_time) AS Fault_Down_Time, COUNT(f.fault_code) AS Fault_Count FROM faults f JOIN fault_codes fc ON f.fault_code = fc.code WHERE machine_number = ? AND date >= ?::timestamp AND date < ?::timestamp AND visible = TRUE GROUP BY fc.description;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(machineNumber));
                pstmt.setTimestamp(2, start);
                pstmt.setTimestamp(3, end);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String fault = rs.getString(1);
                        String faultTimeString = rs.getString(2); // Assuming SUM(fault_time) returns hh:mm:ss
                        String parts[] = faultTimeString.split(":");
                        long hours = Integer.parseInt(parts[0]);
                        long minutes = Integer.parseInt(parts[1]);
                        long seconds = Integer.parseInt(parts[2]);

                        // Calculate total fault time in hours, rounded to 1.d.p
                        double faultHours = roundToOneDecimalPlace(hours + (double) minutes / 60 + (double) seconds / 3600);

                        faultDownTime.put(fault, faultDownTime.getOrDefault(fault, 0.0) + faultHours);
                        faultCount.put(fault, rs.getInt(3));
                        totalFaults += rs.getInt(3);
                    }

                    double totalDownTime = faultDownTime.values().stream().mapToDouble(Double::doubleValue).sum();

                    for (Map.Entry<String, Double> entry : faultDownTime.entrySet()) {
                        String fault = entry.getKey();
                        double downTime = entry.getValue();
                        double downTimePercentage = (totalDownTime == 0.0) ? 100 : downTime / totalDownTime * 100;
                        downTimePercentage = roundToOneDecimalPlace(downTimePercentage);
                        faultTimePercentage.put(fault, downTimePercentage);

                        double count = faultCount.get(fault);
                        double countPercentage = count / totalFaults * 100;
                        countPercentage = roundToOneDecimalPlace(countPercentage);
                        faultCountPercentage.put(fault, countPercentage);
                    }

                    List<Map<String, Object>> faultReport = new ArrayList<>();
                    for (String fault : faultDownTime.keySet()) {
                        Map<String, Object> faultRow = new LinkedHashMap<>();
                        faultRow.put("Fault", fault);
                        faultRow.put("Number of Faults", faultCount.get(fault));
                        faultRow.put("percentage / count", faultCountPercentage.get(fault));
                        faultRow.put("Fault Down Time", faultDownTime.get(fault));
                        faultRow.put("percentage / time", faultTimePercentage.get(fault));
                        faultReport.add(faultRow);
                    }

                    response.put("machineNumber", machineNumber);
                    response.put("totalDownTime", totalDownTime);
                    response.put("faultReport", faultReport);
                }
            }

        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }

        if (response.isEmpty()) {
            response.put("error", "No data found");
        }

        return response;
    }

    public void saveCorrectiveActions(String date, Integer machineNumber, Boolean isDayShift, List<Map<String, String>> faultsList) {
        /**
         * This method saves the corrective actions for a given machine number, date, and shift
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param machineNumber: The machine number
         *                     Example: 3
         *
         * @param isDayShift: A boolean indicating if it is the day shift
         *                  Example: true
         *
         * @param isLinearThread: A boolean indicating if the thread is linear
         *                      Example: false
         *
         * @param faultsList: A list of maps containing the fault data
         *                  Example: [ { "fault": "Standing", "observation": "asgfdas", "action": "asdgasdfg" }, { "fault": "False Stop", "observation": "asdgf", "action": "asdg" } ]
         */

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "INSERT INTO corrective_actions (date, machine_number, isdayshift, fault_code, observation, action) VALUES (?::timestamp, ?, ?, ?, ?, ?);";

            //delete any conflicting data
            String deleteSql = "DELETE FROM corrective_actions WHERE date = ?::timestamp AND machine_number = ? AND isdayshift = ? AND fault_code = ?;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                for (Map<String, String> fault : faultsList) {
                    String getFaultCode = "SELECT code FROM fault_codes WHERE description = ?;";
                    try (PreparedStatement pstmt2 = con.prepareStatement(getFaultCode)) {
                        pstmt2.setString(1, fault.get("fault"));
                        try (ResultSet rs = pstmt2.executeQuery()) {
                            if (!rs.next()) {
                                throw new RuntimeException("Fault code not found");
                            }
                            try (PreparedStatement pstmt3 = con.prepareStatement(deleteSql)) {
                                pstmt3.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                                pstmt3.setInt(2, machineNumber);
                                pstmt3.setBoolean(3, isDayShift);
                                pstmt3.setInt(4, rs.getInt(1));
                                pstmt3.executeUpdate();
                            }
                            pstmt.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                            pstmt.setInt(2, machineNumber);
                            pstmt.setBoolean(3, isDayShift);
                            pstmt.setInt(4, rs.getInt(1));
                            pstmt.setString(5, fault.get("observation"));
                            pstmt.setString(6, fault.get("action"));

                            pstmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Map<String, Object> getCorrectiveAction(String date, Integer machineNumber, Boolean isDayShift, String fault) {
        /**
         * This method is used to get the corrective actions for a given machine number, date, and shift
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @return
         * Example URL:
         */

        Map<String, Object> response = new HashMap<>();

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT observation, action FROM corrective_actions WHERE date = ?::timestamp AND machine_number = ? AND isdayshift = ? AND fault_code = ? AND completed = FALSE;";
            String getFaultCode = "SELECT code FROM fault_codes WHERE description = ?;";

            try (PreparedStatement pstmt = con.prepareStatement(getFaultCode)) {
                pstmt.setString(1, fault);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        response.put("error", "Fault code not found");
                        return response;
                    }
                    int faultCode = rs.getInt(1);
                    try (PreparedStatement pstmt2 = con.prepareStatement(sql)) {
                        pstmt2.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                        pstmt2.setInt(2, machineNumber);
                        pstmt2.setBoolean(3, isDayShift);
                        pstmt2.setInt(4, faultCode);

                        try (ResultSet rs2 = pstmt2.executeQuery()) {
                            if (!rs2.next()) {
                                response.put("error", "No data found");
                                return response;
                            }

                            response.put("observation", rs2.getString(1));
                            response.put("action", rs2.getString(2));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
        }

        return response;
    }

    public Boolean getLinearThread(String date, Integer machineNumber, Boolean isDayShift) {
        /**
         * This method is used to get the linear thread status for a given machine number, date, and shift
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @return
         * Example URL:
         */

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT islinearthread FROM linear_thread WHERE date = ?::timestamp AND machine_number = ? AND isdayshift = ?;";
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setInt(2, machineNumber);
                pstmt.setBoolean(3, isDayShift);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }

                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLinearThread(String date, Integer machineNumber, Boolean isDayShift, Boolean isLinearThread) {
        /**
         * This method is used to set the linear thread status for a given machine number, date, and shift
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @param isLinearThread
         * Example URL:
         */

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {

            String deleteSql = "DELETE FROM linear_thread WHERE date = ?::timestamp AND machine_number = ? AND isdayshift = ?;";
            try (PreparedStatement pstmt = con.prepareStatement(deleteSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setInt(2, machineNumber);
                pstmt.setBoolean(3, isDayShift);
                pstmt.executeUpdate();
            }

            String sql = "INSERT INTO linear_thread (date, machine_number, isdayshift, islinearthread) VALUES (?::timestamp, ?, ?, ?);";
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setInt(2, machineNumber);
                pstmt.setBoolean(3, isDayShift);
                pstmt.setBoolean(4, isLinearThread);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public Map<String, Object> getActionList() {
        Map<String, Object> response = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String sql = "SELECT * FROM corrective_actions WHERE completed = FALSE ORDER BY date ASC;";
        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    List<Map<String, Object>> actionList = new ArrayList<>();
                    while (rs.next()) {
                        LocalDateTime localDateTime = rs.getTimestamp(2).toLocalDateTime();
                        String formattedDateTime = localDateTime.format(formatter);
                        Map<String, Object> action = new HashMap<>();
                        action.put("id", rs.getInt(1));
                        action.put("date", formattedDateTime);
                        action.put("machine_number", rs.getInt(3));
                        action.put("isdayshift", rs.getBoolean(4));
                        action.put("fault_code", rs.getInt(5));
                        action.put("observation", rs.getString(6));
                        action.put("action", rs.getString(7));
                        actionList.add(action);
                    }

                    response.put("actionList", actionList);
                }
            }
        } catch (SQLException e) {
            response.put("error", e.toString());
        }

        return response;
    }

    public void completeAction(Integer id, String date) {
        String sql = "UPDATE corrective_actions SET completed = TRUE, date_completed = ? WHERE id = ?;";
        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(date));
                pstmt.setInt(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, String> getOperators() {
        /**
         * This method returns a list of operators
         */
        Map<Integer, String> operators = new HashMap<>();

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT code, name FROM operators;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        operators.put(rs.getInt(1), rs.getString(2));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return operators;
    }

    public Map<Integer, String> checkAccountableKnitter(String date, String shift, List<Integer> machines) {
        /**
         * This method checks if an accountable knitter is assigned to a machine for a given date and shift
         * If an accountable knitter is assigned, it returns the machine number and the knitter's name
         * If no accountable knitter is assigned, it returns -1 and "Unassigned"
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @param machines: A list of machine numbers
         *                Example: [1, 2, 3]
         */
        Map<Integer, String> accountableKnitters = new HashMap<>();

        Timestamp dateTimestamp = Timestamp.valueOf(date + " 00:00:00");


        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Integer machine : machines) {
            joiner.add("?");
        }

        String inClause = joiner.toString();

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {

            String sql = ("SELECT machine_number, name FROM accountable_knitter ak JOIN operators o ON o.code = ak.operator WHERE date = ?::timestamp AND shift = ? AND machine_number in " + inClause + ";");

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, dateTimestamp);
                pstmt.setString(2, shift);

                int index = 3;

                for (int i = 0; i < machines.size(); i++) {
                    pstmt.setInt(i + index, machines.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        accountableKnitters.put(-1, "Unassigned");
                        return accountableKnitters;
                    } else {
                        accountableKnitters.put(rs.getInt(1), rs.getString(2));
                    }

                    while (rs.next()) {
                        accountableKnitters.put(rs.getInt(1), rs.getString(2));
                    }
                }

            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return accountableKnitters;
    }

    public void SetAccountableKnitter(Integer operator, String date, String shift, List<Integer> machines) {
        /**
         * This method sets the accountable knitter for a given operator, date, shift, and list of machines
         * If there is already an accountable knitter assigned to a machine, it will be replaced
         *
         * @param operator: The operator code
         *                 Example: 1
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param shift: The shift in the format "day" or "night"
         *             Example: "day"
         *
         * @param machines: A list of machine numbers
         *                Example: [1, 2, 3]
         */
        Timestamp dateTimestamp = Timestamp.valueOf(date + " 00:00:00");


        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "DELETE FROM accountable_knitter WHERE date = ?::timestamp AND shift = ? AND machine_number = ?;";
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, dateTimestamp);
                pstmt.setString(2, shift);

                for (Integer machine : machines) {
                    pstmt.setInt(3, machine);
                    pstmt.executeUpdate();
                }
            }

            sql = "INSERT INTO accountable_knitter (date, shift, machine_number, operator) VALUES (?::timestamp, ?, ?, ?);";
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(4, operator);
                pstmt.setTimestamp(1, dateTimestamp);
                pstmt.setString(2, shift);

                for (Integer machine : machines) {
                    pstmt.setInt(3, machine);
                    pstmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private double roundToOneDecimalPlace(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public void inputKnittingFaultLog(String data) {
        /**
         * This method is used to input a knitting fault into the database
         * The data is in the format "dd/MM/yyyy hh:mm:ss;fault_code;fault_description;operator_code;operator_name;fault_time;machine_number"
         */

        String[] parts = data.split(";");
        String unformattedDate = parts[0];
        Integer faultCode = Integer.parseInt(parts[1]);
        Integer operatorNumber = Integer.parseInt(parts[3]);
        String unformattedTime = parts[5];
        Integer machineNumber = Integer.parseInt(parts[6]);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Parse the string to a LocalDateTime object
        LocalDateTime localDateTime = LocalDateTime.parse(unformattedDate, formatter);

        // Convert LocalDateTime to Timestamp
        Timestamp date = Timestamp.valueOf(localDateTime);

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "INSERT INTO faults (date, machine_number, fault_code, operator_code, fault_time) VALUES (?, ?, ?, ?, ?::interval);";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, date);
                pstmt.setInt(2, machineNumber);
                pstmt.setInt(3, faultCode);
                pstmt.setInt(4, operatorNumber);
                pstmt.setString(5, unformattedTime);

                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Knitting Fault Log:" + data);
    }

    public void removeFault(String date, Integer machineNumber) {
        /**
         * This method is used to remove a fault from the database
         *
         * @param date: The date in the format "yyyy-MM-dd"
         *            Example: "2021-08-25"
         *
         * @param machineNumber: The machine number
         *                     Example: 3
         */
        Timestamp dateTimestamp = Timestamp.valueOf(date);

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "UPDATE faults SET visible = FALSE WHERE date = ?::timestamp AND machine_number = ?;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, dateTimestamp);
                pstmt.setInt(2, machineNumber);

                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void inputWarpingFaultLog(String data) {
        // TODO
        System.out.println("Warping Fault Log: " + data);
    }

    public void inputKnittingProductionLog(String data) {
        // TODO
        System.out.println("Knitting Production Log: " + data);
    }

    public void inputWarpingProductionLog(String data) {
        // TODO
        System.out.println("Warping Production Log: " + data);
    }

    public void inputKnittingWarpRefLog(String data) {
        // TODO
        System.out.println("Knitting Warp Ref Log: " + data);
    }
}