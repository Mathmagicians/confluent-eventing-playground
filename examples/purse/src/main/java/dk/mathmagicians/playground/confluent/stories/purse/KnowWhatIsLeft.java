package dk.mathmagicians.playground.confluent.stories.purse;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The purse story, `alices_purse_knows_what_is_left.feature`. One table holds one purse per owner. An owner's
/// sales are keyed by whoever bought, so a table reads the whole stream: its consumer group is its own, the
/// story's name and the owners, one instance per table.
@PrimaryPort
public final class KnowWhatIsLeft implements Story {

    public static final String NAME = "purse";

    private static final Logger log = LoggerFactory.getLogger(KnowWhatIsLeft.class);

    /// What the purse says, one line per trade of an owner: owner, price, balance. INFO when coins come or go,
    /// WARN when the coins are gone and the owner shops on, DEBUG for a trade that is none of its business.
    static final String PASSED_BY = "{} paid {} {} gold coins, none of this table's business";

    public record Purse(String ownerId, BigDecimal balance) {
        public static final String SOLD = "{} sold a fine thing for {} gold coins, the purse jingles with {}";
        public static final String BOUGHT = "{} bought a fine thing {} for {} gold coins, {} left in the purse";
        public static final String OWES = "{} bought a maybe unnecessary thing {} for {} gold coins, but the coins went down a deep rabbit hole: {} short, and the shopping goes on";

        Purse in(BigDecimal price) {
            var nextIncarnation = new Purse(ownerId, balance.add(price));
            log.info(SOLD, ownerId, price, nextIncarnation.balance());
            return nextIncarnation;
        }

        Purse out(BigDecimal price, String thing) {
            var nextIncarnation = new Purse(ownerId, balance.subtract(price));
            if (nextIncarnation.balance().compareTo(BigDecimal.ZERO) < 0) {
                log.warn(OWES, ownerId, thing, price, nextIncarnation.balance().negate());
            } else {
                log.info(BOUGHT, ownerId, thing, price, nextIncarnation.balance());
            }
            return nextIncarnation;
        }

    }

    private final Map<String, Purse> purses = new TreeMap<>();
    private final @Nullable String region;
    private final @Nullable Duration ttl;

    /// The purses at this table, by owner id, with the coins each opens with; the region whose trades the table
    /// hears, nothing for every region; and how long the table sits, nothing for until stopped.
    public KnowWhatIsLeft(Map<String, BigDecimal> openings, @Nullable String region, @Nullable Duration ttl) {
        openings.forEach((k, v) -> purses.put(k, new Purse(k, v)));
        this.region = region;
        this.ttl = ttl;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    @Override
    public Optional<Duration> playsFor() {
        return Optional.ofNullable(ttl);
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
        switch (envelope.payload()) {
            case Transaction transaction -> traded(transaction);
            default -> Story.super.on(envelope);
        }
    }

    private void traded(Transaction transaction) {
        var price = BigDecimal.valueOf(transaction.price());
        purses.computeIfPresent( transaction.sellerId(), (_, purse) -> purse.in(price));
        purses.computeIfPresent(transaction.customerId(), (_, purse) -> purse.out(price, transaction.orderRef().productId()));
        log.debug(PASSED_BY, transaction.customerId(), transaction.sellerId(), transaction.price());
    }

    /// The balance of the owner's purse, what is left, or what the owner owes when it is negative; nobody's purse
    /// is a mistake.
    public BigDecimal balance(String ownerId) {
        var purse = purses.get(ownerId);
        if (purse == null) {
            throw new IllegalArgumentException(
                    ownerId + " has no purse at this table, only " + purses.keySet() + " do; off with their head");
        }
        return purse.balance();
    }
}
