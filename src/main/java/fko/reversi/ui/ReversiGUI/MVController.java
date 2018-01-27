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

import fko.reversi.Playroom;
import fko.reversi.Reversi;
import fko.reversi.remote.RemoteGameRequest;
import fko.reversi.game.*;
import fko.reversi.player.HumanPlayer;
import fko.reversi.player.PlayerType;

import javax.swing.*;

import java.awt.*;

/**
 * This class acts a the MVC controller an handles all user input actions.
 * It is the only class in the ReversiGUI package which writes to the model.
 */
public class MVController {

    // -- back reference to ui --
    private ReversiGUI _ui;

    // -- reference to the _model (playroom) --
    private Playroom _model;

    // -- we only can tell the model the users move when we know which player we have to use --
    private HumanPlayer _moveReceiver = null;
    private final Object _moveReceiverLock = new Object();

    // Is true when we currently show the dialog
    private NewRemoteGameRequestDialog _newRemoteGameDialog = null;
    private RemoteGameRequestPendingDialog _remoteGameRequestPendingDialog = null;

    /**
     * creates a controller object with a back reference to the ui main class
     * @param swingGUI
     */
    public MVController(ReversiGUI swingGUI) {
        this._ui = swingGUI;
        this._model = Reversi.getPlayroom();
    }

   /**
    * This method is called from a mouse event from the user.It hands a move
    * over to a known HumanPlayer and then nulls the reference to the HumanPlayer. To
    * make a HumanPlayer known call setMoveReceiver(HumanPlayer). If no HumanPlayer is
    * known to the object nothing happens
    * @param point representing the coordinates on the current board
    */
   public void setPlayerMove(Point point) {
       synchronized(_moveReceiverLock) {
           if (_moveReceiver!=null) {
                _moveReceiver.setMove(new MoveImpl((int) point.getX(), (int) point.getY(), _moveReceiver.getColor()));
           }
           // After we have handed over the move to the receiver player we delete the reference
           // to the receiver. This will be set again by setMoveReceiver by  the observer update
           // through ReversiGUI
           _moveReceiver=null;
       }
   }

    /**
     * Is called when a HumanPlayer has notified its Oberservers (ReversiGUI) that it is waiting
     * for a move from a human player.
     * If the receiving player is know to the class it accepts new moves through setPlayerMove()
     * usual from mouse input
     * @param player
     */
    public void setMoveReceiver(HumanPlayer player) {
        synchronized(_moveReceiverLock) {
            _moveReceiver = player;
        }
    }

    /**
     * this method tell the playroom (_model) to set the black player type
     * @param type
     */
    public void setPlayerTypeBlackAction(PlayerType type) {
        _model.setPlayerTypeBlack(type);
    }

    /**
     * this method tell the playroom (_model) to set the black player type
     * @param type
     */
    public void setPlayerTypeWhiteAction(PlayerType type) {
        _model.setPlayerTypeWhite(type);
    }

    /**
     * sets the players name and starts a new game
     * @param blackName
     * @param whiteName
     */
    public void startNewGame(String blackName, String whiteName) {
        _model.setNameBlackPlayer(blackName);
        _model.setNameWhitePlayer(whiteName);
        newGameAction();
    }

    /**
     * displays a dialog to start a new game
     */
    public void newGameDialog() {
        NewGameDialog dialog = new NewGameDialog(_ui);
        AbstractDialog.centerComponent(dialog);
        dialog.setVisible(true);
    }

    /**
     * stops a running game
     */
    public void stopCurrentGame() {
        if (_ui.getMainWindow().userConfirmation("Do you really want to stop the game?") == JOptionPane.YES_OPTION) {
            _model.stopPlayroom();
        }
    }

    /**
     * pauses a game
     */
    public void pauseOrResumeCurrentGame() {
        if (_model.getCurrentGame().isPaused()) {
           _model.getCurrentGame().resumeGame();
        } else if (_model.getCurrentGame().isRunning()) {
            _model.getCurrentGame().pauseGame();
        }
    }

    /**
     * toggles the timed game setting
     */
    public void toggleTimedGame() {
        _model.setTimedGame(!_model.isTimedGame());
    }

    /**
     * set the time for black
     * @param sec
     */
    public void setTimeBlackAction(int sec) {
        _model.setTimeBlack(sec);
    }

    /**
     * set the time for white
     * @param sec
     */
    public void setTimeWhiteAction(int sec) {
        _model.setTimeWhite(sec);
    }

