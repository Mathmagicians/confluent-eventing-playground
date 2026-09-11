package dk.mathmagicians.playground.confluent.stories.purse.bdd;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/// Steps of "Alice's purse knows what is left". Every step is pending until the purse exists. Owners are
/// Wonderland characters, see `ParameterTypes.wonderlander`.
public class PurseSteps {

    @Given("{wonderlander}'s purse holds {int} coins")
    public void ownersPurseHoldsCoins(String owner, int coins) {
        throw new PendingException();
    }

    @Given("the tea party has settled orders from {wonderlander}, offers by {wonderlander}, and trades between others")
    public void theTeaPartyHasSettledOrdersFromOffersByAndTradesBetweenOthers(String customer, String seller) {
        throw new PendingException();
    }

    @Given("the tea party has settled orders from {wonderlander} worth more than that")
    public void theTeaPartyHasSettledOrdersFromWorthMoreThanThat(String customer) {
        throw new PendingException();
    }

    @When("the purse reads the stream of transactions")
    public void thePurseReadsTheStreamOfTransactions() {
        throw new PendingException();
    }

    @Then("it put in the price of every transaction where {wonderlander} is the seller")
    public void itPutInThePriceOfEveryTransactionWhereIsTheSeller(String seller) {
        throw new PendingException();
    }

    @Then("took out the price of every transaction where {wonderlander} is the customer")
    public void tookOutThePriceOfEveryTransactionWhereIsTheCustomer(String customer) {
        throw new PendingException();
    }

    @Then("nothing for the others")
    public void nothingForTheOthers() {
        throw new PendingException();
    }

    @Then("it reports what is left")
    public void itReportsWhatIsLeft() {
        throw new PendingException();
    }

    @Then("the purse is empty")
    public void thePurseIsEmpty() {
        throw new PendingException();
    }

    @Then("it reports what {wonderlander} owes")
    public void itReportsWhatOwes(String owner) {
        throw new PendingException();
    }

    @Then("it says so where a human will look")
    public void itSaysSoWhereAHumanWillLook() {
        throw new PendingException();
    }
}
