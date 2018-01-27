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
 * <p>A class representing a move in a Reversi game.</p>
 *
 * <p>A move can only have a meaningful value when it is related to a actual board.</p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class MoveImpl implements Move {

    private final int          _col;
    private final int          _row;
    private final ReversiColor _color;
    private       int          _value = Move.VALUE_UNKNOWN;

    /**
     * Creates a new Move with coordinates and color.
     * @param col
     * @param row
     * @param color
     * @param value
     */
    public MoveImpl(int col, int row, ReversiColor color, int value) {
        this(col,row,color);
        this._value = value;
    }

    /**
     * Creates a new Move with coordinates and color.
     * @param col
     * @param row
     * @param color
     */
    public MoveImpl(int col, int row, ReversiColor color) {
        this._col = col;
        this._row = row;
        this._color = color;
    }

    /**
     * Creates a new Move based as a copy of a given move.
     * @param move
     */
    public MoveImpl(Move move) {
        this(move.getCol(), move.getRow(), move.getColor(), move.getValue());
    }

    /**
     * Returns row of move
     * @return row
     */
    public int getRow() {
        return _row;
    }

    /**
     * Returns col of move
     * @return col
     */
    public int getCol() {
        return _col;
    }

    /**
     * Returns color of move
     * @return color
     */
    public ReversiColor getColor() {
        return this._color;
    }

    /**
     * Returns a string only containing the move information without the value.
     * @return move as string
     */
    public String toMoveString() {
        return new StringBuilder(7)
                .append(_color.toInt())
                .append('(')
                .append(_col)
                .append(',')
                .append(_row)
                .append(')')
                .toString();
    }

    /**
     * Returns a string representation of a move.
     *
     * @return a string representation of the move.
     */
    @Override
	public String toString() {
        return toMoveString() + " value=" + _value;
    }

    /**
     * Getter for value.<br/>
     * Returns Move.VALUE_UNKNOWN when value is not set.<br/>
     * A move can only have a meaningful value when it is related to a actual board.
     * @return value
     */
    public synchronized int getValue() {
        return _value;
    }

    /**
     * Setter for value.<br/>
     * A move can only have a meaningful value when it is related to a actual board.
     * @param value
     */
    public synchronized void setValue(int value) {
        this._value = value;
    }

    /**
     * Indicates whether some other move object is "equal to" this one.
     * <p/>
     * For a Move we compare int, col and color. Value is not considered!
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     * @see #hashCode()
     * @see java.util.Hashtable
     */
    @Override
	public boolean equals(Object obj) {
        if (obj==null || !(obj instanceof MoveImpl)) {
            return false;
        }
        Move m = (Move) obj;
        return _col==m.getCol() && _row==m.getRow() && _color.equals(m.getColor());
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p/>
     * The general contract of <code>hashCode</code> is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     * an execution of a Java application, the <tt>hashCode</tt> method
     * must consistently return the same integer, provided no information
     * used in <tt>equals</tt> comparisons on the object is modified.
     * This integer need not remain consistent from one execution of an
     * application to another execution of the same application.
     * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     * method, then calling the <code>hashCode</code> method on each of
     * the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     * according to the {@link Object#equals(Object)}
     * method, then calling the <tt>hashCode</tt> method on each of the
     * two objects must produce distinct integer results.  However, the
     * programmer should be aware that producing distinct integer results
     * for unequal objects may improve the performance of hashtables.
     * </ul>
     * <p/>
     * As much as is reasonably practical, the hashCode method defined by
     * class <tt>Object</tt> does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
    @Override
	public int hashCode() {
        return _col+_row*31*_color.toInt();
    }

}
