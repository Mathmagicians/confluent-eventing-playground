package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.GeneratorContainer;
import dk.mathmagicians.playground.confluent.eventing.bdd.Published;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.confluent.stories.teaparty.SettleAtTheTeaParty;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/// Test driver of the tea party: the story as a container reading from the end of its topics, the generator's
/// offers and orders as the scenario's oracle, and the transactions topic as the outcome. The tea party settles
/// the k-th order for a thing with the k-th offer for it, whichever came first, so the expected settlements
/// follow from what was published, in the order it was.
class TeaPartyTable {

    /// the party sits long enough for a scenario, the container is stopped when the scenario ends
    private static final String SITS_FOR = "180";

    /// image pulls aside, a settlement is seconds away
    private static final Duration SETTLING = Duration.ofSeconds(90);

    /// how long nothing has to happen before it counts as nothing
    private static final Duration GRACE = Duration.ofSeconds(10);

    /// An order and the offer it is settled with.
    record Settlement(Order order, Offer offer) {}

    private final StoryContainer story;
    private final Cluster cluster;
    private final GeneratorContainer generator;
    private final Published published;

    /// What the generator placed, read back from the topics once, in the order placed.
    private final List<Payload> placed = new ArrayList<>();

    TeaPartyTable(StoryContainer story, Cluster cluster, GeneratorContainer generator, Published published) {
        this.story = story;
        this.cluster = cluster;
        this.generator = generator;
        this.published = published;
    }

    /// A tea party in the region, at the table before anything is placed.
    void sit(String region) {
        cluster.forget(SettleAtTheTeaParty.NAME);
        story.start(SettleAtTheTeaParty.NAME, Map.of("tea-party.region", region, "tea-party.ttl", SITS_FOR));
    }

    void placed(List<String> payloads, String region) {
        var receipts = generator.publish(payloads, region);
        published.add(region, receipts);
        receipts.stream().map(cluster::payload).forEach(placed::add);
    }

    void placedMore(List<String> payloads) {
        placed(payloads, published.region());
    }

    /// Waits for the settlements that follow from what was placed; when none follows, for the grace period.
    void reads() {
        var expected = expected();
        if (expected.isEmpty()) {
            await().pollDelay(GRACE).atMost(GRACE.plusSeconds(1)).until(() -> true);
            return;
        }
        var orders = expected.stream().map(settlement -> settlement.order().id()).collect(Collectors.toSet());
        await().atMost(SETTLING)
                .pollInterval(Duration.ofSeconds(5))
                .pollInSameThread()
                .until(() -> settled().keySet().containsAll(orders));
    }

    void assertEveryOrderOnOfferSettledOnce() {
        var settled = settled();
        for (var expected : expected()) {
            assertThat(settled.get(expected.order().id()))
                    .as("transactions settling %s", expected.order().id())
                    .singleElement()
                    .satisfies(transaction ->
                            assertThat(transaction.offerRef().id()).isEqualTo(expected.offer().id()));
        }
    }

    void assertTransactionsNameTheParties() {
        var settled = settled();
        for (var expected : expected()) {
            var transaction = settled.get(expected.order().id()).getFirst();
            assertThat(transaction.customerId()).isEqualTo(expected.order().customerId());
            assertThat(transaction.sellerId()).isEqualTo(expected.offer().sellerId());
            assertThat(transaction.price()).isEqualTo(expected.offer().price());
        }
    }

    void assertEachOfferTakenOnceByTheLongestWaiting() {
        var offers = settled().values().stream()
                .flatMap(List::stream)
                .map(transaction -> transaction.offerRef().id())
                .toList();
        assertThat(offers).doesNotHaveDuplicates();
        assertEveryOrderOnOfferSettledOnce();
    }

    void assertNothingSettled() {
        assertThat(settled()).as("transactions settling this scenario's orders").isEmpty();
    }

    /// The settlements the rule promises for what was placed: per thing, the orders and the offers in the order
    /// placed, paired first with first.
    private List<Settlement> expected() {
        var offers = new HashMap<String, Deque<Offer>>();
        var orders = new HashMap<String, Deque<Order>>();
        for (var payload : placed) {
            switch (payload) {
                case Offer offer -> offers.computeIfAbsent(offer.productId(), _ -> new ArrayDeque<>()).add(offer);
                case Order order -> orders.computeIfAbsent(order.productId(), _ -> new ArrayDeque<>()).add(order);
                default -> {}
            }
        }
        var settlements = new ArrayList<Settlement>();
        orders.forEach((thing, waiting) -> {
            var onOffer = offers.getOrDefault(thing, new ArrayDeque<>());
            while (!waiting.isEmpty() && !onOffer.isEmpty()) {
                settlements.add(new Settlement(waiting.poll(), onOffer.poll()));
            }
        });
        return settlements;
    }

    /// The transactions on the topic that settle this scenario's orders, by order id.
    private Map<String, List<Transaction>> settled() {
        var ours = orderIds();
        return cluster.transactions().stream()
                .filter(transaction -> ours.contains(transaction.orderRef().id()))
                .collect(Collectors.groupingBy(transaction -> transaction.orderRef().id()));
    }

    private Set<String> orderIds() {
        return placed.stream()
                .filter(Order.class::isInstance)
                .map(payload -> ((Order) payload).id())
                .collect(Collectors.toSet());
    }
}
