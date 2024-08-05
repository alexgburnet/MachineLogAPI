package com.example.machinelogapi;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DataService {

    private CSVParser parser = new CSVParser();
    private SQLFetcher fetcher = new SQLFetcher();

    public DataService(CSVParser parser, SQLFetcher fetcher) {
        this.parser = parser;
        this.fetcher = fetcher;
    }

    @Async
    public CompletableFuture<Map<String, Object>> getOverviewData(String date, String shift) {
        return CompletableFuture.completedFuture(fetcher.getOverviewData(date, shift));
    }

    @Async
    public CompletableFuture<Map<String, Object>> getMachineCardData(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(fetcher.getMachineCardData(machineNumber, date, shift));
    }

    @Async
    public CompletableFuture<int[]> getMachineNumbers() {
        return CompletableFuture.completedFuture(parser.getMachineNumbers());
    }

    @Async
    public CompletableFuture<Map<String, Object>> getFaultLog(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(fetcher.getFaultLog(machineNumber, date, shift));
    }

    @Async
    public CompletableFuture<Map<String, Object>> getFaultReport(String machineNumber, String date, String shift) {
        return CompletableFuture.completedFuture(fetcher.getFaultReport(machineNumber, date, shift));
    }
}
