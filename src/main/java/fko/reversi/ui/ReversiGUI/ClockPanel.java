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

/**
 * Displays the player's clocks
 */
public class ClockPanel extends JPanel {

    // -- back reference to ui --
    private ReversiGUI _ui;

    // -- a thread to update the clock --
    private Thread updater = new updateThread();

    private boolean abort = false;

    // -- some components --
    private JLabel blackTime, blackInfo, whiteTime, whiteInfo;
    private Font timeFont, timeFontSmall, timeFontBold, timeFontSmallBold ,nameFont, nameFontBold;
    private JPanel blackClock, whiteClock;
    private TitledBorder blackClockBorder, whiteClockBorder;

    public ClockPanel(ReversiGUI reversiGUI) {
        super();
        setName("Clock Panel");
        // -- back reference to ui --
        this._ui = reversiGUI;
        setupPanel();
        clear();
        startUpdate();
    }

    private void setupPanel() {
        // -- two columns --
        blackClock = new JPanel(new BorderLayout());
        blackClockBorder = new TitledBorder(new EtchedBorder(), "Black");
        blackClock.setBorder(blackClockBorder);
        whiteClock = new JPanel(new BorderLayout());
        whiteClockBorder = new TitledBorder(new EtchedBorder(), "White");
        whiteClock.setBorder(whiteClockBorder);

        // -- set font --
        timeFont = new Font("Arial", Font.PLAIN, 36);
        timeFontSmall = new Font("Arial", Font.PLAIN, 16);
        timeFontBold = new Font("Arial", Font.BOLD, 36);
        timeFontSmallBold = new Font("Arial", Font.BOLD, 16);
        nameFont = new Font("Arial", Font.PLAIN, 12);
        nameFontBold = new Font("Arial", Font.BOLD, 12);

        // -- clock panel --
        blackTime = new JLabel("", SwingConstants.CENTER);
        blackTime.setFont(timeFont);
        blackClock.add(blackTime, BorderLayout.CENTER);
        whiteTime = new JLabel("", SwingConstants.CENTER);
        whiteTime.setFont(timeFont);
        whiteClock.add(whiteTime, BorderLayout.CENTER);

        // -- level info --
        blackInfo = new JLabel("", SwingConstants.CENTER);
        blackInfo.setFont(timeFontSmall);
        blackClock.add(blackInfo, BorderLayout.SOUTH);
        whiteInfo = new JLabel("", SwingConstants.CENTER);
        whiteInfo.setFont(timeFontSmall);
        whiteClock.add(whiteInfo, BorderLayout.SOUTH);

        // create gui
        setLayout(new GridLayout(0, 2, 0, 0));
        this.add(blackClock);
        this.add(whiteClock);
    }

    public void clear() {
        blackTime.setText("00:00:00");
        blackClockBorder.setTitle("Black");
        whiteTime.setText("00:00:00");
        whiteClockBorder.setTitle("White");
        blackTime.setFont(timeFont);
        blackClockBorder.setTitleFont(nameFont);
        whiteTime.setFont(timeFont);
        whiteClockBorder.setTitleFont(nameFont);
    }

    public void updateGUI() {
        if (Reversi.getPlayroom().getCurrentGame() != null) {
            if (Reversi.getPlayroom().getCurrentGame().getCurBoard().getNextPlayerColor() == fko.reversi.game.ReversiColor.BLACK) {
                blackTime.setFont(timeFontBold);
                blackInfo.setFont(timeFontSmallBold);
                blackClockBorder.setTitleFont(nameFontBold);
                whiteTime.setFont(timeFont);
                whiteInfo.setFont(timeFontSmall);
                whiteClockBorder.setTitleFont(nameFont);
            } else {
                blackTime.setFont(timeFont);
                blackInfo.setFont(timeFontSmall);
                blackClockBorder.setTitleFont(nameFont);
                whiteTime.setFont(timeFontBold);
                whiteInfo.setFont(timeFontSmallBold);
                whiteClockBorder.setTitleFont(nameFontBold);
            }
            blackClockBorder.setTitle("Black: " + Reversi.getPlayroom().getCurrentGame().getPlayerBlack().getName());
            whiteClockBorder.setTitle("White: " + Reversi.getPlayroom().getCurrentGame().getPlayerWhite().getName());
            blackClock.setBorder(blackClockBorder);
            whiteClock.setBorder(whiteClockBorder);
            whiteTime.setText(Reversi.getPlayroom().getCurrentGame().getWhiteClock().getFormattedTime());
            blackTime.setText(Reversi.getPlayroom().getCurrentGame().getBlackClock().getFormattedTime());
        }
        if (Reversi.getPlayroom().isTimedGame()) {
            blackInfo.setText(HelperTools.formatTime(Reversi.getPlayroom().getTimeBlack()));
            whiteInfo.setText(HelperTools.formatTime(Reversi.getPlayroom().getTimeWhite()));
        } else {
            PlayerType playerTypeBlack;
            PlayerType playerTypeWhite;
            if (Reversi.getPlayroom().getCurrentGame() != null) {
                playerTypeBlack=Reversi.getPlayroom().getCurrentGame().getPlayerBlack().getPlayerType();
                playerTypeWhite=Reversi.getPlayroom().getCurrentGame().getPlayerWhite().getPlayerType();
            } else {
                playerTypeBlack=Reversi.getPlayroom().getPlayerTypeBlack();
                playerTypeWhite=Reversi.getPlayroom().getPlayerTypeWhite();
            }
            if (playerTypeBlack == PlayerType.HUMAN) {
                blackInfo.setText("Human");
            } else if (playerTypeBlack == PlayerType.REMOTE) {
                    blackInfo.setText("Remote Player");
            } else if (playerTypeBlack == PlayerType.SERVER) {
                    blackInfo.setText("Server Player");
            } else {
                blackInfo.setText("Computer Level " + Reversi.getPlayroom().getCurrentEngineLevelBlack());
            }
            if (playerTypeWhite == PlayerType.HUMAN) {
                whiteInfo.setText("Human");
            } else if (playerTypeWhite == PlayerType.REMOTE) {
                whiteInfo.setText("Remote Player");
            } else if (playerTypeWhite == PlayerType.SERVER) {
                whiteInfo.setText("Server Player");
            } else {
                whiteInfo.setText("Computer Level " + Reversi.getPlayroom().getCurrentEngineLevelWhite());
            }

        }
        repaint();
    }

    public void startUpdate() {
        updater.start();
    }

    private class updateThread extends Thread {
        private updateThread() {
            super("ClockInfoUpdater");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
        }
        @Override
		public void run() {
            updateRunnable aUpdateRunnable = new updateRunnable();
            while (true) {
                if (abort) {
                    return;
                }
                SwingUtilities.invokeLater(aUpdateRunnable);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // -- ignore --
                }
            }
        }
    }

    private class updateRunnable implements Runnable {
          public void run() {
              updateGUI();
          }
      }

}
