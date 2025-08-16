package ru.misterparser.bonds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BondsApplication {

    public static void main(String[] args) {
        setupChromeDriver();
        SpringApplication.run(BondsApplication.class, args);
    }

    private static void setupChromeDriver() {
        String os = System.getProperty("os.name").toLowerCase();
        String driverPath;

        if (os.contains("win")) {
            driverPath = "drivers/windows/chromedriver.exe";
        } else if (os.contains("mac")) {
            driverPath = "drivers/mac/chromedriver";
        } else {
            driverPath = "drivers/linux/chromedriver";
        }

        System.setProperty("webdriver.chrome.driver", driverPath);
    }
}