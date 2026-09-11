package dk.mathmagicians.playground.confluent.eventing.application;

import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.publishing;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublishingTest {

    private final List<Envelope> published = new ArrayList<>();

    @Test
    void makesTheUseCaseOfARegionFromWhatThePlatformWiredOnce() {
        publishing(published, EventFixtures::dice).from("APAC").publish(order());

        assertThat(published).singleElement().satisfies(envelope -> {
            assertThat(envelope.region()).isEqualTo("APAC");
            assertThat(envelope.app()).isEqualTo(APP);
            assertThat(envelope.at()).isEqualTo(AT);
        });
    }
}
