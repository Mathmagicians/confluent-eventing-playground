package dk.mathmagicians.playground.confluent.eventing.bdd;

import io.cucumber.java.en.When;
import java.util.List;

/// Steps of a story at the table, shared by every story's feature: "the tea party reads the streams of offers and
/// orders", "the purse reads the stream of transactions". A story is started by its own Given, in its own words;
/// reading is the platform's: the story's group has caught up with the topics named.
public class StorySteps {

    private final StoryContainer story;

    public StorySteps(StoryContainer story) {
        this.story = story;
    }

    @When("the {story} reads the stream(s) of {payloads}")
    public void theStoryReadsTheStreamsOf(String name, List<String> payloads) {
        story.assertRunning(name);
        story.awaitCaughtUp(payloads);
    }
}
