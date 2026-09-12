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

import dk.mathmagicians.playground.confluent.eventing.domain.Transaction;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// A table with Alice's and the White Rabbit's purses, over trades built by hand: who paid whom how much. The
/// balances are money, compared as amounts, so a scale never matters.
class KnowWhatIsLeftTest {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final KnowWhatIsLeft table =
            new KnowWhatIsLeft(Map.of(ALICE, new BigDecimal("100"), WHITE_RABBIT, new BigDecimal("50")), TTL);

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
    void opensOnePursePerOwnerWithItsBalance() {
        assertThat(table.balance(ALICE)).isEqualByComparingTo("100");
        assertThat(table.balance(WHITE_RABBIT)).isEqualByComparingTo("50");
    }

    @Test
    void knowsNobodyElsesPurse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> table.balance(MAD_HATTER))
                .withMessageContaining(MAD_HATTER)
                .withMessageContaining(ALICE);
    }

    @Test
    void putsThePriceInWhenTheOwnerSold() {
        table.on(envelope(trade(MAD_HATTER, ALICE, 10.5)));

        assertThat(table.balance(ALICE)).isEqualByComparingTo("110.5");
        assertThat(table.balance(WHITE_RABBIT)).isEqualByComparingTo("50");
    }

    @Test
    void takesThePriceOutWhenTheOwnerBought() {
        table.on(envelope(trade(ALICE, MAD_HATTER, 10.5)));

        assertThat(table.balance(ALICE)).isEqualByComparingTo("89.5");
    }

    @Test
    void movesTheCoinsBetweenTwoPursesAtTheTable() {
        table.on(envelope(trade(ALICE, WHITE_RABBIT, 30)));

        assertThat(table.balance(ALICE)).isEqualByComparingTo("70");
        assertThat(table.balance(WHITE_RABBIT)).isEqualByComparingTo("80");
    }

    @Test
    void letsTheOthersTradesPassBy() {
        table.on(envelope(trade(MAD_HATTER, CHESHIRE_CAT, 99)));

        assertThat(table.balance(ALICE)).isEqualByComparingTo("100");
        assertThat(table.balance(WHITE_RABBIT)).isEqualByComparingTo("50");
    }

    @Test
    void knowsWhatTheOwnerOwesWhenTheShoppingGoesOn() {
        table.on(envelope(trade(WHITE_RABBIT, MAD_HATTER, 60)));
        table.on(envelope(trade(WHITE_RABBIT, CHESHIRE_CAT, 5)));

        assertThat(table.balance(WHITE_RABBIT)).isEqualByComparingTo("-15");
    }

    @Test
    void refusesAPayloadItDoesNotListenTo() {
        var envelope = envelope(offer());

        assertThatIllegalStateException()
                .isThrownBy(() -> table.on(envelope))
                .withMessageContaining("purse");
    }
}
