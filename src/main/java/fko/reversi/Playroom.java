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

package fko.reversi;

import fko.reversi.game.Game;
import fko.reversi.remote.RemoteGameRequest;
import fko.reversi.game.ReversiColor;
import fko.reversi.mvc.ModelEvents.ModelEvent;
import fko.reversi.mvc.ModelEvents.PlayerDependendModelEvent;
import fko.reversi.mvc.ModelObservable;
import fko.reversi.player.*;
import fko.reversi.remote.server.RemotePlayerServer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

/**
 * <p>In the Playroom class the actual games are handled (startet, stopped, etc.). It
 * is able to play a given number of games in a row.</p>
 *
 * <p>This implementation actually only handles one game at a certain time. For this
 * playable version this makes sense as the UI is currently only able to present
 * one game at a time in a meaningful way.</p>
 *
 * <p>For self training or a different implementation of a UI a different
 * Playroom will be necessary.</p>
 *
 * <p>The playroom is also the main observable (Model) for the gui. When the playroom properties change
 * the observers will be notified.</p>
 *
 * <p>The playroom is a singleton and can not be instantiated - use getInstance()</p>
 *
 * <p>When a Playroom is instantiated the first time it checks if a RemoteServer shall be started
 * and if so starts it.<br/>
 * As it could take a while start up the server socket the RemoteServer calls
 * serverStartupCallback(boolean) when it has finished trying to start to
 * indicate if this succeeded.</p>                               
 *
 * <p>To handle games with a remote server the Playroom class also takes care to create remote players
 * which connect to a server. This connection is build first, then a game is created and the remote player
 * asks the server for a new game with the specified parameters. If accepted we start the game and play
 * as if the remote player would be a local player. After the game the Playroom class makes sure the
 * conversation with the server is ended and closed.</p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class Playroom extends ModelObservable implements Runnable {

    // -- Singleton instance --
    private static final Playroom _instance = new Playroom();

    // -- the playroom runs in a separate thread --
    private volatile Thread _playroomThread = null;

    // -- status of the playroom --
    private volatile boolean _isPlaying = false;

    // -- this version of the playroom can handle exactly one game at a time --
    private volatile Game _game = null;

    // -- this is needed to stop multiple games when the stop signal is occuring between two games --
    private volatile boolean _stopMultipleGames = false;

    // -- get values from properties --
    // -- these variables are needed to successfully start a game --
    private volatile boolean _isTimedGame = Boolean.valueOf(Reversi.getProperties().getProperty("timedGame"));
    private volatile long _timeBlack = 1000 * Integer.parseInt(Reversi.getProperties().getProperty("timeBlack"));
    private volatile long _timeWhite = 1000 * Integer.parseInt(Reversi.getProperties().getProperty("timeWhite"));
    private volatile int _currentLevelBlack = Integer.parseInt(Reversi.getProperties().getProperty("engine.black.searchDepth"));
    private volatile int _currentLevelWhite = Integer.parseInt(Reversi.getProperties().getProperty("engine.white.searchDepth"));
    private volatile int _numberOfGames = Integer.parseInt(Reversi.getProperties().getProperty("numberOfGames"));
    private volatile int _boardDimension = Integer.parseInt(Reversi.getProperties().getProperty("boardDimension"));
    private volatile PlayerType _playerTypeBlack = PlayerType.valueOf(Reversi.getProperties().getProperty("playerTypeBlack", "1"));
    private volatile String _namePlayerBlack = Reversi.getProperties().getProperty("nameBlackPlayer", "BLACK_PLAYER");
    private volatile PlayerType _playerTypeWhite = PlayerType.valueOf(Reversi.getProperties().getProperty("playerTypeWhite", "1"));
    private volatile String _namePlayerWhite = Reversi.getProperties().getProperty("nameWhitePlayer", "WHITE_PLAYER");

    // remote player server settings
    private RemotePlayerServer _remotePlayerServer = null;
    private volatile boolean _remotePlayerServerEnabled = Boolean.parseBoolean(Reversi.getProperties().getProperty("remotePlayerServer.enabled"));
    private volatile int _remotePlayerServerPort = Integer.parseInt(Reversi.getProperties().getProperty("remotePlayerServer.port"));

    // remote player client settings for a player playing black
    private volatile String _remotePlayerClientBlackServerName = Reversi.getProperties().getProperty("remotePlayerClient.black.server.ip");
    private volatile int _remotePlayerClientBlackServerPort = Integer.parseInt(Reversi.getProperties().getProperty("remotePlayerClient.black.server.port"));

    // remote player client settings for a player playing white
    private volatile String _remotePlayerClientWhiteServerName = Reversi.getProperties().getProperty("remotePlayerClient.white.server.ip");
    private volatile int _remotePlayerClientWhiteServerPort = Integer.parseInt(Reversi.getProperties().getProperty("remotePlayerClient.white.server.port"));

    // -- counters for multiple games --
    private volatile int _currentGameNumber = 0;
    private volatile int _currentWhiteWins = 0;
    private volatile int _currentBlackWins = 0;
    private volatile int _currentDraws = 0;

    // Game request handling
    private final    Object      _gameRequestLock = new Object();
    private volatile boolean     _gameRequestPending = false;
    private volatile boolean     _gameRequestedFromServer = false;
    private volatile boolean     _gameRequestedFromServerWaitingForAnswer = false;
    private volatile boolean     _gameRequestedFromServerAnswer = false;
    private volatile RemoteGameRequest _Remote_gameRequest = null;
    private volatile ServerPlayer _serverPlayer = null;
    private boolean _noMultipleGames = false; // we do not want multiple games with the server

    /**
     * Default constructor is private as we are a singleton.
     */
    protected Playroom() {
        // if remotePlayerServer is configured to be turned on in properties start it here
        _remotePlayerServer = new RemotePlayerServer(_remotePlayerServerPort);
        if (_remotePlayerServerEnabled) {
            startRemotePlayerServer(_remotePlayerServerPort);
        }
    }

    /**
     * Playroom is a Singleton so use getInstance() to get a reference to the instance.<br/>
     * The instance then checks if a RemoteServer shall be started and if so starts it.<br/>
     * As it could take a while start up the server the RemoteServer calls
     * serverStartupCallback(boolean) when it has finished trying to start to
     * indicate if this succeeded.<br/>
     *
     * @return Playroom instance
     */
    public static Playroom getInstance() {
        return _instance;
    }

    /**
     * Start a new playroom thread to play one or multiple games<br/>
     * The thread then calls run() to actually do the work.
     */
    public void startPlayroom() {
        synchronized (_gameRequestLock) { // synchronize so that the local user cannot start a game during our request
            if ((_gameRequestPending && _gameRequestedFromServer) || _isPlaying) { // there is another request or a running game
                throw new IllegalStateException(
                        "startPlayroom(): Another start request is pending or Playroom already is playing.");
            }
            // There is no other game request and no running game so we process this request
            _gameRequestPending = true;
            _gameRequestedFromServer = false;
            // Now start the thread
            if (_playroomThread == null) {
                _stopMultipleGames = false;
                _playroomThread = new Thread(this, "Playroom");
                _playroomThread.start();
            } else {
                throw new IllegalStateException("startPlayroom(): Playroom thread already exists.");
            }
        }
    }

    /**
     * Start a new playroom thread to play one or multiple games with a remote player through the server<br/>
     * The thread then calls run() to actually do the work.
     */
    public void startServerPlayroom(RemoteGameRequest gr, ServerPlayer sp) {
        synchronized (_gameRequestLock) { // synchronize so that the local user cannot start a game during our request
            if ((_gameRequestPending && !_gameRequestedFromServer) || _isPlaying) { // there is another request or a running game
                throw new IllegalStateException(
                        "startPlayroom(): Another start request is pending or Playroom already is playing.");
            }
            // There is no other game request and no running game so we process this request
            _gameRequestPending = true;
            _gameRequestedFromServer = true;
            // Set the serverPlayer and the game request
            _Remote_gameRequest = gr;
            _serverPlayer = sp;
            // Now start the thread
            if (_playroomThread == null) {
                _stopMultipleGames = false;
                _playroomThread = new Thread(this, "Playroom");
                _playroomThread.start();
            } else {
                throw new IllegalStateException("startServerPlayroom(): Playroom thread already exists.");
            }
        }
    }

    /**
     * Stops the playroom thread and the running game.<br/>
     */
    public synchronized void stopPlayroom() {
        if (_playroomThread==null || !_playroomThread.isAlive() || !_isPlaying) {
            throw new IllegalStateException("stopPlayroom(): Playroom thread is not running");
        }
        // Stopping the game is the only way to stop the playroom thread.
        if (_game != null && _game.isRunningOrPaused()) {
            _game.stopRunningGame();
        }
        _stopMultipleGames = true;
        _playroomThread.interrupt();
    }

    /**
     * Starts one or multiple games.<br/>
     * If multiple games are configured it plays them one after the other in a loop.<br/>
     * It also currently handles the special treatment for RemotePlayers.
     */
    public void run() {

        try { // to finally reset the state of the _playroomThread

            if (Thread.currentThread() != _playroomThread) {
                throw new UnsupportedOperationException("Direct call of Playroom.run() is not supported.");
            }

            // -- set the status to playing --
            _isPlaying = true;

            // Now we are playing, so we don't have a game request pending any more.
            synchronized (_gameRequestLock) {
                _gameRequestPending = false;
                _gameRequestedFromServer = false;
            }

            // -- tell the views that model has changed --
            setChanged();
            notifyObservers(new ModelEvent("PLAYROOM Thread started", SIG_PLAYROOM_THREAD_STARTED));

            _currentGameNumber = 1;
            _currentWhiteWins = 0;
            _currentBlackWins = 0;
            _currentDraws = 0;
            do { // Loop for multiple games

                // Play one game
                playOneGame();

                // If the game has not been created or was stopped instead of ending regularly
                // then break out of the loop
                if (_game==null || _game.isStopped() || _stopMultipleGames) {
                    break;
                }

                // Run GC so that to avoid it during a running game
                //noinspection CallToSystemGC
                System.gc();

            } while (!_noMultipleGames && ++_currentGameNumber <= _numberOfGames); // Loop if we play multiple games in a row

        } catch (Exception e) { // in case anything went wrong we can clean up here
            throw new RuntimeException(e);

        } finally {

            // Reset Game Request
            _Remote_gameRequest = null;
            _serverPlayer = null;

            // Reset game to null
            _game = null;

            // Set status to not playing
            _isPlaying = false;

            // Free _playroomThread so we can startGame a new one --
            _playroomThread = null;

        }

        // Tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM Thread finish",
                SIG_PLAYROOM_THREAD_END));

    }

    /**
     * Prepares and plays one game. Players are created, RemotePlayers are connected and remote games are requested.
     * A game is created and players and game are started. After the game the RemotePlayers are disconnected
     * from the server.
     */
    private void playOneGame() {

        // Create the black player (observer handling is done in createPlayer()
        Player playerBlack = createPlayer(ReversiColor.BLACK);
        assert playerBlack!=null : "Player black may not be null" ;

        // Create the white player (observer handling is done in createPlayer()
        Player playerWhite = createPlayer(ReversiColor.WHITE);
        assert playerWhite!=null : "Player white may not be null" ;

        // If one player is a RemotePlayer then connect to the server and request a game.
        if (playerBlack instanceof RemotePlayer || playerWhite instanceof RemotePlayer) {
            requestGameFromServer(playerBlack, playerWhite);
            if (_Remote_gameRequest ==null) {
                return;
            }
            // ToDo: For now we don't do timed games with remote players
            if (_Remote_gameRequest.isTimedGame()) {
                _Remote_gameRequest.setTimedGame(false);
                System.out.println("Debug: Playroom RemoteGameRequest change to not timed game");
            }
        }

        // Create a new game
        if (_Remote_gameRequest == null) {
            _game = new Game(
                    playerBlack,
                    playerWhite,
                    _boardDimension,
                    _timeBlack,
                    _timeWhite,
                    _isTimedGame
            );
        } else { // If we have a server or remote game request we use the _Remote_gameRequest settings
            _game = new Game(
                    playerBlack,
                    playerWhite,
                    _Remote_gameRequest.getBoardDimension(),
                    _Remote_gameRequest.getBlackTime(),
                    _Remote_gameRequest.getWhiteTime(),
                    _Remote_gameRequest.isTimedGame()
            );
            // we do not want to play multiple games when we have a server game request
            _noMultipleGames = true;
        }

        // Tell the views that model has changed
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM Game created",
                SIG_PLAYROOM_GAME_CREATED));

        // Does the actual game playing with the created players and the created game
        playGame(playerBlack, playerWhite);

        // Disconnect from server in case of player black being a remote server
        if (playerBlack instanceof RemotePlayer) {
            try {
                ((RemotePlayer) playerBlack).disconnectFromServer();
            } catch (IOException e) {
                _stopMultipleGames = true;
                return;
            }
        }

        // Disconnect from server in case of player white being a remote server
        if (playerWhite instanceof RemotePlayer) {
            try {
                ((RemotePlayer) playerWhite).disconnectFromServer();
            } catch (IOException e) {
                _stopMultipleGames = true;
                return;
            }
        }

        // Wait for the threads to finish
        playerBlack.joinPlayerThread();
        playerWhite.joinPlayerThread();

        // If the game ended because it was over (not stopped) we do some logging
        // and we increase the multiple game counters accordingly
        if (_game.isFinished()) {
            if (_game.getGameWinnerStatus() == Game.WINNER_WHITE) {
                _currentWhiteWins++;
            } else if (_game.getGameWinnerStatus() == Game.WINNER_BLACK) {
                _currentBlackWins++;
            } else if (_game.getGameWinnerStatus() == Game.WINNER_DRAW) {
                _currentDraws++;
            }
        }

        // Tell the views that model has changed
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM Game finished",
                SIG_PLAYROOM_GAME_FINISHED));

    }

    /**
     * Does the actual game playing with provided players and the current game object.
     * @param playerBlack
     * @param playerWhite
     */
    private void playGame(Player playerBlack, Player playerWhite) {
        assert playerBlack!=null && playerWhite!=null;
        assert _game!=null && _game.isInitialized();

        // Start players
        playerBlack.startPlayer(_game);
        playerWhite.startPlayer(_game);
        // Start game
        _game.startGameThread();
        // Wait until game is running
        _game.waitUntilRunning();
        // Wait while game is running
        while (_game.isRunningOrPaused()) {
            if (_game.isRunning()) {
                _game.waitWhileRunning();
            }
            if (_game.isPaused()) {
                _game.waitWhileGamePaused();
            }
        }
        // The game is not running any more but we must wait until it had a chance to clean up
        _game.waitUntilGameFinished();
        // Stop the players
        playerBlack.stopPlayer();
        playerWhite.stopPlayer();
        // We must wait for the game thread to complete before starting a new game
        // because other threads (ex. the UI) must get the chance to process the finished game
        // before we eventually startGame a new game
        _game.waitForThreadTermination();
    }

    /**
     * Creates a player. In case of a remote player it also connects it to the server.
     * @return player created - null if player creation failed
     */
    private Player createPlayer(ReversiColor color) {
        assert color.isBlack() || color.isWhite();

        final Player newPlayer;
        // If we have a server game request we might have a player already
        if (_Remote_gameRequest !=null && _serverPlayer != null && _serverPlayer.getColor() == color) {
            newPlayer = _serverPlayer;
        } else {
            // Create a new black player. If this fails we get an exception.
            try {
                if (color==ReversiColor.BLACK) {
                    newPlayer = PlayerFactory.createPlayer(_playerTypeBlack, _namePlayerBlack, ReversiColor.BLACK);
                    setChanged();
                    notifyObservers(new ModelEvent("PLAYROOM Created player Black",
                            SIG_PLAYROOM_CREATED_PLAYER_BLACK));
                } else {
                    newPlayer = PlayerFactory.createPlayer(_playerTypeWhite, _namePlayerWhite, ReversiColor.WHITE);
                    setChanged();
                    notifyObservers(new ModelEvent("PLAYROOM Created player White",
                            SIG_PLAYROOM_CREATED_PLAYER_WHITE));
                }
            } catch (PlayerFactory.PlayerCreationException e) {
                setChanged();
                if (color==ReversiColor.BLACK) {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to create player Black",
                            SIG_PLAYROOM_CREATE_PLAYER_BLACK_FAILED));
                } else {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to create player White",
                            SIG_PLAYROOM_CREATE_PLAYER_WHITE_FAILED));

                }
                throw new RuntimeException("Error creating player.",e);
            }
        }
        return newPlayer;
    }

    /**
     * If one player is a RemotePlayer the player will be connected to the server  and
     * a game request will be sent to the server.
     * @param playerBlack
     * @param playerWhite
     */
    private void requestGameFromServer(Player playerBlack, Player playerWhite) {
        assert playerBlack!=null && playerWhite!=null;

        // Tell the views that model has changed
        setChanged();
        notifyObservers(new PlayerDependendModelEvent("PLAYROOM Request game from server.",
                playerBlack, SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER));

        // First we have no _Remote_gameRequest
        _Remote_gameRequest = null;

        // The remote player must request a new game from the server.
        // Obs: Only one player can be a remote player!!
        if (playerBlack instanceof RemotePlayer) {
            // First connect the player
            if (connectRemotePlayer(playerBlack, ReversiColor.BLACK)) {
                // Create a new RemoteGameRequest based on the current game setting we use for the request
                RemoteGameRequest newRemoteGameRequest = new RemoteGameRequest(
                        _boardDimension, _isTimedGame, _timeBlack, _timeWhite, ReversiColor.WHITE, _playerTypeWhite, _namePlayerWhite );
                // Request a game with this game request and get the game request agreed on.
                if ((_Remote_gameRequest = ((RemotePlayer) playerBlack).requestNewGameFromServer(newRemoteGameRequest)) == null) {
                    // Set game to null so that observers can see that there is/was no game
                    _game = null;
                    _stopMultipleGames = true;
                }
            }
        }
        else if (playerWhite instanceof RemotePlayer) {
            // First connect the player
            if (connectRemotePlayer(playerWhite, ReversiColor.WHITE)) {
                // Create a new RemoteGameRequest based on the current game setting we use for the request
                RemoteGameRequest newRemoteGameRequest = new RemoteGameRequest(
                        _boardDimension, _isTimedGame, _timeBlack, _timeWhite, ReversiColor.BLACK, _playerTypeBlack, _namePlayerBlack );
                // Request a game with this game request and get the game request agreed on.
                if ((_Remote_gameRequest = ((RemotePlayer) playerWhite).requestNewGameFromServer(newRemoteGameRequest)) == null) {
                    // Set game to null so that observers can see that there is/was no game
                    _game = null;
                    _stopMultipleGames = true;
                }
            }
        }

        if (_Remote_gameRequest ==null) {
            // Tell the views that model has changed
            setChanged();
            notifyObservers(new PlayerDependendModelEvent("PLAYROOM Request game from server failed.",
                    playerBlack, SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER_FAILED));
        } else {
            // Tell the views that model has changed
            setChanged();
            notifyObservers(new PlayerDependendModelEvent("PLAYROOM Game request accepted.",
                    playerWhite, SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER_SUCCESSFUL));
        }
    }

    /**
     * This method connects a RemotePlayer to the server.
     * @param newPlayer
     * @param color
     * @return true if successful
     */
    private boolean connectRemotePlayer(Player newPlayer, ReversiColor color) {
        assert newPlayer != null;
        assert color.isBlack()|| color.isWhite();

        // If player is a remote player then connect to server
        if (newPlayer instanceof RemotePlayer) {
            try {
                if (color.isBlack()) {
                    ((RemotePlayer)newPlayer).connectToServer(_remotePlayerClientBlackServerName, _remotePlayerClientBlackServerPort);
                } else {
                    ((RemotePlayer)newPlayer).connectToServer(_remotePlayerClientWhiteServerName, _remotePlayerClientWhiteServerPort);
                }
                return true;
            } catch (UnknownHostException e) {
                Reversi.criticalError("Playroom: " + e.getMessage());
                setChanged();
                if (color==ReversiColor.BLACK) {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to connect player Black to server.",
                            SIG_PLAYROOM_CONNECT_PLAYER_BLACK_FAILED));
                } else {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to connect player White to server.",
                            SIG_PLAYROOM_CONNECT_PLAYER_WHITE_FAILED));
                }
            } catch (IOException e) {
                Reversi.criticalError("Playroom: " + e.getMessage());
                setChanged();
                if (color==ReversiColor.BLACK) {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to connect player Black to server",
                            SIG_PLAYROOM_CONNECT_PLAYER_BLACK_FAILED));
                } else {
                    notifyObservers(new ModelEvent("PLAYROOM Failed to connect player White to server",
                            SIG_PLAYROOM_CONNECT_PLAYER_WHITE_FAILED));

                }
            }
        }
        return false;
    }

    /**
     * Returns if the playroom currently is running a game
     *
     * @return true if the playroom is running a game
     */
    public boolean isPlaying() {
        return _isPlaying;
    }

    /**
     * Starts a remote player server.<br/>
     * As it could take a while start up the server socket the RemoteServer calls
     * serverStartupCallback(boolean) when it has finished trying to start to
     * indicate if this succeded.<br/>
     *
     * @see RemotePlayerServer .startService()
     */
    private void startRemotePlayerServer(int port) {
        assert port>0 && port<65535 : "Not a valid IP port: "+port;
        if (!_remotePlayerServer.isRunning()) {
            _remotePlayerServer = new RemotePlayerServer(port);
            _remotePlayerServer.startService();
        } else {
            throw new IllegalStateException("Start remote server requested although already running!");
        }
    }

    /**
     * Stops a remote player server.
     *
     * @see RemotePlayerServer .stopService()
     */
    private void stopRemotePlayerServer() {
        if (_remotePlayerServer.isRunning()) {
            _remotePlayerServer.stopService();
        }
    }

    /**
     * As it could take a while start up the server socket this is called by the RemoteServer
     * when it has finished trying to start to indicate if this succeded.<br/>
     *
     * @param serverRunning - true when the RemoteServer startup was successful, false otherwise
     */
    public void serverCallback(boolean serverRunning) {
        _remotePlayerServerEnabled = serverRunning;
        // Tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM serverStartupCallback",
                SIG_PLAYROOM_REMOTESERVER_CALLBACK));
    }

    /**
     * Called by the RemotePlayerServer when a remote client asked the RemotePlayerServer to start a new game.
     * - If we are already in the middle of a game we send a busy msg
     * - Playroom tells UI to ask the local user for permission and also prevents local user
     *   to start a new game until he has answered the request.
     * Playroom waits for the answer from the user.
     * (uses a lock to prevent a second startGame request)
     * - If Playroom got ok from UI a game is started with remote player
     * - If Playroom got not ok a denied msg will be sent
     *
     * @return int - 1 if request is accepted and game started, 0 if busy, -1 if user denied
     */
    public int newGameRequestFromRemoteClient(RemoteGameRequest remoteGameRequest) {
        if (remoteGameRequest ==null) {
            throw new IllegalArgumentException("Parameter remoteGameRequest may not be null");
        }

        synchronized (_gameRequestLock) { // synchronize so that the local user cannot start a game during our request
            if (_gameRequestPending || _isPlaying) { // there is another request or a running game
                return 0;
            }
            // We must remember the game request
            _Remote_gameRequest = remoteGameRequest;
            // There is no other game request and no running game so we process this request
            _gameRequestPending = true;
            // The observers need to know that the request came from the server
            _gameRequestedFromServer = true;
            // We must wait for an answer
            _gameRequestedFromServerWaitingForAnswer = true;
            // Tell the views that model has changed --
            setChanged();
            notifyObservers(new ModelEvent("PLAYROOM GameRequestFromRemoteClient",
                    SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST));
            // Now wait for the answer from the user
            while (_gameRequestedFromServerWaitingForAnswer) {
                try {
                    _gameRequestLock.wait();
                } catch (InterruptedException e) {
                    Reversi.criticalError("INTERRUPT during wait for user answer: " + e.getMessage());
                }
            }
            if (_gameRequestedFromServerAnswer) { // accepted
                // reset the flags _Remote_gameRequest* in run()!!
                _gameRequestedFromServerAnswer = false;
                // Tell the views that model has changed --
                setChanged();
                notifyObservers(new ModelEvent("PLAYROOM GameRequestFromRemoteClient - Accepted",
                        SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_ACCEPTED));
                return 1;
            } else { // not accepted
                _gameRequestPending = false;
                _gameRequestedFromServer = false;
                _gameRequestedFromServerWaitingForAnswer = false;
                // Tell the views that model has changed --
                setChanged();
                notifyObservers(new ModelEvent("PLAYROOM GameRequestFromRemoteClient - Denied",
                        SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_DENIED));
                return -1;
            }
        }
    }

    /**
     * Returns the current game request object
     * @return The current game request object
     */
    public RemoteGameRequest getGameRequest() {
        return _Remote_gameRequest;
    }

    /**
     * Getter for _gameRequestPending so that the observers can see we have a game request
     *
     * @return true - if we have a pending request for a new game
     */
    public boolean isGameRequestPending() {
        return _gameRequestPending;
    }

    /**
     * Getter for _gameRequestFromServer so that observers can see we have a game request from the server
     *
     * @return true - if we have a pending request from the RemotePlayerServer for a new game
     */
    public boolean isGameRequestedFromServer() {
        return _gameRequestedFromServer;
    }

    /**
     * Setter to tell the Playroom that we accepted the new game request for the server.<br/>
     * Is usually called be the ui when the user has accepted the remote game request.
     *
     * @param gameRequestedFromServerAnswer
     */
    public void setGameRequestedFromServerAnswer(boolean gameRequestedFromServerAnswer) {
        synchronized (_gameRequestLock) {
            this._gameRequestedFromServerAnswer = gameRequestedFromServerAnswer;
            _gameRequestedFromServerWaitingForAnswer = false;
            _gameRequestLock.notifyAll();
        }
    }

    /**
     * Is called when the user has canceled a remote game request after it has been accepted
     */
    public void userCancelRemoteGameRequest() {
        cancelRemoteGameReguest();
        _remotePlayerServer.cancelRemoteGameRequest();
    }

    /**
     * Chancels a new game request when the game request has been accepted be the server user
     * but the game has never started.
     */
    public void cancelRemoteGameReguest() {
        synchronized (_gameRequestLock) {
            _gameRequestPending = false;
            _gameRequestedFromServer = false;
            _gameRequestedFromServerWaitingForAnswer = false;
        }
        // Tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM GameRequestFromRemoteClient - Canceled",
                SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_CANCELED));
    }

    /**
     * Returns if the playroom is currently waiting for user input due to a remote server game request
     * @return true - if playroom waits for user input
     */
    public boolean isGameRequestedFromServerWaitingForAnswer() {
        return _gameRequestedFromServerWaitingForAnswer;
    }

    /**
     * Returns if the remote player server configured to be enabled.
     * Use get_remotePlayerServerRunning to find out if the server is running
     *
     * @return true if the server should be enabled
     */
    public boolean getRemotePlayerServerEnabled() {
        return _remotePlayerServerEnabled;
    }

    /**
     * Sets the remote server enabled or disabled and starts or stops it accordingly.
     *
     * @param remotePlayerServerEnabled
     */
    public void setRemotePlayerServerEnabled(boolean remotePlayerServerEnabled) {
        if (remotePlayerServerEnabled) {
            startRemotePlayerServer(_remotePlayerServerPort);
            _remotePlayerServerEnabled = true;
        } else {
            stopRemotePlayerServer();
            _remotePlayerServerEnabled = false;
        }
        // -- tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROMM set_remotePlayerServerEnabled",
                SIG_PLAYROOM_SET_REMOTE_PLAYER_SERVER_ENABLED));
    }

    /**
     * Returns the RemoteServerObject
     */
    public RemotePlayerServer getRemotePlayerServer() {
        return _remotePlayerServer;
    }

    /**
     * Returns the current game - maybe null if no game exists
     *
     * @return aGame
     */
    public Game getCurrentGame() {
        return _game;
    }

    /**
     * Returns if new games are timed.<br/>
     * Does not say anything about the current running game.
     *
     * @return true if the current game is timed
     */
    public boolean isTimedGame() {
        return _isTimedGame;
    }

    /**
     * Defines if the next game shall be a timed game or not.
     *
     * @param boolVal
     */
    public void setTimedGame(boolean boolVal) {
        this._isTimedGame = boolVal;
        // Tell the views that model has changed --
        this.setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_isTimedGame",
                SIG_PLAYROOM_SET_IS_TIMED_GAME));
    }

    /**
     * Returns the initial time available for the black player
     *
     * @return time in seconds
     */
    public long getTimeBlack() {
        return _timeBlack;
    }

    /**
     * Sets the initial time available to the black player
     *
     * @param newTimeBlack in seconds
     */
    public void setTimeBlack(long newTimeBlack) {
        if (newTimeBlack<=0) {
            throw new IllegalArgumentException("Parameter newTimeBlack must be > 0. Was " + newTimeBlack);
        }

        this._timeBlack = newTimeBlack;
        // Tell the views that model has changed
        this.setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_timeBlack",
                SIG_PLAYROOM_SET_TIME_BLACK));
    }

    /**
     * Returns the initial time available for the white player
     *
     * @return time in seconds
     */
    public long getTimeWhite() {
        return _timeWhite;
    }

    /**
     * Sets the initial time available to the black player
     *
     * @param newTimeWhite in seconds
     */
    public void setTimeWhite(long newTimeWhite) {
        if (newTimeWhite<=0) {
            throw new IllegalArgumentException("Parameter newTimeWhite must be > 0. Was " + newTimeWhite);
        }

        this._timeWhite = newTimeWhite;
        // Tell the views that model has changed
        this.setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_timeWhite",
                SIG_PLAYROOM_SET_TIME_WHITE));
    }

    /**
     * Returns the current level for a black player engine.
     * The level is currently the identical to the maximal search death of the engine.
     *
     * @return level
     */
    public int getCurrentEngineLevelBlack() {
        return _currentLevelBlack;
    }

    /**
     * Sets the level for a black player engine.
     * The level is currently the identical to the maximal search death of the engine.
     *
     * @param newLevelBlack
     */
    public void setCurrentLevelBlack(int newLevelBlack) {
        if (newLevelBlack<=0) {
            throw new IllegalArgumentException("Parameter newLevelBlack must be > 0. Was " + newLevelBlack);
        }

        this._currentLevelBlack = newLevelBlack;
        // Tell the views that model has changed
        setChanged();
        notifyObservers(new ModelEvent("PLAYROMM set_currentLevelBlack",
                SIG_PLAYROOM_SET_CURRENT_LEVEL_BLACK));
    }

    /**
     * Returns the current level for a white player engine.
     * The level is currently the identical to the maximal search death of the engine.
     *
     * @return level
     */
    public int getCurrentEngineLevelWhite() {
        return _currentLevelWhite;
    }

    /**
     * Sets the level for a white player engine.
     * The level is currently the identical to the maximal search death of the engine.
     *
     * @param newLevelWhite
     */
    public void setCurrentLevelWhite(int newLevelWhite) {
        if (newLevelWhite<=0) {
            throw new IllegalArgumentException("Parameter newLevelWhite must be > 0. Was " + newLevelWhite);
        }

        this._currentLevelWhite = newLevelWhite;
        // Tell the views that model has changed
        setChanged();
        notifyObservers(new ModelEvent("PLAYROMM set_currentLevelWhite",
                SIG_PLAYROOM_SET_CURRENT_LEVEL_WHITE));
    }

    /**
     * Returns the number of games the playroom play in a row.
     *
     * @return number of games
     */
    public int getNumberOfGames() {
        return _numberOfGames;
    }

    /**
     * Sets the number of games the playroom plays in row.
     *
     * @param newNumberOfGames
     */
    public void setNumberOfGames(int newNumberOfGames) {
        if (newNumberOfGames<=0) {
            throw new IllegalArgumentException("Parameter newNumberOfGames must be > 0. Was " + newNumberOfGames);
        }

        this._numberOfGames = newNumberOfGames;
        // Tell the views that model has changed
        setChanged();
        notifyObservers(new ModelEvent("PLAYROMM set_numberOfGames",
                SIG_PLAYROOM_SET_NUMBER_OF_GAMES));
    }

    /**
     * Returns the current board dimension
     *
     * @return number of rows and columns of the current board
     */
    public int getBoardDimension() {
        return _boardDimension;
    }

    /**
     * Sets a new board dimension used for the next game
     *
     * @param newBoardDimension (number of rows and columns on the board)
     */
    public void setBoardDimension(int newBoardDimension) {
        if (newBoardDimension<4 && newBoardDimension%2==0) {
            throw new IllegalArgumentException(
                    "Parameter newBoardDimension must be >= 6 and a multiple of 2. Was " + newBoardDimension);
        }

        this._boardDimension = newBoardDimension;
        // Tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROMM set_boardDimension",
                SIG_PLAYROOM_SET_BOARD_DIMENSION));
    }

    /**
     * Returns the current type of the black player
     *
     * @return player type as defined in the interface Player
     */
    public PlayerType getPlayerTypeBlack() {
        return _playerTypeBlack;
    }

    /**
     * Sets the type of the black player
     *
     * @param newPlayerTypeBlack as defined in the interface Player
     */
    public void setPlayerTypeBlack(PlayerType newPlayerTypeBlack) {
        if (!Arrays.asList(PlayerType.values()).contains(newPlayerTypeBlack)) {
            throw new IllegalArgumentException(
                    "Parameter newPlayerTypeBlack not a valid player type. Was " + newPlayerTypeBlack);
        }

        this._playerTypeBlack = newPlayerTypeBlack;
        // -- tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_playerTypeBlack",
                SIG_PLAYROOM_SET_PLAYER_TYPE_BLACK));
    }

    /**
     * Returns the current type of the white player
     *
     * @return player type as defined in the interface Player
     */
    public PlayerType getPlayerTypeWhite() {
        return _playerTypeWhite;
    }

    /**
     * Sets the type of the white player
     *
     * @param newPlayerTypeWhite as defined in the interface Player
     */
    public void setPlayerTypeWhite(PlayerType newPlayerTypeWhite) {
        if (!Arrays.asList(PlayerType.values()).contains(newPlayerTypeWhite)) {
            throw new IllegalArgumentException(
                    "Parameter newPlayerTypeWhite not a valid player type. Was " + newPlayerTypeWhite);
        }

        this._playerTypeWhite = newPlayerTypeWhite;
        // -- tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_playerTypeWhite",
                SIG_PLAYROOM_SET_PLAYER_TYPE_WHITE));
    }

    /**
     * Returns the current name of the black player
     *
     * @return name of player
     */
    public String getNameBlackPlayer() {
        return _namePlayerBlack;
    }


    /**
     * Sets the name of the black player
     *
     * @param newNameBlackPlayer
     */
    public void setNameBlackPlayer(String newNameBlackPlayer) {
        if (newNameBlackPlayer==null) {
            throw new IllegalArgumentException("Parameter newNameBlackPlayer must not be null.");
        }

        this._namePlayerBlack = newNameBlackPlayer;
        // -- tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_nameBlackPlayer",
                SIG_PLAYROOM_SET_NAME_BLACK_PLAYER));
    }

    /**
     * Returns the current name of the white player
     *
     * @return name of player
     */
    public String getNameWhitePlayer() {
        return _namePlayerWhite;
    }

    /**
     * Sets the name of the white player
     *
     * @param newNameWhitePlayer
     */
    public void setNameWhitePlayer(String newNameWhitePlayer) {
        if (newNameWhitePlayer==null) {
            throw new IllegalArgumentException("Parameter newNameWhitePlayer must not be null.");
        }

        this._namePlayerWhite = newNameWhitePlayer;
        // -- tell the views that model has changed --
        setChanged();
        notifyObservers(new ModelEvent("PLAYROOM set_nameWhitePlayer",
                SIG_PLAYROOM_SET_NAME_WHITE_PLAYER));
    }

    /**
     * Returns the current port for the remote server.
     *
     * @return IP port
     */
    public int getRemotePlayerServerPort() {
        return _remotePlayerServerPort;
    }

    /**
     * Sets the IP port to be used for the server. This setting has no effect on a currently running server.
     * It is only used at server startup. So to change the current port of a running server the server
     * has to be stopped and restarted.
     *
     * @param remotePlayerServerPort - port for accepting incoming connections
     */
    public void setRemotePlayerServerPort(int remotePlayerServerPort) {
        if (remotePlayerServerPort > 0 && remotePlayerServerPort < 65535) {
            this._remotePlayerServerPort = remotePlayerServerPort;
        } else {
            throw new IllegalArgumentException("Not a valid IP port: " + remotePlayerServerPort);
        }
    }

    /**
     * Returns the hostname configured for the RemotePlayerClientConnection when the player is playing black
     *
     * @return String The RemotePlayerClientConnection uses to connect to the RemotePlayerServer
     */
    public String getRemotePlayerClientBlackServerName() {
        return _remotePlayerClientBlackServerName;
    }

    /**
     * Sets the hostname for the server the RemoteClient shall use  when the player is playing black.
     *
     * @param newServerName The hostname the RemoteClient shall use.
     */
    public void setRemotePlayerClientBlackServerName(String newServerName) {
        if (newServerName==null) {
            throw new IllegalArgumentException("Parameter newServerName must not be null.");
        }

        this._remotePlayerClientBlackServerName = newServerName;
    }

    /**
     * Returns the hostname configured for the RemotePlayerClientConnection when the player is playing white
     *
     * @return The RemotePlayerClientConnection uses to connect to the RemotePlayerServer
     */
    public String getRemotePlayerClientWhiteServerName() {
        return _remotePlayerClientWhiteServerName;
    }

    /**
     * Sets the hostname for the server the RemoteClient shall use  when the player is playing white.
     *
     * @param newServerName The hostname the RemoteClient shall use.
     */
    public void setRemotePlayerClientWhiteServerName(String newServerName) {
        if (newServerName==null) {
            throw new IllegalArgumentException("Parameter newServerName must not be null.");
        }

        this._remotePlayerClientWhiteServerName = newServerName;
    }

    /**
     * Returns the IP port configured for the RemotePlayerClientConnection when the player is playing black
     *
     * @return IP port the RemotePlayerClientConnection uses to connect to the RemotePlayerServer
     */
    public int getRemotePlayerClientBlackServerPort() {
        return _remotePlayerClientBlackServerPort;
    }

    /**
     * Sets the IP port for the RemotePlayerClientConnection  when the player is playing black
     * @param port - port to connect the remote server
     */
    public void setRemotePlayerClientBlackServerPort(int port) {
        if (port > 0 && port < 65535) {
            _remotePlayerClientBlackServerPort = port;
        } else {
            throw new IllegalArgumentException("Not a valid IP port: " + port);
        }
    }

    /**
     * Returns the IP port configured for the RemotePlayerClientConnection  when the player is playing white
     *
     * @return IP port the RemotePlayerClientConnection uses to connect to the RemotePlayerServer
     */
    public int getRemotePlayerClientWhiteServerPort() {
        return _remotePlayerClientWhiteServerPort;
    }

    /**
     * Sets the IP port for the RemotePlayerClientConnection when the player is playing white
     * @param port - port to connect to the remote server
     */
    public void setRemotePlayerClientWhiteServerPort(int port) {
        if (port > 0 && port < 65535) {
            _remotePlayerClientWhiteServerPort = port;
        } else {
            throw new IllegalArgumentException("Not a valid IP port: " + port);
        }
    }

    /**
     * Returns the number of the current game.
     * Usefull when multiple games are played in a row.
     *
     * @return number of current game
     */
    public int getCurrentGameNumber() {
        return _currentGameNumber;
    }

    /**
     * Returns the number of wins of the white player.
     * Usefull when multiple games are played in a row.
     *
     * @return number of white wins
     */
    public int getCurrentWhiteWins() {
        return _currentWhiteWins;
    }

    /**
     * Returns the number of wins of the black player.
     * Usefull when multiple games are played in a row.
     *
     * @return number of black wins
     */
    public int getCurrentBlackWins() {
        return _currentBlackWins;
    }

    /**
     * Returns the number of draws
     * Usefull when multiple games are played in a row.
     *
     * @return number of draws
     */
    public int getCurrentDraws() {
        return _currentDraws;
    }

    /**
     * Clean up playroom and call Reversi.exit()
     */
    public void exitReversi() {
        // Tell the remotePlayerServer to stop
        stopRemotePlayerServer();
        // Tell the current game to stop
        if (_playroomThread!=null && _playroomThread.isAlive()) {
            stopPlayroom();
        }
        // Call the exit mothod of Reversi to let that class also do some cleanup
        Reversi.exitReversi();
    }

    public static final int SIG_PLAYROOM_THREAD_STARTED = 1000;
    public static final int SIG_PLAYROOM_CREATED_PLAYER_BLACK = 1010;
    public static final int SIG_PLAYROOM_CREATED_PLAYER_WHITE = 1020;
    public static final int SIG_PLAYROOM_CREATE_PLAYER_BLACK_FAILED = 1030;
    public static final int SIG_PLAYROOM_CONNECT_PLAYER_BLACK_FAILED = 1032;
    public static final int SIG_PLAYROOM_CREATE_PLAYER_WHITE_FAILED = 1040;
    public static final int SIG_PLAYROOM_CONNECT_PLAYER_WHITE_FAILED = 1042;
    public static final int SIG_PLAYROOM_GAME_CREATED = 1050;
    public static final int SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER = 1055;
    public static final int SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER_FAILED = 1056;
    public static final int SIG_PLAYROOM_REQUEST_GAME_FROM_SERVER_SUCCESSFUL = 1057;
    public static final int SIG_PLAYROOM_GAME_FINISHED = 1060;
    public static final int SIG_PLAYROOM_THREAD_END = 1070;
    public static final int SIG_PLAYROOM_SET_IS_TIMED_GAME = 1080;
    public static final int SIG_PLAYROOM_SET_TIME_BLACK = 1090;
    public static final int SIG_PLAYROOM_SET_TIME_WHITE = 1100;
    public static final int SIG_PLAYROOM_SET_CURRENT_LEVEL_BLACK = 1110;
    public static final int SIG_PLAYROOM_SET_CURRENT_LEVEL_WHITE = 1120;
    public static final int SIG_PLAYROOM_SET_NUMBER_OF_GAMES = 1130;
    public static final int SIG_PLAYROOM_SET_BOARD_DIMENSION = 1140;
    public static final int SIG_PLAYROOM_SET_PLAYER_TYPE_BLACK = 1150;
    public static final int SIG_PLAYROOM_SET_NAME_BLACK_PLAYER = 1160;
    public static final int SIG_PLAYROOM_SET_PLAYER_TYPE_WHITE = 1170;
    public static final int SIG_PLAYROOM_SET_NAME_WHITE_PLAYER = 1180;
    public static final int SIG_PLAYROOM_SET_REMOTE_PLAYER_SERVER_ENABLED = 1190;
    public static final int SIG_PLAYROOM_REMOTESERVER_CALLBACK = 1200;
    public static final int SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST = 1210;
    public static final int SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_ACCEPTED = 1220;
    public static final int SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_DENIED = 1230;
    public static final int SIG_PLAYROOM_REMOTESERVER_GAMEREQUEST_CANCELED = 1240;

}