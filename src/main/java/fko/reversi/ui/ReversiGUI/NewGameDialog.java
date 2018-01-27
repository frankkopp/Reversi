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

import fko.reversi.Reversi;
import fko.reversi.player.PlayerType;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This dialog is shown when the user clicked on New Game in Menu Game or Ctrl+N to start
 * a new game.
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class NewGameDialog extends fko.reversi.ui.ReversiGUI.AbstractDialog {

    private ReversiGUI _ui;

    private JTextField blackName, whiteName;
    private JPanel _inputPanel;
    private JRadioButton whitePlayerType_remote;
    private JRadioButton blackPlayerType_remote;


    public NewGameDialog(ReversiGUI _ui2) {
        super(_ui2.getMainWindow(), "New Game", true);
        _ui = _ui2;

        setName("NewGameDialog");
        setTitle("New Game");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        // create gui
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        getContentPane().setLayout(new GridBagLayout());

        // -- BUTTONS --
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        // Start button
        JButton startButton = new JButton("Start");
        startButton.addActionListener(new StartButtonAction());
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new CancelButtonAction());
        GridBagHelper.constrain(buttonPanel, startButton, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(buttonPanel, cancelButton, 2, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);

        setupDialog();

        // -- layout pane --
        GridBagHelper.constrain(getContentPane(), pane, 0, 0, 0, 0, GridBagConstraints.VERTICAL, GridBagConstraints.NORTH, 1.0, 1.0, 0, 0, 0, 0);
        GridBagHelper.constrain(pane, _inputPanel, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(pane, new JPanel(), 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(pane, buttonPanel, 1, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);

        // -- set default button --
        getRootPane().setDefaultButton(startButton);

        // -- pack --
        pack();

    }

    private void setupDialog() {

        _inputPanel = new JPanel(new GridBagLayout());

        // -- Players Names --
        JPanel blackNamePanel = new JPanel();
        blackName = new JTextField(Reversi.getPlayroom().getNameBlackPlayer());
        blackName.setColumns(15);
        blackNamePanel.add(blackName);
        blackNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name Black Player"));
        JPanel whiteNamePanel = new JPanel();
        whiteName = new JTextField(Reversi.getPlayroom().getNameWhitePlayer());
        whiteName.setColumns(15);
        whiteNamePanel.add(whiteName);
        whiteNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name White Player"));
        GridBagHelper.constrain(_inputPanel, blackNamePanel, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(_inputPanel, whiteNamePanel, 2, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

        // -- Player Types --

        // -- Black --
        JPanel blackPlayerTypePanel = new JPanel(new GridBagLayout());
        blackPlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "Black Player is ..."));
        ButtonGroup blackPlayerTypeButtonGroup = new ButtonGroup();

        // -- Human --
        JRadioButton blackPlayerType_Human = new JRadioButton("Human");
        blackPlayerType_Human.setEnabled(true);
        blackPlayerType_Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.HUMAN);
                whitePlayerType_remote.setEnabled(true);
            }
        });
        blackPlayerTypeButtonGroup.add(blackPlayerType_Human);

        // -- Computer --
        JRadioButton blackPlayerType_Computer = new JRadioButton("Computer");
        blackPlayerType_Computer.setEnabled(true);
        blackPlayerType_Computer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.COMPUTER);
                whitePlayerType_remote.setEnabled(true);
            }
        });
        blackPlayerTypeButtonGroup.add(blackPlayerType_Computer);

        // -- Remote --
        blackPlayerType_remote = new JRadioButton("Remote");
        blackPlayerType_remote.setEnabled(true);
        blackPlayerType_remote.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.REMOTE);
                whitePlayerType_remote.setEnabled(false);
            }
        });
        blackPlayerTypeButtonGroup.add(blackPlayerType_remote);

        // -- layout blackPlayerTypePanel --
        GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

        // -- White --
        JPanel whitePlayerTypePanel = new JPanel(new GridBagLayout());
        whitePlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "White Player is ..."));
        ButtonGroup whitePlayerTypeButtonGroup = new ButtonGroup();

        // -- Human --
        JRadioButton whitePlayerType_Human = new JRadioButton("Human");
        whitePlayerType_Human.setEnabled(true);
        whitePlayerType_Human.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.HUMAN);
                blackPlayerType_remote.setEnabled(true);
            }
        });
        whitePlayerTypeButtonGroup.add(whitePlayerType_Human);

        // -- Computer --
        JRadioButton whitePlayerType_Computer = new JRadioButton("Computer");
        whitePlayerType_Computer.setEnabled(true);
        whitePlayerType_Computer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.COMPUTER);
                blackPlayerType_remote.setEnabled(true);
            }
        });
        whitePlayerTypeButtonGroup.add(whitePlayerType_Computer);

        // -- Computer --
        whitePlayerType_remote = new JRadioButton("Remote");
        whitePlayerType_remote.setEnabled(true);
        whitePlayerType_remote.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.REMOTE);
                blackPlayerType_remote.setEnabled(false);
            }
        });
        whitePlayerTypeButtonGroup.add(whitePlayerType_remote);
        
        // -- preselect player types BLACK --
        if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.HUMAN) {
            blackPlayerType_Human.setSelected(true);
        } else if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.COMPUTER) {
            blackPlayerType_Computer.setSelected(true);
        } else if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.REMOTE) {
            blackPlayerType_remote.setSelected(true);
            whitePlayerType_remote.setEnabled(false);
        }

        // -- preselect player types WHITE --
        if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.HUMAN) {
            whitePlayerType_Human.setSelected(true);
        } else if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.COMPUTER) {
            whitePlayerType_Computer.setSelected(true);
        } else if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.REMOTE) {
            whitePlayerType_remote.setSelected(true);
            blackPlayerType_remote.setEnabled(false);
        }

        // -- layout whitePlayerTypePanel --
        GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

        // -- layout inputPanel --
        GridBagHelper.constrain(_inputPanel, blackPlayerTypePanel, 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(_inputPanel, whitePlayerTypePanel, 2, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 2, 2, 2, 2);

    }

    /**
     * Start game and dispose a dialog instance on request.
     */
    private class StartButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _ui.getReversiController().startNewGame(blackName.getText(), whiteName.getText());
            dispose();
        }
    }

    /**
     * Dispose a dialog instance on request.
     */
    private class CancelButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }
}
