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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This dialog is shown when the user clicked on New Game in Menu Game or Ctrl+N to start
 * a new game.
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class RemoteGameRequestPendingDialog extends AbstractDialog {

    private ReversiGUI _ui;

    public RemoteGameRequestPendingDialog(ReversiGUI uiParam) {
        super(uiParam.getMainWindow(), "Remote Game Request Pending", true);
        _ui = uiParam;

        setName("RemoteGameRequestPendingDialog");
        setTitle("Game Request from RemotePlayerServer is pending");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(true);

        // -- close dialog handler --
        this.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent e) {
                cancelRemoteGameRequest();
            }
        });

        // create gui
        getContentPane().setLayout(new BorderLayout());

        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());

        // -- BUTTONS --
        JPanel buttonPanel = new JPanel(new BorderLayout());
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new RemoteGameRequestPendingDialog.CancelButtonAction());
        buttonPanel.add(cancelButton, BorderLayout.CENTER);

        JLabel text = new JLabel("Waiting for confirmation to new game from remote client!");
        pane.add(text);

        getContentPane().add(pane,BorderLayout.CENTER);
        getContentPane().add(buttonPanel,BorderLayout.SOUTH);

        // -- set default button --
        getRootPane().setDefaultButton(cancelButton);

        // -- pack --
        pack();
    }

    /**
     * Cancels the remote game request
     */
    private void cancelRemoteGameRequest() {
        _ui.getReversiController().cancelRemoteGameRequest();
    }

    /**
     * Dispose a dialog instance on request.
     */
    private class CancelButtonAction implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            cancelRemoteGameRequest();
        }
    }
}
