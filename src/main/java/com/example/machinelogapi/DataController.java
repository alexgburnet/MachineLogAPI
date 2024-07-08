package com.example.machinelogapi;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DataController {

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        // Create the nested structure for the response
        Map<String, Object> response = new HashMap<>();

        response.put("data", "Overview");

        // Create the machines data
        Map<String, Object> machines = new HashMap<>();
        machines.put("numbers", List.of("1", "2", "3", "17", "19", "26", "27", "28"));
        machines.put("percentRun", List.of(73, 80, 64, 30, 45, 30, 60, 12));

        // Add machines data to the response
        response.put("machines", machines);

        return response;
    }
}
