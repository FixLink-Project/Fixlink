package com.fixlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FixlinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(FixlinkApplication.class, args);
    }
}
