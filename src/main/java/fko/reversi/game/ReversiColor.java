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
 * <p/>
 * The ReversiColor class represents the two colors of a Reversi game and a special color for empty fields (NONE).
 * This class can not be instanciated. It keeps public references to the only possible instances BLACK, WHITE, NONE.
 * These instances are immutable. As it is not possible to have any other instances of ReversiColors the use of
 * these instances is as fast as if using an int.
 * </p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public final class ReversiColor {

    /**
     * A constant representing the color black.
     */
    public static final ReversiColor BLACK = new ReversiColor(-1);

    /**
     * A constant representing the color white.
     */
    public static final ReversiColor WHITE = new ReversiColor(1);

    /**
     * A constant representing an empty field or no color
     */
    public static final ReversiColor NONE = new ReversiColor(0);

    /**
     * A convinient constant identical to EMPTY
     */
    public static final ReversiColor EMPTY = NONE;

    // color representation: -1 for black, 0 for none, 1 for white
    private final int _color;

    private ReversiColor(int color) {
        _color = color;
    }

    /**
     * Returns the other ReversiColor.
     * @return int - as defined in ReversiColor
     */
    public ReversiColor getInverseColor() {
        if      (this == BLACK) {
            return WHITE;
        } else if (this == WHITE) {
            return BLACK;
        } else if (this == NONE ) {
            throw new UnsupportedOperationException("ReversiColor.NONE has no inverse color");
        } else {
            throw new RuntimeException("Invalid ReversiColor");
        }
    }

    /**
     * array for faster toString operations
     */
    private final static char[] _fieldString = {'X', '-', 'O'};

    /**
     * Returns a character to use for a String representation of the field.<br/>
     * It accepts ReversiColor.BLACK (X), ReversiColor.WHITE (O), ReversiColor.EMPTY (-) otherwise returns
     * an empty character.
     * @return char - one of 'X', '-', 'O' or ' '
     */
    public char toCharSymbol() {
        if (this._color < -1 || this._color > 1) {
            return ' ';
        }
        return _fieldString[_color+1];
    }

    /**
     * array for faster toString operations
     */
    private final static char[] _fieldString2 = {'b', '-', 'w'};

    /**
     * Returns a character to use for a String representation of the field.<br/>
     * It accepts ReversiColor.BLACK (X), ReversiColor.WHITE (O), ReversiColor.EMPTY (-) otherwise returns
     * an empty character.
     * @return char - one of 'b', '-', 'w' or ' '
     */
    public char toChar() {
        if (this._color < -1 || this._color > 1) {
            return ' ';
        }
        return _fieldString2[_color+1];
    }

    /**
     * Returns an int representation of ReversiColor
     * @return -1 for BLACK, 0 for EMPTY, 1 for WHITE
     */
    public int toInt() {
        return this._color;
    }

    /**
     * Returns the ReversiColor for the given int
     * @param i - int representing a Reversi color
     * @throws IllegalArgumentException when i is not a valid Reversi color representation
     */
    public static ReversiColor valueOf(int i) {
        if (i==-1) {
            return BLACK;
        } else if (i==1) {
            return WHITE;
        } else if (i==0) {
            return NONE;
        } else {
            throw new IllegalArgumentException("Given value is not a valid int representation of a Reversi color");
        }
    }

    /**
     * Returns the ReversiColor for the given String.
     * The String may contain -1,0,1 or Black, black, Empty, empty, None, none, WHite, white.
     * @param s - String representing a Reversi color
     * @throws IllegalArgumentException when s is not a valid Reversi color representation
     */
    public static ReversiColor valueOf(String s) {
        if      (s.equals("-1") || s.equals("Black") || s.equals("black")) {
            return BLACK;
        } else if (s.equals("1")  || s.equals("White") || s.equals("white")) {
            return WHITE;
        } else if (s.equals("0")  || s.equals("None") || s.equals("none") || s.equals("Empty") || s.equals("empty")) {
            return NONE;
        } else {
            throw new IllegalArgumentException("Given value is not a valid int representation of a Reversi color");
        }
    }

    /**
     * Returns String representation of the ReversiColor
     */
    @Override
	public String toString() {
        if (this==ReversiColor.BLACK) {
            return "Black";
        } else if (this==ReversiColor.WHITE) {
            return "White";
        } else if (this==ReversiColor.NONE) {
            return "None";
        } else {
            throw new RuntimeException("Invalid ReversiColor");
        }
    }

    /**
     * Convenience method to check if the instance is BLACK
     */
    public boolean isBlack() {
        return this==BLACK;
    }

    /**
     * Convenience method to check if the instance is WHITE
     */
    public boolean isWhite() {
        return this==WHITE;
    }

    /**
     * Convenience method to check if the instance is NONE
     */
    public boolean isNone() {
        return this==NONE;
    }

    /**
     * Convenience method to check if the instance is EMPTY
     */
    public boolean isEmpty() {
        return this==EMPTY;
    }

    /**
     * Convenience method to check if the instance is not EMPTY
     */
    public boolean isNotEmpty() {
        return this!=EMPTY;
    }

}
