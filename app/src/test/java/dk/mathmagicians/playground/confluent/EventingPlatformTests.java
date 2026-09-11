package dk.mathmagicians.playground.confluent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/// The wiring test of the distribution: the platform with the stories on the classpath, the load story playing.
/// One thread for one second, so the load it starts stays a smoke.
@SpringBootTest(properties = {"load.concurrent=1", "load.ttl=1"})
class EventingPlatformTests {

    @Test
    void contextLoads() {
    }
}
