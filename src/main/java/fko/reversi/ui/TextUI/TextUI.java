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

package fko.reversi.ui.TextUI;

import fko.reversi.Playroom;
import fko.reversi.Reversi;
import fko.reversi.mvc.ModelEvents.ModelEvent;
import fko.reversi.game.Game;
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;
import fko.reversi.player.Player;
import fko.reversi.ui.UserInterface;

import java.util.Observable;

/**
 * This is a class to play reversi without a grafical interface.
 *
 * It is not complete yet!
 */
public class TextUI implements UserInterface, Runnable {

    // -- private fields --
    private Thread _textuiThread = new Thread(this, "TextGUI");

    /**
     * default contructor
     */
    public TextUI() {
        _textuiThread.start();
    }

    /**
     * run() implementation from Runnable
     */
    public void run() {

        // TODO: Implement console user interface HERE!!!

        greeting();

        Reversi.getPlayroom().addObserver(this);
        Reversi.getPlayroom().startPlayroom();

    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>ModelObservable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o  the observable object.
     * @param me a ModelEvent passed to the <code>notifyObservers</code>
     *           method.
     */
    public void update(Observable o, ModelEvent me) {
        update(o, (Object)me);
    }

    /**
     * This will be called whenever the model is changed
     * Model in this case is the playroom or game
     * @param model
     * @param parameter
     */
    public void update(Observable model, Object parameter ) {

        // -- we check for the caller object with == we do not check for .equals()
        if (model==Reversi.getPlayroom()) {
            _updatePlayroom(model);
        }
        else if (model==Reversi.getPlayroom().getCurrentGame()) {
            _updateGame(model);
        }

        _println();
    }

    /**
     * is called when model Playroom changed
     */
    private void _updatePlayroom(Observable model) {

        Playroom playroom = (Playroom) model ;

        // -- Status of game? --
        if (playroom.getCurrentGame() != null) {
            // -- game object exists --
            _println("NEW GAME!");

            if (playroom.getCurrentGame().getStatus() == Game.GAME_INITIALIZED) {
                // -- game is initialized --
                _println("GAME INITILAZIED!");
                _println("Player BLACK: "+playroom.getCurrentGame().getPlayerBlack().getName());
                _println("Player WHITE: "+playroom.getCurrentGame().getPlayerWhite().getName());

                // -- now we want to observer the game --
                playroom.getCurrentGame().addObserver(this);
            }
            else {
                // -- game is started --
                Reversi.fatalError("GAME IN UNKONWN STATE!");
            }
        }
    }

    /**
     * is called when model Game changed
     */
    private static void _updateGame(Observable model) {

        Game game = (Game) model;

        // -- find out state of the game --
        switch (game.getStatus()) {

            case Game.GAME_RUNNING:
                // --show last move an cuurent board --
                if (game.getCurBoard().getLastMove() != null) {
                    _println(game.getCurBoard().getLastMove().toString());
                }
                _drawBoard(game.getCurBoard());
                break;

            case Game.GAME_OVER:
               _println("GAME OVER!!!!");
                break;

            case Game.GAME_PAUSED:
               _println("GAME PAUSED!!!!");
                break;

            case Game.GAME_STOPPED:
               _println("GAME STOPPED!!!!");
                break;

       }
    }

    /**
     * Get a move from a human player
     *
     * @param game
     * @param player
     * @return a valid move from the human player
     */
    public Move getMove(Game game, Player player) {
        return null;
   }

    /**
     * Clears infoPanel and prints a greeting message
     */
    private static void greeting() {
        _print(
                "==========================================================\n" +
                "Welcome to Reversi by Frank Kopp!\n" +
                "---------------------------\n\n" +
                "Reversi by Frank Kopp\n" +
                "(c) 2003, 2004, 2005 by Frank Kopp\n" +
                "Email: reversi@familie-kopp.de\n" +
                "--------------------------------------\n" +
                "Reversi by Frank Kopp\n" +
                        '\n' +
                " \"Reversi by Frank Kopp\" is free software; you can redistribute it and/or modify\n" +
                " it under the terms of the GNU General Public License as published by\n" +
                " the Free Software Foundation; either version 2 of the License, or\n" +
                " (at your option) any later version.\n" +
                        '\n' +
                " \"Reversi by Frank Kopp\" is distributed in the hope that it will be useful,\n" +
                " but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                " GNU General Public License for more details.\n" +
                        '\n' +
                " You should have received a copy of the GNU General Public License\n" +
                " along with \"Reversi by Frank Kopp\"; if not, write to the Free Software\n" +
                " Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA" +
                "\n\n\n");
    }


    /**
     * draws a board in textmode onto the infoPanel
     * @param board
     */
    private static void _drawBoard(fko.reversi.game.Board board) {
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
                    /*
                    if (board.isStableField(col, row)) {
                        boardString += (" #!|");
                    } else {
                    */
                    boardString += (" # |");
                    // }
                } else if (color.isWhite()) {
                    /*
                    if (board.isStableField(col, row)) {
                        boardString += (" O!|");
                    } else {
                    */
                    boardString += (" O |");
                    //}
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

        _print(boardString);
    }

    /**
     * print a string to System.out
     */
    private static void _print(String s) {
        System.out.printf(s);
    }

    /**
     * print a string + \n to System.out
     */
    private static void _println(String s) {
        System.out.printf(s+ '\n');
    }

    /**
     * print a \n to System.out
     */
    private static void _println() {
        System.out.printf("\n");
    }

}