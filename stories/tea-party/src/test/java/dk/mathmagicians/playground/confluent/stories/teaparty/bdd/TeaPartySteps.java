package dk.mathmagicians.playground.confluent.stories.teaparty.bdd;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.Published;
import dk.mathmagicians.playground.confluent.eventing.bdd.Region;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.math.BigDecimal;

/// Steps of "At the tea party an order meets an offer", glue to `TeaPartyTable`: how the party sits down, and
/// what it settled. The offers and orders on the streams, the reading, and the cluster are the platform's steps.
public class TeaPartySteps {

    private final TeaPartyTable table;

    public TeaPartySteps(StoryContainer story, Cluster cluster, Published published, Region region) {
        this.table = new TeaPartyTable(story, cluster, published, region);
    }

    @Given("a tea party in region {word}")
    public void aTeaPartyInRegion(String region) {
        table.sit(region);
    }

    @Then("for every order the tea party read there is a transaction, the order was settled")
    public void forEveryOrderTheTeaPartyReadThereIsATransaction() {
        table.assertEveryOrderSettled();
    }

    @Then("{wonderlander} paid {wonderlander} {bigdecimal} coins for the {thing}")
    public void paidCoinsForTheThing(String customer, String seller, BigDecimal price, String thing) {
        table.assertPaid(customer, seller, price, thing);
    }

    @Then("{wonderlander} is still waiting for a {thing}")
    public void isStillWaitingForAThing(String customer, String thing) {
        table.assertStillWaiting(customer, thing);
    }

    @Then("nothing was settled")
    public void nothingWasSettled() {
        table.assertNothingSettled();
    }
}
