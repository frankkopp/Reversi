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
 * Thrown to indicate that we tried to use an illegal move in a context where this is not allowed.<br/>
 * For example when we tried to commit an illegal move to a board.
 */
public class IllegalMoveException extends Exception {

    private static final long serialVersionUID = 1L;

	/**
     * Constructs a new <code>IllegalMoveException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public IllegalMoveException(String msg) {
        super(msg);
    }

    /**
     * Constructs a new <code>IllegalMoveException</code> with no detail message.
     */
    public IllegalMoveException() {
    }

}
