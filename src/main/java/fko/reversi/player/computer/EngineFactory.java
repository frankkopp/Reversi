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

package fko.reversi.player.computer;

import fko.reversi.Reversi;
import fko.reversi.game.ReversiColor;
import fko.reversi.player.Player;

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
 * A factory for engines.<br>
 * It looks up the reversi.properties file for the engine.<br>
 * Example:<br>
 * <code>blackEngine.class = fko.reversi.player.computer.Adam.AdamEngine<br>
 * whiteEngine.class  = fko.reversi.player.computer.TreeSearch_v10.TreeSearchEngine_v10</code>
 */
public class EngineFactory {

    // -- Factories should not be instantiated --
    private EngineFactory() {}

    public static Engine createEngine(Player player, ReversiColor color) {

        String engineClass;
        if (color.isBlack()) {
            engineClass = Reversi.getProperties().getProperty("blackEngine.class");
        } else if (color.isWhite()) {
            engineClass = Reversi.getProperties().getProperty("whiteEngine.class");
        } else {
            throw new IllegalArgumentException("Not a valid ReversiColor for a player: "+color);
        }
        
        if (engineClass == null) {
            engineClass = "fko.reversi.player.computer.Adam.AdamEngine";
            System.err.println("Engine class property could not be found: using default: " + engineClass);
        }

        Engine engine = null;
        try {
            engine = (Engine) ClassLoader.getSystemClassLoader().loadClass(engineClass).newInstance();
            engine.init(player);
        } catch (InstantiationException e) {
            System.err.println("Engine class " + engine + " could not be loaded");
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } catch (IllegalAccessException e) {
            System.err.println("Engine class " + engine + " could not be loaded");
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } catch (ClassNotFoundException e) {
            System.err.println("Engine class " + engine + " could not be loaded");
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }

        return engine;
    }

}
