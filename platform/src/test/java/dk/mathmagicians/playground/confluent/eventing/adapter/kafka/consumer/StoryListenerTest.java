package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import static dk.mathmagicians.playground.confluent.eventing.adapter.kafka.KafkaFixtures.topics;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.acting;
import static dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures.listening;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.mathmagicians.playground.confluent.eventing.application.Stories;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.application.StoryFixtures;
import dk.mathmagicians.playground.confluent.eventing.domain.Offer;
import dk.mathmagicians.playground.confluent.eventing.domain.Order;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

@ExtendWith(MockitoExtension.class)
class StoryListenerTest {

    @Mock
    private ConcurrentKafkaListenerContainerFactory<String, byte[]> containers;

    @Mock
    private ConcurrentMessageListenerContainer<String, byte[]> container;

    private final Reader reader = mock(Reader.class);

    private StoryListener listener(Story story) {
        return new StoryListener(new Stories(List.of(story), story.name()), topics(), reader, containers);
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
        var story = new StoryFixtures.Fake("purse", group, Set.of(Order.class), null, new AtomicInteger());
        var properties = new ContainerProperties("test.orders");
        when(containers.createContainer(any(String[].class))).thenReturn(container);
        when(container.getContainerProperties()).thenReturn(properties);

        listener(story).start();

        assertThat(properties.getGroupId()).isEqualTo(group);
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
