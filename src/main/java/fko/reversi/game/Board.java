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

package fko.reversi.game;

import java.util.List;

/**
 * Interface for a Reversi board.
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public interface Board {

    /**
     * A constant defining the default size of a board (8x8)
     */
    int DEFAULT_DIM = 8;

    /**
     * Returns true if the last player is the next player --> pass
     * @return true - if last player is next player
     */
    boolean hasPass();

    /**
     * Returns true if playerColor has a legal move
     * @return returns true when the next player has a legal move
     */
    boolean hasLegalMoves();

    /**
     * Generates all legal moves for given color
     * @return returns an unordered ArrayList of possible moves
     */
    List<Move> getMoves();

    /**
     * Checks for legal move
     * @param move
     * @return returns true if the given move is legal on this board
     */
    boolean isLegalMove(Move move);

    /**
     * Makes move on board according to rules
     * @param move Move to make on the board
     * @throws IllegalMoveException Throiwn when a illegal move has been passed as parameter
     */
    void makeMove(Move move) throws IllegalMoveException;

    /**
     * Creates a string representation of the board.
     * The string will have dim*dim +2 characters. (In an standard 8x8 field 66)
     * An empty field will be represented by a "-", black as "X", white as "O".
     * The last character will determine who has the next move preceeded by a space
     *
     * @return returns a string representing the current board
     */
    String toString();

    /**
     * Return dimension of board
     * @return return board dimensions
     */
    int getDim();

    /**
     * Checks if col, row is still within the board
     * @param col Column 1 to dim
     * @param row Row 1 to dim
     * @return return if coordinates are within the board boundaries
     */
    boolean isWithinBoard(int col, int row);

    /**
     * Getter for the maximal possible number of moves
     * @return return number of maximum numbers of moves
     */
    int getMaxMoveNumber();

    /**
     * Return color of field row, col
     * @param col Column 1 to dim
     * @param row Row 1 to dim
     * @return color of given field (-1,0,1 -- BLACK, EMPTY, WHITE)
     */
    ReversiColor getField(int col, int row);

    /**
     * Return color of next player or ReversiColor.NONE if there are no more moves.
     * @return color of player for next move or none when there are no more moves.
     */
    ReversiColor getNextPlayerColor();

    /**
     * Return color of last player
     * @return color of player from last move (-1,0,1 -- BLACK, EMPTY, WHITE)
     */
    ReversiColor getLastPlayerColor();

    /**
     * Getter for lastMove
     * @return returns the last move mode on this board
     */
    Move getLastMove();

    /**
     * Getter for lastMoves
     * @return returns the move path for this board
     */
    List<Move> getMoveHistory();

    /**
     * Returns number of black stones
     * @return number of black stone
     */
    int getPiecesBlack();

    /**
     * Returns number of white stones
     * @return number of white stones
     */
    int getPiecesWhite();

    /**
     * Returns the number of move made so far
     * @return returns the number of moves made so far
     */
    int getLastMoveNumber();

    /**
     * Returns the number of the next move
     * @return returns the number of the next move
     */
    int getNextMoveNumber();

    /**
     * Returns the differential of number of pieces
     * @param color
     * @return returns the differential of number of pieces
     */
    int getPiecesDiff(ReversiColor color);

    /**
     * Returns the differential of corners for a given color
     * @param color
     * @return returns the differential of corners for a given color
     */
    int getCornerDiff(ReversiColor color);

    /**
     * Returns the differential of X-squares
     * @param color
     * @return returns the differential of X-squares
     */
    int getXsquaresDiff(ReversiColor color);

    /**
     * Returns the differential of C-squares
     * @param color
     * @return returns the differential of C-squares
     */
    int getCsquaresDiff(ReversiColor color);

    /**
     * Returns the difference in the mobility for the current player
     * @return int - difference of the mobility for the next player
     */
    int getMobilityDiff();

    /**
    * getter for hashKey
    *
    * @return returns a unique hash key for this board
    */
    String getHashKey();

    /**
     * Returns the number of empty fiels next to the field (liberty).
     * @param col Column 1 to dim
     * @param row Row 1 to dim
     * @return Number of empty fields
     */
    int getLiberties(int col, int row);

}
