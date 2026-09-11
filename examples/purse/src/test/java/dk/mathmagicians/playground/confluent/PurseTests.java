package dk.mathmagicians.playground.confluent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/// The wiring test of the distribution: the platform with the purse on the classpath, playing it. The local
/// profile reads nothing, and the ttl left empty keeps the runner from waiting, so the context comes up and goes
/// down with the test.
@SpringBootTest(properties = "purse.ttl=")
class PurseTests {

    @Test
    void contextLoads() {
    }
}
