package dk.mathmagicians.playground.confluent.stories.purse;

import dk.mathmagicians.playground.confluent.eventing.domain.Wonderland;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.convert.DurationUnit;

/// The settings of the purse story, `purse.*`: the purses at this table, `--purse.owners="Alice:1000,White
/// Rabbit:500"`, one entry per purse, the owner as the features name a character of Wonderland and the coins the
/// purse starts with; and how long the table sits, `--purse.ttl` seconds, zero for until stopped. Every value has
/// a default, so the bundle binds at every start whatever the story; a wrong value fails startup with the entry in
/// the message.
@ConfigurationProperties("purse")
public record PurseProperties(
        @DefaultValue("Alice:1000") List<String> owners,
        @DefaultValue("0") @DurationUnit(ChronoUnit.SECONDS) Duration ttl) {

    /// A purse as it opens: whose, by the id on the wire, and with how many coins.
    public record Opening(String ownerId, double coins) {
    }

    private static final String ENTRY = "Name:coins";

    public PurseProperties {
        if (owners == null || owners.isEmpty()) {
            throw new IllegalArgumentException("purse.owners is required: at least one " + ENTRY);
        }
        var seen = new TreeSet<String>();
        for (var entry : owners) {
            var opening = parse(entry);
            if (!seen.add(opening.ownerId())) {
                throw new IllegalArgumentException("purse.owners names " + opening.ownerId() + " twice");
            }
        }
        if (ttl == null || ttl.isNegative()) {
            throw new IllegalArgumentException("purse.ttl must be seconds, zero for until stopped, was " + ttl);
        }
    }

    /// The purses, one per entry, in the order given.
    public List<Opening> openings() {
        return owners.stream().map(PurseProperties::parse).toList();
    }

    private static Opening parse(String entry) {
        var colon = entry.lastIndexOf(':');
        if (colon <= 0 || colon == entry.length() - 1) {
            throw new IllegalArgumentException("purse.owners entry '" + entry + "' must be " + ENTRY);
        }
        var ownerId = Wonderland.characterId(entry.substring(0, colon).strip());
        double coins;
        try {
            coins = Double.parseDouble(entry.substring(colon + 1).strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("purse.owners entry '" + entry + "' must be " + ENTRY, e);
        }
        if (coins < 0) {
            throw new IllegalArgumentException("purse.owners entry '" + entry + "' cannot open with a debt");
        }
        return new Opening(ownerId, coins);
    }
}
