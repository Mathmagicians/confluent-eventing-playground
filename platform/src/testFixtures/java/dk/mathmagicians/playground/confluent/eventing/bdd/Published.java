package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import io.cucumber.spring.ScenarioScope;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/// What the scenario published, by a generator or by hand, receipt by receipt and payload by payload, in the
/// order it landed, and where: one instance per scenario, shared by the step classes that publish and the ones
/// that look for the outcome.
@Component
@ScenarioScope
public class Published {

    private final List<Receipt> receipts = new ArrayList<>();
    private final List<Payload> payloads = new ArrayList<>();
    private String region;

    public void add(String region, List<Receipt> receipts, List<Payload> payloads) {
        this.region = region;
        this.receipts.addAll(receipts);
        this.payloads.addAll(payloads);
    }

    public String region() {
        return region;
    }

    public List<Receipt> receipts() {
        return List.copyOf(receipts);
    }

    public List<Payload> payloads() {
        return List.copyOf(payloads);
    }
}
