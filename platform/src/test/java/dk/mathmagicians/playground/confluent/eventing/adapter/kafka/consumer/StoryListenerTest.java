package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import dk.mathmagicians.playground.confluent.eventing.domain.Payload;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

@ExtendWith(MockitoExtension.class)
class StoryListenerTest {

    /// A story that listens to offers and orders, or to nothing, under the group it names.
    private record Listening(String name, String group, Set<Class<? extends Payload>> listensTo) implements Story {

        Listening(String name, Set<Class<? extends Payload>> listensTo) {
            this(name, name, listensTo);
        }

        @Override
        public void on(Envelope envelope) {
        }
    }

    @Mock
    private ConcurrentKafkaListenerContainerFactory<String, byte[]> containers;

    @Mock
    private ConcurrentMessageListenerContainer<String, byte[]> container;

    private final Reader reader = mock(Reader.class);

    @Test
    void subscribesToTheTopicsOfWhatTheStoryListensToAsTheStorysGroup() {
        var story = new Listening("tea-party", Set.of(Order.class, Offer.class));
        var properties = new ContainerProperties("test.offers", "test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);
        when(container.isRunning()).thenReturn(true);
        var listener = new StoryListener(new Stories(List.of(story), "tea-party"), topics(), reader, containers);

        listener.start();

        verify(containers).createContainer("test.offers", "test.orders");
        assertThat(properties.getGroupId()).isEqualTo("tea-party");
        verify(container).setupMessageListener(any());
        verify(container).start();
        assertThat(listener.isRunning()).isTrue();
    }

    @Test
    void subscribesUnderTheGroupTheStoryAnswers() {
        var story = new Listening("purse", "purse-" + character("Alice"), Set.of(Order.class));
        var properties = new ContainerProperties("test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);
        var listener = new StoryListener(new Stories(List.of(story), "purse"), topics(), reader, containers);

        listener.start();

        assertThat(properties.getGroupId()).isEqualTo("purse-" + character("Alice"));
    }

    @Test
    void startsNoConsumerForAStoryThatListensToNothing() {
        var story = new Listening("load", Set.of());
        var listener = new StoryListener(new Stories(List.of(story), "load"), topics(), reader, containers);

        listener.start();

        verify(containers, never()).createContainer(any(String[].class));
        assertThat(listener.isRunning()).isFalse();
    }

    @Test
    void stopsTheConsumerItStarted() {
        var story = new Listening("tea-party", Set.of(Order.class));
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(new ContainerProperties("test.orders"));
        var listener = new StoryListener(new Stories(List.of(story), "tea-party"), topics(), reader, containers);
        listener.start();

        listener.stop();

        verify(container).stop();
        assertThat(listener.isRunning()).isFalse();
    }
}
