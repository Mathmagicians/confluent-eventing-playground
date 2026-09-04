package dk.mathmagicians.playground.confluent.eventing.random;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/// Word generator, the passed random generator should be the thread local
public final class Words implements Supplier<String> {

    private final List<String> words;
    private final Supplier<RandomGenerator> random;

    private Words(List<String> words, Supplier<RandomGenerator> random) {
        this.words = List.copyOf(words);
        this.random = random;
    }

    public static Words of(List<String> words) {
        return of(words, ThreadLocalRandom::current);
    }

    public static Words of(List<String> words, Supplier<RandomGenerator> random) {
        return new Words(words, random);
    }

    @Override
    public String get() {
        throw new UnsupportedOperationException("not implemented");
    }
}
