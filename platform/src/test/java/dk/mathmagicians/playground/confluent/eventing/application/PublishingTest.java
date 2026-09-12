package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.publishing;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublishingTest {

    private final List<Envelope> published = new ArrayList<>();
    private final Publishing publishing = publishing(published, EventFixtures::dice);

    @Test
    void makesTheUseCaseOfAStorysRegionFromWhatThePlatformWiredOnce() {
        publishing.from("APAC").publish(order());

        assertThat(published).singleElement().satisfies(envelope -> {
            assertThat(envelope.region()).isEqualTo("APAC");
            assertThat(envelope.app()).isEqualTo(APP);
            assertThat(envelope.at()).isEqualTo(AT);
        });
    }

    @Test
    void makesTheUseCaseOfThePlatformsRegionForAStoryWithoutOne() {
        publishing.from(null).publish(order());

        assertThat(published).singleElement().extracting(Envelope::region).isEqualTo(REGION);
    }

    @Test
    void stampsAnEnvelopeAsAPublisherOfTheRegionWould() {
        var order = order();

        var envelope = publishing.envelope("AMER", order);

        assertThat(envelope).isEqualTo(new Envelope(Envelope.id(dice()), "AMER", APP, AT, order));
        assertThat(published).isEmpty();
    }
}
