package dk.mathmagicians.playground.confluent.eventing.adapter.console;

import dk.mathmagicians.playground.confluent.eventing.application.Publishing;
import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

    private final Stories stories;
    private final Publishing publishing;
    private final InputStream in;
    private final PrintStream out;
    private @Nullable Thread reading;

    @Autowired
    ConsoleListener(Stories stories, Publishing publishing) {
        this(stories, publishing, System.in, System.out);
    }

    /// The console over any input and output, the tests' way in.
    ConsoleListener(Stories stories, Publishing publishing, InputStream in, PrintStream out) {
        this.stories = stories;
        this.publishing = publishing;
        this.in = in;
        this.out = out;
    }

    @Override
    public void start() {
        var story = stories.selected();
        if (story.listensTo().isEmpty()) {
            log.info("{} listens to nothing, no console", story.name());
            return;
        }
        var session = new Session(story);
        session.help();
        reading = Thread.ofPlatform().name(THREAD).start(() -> session.read(in));
        log.info("{} listens to the console, {} to type, help for help", story.name(), story.listensTo());
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

    /// One session: the story at the table, the region in force, the three kinds of line.
    final class Session {

        private final Story story;
        private String region;

        Session(Story story) {
            this.story = story;
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

        /// One line: blank is nothing, `help`, `region: <name>`, or a message for the story.
        void line(String line) {
            // FIXME blank: nothing. `help`: help(). `region: <name>`: the region of the session from here on.
            // FIXME otherwise a message: the first word is the type, the rest the fields, Converter.builder(type)
            // FIXME filled by TextFormat.merge, Converter.from(builder.build()) the payload, then
            // FIXME story.on(publishing.envelope(region, payload)); what fails prints the reason, then help()
            out.println("? " + line);
        }

        /// The three kinds of line, and a template per type the story listens to, from the schema.
        void help() {
            // FIXME the three kinds in three lines, then Converter.builder(type).getDescriptorForType().getFields()
            // FIXME per type the story listens to, sorted: the type's name, then `field_name: <empty>` per field,
            // FIXME "" for a string, 0 for a number, { } for a message, on one line
            out.println("help");
        }
    }
}
