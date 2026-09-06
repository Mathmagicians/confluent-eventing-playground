package dk.mathmagicians.playground.confluent.eventing.kafka;

public final class KafkaFixtures {

    /// The test environment's topics, named as `iac/topics.tf` creates them.
    public static Topics topics() {
        return new Topics("test.products", "test.offers", "test.orders", "test.transactions");
    }

    private KafkaFixtures() {
    }
}
