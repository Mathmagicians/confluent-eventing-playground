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
    /// ERROR when the coins are gone and the owner shops on, DEBUG for a trade that is none of its business.
    static final String SOLD = "{} sold a thing for {} gold coins, the purse jingles with {}";
    static final String BOUGHT = "{} bought a thing for {} gold coins, {} left in the purse";
    static final String OWES = "{} bought a thing for {} gold coins, but the coins went down a deep rabbit hole: {} short, and the shopping goes on";
    static final String PASSED_BY = "{} paid {} {} gold coins, none of this table's business";

    // FIXME a domain record Purse(ownerId, BigDecimal balance): in(price) and out(price), pure functions answering
    // FIXME a new purse; this map then holds a Purse per owner id, and balance() below reads the purse's
    private final TreeMap<String, BigDecimal> purses = new TreeMap<>();
    private final @Nullable Duration ttl;

    /// The purses at this table, by owner id, with the coins each opens with, and how long the table sits, nothing
    /// for until stopped.
    public KnowWhatIsLeft(Map<String, BigDecimal> openings, @Nullable Duration ttl) {
        purses.putAll(openings);
        this.ttl = ttl;
    }

    @Override
    public String name() {
        return NAME;
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
        // FIXME the price is money here, BigDecimal.valueOf(transaction.price()), the wire's double stays outside
        // FIXME the seller's purse, when at this table: price in, log.info(SOLD, seller, price, balance)
        // FIXME the customer's purse, when at this table: price out, log.info(BOUGHT, customer, price, balance);
        // FIXME a balance below zero is what the owner owes, log.error(OWES, customer, price, balance.negate())
        // FIXME a trade between two others: nothing but the line below
        log.debug(PASSED_BY, transaction.customerId(), transaction.sellerId(), transaction.price());
    }

    /// The balance of the owner's purse, what is left, or what the owner owes when it is negative; nobody's purse
    /// is a mistake.
    public BigDecimal balance(String ownerId) {
        var balance = purses.get(ownerId);
        if (balance == null) {
            throw new IllegalArgumentException(
                    ownerId + " has no purse at this table, only " + purses.keySet() + " do; off with their head");
        }
        return balance;
    }
}
