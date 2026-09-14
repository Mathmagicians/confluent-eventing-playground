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
/// load or a handful, and the receipts it logs. The image is the `bdd.image` system property, set by the Gradle
/// `bdd` task.
public class GeneratorContainer {

    private static final Logger log = LoggerFactory.getLogger(GeneratorContainer.class);

    static final DockerImageName IMAGE = DockerImageName.parse(System.getProperty("bdd.image"));

    /// What the image reads, by the names of `.env.<ENV>.private` and the GitHub environments, passed through
    private static final List<String> CREDENTIALS =
            List.of(
                    "KAFKA_BOOTSTRAP_SERVERS",
                    "KAFKA_API_KEY",
                    "KAFKA_API_SECRET",
                    "SCHEMA_REGISTRY_REST_ENDPOINT",
                    "SCHEMA_REGISTRY_API_KEY",
                    "SCHEMA_REGISTRY_API_SECRET");

    /// The publisher's receipt line is at DEBUG
    private static final String RECEIPTS_AT_DEBUG = "--logging.level.dk.mathmagicians=DEBUG";

    /// One producer, about one event, then exit
    private static final List<String> MINIMUM =
            List.of("--load.concurrent=1", "--load.interval=1000", "--load.ttl=2", RECEIPTS_AT_DEBUG);

    /// Three producers for two seconds, a dozen events, enough for a thing to be both offered and ordered
    private static final List<String> A_HANDFUL =
            List.of("--load.concurrent=3", "--load.interval=500", "--load.ttl=2", RECEIPTS_AT_DEBUG);

    /// `Published <envelope id> to <topic>-<partition> at offset <offset>`, as `KafkaPublisher` logs it
    private static final Pattern RECEIPT = Pattern.compile("Published (\\S+) to (\\S+)-(\\d+) at offset (\\d+)");

    /// image pull, JVM start, the load, and the exit
    private static final Duration TIMEOUT = Duration.ofMinutes(3);

    /// One generator per payload type, in turn, each a handful of events: every receipt, in the order logged.
    public List<Receipt> publish(List<String> types, String region) {
        return types.stream().flatMap(type -> run(type, region, A_HANDFUL).stream()).toList();
    }

    /// Runs one generator of the payload type in the region to completion and answers the first receipt it
    /// logged. The container's output goes to the test log.
    Receipt publishOne(String type, String region) {
        return run(type, region, MINIMUM).getFirst();
    }

    private List<Receipt> run(String type, String region, List<String> load) {
        try (var generator = new GenericContainer<>(IMAGE)) {
            generator
                    .withEnv(credentials())
                    .withEnv("SPRING_PROFILES_ACTIVE", "test")
                    .withCommand(command(type, region, load))
                    .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(TIMEOUT))
                    .withLogConsumer(new Slf4jLogConsumer(log))
                    .start();
            return receipts(generator.getLogs());
        }
    }

    /// The receipt names the envelope and where it landed.
    void assertReceived(Receipt receipt) {
        assertThat(receipt.envelopeId()).isNotBlank();
        assertThat(receipt.partition()).isNotNegative();
        assertThat(receipt.offset()).isNotNegative();
    }

    /// The credentials of the test cluster from the environment, for any container of the image.
    static Map<String, String> credentials() {
        var env = new HashMap<String, String>();
        for (var name : CREDENTIALS) {
            var value = System.getenv(name);
            assertThat(value).as("%s, from .env.<ENV>.private", name).isNotBlank();
            env.put(name, value);
        }
        return env;
    }

    private static String[] command(String type, String region, List<String> load) {
        return Stream.concat(
                        Stream.of("--load.type=" + type.toLowerCase(), "--load.region=" + region), load.stream())
                .toArray(String[]::new);
    }

    /// Every receipt in the log, in the order logged; a generator that logged none is a failure.
    private static List<Receipt> receipts(String logs) {
        var receipts = RECEIPT.matcher(logs).results()
                .map(line -> new Receipt(
                        line.group(1), line.group(2), Integer.parseInt(line.group(3)), Long.parseLong(line.group(4))))
                .toList();
        assertThat(receipts).as("receipt lines in the generator's log").isNotEmpty();
        return receipts;
    }
}
