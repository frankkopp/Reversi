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
import fko.reversi.game.ReversiColor;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.Format;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class implements the main menu bar
 */
public class MainMenu extends JMenuBar {

    private ReversiGUI _ui;
    private MVController _MVController;

    private ButtonGroup blackLevelGroup;
    private JMenuItem blackLevel_other;

    private ButtonGroup whiteLevelGroup;
    private JMenuItem whiteLevel_other;

    private JCheckBoxMenuItem showMoveList;
    private JCheckBoxMenuItem showEngineInfoBlack;
    private JCheckBoxMenuItem showEngineInfoWhite;
    private JCheckBoxMenuItem showPossibleMoves;

    private JCheckBoxMenuItem remotePlayerServer;

    private ButtonGroup numberOfGamesGroup;
    private JMenuItem numberOfGamesExtras_other;

    private ButtonGroup boardDimensionGroup;
    private JMenuItem boardDimensionExtras_other;

    private static final Format digitFormat = new java.text.DecimalFormat("00");

    // -- Actions --
    private CommandAction newGameAction;
    private CommandAction stopGameAction;
    private CommandAction pauseGameAction;
    private CommandAction resumeGameAction;
    private CommandAction exitAction;
    private CommandAction timedGameAction;
    private CommandAction timeBlackAction;
    private CommandAction timeWhiteAction;
    private CommandAction blackLevelAction;
    private CommandAction whiteLevelAction;
    private CommandAction showPossibleMovesAction;
    private CommandAction showMovelistAction;
    private CommandAction numberOfGamesAction;
    private CommandAction boardDimensionAction;
    private CommandAction showEngineInfoBlackAction;
    private CommandAction showEngineInfoWhiteAction;
    private CommandAction remotePlayerServerAction;
    private CommandAction remotePlayerServerPortAction;
    private CommandAction remotePlayerClientBlackPortAction;
    private CommandAction remotePlayerClientBlackServerNameAction;
    private CommandAction remotePlayerClientWhiteServerNameAction;
    private CommandAction remotePlayerClientWhitePortAction;

