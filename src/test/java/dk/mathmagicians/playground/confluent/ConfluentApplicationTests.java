package dk.mathmagicians.playground.confluent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/// The wiring test. One thread for one second, so the load it starts stays a smoke.
@SpringBootTest(properties = {"load.concurrent=1", "load.ttl=1"})
class ConfluentApplicationTests {

    @Test
    void contextLoads() {
    }

}
