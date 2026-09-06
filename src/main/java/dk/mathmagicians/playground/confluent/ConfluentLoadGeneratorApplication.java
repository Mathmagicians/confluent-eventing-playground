package dk.mathmagicians.playground.confluent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ConfluentLoadGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfluentLoadGeneratorApplication.class, args);
    }

}
