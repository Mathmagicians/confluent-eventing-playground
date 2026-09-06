package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.kafka.core.KafkaAdmin;

/// Test driver for the Confluent test cluster: one admin client from the `kafka` profile, for the life of the
/// suite's context.
class Cluster {

    /// Kafka's own request timeout; the first call pays for DNS, TLS, and SASL
    private static final long TIMEOUT_SECONDS = 30;

    private final AdminClient client;

    Cluster(KafkaAdmin admin) {
        this.client = AdminClient.create(admin.getConfigurationProperties());
    }

    /// The API keys open the cluster: the cheapest authenticated call answers with the cluster id.
    void assertReachable() {
        assertThat(id()).isNotBlank();
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private String id() {
        try {
            return client.describeCluster().clusterId().get(TIMEOUT_SECONDS, SECONDS);
        } catch (ExecutionException e) {
            throw new IllegalStateException("the cluster refused: " + e.getCause().getMessage(), e);
        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "the cluster did not answer within " + TIMEOUT_SECONDS + " s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while describing the cluster", e);
        }
    }
}
