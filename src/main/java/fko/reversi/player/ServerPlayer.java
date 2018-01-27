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
import fko.reversi.game.ReversiColor;
import fko.reversi.remote.server.RemotePlayerServerConnection;
import fko.reversi.remote.server.RemotePlayerServerLogger;
import fko.reversi.mvc.ModelEvents.ModelEvent;

import java.util.Observable;
import java.util.logging.Level;

/**
 * <p/>
 * The ServerPlayer class implements a ServerPlayer player which communicates over TCP/IP with
 * a RemotePlayer.
 * </p>
 *
 * @see fko.reversi.remote.server.RemotePlayerServer
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class ServerPlayer extends AbstractPlayer {

    private RemotePlayerServerConnection _serverConnection = null;

    // indicators to signal that we want a move
    private volatile boolean _wantsMove = false;
    private volatile boolean _hasMove = false;
    private volatile Move    _move = null;

    // lock for use instead of "this"
    private final Object _lock = new Object();

    /**
     * Create a RemotePlayer object
     * @param game - the game the player plays in
     * @param name - a name for the player
     * @param color - the color as defined in interface Player for the player
     */
    protected ServerPlayer(Game game, String name, ReversiColor color) {
        super(game, name, color);
    }

    /**
     * Create a RemotePlayer object
     * @param name - a name for the player
     * @param color - the color as defined in interface Player for the player
     */
    protected ServerPlayer(String name, ReversiColor color) {
        super(name, color);
    }

    /**
     * Starts the player in a new thread and and also starts the remote client in a new thread.
     */
    @Override
	public void startPlayer(Game game) {
        ServerPlayer.log("ServerPlayer: Start server player requested.");
        super.startPlayer(game);
    }

    /**
     * Override the stopPlayer() method of AbstractPlayer to stop the remote player and also
     * the remote client.
     */
    @Override
	public void stopPlayer() {
        ServerPlayer.log("ServerPlayer: Stop server player requested.");
        _serverConnection.endConversation();
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
            _serverConnection.getMove(this.getCurrentGame());
            // test if we are stopped or have a move and wait otherwise
            while (!_hasMove) {
                try {
                    _lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                    ServerPlayer.log("ServerPlayer: interrupt during waiting for move (INTERRUPT)");
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
     * Tells the player that the game has received a new move. Typically only interesting if the player did not send
     * the move itself and the player could have missed that he has been passed.
     */
    @Override
	public void update(Observable o, Object arg) {
        super.update(o, arg);
        if (_serverConnection !=null &&_serverConnection.isConnected()) {
            _serverConnection.gameUpdate((ModelEvent)arg);
        }
    }

    /**
     * Returns the int value of the PlayerType for a given player
     */
    public PlayerType getPlayerType() {
        return PlayerType.SERVER;
    }


    /**
     * Is the ServerPlayer waiting for a move?
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
     * Returns the current server connection
     * @return RemotePlayerServerConnection
     */
    public RemotePlayerServerConnection getServerConnection() {
        return _serverConnection;
    }

    /**
     * Set the server connection to use
     * @param serverConnection
     */
    public void setServerConnection(RemotePlayerServerConnection serverConnection) {
        this._serverConnection = serverConnection;
    }

    /**
     * Writes to log
     */
    public static void log(Level level, String msg) {
        RemotePlayerServerLogger.getLogger().log(level, msg);
        System.out.printf(new StringBuilder().append(level.toString())
                .append(' ').append(msg).append(" (Thread: ").append(Thread.currentThread().getName()).append(')')
                .append(Character.LINE_SEPARATOR).toString());
    }

    /**
     * Writes to log
     */
    public static void log(String msg) {
        RemotePlayerServerLogger.getLogger().log(Level.INFO, msg);
        System.out.printf(new StringBuilder().append("INFO: ").append(msg).append(" (Thread: ")
                .append(Thread.currentThread().getName()).append(')').append(Character.LINE_SEPARATOR).toString());
    }
}
