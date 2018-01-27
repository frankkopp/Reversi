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
import fko.reversi.mvc.ModelEvents.ModelEvent;

/**
 * <p>GPL Disclaimer</p>
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
 * <hr/>
 *
 * A player representing a human usually interacting with a user interface.
 * It provides methods to ask for a move, check if a move is wanted and also a method to receive
 * a move and to give it back to the caller.
 * The caller usual calls getMove() requesting a user move. getMove() sets the class' state to
 * wantsMove()==true and signals its observers that it is awaiting a move.
 * getMove() then blocks in a wait() until it gets a move through a call to setMove(Move).
 */
public class HumanPlayer extends AbstractPlayer {

    private volatile boolean _wantsMove = false;
    private volatile boolean _hasMove = false;
    private volatile Move    _move = null;

    // lock for use instead of "this"
    private final Object _lock = new Object();

    /**
     * This constructor is protected to indicate to use the PlayerFactory to create
     * a new player of this kind
     * @param game - a back reference to the game the player plays in
     * @param name - the name of the player
     * @param color - the color the player has in the current game
     */
    protected HumanPlayer(Game game, String name, ReversiColor color) {
        super(game, name, color);
    }

    /**
     * This constructor is protected to indicate to use the PlayerFactory to create
     * a new player of this kind
     * @param name - the name of the player
     * @param color - the color the player has in the current game
     */
    protected HumanPlayer(String name, ReversiColor color) {
        super(name, color);
    }

    /**
     * Returns the int value of the PlayerType for a given player
     */
    public PlayerType getPlayerType() {
        return PlayerType.HUMAN;
    }

    /**
     * Implementation of getMove() to determine the next move. For a human player we must
     * do a little extra work in here. First we set a "wantsMove" flag and notify the observers.
     * Usually at least one observer is the gui and there the gui now knows we want a new move
     * from the user.
     * This method checks if we have got a move (setMove) and if not waits until we have one.
     * You can interrupt the wait for a move by telling the player to stop (player.stop())
     * and notifying the current thread
     * @return next move
     */
    public Move getMove() {
        synchronized (_lock) {
            // indicate that we want to get a move
            _wantsMove = true;
            setChanged();
            notifyObservers(new ModelEvent("HumanPlayer: wantsMove",
                    SIG_HUMAN_PLAYER_WANTS_MOVE));
            // test if we are stopped or have a move and wait otherwise
            while (!_hasMove) {
                try {
                    _lock.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
                // game is over or stopped
                if (this.isStopped()) {
                    // game is over or stopped
                    _hasMove = false;
                    _wantsMove = false;
                    // tell the observers that we don't need a move anymore
                    setChanged();
                    notifyObservers(new ModelEvent("HumanPlayer: player stopped",
                            SIG_HUMAN_PLAYER_PLAYER_STOPPED));
                    return null;
                }
            }
            // we have a move so reset _hasMove and return the move
            _hasMove = false;
            return _move;
        }
    }

    /**
     * Is the HumanPlayer waiting for a move?
     * @return yes if a move is expected via setMove(Move m)
     */
    public boolean wantsMove() {
    	return _wantsMove;
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
            // tell the observers that we don't need a move anymore
            setChanged();
            notifyObservers(new ModelEvent("HumanPlayer: hasMove",
                    SIG_HUMAN_PLAYER_HAS_MOVE));
            // tell getMove() that we have a move and that is should come back from wait()
            _lock.notifyAll();
        }
    }

    // message for the observers
    public static final int SIG_HUMAN_PLAYER_WANTS_MOVE = 3000;
    public static final int SIG_HUMAN_PLAYER_HAS_MOVE = 3010;
    public static final int SIG_HUMAN_PLAYER_PLAYER_STOPPED = 3020;

}
