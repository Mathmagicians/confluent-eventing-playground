package dk.mathmagicians.playground.confluent.eventing.adapter.cli.console;

import dk.mathmagicians.playground.confluent.eventing.adapter.protobuf.Converter;
import dk.mathmagicians.playground.confluent.eventing.application.Publishing;
import dk.mathmagicians.playground.confluent.eventing.application.Story;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/// Driving adapter for `local`: the console stands in for the topics. When the context starts and the selected
/// story listens, a thread reads the input line by line, a session of three kinds of line, and hands every
/// message to the story as an envelope stamped with the session's region. A story that listens to nothing gets no
/// console. The thread keeps the process alive while the input is open: EOF ends it, and so does the story's
/// ttl, through `stop()`.
///
/// The session: `help` prints the three kinds and a template per type the story listens to, at the start and on
/// request; `region: <name>` sets the region of every message after it, the platform's to begin with; anything
/// else is a message, the type's name and its fields as Protobuf text, `Offer offer_id: "OFF-1" product_id:
/// "P-TOPH" price: 12.5 seller_id: "MAD_HATTER"`. A line the console cannot read prints the reason and the help,
/// and the story never sees it.
@PrimaryAdapter
@Component
@Profile("local")
public final class ConsoleListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ConsoleListener.class);
    private static final String THREAD = "console";

    private final Story story;
    private final Publishing publishing;
    private final InputStream in;
    private final PrintStream out;
    private @Nullable Thread reading;

    @Autowired
    ConsoleListener(Story story, Publishing publishing) {
        this(story, publishing, System.in, System.out);
    }

    /// The console over any input and output, another implementation of listening than kafka
    ConsoleListener(Story story, Publishing publishing, InputStream in, PrintStream out) {
        this.story = story;
        this.publishing = publishing;
        this.in = in;
        this.out = out;
        this.typesThatWeListenTo = this.story.listensTo().stream().map(Class::getSimpleName).collect(Collectors.toSet());
    }

    private final Set<String> typesThatWeListenTo;

    @Override
    public void start() {
        if (story.listensTo().isEmpty()) {
            log.info("{} listens to nothing, no console", story.name());
            return;
        }
        var session = new Session();
        log.info("""
                            Welcome to the Console Listener - we make it easy to test your story without a Kafka cluster, using just the console.
                            This story - {} -  listens to events from the console, accepting events of type: {}, for region {}.
                        """,
                story.name(), typesThatWeListenTo, publishing.region());
        session.help();
        reading = Thread.ofPlatform().name(THREAD).start(() -> session.read(in));

    }

    /// Closing the input ends the reading thread, wherever it waits.
    @Override
    public void stop() {
        if (reading != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new UncheckedIOException("cannot close the console", e);
            }
            reading = null;
        }
    }

    @Override
    public boolean isRunning() {
        return reading != null && reading.isAlive();
    }

    /// One session: the story at the table, the region in force, the kinds of line.
    final class Session {

        private String region;
        final String REGION = "region: ";


        Session() {
            this.region = publishing.region();
        }

        /// Every line until EOF, each on its own so a bad one costs nothing but its help.
        void read(InputStream in) {
            try (var lines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                for (var line = lines.readLine(); line != null; line = lines.readLine()) {
                    line(line);
                }
            } catch (IOException e) {
                log.debug("The console closed", e);
            }
            log.info("The console is closed, {} heard everything", story.name());
        }

        /// One line: blank is nothing, `help`, `q` or `quit`, `region: <name>`, or a message for the story.
        void line(String line) {
            if (line.isBlank()) {
                out.println(">>> Have some tea, said the March Hare. There is no tea. Type something, like a line, or ask for help, like HELP.");
                return;
            }
            out.println("YOU <<<: " + line);

            if (line.equalsIgnoreCase("help")) {
                help();
            } else if (line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit")) {
                quit();
            } else if (line.toLowerCase().startsWith(REGION)) {
                region = line.toUpperCase().substring(REGION.length()).trim();
                out.println("The region is now " + region);
                return;
            } else {
                var tokens = line.split("\\s+", 2);
                var type = getTypeByNameFromListenTo(tokens[0]);
                if (type == null) {
                    out.println(">>> Unrecognized type: " + tokens[0]);
                    help();
                } else {
                    out.println(">>> You entered a message of type " + tokens[0] + " for region " + region);
                    Payload payload;
                    try {
                        payload = Converter.from(type, tokens.length > 1 ? tokens[1] : "");
                    } catch (IllegalArgumentException e) {
                        out.println(">>> The console, with all its might, could not read your message: " + e.getMessage());
                        help();
                        return;
                    }
                    try {
                        story.on(publishing.envelope(region, payload));
                        out.println(">>> The story accepted the message, it is now in the story's hands");
                    } catch (RuntimeException e) {
                        out.println(">>> The story set the message aside: " + e.getMessage());
                    }
                }
            }
        }

        void help() {
            var recordShapes = story.listensTo().stream().map(Converter::shape).sorted().collect(Collectors.joining("\n"));
            var helpText = """
                    The Console Listener accepts three kinds of line:
                    
                    * %s <name> sets the region of every message after it, the platform's default is %s.
                      When you enter a region, every event after it will be stamped with that region.
                    
                    * You may enter an event using protobuf text format, starting with the type's name, then its fields, one per line.
                    
                    * Type help for instructions, q or quit to end the session, Ctrl+D does the same.
                    
                    **************** The types you can input as events are: ****************  
                    
                    %s
                    
                    **************** Curiouser and curiouser! Type a line and see where it goes; Ctrl+D closes the rabbit hole. **************** 
                    """.formatted(REGION, publishing.region(), recordShapes);
            out.println(helpText);
        }

        void quit() {
            out.println(">>> You asked to quit, quiting ... ");
            stop();
        }

        Class<? extends Payload> getTypeByNameFromListenTo(String typeName) {
            return story.listensTo().stream().filter(type -> type.getSimpleName()
                    .equalsIgnoreCase(typeName)).findFirst().orElse(null);
        }
    }
}
