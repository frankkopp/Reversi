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

/**
 * (c) 2003, Frank Kopp (frank.kopp@coremedia.de)
 *
 * Package: fko.reversi.util
 * Date: 27.01.2004
 * Time: 16:22:05
 *
 */
package fko.reversi.util;

import fko.reversi.Reversi;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A logger for Reversi. It uses a Logger for actually handling the logging.
 * Get the Logger through getLogger() and use as documented in there.
 * @see java.util.logging.Logger
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class ReversiLogger {

    private final static Logger _log = Logger.getLogger("fko.reversi");

    private final static ReversiLogger _instance = new ReversiLogger();
    
    /**
     * ReversiLogger is a Singleton so use getInstance()
     * @return ReversiLogger instance
     */
    public static ReversiLogger getInstance() {
        return _instance;
    }

    private ReversiLogger() {
        // get global LOG filename from properties
        String logfileFileName = Reversi.getProperties().getProperty("log.global");

        // -- logging to file found in properties --
        if (logfileFileName != null) {
            logfileFileName = System.getProperty("user.dir") + logfileFileName;
            _log.info("Start Logging to " + logfileFileName);
            // create FileOutputHandler
            try {
                FileHandler fileHandle = new FileHandler(logfileFileName);
                _log.addHandler(fileHandle);
                _log.getHandlers()[0].setFormatter(new SimpleFormatter());
                _log.setUseParentHandlers(false);
            } catch (IOException e) {
                _log.warning("Couldn't open " + logfileFileName + " for logging!");
                _log.warning("Using default (stdout)");
            }
            // -- no LOG file configured in properties, using default --
        } else {
            _log.getParent().getHandlers()[0].setFormatter(new SimpleFormatter());
            _log.info("Start logging to console");
        }

        _log.info("Reversi started");
    }

    /**
     * Returns the Logger object used by this class to acually do the logging.
     * @see java.util.logging.Logger
     * @return The Logger object.
     */
    public static Logger getLogger() {
        return _log;
    }

}
