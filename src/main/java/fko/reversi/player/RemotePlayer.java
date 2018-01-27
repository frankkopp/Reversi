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

package fko.reversi.player;

import fko.reversi.game.Game;
import fko.reversi.game.Move;
import fko.reversi.remote.RemoteGameRequest;
import fko.reversi.game.ReversiColor;
import fko.reversi.mvc.ModelEvents.ModelEvent;
import fko.reversi.remote.client.RemotePlayerClientConnection;
import fko.reversi.remote.client.RemotePlayerClientLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

/**
 * <p>
 * The RemotePlayer class implements a remote player which communicates over TCP/IP with
 * a RemotePlayerServer. <br/>
 * </p>
 *
 * @see fko.reversi.remote.server.RemotePlayerServer
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class RemotePlayer extends AbstractPlayer implements Observer {

    // Logger
    final private RemotePlayerClientLogger _myRemotePlayerClientLogger;

    // The RemotePlayerClientConnection which actually handles the remote connection
    final private RemotePlayerClientConnection _remoteClient;

    // indicators to signal that we want a move
    private volatile boolean _wantsMove = false;
    private volatile boolean _hasMove = false;
    private volatile Move    _move = null;

    // lock for use instead of "this"
    private final Object _lock = new Object();

    /**
     * Create a RemotePlayer object
     * @param name - a name for the player
     * @param color - the color as defined in interface Player for the player
     */
    protected RemotePlayer(String name, ReversiColor color) {
        super(name, color);
        _remoteClient = new RemotePlayerClientConnection(this);
        _myRemotePlayerClientLogger = new RemotePlayerClientLogger(this);
        _remoteClient.setLogger(_myRemotePlayerClientLogger.getLogger());
        _remoteClient.setLogPrefix("RemotePlayerClientConnection");
    }

    /**
     * Starts the player in a new thread and and also starts the remote client in a new thread.
     */
    @Override
	public void startPlayer(Game game) {
        log("Start remote player requested");
        _remoteClient.startConnection();
        super.startPlayer(game);
    }

    /**
     * Override the stopPlayer() method of AbstractPlayer to stop the remote player and also
     * the remote client.
     */
    @Override
	public void stopPlayer() {
        log("Stop remote player requested");
        if (_remoteClient.isConnected()) {
            _remoteClient.stopConnection();
        }
        super.stopPlayer();
    }


    /**
     * Implementation of getMove() for to determine the next move.
     * @return Move
     */
    public Move getMove() {
        synchronized (_lock) {
            // indicate that we want to get a move
            _wantsMove = true;
            // tell the _remoteClient that we want a move
            _remoteClient.getMove(this.getCurrentGame());
            // test if we are stopped or have a move and wait otherwise
            while (!_hasMove) {
                try {
                    _lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                    log("interrupt during waiting for move (INTERRUPT)");
                }
                // game is over or stopped
                if (this.isStopped()) {
                    // game is over or stopped
                    _hasMove = false;
                    _wantsMove = false;
                    return null;
                }
            }
            // we have a move so reset _hasMove and return the move
            _hasMove = false;
            return _move;
        }
    }

    /**
     * Tells the player that the game has a new status.
     */
    @Override
	public void update(Observable o, Object arg) {
        super.update(o, arg);
        _remoteClient.gameUpdate((ModelEvent)arg);
    }

    /**
     * Returns the int value of the PlayerType for a given player
     */
    public PlayerType getPlayerType() {
        return PlayerType.REMOTE;
    }

    /**
     * Is the RemotePlayer waiting for a move?
     * @return yes if a move is expected via setMove(Move m)
     */
    public boolean wantsMove() {
        synchronized (_lock) {
            return _wantsMove;
        }
    }

    /**
     * Set the move and reset the indication that we want a move (wantsMove()==false)
     * @param newMove
     */
    public void setMove(Move newMove) {
        synchronized (_lock) {
            // if we are not waiting we ignore that
            if (!_wantsMove) {
                return;
            }
            _move = newMove;
            _wantsMove = false;
            _hasMove = true;
            // tell getMove() that we have a move and that is should come back from wait()
            _lock.notifyAll();
        }
    }

    /**
     * Tells the RemotePlayerClientConnection to connect to the server
     */
    public void connectToServer(InetAddress ip, int port) throws IOException {
        _remoteClient.connectToServer(ip, port);
    }

    /**
     * Trys to connect the remote player to the server. Returns true if succeded, false otherwise.<br/>
     * Writes fail cause to log.
     * @param host
     * @param port
     */
    public void connectToServer(String host, int port) throws IOException {
        // If player is a RemotePlayer the connect to server now so we can catch any
        // issues during connection here and notify the observers
        // Get the Internet address for the client
        connectToServer(InetAddress.getByName(host), port);
    }


    /**
     * Tells the RemotePlayerClientConnection to disconnect from the server
     */
    public void disconnectFromServer() throws IOException {
        _remoteClient.disconnectFromServer();
    }

    /**
     * Request a new game from the server
     */
    public RemoteGameRequest requestNewGameFromServer(RemoteGameRequest gr) {
        return _remoteClient.requestNewGame(gr);
    }

    /**
     * Writes to log
     */
    public void log(Level level, String msg) {
        _myRemotePlayerClientLogger.getLogger().log(level, msg);
        System.out.printf(new StringBuilder().append(level.toString()).append("RemotePlayer: (Thread: ")
                .append(Thread.currentThread().getName()).append("): ").append(msg).append(Character.LINE_SEPARATOR).toString());
    }

    /**
     * Writes to log
     */
    public void log(String msg) {
        _myRemotePlayerClientLogger.getLogger().log(Level.INFO, msg);
        System.out.printf(new StringBuilder().append("INFO: RemotePlayer: (Thread: ")
                .append(Thread.currentThread().getName()).append("): ").append(msg).append(Character.LINE_SEPARATOR).toString());
    }
}
