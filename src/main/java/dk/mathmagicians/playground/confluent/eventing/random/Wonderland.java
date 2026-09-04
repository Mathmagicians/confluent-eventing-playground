package dk.mathmagicians.playground.confluent.eventing.random;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/// Vocabulary for generated events, Alice likes games.
/// One instance serves all threads; each supplier draws with its own dice.
public final class Wonderland {

    private static final List<String> CHARACTERS = List.of(
            "Alice",
            "White Rabbit",
            "Cheshire Cat",
            "Mad Hatter",
            "March Hare",
            "Dormouse",
            "Queen of Hearts",
            "King of Hearts",
            "Caterpillar",
            "Duchess",
            "Mock Turtle",
            "Gryphon");

    private static final List<String> PLACES = List.of(
            "Rabbit Hole",
            "Pool of Tears",
            "Caucus Race Shore",
            "Duchess's Kitchen",
            "Tea Party Table",
            "Queen's Croquet Ground",
            "Court of Hearts",
            "Looking-Glass Land");

    private static final List<String> THINGS = List.of(
            "Pocket Watch",
            "Drink Me Bottle",
            "Eat Me Cake",
            "White Gloves",
            "Fan",
            "Tea Set",
            "Flamingo Mallet",
            "Hedgehog Ball",
            "Tarts",
            "Golden Key",
            "Hookah",
            "Top Hat");

    private static final List<String> QUOTES = List.of(
            "Curiouser and curiouser!",
            "We're all mad here.",
            "Off with their heads!",
            "Oh dear! Oh dear! I shall be too late!",
            "Begin at the beginning, and go on till you come to the end: then stop.",
            "Why, sometimes I've believed as many as six impossible things before breakfast.",
            "It's no use going back to yesterday, because I was a different person then.",
            "Who in the world am I? Ah, that's the great puzzle.");

    private final Words characters;
    private final Words places;
    private final Words things;
    private final Words quotes;

    public Wonderland() {
        this(ThreadLocalRandom::current);
    }

    public Wonderland(Supplier<RandomGenerator> random) {
        characters = Words.of(CHARACTERS, random);
        places = Words.of(PLACES, random);
        things = Words.of(THINGS, random);
        quotes = Words.of(QUOTES, random);
    }

    public Supplier<String> characters() {
        return characters;
    }

    public Supplier<String> places() {
        return places;
    }

    public Supplier<String> things() {
        return things;
    }

    public Supplier<String> quotes() {
        return quotes;
    }
}
