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

package fko.reversi.remote;

import fko.reversi.Reversi;
import fko.reversi.game.Game;
import fko.reversi.mvc.ModelEvents.ModelEvent;
import fko.reversi.player.Player;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p/>
 * The AbstractReversiRemoteConnection class implements all shared functionalities needed by a RemoteConnection.
 * This class is extended by RemotePlayerServerConnection and RemotePlayerClientConnection.
 * </p>
 * <p/>
 * Discription: Todo
 * </p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public abstract class AbstractReversiRemoteConnection implements Runnable {

    // A remote connection is handled in a seperate thread
    private Thread _connectionThread = null;

    // Reference to the current game
    private Game _currentGame = null;

    // The server player back reference
    private Player _remotePlayer = null;

    // Reference to the local player
    private Player _localPlayer = null;

    // Indicators to control the actual conversation with the client
    private boolean _endConnectionFlag = false;
    private boolean _endConversationFlag = false;
    private boolean _socketErrorFlag = false;

    // The Socket we representing the remote connection
    private SocketChannel _socketChannel = null;

    // For handling the SocketChannel
    private final Charset _charset;
    private final CharsetEncoder _encoder;
    private final CharsetDecoder _decoder;
    private final ByteBuffer _buffer;
    private Selector _selector = null;

    public static final byte LINE_SEPERATOR = Character.LINE_SEPARATOR;

    // Logger for the connection
    private Logger _logger = null;
    private String _logPrefix = "AbstractReversiRemoteConnection";

    /**
     * Is used for gameAction (getGameAction/setGameAction) to indicate the game status reported through gameUpdate().
     * These action signals are used during a game to synchronize the conversation between the local game and
     * the remote player.
     */
    protected static enum GameAction {
        NONE,           // No specific game action defined
        GAME_RUNNING,   // Is set when the game started running
        LOCAL_MOVE,     // We have a new local move that we must then send to the client
        GET_MOVE,       // We must ask the client for a move
        PASS_MOVE,      // We have a pass move ???
        GAME_OVER,      // The game is over - tell the client and close connection
        GAME_STOPPED,   // The game has been stopped - tell the client and close connection
        GAME_FINISHED   // The game thread has finished running

    }

    private volatile GameAction _gameAction = GameAction.NONE;
    // A ReadWriteLock for better locking options than synchronize
    private ReentrantReadWriteLock _rwGameActionLock = new ReentrantReadWriteLock();
    private Lock _readGameActionLock = _rwGameActionLock.readLock();
    private Lock _writeGameActionLock = _rwGameActionLock.writeLock();
    private Condition _gameActionChange = _writeGameActionLock.newCondition();

    // Simple lock to be able to wait for the update from the Observable to be processed
    private volatile boolean _gameUpdateProcessFinished = false;
    private final Object _gameUpdateProcessLock = new Object();


    protected AbstractReversiRemoteConnection() {
        // Set the charset and en-/decoders for handling the SocketChannel reads and writes
        _charset = Charset.defaultCharset();
        _buffer = ByteBuffer.allocate(512);
        _encoder = _charset.newEncoder();
        _decoder = _charset.newDecoder();
    }

    /**
     * Takes over the conversation with the remote player in a separate thread.
     */
    public synchronized void startConnection() {
        if (_connectionThread == null) {
            log("Start Connection request.");
            _connectionThread = new Thread(this, _logPrefix);
            _connectionThread.setPriority(Thread.MIN_PRIORITY);
            _connectionThread.start();
        }
        else {
            Reversi.criticalError("Start connection requested although connection thread already running!");
        }
    }

    /**
     * Stops the conversation with the remote player by interrupting the thread
     */
    public synchronized void stopConnection() {
        if (_connectionThread != null) {
            log("Stop Connection request.");
            _connectionThread.interrupt();
        }
        else {
            throw new IllegalStateException("Stop connection requested although no connection thread exists!");
        }

    }

    /**
     * Waits for the connection thread to die.
     *
     * @throws InterruptedException if another thread has interrupted
     *                              the current thread.  The <i>interrupted status</i> of the
     *                              current thread is cleared when this exception is thrown.
     */
    public void join() throws InterruptedException {
        _connectionThread.join();
    }

    /**
     * Checks if connection is running (connection thread is alive).
     * If the socket for this connection is closed outside this class but the thread is
     * still alive this method might still return true:
     *
     * @return true - if connection thread is alive
     */
    public boolean isConnected() {
        return _socketChannel.isConnected() && _connectionThread != null && _connectionThread.isAlive();
    }

    /**
     * Used so the implementing subclasses can null the thread at the end of run();
     */
    protected void nullConnectionThread() {
        _connectionThread = null;
    }

    /**
     * Used to check if the connection thread is interrupted
     */
    protected boolean isInterrupted() {
        return _connectionThread.isInterrupted();
    }

    /**
     * Tells the Connection that the server player has been stopped.
     */
    public void endConversation() {
        log("End conversation requested!");
        _endConversationFlag = true;
    }

    /**
     * Is called by the player (ServerPlayer) to indicate that we need a move from the remote client.
     */
    public void getMove(Game currentGame) {
        try {
            _writeGameActionLock.lock();
            log("gameAction (GET_MOVE)");
            _gameAction = GameAction.GET_MOVE;
            _gameActionChange.signalAll();
        } finally {
            _writeGameActionLock.unlock();
        }
    }

    /**
     * Tells the remote player server that the game has been updated. This method is used to synchronize the
     * Game and the conversation with the client.<br/>
     * This is done using ModelObservable on Game calling upgate() on ServerPlayer (ModelObserver).
     *
     * @param event - the ModelEvent sent by Game (ModelObservable)
     */
    public synchronized void gameUpdate(ModelEvent event) {

        log("New game status: " + event);

        // Some calls to this method should be blocked until we processed the event.
        boolean waitToProcess = false;
        synchronized (_gameUpdateProcessLock) {
            _gameUpdateProcessFinished = false;
        }

        try {
            _writeGameActionLock.lock(); // lock until parsing finished
            if (event.signals(Game.SIG_GAME_MOVE_MADE)) {
                // We must send the move if it came from the local player
                if (_currentGame.getLastPlayer().equals(_localPlayer)) {
                    log("gameAction (LOCAL_MOVE)");
                    _gameAction = GameAction.LOCAL_MOVE;
                    _gameActionChange.signalAll();
                    waitToProcess = true;
                }
            }
            else if (event.signals(Game.SIG_GAME_GOT_MOVE_FROM_PLAYER)) {
                log("gameAction (IGNORE - GOT_MOVE)");
            }
            else if (event.signals(Game.SIG_GAME_OVER)) {
                log("gameAction (GAME_OVER)");
                _gameAction = GameAction.GAME_OVER;
                _gameActionChange.signalAll();
                waitToProcess = true;
            }
            else if (event.signals(Game.SIG_GAME_WAS_PASS_MOVE)) {
                // We have a pass so we should get a pass notice from the other player (remote server)
                log("gameAction (PASS_MOVE)");
                _gameAction = GameAction.PASS_MOVE;
                _gameActionChange.signalAll();
                waitToProcess = true;
            }
            else if (event.signals(Game.SIG_GAME_STOPPED)) {
                log("gameAction (GAME_STOPPED)");
                _gameAction = GameAction.GAME_STOPPED;
                _gameActionChange.signalAll();
                synchronized (_gameUpdateProcessLock) {
                    _gameUpdateProcessLock.notifyAll();
                }
                waitToProcess = false;
            }
            else if (event.signals(Game.SIG_GAME_RUNNING)) {
                log("gameAction (GAME_RUNNING)");
                _gameAction = GameAction.GAME_RUNNING;
                _gameActionChange.signalAll();
                waitToProcess = true;
            }
            else if (event.signals(Game.SIG_GAME_FINISHED)) {
                log("gameAction (GAME_FINISHED)");
                _gameAction = GameAction.GAME_FINISHED;
                _gameActionChange.signalAll();
                waitToProcess = false;
            }
        } finally {
            _writeGameActionLock.unlock();
        }

        // Wait until we have processed this game update
        if (waitToProcess) {
            synchronized (_gameUpdateProcessLock) {
                log("gameUpdate (WAIT: " + _gameAction.toString() + ") ");
                while (!_gameUpdateProcessFinished) {
                    try {
                        _gameUpdateProcessLock.wait();
                    } catch (InterruptedException e) {
                        log("gameUpdate (WAIT INTERRUPTED)");
                        Reversi.criticalError(e.getMessage());
                    }
                }
                log("gameUpdate (CONT: " + _gameAction.toString() + ") ");
            }
        }
    }

    /**
     * Checks if the conversation shall be continued
     *
     * @return true - if no reason to end the conversation exists
     */
    protected boolean continueConversation() {
        // Check one after the other to be able to log each reason to not continue the conversation
        if (_endConversationFlag) {
            log("End conversation requested.");
            return false;
        }
        if (_endConnectionFlag) {
            log("End connection requested.");
            return false;
        }
        if (_socketErrorFlag) {
            log("Socket error while waiting for message!");
            _endConnectionFlag = true;
            return false;
        }
        if (!_socketChannel.isConnected()) {
            log("Socket closed unexpectetly.");
            _endConnectionFlag = true;
            return false;
        }
        if (_connectionThread != null && _connectionThread.isInterrupted()) {
            log("Connection thread interrupted.");
            _endConnectionFlag = true;
            return false;
        }
        return true;
    }

    /**
     * Tells all waiting threads that processing the update from the game (ModelObserver)
     * is finished. It also resets the GameAction to NONE.
     *
     * @param finishedAction
     */
    protected void finishedGameUpdateProcess(GameAction finishedAction) {
        // Reset the GameAction
        // ToDo: Should possibly be read locked
        if (!_gameAction.equals(GameAction.GAME_STOPPED)) {
            log("gameAction (RESET: "+ _gameAction.toString()+" --> NONE)");
            resetGameAction(); // --> GameAction.NONE
        }
        // Tell waiting threads that we finished to process the new game status
        synchronized (_gameUpdateProcessLock) {
            log("gameUpdate (GAME_ACTION_DONE: " + finishedAction.toString() + " --> NOTIFYALL)");
            _gameUpdateProcessFinished = true;
            _gameUpdateProcessLock.notifyAll();
        }
    }

    /**
     * Sends a String to the remote client and flushes output.
     *
     * @param s
     */
    protected void send(String s) {
        try {
            _socketChannel.write(_encoder.encode(CharBuffer.wrap(s + LINE_SEPERATOR)));
            log(">>> " + s);
        } catch (CharacterCodingException e) {
            Reversi.criticalError("CharacterCodingException while sending: " + e.getMessage());
            log("ERROR - Could not send: " + s);
            _socketErrorFlag = true;
        } catch (ClosedByInterruptException e) {
            Reversi.criticalError("ClosedByInterruptException while sending: " + e.getMessage());
            log("ERROR - Could not send: " + s);
            _socketErrorFlag = true;
        } catch (AsynchronousCloseException e) {
            Reversi.criticalError("AsynchronousCloseException while sending: " + e.getMessage());
            log("ERROR - Could not send: " + s);
            _socketErrorFlag = true;
        } catch (ClosedChannelException e) {
            Reversi.criticalError("ClosedChannelException while sending: " + e.getMessage());
            log("ERROR - Could not send: " + s);
            _socketErrorFlag = true;
        } catch (IOException e) {
            Reversi.criticalError("IOException while sending: " + e.getMessage());
            log("ERROR - Could not send: " + s);
            _socketErrorFlag = true;
        }
    }

    /**
     * Reads a line from the client.<br/>
     * Blocks until it got a line or the input stream is closed.
     *
     * @return String - line from client
     */
    protected String receive(String s) {
        String msgIn = "";
        log("Waiting for message: " + s);
        try {
            while (continueConversation()) {
                // Select channels which have an accept request.
                // Blocks until a channel has an accept request or the timeout is over
                if (_selector.select(250) == 0) {
                    continue;
                }
                // If we shall stop conversation we break here
                if (!continueConversation()) {
                    return null;
                }
                // Get a Set with all SelectionKeys for channels which or ready for I/O
                // Should only be one as we only have one :)
                Set<SelectionKey> keys = _selector.selectedKeys();
                // We just have a timeout so we loop again
                if (keys.size() != 1) {
                    Reversi.criticalError("We should exactly have 1 SocketChannel ready for I/O");
                }
                // Get the SelectionKey for the channel which is ready for I/O
                Iterator<SelectionKey> i = keys.iterator();
                SelectionKey channelKey = i.next();
                i.remove(); // delete it explicitely from the Set
                // Check if the channel is really readable (has data)
                if (!channelKey.isReadable()) {
                    continue;
                }
                // Get the channel from the key
                SocketChannel mySocketChannel = (SocketChannel) channelKey.channel();
                // Check that this is our only channel
                if (!_socketChannel.equals(mySocketChannel)) {
                    Reversi.criticalError("SocketChannel ready for I/O is not the correct channel");
                }
                // Now read bytes from the SocketChannel
                int bytesRead = mySocketChannel.read(_buffer);
                // If read returns -1 it indicates end of stream, which means the client has disconnected.
                if (bytesRead == -1) {
                    channelKey.cancel();
                    mySocketChannel.close();
                    log("Client has closed connection.");
                    _socketErrorFlag = true;
                    return null;
                }
                // Otherwise decode the bytes to string
                _buffer.flip();
                msgIn = _decoder.decode(_buffer).toString().trim();
                _buffer.clear();
                // Now break the loop
                break;
            } // while continueConversation
        } catch (IOException e) {
            log(e.getMessage());
            _socketErrorFlag = true;
        }

        log("<<< " + msgIn);
        return msgIn;
    }

    /**
     * Set a specific Logger for this connection. If not set only stdout is used.
     *
     * @param logger
     */
    public void setLogger(Logger logger) {
        this._logger = logger;
    }

    /**
     * This string is prefixed to all logging output to stdout.
     *
     * @param logPrefix
     */
    public void setLogPrefix(String logPrefix) {
        this._logPrefix = logPrefix;
    }

    protected Game getCurrentGame() {
        return _currentGame;
    }

    /**
     * Set the current game for this connection. It also sets the local player reference
     * obtainable through getLocalPlayer which is necessarily included in game.
     *
     * @param currentGame
     */
    protected void setCurrentGame(Game currentGame) {
        this._currentGame = currentGame;
        // Get a reference to the local player
        if (_currentGame.getPlayerBlack().getColor() == _remotePlayer.getColor()) {
            _localPlayer = _currentGame.getPlayerWhite();
        }
        else {
            _localPlayer = _currentGame.getPlayerBlack();
        }
    }

    protected Player getRemotePlayer() {
        return _remotePlayer;
    }

    protected void setRemotePlayer(Player remotePlayer) {
        this._remotePlayer = remotePlayer;
    }

    /**
     * Returns the local player reference. The reference will be set through setCurrentGame(). If there is no
     * currentGame set then this returns null.
     *
     * @return the local player if current game is available - null otherwise.
     */
    protected Player getLocalPlayer() {
        return _localPlayer;
    }

    protected boolean getEndConnectionFlag() {
        return _endConnectionFlag;
    }

    protected void setEndConnection(boolean endConnection) {
        this._endConnectionFlag = endConnection;
    }

    protected boolean getEndConversationFlag() {
        return _endConversationFlag;
    }

    protected void setEndConversation(boolean endConversation) {
        this._endConversationFlag = endConversation;
    }

    protected boolean getSocketErrorFlag() {
        return _socketErrorFlag;
    }

    protected void setSocketError(boolean socketError) {
        this._socketErrorFlag = socketError;
    }

    protected SocketChannel getSocketChannel() {
        return _socketChannel;
    }

    protected void setSocketChannel(SocketChannel socketChannel) {
        this._socketChannel = socketChannel;
    }

    protected Selector getSelector() {
        return _selector;
    }

    protected void setSelector(Selector selector) {
        this._selector = selector;
    }

    public Lock getWriteGameActionLock() {
        return _writeGameActionLock;
    }

    public Lock getReadGameActionLock() {
        return _readGameActionLock;
    }

    public Condition getGameActionChangeCondition() {
        return _gameActionChange;
    }

    /**
     * Returns the current GameAction to determine the status of the game.<br/>
     * This should be synchronized through the synchronize lock returned by getReadGameActionLock() when
     * used in a transaction manner..
     * @return GameAction - the current GameAction
     * @see #getReadGameActionLock()
     */
    protected GameAction getGameAction() { return _gameAction; }

    /**
     * Sets a new GameAction to define the status of the game.<br/>
     * Use getGameActionLock() to obtain the lock used to synchronize over GameAction.
     *
     * @param gameAction - the new GameAction
     * @see #getWriteGameActionLock()
     * @see #getReadGameActionLock()
     */
    protected void setGameAction(GameAction gameAction) {
        try {
            _writeGameActionLock.lock();
            this._gameAction = gameAction;
        } finally {
            _writeGameActionLock.unlock();
        }
    }

    /**
     * Resets the GameAction to GameAction.NONE <br/>
     * Uses the a write lock to synchronize. Use careful when in having the read lock while calling this method.
     * The read lock can not be upgraded to a write lock.
     *
     * @see #getWriteGameActionLock()
     * @see #getReadGameActionLock()
     */
    protected void resetGameAction() {
        try {
            _writeGameActionLock.lock();
            this._gameAction = GameAction.NONE;
        } finally {
            _writeGameActionLock.unlock();
        }
    }

    /**
     * Writes to log
     */
    protected void log(Level level, String msg) {
        if (_logger != null) {
            _logger.log(level, msg);
        }
        System.out.printf(level.toString() +
                ' ' + _logPrefix + ": (Thread: " + Thread.currentThread().getName() + "): " + msg + Character.LINE_SEPARATOR);
    }

    /**
     * Writes to log
     */
    protected void log(String msg) {
        if (_logger != null) {
            _logger.log(Level.INFO, msg);
        }
        System.out.printf("INFO: " +
                _logPrefix + ": (Thread: " + Thread.currentThread().getName() + "): " + msg + Character.LINE_SEPARATOR);
    }

}
