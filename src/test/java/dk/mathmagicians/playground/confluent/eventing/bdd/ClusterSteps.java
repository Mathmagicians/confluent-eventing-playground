package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.Given;

/// Steps about the cluster, shared by every feature. Each one delegates to the `Cluster` driver.
public class ClusterSteps {

    private final Cluster cluster;

    public ClusterSteps(Cluster cluster) {
        this.cluster = cluster;
    }

    @Given("I have the API keys to the cluster")
    public void iHaveTheApiKeysToTheCluster() {
        cluster.assertReachable();
    }
}
