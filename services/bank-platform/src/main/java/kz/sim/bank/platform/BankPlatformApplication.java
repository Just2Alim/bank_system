package kz.sim.bank.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BankPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankPlatformApplication.class, args);
    }
}
