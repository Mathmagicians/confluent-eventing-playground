package dk.mathmagicians.playground.confluent.eventing.domain;

import java.util.List;
import java.util.random.RandomGenerator;

/// Vocabulary for generated events, Alice likes games. Every draw is a function of the generator passed in.
enum Wonderland {

    CHARACTERS("Alice",
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
            "Gryphon"),
PLACES(
            "Rabbit Hole",
            "Pool of Tears",
            "Caucus Race Shore",
            "Duchess's Kitchen",
            "Tea Party Table",
            "Queen's Croquet Ground",
            "Court of Hearts",
            "Looking-Glass Land"),
THINGS(
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
            "Top Hat"),
    QUOTES(
            "Curiouser and curiouser!",
            "We're all mad here.",
            "Off with their heads!",
            "Oh dear! Oh dear! I shall be too late!",
            "Begin at the beginning, and go on till you come to the end: then stop.",
            "Why, sometimes I've believed as many as six impossible things before breakfast.",
            "It's no use going back to yesterday, because I was a different person then.",
            "Who in the world am I? Ah, that's the great puzzle.");
    private final List<String> words;

    Wonderland(String... words) {
        this.words = List.of(words);
    }

    String next(RandomGenerator random) {
        return words.get(random.nextInt(words.size()));
    }
}
