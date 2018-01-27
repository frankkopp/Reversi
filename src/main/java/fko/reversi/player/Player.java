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

import fko.reversi.game.Board;
import fko.reversi.game.Game;
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;

/**
 * <p>This interface defines a Player able to be used together with the
 * GAME class.</p>
 *
 * <p>
 * A player has a NAME, a certain COLOR.
 * A player can have one of the following states:
 * <b>WAITING | THINKING | HAS_MOVE | STOPPED</b>
 * </p>
 *
 * <p>Typically a player will be either a human, a computer or a remote player.</p>
 */
public interface Player extends Runnable {

    // -- possible states of a player --
    int WAITING = 0;
    int THINKING = 1;
    int HAS_MOVE = 2;
    int STOPPED = 3;

    /**
     * Start a player thread
     */
    void startPlayer(Game game);

    /**
     * Stop a player thread
     */
    void stopPlayer();

    /**
     * join()
     */
    void joinPlayerThread();

    /**
     * return next move
     * @param board
     * @return Move
     */
    Move getNextMove(Board board);

    /**
     * returns the player's color
     * @return int
     */
    ReversiColor getColor();

    /**
     * Sets the color for this player.
     * @param color
     */
    void setColor(ReversiColor color);

    /**
     * returns name of player
     * @return String
     */
    String getName();

    /**
     * Returns the current game the player is in
     * @return Game
     */
    Game getCurrentGame();

    /**
     * Sets the name for this player
     */
    void setName(String name);

    /**
     * Implementation of getMove() for to determine the next move
     * @return Move
     */
    Move getMove();

    boolean isWaiting();

    boolean isThinking();

    boolean hasMove();

    boolean isStopped();

    /**
     * Returns the int value of the PlayerType for a given player
     */
    PlayerType getPlayerType();
}
