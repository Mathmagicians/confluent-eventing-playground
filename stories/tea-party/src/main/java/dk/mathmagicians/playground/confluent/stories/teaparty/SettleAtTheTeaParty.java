package dk.mathmagicians.playground.confluent.stories.teaparty;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The tea party story, `at_the_tea_party_an_order_meets_an_offer.feature`. One instance serves one region and
/// publishes its transactions through `PublishMessage`; its consumer group is the story's name, so the instances
/// of a region share its partitions.
@PrimaryPort
public record SettleAtTheTeaParty(
        String region,
        PublishMessage publishMessage,
        Clock clock,
        Supplier<RandomGenerator> random,
        Duration ttl) implements Story {

    public static final String NAME = "tea-party";

    private static final Logger log = LoggerFactory.getLogger(SettleAtTheTeaParty.class);

    @Override
    public String name() {
        return NAME;
    }

    /// How long the party sits, from the settings; zero is until stopped.
    @Override
    public Duration ttl() {
        return ttl;
    }

    @Override
    public Set<Class<? extends Payload>> listensTo() {
        return Set.of(Offer.class, Order.class);
    }

    /// An offer updates the market price of its thing and settles what waited for it; an order settles at the
    /// market price or waits.
    @Override
    public void on(Envelope envelope) {
        switch (envelope.payload()) {
            case Offer offer -> offered(offer);
            case Order order -> ordered(order);
            default -> throw new IllegalStateException(NAME + " does not listen to " + envelope.payload());
        }
    }

    private void offered(Offer offer) {
        // FIXME the latest offer per thing, keyed by product id: a market, a record holding a map, pure functions
        // FIXME then every order waiting for the thing settles at this offer, oldest first
        log.debug("Offered {} for {} at {}", offer.productId(), offer.price(), offer.sellerId());
    }

    private void ordered(Order order) {
        // FIXME the latest offer for order.productId(): settle(order, offer), or keep the order waiting for the thing
        log.debug("Ordered {} by {}", order.productId(), order.customerId());
    }

    private void settle(Order order, Offer offer) {
        // FIXME Transaction.settle(order, offer, random.get(), clock.instant()), published through publishMessage
        // FIXME one INFO line per settlement: order id, offer id, customer, seller, price
        log.info("Settled {} with {}: {} pays {} {}", order.id(), offer.offerId(), order.customerId(),
                offer.sellerId(), offer.price());
    }
}
