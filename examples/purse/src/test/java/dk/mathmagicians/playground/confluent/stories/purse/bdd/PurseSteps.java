package dk.mathmagicians.playground.confluent.stories.purse.bdd;

import dk.mathmagicians.playground.confluent.eventing.bdd.Region;
import dk.mathmagicians.playground.confluent.eventing.bdd.StoryContainer;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.math.BigDecimal;

/// Steps of "Alice's purse knows what is left", glue to `PurseTable`: how the purse opens, and what it says. The
/// trades on the stream, the reading, and the cluster are the platform's steps. The purse opens by the name the
/// feature writes; what it says names the id on the wire, see `ParameterTypes.wonderlander`.
public class PurseSteps {

    private final PurseTable table;

    public PurseSteps(StoryContainer story, Region region) {
        this.table = new PurseTable(story, region);
    }

    /// A character as the feature names one, `White Rabbit`, kept as written: the purse's settings take names.
    @ParameterType("[A-Z][a-z]+(?: (?:of|[A-Z][a-z]+))*")
    public String owner(String name) {
        return name;
    }

    @Given("{owner}'s purse holds {int} coins")
    public void ownersPurseHoldsCoins(String owner, int coins) {
        table.open(owner, coins);
    }

    @Then("{wonderlander} has {bigdecimal} coins left")
    public void hasCoinsLeft(String owner, BigDecimal coins) {
        table.assertLeft(owner, coins);
    }

    @Then("{wonderlander} owes {bigdecimal} coins")
    public void owesCoins(String owner, BigDecimal coins) {
        table.assertOwes(owner, coins);
    }

    @Then("the purse said nothing about {wonderlander} or {wonderlander}")
    public void thePurseSaidNothingAbout(String one, String other) {
        table.assertSaidNothingAbout(one, other);
    }

    @Then("the purse makes a warning")
    public void thePurseMakesAWarning() {
        table.assertMakesAWarning();
    }
}
