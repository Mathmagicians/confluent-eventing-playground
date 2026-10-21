package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.Given;

/// Steps about the transport, shared by every feature. Each one delegates to the suite's `Transport`.
public class TransportSteps {

    private final Transport transport;

    public TransportSteps(Transport transport) {
        this.transport = transport;
    }

    @Given("I have the API keys to the cluster")
    public void iHaveTheApiKeysToTheCluster() {
        transport.assertOpen();
    }
}
