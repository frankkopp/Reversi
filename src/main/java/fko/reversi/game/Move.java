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


/**
 * <p>This interface describes a move in Reversi.</p>
 * <p>It also contains a value for this move if already calculated.</p>
 * <p>A move can only have a meaningful value when it is related to a actual board.</p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public interface Move {

    /**
     * value if value is not set yet
     */
    int VALUE_UNKNOWN = Integer.MIN_VALUE;

    /**
     * returns row of move
     * @return row
     */
    int getRow();

    /**
     * returns col of move
     * @return col
     */
    int getCol();

    /**
     * returns color of move
     * @return color
     */
    ReversiColor getColor();

    /**
     * toString
     * @return move as string
     */
    String toString();

    /**
     * getter for value
     * @return value
     */
    int getValue();

    /**
     * setter for value
     * @param value
     */
    void setValue(int value);

    /**
     * Returns a string only containing the move information without the value.
     * @return move as string
     */
    String toMoveString();
}
