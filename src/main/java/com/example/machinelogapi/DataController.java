package com.example.machinelogapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/operators")
    public CompletableFuture<Map<Integer, String>> getOperators() {
        return dataService.getOperators();
    }

    @GetMapping("/checkAccountableKnitter")
    public CompletableFuture<Map<Integer, String>> checkAccountableKnitter(@RequestParam(required = true) String date, @RequestParam(required = true) String shift, @RequestParam(required = true) List<Integer> machines) {
        return dataService.checkAccountableKnitter(date, shift, machines);
    }

    @PostMapping("/setAccountableKnitter")
    public CompletableFuture<Void> setAccountableKnitter(@RequestBody Map<String, Object> body) {
        Integer operator = Integer.valueOf(body.get("operator").toString());
        String date = body.get("date").toString();
        String shift = body.get("shift").toString();
        List<Integer> machines = (List<Integer>) body.get("machines");

        System.out.println(operator + " " + date + " " + shift + " " + machines);

        return dataService.setAccountableKnitter(operator, date, shift, machines);
    }
}