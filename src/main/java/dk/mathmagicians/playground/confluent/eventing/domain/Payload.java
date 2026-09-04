package dk.mathmagicians.playground.confluent.eventing.domain;

/// The closed set of payload types. A switch over it is exhaustive.
public sealed interface Payload permits Product, Offer, Order, Transaction {
}
