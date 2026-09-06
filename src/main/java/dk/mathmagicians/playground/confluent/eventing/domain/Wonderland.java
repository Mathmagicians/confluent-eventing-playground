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
            "Who in the world am I? Ah, that's the great puzzle."),
    /// Templates for `formatted`: `%1$s` a thing, `%2$s` a place, `%3$s` a character, `%4$s` a quote.
    DESCRIPTIONS(
            "Our verrry, verrry fine %1$s is carefully imported from %2$s by %3$s, who has been tending to every"
                    + " detail of it. As %3$s says: %4$s",
            "A %1$s of the rarest kind, found only at %2$s and brought to you by %3$s in person. In the words of"
                    + " %3$s: %4$s",
            "Nobody knows a %1$s like %3$s does. Straight from %2$s, polished twice. %3$s puts it plainly: %4$s",
            "The %1$s that %2$s is famous for, at last, selected by %3$s with the greatest care. %3$s insists:"
                    + " %4$s");

    private final List<String> words;

    Wonderland(String... words) {
        this.words = List.of(words);
    }

    String next(RandomGenerator random) {
        return words.get(random.nextInt(words.size()));
    }
}
