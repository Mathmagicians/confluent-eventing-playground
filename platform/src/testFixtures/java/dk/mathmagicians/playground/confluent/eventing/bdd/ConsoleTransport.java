package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.dockerjava.api.async.ResultCallback;
import dk.mathmagicians.playground.confluent.eventing.adapter.cli.console.ConsoleListener;
import dk.mathmagicians.playground.confluent.eventing.adapter.cli.log.LoggingPublisher;
import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.Topics;
import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import io.cucumber.spring.ScenarioScope;
import jakarta.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

/// The transport on `cli`: the console a story reads and the log the image writes. What the scenario publishes
/// is typed into the story's console, the region first, then the payload as a line; what the image publishes is a
/// line in its log, the generator's or the story's. A story has read the streams when its console has handed it
/// every message typed. The topic of a payload is the profile's `topics.*`, as on the cluster.
@ScenarioScope
@Profile("cli")
class ConsoleTransport implements Transport {

    /// the topic a receipt names for what the scenario typed into a story's console
    private static final String CONSOLE = "console";

    /// Spring Boot's line once the context is up and the console reads
    private static final String LISTENING = ".*Started \\S+ in .*\\n";

    private static final Pattern PUBLISHED = pattern(LoggingPublisher.PUBLISHED);
    private static final Pattern HEARD = pattern(ConsoleListener.HEARD);

    private final GeneratorContainer generator;
    private final Topics topics;
    private final List<String> told = new ArrayList<>();
    private @Nullable GenericContainer<?> story;
    private @Nullable OutputStream stdin;
    private @Nullable Closeable attached;

    ConsoleTransport(GeneratorContainer generator, Environment environment) {
        this.generator = generator;
        this.topics = Binder.get(environment).bind("topics", Topics.class).get();
    }

    /// The console and the log need no keys: the Docker host that runs the image answers.
    @Override
    public void assertOpen() {
        assertThat(DockerClientFactory.instance().isDockerAvailable()).as("the Docker host answers").isTrue();
    }

    @Override
    public void listen(GenericContainer<?> story, String group) {
        story.withCreateContainerCmdModifier(command -> command.withStdinOpen(true));
        this.story = story;
    }

    @Override
    public String listening() {
        return LISTENING;
    }

    @Override
    public boolean caughtUp(String group, List<String> payloads) {
        var heard = HEARD.matcher(running().getLogs()).results().map(line -> line.group(1)).collect(toSet());
        return heard.containsAll(told);
    }

    @Override
    public void left(String group) {
        close();
    }

    /// Closes the story's console, when one was attached, and forgets the story.
    @PreDestroy
    void close() {
        try {
            if (stdin != null) {
                stdin.close();
            }
            if (attached != null) {
                attached.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot close the story's console", e);
        }
        stdin = null;
        attached = null;
        story = null;
    }

    @Override
    public Receipt publish(Envelope envelope) {
        type("region: " + envelope.region());
        type(Converter.text(envelope.payload()));
        told.add(envelope.payload().id());
        return new Receipt(envelope.id(), CONSOLE, 0, told.size() - 1);
    }

    @Override
    public List<Transaction> transactions() {
        return logged(running().getLogs())
                .map(Logged::payload)
                .filter(Transaction.class::isInstance)
                .map(Transaction.class::cast)
                .toList();
    }

    @Override
    public List<Payload> payloads(List<Receipt> receipts) {
        return receipts.stream().map(this::logged).map(Logged::payload).toList();
    }

    @Override
    public void assertEnvelopeAt(String topic, Receipt receipt) {
        var line = logged(receipt);

        assertThat(line.receipt()).isEqualTo(receipt);
        assertThat(topics.select(line.payload())).as("the topic of %s", line.type()).isEqualTo(topic);
    }

    @Override
    public void assertSerializedWithTheSchemaOf(String topic, Receipt receipt) {
        var type = typeOn(topic);
        var line = logged(receipt);

        assertThat(line.type()).as("the message on %s", topic).isEqualTo(type.getSimpleName());
        assertThatCode(() -> Converter.from(type, line.fields())).doesNotThrowAnyException();
    }

    @Override
    public void assertRegion(Receipt receipt, String region) {
        assertThat(logged(receipt).region()).isEqualTo(region);
    }

    /// A line the image logged as it published: the receipt, the region, and the payload, its type's name and its
    /// fields as Protobuf text.
    private record Logged(Receipt receipt, String region, String type, String fields) {

        Payload payload() {
            return Converter.from(typeOf(type), fields);
        }
    }

    /// The line the generator logged for the receipt.
    private Logged logged(Receipt receipt) {
        return logged(generator.logs())
                .filter(line -> line.receipt().envelopeId().equals(receipt.envelopeId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no line in the generator's log for " + receipt));
    }

    /// Every line of the log the image wrote as it published, in the order logged.
    private static Stream<Logged> logged(String logs) {
        return PUBLISHED.matcher(logs).results().map(line -> {
            var payload = line.group(7).split(" ", 2);
            return new Logged(
                    new Receipt(line.group(1), line.group(2), Integer.parseInt(line.group(3)),
                            Long.parseLong(line.group(4))),
                    line.group(6), payload[0], payload.length > 1 ? payload[1] : "");
        });
    }

    /// Types a line into the story's console, attached at the first line.
    private void type(String line) {
        try {
            var console = stdin();
            console.write((line + "\n").getBytes(UTF_8));
            console.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot type into the story's console: " + line, e);
        }
    }

    private OutputStream stdin() throws IOException {
        if (stdin == null) {
            var running = running();
            var in = new PipedInputStream();
            stdin = new PipedOutputStream(in);
            attached = running.getDockerClient()
                    .attachContainerCmd(running.getContainerId())
                    .withStdIn(in)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<>());
        }
        return stdin;
    }

    private GenericContainer<?> running() {
        if (story == null) {
            throw new IllegalStateException("no story is running; start one first");
        }
        return story;
    }

    /// The payload type whose topic it is, from the profile's `topics.*`.
    private Class<? extends Payload> typeOn(String topic) {
        return Stream.of(Payload.class.getPermittedSubclasses())
                .map(type -> type.asSubclass(Payload.class))
                .filter(type -> topics.of(type).equals(topic))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(topic + " is no payload's topic"));
    }

    private static Class<? extends Payload> typeOf(String name) {
        return Stream.of(Payload.class.getPermittedSubclasses())
                .filter(type -> type.getSimpleName().equals(name))
                .map(type -> type.asSubclass(Payload.class))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(name + " is no payload type"));
    }

    /// The pattern of an SLF4J message: its text as it is, each `{}` a word but the last, the rest of the line.
    private static Pattern pattern(String message) {
        var parts = message.split("\\{}", -1);
        var last = parts.length - 1;
        return Pattern.compile(Stream.of(parts).limit(last).map(Pattern::quote).collect(joining("(\\S+)"))
                + "(.+)" + Pattern.quote(parts[last]));
    }
}
