package dk.mathmagicians.playground.confluent.eventing.application;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/// The stories on the classpath, and the one this process plays, `--story=<name>`. Two stories of one name are
/// a configuration error, and so is a name nobody answers to: both fail the start with the names known.
public final class Stories {

    private final TreeMap<String, Story> byName = new TreeMap<>();
    private final Story selected;

    public Stories(Collection<Story> stories, String selected) {
        for (var story : stories) {
            var other = byName.put(story.name(), story);
            if (other != null) {
                throw new IllegalArgumentException("two stories are named " + story.name() + ": "
                        + other.getClass().getName() + " and " + story.getClass().getName());
            }
        }
        this.selected = byName.get(selected);
        if (this.selected == null) {
            throw new IllegalArgumentException("story " + selected + " is unknown, the stories are " + names());
        }
    }

    public Story selected() {
        return selected;
    }

    public Set<String> names() {
        return byName.keySet();
    }
}
