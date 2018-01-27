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

package fko.reversi.ui.ReversiGUI;

import fko.reversi.game.Board;
import fko.reversi.game.Game;
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Iterator;
import java.util.List;

/**
 * This class shows a list of moves in a table within a JScrollpane.
 * The list is updated when the board of a game is changed that is when a move is made
 */
public class MoveList extends JScrollPane {

    // -- _moves --
    private DefaultTableModel _moves;

    // -- the table --
    private JTable _movelist;
    private final String[] nullRow = new String[] {"", "", "", "", "", ""};

    /**
     * Creates an empty (no viewport view) <code>JScrollPane</code>
     * where both horizontal and vertical scrollbars appear when needed.
     */
    public MoveList() {
        String[] columnNames = { "Move Number", "Black Moves", "Move Values", "Move Number", "White Moves", "Move Values" };

        _moves = new DefaultTableModel(columnNames,0);

        _movelist = new JTable(_moves);
        _movelist.setShowGrid(true);
        _movelist.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        this.setViewportView(_movelist);
        this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        this.setDoubleBuffered(true);
    }

    /**
     * clears the list
     */
    protected void clear() {
        // -- removes all rows --
        _moves.setNumRows(0);
    }

    /**
     * builds the list of move of the given board
     * @param game
     */
    protected void drawMove(Game game) {

        // -- reference the current board --
        Board board = game.getCurBoard();

        // -- reference to the move list of the board --
        List<Move> moves = board.getMoveHistory();

        // -- cycle through all moves and copy them to the movelist --
        int counter = 0;
        ReversiColor lastPlayer = ReversiColor.NONE;
        int passes = 0;
        boolean isPass = false;
        for (Iterator<Move> i = moves.listIterator();i.hasNext();) {
            counter++;
            Move move = i.next();

            // -- was this move a pass? --
            if (move.getColor() == lastPlayer) {
                // -- pass --
                passes++;
                isPass = true;
            }

            // -- calculate the last row we need --
            int lastRow = ((counter + passes + 1) >> 1) - 1;

            // -- do we have enough rows defined? --
            if (lastRow >= _movelist.getRowCount()) {
                _moves.addRow( nullRow );
            }

            // -- if we had a pass print out "-"s --
            if (isPass) {
                // -- black did the last move --> white passed
                if (move.getColor().isBlack()) {
                    _moves.setValueAt("-", lastRow-1, 3);
                    _moves.setValueAt("-", lastRow-1, 4);
                    _moves.setValueAt("-", lastRow-1, 5);
                // -- white did the last move --> black passed
                } else if (move.getColor().isWhite()) {
                    _moves.setValueAt("-", lastRow, 0);
                    _moves.setValueAt("-", lastRow, 1);
                    _moves.setValueAt("-", lastRow, 2);
                }
            }

            // -- prepare some Strings we need to format the output in the list/table --
            String moveNumberString = Integer.toString(counter) + '.';
            String moveValueString = getMoveValueString(move.getValue());

            // -- when user is black write in the left cells
            String moveString;
            if (move.getColor().isBlack()) {
                // -- format the moveString --
                //noinspection ObjectAllocationInLoop
                moveString = new StringBuilder(10)
                        .append(move.getColor().toChar())
                        .append('(')
                        .append(move.getCol())
                        .append(',')
                        .append(move.getRow())
                        .append(')')
                        .toString();
                _moves.setValueAt(moveNumberString, lastRow, 0);
                _moves.setValueAt(moveString      , lastRow, 1);
                _moves.setValueAt(moveValueString , lastRow, 2);
            // -- when user is white write in the right cells
            } else if (move.getColor().isWhite()) {
                //noinspection ObjectAllocationInLoop
                moveString = new StringBuilder(10)
                        .append(move.getColor().toChar())
                        .append('(')
                        .append(move.getCol())
                        .append(',')
                        .append(move.getRow())
                        .append(')')
                        .toString();
                _moves.setValueAt(moveNumberString, lastRow, 3);
                _moves.setValueAt(moveString      , lastRow, 4);
                _moves.setValueAt(moveValueString , lastRow, 5);
            }

            lastPlayer = move.getColor();
            isPass=false;
        }

        // -- autoscroll to the last entry --
        this.getVerticalScrollBar().setValue(this.getVerticalScrollBar().getMaximum());

        /*
        vsb.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!e.getValueIsAdjusting()) {
                    vsb.setValue(vsb.getMaximum());
                }
            }
        });
        */
    }

    /**
     * formats a string based on the integer value of the move
     * @param value integer value of a move
     * @return String representing the value of the move
     */
    private static String getMoveValueString(int value) {
        String moveBlackValueString = "";
        if (value > 0) {
            moveBlackValueString += "+";
        } else if (value == 0) {
            moveBlackValueString += " ";
        }
        moveBlackValueString += value;
        if (value ==  Integer.MAX_VALUE) {
            moveBlackValueString = "+inf";
        }
        if (value == -Integer.MAX_VALUE) {
            moveBlackValueString = "-inf";
        }
        if (value ==  Integer.MIN_VALUE) {
            moveBlackValueString = "only";
        }
        return moveBlackValueString;
    }

}
