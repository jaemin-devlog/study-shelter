package likelion.simsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SimsimApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimsimApplication.class, args);
    }
}
