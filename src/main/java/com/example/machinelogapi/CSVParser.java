package com.example.machinelogapi;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.Duration;
import java.math.BigDecimal;
import java.math.RoundingMode;

import jcifs.CIFSContext;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

import jcifs.context.BaseContext;
import jcifs.context.SingletonContext;

import org.springframework.stereotype.Component;


@Component
public class CSVParser {

    String delimiter = ";";
    String line = "";

    String smbUrl = "smb://10.10.2.5/Long Eaton/STILLAGE REPORTS/";
    // Long Eaton/STILLAGE REPORTS/%runningtime10112023
    String smbUsername;
    String smbPassword;
    String psqlUsername;
    String psqlPassword;

    Properties props = new Properties();

    public CSVParser() {
        try {
            props.load(new FileInputStream("config.properties"));
            smbUsername = props.getProperty("smb.username");
            smbPassword = props.getProperty("smb.password");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public Map<String, Object> getOverviewData(String date) {
        Map<String, Object> response = new HashMap<>();
        Map<Integer, Duration> machineFaultTimes = new HashMap<>();

        String fulldate = formatDateDDMMYYYY(date);
        String csvFile = smbUrl + "%runningtime" + fulldate + "/" + date + " All Machines Knitting MCs Fault Log.csv";
        //String csvFile = "smb://10.10.2.5/Long Eaton/STILLAGE REPORTS/%running time12072024/test.2.test.csv";

        CIFSContext baseContext = SingletonContext.getInstance();
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator("", smbUsername, smbPassword);
        CIFSContext authContext = baseContext.withCredentials(auth);

        try {
            SmbFile smbFile = new SmbFile(csvFile, authContext);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new SmbFileInputStream(smbFile), StandardCharsets.UTF_16))) {
                br.readLine(); // Skip header lines
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(delimiter);

                    int machineNo = Integer.parseInt(columns[6].trim());
                    String faultTimeStr = columns[5].trim();
                    Duration faultDuration = parseFaultTime(faultTimeStr);

                    machineFaultTimes.put(machineNo, machineFaultTimes.getOrDefault(machineNo, Duration.ZERO).plus(faultDuration));
                }

                List<Integer> machineNumbers = new ArrayList<>();
                List<Double> percentRun = new ArrayList<>();

                for (Map.Entry<Integer, Duration> entry : machineFaultTimes.entrySet()) {
                    int machineNo = entry.getKey();
                    Duration faultTime = entry.getValue();
                    Duration totalTime = Duration.ofHours(24);
                    Duration runningTime = totalTime.minus(faultTime);
                    double runningTimePercentage = (double) runningTime.toMinutes() / totalTime.toMinutes() * 100;

                    machineNumbers.add(machineNo);
                    percentRun.add(runningTimePercentage);
                }

                Map<String, Object> machines = new HashMap<>();
                machines.put("numbers", machineNumbers);
                machines.put("percentRun", percentRun);

                // Add machines data to the response
                response.put("machines", machines);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Return a consistent structure with error message
            response.put("error", "Error reading CSV file: " + e.getMessage());
            response.put("machines", new HashMap<>()); // Provide empty machines structure
        }

