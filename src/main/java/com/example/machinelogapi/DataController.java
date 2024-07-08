package com.example.machinelogapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DataController {

    @GetMapping("/overview")
    public Map<String, Object> getOverview(@RequestParam(value = "machines", defaultValue = "10") int machines,
                                           @RequestParam(value = "logs", defaultValue = "100") int logs) {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalMachines", machines);
        overview.put("totalLogs", logs);
        return overview;
    }
}
