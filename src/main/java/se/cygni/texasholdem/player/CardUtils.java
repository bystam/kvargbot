package se.cygni.texasholdem.player;

import se.cygni.texasholdem.game.Card;
import se.cygni.texasholdem.game.Hand;
import se.cygni.texasholdem.game.definitions.Rank;

import java.util.List;

/**
 * Created by bystam on 21/04/14.
 */
public class CardUtils {

    public static Rank getPairRank (Hand hand) {
        List<Card> cards = hand.getCards();
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i).getRank() == cards.get(i+1).getRank())
                return cards.get(i).getRank();
        }
        throw new RuntimeException("hand has no pair");
    }
}
