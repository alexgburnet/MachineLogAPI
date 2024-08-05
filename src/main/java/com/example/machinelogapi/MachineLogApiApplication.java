package com.example.machinelogapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MachineLogApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MachineLogApiApplication.class, args);
    }

}
