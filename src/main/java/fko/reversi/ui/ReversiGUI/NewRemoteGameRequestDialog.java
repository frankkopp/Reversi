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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import fko.reversi.Reversi;
import fko.reversi.player.PlayerType;
import fko.reversi.remote.RemoteGameRequest;

/**
 * This dialog is shown when the user clicked on New Game in Menu Game or Ctrl+N to start
 * a new game.
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class NewRemoteGameRequestDialog extends AbstractDialog {

    private ReversiGUI _ui;
    private RemoteGameRequest _gr;

    private JTextField blackName, whiteName;
    private JPanel _infoPanel;


    public NewRemoteGameRequestDialog(ReversiGUI uiParam, RemoteGameRequest remoteGameRequest) {
        super(uiParam.getMainWindow(), "New Game", true);
        _ui = uiParam;
        _gr = remoteGameRequest;

        setName("NewRemoteGameRequestDialog");
        setTitle("New Game Request from RemotePlayerServer");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(true);

        // -- close dialog handler --
        this.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent e) {
                _ui.getReversiController().acceptRemoteGameRequest(false);
                dispose();
            }
        });

        // create gui
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        getContentPane().setLayout(new GridBagLayout());

        // -- BUTTONS --
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        // Start button
        JButton startButton = new JButton("Start");
        startButton.addActionListener(new NewRemoteGameRequestDialog.StartButtonAction());
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new NewRemoteGameRequestDialog.CancelButtonAction());
        GridBagHelper.constrain(buttonPanel, startButton, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(buttonPanel, cancelButton, 2, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);

        setupDialog();

        // -- layout pane --
        GridBagHelper.constrain(getContentPane(), pane, 0, 0, 0, 0, GridBagConstraints.VERTICAL, GridBagConstraints.NORTH, 1.0, 1.0, 0, 0, 0, 0);
        GridBagHelper.constrain(pane, _infoPanel, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(pane, new JPanel(), 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);
        GridBagHelper.constrain(pane, buttonPanel, 1, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 5, 8, 5, 8);

        // -- set default button --
        getRootPane().setDefaultButton(startButton);

        // -- pack --
        pack();

    }

    private void setupDialog() {

        _infoPanel = new JPanel(new GridBagLayout());

        JPanel blackNamePanel, blackPlayerTypePanel, whiteNamePanel, whitePlayerTypePanel;

        // -- Players Names --
        if (_gr.getLocalPlayerColor().isBlack()) {

            blackNamePanel = new JPanel();
            blackName = new JTextField(_gr.getLocalPlayerName());
            blackName.setColumns(15);
            blackNamePanel.add(blackName);
            blackNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name Black Player"));
            blackName.setEditable(false);

            whiteNamePanel = new JPanel();
            whiteName = new JTextField(Reversi.getPlayroom().getNameWhitePlayer());
            whiteName.setColumns(15);
            whiteNamePanel.add(whiteName);
            whiteNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name White Player"));

            GridBagHelper.constrain(_infoPanel, blackNamePanel, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(_infoPanel, whiteNamePanel, 2, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

            // -- Player Types --

            // -- Black --
            blackPlayerTypePanel = new JPanel(new GridBagLayout());
            blackPlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "Black Player is ..."));
            ButtonGroup blackPlayerTypeButtonGroup = new ButtonGroup();

            // -- Human --
            JRadioButton blackPlayerType_Human = new JRadioButton("Human");
            blackPlayerType_Human.setEnabled(true);
            blackPlayerType_Human.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.HUMAN);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Human);

            // -- Computer --
            JRadioButton blackPlayerType_Computer = new JRadioButton("Computer");
            blackPlayerType_Computer.setEnabled(true);
            blackPlayerType_Computer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.COMPUTER);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Computer);

            // -- Remote --
            JRadioButton blackPlayerType_Remote = new JRadioButton("Remote");
            blackPlayerType_Remote.setEnabled(true);
            blackPlayerType_Remote.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.REMOTE);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Remote);

            // -- preselect player types --
            if (_gr.getLocalPlayerType() == PlayerType.HUMAN) {
                blackPlayerType_Human.setSelected(true);
                blackPlayerType_Human.setEnabled(true);
                blackPlayerType_Computer.setEnabled(false);
                blackPlayerType_Remote.setEnabled(false);
            } else if (_gr.getLocalPlayerType() == PlayerType.COMPUTER) {
                blackPlayerType_Computer.setSelected(true);
                blackPlayerType_Human.setEnabled(false);
                blackPlayerType_Computer.setEnabled(true);
                blackPlayerType_Remote.setEnabled(false);
            } else if (_gr.getLocalPlayerType() == PlayerType.REMOTE) {
                blackPlayerType_Remote.setSelected(true);
                blackPlayerType_Human.setEnabled(false);
                blackPlayerType_Computer.setEnabled(false);
                blackPlayerType_Remote.setEnabled(true);
            }

            // -- layout blackPlayerTypePanel --
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

            // -- White --
            whitePlayerTypePanel = new JPanel(new GridBagLayout());
            whitePlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "White Player is ..."));
            ButtonGroup whitePlayerTypeButtonGroup = new ButtonGroup();

            // -- Human --
            JRadioButton whitePlayerType_Human = new JRadioButton("Human");
            whitePlayerType_Human.setEnabled(true);
            whitePlayerType_Human.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.HUMAN);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Human);

            // -- Computer --
            JRadioButton whitePlayerType_Computer = new JRadioButton("Computer");
            whitePlayerType_Computer.setEnabled(true);
            whitePlayerType_Computer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.COMPUTER);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Computer);

            // -- Computer --
            JRadioButton whitePlayerType_Remote = new JRadioButton("Remote");
            whitePlayerType_Remote.setEnabled(true);
            whitePlayerType_Remote.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.REMOTE);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Remote);

            // -- preselect player types --
            if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.HUMAN) {
                whitePlayerType_Human.setSelected(true);
            } else if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.COMPUTER) {
                whitePlayerType_Computer.setSelected(true);
            } else if (Reversi.getPlayroom().getPlayerTypeWhite() == PlayerType.REMOTE) {
                whitePlayerType_Remote.setSelected(true);
            }

            // -- layout whitePlayerTypePanel --
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

        } else if (_gr.getLocalPlayerColor().isWhite()) {

            blackNamePanel = new JPanel();
            blackName = new JTextField(Reversi.getPlayroom().getNameBlackPlayer());
            blackName.setColumns(15);
            blackNamePanel.add(blackName);
            blackNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name Black Player"));

            whiteNamePanel = new JPanel();
            whiteName = new JTextField(_gr.getLocalPlayerName());
            whiteName.setColumns(15);
            whiteNamePanel.add(whiteName);
            whiteNamePanel.setBorder(new TitledBorder(new EtchedBorder(), "Name White Player"));
            whiteName.setEditable(false);

            GridBagHelper.constrain(_infoPanel, blackNamePanel, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(_infoPanel, whiteNamePanel, 2, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

            // -- Player Types --

            // -- Black --
            blackPlayerTypePanel = new JPanel(new GridBagLayout());
            blackPlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "Black Player is ..."));
            ButtonGroup blackPlayerTypeButtonGroup = new ButtonGroup();

            // -- Human --
            JRadioButton blackPlayerType_Human = new JRadioButton("Human");
            blackPlayerType_Human.setEnabled(true);
            blackPlayerType_Human.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.HUMAN);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Human);

            // -- Computer --
            JRadioButton blackPlayerType_Computer = new JRadioButton("Computer");
            blackPlayerType_Computer.setEnabled(true);
            blackPlayerType_Computer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.COMPUTER);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Computer);

            // -- Remote --
            JRadioButton blackPlayerType_Remote = new JRadioButton("Remote");
            blackPlayerType_Remote.setEnabled(true);
            blackPlayerType_Remote.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeBlackAction(PlayerType.REMOTE);
                }
            });
            blackPlayerTypeButtonGroup.add(blackPlayerType_Remote);

            // -- layout blackPlayerTypePanel --
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(blackPlayerTypePanel, blackPlayerType_Remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

            // -- preselect player types --
            if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.HUMAN) {
                blackPlayerType_Human.setSelected(true);
            } else if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.COMPUTER) {
                blackPlayerType_Computer.setSelected(true);
            } else if (Reversi.getPlayroom().getPlayerTypeBlack() == PlayerType.REMOTE) {
                blackPlayerType_Remote.setSelected(true);
            }

            // -- White --
            whitePlayerTypePanel = new JPanel(new GridBagLayout());
            whitePlayerTypePanel.setBorder(new TitledBorder(new EtchedBorder(), "White Player is ..."));
            ButtonGroup whitePlayerTypeButtonGroup = new ButtonGroup();

            // -- Human --
            JRadioButton whitePlayerType_Human = new JRadioButton("Human");
            whitePlayerType_Human.setEnabled(true);
            whitePlayerType_Human.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.HUMAN);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Human);

            // -- Computer --
            JRadioButton whitePlayerType_Computer = new JRadioButton("Computer");
            whitePlayerType_Computer.setEnabled(true);
            whitePlayerType_Computer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.COMPUTER);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Computer);

            // -- Computer --
            JRadioButton whitePlayerType_Remote = new JRadioButton("Remote");
            whitePlayerType_Remote.setEnabled(true);
            whitePlayerType_Remote.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _ui.getReversiController().setPlayerTypeWhiteAction(PlayerType.REMOTE);
                }
            });
            whitePlayerTypeButtonGroup.add(whitePlayerType_Remote);

            // -- preselect player types --
            // -- preselect player types --
            if (_gr.getLocalPlayerType() == PlayerType.HUMAN) {
                whitePlayerType_Human.setSelected(true);
                whitePlayerType_Human.setEnabled(true);
                whitePlayerType_Computer.setEnabled(false);
                whitePlayerType_Remote.setEnabled(false);
            } else if (_gr.getLocalPlayerType() == PlayerType.COMPUTER) {
                whitePlayerType_Computer.setSelected(true);
                whitePlayerType_Human.setEnabled(false);
                whitePlayerType_Computer.setEnabled(true);
                whitePlayerType_Remote.setEnabled(false);
            } else if (_gr.getLocalPlayerType() == PlayerType.REMOTE) {
                whitePlayerType_Remote.setSelected(true);
                whitePlayerType_Human.setEnabled(false);
                whitePlayerType_Computer.setEnabled(false);
                whitePlayerType_Remote.setEnabled(true);
            }

            // -- layout whitePlayerTypePanel --
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Human,    1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Computer, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);
            GridBagHelper.constrain(whitePlayerTypePanel, whitePlayerType_Remote,   1, 3, 1, 1, GridBagConstraints.EAST, GridBagConstraints.WEST, 1.0, 0.0, 2, 2, 2, 2);

        } else {
            throw new RuntimeException("Neither BLACK nor WHITE");
        }

        // -- layout inputPanel --
        GridBagHelper.constrain(_infoPanel, blackPlayerTypePanel, 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 2, 2, 2, 2);
        GridBagHelper.constrain(_infoPanel, whitePlayerTypePanel, 2, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.CENTER, 0.0, 0.0, 2, 2, 2, 2);

    }

    /**
     * Start game and dispose a dialog instance on request.
     */
    private class StartButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _ui.getReversiController().acceptRemoteGameRequest(true);
            dispose();
        }
    }

    /**
     * Dispose a dialog instance on request.
     */
    private class CancelButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            _ui.getReversiController().acceptRemoteGameRequest(false);
            dispose();
        }
    }
}
