package dk.mathmagicians.playground.confluent.eventing.bdd;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.trade;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import io.cucumber.java.en.Given;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/// Steps putting events on the streams by hand, in the scenario's region, one Wonderlander at a time: "Mad
/// Hatter offers a Top Hat for 12.5 coins", "Alice orders a Top Hat", "Mad Hatter paid Alice 20.25 coins for a
/// thing". What was placed is in `Published`, for the steps that look for the outcome. Ids carry the scenario's
/// salt, so they are the scenario's own on the wire.
public class StreamSteps {

    private final Cluster cluster;
    private final Published published;
    private final Region region;
    private int placed;

    public StreamSteps(Cluster cluster, Published published, Region region) {
        this.cluster = cluster;
        this.published = published;
        this.region = region;
    }

    @Given("{wonderlander} offers a {thing} for {bigdecimal} coins")
    public void offersAThingFor(String seller, String thing, BigDecimal price) {
        place(offer(id(Offer.class), thing, price.doubleValue(), seller));
    }

    @Given("{wonderlander} orders a {thing}")
    public void ordersAThing(String customer, String thing) {
        place(order(id(Order.class), customer, thing));
    }

    @Given("{wonderlander} paid {wonderlander} {bigdecimal} coins for a thing")
    public void paidCoinsForAThing(String customer, String seller, BigDecimal price) {
        place(trade(customer, seller, price.doubleValue()));
    }

    private void place(Payload payload) {
        var where = region.current();
        var receipt = cluster.publish(Envelope.of(ThreadLocalRandom.current(), where, APP, Instant.now(), payload));
        published.add(where, List.of(receipt), List.of(payload));
    }

    /// `OFF-3f9a2c1d-1`: the type, the scenario's salt, the count so far.
    private String id(Class<? extends Payload> type) {
        return type.getSimpleName().substring(0, 3).toUpperCase() + "-" + region.salt() + "-" + ++placed;
    }
}
