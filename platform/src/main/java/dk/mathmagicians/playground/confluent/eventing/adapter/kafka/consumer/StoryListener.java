package dk.mathmagicians.playground.confluent.eventing.adapter.kafka.consumer;

import dk.mathmagicians.playground.confluent.eventing.adapter.kafka.Topics;
import dk.mathmagicians.playground.confluent.eventing.application.Story;
import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import java.util.Arrays;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

/// Driving adapter for `test` and `prod`: the consumer. When the context starts it subscribes to the topics of
/// what the selected story listens to, under the story's group, `spring.kafka.consumer.group-id` when a
/// deployment names one, and hands every record the story hears to it as an envelope: a story from a region
/// hears its region only. A story that listens to nothing gets no consumer. What the story throws propagates
/// to the container's error handler, `DeadLetters`, which sets the record aside on the dead-letter topic.
@PrimaryAdapter
@Component
@Profile("!local")
public final class StoryListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(StoryListener.class);

    private final Story story;
    private final Topics topics;
    private final Reader reader;
    private final ConcurrentKafkaListenerContainerFactory<?, ?> containers;
    private final String group;
    private @Nullable MessageListenerContainer container;

    StoryListener(Story story, Topics topics, Reader reader,
                  ConcurrentKafkaListenerContainerFactory<?, ?> containers, KafkaProperties properties) {
        this.story = story;
        this.topics = topics;
        this.reader = reader;
        this.containers = containers;
        var named = properties.getConsumer().getGroupId();
        this.group = named != null ? named : story.group();
    }

    @Override
    public void start() {
        var subscribed = story.listensTo().stream().map(topics::of).sorted().toArray(String[]::new);
        if (subscribed.length == 0) {
            log.info("{} listens to nothing, no consumer", story.name());
            return;
        }
        var listening = containers.createContainer(subscribed);
        listening.getContainerProperties().setGroupId(group);
        listening.setupMessageListener((MessageListener<String, byte[]>) record -> on(reader.read(record)));
        listening.start();
        container = listening;
        log.info("{} listens to {} as the group {}", story.name(), Arrays.toString(subscribed), group);
    }

    private void on(Envelope envelope) {
        if (story.hears(envelope)) {
            story.on(envelope);
        } else {
            log.debug("{} is from {}, not for {}", envelope.id(), envelope.region(), story.name());
        }
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
            container = null;
        }
    }

    @Override
    public boolean isRunning() {
        return container != null && container.isRunning();
    }
}
