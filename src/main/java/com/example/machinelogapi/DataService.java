/**
 *  This class is the service class for the API
 *   It fetches the data from the database and returns it to the controller.
 *   It also updates the database with new data, live from the machines.
 *   The methods in this class are annotated with @Async to make them asynchronous.
 *
 */

package com.example.machinelogapi;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DataService {
    private SQLManager sqlmanager = new SQLManager();

    public DataService(SQLManager sqlmanager) {
        this.sqlmanager = sqlmanager;
    }

    @Async
    public CompletableFuture<Map<String, Object>> getOverviewData(String date, String shift) {
        return CompletableFuture.completedFuture(sqlmanager.getOverviewData(date, shift));
    }

    @Async
    public CompletableFuture<Map<String, Object>> getMachineCardData(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(sqlmanager.getMachineCardData(machineNumber, date, shift));
    }

    @Async
    public CompletableFuture<int[]> getMachineNumbers() {
        return CompletableFuture.completedFuture(sqlmanager.getMachineNumbers());
    }

    @Async
    public CompletableFuture<Map<String, Object>> getFaultLog(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(sqlmanager.getFaultLog(machineNumber, date, shift));
    }

    @Async
    public CompletableFuture<Map<String, Object>> getFaultReport(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(sqlmanager.getFaultReport(machineNumber, date, shift));
    }

    @Async
    public CompletableFuture<Map<Integer, String>> getOperators() {
        return CompletableFuture.completedFuture(sqlmanager.getOperators());
    }

    @Async
    public CompletableFuture<Map<Integer, String>> checkAccountableKnitter(String date, String shift, List<Integer> machines) {
        return CompletableFuture.completedFuture(sqlmanager.checkAccountableKnitter(date, shift, machines));
    }

    @Async
    public CompletableFuture<Void> setAccountableKnitter(Integer Operator, String date, String shift, List<Integer> Machines) {
        sqlmanager.SetAccountableKnitter(Operator, date, shift, Machines);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> inputKnittingFaultLog(String data) {
        sqlmanager.inputKnittingFaultLog(data);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> inputWarpingFaultLog(String data) {
        sqlmanager.inputWarpingFaultLog(data);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> inputKnittingProductionLog(String data) {
        sqlmanager.inputKnittingProductionLog(data);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    CompletableFuture<Void> inputWarpingProductionLog(String data) {
        sqlmanager.inputWarpingProductionLog(data);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    CompletableFuture<Void> inputKnittingWarpRefLog(String data) {
        sqlmanager.inputKnittingWarpRefLog(data);
        return CompletableFuture.completedFuture(null);
    }
}