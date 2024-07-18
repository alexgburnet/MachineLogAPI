package com.example.machinelogapi;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DataController {

    CSVParser parser = new CSVParser();
    SQLFetcher fetcher = new SQLFetcher();

    @GetMapping("/overview")
    public Map<String, Object> getOverview(@RequestParam(required = true) String date) {
        return fetcher.getOverviewData(date);
    }

    @GetMapping("/machineCard")
    public Map<String, Object> getMachineCard (@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date) {
        return fetcher.getMachineCardData(machineNumber, date);
        // http://localhost:8080/api/machine?machineNumber=3
    }

    @GetMapping("/machineNumbers")
    public int[] getMachineNumbers() {
        return parser.getMachineNumbers();
    }

    @GetMapping("/faultLog")
    public Map<String, Object> getFaultLog(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date) {
        return parser.getFaultLog(machineNumber, date);
    }

    @GetMapping("/faultReport")
    public Map<String, Object> getFaultReport(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date) {
        return parser.getFaultReport(machineNumber, date);
    }
}