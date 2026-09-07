package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.ParameterType;
import org.springframework.core.env.Environment;

/// Parameter types shared by every feature.
public class ParameterTypes {

    private final Environment environment;

    public ParameterTypes(Environment environment) {
        this.environment = environment;
    }

    /// `topic orders` or `dead-letter topic for orders`, resolved through the `topics.*` properties of the profile
    /// to the name on the cluster: `test.orders`, `test.orders.DLT`.
    @ParameterType("topic (\\w+)|dead-letter topic for (\\w+)")
    public String topic(String name, String deadLetterOf) {
        return name != null ? named(name) : named(deadLetterOf) + ".DLT";
    }

    private String named(String payload) {
        return environment.getRequiredProperty("topics." + payload);
    }
}
