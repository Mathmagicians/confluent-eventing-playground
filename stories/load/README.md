# The load

The generator: one payload type to its topic, many producers, a region, a ttl. `--story=load`, the default, and
the `--load.*` settings, see the Play section of the root README.

## The feature

The load has no feature of its own; the platform's `i_can_generate_events.feature` is its test, and the other
stories drive it through the platform's generator steps:

| Scenarios | Unique steps | Of the platform | Of the load |
|-----------|--------------|-----------------|-------------|
| 3         | 7            | 7               | 0           |
