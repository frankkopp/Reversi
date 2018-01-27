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

package fko.reversi.player.computer.TreeSearch;

import fko.reversi.game.Board;
import fko.reversi.game.Move;

import java.util.List;

/**
 * Analyses a given board and returns an heuristic value for that board
 */
public interface TreeSearchBoardAnalyser {
    /**
     * Analyses the board always as the BLACK player and returns the evaluation value.
     * @param board
     * @return int
     */
    int analyse(TreeSearchBoard board);

    /**
     * returns if board is relativly quiet so that we don't have to care
     * about the horizont problem
     * @param board
     * @return true if board is quiet
     */
    boolean notQuiet(Board board);

    /**
     * sort a list of moves -- the most promising first
     * @param board
     * @param moves
     */
    void sortMoves(Board board, List<Move> moves);

    /**
     * returns string representation of class
     */
    String toString();
}