    /**
     * set the level of the player
     * @param player
     * @param level
     */
    public void setLevelAction(ReversiColor player, int level) {
        if (player.isBlack()) {
            _model.setCurrentLevelBlack(level);
        } else if (player.isWhite()) {
            _model.setCurrentLevelWhite(level);
        } else {
            throw new IllegalArgumentException("Player color must be BLACK or WHITE. Was "+player.toString());
        }
    }

    /**
     * toggles the setting to show all possible moves
     */
    public void toggleShowPossibleMovesAction() {
        _ui.set_showPossibleMoves(_ui.getMainWindow().getReversiMenu().getShowPossibleMoves().getState());
        _ui.getMainWindow().repaint();
    }

    /**
     * toggles the setting to show all possible moves
     */
    public void toggleShowMoveListAction() {
        _ui.set_showMoveListWindow(_ui.getMainWindow().getReversiMenu().getShowMoveList().getState());
        _ui.getMoveListWindow().setVisible(_ui.getMainWindow().getReversiMenu().getShowMoveList().getState());
    }


    /**
     * toggles the setting to show all possible moves
     */
    public void toggleShowEngineInfoBlackAction() {
        _ui.set_showEngineInfoWindowBlack(_ui.getMainWindow().getReversiMenu().getShowEngineInfoBlack().getState());
        _ui.getEngineInfoWindowBlack().setVisible(_ui.getMainWindow().getReversiMenu().getShowEngineInfoBlack().getState());
    }

    /**
     * toggles the setting to show all possible moves
     */
    public void toggleShowEngineInfoWhiteAction() {
        _ui.set_showEngineInfoWindowWhite(_ui.getMainWindow().getReversiMenu().getShowEngineInfoWhite().getState());
        _ui.getEngineInfoWindowWhite().setVisible(_ui.getMainWindow().getReversiMenu().getShowEngineInfoWhite().getState());
    }

    /**
     * turns the remote player server on or off
     */
    public void toggleRemotePlayerServerAction() {
        _model.setRemotePlayerServerEnabled(_ui.getMainWindow().getReversiMenu().getRemotePlayerServer().getState());
    }

    public void setNumberOfGamesAction(int number) {
        _model.setNumberOfGames(number);
    }

    public void setBoardDimensionAction(int dim) {
        _model.setBoardDimension(dim);
    }

    public void setRemoteServerPortAction(int port) {
        _model.setRemotePlayerServerPort(port);
    }

    public void setRemoteClientBlackPortAction(int port) {
        _model.setRemotePlayerClientBlackServerPort(port);
    }

    public void setRemoteClientWhitePortAction(int port) {
        _model.setRemotePlayerClientWhiteServerPort(port);
    }

    public void setRemoteClientBlackServerNameAction(String newHostname) {
        _model.setRemotePlayerClientBlackServerName(newHostname);
    }

    public void setRemoteClientWhiteServerNameAction(String newHostname) {
        _model.setRemotePlayerClientWhiteServerName(newHostname);
    }

    /**
     * Tell the model to clean up and exit
     */
    public void exitReversi() {
        _model.exitReversi();
    }

    /**
     * starts a new game
     */
    private synchronized void newGameAction() {
        if (!noCurrentGame()) {
            return;
        }
        _model.startPlayroom();
    }

    /**
     * determines if the game is over or stopped
     * @return true when game is over or stopped
     */
    private boolean noCurrentGame() {
        if (_model.getCurrentGame() == null) {
            return true;
        }
        return _model.getCurrentGame().isFinished();
    }

    /**
     * Tells the playroom that we have either accepted or denied the remote game request
     * @param accepted
     */
    public void acceptRemoteGameRequest(boolean accepted) {
        _model.setGameRequestedFromServerAnswer(accepted);
    }

    /**
     * Tells the playroom that we have canceled the game request
     */
    public void cancelRemoteGameRequest() {
        _model.userCancelRemoteGameRequest();
    }

    /**
     * displays a dialog to get an answer from the user for a remote game request
     */
    public void newRemoteGameRequestDialog(RemoteGameRequest gr) {
        _newRemoteGameDialog = new NewRemoteGameRequestDialog(_ui, gr);
        AbstractDialog.centerComponent(_newRemoteGameDialog);
        _newRemoteGameDialog.setVisible(true);
    }

    public void showRequestPendingDialog(boolean show) {
        if (_remoteGameRequestPendingDialog==null) {
            _remoteGameRequestPendingDialog = new RemoteGameRequestPendingDialog(_ui);
            AbstractDialog.centerComponent(_remoteGameRequestPendingDialog);
        }
        _remoteGameRequestPendingDialog.setVisible(show);
    }
}
