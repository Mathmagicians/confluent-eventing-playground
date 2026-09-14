package dk.mathmagicians.playground.confluent.stories.purse.bdd;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.DORMOUSE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MAD_HATTER;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MARCH_HARE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.trade;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.confluent.stories.purse.KnowWhatIsLeft;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Test driver of the purse: the story as a container with one purse, opened before the trades, and the
/// scenario standing in for the tea party, publishing the transactions itself. What the purse says about its
/// owner's trades is read from its log, one line per trade, as `KnowWhatIsLeft` phrases them.
class PurseTable {

    private static final String SITS_FOR = "120";
    private static final Duration HEARING = Duration.ofSeconds(60);
    private static final BigDecimal SOLD_FOR = new BigDecimal("20.25");
    private static final BigDecimal BOUGHT_FOR = new BigDecimal("10.5");
    private static final double OTHERS_TRADE = 99;

    /// The owner's lines: who, the price, and the balance or the shortfall, numbers as `BigDecimal` prints them.
    private static final Pattern SOLD = Pattern.compile("(\\S+) sold a thing for (\\S+) gold coins, the purse jingles with (\\S+)");
    private static final Pattern BOUGHT = Pattern.compile("(\\S+) bought a thing for (\\S+) gold coins, (\\S+) left in the purse");
    private static final Pattern OWES = Pattern.compile("(\\S+) bought a thing for (\\S+) gold coins, but the coins went down a deep rabbit hole: (\\S+) short");
    private static final Pattern ANY_TRADE = Pattern.compile("(\\S+) (?:sold|bought) a thing for");

    private final StoryContainer story;
    private final Cluster cluster;
    private String owner = "";
    private BigDecimal coins = BigDecimal.ZERO;
    private final List<Transaction> trades = new ArrayList<>();

    PurseTable(StoryContainer story, Cluster cluster) {
        this.story = story;
        this.cluster = cluster;
    }

    /// Opens the owner's purse with the coins: the story starts and listens before any trade is settled.
    void open(String name, String ownerId, int coins) {
        this.owner = ownerId;
        this.coins = BigDecimal.valueOf(coins);
        var table = new KnowWhatIsLeft(Map.of(ownerId, this.coins), null);
        cluster.forget(table.group());
        story.start(KnowWhatIsLeft.NAME, Map.of("purse.owners", name + ":" + coins, "purse.ttl", SITS_FOR));
    }

    /// The owner sold, the owner bought, and two others traded between themselves.
    void settledAroundTheOwner() {
        var others = Stream.of(MAD_HATTER, MARCH_HARE, DORMOUSE).filter(other -> !other.equals(owner)).toList();
        settle(trade(others.get(0), owner, SOLD_FOR.doubleValue()));
        settle(trade(owner, others.get(0), BOUGHT_FOR.doubleValue()));
        settle(trade(others.get(0), others.get(1), OTHERS_TRADE));
    }

    /// The owner bought for one coin more than the purse holds.
    void settledBeyondTheOwner() {
        settle(trade(owner, MAD_HATTER, coins.add(BigDecimal.ONE).doubleValue()));
    }

    /// Waits until the purse has said something about each of the owner's trades.
    void reads() {
        var ownersTrades = (int) trades.stream()
                .filter(trade -> trade.customerId().equals(owner) || trade.sellerId().equals(owner))
                .count();
        story.awaitLines(Pattern.compile(Pattern.quote(owner) + " (?:sold|bought) a thing for"), ownersTrades, HEARING);
    }

    void assertPutInWhatTheOwnerSold() {
        var line = lastLine(SOLD);
        assertThat(line.group(1)).isEqualTo(owner);
        assertThat(new BigDecimal(line.group(2))).isEqualByComparingTo(SOLD_FOR);
        assertThat(new BigDecimal(line.group(3))).isEqualByComparingTo(coins.add(SOLD_FOR));
    }

    void assertTookOutWhatTheOwnerBought() {
        var line = lastLine(BOUGHT);
        assertThat(line.group(1)).isEqualTo(owner);
        assertThat(new BigDecimal(line.group(2))).isEqualByComparingTo(BOUGHT_FOR);
    }

    void assertNothingForTheOthers() {
        var speakers = ANY_TRADE.matcher(story.logs()).results().map(line -> line.group(1)).toList();
        assertThat(speakers).containsOnly(owner);
    }

    void assertReportsWhatIsLeft() {
        var line = lastLine(BOUGHT);
        assertThat(new BigDecimal(line.group(3))).isEqualByComparingTo(coins.add(SOLD_FOR).subtract(BOUGHT_FOR));
    }

    void assertEmpty() {
        assertThat(OWES.matcher(story.logs()).find()).as("a line saying the coins are gone").isTrue();
    }

    void assertReportsWhatTheOwnerOwes(String ownerId) {
        var line = lastLine(OWES);
        assertThat(line.group(1)).isEqualTo(ownerId);
        assertThat(new BigDecimal(line.group(3))).isEqualByComparingTo(BigDecimal.ONE);
    }

    /// The shortfall is logged at ERROR, the level a human looks at.
    void assertSaysSoWhereAHumanLooks() {
        var lines = story.logs().lines().filter(line -> OWES.matcher(line).find()).toList();
        assertThat(lines).isNotEmpty().allSatisfy(line -> assertThat(line).contains("ERROR"));
    }

    private void settle(Transaction trade) {
        trades.add(trade);
        cluster.publish(Envelope.of(ThreadLocalRandom.current(), "EMEA", "bdd", Instant.now(), trade));
    }

    private MatchResult lastLine(Pattern line) {
        var matches = line.matcher(story.logs()).results().toList();
        assertThat(matches).as("a line matching %s", line).isNotEmpty();
        return matches.getLast();
    }
}
