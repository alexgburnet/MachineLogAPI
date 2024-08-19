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

    @PostMapping("/saveFaultReport")
    public CompletableFuture<Void> saveCorrectiveActions(@RequestBody(required = true) Map<String, Object> body) {
        /**
         * This method is used to save the fault report for a given machine number, date, and shift.
         * @param body
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/saveFaultReport
         *
         * Faults in format:
         *
         * "faults": [
         *     {
         *       "fault": "Standing",
         *       "observation": "asgfdas",
         *       "action": "asdgasdfg"
         *     },
         *     {
         *       "fault": "False Stop",
         *       "observation": "asdgf",
         *       "action": "asdg"
         *     }
         *   ]
         *
         */

        String date = body.get("date").toString();
        Integer machineNumber = Integer.valueOf(body.get("machineNumber").toString());
        boolean isDayShift = Boolean.parseBoolean(body.get("isDayShift").toString());
        List<Map<String, String>> faultsList = (List<Map<String, String>>) body.get("faults");

        return dataService.saveCorrectiveActions(date, machineNumber, isDayShift, faultsList);
    }

    @GetMapping("/getCorrectiveAction")
    public CompletableFuture<Map<String, Object>> getCorrectiveAction(@RequestParam(required = true) String date, @RequestParam(required = true) Integer machineNumber, @RequestParam(required = true) Boolean isDayShift, @RequestParam(required = true) String fault) {
        /**
         * This method is used to get the corrective action for a given machine number, date, shift, and fault code.
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @param faultCode
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/getCorrectiveAction?date=2021-07-01&machineNumber=3&isDayShift=true&faultCode=1
         */
        return dataService.getCorrectiveAction(date, machineNumber, isDayShift, fault);
    }

    @GetMapping("/getLinearThread")
    public CompletableFuture<Boolean> getLinearThread(@RequestParam(required = true) String date, @RequestParam(required = true) Integer machineNumber, @RequestParam(required = true) Boolean isDayShift) {
        /**
         * This method is used to get the linear thread status for a given machine number, date, and shift.
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/getLinearThread?date=2021-07-01&machineNumber=3&isDayShift=true
         */
        return dataService.getLinearThread(date, machineNumber, isDayShift);
    }

    @PostMapping("/setLinearThread")
    public CompletableFuture<Void> setLinearThread(@RequestParam(required = true) String date, @RequestParam(required = true) Integer machineNumber, @RequestParam(required = true) Boolean isDayShift, @RequestParam(required = true) Boolean linearThread) {
        /**
         * This method is used to set the linear thread status for a given machine number, date, shift, and linear thread status.
         * @param date
         * @param machineNumber
         * @param isDayShift
         * @param linearThread
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/setLinearThread?date=2021-07-01&machineNumber=3&isDayShift=true&linearThread=true
         */
        return dataService.setLinearThread(date, machineNumber, isDayShift, linearThread);
    }

    @GetMapping("/actionList")
    public CompletableFuture<Map<String, Object>> getActionList() {
        /**
         * This method is used to get the list of actions.
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/actionList
         */
        return dataService.getActionList();
    }

    @PostMapping("completeAction")
    public CompletableFuture<Void> completeAction(@RequestBody(required = true) Map<String, Object> body) {
        /**
         * This method is used to complete an action.
         * @param body
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/completeAction
         */
        int id = Integer.parseInt(body.get("id").toString());

        String date = body.get("date").toString();

        return dataService.completeAction(id, date);
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

    @PostMapping("/removeFault")
    public CompletableFuture<Void> removeFault(@RequestBody(required = true) Map<String, Object> body) {
        /**
         * This method is used to remove a fault from the database.
         * @param body
         * @return
         *
         * Example URL:
         * http://localhost:8080/api/removeFault
         */
        String date = body.get("date").toString();
        Integer machineNumber = Integer.valueOf(body.get("machineNumber").toString());

        return dataService.removeFault(date, machineNumber);
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

    @PostMapping("/InputKnittingFaultLog")
    public CompletableFuture<Void> InputKnittingFaultLog(@RequestParam String data) {

        return dataService.inputKnittingFaultLog(data);
    }

    @PostMapping("/InputWarpingFaultLog")
    public CompletableFuture<Void> InputWarpingFaultLog(@RequestParam String data) {

        return dataService.inputWarpingFaultLog(data);
    }

    @PostMapping("/InputKnittingProductionLog")
    public CompletableFuture<Void> InputKnittingProductionLog(@RequestParam String data) {

        return dataService.inputKnittingProductionLog(data);
    }

    @PostMapping("/InputWarpingProductionLog")
    public CompletableFuture<Void> InputWarpingProductionLog(@RequestParam String data) {

        return dataService.inputWarpingProductionLog(data);
    }

    @PostMapping("/InputKnittingWarpRefLog")
    public CompletableFuture<Void> InputKnittingWarpRefLog(@RequestParam String data) {

        return dataService.inputKnittingWarpRefLog(data);
    }
}