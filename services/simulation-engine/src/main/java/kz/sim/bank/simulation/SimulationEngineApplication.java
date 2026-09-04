package kz.sim.bank.simulation;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SimulationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulationEngineApplication.class, args);
    }

    @Bean
    Clock wallClock() {
        return Clock.systemUTC();
    }
}
