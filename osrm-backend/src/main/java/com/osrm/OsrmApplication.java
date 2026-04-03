package com.osrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OsrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsrmApplication.class, args);
    }
}
