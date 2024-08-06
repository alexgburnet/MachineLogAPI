package com.example.machinelogapi;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;


import org.springframework.stereotype.Component;


@Component
public class SQLFetcher {
    String dbURL = "jdbc:postgresql://10.0.0.85:5432/fault_log";

    String username;
    String password;

    Connection con;

    SQLFetcher() {

        try {
            // Load the properties file

            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            username = props.getProperty("psql.username");
            password = props.getProperty("psql.password");

            Class.forName("org.postgresql.Driver");


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getOverviewData(String date, String shift) {

        Timestamp start;
        Timestamp end;
        double shiftHours;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 00:06:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 00:06:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
        }

        Map<String, Object> response = new HashMap<>();
        Map<Integer, Double> machinePercentRun = new HashMap<>();
        int totalMachines = 0; // To count total machines for average calculation

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT machine_number, SUM(fault_time) FROM faults WHERE date >= ?::timestamp AND date < ?::timestamp GROUP BY machine_number";

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, start);
                pstmt.setTimestamp(2, end);

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
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();

        Timestamp start;
        Timestamp end;
        double shiftHours;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 00:06:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 00:06:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
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
                    "GROUP BY\n" +
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

                }
            }

        } catch (SQLException e) {
            response.put("error", "Failed to connect to the database");
            e.printStackTrace();
        }
        return response;
    }

    public Map<String, Object> getFaultLog(String machineNumber, String date, String shift) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> faultLog = new ArrayList<>();

        Timestamp start;
        Timestamp end;
        double shiftHours;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 00:06:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 00:06:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
        }

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT date, fc.description AS Fault, o.name AS Operator, fault_time FROM faults f JOIN fault_codes fc ON f.fault_code = fc.code JOIN operators o ON f.operator_code = o.code WHERE machine_number = ? AND date >= ?::timestamp AND date < ?::timestamp ORDER BY date;";


            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, Integer.parseInt(machineNumber));
                pstmt.setTimestamp(2, start);
                pstmt.setTimestamp(3, end);

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

    public Map<String, Object> getFaultReport(String machineNumber, String date, String shift) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();
        Map<String, Double> faultTimePercentage = new HashMap<>();
        Map<String, Double> faultCount = new HashMap<>();
        Map<String, Double> faultCountPercentage = new HashMap<>();
        int totalFaults = 0;

        Timestamp start;
        Timestamp end;
        double shiftHours;

        if (shift.equals("day")) {
            start = Timestamp.valueOf(date + " 00:06:00");
            end = Timestamp.valueOf(date + " 17:30:00");
            shiftHours = 11.5;
        } else {
            start = Timestamp.valueOf(date + " 17:30:00");
            end = Timestamp.valueOf(LocalDateTime.parse(date + " 00:06:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            shiftHours = 12.5;
        }

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {
            String sql = "SELECT fc.description AS Fault, SUM(f.fault_time) AS Fault_Down_Time, COUNT(f.fault_code) AS Fault_Count FROM faults f JOIN fault_codes fc ON f.fault_code = fc.code WHERE machine_number = ? AND date >= ?::timestamp AND date < ?::timestamp GROUP BY fc.description;";

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

                        // Calculate total fault time in hours
                        double faultHours = hours + (double) minutes / 60 + (double) seconds / 3600;

                        faultDownTime.put(fault, faultDownTime.getOrDefault(fault, 0.0) + faultHours);
                        faultCount.put(fault, faultCount.getOrDefault(fault, 0.0) + 1);
                        totalFaults++;
                    }

                    double totalDownTime = faultDownTime.values().stream().mapToDouble(Double::doubleValue).sum();

                    for (Map.Entry<String, Double> entry : faultDownTime.entrySet()) {
                        String fault = entry.getKey();
                        double downTime = entry.getValue();
                        double downTimePercentage = downTime / totalDownTime * 100;
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

        return response;
    }

    public Map<Integer, String> getOperators() {
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

        Map<Integer, String> accountableKnitters = new HashMap<>();

        Timestamp dateTimestamp = Timestamp.valueOf(date + " 00:00:00");

        System.out.println("Date: " + dateTimestamp);
        System.out.println("Shift: " + shift);
        System.out.println("Machines: " + machines);


        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Integer machine : machines) {
            joiner.add("?");
        }

        String inClause = joiner.toString();

        try (Connection con = DriverManager.getConnection(dbURL, username, password)) {

            String sql = ("SELECT machine_number, name FROM accountable_knitter ak JOIN operators o ON o.code = ak.operator WHERE date = ?::timestamp AND shift = ? AND machine_number in " + inClause + ";");
            System.out.println(sql);

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setTimestamp(1, dateTimestamp);
                pstmt.setString(2, shift);

                int index = 3;

                for (int i = 0; i < machines.size(); i++) {
                    pstmt.setInt(i + index, machines.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        accountableKnitters.put(-1, "No accountable knitters found");
                        return accountableKnitters;
                    }

                    while (rs.next()) {
                        accountableKnitters.put(rs.getInt(1), rs.getString(2));
                    }
                }

            }


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return null;
    }

    public void SetAccountableKnitter(Integer operator, String date, String shift, List<Integer> machines) {

        Timestamp dateTimestamp = Timestamp.valueOf(date + " 00:00:00");

        System.out.println("Operator: " + operator);
        System.out.println("Date: " + dateTimestamp);
        System.out.println("Shift: " + shift);
        System.out.println("Machines: " + machines);

        return;
    }


    private double roundToOneDecimalPlace(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


}
