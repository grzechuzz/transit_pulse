package com.transitpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class TransitPulseApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransitPulseApiApplication.class, args);
    }

}
