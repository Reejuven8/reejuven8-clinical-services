package com.reejuven8.ninemo.clinical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NinemoClinicalApplication {
    public static void main(String[] args) {
        SpringApplication.run(NinemoClinicalApplication.class, args);
    }
}
