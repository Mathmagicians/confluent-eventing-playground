package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import dk.mathmagicians.playground.eventing.ProductDTO;
import io.cucumber.java.en.Then;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.core.env.Environment;

/// Steps about the tables the Flink cluster in the cloud keeps, set up with IaC. A table is a topic, so its rows
/// are read like any other records, the last one per key being the row. What the generator published, in
/// `Published`, is the oracle: the rows must say what was actually sent.
public class FlinkSteps {

    /// a materialized table catches up with a few records well within this
    private static final Duration CATCHES_UP = Duration.ofMinutes(3);
    private static final Duration LOOK_AGAIN = Duration.ofSeconds(10);

    private final Cluster cluster;
    private final Published published;
    private final Environment environment;

    public FlinkSteps(Cluster cluster, Published published, Environment environment) {
        this.cluster = cluster;
        this.published = published;
        this.environment = environment;
    }

    @Then("the Flink cluster in the cloud shows in table {word} each product of region {word} once, in its latest version, with its versions counted")
    public void theFlinkClusterInTheCloudShowsInTableEachProductOfRegionOnce(String table, String region) {
        var products = published.receipts().stream()
                .filter(receipt -> receipt.topic().equals(environment.getRequiredProperty("topics.products")))
                .map(receipt -> cluster.value(
                        cluster.read(receipt.topic(), receipt.partition(), receipt.offset()), ProductDTO.Product.class))
                .toList();
        assertThat(products).as("products the generator published").isNotEmpty();
        var latest = products.stream()
                .collect(toMap(ProductDTO.Product::getProductId, product -> product, (_, later) -> later, LinkedHashMap::new));
        var versions = products.stream().collect(groupingBy(ProductDTO.Product::getProductId, counting()));
        var topic = table(table);

        await().atMost(CATCHES_UP).pollInterval(LOOK_AGAIN).untilAsserted(() -> {
            var rows = cluster.readAll(topic).stream()
                    .filter(record -> record.value() != null)
                    .map(cluster::value)
                    .filter(row -> text(row, "region").equals(region))
                    .collect(toMap(row -> text(row, "product_id"), row -> row, (_, later) -> later));

            assertThat(rows.keySet()).as("products of %s in %s", region, topic).containsAll(latest.keySet());
            latest.forEach((id, product) -> {
                var row = rows.get(id);
                assertThat(text(row, "product_name")).isEqualTo(product.getProductName());
                assertThat(text(row, "producer_id")).isEqualTo(product.getProducerId());
                // the table's column is product_descriptions, the query's alias
                assertThat(text(row, "product_descriptions")).isEqualTo(product.getProductDescription());
                assertThat(Long.parseLong(text(row, "versions"))).as("versions of %s", id)
                        .isGreaterThanOrEqualTo(versions.get(id));
            });
        });
    }

    /// a table set up with IaC carries the environment prefix of the topics: `test.products.lvs`
    private String table(String name) {
        return environment.getRequiredProperty("topics.products").split("\\.")[0] + "." + name;
    }

    /// a column of a row as text; a nullable column travels in a wrapper message whose one field is `value`
    private static String text(DynamicMessage row, String column) {
        var field = row.getDescriptorForType().findFieldByName(column);
        assertThat(field)
                .as("column %s among %s", column, row.getDescriptorForType().getFields().stream().map(FieldDescriptor::getName).toList())
                .isNotNull();
        var value = row.getField(field);
        if (value instanceof DynamicMessage wrapped) {
            var inner = wrapped.getDescriptorForType().findFieldByName("value");
            value = inner == null ? wrapped : wrapped.getField(inner);
        }
        return Objects.toString(value);
    }
}
