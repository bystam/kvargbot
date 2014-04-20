package se.cygni.texasholdem.player;

import se.cygni.texasholdem.client.CurrentPlayState;
import se.cygni.texasholdem.game.Card;
import se.cygni.texasholdem.game.Deck;
import se.cygni.texasholdem.game.Hand;
import se.cygni.texasholdem.game.definitions.PokerHand;
import se.cygni.texasholdem.game.definitions.Rank;
import static se.cygni.texasholdem.game.definitions.Rank.*;

import se.cygni.texasholdem.game.util.CardsUtil;
import se.cygni.texasholdem.game.util.PokerHandUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bystam on 20/04/14.
 */
public class Algorithms {

    private CurrentPlayState playState;
    private Hand myHand;
    private List<Card> boardCards;

    public Algorithms(CurrentPlayState playState) {
        this.playState = playState;
        boardCards = playState.getCommunityCards();
        myHand = new PokerHandUtil(boardCards, playState.getMyCards()).getBestHand();
    }

    public double chenFormula () {
        Card one = playState.getMyCards().get(0);
        Card two = playState.getMyCards().get(1);
    	int cardOneValue = one.getRank().getOrderValue();
    	int cardTwoValue = two.getRank().getOrderValue();
    	double baseScore = Math.max(getChenCardScore(one), getChenCardScore(two));
    	if(cardOneValue == cardTwoValue) {
    		baseScore = Math.max(5, baseScore * 2);
    	}
    	if(one.getSuit().compareTo(two.getSuit()) == 0) {
    		baseScore += 2;
    	}
    	
    	int gap = Math.abs(cardOneValue - cardTwoValue);
    	switch (gap) {
    	case 0: break;
    	case 1: baseScore++; break;
    	case 2: baseScore--; break;
    	case 3: baseScore -= 2; break;
    	case 4: baseScore -= 4; break;
    	default: baseScore -= 5; break;
    	}
    	
		return baseScore;// - (double) gap;
    }
    
    private double getChenCardScore (Card card) {
    	switch(card.getRank()) {
    	case ACE: return 10;
    	case KING: return 8;
    	case QUEEN: return 7;
    	case JACK: return 6;
    	default: return card.getRank().getOrderValue() / 2.0;
    	}
    }
    public double getHandStrength() {
        double ahead = 0, tied = 0, behind = 0;

        for (Hand oppHand : getAllOpponentCombinations()) {
            PokerHand myRank = myHand.getPokerHand();
            PokerHand oppRank = oppHand.getPokerHand();
            if (compareHands(myHand, oppHand) < 0)
                ahead++;
            else if (compareHands(myHand, oppHand) == 0)
                tied++;
            else
                behind++;

        }
        return (ahead + tied / 2) / (ahead + tied + behind);
    }

    private int compareHands (Hand my, Hand opp) {
        if (my.getPokerHand().getOrderValue() > opp.getPokerHand().getOrderValue())
            return -1;
        else if (my.getPokerHand().getOrderValue() < opp.getPokerHand().getOrderValue())
            return 1;

        switch(my.getPokerHand()) {
            case ONE_PAIR: return comparePairs(my, opp);
            case TWO_PAIRS: return compareTwoPairs(my, opp);
            default: break;
        }
        return 0;
    }

    private int comparePairs(Hand my, Hand opp) {
        Rank myPair = CardUtils.getPairRank(my);
        Rank oppPair = CardUtils.getPairRank(opp);
        return compareRanks(myPair, oppPair);
    }

    private int compareTwoPairs(Hand my, Hand opp) {
        Rank myTwoPairs = CardUtils.getHighestTwoPairRank(my);
        Rank oppTwoPairs = CardUtils.getHighestTwoPairRank(opp);
        return compareRanks(myTwoPairs, oppTwoPairs);
    }

    private int compareRanks (Rank my, Rank opp) {
        int diff = my.getOrderValue() - opp.getOrderValue();
        return diff > 0 ? -1 : (diff == 0 ? 0 : 1);
    }

    private List<Hand> getAllOpponentCombinations() {
        List<Hand> allCombinations = new ArrayList<>();
        List<Card> unseenCards = getUnseenCards();
        for (int i = 0; i < unseenCards.size(); i++) {
            for (int k = i + 1; k < unseenCards.size(); k++) {
                List<Card> oppCards = Arrays.asList(unseenCards.get(i), unseenCards.get(k));
                PokerHandUtil oppUtil = new PokerHandUtil(boardCards, oppCards);
                Hand oppHand = oppUtil.getBestHand();

                allCombinations.add(oppHand);
            }
        }
        return allCombinations;
    }

    private List<Card> getUnseenCards () {
        List<Card> unseen = Deck.getOrderedListOfCards();
        unseen.removeAll (playState.getMyCardsAndCommunityCards());
        return unseen;
    }
}
