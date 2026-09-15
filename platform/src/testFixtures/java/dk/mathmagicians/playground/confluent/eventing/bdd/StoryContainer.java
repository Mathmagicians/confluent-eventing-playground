package dk.mathmagicians.playground.confluent.eventing.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.cucumber.spring.ScenarioScope;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.kafka.common.TopicPartition;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

/// Test driver running the image under test as a story that listens: one container per scenario, started with
/// the story's settings under a group of the scenario's own, `<story>-testrun-<salt>`, reading its topics from
/// their end, so what the scenario publishes after the start is what the story sees. The container's log is the
/// story's voice; the scenario reads it. When the scenario ends the story is stopped the way a deployment stops
/// it, and its group is forgotten.
@Component
@ScenarioScope
public class StoryContainer {

    private static final Logger log = LoggerFactory.getLogger(StoryContainer.class);

    /// Spring Kafka's line once the consumer has its partitions: from here on the story sees what is published
    private static final String LISTENING = ".*partitions assigned.*\\n";

    /// image start, the context, the group join
    private static final Duration STARTUP = Duration.ofMinutes(3);

    /// a story reads what its topics get from here on; a settlement, a purse's line, is seconds behind
    private static final Duration CATCHING_UP = Duration.ofSeconds(90);

    /// what Spring gets to close the context on SIGTERM, so the consumer leaves its group, before the kill
    private static final int GRACEFUL_SECONDS = 20;

    private final Cluster cluster;
    private final Region region;
    private @Nullable GenericContainer<?> container;
    private String name = "";
    private String group = "";
    private Map<TopicPartition, Long> endAtStart = Map.of();

    StoryContainer(Cluster cluster, Region region) {
        this.cluster = cluster;
        this.region = region;
    }

    /// Starts the story with its settings, `--<name>.<key>=<value>` each, and returns once it listens.
    public void start(String story, Map<String, String> settings) {
        name = story;
        group = story + "-testrun-" + region.salt();
        endAtStart = cluster.endOffsets(cluster.topics());
        var command = Stream.concat(
                        Stream.of(
                                "--story=" + story,
                                "--spring.kafka.consumer.group-id=" + group,
                                "--spring.kafka.consumer.auto-offset-reset=latest"),
                        settings.entrySet().stream().map(setting -> "--" + setting.getKey() + "=" + setting.getValue()))
                .toArray(String[]::new);
        var started = new GenericContainer<>(GeneratorContainer.IMAGE)
                .withEnv(GeneratorContainer.credentials())
                .withEnv("SPRING_PROFILES_ACTIVE", "test")
                .withCommand(command)
                .withLogConsumer(new Slf4jLogConsumer(log))
                .waitingFor(Wait.forLogMessage(LISTENING, 1).withStartupTimeout(STARTUP));
        started.start();
        container = started;
    }

    /// The story named is the one running; a feature that names another has the wrong table.
    public void assertRunning(String story) {
        running();
        assertThat(name).as("the story at the table").isEqualTo(story);
    }

    /// Waits until the story has read the streams named to their end: its group's committed offsets have reached
    /// the end of every partition that got records since the story started.
    public void awaitCaughtUp(List<String> payloads) {
        var topics = payloads.stream().map(cluster::topic).toList();
        await().atMost(CATCHING_UP)
                .pollInterval(Duration.ofSeconds(3))
                .pollInSameThread()
                .until(() -> cluster.caughtUp(group, topics, endAtStart));
    }

    /// Everything the story has logged so far.
    public String logs() {
        return running().getLogs();
    }

    /// Waits until the story's log has at least the number of lines matching, within the time given. Polls on
    /// the scenario's own thread, where this scenario-scoped driver is in reach.
    public void awaitLines(Pattern line, int count, Duration within) {
        await().atMost(within)
                .pollInterval(Duration.ofSeconds(1))
                .pollInSameThread()
                .until(() -> line.matcher(logs()).results().count() >= count);
    }

    /// Stops the story the way a deployment does, SIGTERM first, so the context closes and the consumer leaves
    /// its group; then the container goes, and the group with it.
    @PreDestroy
    public void stop() {
        if (container != null) {
            container.getDockerClient()
                    .stopContainerCmd(container.getContainerId())
                    .withTimeout(GRACEFUL_SECONDS)
                    .exec();
            container.stop();
            container = null;
            cluster.forget(group);
        }
    }

    private GenericContainer<?> running() {
        if (container == null) {
            throw new IllegalStateException("no story is running; start one first");
        }
        return container;
    }
}
