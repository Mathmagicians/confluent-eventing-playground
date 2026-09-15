package dk.mathmagicians.playground.confluent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/// The substrate's main class: a distribution names it as its own. It scans the platform's packages; a story's
/// beans and settings come from the story's auto-configuration, which is how a jar adds them.
@Modulithic
@SpringBootApplication
@ConfigurationPropertiesScan("dk.mathmagicians.playground.confluent.eventing")
public class EventingPlatform {

    public static void main(String[] args) {
        SpringApplication.run(EventingPlatform.class, args);
    }
}
