package se.cygni.texasholdem.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.cygni.texasholdem.client.CurrentPlayState;
import se.cygni.texasholdem.client.PlayerClient;
import se.cygni.texasholdem.communication.message.event.*;
import se.cygni.texasholdem.communication.message.request.ActionRequest;
import se.cygni.texasholdem.game.*;
import se.cygni.texasholdem.game.definitions.PokerHand;
import se.cygni.texasholdem.game.util.PokerHandUtil;

import java.util.*;

/**
 * This is an example Poker bot player, you can use it as
 * a starting point when creating your own.
 * <p/>
 * If you choose to create your own class don't forget that
 * it must implement the interface Player
 *
 * @see Player
 *      <p/>
 *      Javadocs for common utilities and classes used may be
 *      found here:
 *      http://poker.cygni.se/mavensite/texas-holdem-common/apidocs/index.html
 *      <p/>
 *      You can inspect the games you bot has played here:
 *      http://poker.cygni.se/showgame
 */
public class KvargBot implements Player {

    private static Logger log = LoggerFactory.getLogger(KvargBot.class);

    private final String serverHost;
    private final int serverPort;
    private final PlayerClient playerClient;
    private Action callAction;
    private Action checkAction;
    private Action raiseAction;
    private Action foldAction;
    private Action allInAction;
    private CurrentPlayState playState;
    private Hand myHand;
    private List<Card> boardCards;
    private Algorithms algorithms;

    /**
     * Default constructor for a Java Poker Bot.
     *
     * @param serverHost IP or hostname to the poker server
     * @param serverPort port at which the poker server listens
     */
    public KvargBot(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;

        // Initialize the player client
        playerClient = new PlayerClient(this, serverHost, serverPort);
    }

    public void playATrainingGame() throws Exception {
        playerClient.connect();
        playerClient.registerForPlay(Room.TRAINING);
    }

