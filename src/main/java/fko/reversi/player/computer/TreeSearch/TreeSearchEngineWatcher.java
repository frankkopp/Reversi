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

import fko.reversi.game.Move;

public interface TreeSearchEngineWatcher {

    /**
     * return the number of possible moves for the current move
     * @return int
     */
    int numberOfPossibleMoves();

    /**
     * return the current move number
     * @return int
     */
    int getCurMoveNumber();

    /**
     * returns the current move in calculation
     * @return Move
     */
    Move getCurMove();


    /**
     * return the current best move
     * @return Move
     */
    Move getMaxValueMove();

    /**
     * returns the current depth in the search tree (without non-quite extra depth)
     * @return int
     */
    int getCurSearchDepth();

    /**
     * returns the current depth in the search tree (with non-quite extra depth)
     * @return int
     */
    int getCurExtraSearchDepth();

    /**
     * return the number of nodes checked so far
     * @return int
     */
    int getNodesChecked();

    /**
     * returns the number of nodes per second for the current calculation
     * @return int
     */
    int getCurNodesPerSecond();

    /**
     * returns the used time for the current move
     * @return long
     */
    long getCurUsedTime();

    /**
     * return the number of boards analysed so far
     * @return int
     */
    int getBoardsChecked();

    /**
     * return the number of non-quiet boards found so far
     * @return int
     */
    int getBoardsNonQuiet();

    /**
     * return the number of cache hits so far
     * @return int
     */
    int getCacheHits();

    /**
     * return the nubmer of cache misses so far
     * @return int
     */
    int getCacheMisses();

    /**
     * return the current cache size
     * @return int
     */
    int getCurCacheSize();

    /**
     * return the current number of boards in cache
     * @return int
     */
    int getCurCachedBoards();
}
