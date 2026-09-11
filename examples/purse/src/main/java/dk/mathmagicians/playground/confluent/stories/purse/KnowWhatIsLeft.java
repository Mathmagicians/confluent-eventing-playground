package dk.mathmagicians.playground.confluent.stories.purse;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The purse story, the feature's words: a purse holds coins for its owner and reads the stream of transactions.
/// When the owner sold, the price goes in; when the owner bought, it comes out; the others' trades pass by. The
/// purse knows what is left, and when it is empty and the owner shops on, it says what she owes, where a human
/// will look. One table holds one purse per owner. An owner's sales are keyed by whoever bought, so a table reads
/// the whole stream: its consumer group is its own, the story's name and the owners, one instance per table.
@PrimaryPort
public final class KnowWhatIsLeft implements Story {

    public static final String NAME = "purse";

    private static final Logger log = LoggerFactory.getLogger(KnowWhatIsLeft.class);

    // FIXME a domain record Purse(ownerId, coins): in(price), out(price), left(), isEmpty(), owes(), pure
    // FIXME functions answering a new purse; this map then holds a Purse per owner id
    private final TreeMap<String, Double> purses = new TreeMap<>();
    private final Duration ttl;

    /// The purses at this table, by owner id, with the coins each opens with, and how long the table sits.
    public KnowWhatIsLeft(Map<String, Double> openings, Duration ttl) {
        purses.putAll(openings);
        this.ttl = ttl;
    }

    @Override
    public String name() {
        return NAME;
    }

    /// How long the table sits, from the settings; zero is until stopped.
    @Override
    public Duration ttl() {
        return ttl;
    }

    /// The story's name and the owners, sorted: `purse-ALICE-WHITE_RABBIT`.
    @Override
    public String group() {
        return NAME + "-" + String.join("-", purses.keySet());
    }

    @Override
    public Set<Class<? extends Payload>> listensTo() {
        return Set.of(Transaction.class);
    }

    @Override
    public void on(Envelope envelope) {
        if (!(envelope.payload() instanceof Transaction transaction)) {
            throw new IllegalStateException(NAME + " does not listen to " + envelope.payload());
        }
        // FIXME the seller's purse, when at this table: coins in, one INFO line with the price and what is left
        // FIXME the customer's purse, when at this table: coins out, one INFO line; below zero, one ERROR line with
        // FIXME what the owner owes
        // FIXME a trade between two others: nothing, at most a DEBUG line
        log.debug("Saw {}: {} pays {} {}", transaction.transactionId(), transaction.customerId(),
                transaction.sellerId(), transaction.price());
    }

    /// What the owner's purse holds, negative when the owner owes; nobody's purse is a mistake.
    public double left(String ownerId) {
        var coins = purses.get(ownerId);
        if (coins == null) {
            throw new IllegalArgumentException(ownerId + " has no purse at this table, " + purses.keySet() + " do");
        }
        return coins;
    }
}
