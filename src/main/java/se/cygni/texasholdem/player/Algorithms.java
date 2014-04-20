package se.cygni.texasholdem.player;

import se.cygni.texasholdem.client.CurrentPlayState;
import se.cygni.texasholdem.game.Card;
import se.cygni.texasholdem.game.Deck;
import se.cygni.texasholdem.game.Hand;
import se.cygni.texasholdem.game.definitions.PokerHand;
import se.cygni.texasholdem.game.definitions.Rank;
import static se.cygni.texasholdem.game.definitions.Rank.*;
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


        return 0;
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

    static final int AHEAD = 0, TIED = 1, BEHIND = 2;

    public HandPotential getHandPotential () {
        double HP[][] = new double[3][3], HPTotal[] = new double[3];
        for (Hand oppHand : getAllOpponentCombinations()) {
            PokerHand myRank = myHand.getPokerHand();
            PokerHand oppRank = oppHand.getPokerHand();
            int index;
            if (myRank.compareTo(oppRank) < 0) // myhand > opprank
                index = AHEAD;
            else if (myRank.compareTo(oppRank) == 0) // myhand == opprank
                index = TIED;
            else
                index = BEHIND;

            HPTotal[index]++;

            for (List<Card> possibleBoard : getAllPossibleBoardFinishes(oppHand)) {
                PokerHand ourBest = new PokerHandUtil(possibleBoard, myHand.getCards()).getBestHand().getPokerHand();
                PokerHand oppBest = new PokerHandUtil(possibleBoard, oppHand.getCards()).getBestHand().getPokerHand();

                if (ourBest.compareTo(oppBest) < 0) // myBest > oppBest
                    HP[index][AHEAD]++;
                else if (ourBest.compareTo(oppBest) == 0) // myBest == oppBest
                    HP[index][TIED]++;
                else
                    HP[index][BEHIND]++;
            }
        }

        double ppot = (HP[BEHIND][AHEAD] + HP[BEHIND][TIED]/2 + HP[TIED][AHEAD]/2) / (HPTotal[BEHIND]+HPTotal[TIED]/2);
        double npot = (HP[AHEAD][BEHIND] + HP[TIED][BEHIND]/2 + HP[AHEAD][TIED]/2) / (HPTotal[AHEAD]+HPTotal[TIED]/2);

        return new HandPotential(ppot, npot);
    }

    private List<List<Card>> getAllPossibleBoardFinishes(Hand oppHand) {
        List<Card> unseenCards = getUnseenCards();
        unseenCards.removeAll (oppHand.getCards());
        List<List<Card>> allPossibleBoardFinishes = new ArrayList<>();

        for (int i = 0; i < unseenCards.size(); i++) {
            List<Card> possibleBoardFinish = new ArrayList<>(boardCards);
            allPossibleBoardFinishes.add(possibleBoardFinish);

            possibleBoardFinish.add(unseenCards.get(i));
            if (possibleBoardFinish.size() == 5) // completed with only one card
                continue;

            for (int k = i + 1; k < unseenCards.size(); k++) {
                possibleBoardFinish.add(unseenCards.get(k));
            }
            if (possibleBoardFinish.size() != 5)
                throw new IllegalStateException("Possible boardcards are not 5");
        }
        return allPossibleBoardFinishes;
    }

    public static final class HandPotential {
        final double ppot, npot;

        HandPotential (double ppot, double npot) {
            this.ppot = ppot;
            this.npot = npot;
        }
    }
}
