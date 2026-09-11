package dk.mathmagicians.playground.confluent.stories.purse;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.ALICE;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.CHESHIRE_CAT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.MAD_HATTER;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.WHITE_RABBIT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.trade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// A table with Alice's and the White Rabbit's purses, over trades built by hand: who paid whom how much.
class KnowWhatIsLeftTest {

    private static final double CENT = 0.005;
    private static final Duration TTL = Duration.ofSeconds(30);

    private final KnowWhatIsLeft table = new KnowWhatIsLeft(Map.of(ALICE, 100.0, WHITE_RABBIT, 50.0), TTL);

    @Test
    void isThePurseStoryListeningToTransactions() {
        assertThat(table.name()).isEqualTo("purse");
        assertThat(table.listensTo()).containsExactly(Transaction.class);
        assertThat(table.playsFor()).hasValue(TTL);
    }

    @Test
    void readsUnderAGroupOfItsOwnNamedAfterTheOwners() {
        assertThat(table.group()).isEqualTo("purse-" + ALICE + "-" + WHITE_RABBIT);
    }

    @Test
    void opensOnePursePerOwnerWithItsCoins() {
        assertThat(table.left(ALICE)).isEqualTo(100);
        assertThat(table.left(WHITE_RABBIT)).isEqualTo(50);
    }

    @Test
    void knowsNobodyElsesPurse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> table.left(MAD_HATTER))
                .withMessageContaining(MAD_HATTER)
                .withMessageContaining(ALICE);
    }

    @Test
    void putsThePriceInWhenTheOwnerSold() {
        table.on(envelope(trade(MAD_HATTER, ALICE, 10.5)));

        assertThat(table.left(ALICE)).isCloseTo(110.5, within(CENT));
        assertThat(table.left(WHITE_RABBIT)).isEqualTo(50);
    }

    @Test
    void takesThePriceOutWhenTheOwnerBought() {
        table.on(envelope(trade(ALICE, MAD_HATTER, 10.5)));

        assertThat(table.left(ALICE)).isCloseTo(89.5, within(CENT));
    }

    @Test
    void movesTheCoinsBetweenTwoPursesAtTheTable() {
        table.on(envelope(trade(ALICE, WHITE_RABBIT, 30)));

        assertThat(table.left(ALICE)).isCloseTo(70, within(CENT));
        assertThat(table.left(WHITE_RABBIT)).isCloseTo(80, within(CENT));
    }

    @Test
    void letsTheOthersTradesPassBy() {
        table.on(envelope(trade(MAD_HATTER, CHESHIRE_CAT, 99)));

        assertThat(table.left(ALICE)).isEqualTo(100);
        assertThat(table.left(WHITE_RABBIT)).isEqualTo(50);
    }

    @Test
    void knowsWhatTheOwnerOwesWhenTheShoppingGoesOn() {
        table.on(envelope(trade(WHITE_RABBIT, MAD_HATTER, 60)));
        table.on(envelope(trade(WHITE_RABBIT, CHESHIRE_CAT, 5)));

        assertThat(table.left(WHITE_RABBIT)).isCloseTo(-15, within(CENT));
    }

    @Test
    void refusesAPayloadItDoesNotListenTo() {
        var envelope = envelope(offer());

        assertThatIllegalStateException()
                .isThrownBy(() -> table.on(envelope))
                .withMessageContaining("purse");
    }
}
