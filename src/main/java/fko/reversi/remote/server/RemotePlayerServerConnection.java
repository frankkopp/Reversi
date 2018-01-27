/*
 * <p>GPL Dislaimer</p>
 * <p>
 * "Reversi by Frank Kopp"
 * Copyright 2003, 2004, 2005, 2006 Frank Kopp
 * mail-to:frank@familie-kopp.de
 *
 * This file is part of "Reversi by Frank Kopp".
 *
 * "Reversi by Frank Kopp" is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * "Reversi by Frank Kopp" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with "Reversi by Frank Kopp"; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </p>
 *
 *
 */

package fko.reversi.remote.server;

import fko.reversi.Reversi;
import fko.reversi.remote.AbstractReversiRemoteConnection;
import fko.reversi.remote.RemoteProtocol;
import fko.reversi.remote.ProtocolException;
import fko.reversi.remote.RemoteGameRequest;
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;
import fko.reversi.player.PlayerFactory;
import fko.reversi.player.ServerPlayer;
import fko.reversi.player.PlayerType;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * <p>The RemotePlayerServerConnection class ...
 * </p>
 */
public class RemotePlayerServerConnection extends AbstractReversiRemoteConnection
{

    // A back reference to the RemotePlayerServer
    private final RemotePlayerServer _server;

    /**
     * Create a handler for a remote connection
     * @param server
     * @param socketChannel
     */
    public RemotePlayerServerConnection(RemotePlayerServer server, SocketChannel socketChannel) throws IOException {
        super();
        // The back reference to the RemotePlayerServer
        _server = server;
        // The SocketChannel is already connected
        setSocketChannel(socketChannel);
        // Put this SocketChannel into nonblocking mode
        getSocketChannel().configureBlocking(false);
        // Create a Selector for the channel
        setSelector(Selector.open());
        // Register the SocketChannel with the Selector
        getSocketChannel().register(getSelector(), SelectionKey.OP_READ);
        log("New Connection created!");
    }

    /**
     * A connection to a remote client is handled in its own thread. The method run() implements the
     * Runnable interface. It will be started in a seperate thread.<br/>
     * In run() we handle the actual conversation with a remote client be sending and receiving messages
     * and also taking the appropriate actions.
     *
     * @see Thread#run()
     */
    public void run() {

        log("Connection started!");

        // to gracefully end a connection
        setEndConnection(false);

        // The actual conversation loop
        while (!getEndConnectionFlag() && !getSocketErrorFlag() && !isInterrupted()) {
            // To end the conversation but keep the connection
            setEndConversation(false);
            // Send greeting
            send(RemoteProtocol.getGreetingCmd()+" this is ReversiByFrankKopp v"+ Reversi.VERSION);
            handleGameRequest();
            waitASec();
        }

        // tell the server that we have finished the conversation
        _server.connectionClosed();

        // terminate the thread
        nullConnectionThread();

        log("Connection ended!");
    }

