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

import fko.reversi.player.PlayerType;
import fko.reversi.game.ReversiColor;

/**
 * <p/>
 * The RemoteGameRequest class is used by the RemotePlayerServer to ask the Playroom and the user to accept or deny a
 * game request from a remote player.
 * </p>
 * <ul>
 * <li>board dimension</li>
 * <li>black time in ms</li>
 * <li>white time in ms</li>
 * <li>remote player color (so server must accept the opponents color)</li>
 * <li>remote player type</li>
 * <li>remote player name</li>
 * </ul>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class RemoteGameRequest {

    //  -- Fields --
    private int               _boardDimension;
    private boolean           _isTimedGame;
    private long              _blackTime;
    private long              _whiteTime;
    private ReversiColor _localPlayerColor;
    private PlayerType _localPlayerType;
    private String            _localPlayerName;

    public RemoteGameRequest() {
        this(8,false,0,0,ReversiColor.BLACK, PlayerType.HUMAN,"Black Player Default Name");
    }

    public RemoteGameRequest(
            int dim,
            boolean timedgame,
            long blacktime,
            long whitetime,
            ReversiColor localPlayerColor,
            PlayerType localPlayerType,
            String localPlayerName)
    {
        _boardDimension = dim;
        _isTimedGame = timedgame;
        _blackTime = blacktime;
        _whiteTime = whitetime;
        _localPlayerColor = localPlayerColor;
        _localPlayerType = localPlayerType;
        _localPlayerName = localPlayerName;
    }

    public int getBoardDimension() {
        return _boardDimension;
    }

    public void setBoardDimension(int boardDimension) {
        this._boardDimension = boardDimension;
    }

    public boolean isTimedGame() {
        return _isTimedGame;
    }

    public void setTimedGame(boolean isTimedGame) {
        this._isTimedGame = isTimedGame;
    }

    public long getBlackTime() {
        return _blackTime;
    }

    public void setBlackTime(long blackTime) {
        this._blackTime = blackTime;
    }

    public long getWhiteTime() {
        return _whiteTime;
    }

    public void setWhiteTime(long whiteTime) {
        this._whiteTime = whiteTime;
    }

    public ReversiColor getLocalPlayerColor() {
        return _localPlayerColor;
    }

    public void setRemotePlayerColor(ReversiColor remotePlayerColor) {
        this._localPlayerColor = remotePlayerColor;
    }

    public PlayerType getLocalPlayerType() {
        return _localPlayerType;
    }

    public void setRemotePlayerType(PlayerType remotePlayerType) {
        this._localPlayerType = remotePlayerType;
    }

    public String getLocalPlayerName() {
        return _localPlayerName;
    }

    public void setRemotePlayerName(String remotePlayerName) {
        this._localPlayerName = remotePlayerName;
    }

    /**
     * <li>board dimension</li>
     * <li>timed game</li>
     * <li>black time in ms</li>
     * <li>white time in ms</li>
     * <li>remote player color (so server must accept the opponents color)</li>
     * <li>remote player type</li>
     * <li>remote player name</li>
     * @return a string representation of a game request which can be used directly for the protocol
     */
    @Override
	public String toString() {
        StringBuilder sb = new StringBuilder(30);
        sb.append(_boardDimension).append(' ')
            .append((_isTimedGame ? '1' : '0')).append(' ')
            .append(_blackTime).append(' ')
            .append(_whiteTime).append(' ')
            .append(_localPlayerColor.toInt()).append(' ')
            .append(_localPlayerType).append(' ')
            .append('"').append(_localPlayerName).append('"');
        return sb.toString();
    }
}