        return response;
    }

    public Map<String, Object> getMachineCardData(String machineNumber, String date) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();

        String fulldate = formatDateDDMMYYYY(date);
        String csvFile = smbUrl + "%runningtime" + fulldate + "/" + date + " All Machines Knitting MCs Fault Log.csv";
        //String csvFile = "smb://10.10.2.5/Long Eaton/STILLAGE REPORTS/%running time12072024/test.2.test.csv";

        CIFSContext baseContext = SingletonContext.getInstance();
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator("", smbUsername, smbPassword);
        CIFSContext authContext = baseContext.withCredentials(auth);



        try {
            SmbFile smbFile = new SmbFile(csvFile, authContext);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new SmbFileInputStream(smbFile), StandardCharsets.UTF_16))) {
                br.readLine(); // Skip header lines
                br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(delimiter);

                    int machineNo = Integer.parseInt(columns[6].trim());
                    if (!String.valueOf(machineNo).equals(machineNumber)) {
                        continue; // Skip records that don't match the given machine number
                    }

                    String fault = columns[2].trim(); // Assuming fault is in column 3 (index 2)
                    String faultTimeStr = columns[5].trim();
                    Duration faultDuration = parseFaultTime(faultTimeStr);

                    double faultHours = faultDuration.toMinutes() / 60.0;
                    faultDownTime.put(fault, faultDownTime.getOrDefault(fault, 0.0) + faultHours);
                }

                double totalDownTime = 0.0;
                for (Map.Entry<String, Double> entry : faultDownTime.entrySet()) {
                    double downTime = entry.getValue();
                    totalDownTime += downTime;
                }

                response.put("machineNumber", machineNumber);
                response.put("downTime", faultDownTime);
                response.put("totalDownTime", totalDownTime);

            }
        } catch (IOException e) {
            // Return a consistent structure with error message
            response.put("error", "Error reading CSV file: " + e.getMessage());
            response.put("downTime", new HashMap<>()); // Provide empty downTime structure
            response.put("totalDownTime", 0.0); // Provide default value
        }

        return response;
    }

    public Map<String, Object> getFaultLog(String machineNo, String date) {
        Map<String, Object> response = new HashMap<>();

        String fulldate = formatDateDDMMYYYY(date);
        String csvFile = smbUrl + "%runningtime" + fulldate + "/" + date + " All Machines Knitting MCs Fault Log.csv";

        CIFSContext baseContext = SingletonContext.getInstance();
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator("", smbUsername, smbPassword);
        CIFSContext authContext = baseContext.withCredentials(auth);

        try {
            SmbFile smbFile = new SmbFile(csvFile, authContext);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new SmbFileInputStream(smbFile), StandardCharsets.UTF_16))) {
                br.readLine(); // Skip line denoting delimiter
                String[] header = br.readLine().split(delimiter);
                List<Map<String, String>> faultLog = new ArrayList<>();

                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(delimiter);
                    if (!columns[6].trim().equals(machineNo)) {
                        continue; // Skip records that don't match the given machine number
                    }
                    Map<String, String> fault = new HashMap<>();
                    for (int i = 0; i < columns.length; i++) {
                        fault.put(header[i], columns[i]);
                    }
                    faultLog.add(fault);
                }

                response.put("header", header);
                response.put("faultLog", faultLog);
            }
        } catch (IOException e) {
            // Return a consistent structure with error message
            response.put("error", "Error reading CSV file: " + e.getMessage());
            response.put("faultLog", new ArrayList<>()); // Provide empty faultLog structure
        }

        return response;
    }


    public Map<String, Object> getFaultReport(String machineNumber, String date) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Double> faultDownTime = new HashMap<>();
        Map<String, Double> faultTimePercentage = new HashMap<>();
        Map<String, Double> faultCount = new HashMap<>();
        Map<String, Double> faultCountPercentage = new HashMap<>();
        int totalFaults = 0;

        String fulldate = formatDateDDMMYYYY(date);
        String csvFile = smbUrl + "%runningtime" + fulldate + "/" + date + " All Machines Knitting MCs Fault Log.csv";

        CIFSContext baseContext = SingletonContext.getInstance();
        NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator("", smbUsername, smbPassword);
        CIFSContext authContext = baseContext.withCredentials(auth);

        try {
            SmbFile smbFile = new SmbFile(csvFile, authContext);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new SmbFileInputStream(smbFile), StandardCharsets.UTF_16))) {
                br.readLine(); // Skip header lines
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(delimiter);
                    int machineNo = Integer.parseInt(columns[6].trim());
                    if (!String.valueOf(machineNo).equals(machineNumber)) {
                        continue; // Skip records that don't match the given machine number
                    }

                    String fault = columns[2].trim(); // Assuming fault is in column 3 (index 2)
                    String faultTimeStr = columns[5].trim();
                    Duration faultDuration = parseFaultTime(faultTimeStr);

                    double faultHours = faultDuration.toMinutes() / 60.0;
                    faultHours = roundToOneDecimalPlace(faultHours);

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
        } catch (IOException e) {
            response.put("error", "Error reading CSV file: " + e.getMessage());
            response.put("downTime", new HashMap<>());
            response.put("totalDownTime", 0.0);
        }

        return response;
    }


    // Helper method to round a double to 1 decimal place
    private double roundToOneDecimalPlace(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public int[] getMachineNumbers() {
        return new int[] {1, 2, 3, 17, 19, 26, 27, 28};
    }

    private static Duration parseFaultTime(String faultTimeStr) {
        String[] timeParts = faultTimeStr.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);
        int seconds = Integer.parseInt(timeParts[2]);
        return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    private static String formatDateDDMMYYYY(String date) {
        String[] dateParts = date.split("\\.");
        for (int i = 0; i < dateParts.length; i++) {
            if (dateParts[i].length() == 1) {
                dateParts[i] = "0" + dateParts[i];
            }
        }
        return dateParts[0] + dateParts[1] + dateParts[2];
    }


}
