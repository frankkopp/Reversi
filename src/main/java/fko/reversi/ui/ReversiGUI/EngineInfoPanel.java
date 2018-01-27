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
import fko.reversi.game.*;
import fko.reversi.game.ReversiColor;
import fko.reversi.player.ComputerPlayer;
import fko.reversi.player.Player;
import fko.reversi.player.computer.TreeSearch.TreeSearchEngineWatcher;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.Format;

/**
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
 * <hr/>
 *
 */
public class EngineInfoPanel extends JPanel {

    private final Playroom _model;

    private Thread updater = new updateThread();

    // -- if an engine color is set then the panel only shows this engine --
    private final ReversiColor _engineColor;

    private boolean _abort = false;

    private JLabel curMove, curValue, curDepth, curNodes, curSpeed, curTime;
    private JLabel curBoards, curNonQuiet, curCacheMisses, curCacheHits, curCacheSize, curCachedBorads;

    private static final Format digitFormat = new java.text.DecimalFormat("00");
    private static final Format numberFormat = new DecimalFormat();
    private JLabel curMoveLabel;
    private JLabel curValueLabel;
    private JLabel curDepthLabel;
    private JLabel curNodesLabel;
    private JLabel curSpeedLabel;
    private JLabel curTimeLabel;
    private JLabel curBoardsLabel;
    private JLabel curNonQuietLabel;
    private JLabel curCacheMissesLabel;
    private JLabel curCacheHitsLabel;
    private JLabel curTotalRamLabel;
    private JLabel curFreeRamLabel;
    private JPanel _infoPanel1;
    private JPanel _infoPanel2;
    private JPanel _infoPanel3;


    /**
     * Contructor
     */
    public EngineInfoPanel(ReversiColor engineColor) {
        super();
        if (!engineColor.isBlack() && !engineColor.isWhite()) {
            throw new IllegalArgumentException(
                    "Parameter engineColor must be either ReversiColor.BLACK or ReversiColor.WHITE. Was " + engineColor);
        }
        _model = Reversi.getPlayroom();
        _engineColor=engineColor;
        this.buildPanel();
    }

    private void buildPanel() {
        setName("Engine Info Panel");

        // -- 3 panels --
        _infoPanel1 = new JPanel(new GridBagLayout());
        _infoPanel1.setOpaque(false);
        _infoPanel2 = new JPanel(new GridBagLayout());
        _infoPanel2.setOpaque(false);
        _infoPanel3 = new JPanel(new GridBagLayout());
        _infoPanel3.setOpaque(false);

        // -- build the panels --
        setupLabels();
        layoutPanel();

        // -- layout the 3 panels in columns --
        setLayout(new GridLayout(0, 3, 0, 0));
        add(_infoPanel1);
        add(_infoPanel2);
        add(_infoPanel3);

        updateGUI();
        startUpdate();

    }

