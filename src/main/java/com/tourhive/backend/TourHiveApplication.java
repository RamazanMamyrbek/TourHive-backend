package com.tourhive.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TourHiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourHiveApplication.class, args);
    }
}
