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

package fko.reversi.remote;

import fko.reversi.game.*;
import fko.reversi.player.Player;
import fko.reversi.player.PlayerType;

/**
 * This class defines methods to enapsulate the commands for the protocol of the RemotePlayer connection.
 */
public class RemoteProtocol {

    // Greeting
    private static final String MSG_GREETING = "HELLO";

    // Pre Game
    private static final String MSG_GAME_REQUEST = "GAME_REQUEST";
    private static final String MSG_REFUSED_BUSY = "REFUSED_BUSY";
    private static final String MSG_REFUSED_DENIED = "REFUSED_DENIED";
    private static final String MSG_NEW_GAME = "NEW_GAME";
    private static final String MSG_NEW_GAME_ACCEPTED = "NEW_GAME_ACCEPTED";
    private static final String MSG_START_GAME = "START_GAME";
    private static final String MSG_START_GAME_CONFIRM = "START_GAME_CONFIRM";

    // Game
    private static final String MSG_MOVE = "MOVE";
    private static final String MSG_MOVE_CONFIRM = "MOVE_CONFIRM";
    private static final String MSG_GET_MOVE = "GET_MOVE";
    private static final String MSG_ILLEGAL_MOVE = "ILLEGAL_MOVE";
    private static final String MSG_PASS = "PASS";
    private static final String MSG_GAME_OVER = "GAME_OVER";
    private static final String MSG_GAME_OVER_CONFIRM = "GAME_OVER_CONFIRM";

    // End
    private static final String MSG_END = "END";
    private static final String MSG_END_CONFIRM = "END_CONFIRM";

    // Error messages
    private static final String MSG_UNKNOWN_CMD = "UNKNOWN_CMD";

    // Misc
    private static final String MSG_NOOP = "NOOP";

    // Not yet used
    private static final String MSG_BOARD = "BOARD ";
    private static final String MSG_IO_ERROR = "IO_ERROR";
    private static final String MSG_INTERRUPTED = "IMTERRUPTED";
    private static final String MSG_MALFORMED_CMD = "MALFORMED_CMD";
    private static final String MSG_NOT_YET_IMPLEMENTED = "NOT_YET_IMPLEMENTED";

    /**
     * Constructor
     */
    protected RemoteProtocol() {
    }

    /**
     * Returns a greeting command for the beginning of a conversation
     *
     * @return String - greeting message
     */
    public static String getGreetingCmd() {
        return MSG_GREETING;
    }

    /**
     * Checks if s is a greeting command
     *
     * @return true when greeting command
     */
    public static boolean isGreetingCmd(String s) {
        return s.equals(MSG_GREETING);
    }

    /**
     * Send a new game request to the server.<br/>
     * This will send a game request command with the following parameters:<br/>
     * <ul>
     * <li>board dimension</li>
     * <li>timed game< (1|0)/li>
     * <li>black time in ms</li>
     * <li>white time in ms</li>
     * <li>remote player color (so server must accept the opponents color)</li>
     * <li>remote player type</li>
     * <li>remote player name</li>
     * </ul>
     *
     * @param game
     * @param localPlayer
     */
    public static String getGameRequestCmd(final Game game, final Player localPlayer) {
        return (new StringBuilder(30)
                .append(MSG_GAME_REQUEST).append(' ')
                .append(game.getCurBoard().getDim()).append(' ')
                .append(game.isTimedGame() ? '1' : '0').append(' ')
                .append(game.getBlackTime()).append(' ')
                .append(game.getWhiteTime()).append(' ')
                .append(localPlayer.getColor().toInt()).append(' ')
                .append(localPlayer.getPlayerType()).append(' ')
                .append('"').append(localPlayer.getName()).append('"')
                .toString()
        );
    }

    /**
     * Send a new game request to the server.<br/>
     * This will send a game request command with the following parameters:<br/>
     * <ul>
     * <li>board dimension</li>
     * <li>timed game< (1|0)/li>
     * <li>black time in ms</li>
     * <li>white time in ms</li>
     * <li>remote player color (so server must accept the opponents color)</li>
     * <li>remote player type</li>
     * <li>remote player name</li>
     * </ul>
     *
     * @param gr
     */
    public static String getGameRequestCmd(final RemoteGameRequest gr) {
        StringBuilder sb = new StringBuilder(MSG_GAME_REQUEST);
        sb.append(' ').append(gr.toString());
        return sb.toString();
    }

