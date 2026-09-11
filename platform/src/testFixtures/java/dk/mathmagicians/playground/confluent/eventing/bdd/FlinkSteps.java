package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import dk.mathmagicians.playground.eventing.ProductDTO;
import io.cucumber.java.en.Then;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;

/// Steps about the Flink cluster in the cloud and the tables our Terraform declared on it. What the generator
/// published, in `Published`, is the oracle: the rows must say what was actually sent.
public class FlinkSteps {

    /// a materialized table catches up with a few records well within this
    private static final Duration CATCHES_UP = Duration.ofMinutes(3);
    private static final Duration LOOK_AGAIN = Duration.ofSeconds(10);

    private final FlinkQuery flink;
    private final Cluster cluster;
    private final Published published;
    private final Environment environment;

    public FlinkSteps(FlinkQuery flink, Cluster cluster, Published published, Environment environment) {
        this.flink = flink;
        this.cluster = cluster;
        this.published = published;
        this.environment = environment;
    }

    @Then("the Flink cluster in the cloud runs my table {word}")
    public void theFlinkClusterInTheCloudRunsMyTable(String table) {
        flink.assertRunning(table(table));
    }

    @Then("the Flink cluster in the cloud shows each product once, in its latest version, with its versions counted")
    public void theFlinkClusterInTheCloudShowsEachProductOnceInItsLatestVersionWithItsVersionsCounted() {
        var products = published.receipts().stream()
                .filter(receipt -> receipt.topic().equals(environment.getRequiredProperty("topics.products")))
                .map(receipt -> cluster.value(
                        cluster.read(receipt.topic(), receipt.partition(), receipt.offset()), ProductDTO.Product.class))
                .toList();
        assertThat(products).as("products the generator published").isNotEmpty();
        var latest = products.stream()
                .collect(toMap(ProductDTO.Product::getProductId, product -> product, (_, later) -> later, LinkedHashMap::new));
        var versions = products.stream().collect(groupingBy(ProductDTO.Product::getProductId, counting()));
        var table = table("product_catalog");
        var sql = ("SELECT product_id, product_name, producer_id, product_description, versions FROM `%s` "
                + "WHERE product_id IN (%s) LIMIT %d")
                .formatted(
                        table,
                        latest.keySet().stream().map(id -> "'" + id + "'").collect(joining(", ")),
                        latest.size());

        await().atMost(CATCHES_UP).pollInterval(LOOK_AGAIN).untilAsserted(() -> {
            var rows = flink.rows(table, sql).stream().collect(toMap(row -> row.get("product_id"), row -> row));

            assertThat(rows.keySet()).containsExactlyInAnyOrderElementsOf(latest.keySet());
            latest.forEach((id, product) -> {
                var row = rows.get(id);
                assertThat(row)
                        .containsEntry("product_name", product.getProductName())
                        .containsEntry("producer_id", product.getProducerId())
                        .containsEntry("product_description", product.getProductDescription());
                assertThat(Long.parseLong(row.get("versions"))).as("versions of %s", id)
                        .isGreaterThanOrEqualTo(versions.get(id));
            });
        });
    }

    /// a table set up with IaC carries the environment prefix of the topics, `test.product_catalog`
    private String table(String name) {
        return environment.getRequiredProperty("topics.products").split("\\.")[0] + "." + name;
    }
}
