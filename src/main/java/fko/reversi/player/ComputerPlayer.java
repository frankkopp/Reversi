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

package fko.reversi.player;

import fko.reversi.game.Game;
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;
import fko.reversi.player.computer.Engine;
import fko.reversi.player.computer.EngineFactory;

/**
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
 * <hr/>
 * 
 * A computer player for Reversi.
 */
public class ComputerPlayer extends AbstractPlayer {

    private final Engine _engine;

    /**
     * This constructor is protected to indicate to use the PlayerFactory to create
     * a new player of this kind
     * @param game - a back reference to the game the player plays in
     * @param name - the name of the player
     * @param color - the color the player has in the current game
     */
    protected ComputerPlayer(Game game, String name, ReversiColor color) {
        super(game, name, color);
        this._engine = EngineFactory.createEngine(this, color);
    }

    /**
     * This constructor is protected to indicate to use the PlayerFactory to create
     * a new player of this kind
     * @param name - the name of the player
     * @param color - the color the player has in the current game
     */
    protected ComputerPlayer(String name, ReversiColor color) {
        super(name, color);
        this._engine = EngineFactory.createEngine(this, color);
    }

    /**
     * Implementation of getMove() for to determine th next move
     * @return return computed move
     */
    public Move getMove() {
        // <ENGINE>
        return _engine.getNextMove(getCurBoard());
        // </ENGINE>
    }

    /**
     * return the player engine
     */
    public Engine getEngine() {
        return _engine;
    }

    /**
     * This method may be overwritten to to some extra stuff when startPlayer() is called and
     * before the actual Thread is started.
     */
    @Override
	protected void startPlayerPrepare() {
        super.startPlayerPrepare();
        _engine.setGame(getCurrentGame());
    }

    /**
     * Returns the int value of the PlayerType for a given player
     */
    public PlayerType getPlayerType() {
        return PlayerType.COMPUTER;
    }

}
