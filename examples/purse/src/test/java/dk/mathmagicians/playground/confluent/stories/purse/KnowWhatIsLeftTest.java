package dk.mathmagicians.playground.confluent.stories.purse;

import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.AT;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.character;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.envelope;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.offer;
import static dk.mathmagicians.playground.confluent.eventing.domain.EventFixtures.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

import dk.mathmagicians.playground.confluent.eventing.domain.Envelope;
import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// A table with Alice's and the White Rabbit's purses, over trades built by hand: who paid whom how much.
class KnowWhatIsLeftTest {

    private static final String ALICE = character("Alice");
    private static final String RABBIT = character("White Rabbit");
    private static final String HATTER = character("Mad Hatter");
    private static final String CAT = character("Cheshire Cat");
    private static final double CENT = 0.005;

    private final KnowWhatIsLeft table = new KnowWhatIsLeft(Map.of(ALICE, 100.0, RABBIT, 50.0));

    /// A settled trade: the customer pays the seller the price.
    private static Envelope trade(String customer, String seller, double price) {
        return envelope(new Transaction("TX-1", order(), offer(), customer, seller, price, AT));
    }

    @Test
    void isThePurseStoryListeningToTransactions() {
        assertThat(table.name()).isEqualTo("purse");
        assertThat(table.listensTo()).containsExactly(Transaction.class);
    }

    @Test
    void readsUnderAGroupOfItsOwnNamedAfterTheOwners() {
        assertThat(table.group()).isEqualTo("purse-" + ALICE + "-" + RABBIT);
    }

    @Test
    void opensOnePursePerOwnerWithItsCoins() {
        assertThat(table.left(ALICE)).isEqualTo(100);
        assertThat(table.left(RABBIT)).isEqualTo(50);
    }

    @Test
    void knowsNobodyElsesPurse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> table.left(HATTER))
                .withMessageContaining(HATTER)
                .withMessageContaining(ALICE);
    }

    @Test
    void putsThePriceInWhenTheOwnerSold() {
        table.on(trade(HATTER, ALICE, 10.5));

        assertThat(table.left(ALICE)).isCloseTo(110.5, within(CENT));
        assertThat(table.left(RABBIT)).isEqualTo(50);
    }

    @Test
    void takesThePriceOutWhenTheOwnerBought() {
        table.on(trade(ALICE, HATTER, 10.5));

        assertThat(table.left(ALICE)).isCloseTo(89.5, within(CENT));
    }

    @Test
    void movesTheCoinsBetweenTwoPursesAtTheTable() {
        table.on(trade(ALICE, RABBIT, 30));

        assertThat(table.left(ALICE)).isCloseTo(70, within(CENT));
        assertThat(table.left(RABBIT)).isCloseTo(80, within(CENT));
    }

    @Test
    void letsTheOthersTradesPassBy() {
        table.on(trade(HATTER, CAT, 99));

        assertThat(table.left(ALICE)).isEqualTo(100);
        assertThat(table.left(RABBIT)).isEqualTo(50);
    }

    @Test
    void knowsWhatTheOwnerOwesWhenTheShoppingGoesOn() {
        table.on(trade(RABBIT, HATTER, 60));
        table.on(trade(RABBIT, CAT, 5));

        assertThat(table.left(RABBIT)).isCloseTo(-15, within(CENT));
    }

    @Test
    void refusesAPayloadItDoesNotListenTo() {
        var envelope = envelope(offer());

        assertThatIllegalStateException()
                .isThrownBy(() -> table.on(envelope))
                .withMessageContaining("purse");
    }
}
