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

package fko.reversi.remote.server;

import fko.reversi.Reversi;

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
 * A logger for RemotePlayerServer.
 *
 * ToDo: Make the loggers more general
 *
  */
public class RemotePlayerServerLogger {

    private final static Logger _log = Logger.getLogger("fko.reversi.remote.server");

    private final static RemotePlayerServerLogger _instance = new RemotePlayerServerLogger();

    /**
     * ReversiLogger is a Singleton so use getInstance()
     * @return ReversiLogger instance
     */
    public static RemotePlayerServerLogger getInstance() {
        return RemotePlayerServerLogger._instance;
    }

    private RemotePlayerServerLogger() {
        // get global LOG filename fromproperties
        String logfileFileName = Reversi.getProperties().getProperty("remotePlayerServer.log");

        // -- logging to file found in properties --
        if (logfileFileName != null) {
            logfileFileName = System.getProperty("user.dir") + logfileFileName;
            RemotePlayerServerLogger._log.info("Start Logging to " + logfileFileName);
            // create FileOutputHandler
            try {
                FileHandler fileHandle = new FileHandler(logfileFileName);
                RemotePlayerServerLogger._log.addHandler(fileHandle);
                RemotePlayerServerLogger._log.getHandlers()[0].setFormatter(new SimpleFormatter());
                RemotePlayerServerLogger._log.setUseParentHandlers(false);
            } catch (IOException e) {
                RemotePlayerServerLogger._log.warning("Couldn't open " + logfileFileName + " for logging!");
                RemotePlayerServerLogger._log.warning("Using default (stdout)");
            }
            // -- no LOG file configured in properties, using default --
        } else {
            RemotePlayerServerLogger._log.getParent().getHandlers()[0].setFormatter(new SimpleFormatter());
            RemotePlayerServerLogger._log.info("Start logging to console");
        }

        RemotePlayerServerLogger._log.info("Reversi started");
    }

    public static Logger getLogger() {
        return RemotePlayerServerLogger._log;
    }

    @Override
	public String toString() {
        return "Class ReversiLogger";
    }
}
