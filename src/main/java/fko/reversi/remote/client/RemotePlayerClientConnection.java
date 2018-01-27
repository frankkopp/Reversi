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

package fko.reversi.remote.client;

import fko.reversi.remote.RemoteGameRequest;
import fko.reversi.game.Move;
import fko.reversi.player.RemotePlayer;
import fko.reversi.remote.AbstractReversiRemoteConnection;
import fko.reversi.remote.ProtocolException;
import fko.reversi.remote.RemoteProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * <p>The RemotePlayerClientConnection class handles the connection to the RemotePlayerServer.</p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class RemotePlayerClientConnection extends AbstractReversiRemoteConnection
{
    // The IP address the client contacts the server
    private InetAddress _serverIP = null;

    // The port the clients contacts the server
    private int _serverPort = -1;

    // Flag to indicate if we already have requested a game
    private boolean _gameRequested = false;

    /**
     * Creates a RemotePlayerClientConnection with a specific IP address and a port
     * @param remotePlayer
     */
    public RemotePlayerClientConnection(final RemotePlayer remotePlayer) {
        super();
        // The back reference to the RemotePlayer
        setRemotePlayer(remotePlayer);
        log("created");
    }

    /**
     * The RemotePlayerClientConnection runs in a seperate thread. A game must be requested from the server
     * before we can successfully start the RemotePlayerClientConnection.
     * @see #requestNewGame(fko.reversi.remote.RemoteGameRequest)
     */
    public void run() {

        log("Started");

        try {
            if (!_gameRequested) {
                throw new IllegalStateException("RemoteConnection has been started without having a game requested");
            } else {
                handleStartGame();
            }
        } finally { // disconnect no matter what happend
            try {
                disconnectFromServer();
            } catch (IOException ignore) { /* ignore */ }
            nullConnectionThread();
        }

        log("Stopped");
    }

    /**
     * Handles the start game conversation part
     */
    private void handleStartGame() {

        while (continueConversation()) {

            // read a line from the client
            final String msgIn = receive("Start game from server.");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                setEndConnection(true);
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                setEndConnection(true);
                log("Server closed connection unexpectetly.");
            } else if (RemoteProtocol.isStartGameCmd(msgIn)) {
                log("Start game from server received.");
                send(RemoteProtocol.getStartGameConfirmCmd());
                handleGame();
                break; // do not continue this conversation phase
            } else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                break; // do not continue this conversation phase
            }

        }
    }

    /**
     * Handles the start game conversation part
     */
    private void handleGame() {

        prepareGame();

        // Now handle the game conversation phase
        while (continueConversation()) {

            // read a line from the client
            final String msgIn = receive("Command from server.");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                setEndConnection(true);
                break; // do not continue this conversation phase
            }

            if (msgIn==null) {
                log("Server closed connection unexpectetly.");
                setEndConnection(true);

            // Move received
            } else if (RemoteProtocol.isMoveCmd(msgIn)) {
                log("Move received: "+msgIn);
                if (awaitGameStatus(GameAction.GET_MOVE)) {
                    log("Sending received move to local game.");
                } else {
                    log("Local game does not expect a move (GameAction: "+getGameAction()+") - ending conncection!");
                    setEndConnection(true);
                    break; // do not continue this conversation phase
                }
                Move move = RemoteProtocol.parseMove(msgIn);
                // Check if move is legal
                if (getCurrentGame().getCurBoard().isLegalMove(move)) {
                    // We have a move, so give it back to the player
                    ((RemotePlayer)getRemotePlayer()).setMove(move);
                    send(RemoteProtocol.getMoveConfirmCmd());
                    finishedGameUpdateProcess(GameAction.GET_MOVE);
                } else {
                    // If we got an illegal move we sent the IllegalMove command and wait for another move
                    send(RemoteProtocol.getIllegalMoveCmd());
                }

            // Move received
            } else if (RemoteProtocol.isGetMoveCmd(msgIn)) {
                log("Move request received.");

                if (awaitGameStatus(GameAction.LOCAL_MOVE)) {
                    log("Local game has move.");
                } else if (awaitGameStatus(GameAction.PASS_MOVE)) {
                    log("Local game has pass.");
                } else {
                    log("Local game does not have a move/pass (GameAction: "+getGameAction()+") - ending conncection!");
                    setEndConnection(true);
                    break; // do not continue this conversation phase
                }

                // If we have a pass we finish this GameAction and wait until LOCAL_MOVE
                if (getGameAction() == GameAction.PASS_MOVE) {
                    finishedGameUpdateProcess(GameAction.PASS_MOVE);
                    if (awaitGameStatus(GameAction.LOCAL_MOVE)) {
                        log("Local game has move.");
                    } else {
                        log("Local game does not have a move/pass (GameAction: "+getGameAction()+") - ending conncection!");
                        setEndConnection(true);
                        break; // do not continue this conversation phase
                    }
                }

                log("Send move from local player to server: "
                        + getCurrentGame().getCurBoard().getLastMove().toString());
                send(RemoteProtocol.getSendMoveCmd(getCurrentGame().getCurBoard().getLastMove()));
                finishedGameUpdateProcess(GameAction.LOCAL_MOVE);


            // Illegal move received
            } else if (RemoteProtocol.isIllegalMoveCmd(msgIn)) {
                log("Illegal move command received - requests move.");
                // TODO

            // Game over received
            } else if (RemoteProtocol.isGameOverCmd(msgIn)) {
                log("Game over received: "+msgIn);
                if (awaitGameStatus(GameAction.GAME_OVER)) {
                    log("Confirming game over.");
                } else {
                    log("Local game does not expect game to be over (GameAction: "+getGameAction()+") - ending conncection!");
                    setEndConnection(true);
                    break; // do not continue this conversation phase
                }
                send(RemoteProtocol.getGameOverConfirmCmd());
                finishedGameUpdateProcess(GameAction.GAME_OVER);
                break; // do not continue this conversation phase

            } else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                setEndConnection(true);
                break; // do not continue this conversation phase
            }

        }
    }

    private boolean awaitGameStatus(final GameAction exptectedGameAction) {
        // Wait for a new game status
        getWriteGameActionLock().lock();
        try {
            while (getGameAction().equals(GameAction.NONE)) {
                try {
                    log("gameAction (WAIT for "+exptectedGameAction+")");
                    getGameActionChangeCondition().await();
                } catch (InterruptedException e) {
                    log("gameAction (WAIT INTERRUPTED):"+e.getMessage());
                    if (!continueConversation()) {
                        log("gameAction (STOPPED)");
                        return false;
                    }
                }
            }
            return getGameAction().equals(exptectedGameAction);
        } finally {
            getWriteGameActionLock().unlock();
        }
    }

    /**
     * Requests a new game with the server.
     */
    public RemoteGameRequest requestNewGame(final RemoteGameRequest gr) {

        log("Request game from server.");
        send(RemoteProtocol.getGameRequestCmd(gr));

        RemoteGameRequest newRemoteGameRequest = null;
        _gameRequested=false;

        while (continueConversation()) {

            // read a line from the client
            final String msgIn = receive("New game from server.");

            // Re-check if we shall continue the conversation after returning from receive()
            if (!continueConversation()) {
                setEndConnection(true);
                newRemoteGameRequest = null;
                break; // do not continue this conversation phase
            }

            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Server closed connection unexpectetly.");
                setEndConnection(true);
            // New game has been accepted
            } else if (RemoteProtocol.isNewGameCmd(msgIn)) {
                log("New game from server received.");
                // TODO: For now we will accept any game from the server as it is the same as we sent.
                // TODO: Later it is planned to be able to accept or deny the actual game sent by the server.
                try {
                    newRemoteGameRequest = RemoteProtocol.parseNewGameCmd(msgIn);
                } catch (ProtocolException e) {
                    log("Malformed new game command received");
                    newRemoteGameRequest =null;
                }
                send(RemoteProtocol.getNewGameAcceptedCmd());
                _gameRequested=true;
                break; // do not continue this conversation phase
            // New game has been denied
            } else if (RemoteProtocol.isRefusedDeniedCmd(msgIn)) {
                log("New game from server refused - server denied game request.");
                endServerConnectionGracefully();
                setEndConnection(true);
            // New game has been refused (busy)
            } else if (RemoteProtocol.isRefusedBusyCmd(msgIn)) {
                log("New game from server refused - server is busy.");
                endServerConnectionGracefully();
                setEndConnection(true);
            // End from server
            } else {
                baseMsgParsing(msgIn);
            }

            // After parsing the received message we check again and clean up
            if (!continueConversation()) {
                newRemoteGameRequest =null;
                break; // do not continue this conversation phase
            }
        }
        return newRemoteGameRequest;
    }

    /**
     * Parses received message for base commands like END, NOOP, UNKNOWN_CMD and unknown commandd.
     * Sets EndConnection when received END command or received UNKNOWN_CMD.
     * @param msgIn
     */
    private void baseMsgParsing(final String msgIn) {
        // End command
        if (RemoteProtocol.isEndCmd(msgIn)) {
            log("End conversation request received!");
            setEndConnection(true);
        }
        // NOOP (no operation)
        else if (RemoteProtocol.isNoopCmd(msgIn)) {
            log("NOOP request received!");
            send(RemoteProtocol.getNoopConfirmCmd());
        }
        // Unknown Command command
        else if (RemoteProtocol.isUnknownCommandCmd(msgIn)) {
            log("Server sent \"unkown command received\"! Ending conncection.");
            send(RemoteProtocol.getEndCmd());
            String r = receive("New game from server.");
            if (RemoteProtocol.isEndConfirmCmd(r)) {
                log("Server confirmed end connection.");
            } else {
                log("Serverdid not confirme end connection - ending anyway.");
            }
            setEndConnection(true);
        // Unknown command
        } else {
            log("Unkown command received!");
            send(RemoteProtocol.getUnknownCommandCmd());
        }
    }

    /**
     * When receiving a start game command we prepare ourselves for the new game.
     * That is waiting until game is running, getting the current game reference and
     * a reference to the local player.
     */
    private void prepareGame() {
        final GameAction oldAction;

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
            oldAction=getGameAction();
        } finally {
            getWriteGameActionLock().unlock();
        }

        // Get the current game from our ServerPlayer
        setCurrentGame(getRemotePlayer().getCurrentGame());

        // Tell the waiting thread that we know that the game is running now
        finishedGameUpdateProcess(oldAction);
    }

    /**
     * Sends end connection command and waits for answer
     */
    public void endServerConnectionGracefully() {
        log("Ending conversation with server.");
        send(RemoteProtocol.getEndCmd());
        // read a line from the client
        final String msgIn = receive("End confirmation from server.");
        // Check if the connection has been closed by client
        if (msgIn==null) {
            log("Server closed connection unexpectetly.");
            // New game has been accepted
        } else if (RemoteProtocol.isEndConfirmCmd(msgIn)) {
            log("Server confirmed ending conversation.");
        }
        else {
            log("Non-confirmation received. Ending connection anyway!");
        }
        try {
            disconnectFromServer();
        } catch (IOException e) {
            // ignore
        }
    }


    /**
     * Connects the RemotePlayerClientConnection to the Server
     * @throws IOException
     */
    public void connectToServer(final InetAddress serverIp, final int serverPort) throws IOException {
        if (getSocketChannel() == null || !getSocketChannel().isConnected()) {

            log("Connecting to server: "+ serverIp + ':' + serverPort);

            _serverIP = serverIp;
            _serverPort = serverPort;

            // Creating and connecting a SocketChannel to the server
            setSocketChannel(SocketChannel.open(new InetSocketAddress(_serverIP, _serverPort)));
            // Put this SocketChannel into nonblocking mode
            getSocketChannel().configureBlocking(false);
            // Create a Selector for the channel
            setSelector(Selector.open());
            // Register the SocketChannel with the Selector
            getSocketChannel().register(getSelector(), SelectionKey.OP_READ);

            // Receive HELLO from server
            String msgIn = receive("HELLO from server.");
            // Check if the connection has been closed by client
            if (msgIn==null) {
                log("Server closed connection unexpectetly.");
                throw new IOException("Server closed connection unexpectetly before receiving HELLO.");
            }
            // The server sent HELLO
            else if (RemoteProtocol.isGreetingCmd(msgIn)) {
                log("Server greeting received!");
            }

            log("Connected to server: "+ serverIp + ':' + serverPort);

        } else {
            log("Connect to server request but already connected!");
            throw new IllegalStateException("Connect to server request but already connected!");
        }
    }

    /**
     * Closes the conenction to the sever
     */
    public void disconnectFromServer() throws IOException {
        if (getSocketChannel().isConnected()) {
            log("Disconnecting from server: "+ _serverIP + ':' + _serverPort);
            getSocketChannel().close();
        } else {
            log("Disconnect from server request but not connected");
            throw new IllegalStateException("Disconnect from server without beeing connected!");
        }
        log("Disconnected from server: "+ _serverIP + ':' + _serverPort);
    }

}
