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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The SatelliteWindow class extends a JDialog mainly with the ability to store its window position
 * with the help of ReversiGUI.getWindowState().
 */
public class SatelliteWindow extends JDialog {

    // -- my label to store and restore the windows --
    private String _myLabel;

    // -- dispose or setViable=false --
    private boolean _dispose = false;

    /**
     * Creates a new, initially invisible <code>Frame</code> with the
     * specified title.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @param title the title for the frame
     * @param label a label for the frame to store and restore the last window size and position
     */
    public SatelliteWindow(Frame frame, String title, String label) {
        super(frame, title);

        // -- only keep word characters [a-zA-Z_0-9] --
        this._myLabel = label.trim().replaceAll("\\W+","");

        // -- get last window position ans size--
        int windowLocX = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowLocationX_"+_myLabel) == null
            ? "100" : ReversiGUI.getWindowState().getProperty("windowLocationX_"+_myLabel));
        int windowLocY = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowLocationY_"+_myLabel) == null
            ? "200" : ReversiGUI.getWindowState().getProperty("windowLocationY_"+_myLabel));
        int windowSizeX = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowSizeX_"+_myLabel) == null
            ? "600" : ReversiGUI.getWindowState().getProperty("windowSizeX_"+_myLabel));
        int windowSizeY = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowSizeY_"+_myLabel) == null
            ? "800" : ReversiGUI.getWindowState().getProperty("windowSizeY_"+_myLabel));

                // -- position and resize the window  --
        this.setLocation(windowLocX, windowLocY);
        this.setSize(new Dimension(windowSizeX, windowSizeY));

        // -- handle closing of window with the closing listener --
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // -- close frame handler --
        this.addWindowListener(new WindowAdapter() {
            @Override
			public void windowClosing(WindowEvent e) {
                closeWindowAction();
            }
        });

        // -- set layout of content pane
        getContentPane().setLayout(new BorderLayout());

     }

    /**
     * returns the setting of the dispose mode
     * @return true if window will be disposed on close, false when window will be set invisible
     */
    public boolean is_dispose() {
        return _dispose;
    }

    /**
     * set mode for window close
     * @param newDisposeMode true for dispose on close, false for set invisible on close
     */
    public void set_dispose(boolean newDisposeMode) {
        this._dispose = newDisposeMode;
    }

    /**
     * Is called to close the window. This also stores the current windows state to a file.
     * <p/>
     * mainWindow.setLocation(windowLocX, windowLocY);
     * mainWindow.setSize(new Dimension(windowSizeX, windowSizeY));
     */
    protected synchronized void closeWindowAction() {
        ReversiGUI.getWindowState().setProperty("windowLocationX_"+_myLabel, String.valueOf(this.getLocation().x));
        ReversiGUI.getWindowState().setProperty("windowLocationY_"+_myLabel, String.valueOf(this.getLocation().y));
        ReversiGUI.getWindowState().setProperty("windowSizeX_"+_myLabel, String.valueOf(this.getSize().width));
        ReversiGUI.getWindowState().setProperty("windowSizeY_"+_myLabel, String.valueOf(this.getSize().height));
        if (_dispose) {
            this.dispose();
        } else {
            this.setVisible(false);
        }
    }


}
