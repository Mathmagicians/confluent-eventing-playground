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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final String THREAD = ConsoleListener.class.getSimpleName() + "-Thread";

    private final Story story;
    private final Publishing publishing;
    private final InputStream in;
    private final PrintStream out;
    private final Set<String> typesThatWeListenTo;
    private @Nullable Thread reading;

    @Autowired
    ConsoleListener(Story story, Publishing publishing) {
        this(story, publishing, System.in, System.out);
    }

    /// The console over any input and output, the tests' way in.
    ConsoleListener(Story story, Publishing publishing, InputStream in, PrintStream out) {
        this.story = story;
        this.publishing = publishing;
        this.in = in;
        this.out = out;
        this.typesThatWeListenTo = story.listensTo().stream().map(Class::getSimpleName).collect(Collectors.toSet());
    }

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
        new Line.Help().act(session);
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

    sealed interface Line {

        void act(Session session);

        static Line of(String line) {
            var tokens = line.strip().split("\\s+", 2);
            Stream<Function<String[], Optional<Line>>> streamFroms = Stream.of(Blank::from, Help::from, Quit::from, Region::from, Message::from);
            return streamFroms.flatMap(kind -> kind.apply(tokens).stream()).findFirst().orElseThrow();
        }

        record Blank() implements Line {
            static Optional<Line> from(String[] tokens) {
                return tokens.length == 0 || tokens[0].isBlank() ? Optional.of(new Blank()) : Optional.empty();
            }

            @Override
            public void act(Session session) {
                session.say("Hey, you entered a blank line, nothing to do, try `help` for instructions. If you dont know whre you are going, any road takes you there.");
            }
        }

        record Help() implements Line {
            static final String HELP = "help";

            static Optional<Line> from(String[] tokens) {
                return tokens.length == 1 && tokens[0].equalsIgnoreCase(HELP) ? Optional.of(new Help()) : Optional.empty();
            }

            @Override
            public void act(Session session) {
                var recordShapes = session.listensTo().
                        stream().map(Converter::shape).sorted().collect(Collectors.joining("\n"));
                var helpText = """
                        The Console Listener accepts three kinds of line:
                        
                        * %s <name> sets the region of every message after it, the platform's default is %s.
                          When you enter a region, every event after it will be stamped with that region.
                        
                        * You may enter an event using protobuf text format, starting with the type's name, then its fields, one per line.
                        
                        * Type help for instructions, q or quit to end the session, Ctrl+D does the same.
                        
                        **************** The types you can input as events are: ****************  
                        
                        %s
                        
                        **************** Curiouser and curiouser! Type a line and see where it goes; Ctrl+D closes the rabbit hole. **************** 
                        """.formatted(Region.REGION, session.region(), recordShapes);
                session.say(helpText);
            }
        }

            record Quit() implements Line {
                static final Set<String> QUIT_COMMANDS = Set.of("q", "quit");

                static Optional<Line> from(String[] tokens) {
                    return tokens.length == 1 && QUIT_COMMANDS.contains(tokens[0].toLowerCase()) ? Optional.of(new Quit()) : Optional.empty();
                }

                @Override
                public void act(Session session) {
                    session.say("You asked to quit, quitting ... ");
                    session.close();
                }
            }

                record Region(String r) implements Line {
                    static final String REGION = "region:";

                    static Optional<Line> from(String[] tokens) {
                        return tokens[0].equalsIgnoreCase(REGION)
                                ? Optional.of(new Region(tokens.length > 1 ? tokens[1] : ""))
                                : Optional.empty();
                    }

                    @Override
                    public void act(Session session) {
                        var name = r.trim().toUpperCase();
                        if (name.isEmpty()) {
                            session.say(">>> A region needs a name; it stays " + session.region);
                            return;
                        }
                        session.region = name;
                        session.say("The region is now " + session.region + ", will be used for all the following messages.");
                    }
                }

                record Message(String type, String body) implements Line {
                    static Optional<Line> from(String[] tokens) {
                        return Optional.of(new Message(tokens[0], tokens.length > 1 ? tokens[1] : ""));
                    }

                    @Override
                    public void act(Session session) {
                        var typeClass = session.findTypeListenedToFromName(type);
                        if (typeClass.isEmpty()) {
                            session.say(">>> Unrecognized type: " + type);
                            new Help().act(session);
                            return;
                        }
                        Payload payload;
                        try {
                            payload = Converter.from(typeClass.get(), body);
                            session.publish(payload);
                            log.info(">>> Published {} to {} in region {}", payload.id(), typeClass.get().getSimpleName(), session.region());
                        } catch(IllegalArgumentException e) {
                            session.say(">>> Failed to read the message: " + e.getMessage());
                            new Help().act(session);
                        }
                    }
                }
            }

            /// Session controls handling the user input one line at a time
            final class Session {

                private String region;

                Session() {
                    this.region = publishing.region();
                }

                void say(String message) {
                    out.println(message);
                }

                /// The region in force, stamped on every message until the next `region:` line.
                String region() {
                    return region;
                }

                void region(String name) {
                    region = name;
                }

                /// The types the story listens to, what a message may be.
                Set<Class<? extends Payload>> listensTo() {
                    return story.listensTo();
                }

                /// return a type the story listens to by its name, case-insensitive, or empty when none is.
                Optional<Class<? extends Payload>> findTypeListenedToFromName(String word) {
                    return story.listensTo().stream()
                            .filter(type -> type.getSimpleName().equalsIgnoreCase(word))
                            .findFirst();
                }

                /// The payload to the story, as an envelope of the region in force; what the story throws comes back.
                void publish(Payload payload) {
                    story.on(publishing.envelope(region, payload));
                }

                /// Closes the input, which ends the reading wherever it waits: what quit does, and stop.
                void close() {
                    stop();
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
                    switch (Line.of(line)) {
                        case Line.Blank blank -> blank.act(this);
                        case Line.Help help -> help.act(this);
                        case Line.Quit quit -> quit.act(this);
                        case Line.Region region -> region.act(this);
                        case Line.Message message -> message.act(this);
                    }
                }

            }
        }
