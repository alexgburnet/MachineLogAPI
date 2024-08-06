/**
 *  This class is the controller for the API. It handles all the requests and responses.
 *   It uses the DataService class to get the data from the database and return it to the client.
 *   The DataService class is injected into this class using the @Autowired annotation.
 *   The methods in this class are annotated with @GetMapping or @PostMapping to specify the HTTP method.
 *   The @RequestParam annotation is used to get the query parameters from the URL.
 *   The @RequestBody annotation is used to get the request body from the client.
 *   The methods in this class return CompletableFuture objects to make them asynchronous.
 */

package com.example.machinelogapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DataController {


    @Autowired
    private DataService dataService;

    @GetMapping("/overview")
    public CompletableFuture<Map<String, Object>> getOverview(@RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        /**
         * This method is used to get the overview data for a given date and shift.
         * @param date
         * @param shift
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/overview?date=2021-07-01&shift=day
         */
        return dataService.getOverviewData(date, shift);
    }

    @GetMapping("/machineCard")
    public CompletableFuture<Map<String, Object>> getMachineCard (@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        /**
         * This method is used to get the machine card data for a given machine number, date, and shift.
         * @param machineNumber
         * @param date
         * @param shift
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/machineCard?machineNumber=3&date=2021-07-01&shift=day
         */
        return dataService.getMachineCardData(machineNumber, date, shift);
    }

    @GetMapping("/machineNumbers")
    public CompletableFuture<int[]> getMachineNumbers() {
        /**
         * This method is used to get the list of machine numbers.
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/machineNumbers
         */
        return dataService.getMachineNumbers();
    }

    @GetMapping("/faultLog")
    public CompletableFuture<Map<String, Object>> getFaultLog(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        /**
         * This method is used to get the fault log for a given machine number, date, and shift.
         * @param machineNumber
         * @param date
         * @param shift
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/faultLog?machineNumber=3&date=2021-07-01&shift=day
         */
        return dataService.getFaultLog(machineNumber, date, shift);
    }

    @GetMapping("/faultReport")
    public CompletableFuture<Map<String, Object>> getFaultReport(@RequestParam(required = true) String machineNumber, @RequestParam(required = true) String date, @RequestParam(required = true) String shift) {
        /**
         * This method is used to get the fault report for a given machine number, date, and shift.
         * @param machineNumber
         * @param date
         * @param shift
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/faultReport?machineNumber=3&date=2021-07-01&shift=day
         */
        return dataService.getFaultReport(machineNumber, date, shift);
    }

    @GetMapping("/operators")
    public CompletableFuture<Map<Integer, String>> getOperators() {
        /**
         * This method is used to get the list of operators.
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/operators
         */
        return dataService.getOperators();
    }

    @GetMapping("/checkAccountableKnitter")
    public CompletableFuture<Map<Integer, String>> checkAccountableKnitter(@RequestParam(required = true) String date, @RequestParam(required = true) String shift, @RequestParam(required = true) List<Integer> machines) {
        /**
         * This method checks of there is anyone accountable for a particular list of machines for a given shift.
         * @param date
         * @param shift
         * @param machines
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/checkAccountableKnitter?date=2021-07-01&shift=day&machines=3,4,5
         */
        return dataService.checkAccountableKnitter(date, shift, machines);
    }

    @PostMapping("/setAccountableKnitter")
    public CompletableFuture<Void> setAccountableKnitter(@RequestBody Map<String, Object> body) {
        /**
         * This method is used to set the accountable knitter for a given operator, date, shift, and list of machines.
         * @param body
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/setAccountableKnitter
         */
        Integer operator = Integer.valueOf(body.get("operator").toString());
        String date = body.get("date").toString();
        String shift = body.get("shift").toString();
        List<?> machinesList = (List<?>) body.get("machines");
        List<Integer> machines = machinesList.stream()
                .map(obj -> Integer.valueOf(obj.toString()))
                .collect(Collectors.toList());

        return dataService.setAccountableKnitter(operator, date, shift, machines);
    }
}