    private void layoutPanel() {
        // -- column 1 --
        GridBagHelper.constrain(_infoPanel1, curMoveLabel, 0, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel1, curMove, 1, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel1, curNodesLabel, 0, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel1, curNodes, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel1, curSpeedLabel, 0, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel1, curSpeed, 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel1, curTimeLabel, 0, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel1, curTime, 1, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        JPanel seperator1 = new JPanel();
        seperator1.setOpaque(false);
        seperator1.setBorder(new EtchedBorder());

        // -- column 2 --
        GridBagHelper.constrain(_infoPanel2, curDepthLabel, 0, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel2, curDepth, 1, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel2, curBoardsLabel, 0, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel2, curBoards, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel2, curCacheMissesLabel, 0, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel2, curCacheMisses, 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel2, curTotalRamLabel, 0, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel2, curCacheSize, 1, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        JPanel seperator2 = new JPanel();
        seperator2.setOpaque(false);
        seperator2.setBorder(new EtchedBorder());

        // -- column 3 --
        GridBagHelper.constrain(_infoPanel3, curValueLabel, 0, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel3, curValue, 1, 0, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel3, curNonQuietLabel, 0, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel3, curNonQuiet, 1, 1, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel3, curCacheHitsLabel, 0, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel3, curCacheHits, 1, 2, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        GridBagHelper.constrain(_infoPanel3, curFreeRamLabel, 0, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 0.0, 0.0, 0, 0, 4, 4);
        GridBagHelper.constrain(_infoPanel3, curCachedBorads, 1, 3, 1, 1, GridBagConstraints.NONE, GridBagConstraints.WEST, 1.0, 0.0, 0, 0, 4, 4);

        JPanel seperator3 = new JPanel();
        seperator3.setOpaque(false);
        seperator3.setBorder(new EtchedBorder());
    }

    private void setupLabels() {
        // curMove
        curMoveLabel = new JLabel("Move:");
        curMove = new JLabel();
        // curValue
        curValueLabel = new JLabel("Value:");
        curValue = new JLabel();
        // curDepth
        curDepthLabel = new JLabel("Depth:");
        curDepth = new JLabel();
        // curDepth
        curNodesLabel = new JLabel("Nodes:");
        curNodes = new JLabel();
        // curDepth
        curSpeedLabel = new JLabel("Speed:");
        curSpeed = new JLabel();
        // curDepth
        curTimeLabel = new JLabel("Time:");
        curTime = new JLabel();
        // curBoards
        curBoardsLabel = new JLabel("Boards:");
        curBoards = new JLabel();
        // curNonQuiet
        curNonQuietLabel = new JLabel("NonQuiet:");
        curNonQuiet = new JLabel();
        // curCacheMisses
        curCacheMissesLabel = new JLabel("Cache Misses:");
        curCacheMisses = new JLabel();
        // curCacheHits
        curCacheHitsLabel = new JLabel("Cache Hits:");
        curCacheHits = new JLabel();
        // curTotalRAM
        curTotalRamLabel = new JLabel("Cache Use:");
        curCacheSize = new JLabel();
        // curUsedRAM
        curFreeRamLabel = new JLabel("Cache Size:");
        curCachedBorads = new JLabel();
    }

    /**
     * clears all including the player name
     */
    private void clearAll() {
        setBorder(new TitledBorder(new EtchedBorder(), " "));
        clear();
    }

    /**
     * clears the info panel but leaves the player name
     */
    private void clear() {
        curNodes.setText("");
        curBoards.setText("");
        curNonQuiet.setText("");
        curCacheHits.setText("");
        curCacheMisses.setText("");
        curSpeed.setText("");
        curCacheSize.setText("");
        curCachedBorads.setText("");
        curDepth.setText("");
        clearWait();
    }

    /**
     * clears the info panel while the other player's turn
     */
    private void clearWait() {
        curMove.setText("");
        curValue.setText("");
        curTime.setText("");
    }

    private void updateGUI() {
        Game game = _model.getCurrentGame();

        if (game != null) {
            if (game.isRunning()) {

                if (_engineColor.isBlack()) {
                    this.setBorder(new TitledBorder(new EtchedBorder(), ("Player: "+game.getPlayerBlack().getName())));
                    // -- only update the details when it is a COMPUTER player with an engine --
                    if (game.getPlayerBlack() instanceof ComputerPlayer) {
                        if (game.getPlayerBlack().isWaiting()) {
                            clearWait();
                            this.setBorder(
                                    new TitledBorder(
                                        new EtchedBorder(),
                                        ("Player: "+game.getPlayerBlack().getName()+" <WAITING>")));

                        } else {
                            updateGUI(game, game.getPlayerBlack());
                        }
                    } else {
                        clear();
                        this.setBorder(
                                new TitledBorder(
                                    new EtchedBorder(),
                                    ("Player: "+game.getPlayerBlack().getName()+" <no info available>")));
                    }
                } else if (_engineColor.isWhite()) {
                    this.setBorder(new TitledBorder(new EtchedBorder(), ("Player: "+game.getPlayerWhite().getName())));
                    // -- only update the details when it is a COMPUTER player with an engine --
                    if (game.getPlayerWhite() instanceof ComputerPlayer) {
                        if (game.getPlayerWhite().isWaiting()) {
                            clearWait();
                            this.setBorder(
                                    new TitledBorder(
                                        new EtchedBorder(),
                                        ("Player: "+game.getPlayerWhite().getName()+" <WAITING>")));

                        } else {
                            updateGUI(game, game.getPlayerWhite());
                        }
                    } else {
                        clear();
                        this.setBorder(
                                new TitledBorder(
                                        new EtchedBorder(),
                                        ("Player: "+game.getPlayerWhite().getName()+" <no info available>")));
                    }
                }
            }
        } else {
            clearAll();
        }
    }

    private void updateGUI(Game game, Player player) {

        // -- we can only watch the engine when the Interface "TreeSearchEngineWatcher" is implemented --
        if (((ComputerPlayer)player).getEngine() instanceof TreeSearchEngineWatcher) {

            TreeSearchEngineWatcher engine = (TreeSearchEngineWatcher)((ComputerPlayer)player).getEngine();

            // -- current move in calculation --
            if (engine.getCurMove() != null) {
                engineShowCurMove(
                    game.getCurBoard().getNextMoveNumber(),
                    engine.getCurMove(),
                    engine.getCurMoveNumber(),
                    engine.numberOfPossibleMoves()
                );
            }

            // -- current calculated value for the best move so far --
            if (engine.getMaxValueMove() != null) {
                engineShowCurValue(
                        engine.getMaxValueMove()
                );
            }

            // -- current search depth --
            curDepth.setText(engine.getCurSearchDepth()+"/"+engine.getCurExtraSearchDepth());

            // -- current number of checked nodes --
            curNodes.setText(numberFormat.format(engine.getNodesChecked()) + " N");

            // -- current number of nodes per second --
            curSpeed.setText(numberFormat.format(engine.getCurNodesPerSecond()) + " N/s");

            // -- current time used for the move --
            engineShowCurTime(engine.getCurUsedTime());

            // -- show the number of boards analysed so far --
            this.curBoards.setText(numberFormat.format(engine.getBoardsChecked()) + " B");

            // -- show the number of non-quiet boards found so far --
            this.curNonQuiet.setText(numberFormat.format(engine.getBoardsNonQuiet()) + " NB");

            // -- show the number of cache hits ans misses so far --
            engineShowCurCacheStats(engine.getCacheHits(), engine.getCacheMisses());

            // -- show the current capacity of the board cache --
            curCacheSize.setText(numberFormat.format(engine.getCurCacheSize()));

            // -- show the numer of boards in the cache --
            curCachedBorads.setText(numberFormat.format(engine.getCurCachedBoards()));


        } else {
            clear();
            this.setBorder(
                    new TitledBorder(
                            new EtchedBorder(),
                            ("Engine info: "+player.getName()+" <no info available>")));
        }

    }

    /**
     * shows the move the engine is currently working on
     * @param move
     */
    private void engineShowCurMove(int nextMoveNumber, Move move, int moveNumber, int numberOfMoves) {
        String text = digitFormat.format(nextMoveNumber) + ". ";
        if (move.getColor().isBlack()) {
            text += "b";
        } else if (move.getColor().isWhite()) {
            text += "w";
        }
        this.curMove.setText(
                text
                + '(' + move.getCol() + ',' + move.getRow() + ") "
                + moveNumber + '/' + numberOfMoves
        );
    }

    /**
     * shows the highest value and the move so far
     * @param move
     */
    private void engineShowCurValue(Move move) {
        String text = "";
        int value = move.getValue();
        if (value > 0) {
            text += "+";
        }
        if (value == Integer.MAX_VALUE) {
            text += "max ";
        } else if (value == -Integer.MAX_VALUE) {
            text += "min ";
        } else {
            text += value + "  ";
        }
        if (move.getColor().isBlack()) {
            text += "b";
        } else if (move.getColor().isWhite()) {
            text += "w";
        }
        this.curValue.setText(
                        text
                        + '(' + move.getCol()
                        + ',' + move.getRow() + ')'
        );
    }

    /**
     * shows the time elapsed so far
     */
    private void engineShowCurTime(long time) {
        String hour   = digitFormat.format((int) (time / 1000L / 60L / 60L));
        String minute = digitFormat.format((int) (time / 1000L / 60L) % 60);
        String second = digitFormat.format((int) (time / 1000L) % 60);
        String milli  = digitFormat.format((int) time % 100);
        curTime.setText(hour + ':' + minute + ':' + second + ':' + milli);
    }

    /**
     * shows the number of current boards evaluated so far
     * @param cachehits
     */
    private void engineShowCurCacheStats(int cachehits, int cachemisses) {
        int percent = (int) (100.0F * ((float) cachehits / (float) (cachehits + cachemisses)));
        this.curCacheMisses.setText(numberFormat.format(cachemisses));
        this.curCacheHits.setText(numberFormat.format(cachehits) + " (" + percent + "%)");
    }

    /**
     * starts the background thread to update the panel regularly
     */
    private void startUpdate() {
        updater.start();
    }

    private class updateThread extends Thread {
        private updateThread() {
            super("EngineInfoUpdater");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }
        @Override
		public void run() {
            updateRunnable aUpdateRunnable = new updateRunnable();
            while (true) {
                SwingUtilities.invokeLater(aUpdateRunnable);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // -- ignore --
                }
                if (_abort) {
                    return;
                }

            }
        }

        private class updateRunnable implements Runnable {
            public void run() {
                updateGUI();
            }
        }
    }
}
