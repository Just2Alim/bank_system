package kz.sim.bank.legacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "kz.sim.bank")
public class LegacyCoreSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegacyCoreSimulatorApplication.class, args);
    }
}
