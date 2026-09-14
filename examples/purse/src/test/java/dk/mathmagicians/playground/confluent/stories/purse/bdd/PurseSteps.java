package dk.mathmagicians.playground.confluent.stories.purse.bdd;

import dk.mathmagicians.playground.confluent.eventing.bdd.Cluster;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import dk.mathmagicians.playground.confluent.eventing.domain.Wonderland;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/// Steps of "Alice's purse knows what is left", glue to `PurseTable`. Owners are Wonderland characters: the
/// purse is opened by the name the feature writes, and answers about the id on the wire, see
/// `ParameterTypes.wonderlander`.
public class PurseSteps {

    /// A character as the feature names one and as the wire keys one, `White Rabbit` and `WHITE_RABBIT`.
    public record Owner(String name, String id) {}

    private final PurseTable table;

    public PurseSteps(StoryContainer story, Cluster cluster) {
        this.table = new PurseTable(story, cluster);
    }

    @ParameterType("[A-Z][a-z]+(?: (?:of|[A-Z][a-z]+))*")
    public Owner owner(String name) {
        return new Owner(name, Wonderland.characterId(name));
    }

    @Given("{owner}'s purse holds {int} coins")
    public void ownersPurseHoldsCoins(Owner owner, int coins) {
        table.open(owner.name(), owner.id(), coins);
    }

    @Given("the tea party has settled orders from {wonderlander}, offers by {wonderlander}, and trades between others")
    public void theTeaPartyHasSettledOrdersFromOffersByAndTradesBetweenOthers(String customer, String seller) {
        table.settledAroundTheOwner();
    }

    @Given("the tea party has settled orders from {wonderlander} worth more than that")
    public void theTeaPartyHasSettledOrdersFromWorthMoreThanThat(String customer) {
        table.settledBeyondTheOwner();
    }

    @When("the purse reads the stream of transactions")
    public void thePurseReadsTheStreamOfTransactions() {
        table.reads();
    }

    @Then("it put in the price of every transaction where {wonderlander} is the seller")
    public void itPutInThePriceOfEveryTransactionWhereIsTheSeller(String seller) {
        table.assertPutInWhatTheOwnerSold();
    }

    @Then("took out the price of every transaction where {wonderlander} is the customer")
    public void tookOutThePriceOfEveryTransactionWhereIsTheCustomer(String customer) {
        table.assertTookOutWhatTheOwnerBought();
    }

    @Then("nothing for the others")
    public void nothingForTheOthers() {
        table.assertNothingForTheOthers();
    }

    @Then("it reports what is left")
    public void itReportsWhatIsLeft() {
        table.assertReportsWhatIsLeft();
    }

    @Then("the purse is empty")
    public void thePurseIsEmpty() {
        table.assertEmpty();
    }

    @Then("it reports what {wonderlander} owes")
    public void itReportsWhatOwes(String owner) {
        table.assertReportsWhatTheOwnerOwes(owner);
    }

    @Then("it says so where a human will look")
    public void itSaysSoWhereAHumanWillLook() {
        table.assertSaysSoWhereAHumanLooks();
    }
}