    /**
     * The main method to start your bot.
     *
     * @param args
     */
    public static void main(String... args) {
        KvargBot bot = new KvargBot("poker.cygni.se", 4711);

        try {
            bot.playATrainingGame();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The name you choose must be unique, if another connected bot has
     * the same name your bot will be denied connection.
     *
     * @return The name under which this bot will be known
     */
    @Override
    public String getName() {
        return "KVARGBOT-3000";
    }

    /**
     * This is where you supply your bot with your special mojo!
     * <p/>
     * The ActionRequest contains a list of all the possible actions
     * your bot can perform.
     *
     * @param request The list of Actions that the bot may perform.
     *
     * @return The action the bot wants to perform.
     *
     * @see ActionRequest
     *      <p/>
     *      Given the current situation you need to choose the best
     *      action. It is not allowed to change any values in the
     *      ActionRequest. The amount you may RAISE or CALL is already
     *      predermined by the poker server.
     *      <p/>
     *      If an invalid Action is returned the server will ask two
     *      more times. Failure to comply (i.e. returning an incorrect
     *      or non valid Action) will result in a forced FOLD for the
     *      current Game Round.
     * @see Action
     */
    @Override
    public Action actionRequired(ActionRequest request) {

        Action response = getBestAction(request);
        log.info("I'm going to {} {}",
                response.getActionType(),
                response.getAmount() > 0 ? "with " + response.getAmount() : "");

        return response;
    }

    /**
     * A helper method that returns this bots idea of the best action.
     * Note! This is just an example, you need to add your own smartness
     * to win.
     *
     * @param request
     *
     * @return
     */
    private Action getBestAction(ActionRequest request) {
        setPossibleActions(request);
        playState = playerClient.getCurrentPlayState();
        boardCards = playState.getCommunityCards();
        algorithms = new Algorithms(playState);
        setMyHand();

        double winChance = algorithms.getHandStrength();

        if (boardCards.size() == 0) // pre-flop
            return preFlop();
        else if (boardCards.size() == 3) // flop
            return flop();
        else if (boardCards.size() == 4) // turn
            return turn();
        else if (boardCards.size() == 5) // river
            return river();

        return foldAction;
    }

    private Action preFlop() {
    	double chenScore = algorithms.chenFormula(myHand.getCards().get(0), myHand.getCards().get(1));
    	if(chenScore > 9 && raiseAction != null) 
    		return raiseAction;
    	else if (chenScore >= 8 && callAction != null)
    		return callAction;
        return checkAction != null ? checkAction : foldAction;
    }

    private Action flop() {
        return null;
    }

    private Action turn() {
        return null;
    }

    private Action river() {
        return null;
    }

    private boolean shouldAllIn(double handStrength) {
        boolean hasPair = myHand.getPokerHand().getOrderValue() == PokerHand.ONE_PAIR.getOrderValue();
        if (hasPair)
            return false;
        return handStrength > 0.8 && allInAction != null;
    }

    private boolean shouldRaise(double handStrength) {
        return handStrength > 0.6 && raiseAction != null;
    }

    private boolean shouldCall(double handStrength) {
        if (getNumberOfOpponents() == 1)
            handStrength += 0.2;
        boolean isFreeCall = playState.amIBigBlindPlayer() && boardCards.isEmpty();
        return (handStrength > 0.4 || isFreeCall) && callAction != null;
    }

    private int getNumberOfOpponents () {
        return playState.getNumberOfPlayers() - playState.getNumberOfFoldedPlayers() - 1;
    }

    private void setMyHand() {
        PokerHandUtil ourUtil = new PokerHandUtil(boardCards, playState.getMyCards());
        myHand = ourUtil.getBestHand();
    }


    /**
     * **********************************************************************
     * <p/>
     * Event methods
     * <p/>
     * These methods tells the bot what is happening around the Poker Table.
     * The methods must be implemented but it is not mandatory to act on the
     * information provided.
     * <p/>
     * The helper class CurrentPlayState provides most of the book keeping
     * needed to keep track of the total picture around the table.
     *
     * @see CurrentPlayState
     *      <p/>
     *      ***********************************************************************
     */

    @Override
    public void onPlayIsStarted(final PlayIsStartedEvent event) {
        log.debug("Play is started");
    }

    @Override
    public void onTableChangedStateEvent(TableChangedStateEvent event) {

        log.debug("Table changed state: {}", event.getState());
    }

    @Override
    public void onYouHaveBeenDealtACard(final YouHaveBeenDealtACardEvent event) {

        log.debug("I, {}, got a card: {}", getName(), event.getCard());
    }

    @Override
    public void onCommunityHasBeenDealtACard(
            final CommunityHasBeenDealtACardEvent event) {

        log.debug("Community got a card: {}", event.getCard());
    }

    @Override
    public void onPlayerBetBigBlind(PlayerBetBigBlindEvent event) {

        log.debug("{} placed big blind with amount {}", event.getPlayer().getName(), event.getBigBlind());
    }

    @Override
    public void onPlayerBetSmallBlind(PlayerBetSmallBlindEvent event) {

        log.debug("{} placed small blind with amount {}", event.getPlayer().getName(), event.getSmallBlind());
    }

    @Override
    public void onPlayerFolded(final PlayerFoldedEvent event) {

        log.debug("{} folded after putting {} in the pot", event.getPlayer().getName(), event.getInvestmentInPot());
    }

    @Override
    public void onPlayerForcedFolded(PlayerForcedFoldedEvent event) {

        log.debug("NOT GOOD! {} was forced to fold after putting {} in the pot because exceeding the time limit", event.getPlayer().getName(), event.getInvestmentInPot());
    }

    @Override
    public void onPlayerCalled(final PlayerCalledEvent event) {

        log.debug("{} called with amount {}", event.getPlayer().getName(), event.getCallBet());
    }

    @Override
    public void onPlayerRaised(final PlayerRaisedEvent event) {

        log.debug("{} raised with bet {}", event.getPlayer().getName(), event.getRaiseBet());
    }

    @Override
    public void onTableIsDone(TableIsDoneEvent event) {

        log.debug("Table is done, I'm leaving the table with ${}", playerClient.getCurrentPlayState().getMyCurrentChipAmount());
        log.info("Ending poker session, the last game may be viewed at: http://{}/showgame/table/{}", serverHost, playerClient.getCurrentPlayState().getTableId());
    }

    @Override
    public void onPlayerWentAllIn(final PlayerWentAllInEvent event) {

        log.debug("{} went all in with amount {}", event.getPlayer().getName(), event.getAllInAmount());
    }

    @Override
    public void onPlayerChecked(final PlayerCheckedEvent event) {

        log.debug("{} checked", event.getPlayer().getName());
    }

    @Override
    public void onYouWonAmount(final YouWonAmountEvent event) {

        log.debug("I, {}, won: {}", getName(), event.getWonAmount());
    }

    @Override
    public void onShowDown(final ShowDownEvent event) {

        if (!log.isInfoEnabled()) {
            return;
        }

        final StringBuilder sb = new StringBuilder();
        final Formatter formatter = new Formatter(sb);

        sb.append("ShowDown:\n");

        for (final PlayerShowDown psd : event.getPlayersShowDown()) {
            formatter.format("%-13s won: %6s  hand: %-15s ",
                    psd.getPlayer().getName(),
                    psd.getHand().isFolded() ? "Fold" : psd.getWonAmount(),
                    psd.getHand().getPokerHand().getName());

            sb.append(" cards: | ");
            for (final Card card : psd.getHand().getCards()) {
                formatter.format("%-13s | ", card);
            }
            sb.append("\n");
        }

        log.info(sb.toString());
    }

    @Override
    public void onPlayerQuit(final PlayerQuitEvent event) {

        log.debug("Player {} has quit", event.getPlayer());
    }

    @Override
    public void connectionToGameServerLost() {

        log.debug("Lost connection to game server, exiting");
        System.exit(0);
    }

    @Override
    public void connectionToGameServerEstablished() {

        log.debug("Connection to game server established");
    }

    @Override
    public void serverIsShuttingDown(final ServerIsShuttingDownEvent event) {
        log.debug("Server is shutting down");
    }



    private void setPossibleActions(ActionRequest request) {
        callAction = null;
        checkAction = null;
        raiseAction = null;
        foldAction = null;
        allInAction = null;

        for (final Action action : request.getPossibleActions()) {
            switch (action.getActionType()) {
                case CALL:
                    callAction = action;
                    break;
                case CHECK:
                    checkAction = action;
                    break;
                case FOLD:
                    foldAction = action;
                    break;
                case RAISE:
                    raiseAction = action;
                    break;
                case ALL_IN:
                    allInAction = action;
                default:
                    break;
            }
        }
    }
}
