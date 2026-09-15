package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.acting;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.listening;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.listeningFrom;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.APP;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.OTHER_REGION;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.dice;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;

@ExtendWith(MockitoExtension.class)
class StoryListenerTest {

    @Mock
    private ConcurrentKafkaListenerContainerFactory<String, byte[]> containers;

    @Mock
    private ConcurrentMessageListenerContainer<String, byte[]> container;

    private final Reader reader = mock(Reader.class);

    private StoryListener listener(Story story) {
        return listener(story, new KafkaProperties());
    }

    private StoryListener listener(Story story, KafkaProperties properties) {
        return new StoryListener(story, topics(), reader, containers, properties);
    }

    @Test
    void subscribesToTheTopicsOfWhatTheStoryListensToAsTheStorysGroup() {
        var properties = new ContainerProperties("test.offers", "test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);
        when(container.isRunning()).thenReturn(true);
        var listener = listener(listening("tea-party", Set.of(Order.class, Offer.class), null));

        listener.start();

        verify(containers).createContainer("test.offers", "test.orders");
        assertThat(properties.getGroupId()).isEqualTo("tea-party");
        verify(container).setupMessageListener(any());
        verify(container).start();
        assertThat(listener.isRunning()).isTrue();
    }

    @Test
    void subscribesUnderTheGroupTheStoryAnswers() {
        var group = "purse-" + ALICE;
        var story = new StoryFixtures.Fake(
                "purse", group, null, Set.of(Order.class), null, new AtomicInteger(), new ArrayList<>());
        var properties = new ContainerProperties("test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);

        listener(story).start();

        assertThat(properties.getGroupId()).isEqualTo(group);
    }

    /// A deployment that names the group, `spring.kafka.consumer.group-id`, has its way: a test run of its own.
    @Test
    void subscribesUnderTheGroupTheDeploymentNamesInstead() {
        var kafka = new KafkaProperties();
        kafka.getConsumer().setGroupId("tea-party-testrun-3f9a");
        var properties = new ContainerProperties("test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);

        listener(listening("tea-party", Set.of(Order.class), null), kafka).start();

        assertThat(properties.getGroupId()).isEqualTo("tea-party-testrun-3f9a");
    }

    /// What the container hands the listener: the story gets the envelopes of its region, the others pass by.
    @Test
    void handsTheStoryTheEnvelopesOfItsRegionOnly() {
        var story = listeningFrom(OTHER_REGION, "tea-party", Set.of(Order.class));
        var record = new ConsumerRecord<String, byte[]>("test.orders", 0, 0L, "key", new byte[0]);
        var fromApac = Envelope.of(dice(), OTHER_REGION, APP, AT, order());
        var fromEmea = envelope(order());
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(new ContainerProperties("test.orders"));
        when(reader.read(record)).thenReturn(fromEmea, fromApac);
        var onMessage = ArgumentCaptor.forClass(Object.class);
        listener(story).start();
        verify(container).setupMessageListener(onMessage.capture());
        @SuppressWarnings("unchecked")
        var listener = (MessageListener<String, byte[]>) onMessage.getValue();

        listener.onMessage(record);
        listener.onMessage(record);

        assertThat(story.heard()).containsExactly(fromApac);
    }

    @Test
    void startsNoConsumerForAStoryThatListensToNothing() {
        var listener = listener(acting("load", null));

        listener.start();

        verify(containers, never()).createContainer(any(String[].class));
        assertThat(listener.isRunning()).isFalse();
    }

    @Test
    void stopsTheConsumerItStarted() {
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(new ContainerProperties("test.orders"));
        var listener = listener(listening("tea-party", Set.of(Order.class), null));
        listener.start();

        listener.stop();

        verify(container).stop();
        assertThat(listener.isRunning()).isFalse();
    }
}
