package com.example.machinelogapi;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class DataService {

    private CSVParser parser = new CSVParser();
    private SQLFetcher fetcher = new SQLFetcher();
    private SQLPoster poster = new SQLPoster();


    public DataService(CSVParser parser, SQLFetcher fetcher, SQLPoster poster) {
        this.parser = parser;
        this.fetcher = fetcher;
        this.poster = poster;

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

    @Async
    public CompletableFuture<Map<Integer, String>> getOperators() {
        return CompletableFuture.completedFuture(fetcher.getOperators());
    }

    @Async
    public CompletableFuture<Map<Integer, String>> checkAccountableKnitter(String date, String shift, List<Integer> machines) {
        return CompletableFuture.completedFuture(fetcher.checkAccountableKnitter(date, shift, machines));
    }

    @Async
    public CompletableFuture<Void> setAccountableKnitter(Integer Operator, String date, String shift, List<Integer> Machines) {
        fetcher.SetAccountableKnitter(Operator, date, shift, Machines);
        return CompletableFuture.completedFuture(null);
    }
}