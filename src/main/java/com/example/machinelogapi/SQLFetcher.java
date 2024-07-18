package com.example.machinelogapi;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SQLFetcher {
    String dbURL = "jdbc:postgresql://10.0.0.85:5432/fault_log";

    String username;
    String password;

    Connection con;

    SQLFetcher() {

        try {
            // Load the properties file

            Properties props = new Properties();
            props.load(new FileInputStream("config/config.properties"));
            // username = props.getProperty("psql.username");
            //String password = props.getProperty("psql.password");

            username = "REDACTED";
            password = "REDACTED";

            Class.forName("org.postgresql.Driver");


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getOverviewData(String date) {
        Map<String, Object> response = new HashMap<>();
        Map<Integer, Double> machinePercentRun = new HashMap<>();
        int totalMachines = 0; // To count total machines for average calculation

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT machine_number, SUM(fault_time) FROM faults WHERE date >= ?::timestamp AND date < ?::timestamp GROUP BY machine_number";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setTimestamp(2, Timestamp.valueOf(date + " 23:59:59"));

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int machineNumber = rs.getInt(1);
                        String faultTimeString = rs.getString(2); // Assuming SUM(fault_time) returns hh:mm:ss

                        // Parse fault time string into hours, minutes, seconds
                        String[] parts = faultTimeString.split(":");
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        int seconds = Integer.parseInt(parts[2]);

                        // Calculate total fault time in seconds
                        long faultTimeSeconds = hours * 3600 + minutes * 60 + seconds;

                        // Calculate percent running time
                        double percentRunning = ((24.0 * 3600 - faultTimeSeconds) / (24.0 * 3600)) * 100.0;
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

    public Map<String, Object> getMachineCardData(String machineNumber, String date) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();

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
                    "GROUP BY\n" +
                    "    fc.code, fc.description;";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(2, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setTimestamp(3, Timestamp.valueOf(date + " 23:59:59"));
                pstmt.setInt(1, Integer.parseInt(machineNumber));

                try (ResultSet rs = pstmt.executeQuery()) {
                    double totalFaultTime = 0;
                    while (rs.next()) {

                        String fault = rs.getString(2);
                        System.out.println(fault);
                        String faultTimeString = rs.getString(3); // Assuming SUM(fault_time) returns hh:mm:ss
                        System.out.println(faultTimeString);
                        String parts[] = faultTimeString.split(":");
                        long hours = Integer.parseInt(parts[0]);
                        long minutes = Integer.parseInt(parts[1]);
                        long seconds = Integer.parseInt(parts[2]);

                        // Calculate total fault time in seconds
                        double faultTimeHours = hours + (double) minutes / 60 + (double) seconds / 3600;
                        System.out.println(faultTimeHours);

                        faultDownTime.put(fault, faultTimeHours);
                        totalFaultTime += faultTimeHours;
                    }

                    response.put("machineNumber", machineNumber);
                    response.put("downTime", faultDownTime);
                    response.put("totalDownTime", totalFaultTime);

                }
            }

        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }
        return response;
    }

    public Map<String, Object> getFaultLog(String machineNumber, String date) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> faultLog = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT date, fc.description AS Fault, o.name AS Operator, fault_time FROM faults f JOIN fault_codes fc ON f.fault_code = fc.code JOIN operators o ON f.operator_code = o.code WHERE machine_number = ? AND date >= ?::timestamp AND date < ?::timestamp ORDER BY date;";


            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(machineNumber));
                pstmt.setTimestamp(2, Timestamp.valueOf(date + " 00:00:00"));
                pstmt.setTimestamp(3, Timestamp.valueOf(date + " 23:59:59"));

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> faultData = new HashMap<>();
                        faultData.put("Date", rs.getTimestamp(1).toString());
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

        return response;
    }

}
