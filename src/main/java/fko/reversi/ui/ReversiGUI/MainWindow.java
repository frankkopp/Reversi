/*
 * <p>GPL Disclaimer</p>
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

import javax.swing.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This class represents the main window (JFrame) itself
 */
public class MainWindow extends JFrame implements KeyListener {
   
	private static final long serialVersionUID = 1L;

	// -- back reference to the ui object
    private ReversiGUI _ui;

    // -- our menu --
    private MainMenu _reversiMenu;

    // -- components --
    private JPanel        _mainPanel;
    private JSplitPane    _workPane;
    private StatusPanel   _statusPanel;

    /**
     * Constructs a new frame that is initially invisible.
     */
    public MainWindow(ReversiGUI reversiGUI) {

        super("Reversi by Frank Kopp (c) 2003-2006 (Version: " + Reversi.VERSION + ')');

        // -- backreference to the ui object
        this._ui = reversiGUI;

        // -- get last window position and size--
        int windowLocX = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowLocationX") == null
            ? "100" : ReversiGUI.getWindowState().getProperty("windowLocationX"));
        int windowLocY = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowLocationY") == null
            ? "200" : ReversiGUI.getWindowState().getProperty("windowLocationY"));
        int windowSizeX = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowSizeX") == null
            ? "600" : ReversiGUI.getWindowState().getProperty("windowSizeX"));
        int windowSizeY = Integer.parseInt(
            ReversiGUI.getWindowState().getProperty("windowSizeY") == null
            ? "800" : ReversiGUI.getWindowState().getProperty("windowSizeY"));

        // -- position and resize the window  --
        this.setLocation(windowLocX, windowLocY);
        this.setSize(new Dimension(windowSizeX, windowSizeY));

        // -- handle closing of window with the closing listener --
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
                _ui.exitReversi();
            }
        });

        // -- key inputs --
        this.addKeyListener(this);

        // -- set layout of content pane
        getContentPane().setLayout(new BorderLayout());

        // ----------------------------------------------------
        // -- window components --

        // -- set menu --
        _reversiMenu = new MainMenu(_ui);
        this.setJMenuBar(_reversiMenu);

        // -- main panel --
        _mainPanel = new JPanel(new BorderLayout());
        getContentPane().add(_mainPanel, BorderLayout.CENTER);

        // -- split panel
        _workPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        _workPane.setOneTouchExpandable(true);
        _workPane.setContinuousLayout(true);

        // -- set divider location to last known position --
        int dividerLocation =
            ReversiGUI.getWindowState().getProperty("dividerLocation") == null ?
                _workPane.getHeight()
                - _workPane.getInsets().bottom
                - _workPane.getInsets().top
                - _workPane.getDividerSize()
            : Integer.parseInt(ReversiGUI.getWindowState().getProperty("dividerLocation"));
        _workPane.setResizeWeight(0.5);
        _workPane.setDividerLocation(dividerLocation);

        _mainPanel.add(_workPane, BorderLayout.CENTER);

        // -- status line --
        _statusPanel = new StatusPanel();
        getContentPane().add(_statusPanel, BorderLayout.SOUTH);

        // -- keyboard events --
        setFocusable(true);

    }

    /**
     * Is called to close the window. This also stores the current windows state to a file.
     */
    protected synchronized void closeWindowAction() {
        ReversiGUI.getWindowState().setProperty("windowLocationX", String.valueOf(this.getLocation().x));
        ReversiGUI.getWindowState().setProperty("windowLocationY", String.valueOf(this.getLocation().y));
        ReversiGUI.getWindowState().setProperty("windowSizeX", String.valueOf(this.getSize().width));
        ReversiGUI.getWindowState().setProperty("windowSizeY", String.valueOf(this.getSize().height));
        ReversiGUI.getWindowState().setProperty("dividerLocation", String.valueOf(this._workPane.getDividerLocation()));
        this.dispose();
    }

    /**
     * user yes or no question
     *
     * @param question
     * @return int
     */
    protected int userConfirmation(String question) {
        return JOptionPane.showConfirmDialog(
                this,
                question,
                "Question",
                JOptionPane.YES_NO_OPTION);
    }

    /**
     * Creates a message box asking the user for input
     *
     * @param question
     * @param title
     * @return the string with the user input
     */
    protected String userInputDialog(String question, String title) {
        return JOptionPane.showInputDialog(
                this,
                question,
                title,
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Creates a message box asking the user for input
     *
     * @param question
     * @param title
     * @param defString
     * @return String
     */
    protected String userInputDialog(String question, String title, String defString) {
        return (String) JOptionPane.showInputDialog(
                this,
                question,
                title,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defString);
    }

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link java.awt.event.KeyEvent} for a definition of
     * a key typed event.
     */
    public void keyTyped(KeyEvent e) {
        //System.out.printf(Integer.toString(e.getKeyCode())+"\n");
    }

    /**
     * Invoked when a key has been pressed.
     * See the class description for {@link java.awt.event.KeyEvent} for a definition of
     * a key pressed event.
     */
    public void keyPressed(KeyEvent e) {
        /*if (Reversi.is_debug()) {
            System.out.printf(Integer.toString(e.getKeyCode())+" : " + e.getKeyChar() + "\n");
        }*/
    }

    /**
     * Invoked when a key has been released.
     * See the class description for {@link java.awt.event.KeyEvent} for a definition of
     * a key released event.
     */
    public void keyReleased(KeyEvent e) {
        //System.out.printf(Integer.toString(e.getKeyCode())+"\n");
    }

    public MainMenu getReversiMenu() {
        return _reversiMenu;
    }

    public JPanel getMainPanel() {
        return _mainPanel;
    }

    public JSplitPane getWorkPane() {
        return _workPane;
    }

    public StatusPanel getStatusPanel() {
        return _statusPanel;
    }
}
