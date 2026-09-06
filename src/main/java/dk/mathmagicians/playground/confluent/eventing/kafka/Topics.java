package dk.mathmagicians.playground.confluent.eventing.kafka;

import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Product;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

/// The topic per payload type. One set per environment, named in the profile's properties as `iac/topics.tf`
/// creates them.
@ConfigurationProperties("topics")
@Profile("!local")
public record Topics(String products, String offers, String orders, String transactions) {

    public Topics {
        require("topics.products", products);
        require("topics.offers", offers);
        require("topics.orders", orders);
        require("topics.transactions", transactions);
    }

    /// The topic a payload goes to.
    public String select(Payload payload) {
        return switch (payload) {
            case Product _ -> products;
            case Offer _ -> offers;
            case Order _ -> orders;
            case Transaction _ -> transactions;
        };
    }

    private static void require(String property, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(property + " is required, was '" + name + "'");
        }
    }
}
