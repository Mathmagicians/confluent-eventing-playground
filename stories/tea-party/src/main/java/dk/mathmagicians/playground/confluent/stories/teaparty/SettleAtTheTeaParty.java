package dk.mathmagicians.playground.confluent.stories.teaparty;

import dk.mathmagicians.playground.confluent.eventing.application.PublishMessage;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.*;

import java.time.Clock;
import java.time.Duration;
import java.util.*;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The tea party story, `at_the_tea_party_an_order_meets_an_offer.feature`. One instance serves one region and
/// publishes its transactions through `PublishMessage`; its consumer group is the story's name, so the instances
/// of a region share its partitions.
@PrimaryPort
public class SettleAtTheTeaParty implements Story {

    private final PublishMessage publishMessage;
    private final Clock clock;
    private final @Nullable Duration ttl;
    private final String teapartyName ;

    public SettleAtTheTeaParty(String region, PublishMessage publishMessage, Clock clock, @Nullable Duration ttl) {
        this.publishMessage = publishMessage;
        this.clock = clock;
        this.ttl = ttl;
        this.teapartyName = String.join(" ", region, NAME, "🫖🎉");
    }

    public static final String NAME = "tea-party";

    private static final Logger log = LoggerFactory.getLogger(SettleAtTheTeaParty.class);

    private final Map<String, Deque<Offer>> offers = new HashMap<>();
    private final Map<String, Deque<Order>> orders = new HashMap<>();

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<Duration> playsFor() {
        return Optional.ofNullable(ttl);
    }

    @Override
    public Set<Class<? extends Payload>> listensTo() {
        return Set.of(Offer.class, Order.class);
    }

    /// An offer looks for an order of that thing, an order looks for a kind offer of that thing.
    /// If they meet, they settle and live happily everafter, and a transaction is published. If not, they wait.
    @Override
    public void on(Envelope envelope) {
        switch (envelope.payload()) {
            case Offer offer -> meet(offer.productId(), orders, offers, offer)
                    .ifPresent(o -> settle(o, offer));
            case Order order -> meet(order.productId(), offers, orders, order)
                    .ifPresent(o -> settle(order, o));
            default -> Story.super.on(envelope);
        }
    }

    private static <T extends Payload, U extends Payload> Optional<U> meet(String thing, Map<String, Deque<U>> waiting, Map<String, Deque<T>> ownKind, T
            newcomer) {
        log.debug("Newcomer {} looking for  {}", newcomer.id(), thing);
        var metMyPartner = Optional.ofNullable(waiting.get(thing)) .map(Deque::pollFirst);
        if (metMyPartner.isEmpty()) {
            var queue = ownKind.computeIfAbsent(thing, k -> new LinkedList<>());
            queue.addLast(newcomer);
            log.debug("No partner for {}: Wonderlanders are waiting in queue of length {} for {}", newcomer, queue.size(), thing);
        }
        return metMyPartner;
    }


    private void settle(Order order, Offer offer) {
        var tx = Transaction.settle(order, offer, clock.instant());
        var completableFuture = publishMessage.publish(tx);
        log.info( "{}: Se-tea-tled {} with {}: {} pays {} {}", teapartyName, order.id(), offer.id(), order.customerId(),
                offer.sellerId(), offer.price());
        var receipt = completableFuture.join();
        log.debug("{}: Published transaction {} at offset {}, partition {}, market price; {},", teapartyName, tx.id(), receipt.partition(), receipt.offset(), offer.price());
    }
}