    /**
     * Checks if command is a new game request.<br/>
     * A new game request must have the following:
     * (instead of newlines use space)
     * <pre>
     * MSG_NEW_GAME
     * &lt;board dimension&gt;
     * &lt;timed game (0|1)&gt;
     * &lt;black time in ms&gt;
     * &lt;white time in ms&gt;
     * &lt;local player color&gt;
     * &lt;local player type&gt;
     * "&lt;local player name&gt;"
     * </pre>
     */
    public static boolean isGameRequestCmd(String s) {
        // Is does it start with a request game cmd?
        if (!s.startsWith(MSG_GAME_REQUEST)) {
            return false;
        }
        return s.substring(MSG_GAME_REQUEST.length()).trim().matches("\\d+ [1|0] \\d+ \\d+ -?1 [A-Z]+ \"[\\S| ]*\"");
    }

    /**
     * Checks if command is a game request.<br/>
     * A game request must have the following:
     * (instead of newlines use space)
     * <pre>
     * MSG_NEW_GAME
     * &lt;board dimension&gt;
     * &lt;timed game (0|1)&gt;
     * &lt;black time in ms&gt;
     * &lt;white time in ms&gt;
     * &lt;local player color&gt;
     * &lt;local player type&gt;
     * "&lt;local player name&gt;"
     * </pre>
     */
    public static RemoteGameRequest parseGameRequest(String s) throws ProtocolException {
        if (isGameRequestCmd(s)) {
            s = s.substring(MSG_GAME_REQUEST.length()).trim();
            // s.matches("\\d+ [1|0] \\d+ \\d+ -?1 \\d \"[\\S| ]*\"");
            // by splitting the string at all occurencies of " we get the name
            String name = (s.split("\""))[1];
            // the rest is found by splitting with ' ' (Space)
            String[] p = s.split(" ");
            return new RemoteGameRequest(
                    Integer.valueOf(p[0]),
                    (Integer.valueOf(p[1]) == 1),
                    Long.valueOf(p[2]),
                    Long.valueOf(p[3]),
                    ReversiColor.valueOf(p[4]),
                    PlayerType.valueOf(p[5]),
                    name
            );
        } else {
            // If it is not a valid game request then return a standard game request
            throw new ProtocolException("Not a valid game request: " + s + " Using default game request");
        }
    }

    /**
     * Returns the command for a new game
     */
    public static String getNewGameCmd(RemoteGameRequest gr) {
        return MSG_NEW_GAME + ' '  + gr.toString();
    }

    /**
     * Checks if the received message is a new game command
     */
    public static boolean isNewGameCmd(String s) {
        // Is does it start with a request game cmd?
        if (!s.startsWith(MSG_NEW_GAME)) {
            return false;
        }
        s = s.substring(MSG_NEW_GAME.length()).trim();
        return s.matches("\\d+ [1|0] \\d+ \\d+ -?1 [A-Z]+ \"[\\S| ]*\"");
    }

    /**
     * Checks if command is a new game command.<br/>
     * A new game must have the following:
     * (instead of newlines use space)
     * <pre>
     * MSG_NEW_GAME
     * &lt;board dimension&gt;
     * &lt;timed game (0|1)&gt;
     * &lt;black time in ms&gt;
     * &lt;white time in ms&gt;
     * &lt;local player color&gt;
     * &lt;local player type&gt;
     * "&lt;local player name&gt;"
     * </pre>
     */
    public static RemoteGameRequest parseNewGameCmd(String s) throws ProtocolException {
        if (isNewGameCmd(s)) {
            s = s.substring(MSG_NEW_GAME.length()).trim();
            // s.matches("\\d+ [1|0] \\d+ \\d+ -?1 \\d \"[\\S| ]*\"");
            // by splitting the string at all occurencies of " we get the name
            String name = (s.split("\""))[1];
            // the rest is found by splitting with ' ' (Space)
            String[] p = s.split(" ");
            return new RemoteGameRequest(
                    Integer.valueOf(p[0]),
                    (Integer.valueOf(p[1]) == 1),
                    Long.valueOf(p[2]),
                    Long.valueOf(p[3]),
                    ReversiColor.valueOf(p[4]),
                    PlayerType.valueOf(p[5]),
                    name
            );
        } else {
            // If it is not a valid game request then return a standard game request
            throw new ProtocolException("Not a valid new game command: " + s + " Using default game request");
        }
    }

