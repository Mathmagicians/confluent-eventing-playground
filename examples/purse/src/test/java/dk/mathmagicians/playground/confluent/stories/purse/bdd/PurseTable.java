package dk.mathmagicians.playground.confluent.stories.purse.bdd;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.bdd.Region;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import dk.mathmagicians.playground.confluent.stories.purse.KnowWhatIsLeft;
import dk.mathmagicians.playground.confluent.stories.purse.KnowWhatIsLeft.Purse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Test driver of the purse's outcome: the story as a container with one purse in the scenario's region, opened
/// before the trades, and what it says about its owner's trades, read from its log, one line per trade, as
/// `Purse` phrases them.
class PurseTable {

    private static final String SITS_FOR = "120";

    /// The purse's lines as patterns: each `{}` of the message a word, who, the price, the balance or the
    /// shortfall, numbers as `BigDecimal` prints them.
    private static final Pattern SOLD = line(Purse.SOLD);
    private static final Pattern BOUGHT = line(Purse.BOUGHT);
    private static final Pattern OWES = line(Purse.OWES);

    private final StoryContainer story;
    private final Region region;

    PurseTable(StoryContainer story, Region region) {
        this.story = story;
        this.region = region;
    }

    /// Opens the owner's purse with the coins, in the scenario's region: the story starts and listens before any
    /// trade is on the stream.
    void open(String name, int coins) {
        var settings = KnowWhatIsLeft.NAME + ".";
        story.start(KnowWhatIsLeft.NAME, Map.of(
                settings + "owners", name + ":" + coins,
                settings + "region", region.of(REGION),
                settings + "ttl", SITS_FOR));
    }

    /// The balance on the owner's last line, a sale's or a purchase's.
    void assertLeft(String owner, BigDecimal coins) {
        var last = Stream.concat(lines(SOLD).stream(), lines(BOUGHT).stream())
                .filter(line -> line.group(1).equals(owner))
                .max(Comparator.comparingInt(MatchResult::start));
        assertThat(last).as("a line of %s's purse", owner).isPresent();
        assertThat(new BigDecimal(last.get().group(3))).isEqualByComparingTo(coins);
    }

    void assertOwes(String owner, BigDecimal coins) {
        var lines = lines(OWES).stream().filter(line -> line.group(1).equals(owner)).toList();
        assertThat(lines).as("a line saying %s's coins are gone", owner).isNotEmpty();
        assertThat(new BigDecimal(lines.getLast().group(3))).isEqualByComparingTo(coins);
    }

    void assertSaidNothingAbout(String one, String other) {
        var speakers = Stream.of(SOLD, BOUGHT, OWES)
                .flatMap(line -> lines(line).stream())
                .map(line -> line.group(1))
                .toList();
        assertThat(speakers).doesNotContain(one, other);
    }

    /// The shortfall's line is a warning.
    void assertMakesAWarning() {
        var lines = story.logs().lines().filter(line -> OWES.matcher(line).find()).toList();
        assertThat(lines).isNotEmpty().allSatisfy(line -> assertThat(line).contains("WARN"));
    }

    private List<MatchResult> lines(Pattern line) {
        return line.matcher(story.logs()).results().toList();
    }

    /// The pattern of an SLF4J message: its text as it is, each `{}` a word.
    private static Pattern line(String message) {
        return Pattern.compile(Stream.of(message.split("\\{}", -1)).map(Pattern::quote).collect(joining("(\\S+)")));
    }
}
