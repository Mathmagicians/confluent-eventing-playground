package dk.mathmagicians.playground.confluent.eventing.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

/// Test driver running the image under test as a container: one generator against the test cluster, the minimum
/// load, and the receipt it logs. The image is the `bdd.image` system property, set by the Gradle `bdd` task.
class GeneratorContainer {

    private static final Logger log = LoggerFactory.getLogger(GeneratorContainer.class);

    private static final DockerImageName IMAGE = DockerImageName.parse(System.getProperty("bdd.image"));

    /// What the image reads, by the names of `.env.<ENV>.private` and the GitHub environments, passed through
    private static final List<String> CREDENTIALS =
            List.of(
                    "KAFKA_BOOTSTRAP_SERVERS",
                    "KAFKA_API_KEY",
                    "KAFKA_API_SECRET",
                    "SCHEMA_REGISTRY_REST_ENDPOINT",
                    "SCHEMA_REGISTRY_API_KEY",
                    "SCHEMA_REGISTRY_API_SECRET");

    /// One producer, about one event, then exit; the publisher's receipt line is at DEBUG
    private static final List<String> MINIMUM =
            List.of(
                    "--load.concurrent=1",
                    "--load.interval=1000",
                    "--load.ttl=2",
                    "--logging.level.dk.mathmagicians=DEBUG");

    /// `Published <envelope id> to <topic>-<partition> at offset <offset>`, as `KafkaPublisher` logs it
    private static final Pattern RECEIPT = Pattern.compile("Published (\\S+) to (\\S+)-(\\d+) at offset (\\d+)");

    /// image pull, JVM start, the load, and the exit
    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    /// One generator per payload type, in turn, each the minimum load: the receipts in the order they were logged.
    List<Receipt> publish(List<String> types, String region) {
        return types.stream().map(type -> publishOne(type, region)).toList();
    }

    /// Runs one generator of the payload type in the region to completion and answers the first receipt it
    /// logged. The container's output goes to the test log.
    Receipt publishOne(String type, String region) {
        try (var generator = new GenericContainer<>(IMAGE)) {
            generator
                    .withEnv(credentials())
                    .withEnv("SPRING_PROFILES_ACTIVE", "test")
                    .withCommand(command(type, region))
                    .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(TIMEOUT))
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .start();
            return receipt(generator.getLogs());
        }
    }

    /// The receipt names the envelope and where it landed.
    void assertReceived(Receipt receipt) {
        assertThat(receipt.envelopeId()).isNotBlank();
        assertThat(receipt.partition()).isNotNegative();
        assertThat(receipt.offset()).isNotNegative();
    }

    private static Map<String, String> credentials() {
        var env = new HashMap<String, String>();
        for (var name : CREDENTIALS) {
            var value = System.getenv(name);
            assertThat(value).as("%s, from .env.<ENV>.private", name).isNotBlank();
            env.put(name, value);
        }
        return env;
    }

    private static String[] command(String type, String region) {
        return Stream.concat(
                        Stream.of("--load.type=" + type.toLowerCase(), "--load.region=" + region),
                        MINIMUM.stream())
                .toArray(String[]::new);
    }

    private static Receipt receipt(String logs) {
        var line = RECEIPT.matcher(logs);
        assertThat(line.find()).as("a receipt line in the generator's log").isTrue();
        return new Receipt(
                line.group(1), line.group(2), Integer.parseInt(line.group(3)), Long.parseLong(line.group(4)));
    }
}