    /**
     * Checks if the received message was a new game accepted command
     */
    public static String getNewGameAcceptedCmd() {
        return MSG_NEW_GAME_ACCEPTED;
    }

    /**
     * Checks if the received message was a new game accepted command
     */
    public static boolean isNewGameAcceptedCmd(String s) {
        return s.equals(MSG_NEW_GAME_ACCEPTED);
    }

    /**
     * Returns the command that indicates the client that we start the game
     */
    public static String getStartGameCmd() {
        return MSG_START_GAME;
    }

    /**
     * Checks if received message is StartGame command
     */
    public static boolean isStartGameCmd(String s) {
        return s.equals(MSG_START_GAME);
    }

    /**
     * Returns the command that indicates the client received the start game command
     */
    public static String getStartGameConfirmCmd() {
        return MSG_START_GAME_CONFIRM;
    }

    /**
     * Checks if received message is StartGameConfirm
     */
    public static boolean isStartGameConfirmCmd(String s) {
        return s.equals(MSG_START_GAME_CONFIRM);
    }

    /**
     * Returns the command to end a conversation
     */
    public static String getEndCmd() {
        return MSG_END;
    }

    /**
     * Checks if the received message was a end conversation command
     *
     * @return true - when s was a end conversation command
     */
    public static boolean isEndCmd(String s) {
        return s.equals(MSG_END);
    }

    /**
     * Returns the command to confirm end conversation
     */
    public static String getEndConfirmCmd() {
        return MSG_END_CONFIRM;
    }

    /**
     * Checks is received message was an end confirmation command
     */
    public static boolean isEndConfirmCmd(String s) {
        return s.equals(MSG_END_CONFIRM);
    }

    /**
     * Checks for Null command - do nothing but a noop reply should happen
     *
     * @return null command
     */
    public static boolean isNoopCmd(String s) {
        return s.equals(MSG_NOOP);
    }

    /**
     * Returns Null command - do nothing but a noop reply should happen
     *
     * @return null command
     */
    public static String getNoopCmd() {
        return MSG_NOOP;
    }

    /**
     * Null command confirmation
     *
     * @return null command confirmation
     */
    public static String getNoopConfirmCmd() {
        return MSG_NOOP;
    }

    /**
     * Returns command to tell that the last received command was not recognized
     *
     * @return unknown command
     */
    public static boolean isUnknownCommandCmd(String s) {
        return s.equals(MSG_UNKNOWN_CMD);
    }

    /**
     * Returns command to tell that the last received command was not recognized
     *
     * @return unknown command
     */
    public static String getUnknownCommandCmd() {
        return MSG_UNKNOWN_CMD;
    }

    /**
     * Refuse connection
     *
     * @return refuse cmd
     */
    public static String getRefuseBusyCmd() {
        return MSG_REFUSED_BUSY;
    }

    /**
     * Check if command game request was refused (busy)
     */
    public static boolean isRefusedBusyCmd(String s) {
        return s.equals(MSG_REFUSED_BUSY);
    }

    /**
     * Refuse connection
     *
     * @return refuse cmd
     */
    public static String getRefuseDeniedCmd() {
        return MSG_REFUSED_DENIED;
    }

    /**
     * Check if command game request was refused (denied)
     */
    public static boolean isRefusedDeniedCmd(String s) {
        return s.equals(MSG_REFUSED_DENIED);
    }

    /**
     * Is send when the conversation is interrupted unexpectectly
     *
     * @return interrupted message
     */
    public static String getInterruptedCmd() {
        return MSG_INTERRUPTED;
    }

    /**
     * Is send when there is an io error and we had the chance to send anyway
     *
     * @return ioError message
     */
    public static String getIoErrorCmd() {
        return MSG_IO_ERROR;
    }

    /**
     * Returns command to send a move to play to the remote player
     *
     * @param move
     * @return move command
     */
    public static String getSendMoveCmd(Move move) {
        return MSG_MOVE + ' ' + move.toString();
    }

    /**
     * Returns the command to ask for a move from the client
     */
    public static String getGetMoveCmd() {
        return MSG_GET_MOVE;
    }

    /**
     * Checks if command is a get move command
     */
    public static boolean isGetMoveCmd(String s) {
        return s.equals(MSG_GET_MOVE);
    }

    /**
     * Returns command to send a pass to play to the remote player.<br/>
     * The pass command is followed by the last move on the board
     *
     * @param move
     * @return move command
     */
    public static String getPassMoveCmd(Move move) {
        return MSG_PASS + ' ' + move.toString();
    }

