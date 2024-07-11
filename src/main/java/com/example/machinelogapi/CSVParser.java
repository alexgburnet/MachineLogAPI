package com.example.machinelogapi;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.Duration;

public class CSVParser {

    String csvFile = "data/4.7.2024 All Machines Knitting MCs Fault Log.csv";
    String delimiter = ";";
    String line = "";

    public Map<String, Object> getOverviewData(String date) {
        Map<String, Object> response = new HashMap<>();

        Map<Integer, Duration> machineFaultTimes = new HashMap<>();

        csvFile = "data/" + date + " All Machines Knitting MCs Fault Log.csv";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_16))) {
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

        csvFile = "data/" + date + " All Machines Knitting MCs Fault Log.csv";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_16))) {
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

        csvFile = "data/" + date + " All Machines Knitting MCs Fault Log.csv";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_16))) {

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


        csvFile = "data/" + date + " All Machines Knitting MCs Fault Log.csv";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_16))) {
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
                faultCount.put(fault, faultCount.getOrDefault(fault, 0.0) + 1);
                totalFaults++;
            }

            double totalDownTime = 0.0;
            for (Map.Entry<String, Double> entry : faultDownTime.entrySet()) {
                double downTime = entry.getValue();
                totalDownTime += downTime;
            }

            for (Map.Entry<String, Double> entry : faultDownTime.entrySet()) {
                String fault = entry.getKey();
                double downTime = entry.getValue();
                double downTimePercentage = downTime / totalDownTime * 100;
                faultTimePercentage.put(fault, downTimePercentage);

                double count = faultCount.get(fault);
                double countPercentage = count / totalFaults * 100;
                faultCountPercentage.put(fault, countPercentage);
            }


            response.put("machineNumber", machineNumber);
            response.put("totalDownTime", totalDownTime);
            response.put("downTime", faultDownTime);
            response.put("downTimePercentage", faultTimePercentage);
            response.put("faultCount", faultCount);
            response.put("faultCountPercentage", faultCountPercentage);

        } catch (IOException e) {
            // Return a consistent structure with error message
            response.put("error", "Error reading CSV file: " + e.getMessage());
            response.put("downTime", new HashMap<>()); // Provide empty downTime structure
            response.put("totalDownTime", 0.0); // Provide default value
        }

        return response;
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

}
