package com.example.machinelogapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DataController {

    private final CSVParser parser = new CSVParser();
    private final SQLFetcher fetcher = new SQLFetcher();

    @Autowired
    private DataService dataService;

    @GetMapping("/overview")
    public CompletableFuture<Map<String, Object>> getOverview(@RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        return dataService.getOverviewData(date, shift);
    }

    @GetMapping("/machineCard")
    public CompletableFuture<Map<String, Object>> getMachineCard (@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        return dataService.getMachineCardData(machineNumber, date, shift);
        // http://localhost:8080/api/machine?machineNumber=3
    }

    @GetMapping("/machineNumbers")
    public CompletableFuture<int[]> getMachineNumbers() {
        return dataService.getMachineNumbers();
    }

    @GetMapping("/faultLog")
    public CompletableFuture<Map<String, Object>> getFaultLog(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        return dataService.getFaultLog(machineNumber, date, shift);
    }

    @GetMapping("/faultReport")
    public CompletableFuture<Map<String, Object>> getFaultReport(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        return dataService.getFaultReport(machineNumber, date, shift);
    }
}