    public static boolean isPassCmd(String s) {
        // Is does it start with a move cmd?
        if (!s.startsWith(MSG_PASS)) {
            return false;
        }
        s = s.substring(MSG_PASS.length()).trim();
        return s.matches("-?1\\([1-9][0-9]?,[1-9][0-9]?\\)");
    }

    /**
     * Parses a server msg to return a move
     *
     * @param msgIn
     * @return Move - the move from the server message
     */
    public static Move parsePass(String msgIn) {
        msgIn = msgIn.substring(MSG_PASS.length());
        return parseMoveOrPass(msgIn);
    }

    /**
     * Checks if command is a move from the other player
     *
     * @return move command
     */
    public static boolean isMoveCmd(String s) {
        // Is does it start with a move cmd?
        if (!s.startsWith(MSG_MOVE)) {
            return false;
        }
        return isMatchMove(s);
    }

    /**
     * Returns command for move confirm command
     */
    public static String getMoveConfirmCmd() {
        return MSG_MOVE_CONFIRM;
    }

    /**
     * Checks if command is a move confirmation
     */
    public static boolean isMoveConfirmCmd(String s) {
        return s.equals(MSG_MOVE_CONFIRM);
    }

    /**
     * Returns command for game over followed by the last move on the board.
     *
     * @return game over command
     */
    public static String getGameOverCmd(ReversiColor winner) {
        if (winner.isBlack()) {
            return MSG_GAME_OVER + " 1:0";
        } else if (winner.isWhite()) {
            return MSG_GAME_OVER + " 0:1";
        } else if (winner.isNone()) {
            return MSG_GAME_OVER + " 1:1";
        } else {
            throw new RuntimeException("Invalid Reversi color for winner");
        }
    }

    /**
     * Checks if we have a game over command
     */
    public static boolean isGameOverCmd(String s) {
        return s.startsWith(MSG_GAME_OVER);
    }

    /**
     * Returns command for game over confirm command
     */
    public static String getGameOverConfirmCmd() {
        return MSG_GAME_OVER_CONFIRM;
    }

    /**
     * Checks if we have a game over command
     */
    public static boolean isGameOverConfirmCmd(String s) {
        return s.equals(MSG_GAME_OVER_CONFIRM);
    }

    /**
     * Checks is the given string is a valid string representation of a move
     *
     * @param s
     * @return true is string represents a move
     */
    private static boolean isMatchMove(String s) {
        s = s.substring(MSG_MOVE.length()).trim();
        return s.matches("-?1\\([1-9][0-9]?,[1-9][0-9]?\\)");
    }

    /**
     * Parses a server msg to return a move
     *
     * @param msgIn
     * @return Move - the move from the server message
     */
    public static Move parseMove(String msgIn) {
        msgIn = msgIn.substring(MSG_MOVE.length());
        return parseMoveOrPass(msgIn.trim());
    }

    /**
     * Does the actual parsing of the string and returns a move.
     *
     * @param msgIn
     * @return Move - A move parsed from the string
     */
    private static Move parseMoveOrPass(String msgIn) {
        ReversiColor color = ReversiColor.valueOf(msgIn.substring(0, msgIn.indexOf('(')));
        int col = Integer.valueOf(msgIn.substring(msgIn.indexOf('(') + 1, msgIn.indexOf(',')));
        int row = Integer.valueOf(msgIn.substring(msgIn.indexOf(',') + 1, msgIn.indexOf(')')));
        return new MoveImpl(col, row, color);
    }


    /**
     * Returns a message when a illegal move has been received
     * @return String - illegal move message
     */
    public static String getIllegalMoveCmd() {
        return MSG_ILLEGAL_MOVE;
    }

    /**
     * Checks if command is illegal move command
     */
    public static boolean isIllegalMoveCmd(String s) {
        return s.equals(MSG_ILLEGAL_MOVE);
    }

    /**
     * Returns a board string
     *
     * @param b
     * @return String - representing the given board
     */
    public static String getSendBoardCmd(Board b) {
        return MSG_BOARD + b.toString();
    }


    /**
     * Malformed command
     *
     * @return unknown command
     */
    public static String malformedCmd() {
        return MSG_MALFORMED_CMD;
    }

    /**
     * Returns the not yet implemented message
     */
    public static String getNotYetImplementedCmd() {
        return MSG_NOT_YET_IMPLEMENTED;
    }

}
