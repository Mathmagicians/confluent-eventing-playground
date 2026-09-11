package dk.mathmagicians.playground.confluent.eventing.bdd;

import dk.mathmagicians.playground.confluent.eventing.domain.Receipt;
import io.cucumber.spring.ScenarioScope;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/// What the generator published in this scenario, receipt by receipt, and where: one instance per scenario, shared
/// by the step classes that publish and the ones that look for the outcome.
@Component
@ScenarioScope
public class Published {

    private final List<Receipt> receipts = new ArrayList<>();
    private String region;

    void add(String region, List<Receipt> more) {
        this.region = region;
        receipts.addAll(more);
    }

    String region() {
        return region;
    }

    List<Receipt> receipts() {
        return List.copyOf(receipts);
    }
}
