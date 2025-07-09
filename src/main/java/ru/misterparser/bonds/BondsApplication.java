package ru.misterparser.bonds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BondsApplication {

    public static void main(String[] args) {
        SpringApplication.run(BondsApplication.class, args);
    }
}