    /**
     * Handles the phase where the server expects a game request from the client
     */
    private void handleGameRequest() {

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("Game Request");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly.");
                setEndConnection(true);
            }
            // The client requested a new game
            else if (RemoteProtocol.isGameRequestCmd(msgIn)) {
                log("Game request received!");
                RemoteGameRequest gr;
                try {
                    gr = RemoteProtocol.parseGameRequest(msgIn);
                } catch (ProtocolException e) {
                    log("Malformed game request command received!");
                    send(RemoteProtocol.getUnknownCommandCmd());
                    break; // do not continue this conversation phase
                }
                // Ask the Playroom to tell the user that a new game request is pending and that
                // the user must decide what to do.
                // NEW_GAME 8 0 15000 15000 1 3 "WEISS   (v12)"
                // The playroom blocks all other game requests until we acually start a game.
                // Therefore we must chancel this game request when we do not start the game.
                switch (Reversi.getPlayroom().newGameRequestFromRemoteClient(gr)) {
                    case 0: // there is already a game running
                        log("Game request refused - BUSY!");
                        send(RemoteProtocol.getRefuseBusyCmd());
                        break;
                    case -1: // user denied request
                        log("Game request refused - DENIED!");
                        send(RemoteProtocol.getRefuseDeniedCmd());
                        break;
                    case 1: // request accepted as received
                        log("Game request accepted!");
                        handleNewGame(gr);
                        break;
                }
                break; // do not continue this conversation phase
            }
            else {
                baseMsgParsing(msgIn);
            }
        }
    }

    /**
     * handles the phase wehere teh server offers a new game to the client
     * @param gr
     */
    private void handleNewGame(RemoteGameRequest gr) {

        log("Send new game command!");
        send(RemoteProtocol.getNewGameCmd(gr));

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("New game accepted");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                // The playroom blocks all other game requests until we acually start a game.
                // Therefore we must chancel this game request when we do not start the game.
                Reversi.getPlayroom().cancelRemoteGameReguest();
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly.");
                setEndConnection(true);
            // New game has been accepted
            } else if (RemoteProtocol.isNewGameAcceptedCmd(msgIn)) {
                log("New game accepted received!");
                handleStartGame(gr);
                break; // do not continue this conversation phase
            } else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                // The playroom blocks all other game requests until we acually start a game.
                // Therefore we must chancel this game request when we do not start the game.
                Reversi.getPlayroom().cancelRemoteGameReguest();
                break; // do not continue this conversation phase
            }
        }
    }

    /**
     * Handles the start game phase with the client
     * @param gr
     */
    private void handleStartGame(RemoteGameRequest gr) {

        log("Send start game command!");
        send(RemoteProtocol.getStartGameCmd());

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("Start game confirmation");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                Reversi.getPlayroom().cancelRemoteGameReguest();
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly.");
                setEndConnection(true);
            }
            // Start game confirmed
            else if (RemoteProtocol.isStartGameConfirmCmd(msgIn)) {
                log("Start game confirmed!");
                handleGame(gr);
                break; // do not continue this conversation phase
            }
            else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                // The playroom blocks all other game requests until we acually start a game.
                // Therefore we must chancel this game request when we do not start the game.
                Reversi.getPlayroom().cancelRemoteGameReguest();
                break; // do not continue this conversation phase
            }
        }
    }

    /**
     * Handles the game phase with the client
     * @param gr
     */
    private void handleGame(RemoteGameRequest gr) {

        GameAction oldAction;

        // Start the players and the Playroom
        prepareGame(gr);

        // Loop until interrupted or conversation ended
        while (continueConversation()) {

            // We must synchronize our actions with the game thread

            // Wait for a new game status
            getWriteGameActionLock().lock();
            try {

                while (getGameAction().equals(GameAction.NONE)) {
                    try {
                        log("gameAction (WAIT)");
                        getGameActionChangeCondition().await();
                    } catch (InterruptedException e) {
                        log("gameAction (WAIT INTERRUPTED):"+e.getMessage());
                        if (!continueConversation()) {
                            log("gameAction (STOPPED)");
                            stopPlayroom();
                            return;
                        }
                    }
                }

                // now optain read lock and release write lock
                getReadGameActionLock().lock();
                getWriteGameActionLock().unlock();

                log("gameAction (PROCESSING: "+getGameAction().toString()+')');
                switch(getGameAction()) {
                    case LOCAL_MOVE: // send local move to client
                        sendLocalMove();
                        break;
                    case GET_MOVE: // ask client for move
                        getMoveFromClient();
                        break;
                    case GAME_STOPPED: // game has been stopped
                        sendEnd();
                        setEndConnection(true);
                        break;
                    case GAME_OVER: // the game is over
                        if (getCurrentGame().getLastPlayer().equals(getLocalPlayer())) {
                            sendLocalMove();
                        }
                        sendGameOver();
                        setEndConnection(true);
                        break;
                }
                oldAction=getGameAction();

            } finally {
                getReadGameActionLock().unlock();
            }

            finishedGameUpdateProcess(oldAction);

        }
        stopPlayroom();
        log("conversation ended");
    }

    /**
     * Sends a local move to the client and expects a confirmation
     */
    private void sendLocalMove() {

        log("Send move from local player to client: " + getCurrentGame().getCurBoard().getLastMove().toString());
        send(RemoteProtocol.getSendMoveCmd(getCurrentGame().getCurBoard().getLastMove()));

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("Move confirmation");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly.");
                setEndConnection(true);
            }
            else if (RemoteProtocol.isMoveConfirmCmd(msgIn)) {
                log("Move confirmed!");
                break; // do not continue this conversation phase;
            }
            else if (RemoteProtocol.isIllegalMoveCmd(msgIn)) {
                log("Client sent IllegalMoveCmd - this should not happen - end conversation!");
                setEndConnection(true);
            }
            else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                stopPlayroom();
                break; // do not continue this conversation phase
            }
        }
    }

    private void sendGameOver() {

        log("Send game over to client");

        final int          tmpWinnerStatus = getCurrentGame().getGameWinnerStatus();
        final ReversiColor winner;
        
        if      (tmpWinnerStatus ==  0) {
            winner = ReversiColor.NONE;
        } else if (tmpWinnerStatus == -1) {
            winner = ReversiColor.BLACK;
        } else if (tmpWinnerStatus ==  1) {
            winner = ReversiColor.WHITE;
        } else {
            throw new RuntimeException("Invalid Winner");
        }

        send(RemoteProtocol.getGameOverCmd(winner));

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("Game Over confirmation");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                stopPlayroom();
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly.");
                setEndConnection(true);
            }
            else if (RemoteProtocol.isGameOverConfirmCmd(msgIn)) {
                log("Game Over confirmed!");
                break; // do not continue this conversation phase
            }
            else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                stopPlayroom();
                break; // do not continue this conversation phase
            }
        }
    }


    /**
     * Gets a move from the client
     */
    private void getMoveFromClient() {

        log("Ask client for move");
        send(RemoteProtocol.getGetMoveCmd());

        while (continueConversation()) {

            // read a line from the client
            String msgIn = receive("Move from client");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                log("Conversation ended while waiting for move.");
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Client closed connection unexpectetly while waiting for move.");
                setEndConnection(true);
            }
            // The client sent a move
            else if (RemoteProtocol.isMoveCmd(msgIn)) {
                log("Move from client: " + msgIn);
                Move move = RemoteProtocol.parseMove(msgIn);
                // Check if move is legal here to be able to react accordingly
                // This will also be checked in Game.nextMove()
                if (getCurrentGame().getCurBoard().isLegalMove(move)) {
                    // We have a move, so give it back to the player
                    ((ServerPlayer)getRemotePlayer()).setMove(move);
                    break; // do not continue this conversation phase
                } else {
                    // If we got an illegal move we sent the IllegalMove command and wait for another move
                    send(RemoteProtocol.getIllegalMoveCmd());
                }
                
            }
            else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                stopPlayroom();
                break; // do not continue this conversation phase
            }
        }
    }

    /**
     * Parses received message for base commands like END, NOOP, UNKNOWN_CMD and unknown commandd.
     * Sets EndConnection when received END command or received UNKNOWN_CMD.
     * @param msgIn
     */
    private void baseMsgParsing(String msgIn) {
        // End command
        if (RemoteProtocol.isEndCmd(msgIn)) {
            log("End conversation request received!");
            sendEndConfirm();
            setEndConnection(true);
        }
        // NOOP (no operation)
        else if (RemoteProtocol.isNoopCmd(msgIn)) {
            log("NOOP request received!");
            send(RemoteProtocol.getNoopConfirmCmd());
        }
        // Unknown Command command
        else if (RemoteProtocol.isUnknownCommandCmd(msgIn)){
            log("Client sent \"unkown command received\"! Ending conncection");
            sendEnd();
            setEndConnection(true);
        }
        // Unknown command
        else {
            log("Unkown command received!");
            send(RemoteProtocol.getUnknownCommandCmd());
        }
    }

    /**
     * Creates the server player and starts the Playroom. To synchronize with the Playroom this method waits
     * until the game isactually running. We will be notified from the Game through ModelObservable/ModelObserver
     * via the RemotePlayer by calling gameUpdate().
     * @param gr
     */
    private void prepareGame(RemoteGameRequest gr) {
        // Create the serverPlayer to use for the game
        log("Creating server player");
        try {
            setRemotePlayer(PlayerFactory.createPlayer(
                    PlayerType.SERVER, gr.getLocalPlayerName(), gr.getLocalPlayerColor()));
        } catch (PlayerFactory.PlayerCreationException e) {
            Reversi.criticalError("Couldn't create ServerPlayer. "+e.getMessage());
            setEndConnection(true);
        }
        // Tell the serverPlayer that we (this) handle the connection
        ((ServerPlayer)getRemotePlayer()).setServerConnection(this);

        // Now start the Playroom
        log("Starting Playroom");
        Reversi.getPlayroom().startServerPlayroom(gr, (ServerPlayer)getRemotePlayer());

        // We must wait until the game is actually running.
        // We will be noticed through "gameUpdate()" which is called by the ServerPlayer which is
        // an observer to game.
        getWriteGameActionLock().lock();
        try {
            while (!getGameAction().equals(GameAction.GAME_RUNNING)) {
                try {
                    getGameActionChangeCondition().await();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        } finally {
            getWriteGameActionLock().unlock();
        }

        log("Starting game between local player and server");

        // Get the current game from our ServerPlayer
        setCurrentGame(getRemotePlayer().getCurrentGame());

    }

    /**
     * Stops the Playroom and therefore any running games.
     */
    private void stopPlayroom() {
        log("Stop Playroom");
        Reversi.getPlayroom().stopPlayroom();
    }

    /**
     * Sends a refuse connection message.
     */
    public void refuseConversation() {
        send(RemoteProtocol.getRefuseBusyCmd());
    }

    /**
     * Tells the connection to stop and restart the current conversation.
     * This keeps the connection open.
     */
    public void cancelRemoteGameRequest() {
        log("Cancel remote game request!");
        setEndConversation(true);
    }

    /**
     * This method send an end and waits a bit (1sec) to give
     * the client a chance to get this before we actually close the connection.
     */
    private void sendEnd() {
        send(RemoteProtocol.getEndCmd());
        waitASec();
    }

    /**
     * This method send an end confirmation and waits a bit (1sec) to give
     * the client a chance to get this before we actually close the connection.
     */
    private void sendEndConfirm() {
        send(RemoteProtocol.getEndConfirmCmd());
        waitASec();
    }

    /**
     * Pauses the current thread for a second. Ignores any InterruptedExceptions.
     */
    private static void waitASec() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
