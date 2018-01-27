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

import fko.reversi.game.*;
import fko.reversi.Reversi;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.Color;

/**
 * A panel with a textarea to show information like move list etc.
 */
public class InfoPanel extends JScrollPane {

    private JTextArea textArea = null;

    /**
     * Constructs a new TextArea.  A default model is set, the initial string
     * is null, and rows/columns are set to 0.
     */
    public InfoPanel() {
        super();
        textArea = new JTextArea();
        textArea.setBorder(new LineBorder(Color.darkGray, 1, true));
        textArea.setEditable(false);
        textArea.setTabSize(8);
        textArea.setColumns(40);
        textArea.setFont(new Font("Lucida Console", Font.PLAIN, 12));
        textArea.setDoubleBuffered(true);
        textArea.setAutoscrolls(true);
        this.setViewportView(textArea);
    }

    /**
     * print a text into the info pane
     * @param text
     */
    public synchronized void printInfo(String text) {
        textArea.append(text);
        SwingUtilities.invokeLater(_autoScroller);
    }

    /**
     * print a text plus newline into the info pane
     * @param text
     */
    public synchronized void printInfoln(String text) {
        printInfo(text+ '\n');
    }

    /**
      * draws a board in textmode onto the infoPanel
      * @param board
      */
     public void drawBoard(fko.reversi.game.Board board) {

         int dim = board.getDim();

         String boardString = "";

         // backwards as highest row is on top
         for (int row = dim; row > 0; row--) {
             // upper border
             boardString += ("    -");   // 4 * space
             for (int col = dim; col > 0; col--) {
                 boardString += ("----"); // dim * -
             }
             boardString += ("\n");
             // row number
             if (row < 10) {
                 boardString += (' ' + Integer.toString(row) + ": |");
             } else {
                 boardString += (Integer.toString(row) + ": |");
             }
             // col fields
             for (int col = 1; col <= dim; col++) {
                 ReversiColor color = board.getField(col, row);
                 if (color.isEmpty()) {
                     boardString += ("   |");
                 } else if (color.isBlack()) {
                     boardString += (" # |");
                 } else if (color.isWhite()) {
                     boardString += (" O |");
                 } else {
                     Reversi.fatalError("field value not allowed: field(" + row + ',' + col + ')');
                 }
             }
             boardString += ("\n");
         }
         // lower border
         boardString += ("    -");   // 4 * space
         for (int col = dim; col > 0; col--) {
             boardString += ("----"); // dim * -
         }
         boardString += ("\n");
         // col numbers
         boardString += ("     ");   // 4 * space
         for (int col = 1; col <= dim; col++) {
             if (col < 10) {
                 boardString += (" " + col + "  ");
             } else {
                 boardString += (" " + col + ' ');
             }
         }
         boardString += ("\n");

         printInfo(boardString);
     }

    /**
     * print move
     *
     * @param board
     */
    public synchronized void drawMove(final Board board) {
        Move move = board.getLastMove();
        if (move==null) {
            return;
        }
        int moveValue = move.getValue();
        String moveValueString = "";
        if (moveValue > 0) {
            moveValueString += "+";
        } else if (moveValue == 0) {
            moveValueString += " ";
        }
        moveValueString += moveValue;
        if (moveValue == Integer.MAX_VALUE) {
            moveValueString = "+inf";
        }
        if (moveValue == -Integer.MAX_VALUE) {
            moveValueString = "-inf";
        }
        if (moveValue == Integer.MIN_VALUE) {
            moveValueString = "only";
        }
        printInfoln(String.valueOf(board.getLastMoveNumber()) + ". "
                    + move.getColor().toCharSymbol() + '(' + move.getCol() + ',' + move.getRow() + ')'
                    + " (=" + moveValueString + ')'
                    + "    ");
    }

    /**
     * Thread to scroll infoPanel to the last position
     */
    private final Runnable _autoScroller = new Runnable() {
        public void run() {
            textArea.setCaretPosition(textArea.getText().length());
        }
    };



}