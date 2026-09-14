package dk.mathmagicians.playground.confluent.eventing.bdd;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;

import io.cucumber.spring.ScenarioScope;
import java.util.UUID;
import org.springframework.stereotype.Component;

/// The scenario's own corner of the cluster: the feature's region with a salt nobody else has, `EMEA` as
/// `EMEA-3f9a2c1d`, one salt per scenario. Every generator, story, and event of the scenario uses it, so two
/// runs at once, or a run beside the flock, never hear each other. The region in force is the last one a step
/// named, the fixtures' to begin with.
@Component
@ScenarioScope
public class Region {

    private final String salt = UUID.randomUUID().toString().substring(0, 8);
    private String current = REGION + "-" + salt;

    public String salt() {
        return salt;
    }

    /// The feature's word as this scenario's region, in force from here on.
    public String of(String word) {
        current = word + "-" + salt;
        return current;
    }

    /// The region in force.
    public String current() {
        return current;
    }
}
