package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.Topics;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Wonderland;
import io.cucumber.java.ParameterType;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.env.Environment;

/// Parameter types shared by every feature.
public class ParameterTypes {

    private final Environment environment;

    public ParameterTypes(Environment environment) {
        this.environment = environment;
    }

    /// `topic orders`, `topic products.lvs`, or `dead-letter topic for orders`, resolved through the `topics.*`
    /// properties of the profile to the name on the cluster: `test.orders`, `test.products.lvs`, `test.orders.DLT`.
    @ParameterType("topic ([\\w.]+)|dead-letter topic for (\\w+)")
    public String topic(String name, String deadLetterOf) {
        return name != null ? named(name) : Topics.deadLetterOf(named(deadLetterOf));
    }

    /// `products`, `offers and orders`, `products, offers and orders`: the payload types a generator produces, as
    /// the `--load.type` values, `product`, `offer`, `order`. Only these words, so "orders and no offers" is
    /// somebody else's step.
    @ParameterType(
            "((?:products|offers|orders|transactions)(?:, (?:products|offers|orders|transactions))*"
                    + "(?: and (?:products|offers|orders|transactions))?)")
    public List<String> payloads(String list) {
        return Stream.of(list.split(", | and "))
                .map(plural -> plural.substring(0, plural.length() - 1))
                .toList();
    }

    /// The name on the cluster: the profile's `topics.*` property, or, for a table set up with IaC, the
    /// environment prefix of the products topic in front of the name.
    private String named(String payload) {
        var property = environment.getProperty("topics." + payload);
        if (property != null) {
            return property;
        }
        var prefix = environment.getRequiredProperty("topics.products").split("\\.")[0];
        return prefix + "." + payload;
    }

    /// A wonderlander as the features name one, `Alice`, `White Rabbit`, `Queen of Hearts`: a character in
    /// Wonderland, which answers with the key on the wire, `WHITE_RABBIT`, and refuses anyone else.
    @ParameterType("[A-Z][a-z]+(?: (?:of|[A-Z][a-z]+))*")
    public String wonderlander(String name) {
        return Wonderland.characterId(name);
    }

    /// A story as the features name one, `tea party`, `purse`: its name on the command line, `tea-party`.
    @ParameterType("[a-z]+(?: [a-z]+)*")
    public String story(String words) {
        return words.replace(' ', '-');
    }

    /// A thing as the features name one, `Top Hat`, `Drink Me Bottle`: its product id on the wire, `P-TOPH`.
    @ParameterType("[A-Z][a-z]+(?: [A-Z][a-z]+)*")
    public String thing(String name) {
        return EventFixtures.thing(name);
    }
}