    public MainMenu(ReversiGUI _ui2) {

        super();

        // -- back reference to the ui object and controller --
        this._ui = _ui2;
        this._MVController = _ui.getReversiController();

        try { // -- because we use Command.parse() --

            /* **********************************************************************
            // -- menu Level --
            ************************************************************************/
            JMenu menuGame = new JMenu("Game");
            add(menuGame);

            // -- NEW GAME --
            newGameAction = new CommandAction(
                    Command.parse(_MVController, "newGameDialog"),
                    "New Game ...", null, "New Game ...",
                    KeyStroke.getKeyStroke(78, java.awt.event.InputEvent.CTRL_MASK),
                    0,
                    true
            );
            menuGame.add(newGameAction);

            // -- STOP GAME --
            stopGameAction = new CommandAction(
                    Command.parse(_MVController, "stopCurrentGame"),
                    "Stop Game", null, "Stop Game",
                    KeyStroke.getKeyStroke(83, java.awt.event.InputEvent.CTRL_MASK),
                    0,
                    false
            );
            menuGame.add(stopGameAction);

            // -- PAUSE GAME --
            pauseGameAction = new CommandAction(
                    Command.parse(_MVController, "pauseOrResumeCurrentGame"),
                    "Pause Game", null, "Pause Game",
                    KeyStroke.getKeyStroke(80, java.awt.event.InputEvent.CTRL_MASK),
                    0,
                    false
            );
            menuGame.add(pauseGameAction);

            // -- RESUME GAME --
            resumeGameAction = new CommandAction(
                    Command.parse(_MVController, "pauseOrResumeCurrentGame"),
                    "Resume Game", null, "Resume Game",
                    KeyStroke.getKeyStroke(80, java.awt.event.InputEvent.CTRL_MASK),
                    0,
                    false
            );
            menuGame.add(resumeGameAction);

            menuGame.addSeparator();

            // -- EXIT --
            exitAction = new CommandAction(
                    Command.parse(_ui, "exitReversi"),
                    "Exit programm.", null, "Exit programm.",
                    KeyStroke.getKeyStroke(81, java.awt.event.InputEvent.CTRL_MASK),
                    0,
                    true
            );
            menuGame.add(exitAction);

            /* **********************************************************************
            // -- menu Level --
            ************************************************************************/
            JMenu menuLevel = new JMenu("Level");
            menuLevel.setEnabled(true);
            add(menuLevel);

            // -- timed game toggle --
            timedGameAction = new CommandAction(
                    Command.parse(_MVController, "toggleTimedGame"),
                    "Timed Game.", null, "Timed Game.",
                    null, 0,
                    true
            );
            JCheckBoxMenuItem timedGame = new JCheckBoxMenuItem(timedGameAction);
            timedGame.setState(Reversi.getPlayroom().isTimedGame());
            menuLevel.add(timedGame);

            // -- black time --
            timeBlackAction = new CommandAction(
                    Command.parse(
                            new timeBlackActionListener()
                            , "dialog"
                    ),
                    "Time Black...", null, "Time Black...",
                    null, 0,
                    true

            );
            menuLevel.add(timeBlackAction);

            // -- white time --
            timeWhiteAction = new CommandAction(
                    Command.parse(
                            new timeWhiteActionListener()
                            , "dialog"
                    ),
                    "Time White...", null, "Time White...",
                    null, 0,
                    true
            );
            menuLevel.add(timeWhiteAction);

            menuLevel.addSeparator();

            // -- black level --
            blackLevelAction = new CommandAction(
                    null,
                    "Black Level", null, "Black Level",
                    null, 0,
                    true
            );
            JMenu blackLevel = new JMenu(blackLevelAction);
            // -- will be set to true when player black is an engine
            menuLevel.add(blackLevel);

            // -- radio grouped --
            blackLevelGroup = new ButtonGroup();

            // Add levels to the black level group
            addBlackLevel(2, blackLevel);
            addBlackLevel(4, blackLevel);
            addBlackLevel(6, blackLevel);
            addBlackLevel(8, blackLevel);
            addBlackLevel(10, blackLevel);
            addBlackLevel(20, blackLevel);
            addBlackLevelOther(blackLevel);

            // -- select the initial level --
            String lb = Integer.toString(Reversi.getPlayroom().getCurrentEngineLevelBlack());
            Enumeration group = blackLevelGroup.getElements();
            boolean selected = false;
            selected = selectItem(group, lb, selected);
            if (!selected) {
                blackLevel_other.setSelected(true);
            }

            // -- white level --
            whiteLevelAction = new CommandAction(
                    null,
                    "White Level", null, "White Level",
                    null, 0,
                    true
            );
            JMenu whiteLevel = new JMenu(whiteLevelAction);
            // -- will be set to true when player black is an engine
            menuLevel.add(whiteLevel);

            // -- radio grouped --
            whiteLevelGroup = new ButtonGroup();

            addWhiteLevel(2, whiteLevel);
            addWhiteLevel(4, whiteLevel);
            addWhiteLevel(6, whiteLevel);
            addWhiteLevel(8, whiteLevel);
            addWhiteLevel(10, whiteLevel);
            addWhiteLevel(20, whiteLevel);
            addWhiteLevelOther(whiteLevel);

            // -- select the initial level --
            String lw = Integer.toString(Reversi.getPlayroom().getCurrentEngineLevelWhite());
            group = whiteLevelGroup.getElements();
            selected = false;
            selected = selectItem(group, lw, selected);
            if (!selected) {
                whiteLevel_other.setSelected(true);
            }

            /* **********************************************************************
            // -- menu Remote --
            ************************************************************************/

            // -- remote player server menu --
            JMenu menuRemote = new JMenu("Remote");
            add(menuRemote);

            // -- show move list --
            remotePlayerServerAction = new CommandAction(
                    Command.parse(_MVController, "toggleRemotePlayerServerAction"),
                    "Remote Player Server Enabled", null, "Enables or disables the remote player server",
                    null, 0,
                    true
            );
            remotePlayerServer = new JCheckBoxMenuItem(remotePlayerServerAction);
            remotePlayerServer.setState(Reversi.getPlayroom().getRemotePlayerServerEnabled());
            menuRemote.add(remotePlayerServer);

            // RemoteServer port
            remotePlayerServerPortAction = new CommandAction(
                    Command.parse(
                            new remotePlayerServerPortActionListener()
                            , "dialog"
                    ),
                    "RemoteServer Port...", null, "RemoteServer Port...",
                    null, 0,
                    true
            );
            menuRemote.add(remotePlayerServerPortAction);

            // RemoteClient server ip for player playing black
            remotePlayerClientBlackServerNameAction = new CommandAction(
                    Command.parse(
                            new remotePlayerClientBlackServerNameActionListener()
                            , "dialog"
                    ),
                    "RemoteClient (black) Hostname...", null, "RemoteClient (black) Hostname...",
                    null, 0,
                    true
            );
            menuRemote.add(remotePlayerClientBlackServerNameAction);

            // RemoteClient server port for player playing black
            remotePlayerClientBlackPortAction = new CommandAction(
                    Command.parse(
                            new remotePlayerClientBlackServerPortActionListener()
                            , "dialog"
                    ),
                    "RemoteClient (black) Port...", null, "RemoteClient (black) Port...",
                    null, 0,
                    true
            );
            menuRemote.add(remotePlayerClientBlackPortAction);

            // RemoteClient server ip for player playing white
            remotePlayerClientWhiteServerNameAction = new CommandAction(
                    Command.parse(
                            new remotePlayerClientBWhiteServerNameActionListener()
                            , "dialog"
                    ),
                    "RemoteClient (white) Hostname...", null, "RemoteClient (white) Hostname...",
                    null, 0,
                    true
            );
            menuRemote.add(remotePlayerClientWhiteServerNameAction);

            // RemoteClient server port for player playing white
            remotePlayerClientWhitePortAction = new CommandAction(
                    Command.parse(
                            new remotePlayerClientBWhiteServerPortActionListener()
                            , "dialog"
                    ),
                    "RemoteClient (white) Port...", null, "RemoteClient (white) Port...",
                    null, 0,
                    true
            );
            menuRemote.add(remotePlayerClientWhitePortAction);

            // -- extras menu --
            JMenu menuExtras = new JMenu("Extras");
            add(menuExtras);

            // -- show move list --
            showMovelistAction = new CommandAction(
                    Command.parse(_MVController, "toggleShowMoveListAction"),
                    "Show move list", null, "Show move list",
                    null, 0,
                    true
            );
            showMoveList = new JCheckBoxMenuItem(showMovelistAction);
            showMoveList.setState(_ui.is_showPossibleMoves());
            menuExtras.add(showMoveList);

            // -- show engine info black --
            showEngineInfoBlackAction = new CommandAction(
                    Command.parse(_MVController, "toggleShowEngineInfoBlackAction"),
                    "Show black engine info", null, "Show engine info for black player",
                    null, 0,
                    true
            );
            showEngineInfoBlack = new JCheckBoxMenuItem(showEngineInfoBlackAction);
            showEngineInfoBlack.setState(_ui.is_showEngineInfoWindowBlack());
            menuExtras.add(showEngineInfoBlack);

            // -- show engine info black --
            showEngineInfoWhiteAction = new CommandAction(
                    Command.parse(_MVController, "toggleShowEngineInfoWhiteAction"),
                    "Show white engine info", null, "Show engine info for white player",
                    null, 0,
                    true
            );
            showEngineInfoWhite = new JCheckBoxMenuItem(showEngineInfoWhiteAction);
            showEngineInfoWhite.setState(_ui.is_showEngineInfoWindowWhite());
            menuExtras.add(showEngineInfoWhite);

            // -- show possible move toggle --
            showPossibleMovesAction = new CommandAction(
                    Command.parse(_MVController, "toggleShowPossibleMovesAction"),
                    "Show possible moves", null, "Show possible moves",
                    null, 0,
                    true
            );
            showPossibleMoves = new JCheckBoxMenuItem(showPossibleMovesAction);
            showPossibleMoves.setState(_ui.is_showPossibleMoves());
            menuExtras.add(showPossibleMoves);

            menuExtras.addSeparator();

            // -- number of games --
            numberOfGamesAction = new CommandAction(
                    null,
                    "Number of games", null, "Sets the number of games to play in a row (good for computer vs. computer)",
                    null, 0,
                    true
            );
            JMenu numberOfGamesExtras = new JMenu(numberOfGamesAction);
            menuExtras.add(numberOfGamesExtras);

            // -- radio grouped --
            numberOfGamesGroup = new ButtonGroup();

            addNumberOfGamesMenuItem(1, numberOfGamesExtras);
            addNumberOfGamesMenuItem(5, numberOfGamesExtras);
            addNumberOfGamesMenuItem(10, numberOfGamesExtras);
            addNumberOfGamesMenuItem(20, numberOfGamesExtras);
            addNumberOfGamesMenuItem(50, numberOfGamesExtras);
            addNumberOfGamesMenuItem(100, numberOfGamesExtras);
            addNumberOfGamesOtherMenuItem(numberOfGamesExtras);

            // -- select the initial number --
            String nog = Integer.toString(Reversi.getPlayroom().getNumberOfGames());
            group = numberOfGamesGroup.getElements();
            selected = false;
            selected = selectItem(group, nog, selected);
            if (!selected) {
                numberOfGamesExtras_other.setSelected(true);
            }

            menuExtras.addSeparator();

            // -- board dimension --
            boardDimensionAction = new CommandAction(
                    null,
                    "Board dimensions", null, "Sets the board size",
                    null, 0,
                    true
            );
            JMenu boardDimensionExtras = new JMenu(boardDimensionAction);
            menuExtras.add(boardDimensionExtras);

            // -- radio grouped --
            boardDimensionGroup = new ButtonGroup();

            addBoardDimensionMenuItem(4, boardDimensionExtras);
            addBoardDimensionMenuItem(6, boardDimensionExtras);
            addBoardDimensionMenuItem(8, boardDimensionExtras);
            addBoardDimensionMenuItem(10, boardDimensionExtras);
            addBoardDimensionMenuItem(12, boardDimensionExtras);
            addBoardDimensionMenuItem(20, boardDimensionExtras);
            addBoardDimensionOtherMenuItem(boardDimensionExtras);

            // -- select the initial level --
            String bd = Integer.toString(Reversi.getPlayroom().getBoardDimension());
            group = boardDimensionGroup.getElements();
            selected = false;
            selected = selectItem(group, bd, selected);
            if (!selected) {
                boardDimensionExtras_other.setSelected(true);
            }

            menuExtras.addSeparator();

            menuExtras.add(initLookAndFeelMenu());

            // -- help menu --
            JMenu menuHelp = new JMenu("?");
            add(menuHelp);

            JMenuItem helpHelp = new JMenuItem("Help");
            helpHelp.setToolTipText("Not yet available!");
            helpHelp.setEnabled(false);
            menuHelp.add(helpHelp);

            menuHelp.addSeparator();

            JMenuItem helpAbout = new JMenuItem("About");
            helpAbout.setToolTipText("About this program");
            helpAbout.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AboutDialog dialog = new AboutDialog();
                    dialog.pack();
                    dialog.setResizable(false);
                    AbstractDialog.centerComponent(dialog);
                    dialog.setVisible(true);
                }
            });
            helpAbout.setEnabled(true);
            menuHelp.add(helpAbout);

        } catch (IOException e) { // because we use Command.parse()
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void addBlackLevel(final int level, JMenu blackLevel) {
        JMenuItem myBlackLevel = new JRadioButtonMenuItem(String.valueOf(level));
        myBlackLevel.setEnabled(true);
        myBlackLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _MVController.setLevelAction(ReversiColor.BLACK, level);
            }
        });
        blackLevel.add(myBlackLevel);
        blackLevelGroup.add(myBlackLevel);
    }

    private void addBlackLevelOther(JMenu blackLevel) {
        blackLevel_other = new JRadioButtonMenuItem("other");
        blackLevel_other.setEnabled(true);
        blackLevel_other.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // -- default question --
                String question = "Please enter a level for Black";
                String userInput;
                int level = 0;
                do { // -- loop until there is a valid input or cancel --
                    // -- get the input --
                    userInput = _ui.getMainWindow().userInputDialog(question, "Black Level");
                    try {
                        level = Integer.parseInt(userInput);
                    } catch (NumberFormatException nfe) {
                        // -- there was no integer in the input --
                        question = "The level for Black must be a valid postive number";
                    }
                    if (level < 1) { // -- level must be > 0 --
                        question = "The level for Black must be a valid postive number";
                        level = 0;
                    }
                } while (userInput != null && level == 0);
                // -- set level if not cancel has been pressed --
                if (userInput != null) {
                    _MVController.setLevelAction(ReversiColor.BLACK, level);
                }
                // -- select the current level --
                String lb = Integer.toString(Reversi.getPlayroom().getCurrentEngineLevelBlack());
                Enumeration group = blackLevelGroup.getElements();
                boolean selected = false;
                selected = selectItem(group, lb, selected);
                if (!selected) {
                    blackLevel_other.setSelected(true);
                }
            }
        });
        blackLevel.add(blackLevel_other);
        blackLevelGroup.add(blackLevel_other);
    }

    private void addWhiteLevel(final int level, JMenu whiteLevel) {
        JMenuItem myWhiteLevel = new JRadioButtonMenuItem(String.valueOf(level));
        myWhiteLevel.setEnabled(true);
        myWhiteLevel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _MVController.setLevelAction(ReversiColor.WHITE, level);
            }
        });
        whiteLevel.add(myWhiteLevel);
        whiteLevelGroup.add(myWhiteLevel);
    }

    private void addWhiteLevelOther(JMenu whiteLevel) {
        whiteLevel_other = new JRadioButtonMenuItem("other");
        whiteLevel_other.setEnabled(true);
        whiteLevel_other.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // -- default question --
                String question = "Please enter a level for White";
                String userInput;
                int level = 0;
                do { // -- loop until there is a valid input or cancel --
                    // -- get the input --
                    userInput = _ui.getMainWindow().userInputDialog(question, "White Level");
                    try {
                        level = Integer.parseInt(userInput);
                    } catch (NumberFormatException nfe) {
                        // -- there was no integer in the input --
                        question = "The level for White must be a valid postive number";
                    }
                    if (level < 1) { // -- level must be > 0 --
                        question = "The level for White must be a valid postive number";
                        level = 0;
                    }
                } while (userInput != null && level == 0);
                // -- set level if not cancel has been pressed --
                if (userInput != null) {
                    _MVController.setLevelAction(ReversiColor.WHITE, level);
                }
                // -- select the current level --
                String lb = Integer.toString(Reversi.getPlayroom().getCurrentEngineLevelWhite());
                Enumeration group = whiteLevelGroup.getElements();
                boolean selected = false;
                selected = selectItem(group, lb, selected);
                if (!selected) {
                    whiteLevel_other.setSelected(true);
                }
            }
        });
        whiteLevel.add(whiteLevel_other);
        whiteLevelGroup.add(whiteLevel_other);
    }

    private void addNumberOfGamesMenuItem(final int t, JMenu numberOfGamesExtras) {
        JMenuItem numberOfGamesExtras_1 = new JRadioButtonMenuItem(String.valueOf(t));
        numberOfGamesExtras_1.setEnabled(true);
        numberOfGamesExtras_1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _MVController.setNumberOfGamesAction(t);
            }
        });
        numberOfGamesExtras.add(numberOfGamesExtras_1);
        numberOfGamesGroup.add(numberOfGamesExtras_1);
    }

    private void addNumberOfGamesOtherMenuItem(JMenu numberOfGamesExtras) {
        numberOfGamesExtras_other = new JRadioButtonMenuItem("other");
        numberOfGamesExtras_other.setEnabled(true);
        numberOfGamesExtras_other.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // -- default question --
                String question = "Please enter the number of sequential games";
                String userInput;
                int number = 0;
                do { // -- loop until there is a valid input or cancel --
                    // -- get the input --
                    userInput = _ui.getMainWindow().userInputDialog(question, "Number of sequential games");
                    try {
                        number = Integer.parseInt(userInput);
                    } catch (NumberFormatException nfe) {
                        // -- there was no integer in the input --
                        question = "The number of games must be a valid postive number";
                    }
                    if (number < 1) { // -- level must be > 3 --
                        question = "The number of games must at least 1";
                        number = 0;
                    }
                } while (userInput != null && number == 0);
                // -- set number if not cancel has been pressed --
                if (userInput != null) {
                    _MVController.setNumberOfGamesAction(number);
                }
                // -- select the current level --
                String lb = Integer.toString(Reversi.getPlayroom().getBoardDimension());
                Enumeration group = numberOfGamesGroup.getElements();
                boolean selected = false;
                selected = selectItem(group, lb, selected);
                if (!selected) {
                    numberOfGamesExtras_other.setSelected(true);
                }
            }
        });
        numberOfGamesExtras.add(numberOfGamesExtras_other);
        numberOfGamesGroup.add(numberOfGamesExtras_other);
    }

    private void addBoardDimensionMenuItem(final int t, JMenu boardDimensionExtras) {
        JMenuItem boardDimensionExtras_4 = new JRadioButtonMenuItem(String.valueOf(t));
        boardDimensionExtras_4.setEnabled(true);
        boardDimensionExtras_4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _MVController.setBoardDimensionAction(t);
            }
        });
        boardDimensionExtras.add(boardDimensionExtras_4);
        boardDimensionGroup.add(boardDimensionExtras_4);
    }

    private void addBoardDimensionOtherMenuItem(JMenu boardDimensionExtras) {
        boardDimensionExtras_other = new JRadioButtonMenuItem("other");
        boardDimensionExtras_other.setEnabled(true);
        boardDimensionExtras_other.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // -- default question --
                String question = "Please enter a new board dimension";
                String userInput;
                int dimension = 0;
                do { // -- loop until there is a valid input or cancel --
                    // -- get the input --
                    userInput = _ui.getMainWindow().userInputDialog(question, "Board Dimension");
                    try {
                        dimension = Integer.parseInt(userInput);
                    } catch (NumberFormatException nfe) {
                        // -- there was no integer in the input --
                        question = "The board dimension must be a valid postive number";
                    }
                    if (dimension < 4) { // -- level must be > 3 --
                        question = "The board dimension must at least 4";
                        dimension = 0;
                    }
                } while (userInput != null && dimension == 0);
                // -- set level if not cancel has been pressed --
                if (userInput != null) {
                    _MVController.setBoardDimensionAction(dimension);
                }
                // -- select the current level --
                String lb = Integer.toString(Reversi.getPlayroom().getBoardDimension());
                Enumeration group = boardDimensionGroup.getElements();
                boolean selected = false;
                selected = selectItem(group, lb, selected);
                if (!selected) {
                    boardDimensionExtras_other.setSelected(true);
                }
            }
        });
        boardDimensionExtras.add(boardDimensionExtras_other);
        boardDimensionGroup.add(boardDimensionExtras_other);
    }

    private String getValidHostnameFromUser(String origQuestion, String dialogTitle, ReversiColor color) {
        String newHostname = null;
        String question = origQuestion;
        String userInput;
        do { // -- loop until there is a valid input or cancel --
            // -- get the input --
            String defaultString;
            if (color==ReversiColor.BLACK) {
                defaultString = String.valueOf(Reversi.getPlayroom().getRemotePlayerClientBlackServerName());
            } else {
                defaultString = String.valueOf(Reversi.getPlayroom().getRemotePlayerClientWhiteServerName());
            }
            // build dialog
            userInput = _ui.getMainWindow().userInputDialog(question, dialogTitle, defaultString);
            // -- cancel --
            if (userInput == null) {
                return null;
            }
            // Input must be a number betwenn 1024 and 65535
            try {
                // Check the address and ignore the output. If address is not valid an Exception is thrown.
                //noinspection ResultOfMethodCallIgnored
                InetAddress.getByName(userInput);
            } catch (UnknownHostException e1) {
                question = "Not a valid host!\n" + origQuestion;
                continue;
            }
            newHostname=userInput;
        } while (newHostname==null);
        return newHostname;
    }

    private static boolean selectItem(Enumeration group, String lb, boolean selected) {
        while (group.hasMoreElements()) {
            JRadioButtonMenuItem curItem = (JRadioButtonMenuItem) group.nextElement();
            if (curItem.getText().matches(lb)) {
                curItem.setSelected(true);
                selected = true;
            }
        }
        return selected;
    }

    private JMenu initLookAndFeelMenu() {
        // -- Look & Feel --
        JMenu lookAndFeel = new JMenu("Look & Feel");
        lookAndFeel.setToolTipText("Change the look & feel");
        lookAndFeel.setEnabled(true);

        // -- radio grouped --
        ButtonGroup lookAndFeelGroup = new ButtonGroup();

        // -- check for look & feels --
        UIManager.LookAndFeelInfo[] plafs = UIManager.getInstalledLookAndFeels();

        // -- create menueitem for all foound look&feels --
        for (UIManager.LookAndFeelInfo plaf : plafs) {
            String plafsName = plaf.getName();
            final String plafClassName = plaf.getClassName();
            //noinspection ObjectAllocationInLoop
            JMenuItem menuitem = lookAndFeel.add(new JRadioButtonMenuItem(plafsName+" ("+plafClassName+")"));
            // -- action listener --
            //noinspection ObjectAllocationInLoop
            menuitem.addActionListener(new lookAndFeelActionListener(plafClassName));
            lookAndFeelGroup.add(menuitem);
        }
        return lookAndFeel;
    }

    public JCheckBoxMenuItem getShowMoveList() {
        return showMoveList;
    }

    public JCheckBoxMenuItem getShowEngineInfoBlack() {
        return showEngineInfoBlack;
    }

    public JCheckBoxMenuItem getShowEngineInfoWhite() {
        return showEngineInfoWhite;
    }

    public JCheckBoxMenuItem getShowPossibleMoves() {
        return showPossibleMoves;
    }

    public JCheckBoxMenuItem getRemotePlayerServer() {
        return remotePlayerServer;
    }

    public CommandAction getNewGameAction() {
        return newGameAction;
    }

    public CommandAction getStopGameAction() {
        return stopGameAction;
    }

    public CommandAction getPauseGameAction() {
        return pauseGameAction;
    }

    public CommandAction getResumeGameAction() {
        return resumeGameAction;
    }

    public CommandAction getExitAction() {
        return exitAction;
    }

    public CommandAction getTimedGameAction() {
        return timedGameAction;
    }

    public CommandAction getTimeBlackAction() {
        return timeBlackAction;
    }

    public CommandAction getTimeWhiteAction() {
        return timeWhiteAction;
    }

    public CommandAction getBlackLevelAction() {
        return blackLevelAction;
    }

    public CommandAction getWhiteLevelAction() {
        return whiteLevelAction;
    }

    public CommandAction getShowPossibleMovesAction() {
        return showPossibleMovesAction;
    }

    public CommandAction getShowMovelistAction() {
        return showMovelistAction;
    }

    public CommandAction getNumberOfGamesAction() {
        return numberOfGamesAction;
    }

    public CommandAction getBoardDimensionAction() {
        return boardDimensionAction;
    }

    public CommandAction getShowEngineInfoBlackAction() {
        return showEngineInfoBlackAction;
    }

    public CommandAction getShowEngineInfoWhiteAction() {
        return showEngineInfoWhiteAction;
    }

    public CommandAction getRemotePlayerServerAction() {
        return remotePlayerServerAction;
    }

    public CommandAction getRemotePlayerServerPortAction() {
        return remotePlayerServerPortAction;
    }

    public CommandAction getRemotePlayerClientBlackPortAction() {
        return remotePlayerClientBlackPortAction;
    }

    public CommandAction getRemotePlayerClientBlackServerNameAction() {
        return remotePlayerClientBlackServerNameAction;
    }

    public CommandAction getRemotePlayerClientWhiteServerNameAction() {
        return remotePlayerClientWhiteServerNameAction;
    }

    public CommandAction getRemotePlayerClientWhitePortAction() {
        return remotePlayerClientWhitePortAction;
    }

    private class lookAndFeelActionListener implements ActionListener {
        private final String plafClassName;

        private lookAndFeelActionListener(String aPlafClassName) {
            this.plafClassName = aPlafClassName;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                e.getSource();
                UIManager.setLookAndFeel(plafClassName);
                SwingUtilities.updateComponentTreeUI(_ui.getMainWindow());
                SwingUtilities.updateComponentTreeUI(_ui.getMoveListWindow());
                SwingUtilities.updateComponentTreeUI(_ui.getEngineInfoWindowBlack());
                SwingUtilities.updateComponentTreeUI(_ui.getEngineInfoWindowWhite());
            } catch (Exception ex) {
                //noinspection UseOfSystemOutOrSystemErr
                System.err.println(ex);
            }
        }
    }

    private class timeWhiteActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // -- default question --
            String origQuestion = "Please enter the time available for white (min:sec)";
            String question = origQuestion;
            String userInput;
            int timeInMilliSec = 0;
            do { // -- loop until there is a valid input or cancel --
                // -- get the input --
                long timeWhite = Reversi.getPlayroom().getTimeWhite() / 1000;
                String defaultString = (timeWhite / 60) + ":" + digitFormat.format((timeWhite % 60));
                userInput = _ui.getMainWindow().userInputDialog(question, "Time White", defaultString);
                // -- cancel --
                if (userInput == null) {
                    return;
                }
                // -- input must be of format min:sec --
                // -- ^((\d*:[0-5])?|(:[0-5])?|(\d*)?)\d$ --
                if (!(userInput.matches("^\\d$")
                        || userInput.matches("^\\d*\\d$")
                        || userInput.matches("^:[0-5]\\d$")
                        || userInput.matches("^\\d*:[0-5]\\d$")
                )
                        ) {
                    question = "Wrong Format\n" + origQuestion;
                    continue;
                } else {
                    question = origQuestion;
                    // -- is there a colon? --
                    int indexOfColon;
                    if ((indexOfColon = userInput.indexOf(':')) >= 0) {
                        // -- is it at the first place? --
                        if (userInput.startsWith(":")) {
                            // -- if there is a colon we only allow numbers <60
                            timeInMilliSec = Integer.parseInt(userInput.substring(1));
                        } else { // -- not starting with a colon --
                            int min = Integer.parseInt(userInput.substring(0, indexOfColon));
                            int sec = Integer.parseInt(userInput.substring(indexOfColon + 1));
                            timeInMilliSec = min * 60 + sec;
                        }
                    } else { // -- no colon --
                        try {
                            timeInMilliSec = Integer.parseInt(userInput);
                        } catch (NumberFormatException ex) {
                            question = "Not a valid time in sec. Too big?\n" + origQuestion;
                        }
                    }
                }
            } while (timeInMilliSec == 0);
            // -- set level if not cancel has been pressed--
            _MVController.setTimeWhiteAction(timeInMilliSec * 1000);
        }
    }

    private class timeBlackActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // -- default question --
            String origQuestion = "Please enter the time available for black (min:sec)";
            String question = origQuestion;
            String userInput;
            int timeInMilliSec = 0;
            do { // -- loop until there is a valid input or cancel --
                // -- get the input --
                long timeBlack = Reversi.getPlayroom().getTimeBlack() / 1000;
                String defaultString = (timeBlack / 60) + ":" + digitFormat.format((timeBlack % 60));
                userInput = _ui.getMainWindow().userInputDialog(question, "Time Black", defaultString);
                // -- cancel --
                if (userInput == null) {
                    return;
                }
                // -- input must be of format min:sec --
                // -- ^((\d*:[0-5])?|(:[0-5])?|(\d*)?)\d$ --
                if (!(userInput.matches("^\\d$")
                        || userInput.matches("^\\d*\\d$")
                        || userInput.matches("^:[0-5]\\d$")
                        || userInput.matches("^\\d*:[0-5]\\d$")
                )
                        ) {
                    question = "Wrong Format\n" + origQuestion;
                    continue;
                } else {
                    question = origQuestion;
                    // -- is there a colon? --
                    int indexOfColon;
                    if ((indexOfColon = userInput.indexOf(':')) >= 0) {
                        // -- is it at the first place? --
                        if (userInput.startsWith(":")) {
                            // -- if there is a colon we only allow numbers <60
                            timeInMilliSec = Integer.parseInt(userInput.substring(1));
                        } else { // -- not starting with a colon --
                            int min = Integer.parseInt(userInput.substring(0, indexOfColon));
                            int sec = Integer.parseInt(userInput.substring(indexOfColon + 1));
                            timeInMilliSec = min * 60 + sec;
                        }
                    } else { // -- no colon --
                        try {
                            timeInMilliSec = Integer.parseInt(userInput);
                        } catch (NumberFormatException ex) {
                            question = "Not a valid time in sec. Too big?\n" + origQuestion;
                        }
                    }
                }
            } while (timeInMilliSec == 0);
            // -- set level if not cancel has been pressed--
            _MVController.setTimeBlackAction(timeInMilliSec * 1000);
        }
    }

    private class remotePlayerServerPortActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // Question
            String origQuestion = "Please enter the port the RemoteServer shall use:\n"
                    +"(Restart the RemoteServer to activate the new setting.)";
            String question = origQuestion;
            String userInput;
            int newPort = 0;
            do { // -- loop until there is a valid input or cancel --
                // -- get the input --
                String defaultString = String.valueOf(Reversi.getPlayroom().getRemotePlayerServerPort());
                userInput = _ui.getMainWindow().userInputDialog(question, "RemoteServer Port", defaultString);
                // -- cancel --
                if (userInput == null) {
                    return;
                }
                // Input must be a number betwenn 1024 and 65535
                int port;
                try {
                    port = Integer.parseInt(userInput);
                } catch(NumberFormatException nfe) {
                    question = "Not a valid number!\n" + origQuestion;
                    continue;
                }
                if (port < 1024 || port > 65535) {
                    question = "Not a valid port!\n" + origQuestion;
                    continue;
                } else {
                    newPort = port;
                }
            } while (newPort == 0);
            // -- set level if not cancel has been pressed--
            _MVController.setRemoteServerPortAction(newPort);
        }
    }

    private class remotePlayerClientBlackServerNameActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // Question
            String origQuestion = "Please enter the hostname of the server the RemotePlayer playing black shall connect to:\n"
                    +"(Is used for the next connection.)";
            String dialogTitle = "RemoteClient (black) Server Name";
            String newHostname = getValidHostnameFromUser(origQuestion, dialogTitle, ReversiColor.BLACK);
            if (newHostname==null) {
                return;
            }
            // -- set level if not cancel has been pressed--
            _MVController.setRemoteClientBlackServerNameAction(newHostname);
        }
    }

    private class remotePlayerClientBlackServerPortActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // Question
            String origQuestion = "Please enter the port the RemotePlayer playing black shall use:\n"
                    +"(Is used for the next connection.)";
            String question = origQuestion;
            String userInput;
            int newPort = 0;
            do { // -- loop until there is a valid input or cancel --
                // -- get the input --
                String defaultString = String.valueOf(Reversi.getPlayroom().getRemotePlayerClientBlackServerPort());
                userInput = _ui.getMainWindow().userInputDialog(question, "RemoteClient (black) Port", defaultString);
                // -- cancel --
                if (userInput == null) {
                    return;
                }
                // Input must be a number betwenn 1024 and 65535
                int port;
                try {
                    port = Integer.parseInt(userInput);
                } catch(NumberFormatException nfe) {
                    question = "Not a valid number!\n" + origQuestion;
                    continue;
                }
                if (port < 1024 || port > 65535) {
                    question = "Not a valid port!\n" + origQuestion;
                    continue;
                } else {
                    newPort = port;
                }
            } while (newPort == 0);
            // -- set level if not cancel has been pressed--
            _MVController.setRemoteClientBlackPortAction(newPort);
        }
    }

    private class remotePlayerClientBWhiteServerNameActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // Question
            String origQuestion = "Please enter the hostname of the server the RemotePlayer playing white shall connect to:\n"
                    +"(Is used for the next connection.)";
            String dialogTitle = "RemoteClient (white) Server Name";
            String newHostname = getValidHostnameFromUser(origQuestion, dialogTitle, ReversiColor.WHITE);
            if (newHostname==null) {
                return;
            }
            // -- set level if not cancel has been pressed--
            _MVController.setRemoteClientWhiteServerNameAction(newHostname);
        }
    }

    private class remotePlayerClientBWhiteServerPortActionListener implements ActionListener {
        public void dialog() {
            actionPerformed(null);
        }

        public void actionPerformed(ActionEvent e) {
            // Question
            String origQuestion = "Please enter the port the RemotePlayer playing white shall use:\n"
                    +"(Is used for the next connection.)";
            String question = origQuestion;
            String userInput;
            int newPort = 0;
            do { // -- loop until there is a valid input or cancel --
                // -- get the input --
                String defaultString = String.valueOf(Reversi.getPlayroom().getRemotePlayerClientWhiteServerPort());
                userInput = _ui.getMainWindow().userInputDialog(question, "RemoteClient (white) Port", defaultString);
                // -- cancel --
                if (userInput == null) {
                    return;
                }
                // Input must be a number betwenn 1024 and 65535
                int port;
                try {
                    port = Integer.parseInt(userInput);
                } catch(NumberFormatException nfe) {
                    question = "Not a valid number!\n" + origQuestion;
                    continue;
                }
                if (port < 1024 || port > 65535) {
                    question = "Not a valid port!\n" + origQuestion;
                    continue;
                } else {
                    newPort = port;
                }
            } while (newPort == 0);
            // -- set level if not cancel has been pressed--
            _MVController.setRemoteClientWhitePortAction(newPort);
        }
    }
}