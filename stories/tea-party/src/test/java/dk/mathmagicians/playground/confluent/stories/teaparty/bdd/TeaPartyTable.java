package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.Published;
import dk.mathmagicians.playground.confluent.eventing.bdd.Region;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import dk.mathmagicians.playground.confluent.stories.teaparty.SettleAtTheTeaParty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/// Test driver of the tea party's outcome: the orders the scenario placed are the oracle, the transactions
/// topic the result, an order settled when a transaction names it.
class TeaPartyTable {

    /// the party sits long enough for a scenario; the container is stopped when the scenario ends
    private static final String SITS_FOR = "180";

    private final StoryContainer story;
    private final Cluster cluster;
    private final Published published;
    private final Region region;

    TeaPartyTable(StoryContainer story, Cluster cluster, Published published, Region region) {
        this.story = story;
        this.cluster = cluster;
        this.published = published;
        this.region = region;
    }

    /// A tea party in the scenario's region, at the table before anything is placed.
    void sit(String word) {
        var settings = SettleAtTheTeaParty.NAME + ".";
        story.start(SettleAtTheTeaParty.NAME,
                Map.of(settings + "region", region.of(word), settings + "ttl", SITS_FOR));
    }

    void assertEveryOrderSettled() {
        var settled = settled();
        for (var order : orders()) {
            assertThat(settled).filteredOn(transaction -> transaction.orderRef().id().equals(order.id()))
                    .as("transactions settling %s", order.id())
                    .hasSize(1);
        }
    }

    void assertPaid(String customer, String seller, BigDecimal price, String thing) {
        assertThat(settled())
                .as("%s paid %s %s for %s", customer, seller, price, thing)
                .anySatisfy(transaction -> {
                    assertThat(transaction.customerId()).isEqualTo(customer);
                    assertThat(transaction.sellerId()).isEqualTo(seller);
                    assertThat(BigDecimal.valueOf(transaction.price())).isEqualByComparingTo(price);
                    assertThat(transaction.orderRef().productId()).isEqualTo(thing);
                });
    }

    void assertStillWaiting(String customer, String thing) {
        assertThat(settled())
                .as("%s's order for %s", customer, thing)
                .noneMatch(transaction -> transaction.customerId().equals(customer)
                        && transaction.orderRef().productId().equals(thing));
    }

    void assertNothingSettled() {
        assertThat(settled()).as("transactions settling this scenario's orders").isEmpty();
    }

    /// The transactions on the topic that settle this scenario's orders.
    private List<Transaction> settled() {
        var ours = orders().stream().map(Order::id).toList();
        return cluster.transactions().stream()
                .filter(transaction -> ours.contains(transaction.orderRef().id()))
                .toList();
    }

    private List<Order> orders() {
        return published.payloads().stream()
                .filter(Order.class::isInstance)
                .map(Order.class::cast)
                .toList();
    }
}
