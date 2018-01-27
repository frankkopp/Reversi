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

package fko.reversi.mvc.ModelEvents;

import fko.reversi.player.Player;

/**
 * <p/>
 * The PlayerDependendModelEvent class represents a ModeEvent but also has a parameter
 * player which can be queried by getPlayer()
 * </p>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class PlayerDependendModelEvent extends ModelEvent {

    private final Player _thePlayer;
    public Player getPlayer() {
        return _thePlayer;
    }

    public PlayerDependendModelEvent(Player aPlayer) {
        super();
        _thePlayer=aPlayer;
    }

    public PlayerDependendModelEvent(String name, Player aPlayer) {
        super(name);
        _thePlayer=aPlayer;
    }

    public PlayerDependendModelEvent(String name, Player aPlayer, int aSignal) {
        super(name, aSignal);
        _thePlayer=aPlayer;
    }


}
