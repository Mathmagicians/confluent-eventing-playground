package dk.mathmagicians.playground.confluent.eventing.bdd;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Test driver for the tables our Terraform set up on the Flink cluster in the cloud, through Confluent's
/// statements API: whether a table's statement runs, and the rows a bounded query over it answers. The query runs
/// where the table's statement runs: same pool, same principal, same catalog and database, read off that
/// statement. What the driver reads from the environment is what `make flink-statements` reads: the Flink key
/// pair, the environment id, and the Cloud key pair for two lookups, the organization and the key's region.
class FlinkQuery {

    private static final Logger log = LoggerFactory.getLogger(FlinkQuery.class);

    private static final String MANAGEMENT = "https://api.confluent.cloud";
    private static final String QUERY = "bdd-";
    private static final Duration POLL = Duration.ofSeconds(2);
    /// a bounded query on a small table: submission, scheduling on the pool, and the rows
    private static final Duration TIMEOUT = Duration.ofSeconds(90);
    /// the changelog operations of a result row: insert and update-after add it, update-before and delete take it
    private static final int INSERT = 0;
    private static final int UPDATE_AFTER = 2;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
    private final String environmentId = required("ENVIRONMENT_ID");
    private final String flinkKey = required("FLINK_API_KEY");
    private final String flinkAuth = basic(flinkKey, required("FLINK_API_SECRET"));
    private final String cloudAuth = basic(required("CLOUD_API_KEY"), required("CLOUD_API_SECRET"));

    private String statements;

    /// The statement that set the table up is on the pool and has not failed or stopped.
    void assertRunning(String table) {
        assertThat(statementOf(table).path("status").path("phase").asText())
                .as("%s: %s", table, statementOf(table).path("status").path("detail").asText())
                .isIn("PENDING", "RUNNING", "COMPLETED");
    }

    /// The rows a bounded query over the table answers, a `SELECT ... LIMIT n`, each row as column name to value
    /// as text. The query statement is deleted afterwards.
    List<Map<String, String>> rows(String table, String sql) {
        var name = QUERY + UUID.randomUUID();
        var session = statementOf(table).path("spec");
        var body = json.createObjectNode();
        body.put("name", name);
        var spec = body.putObject("spec");
        spec.put("statement", sql)
                .put("compute_pool_id", session.path("compute_pool_id").asText())
                .put("principal", session.path("principal").asText());
        spec.set("properties", session.path("properties"));
        send(HttpRequest.newBuilder(URI.create(statements())).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())), flinkAuth);
        try {
            return collect(name);
        } finally {
            send(HttpRequest.newBuilder(URI.create(statements() + "/" + name)).DELETE(), flinkAuth);
        }
    }

    private List<Map<String, String>> collect(String name) {
        var deadline = Instant.now().plus(TIMEOUT);
        var rows = new ArrayList<List<String>>();
        List<String> columns = null;
        var next = statements() + "/" + name + "/results";
        while (Instant.now().isBefore(deadline)) {
            var statement = get(statements() + "/" + name, flinkAuth);
            var phase = statement.path("status").path("phase").asText();
            if (phase.equals("FAILED")) {
                throw new IllegalStateException(name + " failed: " + statement.path("status").path("detail").asText());
            }
            if (columns == null && statement.path("status").path("traits").has("schema")) {
                columns = columns(statement);
            }
            var page = get(next, flinkAuth);
            for (var change : page.path("results").path("data")) {
                var row = new ArrayList<String>();
                change.path("row").forEach(value -> row.add(value.asText()));
                if (change.path("op").asInt() == INSERT || change.path("op").asInt() == UPDATE_AFTER) {
                    rows.add(row);
                } else {
                    rows.remove(row);
                }
            }
            var more = page.path("metadata").path("next").asText("");
            if (phase.equals("COMPLETED") && more.isEmpty()) {
                return named(columns, rows);
            }
            if (!more.isEmpty()) {
                next = more;
            }
            sleep();
        }
        throw new IllegalStateException(name + " did not complete within " + TIMEOUT);
    }

    /// the column names of a statement's result, known once the statement is scheduled
    private static List<String> columns(JsonNode statement) {
        var columns = new ArrayList<String>();
        statement.path("status").path("traits").path("schema").path("columns")
                .forEach(column -> columns.add(column.path("name").asText()));
        return columns;
    }

    private static List<Map<String, String>> named(List<String> columns, List<List<String>> rows) {
        assertThat(columns).as("the result schema").isNotNull();
        return rows.stream().map(row -> {
            var named = new LinkedHashMap<String, String>();
            for (var i = 0; i < columns.size(); i++) {
                named.put(columns.get(i), row.get(i));
            }
            return (Map<String, String>) named;
        }).toList();
    }

    /// the statement whose SQL names the table, other than a query of ours
    private JsonNode statementOf(String table) {
        var found = new ArrayList<JsonNode>();
        get(statements(), flinkAuth).path("data").forEach(found::add);
        var statement = found.stream()
                .filter(each -> !each.path("name").asText().startsWith(QUERY))
                .filter(each -> each.path("spec").path("statement").asText().contains("`" + table + "`"))
                .findFirst();
        assertThat(statement)
                .as("a statement for %s among %s", table, found.stream().map(each -> each.path("name").asText()).toList())
                .isPresent();
        return statement.get();
    }

    /// `<the Flink key's region endpoint>/sql/v1/organizations/<org>/environments/<env>/statements`
    private String statements() {
        if (statements == null) {
            var organization = get(MANAGEMENT + "/org/v2/organizations", cloudAuth).path("data").get(0).path("id").asText();
            var region = get(MANAGEMENT + "/iam/v2/api-keys/" + flinkKey, cloudAuth).path("spec").path("resource").path("id").asText();
            var endpoint = "";
            for (var each : get(MANAGEMENT + "/fcpm/v2/regions?cloud=" + region.split("\\.")[0].toUpperCase(), cloudAuth).path("data")) {
                if (each.path("id").asText().equals(region)) {
                    endpoint = each.path("http_endpoint").asText();
                }
            }
            assertThat(endpoint).as("the endpoint of the Flink key's region %s", region).isNotEmpty();
            statements = endpoint + "/sql/v1/organizations/" + organization + "/environments/" + environmentId + "/statements";
        }
        return statements;
    }

    private JsonNode get(String url, String auth) {
        return send(HttpRequest.newBuilder(URI.create(url)).GET(), auth);
    }

    private JsonNode send(HttpRequest.Builder builder, String auth) {
        var request = builder.header("Authorization", auth).build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException(
                        request.method() + " " + request.uri() + " answered " + response.statusCode() + ": " + response.body());
            }
            log.debug("{} {} answered {}", request.method(), request.uri(), response.statusCode());
            return response.body().isEmpty() ? json.nullNode() : json.readTree(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException(request.uri() + " did not answer", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while calling " + request.uri(), e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for a statement", e);
        }
    }

    private static String basic(String key, String secret) {
        return "Basic " + Base64.getEncoder().encodeToString((key + ":" + secret).getBytes(UTF_8));
    }

    private static String required(String name) {
        var value = System.getenv(name);
        assertThat(value).as("%s, from .env.<ENV>.private", name).isNotBlank();
        return value;
    }
}
