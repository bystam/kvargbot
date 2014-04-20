package se.cygni.texasholdem.player;

import se.cygni.texasholdem.game.Card;
import se.cygni.texasholdem.game.Hand;
import se.cygni.texasholdem.game.definitions.CardSortBy;
import se.cygni.texasholdem.game.definitions.Rank;

import java.util.List;

/**
 * Created by bystam on 21/04/14.
 */
public class CardUtils {

    public static Rank getHighHandRank(Hand hand) {
        return hand.getCards().get(0).getRank();
    }

    public static Rank getPairRank (Hand hand) {
        List<Card> cards = hand.getCards();
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i).getRank() == cards.get(i+1).getRank())
                return cards.get(i).getRank();
        }
        throw new RuntimeException("hand has no pair");
    }

    public static Rank getHighestTwoPairRank (Hand hand) {
        List<Card> cards = hand.getCards();
        Rank highest = null;
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i).getRank() == cards.get(i+1).getRank()) {
                if (highest == null || highest.getOrderValue() < cards.get(i).getRank().getOrderValue())
                    highest = cards.get(i).getRank();
            }
        }
        if (highest == null)
            throw new RuntimeException("hand has no pair");
        return highest;
    }

    public static Rank getThreeOfAKindRank (Hand hand) {
        List<Card> cards = hand.getCards();
        for (int i = 0; i < cards.size() - 2; i++) {
            if (cards.get(i).getRank() == cards.get(i+1).getRank() &&
                    cards.get(i+1).getRank() == cards.get(i+2).getRank())
                return cards.get(i).getRank();
        }

        throw new RuntimeException("hand has no three of a kind");
    }

    public static Rank getStraightRank (Hand hand) {
        return getHighHandRank(hand);
    }
}
