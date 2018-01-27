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

package fko.reversi.remote.client;

import fko.reversi.Reversi;
import fko.reversi.player.Player;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

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
 * A logger for RemotePlayerClient.
 *
  */
public class RemotePlayerClientLogger {

    private Logger _logger;
    private String _logfileFileName;

    public RemotePlayerClientLogger(Player player) {

        String colorString = player.getColor().toString().toLowerCase();

        _logger = Logger.getLogger("fko.reversi.remote.client."+colorString);

        // get global LOG filename fromproperties
        _logfileFileName = Reversi.getProperties().getProperty("remotePlayerClient."+colorString+".log");

        // -- logging to file found in properties --
        if (_logfileFileName != null) {
            _logfileFileName = System.getProperty("user.dir") + _logfileFileName;
            _logger.info("Start Logging to " + _logfileFileName);
            // create FileOutputHandler
            try {
                FileHandler fileHandle = new FileHandler(_logfileFileName);
                _logger.addHandler(fileHandle);
                _logger.getHandlers()[0].setFormatter(new SimpleFormatter());
                _logger.setUseParentHandlers(false);
            } catch (IOException e) {
                _logger.warning("Couldn't open " + _logfileFileName + " for logging!");
                _logger.warning("Using default (stdout)");
            }
            // -- no LOG file configured in properties, using default --
        } else {
            _logger.getParent().getHandlers()[0].setFormatter(new SimpleFormatter());
            _logger.info("Start logging to console");
        }

        _logger.info("Reversi started");
    }

    public Logger getLogger() {
        return _logger;
    }

    @Override
	public String toString() {
        return "Class ReversiLogger: "+_logfileFileName;
    }
}
