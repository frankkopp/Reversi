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
 */

package fko.reversi;

import fko.reversi.ui.UserInterface;
import fko.reversi.ui.UserInterfaceFactory;
import fko.reversi.util.ReversiLogger;
import fko.reversi.util.ReversiProperties;
import fko.reversi.util.CmdLineParser;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * <p>The Reversi class is the main class handling the startup
 * and processing the parameters. It also holds all the
 * global variables. It can not be instantiated.</p>
 *
 * <h2>Program Structure</h2>
 *
 * <ul>
 * <li><b>Reversi</b><br/>
 * The Reversi class is used to start up the application using a <i>Playroom</i> and a <i>UI</i> (user interface).
 * It also processes command line arguments. It gives access to the <i>ReversiProperties</i> which has
 * configuration information about the application. The Reversi main logging class <i>ReversiLogger</i> can
 * also be accessed through this class.<br/><br/>
 * </li>
 *      <ul>
 *      <li><b>Playroom</b><br/>
 *      The Playroom class is the main controlling part of the application. It represents and gives access
 *      to the model independently from the user interface (Model View Controller).<br/>
 *      The Playroom is started through the user interface (UI) or the RemotePlayerServer and configured
 *      through properties and/or the user interface or the RemotePlayerServer. It is responsible to build
 *      and start players and games and also to give access to all model information which has to be queried
 *      and changed by the user interface.<br/>
 *      The Playroom is started by the user interface and is running in a separate thread to actually
 *      play one or more games in a row. For doing this it creates a game with the current settings and also
 *      creates two players to play in that game. When players and game are initialized it starts the game
 *      the game which is also running in a separate thread.<br/>
 *      The Playroom class also handles the RemotePlayerServer to allow remote user to connect and start
 *      a game over the network.<br/><br/>
 *      </li>
 *          <ul>
 *          <li><b>Game</b><br/>
 *          Description<br/><br/>
 *          </li>
 *              <ul>
 *              <li><b>Player</b><br/>
 *              Description<br/><br/>
 *              </li>
 *              Description<br/><br/>
 *              <li><b>Board</b><br/>
 *              Description<br/><br/>
 *              </li>
 *              <li><b>Clock</b><br/>
 *              Description<br/><br/>
 *              </li><br/>
 *              </ul>
 *          <li><b>RemotePlayerServer</b><br/>
 *          Description<br/><br/>
 *          </li>
 *          </ul>
 *      <li><b>UI</b><br/>
 *      Description
 *      </li>
 *      <li><b>Properties</b><br/>
 *      Description
 *      </li>
 *      <li><b>ReversiLogger</b><br/>
 *      Description
 *      </li>
 *      </ul>
 * </ul>
 *
 * <p>
 * <h2>ToDo List</h2>
 * DONE: About Copyright and GPL text<br/>
 * DONE: Board toString() for Hash and Debugging<br/>
 * DONE: MVC-Pattern clean up<br/>
 * DONE: Refactor Board/BoardImpl - get rid of switchPlayerColor() -> should be done after in makeMove() <br/>
 * DONE:      switchPlayer eliminated...but new Engine code (MTD(f)) must be tested and debugged<br/>
 * DONE: Introduce program parameter - especially -start<br/>
 * DONE: Add TimeKeeper to TreeSearch Engine to avoid out-of-time situations<br/>
 * DONE: Clock Optimizing<br/>
 * DONE: Refactor Board/BoardImpl - Inheritance of BoardImpl should be double checked<br/>
 * TODO: Remote User (completing)<br/>
 * TODO: Introduce resource bundles - multi language<br/>
 * TODO: Undo Move function<br/>
 * TODO: Force Move - break thinking<br/>
 * TODO: Select Engine via menu<br/>
 * TODO: Refactor SwingGUI - See book Java Swing - use Actions, etc.<br/>
 * TODO: Opening Book<br/>
 * TODO: End Game Solver<br/>
 * TODO: Genetic Training<br/>
 * TODO: Help text for usage and information<br/>
 * TODO: More and better Javadoc documentation ;-)<br/>
 * </p>
 * Known Issues:
 *      * RemotePlayer and RemotePlayerServer are not working.
 *      *
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class Reversi {

    /**
     * This constant holds the current version of Reversi by Frank Kopp
     */
    public static final String VERSION = "1.5 alpha 5";

	private static Reversi _myReversi = null;

    private final Playroom _playroom;
    private final UserInterface _ui;

    // -- debug --
    private static boolean _debug = Boolean.valueOf(getProperties().getProperty("debug", "false"));

    /**
     * The main() method parses the command line arguments and processes them and finally creates an instance
     * of the Reversi class.<br/>
     * @param args command line options
     */
    public static void main(final String[] args) {

        CmdLineParser cp = new CmdLineParser();
        CmdLineParser.Option debug    = cp.addBooleanOption('d', "debug"  );
        CmdLineParser.Option start    = cp.addBooleanOption('s', "start"  );
        CmdLineParser.Option cache    = cp.addBooleanOption('c', "cache"  );
        CmdLineParser.Option nocache  = cp.addBooleanOption("nocache");
        CmdLineParser.Option server   = cp.addBooleanOption('r', "server");
        CmdLineParser.Option noserver = cp.addBooleanOption("noserver");
        CmdLineParser.Option usage    = cp.addBooleanOption('?', "help"   );

        // Parse cmd line args
        try { cp.parse(args); }
        catch ( CmdLineParser.OptionException e ) {
            System.err.println(e.getMessage());
            printUsage();
            exitReversi(2);
        }

        // Usage
        if ((Boolean) cp.getOptionValue(usage)) {
            printUsage();
            exitReversi(0);
        }

        // Set properties according to the command line options
        if ((Boolean) cp.getOptionValue(debug)) { changeProperty("debug", "true"); _debug = true;  }
        if ((Boolean) cp.getOptionValue(start)) { changeProperty("start", "true");}
        if ((Boolean) cp.getOptionValue(cache)) { changeProperty("engine.cacheEnabled", "true");  }
        if ((Boolean) cp.getOptionValue(nocache)) { changeProperty("engine.cacheEnabled", "false");  }
        if ((Boolean) cp.getOptionValue(server)) { changeProperty("remotePlayerServer.enabled", "true" );  }
        if ((Boolean) cp.getOptionValue(noserver)) { changeProperty("remotePlayerServer.enabled", "false");  }

        // Now create our singleton instance of Reversi
        _myReversi = new Reversi();

    }

    /**
     * Singleton instance so this constructor is private.
     * This gets the Playroom instance, creates the gui and adds it to the Playroom instance as an Observer (MVC).
     */
    private Reversi() {
        // Create and get an instance of the singleton Playroom class
        _playroom = Playroom.getInstance();
        // Create and get an instance of an interface for Reversi.
        _ui = UserInterfaceFactory.getUI();
        // The user interface (View) is an Observer to the Playroom (Model)
        _playroom.addObserver(_ui);

        // Start game automatically?
        if (Boolean.valueOf(getProperties().getProperty("start"))) {
            _playroom.startPlayroom();
        }
    }

    /**
     * Returns ReversiProperties instance
     * @return ReversiProperties instance
     */
    public static Properties getProperties() {
        return ReversiProperties.getInstance();
    }

    /**
     * Returns ReversiLogger instance
     * @return ReversiLogger instance
     */
    public static Logger getLogger() {
        return ReversiLogger.getLogger();
    }

    /**
     * Returns Playroom instance
     * @return Playroom instance
     */
    public static Playroom getPlayroom() {
        return Playroom.getInstance();
    }

    /**
     * Called when there is an unexpected unrecoverable error.<br/>
     * Prints a stack trace together with a provided message.<br/>
     * Terminates with <tt>exit(1)</tt>.
     * @param message to be displayed with the exception message
     */
    public static void fatalError(String message) {
        Exception e = new Exception(message);
        e.printStackTrace();
        exitReversi(1);
    }

    /**
     * Called when there is an unexpected but recoverable error.<br/>
     * Prints a stack trace together with a provided message.<br/>
     * @param message to be displayed with the exception message
     */
    public static void criticalError(String message) {
        Exception e = new Exception(message);
        e.printStackTrace();
    }

    /**
     * Clean up and exit the application
     */
    public static void exitReversi() {
        exitReversi(0);
    }

    /**
     * Clean up and exit the application
     */
    private static void exitReversi(int returnCode) {
        // nothing to clean up yet
        System.exit(returnCode);
    }

    /**
     * Returns if the application is in debug mode. This can be set in the Reversi properties.
     * @return true when we are in debug mode
     */
    public static boolean isDebug() {
        return _debug;
    }

    /**
     * Changes a property due to a command line option.
     * @param name
     * @param value
     */
    private static void changeProperty(String name, String value) {
        getProperties().setProperty(name, value);
        System.out.println("Startup option: "+name + '=' +value);
    }

    /**
     * Usage message.
     */
    private static void printUsage() {
        System.out.println();
        System.out.println(
            "Usage: OptionTest [-d,--debug] [-s,--start] [-c,--cache] [--nocache]\n"
           +"                  [-r,--server] [--noserver] [-?, --help]"
        );
        System.out.println("Options:");
        System.out.println();
        System.out.println("-d debug mode");
        System.out.println("-s start game immediately with default settings");
        System.out.println("-c enables the cache for the engines");
        System.out.println("--nocache disables the cache for the engines (overrides -c)");
        System.out.println("-r enables the server for remote players");
        System.out.println("--noserver disables the server for remote players (overrides -r)");
        System.out.println();
    }

}
