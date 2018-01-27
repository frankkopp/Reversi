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
import fko.reversi.mvc.ModelEvents.ModelEvent;
import fko.reversi.mvc.ModelObservable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * <p>A server to accept remote play requests.</p>
 * <p>This class runs its own thread by implementing the Runnable interface.</p>
 */
public class RemotePlayerServer extends ModelObservable implements Runnable {

    // The servers thread
    private Thread _serverThread = null;

    // The port the server accepts connection requests
    // This is set in the constructor
    private final int _port;

    // We only want to allow one connection but we want to send a proper msg if we are busy.
    private volatile int _numberOfConnections = 0;
    private final static int _MAX_NUMBER_OF_CONNECTIONS = 1;
    // A lock to synchronise the access to numberOfConnections.
    private final Object _numberOfConnectionsLock = new Object();

    // Remember the one remote client we allow.
    private SocketChannel _remoteSocketChannel = null;

    // Remember the one conversation with a remote client we allow.
    private volatile RemotePlayerServerConnection _remoteConnection = null;

    /**
     * Creates a remote player server listening on the specified port
     * @param port the remote player server listens
     */
    public RemotePlayerServer(int port) {
        _port = port;
    }

    /**
     * Starts listening for incoming requests in seperate thread
     */
    public synchronized void startService() {
        if (_serverThread == null) {
            log("Start Server request.");
            _serverThread = new Thread(this, "RemotePlayerServer");
            _serverThread.setPriority(Thread.MIN_PRIORITY);
            _serverThread.start();
        } else {
            Reversi.criticalError("Start RemotePlayerServer requested while remote server thread is running.");
        }
    }

    /**
     * Stops listening for incoming requests and shuts down the server.
     */
    public synchronized void stopService() {
        if (_serverThread != null) {
            log("Stop Server request.");
            _serverThread.interrupt();
        } else {
            Reversi.criticalError("Stop RemotePLayerServer requested although remote server thread is not running.");
        }
    }

    /**
     * The RemotePlayerServer runs in a seperate thread und run() implements the Runnable interface.<br/>
     * In this method we create a ServerSocket to accept any incoming connections. If we already have one connection
     * we refuse further requests gracefully by sending a RemoteProtocol.MSG_REFUSED_BUSY message.<br/>
     * Onces we have a incoming connection (socket) we hand it over to the class RemotePlayerServerConnection
     * to actually process the communication with the remote client in a seperate thread.<br/>
     * This method uses a loop to continue to listen for incoming requests but refuse them.<br/>
     * If the server is stopped (stopService) the thread is interrupted and the Input- and OutputStreams are
     * closed so that the RemotePlayerServerConnection gets a SocketException while reading from the InputStream.
     * The RemotePlayerServerConnection then sends a message to the remote client and drops out of the thread's
     * run() method and stops.
     * Then we close the ServerSocket.<br/>
     */
    public void run() {

        log("Server started");

        Selector _selector;
        ServerSocketChannel _server;

        try {

            // Create a selector for ServerSockerChannel
            _selector = Selector.open();
            // Create new ServerSocketChannel
            _server = ServerSocketChannel.open();
            // Bind the socket to our current port
            _server.socket().bind(new InetSocketAddress(_port));
            // Put the ServerSocketChannel into nonblocking mode
            _server.configureBlocking(false);
            // Now register it with the Selector
            _server.register(_selector, SelectionKey.OP_ACCEPT);

        } catch (IOException e) {
            String msg = "IO Exeption while trying to create the ServerSocket";
            log(Level.SEVERE, msg);
            Reversi.criticalError(msg+": "+e.getMessage());
            Reversi.getPlayroom().serverCallback(false);
            _serverThread=null;
            return;
        }

        // If we are here the ServerSocket is created successfully
        log("Socket created - Port " + _port);
        Reversi.getPlayroom().serverCallback(true);

        _numberOfConnections = 0;
        _remoteSocketChannel = null;
        _remoteConnection = null;
        log("Waiting for remote connection");
        while (!_serverThread.isInterrupted()) {
            try { // SocketTimeoutException, IOException

                // Listen for incoming requests.
                // If we already have a connection then refuse the incoming new one.
                // If not start communication in a seperate thread.

                // Select channels which have an accept request.
                // Blocks until a channel has an accept request or the timeout is over
                if (_selector.select(250)==0) {
                    // we have a timeout
                    continue;
                }

                // After the timeout we check if we have been interrupted (server shutdown)
                if (_serverThread.isInterrupted()) {
                    log("Server thread interrupted.");
                    break;
                }

                // Get a Set with all SelectionKeys for channels which or ready for I/O
                // Should only be one as we only have one :)
                Set keys = _selector.selectedKeys();

                // Check if it really is exactly 1
                if (keys.size() != 1) {
                    Reversi.criticalError("We should exactly have 1 ServerSocketChannel ready for I/O");
                }

                // Get the SelectionKey for the channel which is ready for I/O
                Iterator i = keys.iterator();
                SelectionKey channelKey = (SelectionKey)i.next();
                i.remove(); // delete it explicitely from the Set

                // Activity on the ServerSocketChannel means a client is trying to connect to the server
                if (!channelKey.isAcceptable()) {
                    continue;
                }

                // Accept the client and optain a SocketChannel to communicate with the client
                SocketChannel newSocketChannel = _server.accept();

                log("New RemotePlayerServerConnection established: " + newSocketChannel.socket().getInetAddress());

                // Create a new conversation handler
                RemotePlayerServerConnection newConnection =
                        new RemotePlayerServerConnection(this, newSocketChannel);
                // Set logger for newConnection
                newConnection.setLogger(RemotePlayerServerLogger.getLogger());
                newConnection.setLogPrefix("RemotePlayerServerConnection");

                // If we already have a connection we refuse a new request.
                // Otherwise we accept it ans start a new conversation.
                synchronized (_numberOfConnectionsLock) {
                    if (_numberOfConnections >= _MAX_NUMBER_OF_CONNECTIONS) {
                        log("Refuse new connection: "+_numberOfConnections+">="+_MAX_NUMBER_OF_CONNECTIONS +" conenctions");
                        newConnection.refuseConversation();
                        log("New connection refused");
                        newSocketChannel.close();
                        log("New RemotePlayerServerConnection closed: " + newSocketChannel.socket().getInetAddress());
                    } else {
                        _remoteSocketChannel = newSocketChannel;
                        _remoteConnection = newConnection;
                        log("Start new connection: " + newSocketChannel.socket().getInetAddress());
                        // doConversation will start the communication in a seperate thread
                        _remoteConnection.startConnection();
                        _numberOfConnections++;
                        setChanged();
                        notifyObservers(new ModelEvent("RemotePlayerServer: New RemoteConnection"));
                    }
                }
            } catch (IOException e) {
                String msg = "IO Exeption while trying to accept connections on the ServerSocket";
                log(Level.SEVERE, msg);
                Reversi.fatalError(msg);
            }
        } // end while

        // Close the remote connection
        if (_remoteConnection!=null) {
            log("Closing remote connection.");
            // Tell the client we are ending the conversation
            _remoteConnection.stopConnection();
            // Wait until the connection has been closed
            while (_remoteConnection!=null) {
                try {
                    _remoteConnection.join();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        // Close the ServerSocket
        log("Closing server socket.");
        try {
            _server.close();
            log("Server stopped");
        } catch (IOException e) {
            Reversi.criticalError("RemotePlayerServer: IO Exeption while trying to close the ServerSocket: "+e.getMessage());
            log("IO Exeption while trying to close the ServerSocket");
        }

        // Tell the playroom we are down
        Reversi.getPlayroom().serverCallback(false);

        // remove the thread
        _serverThread = null;

    }


    /**
     * This is a callback from RemotePlayerServerConnection when he has ended the conversation. It closes the
     * socket to the remote client and allows the server to accept a new connection.
     */
    public void connectionClosed() {
        log("Connection closed. (CALLBACK)");
        _remoteConnection = null;

        // Close the socket itself
        if (_remoteSocketChannel.isConnected()) {
            log("Closing remote socket.");
            try {
                _remoteSocketChannel.close();
            } catch (IOException e) {
                Reversi.criticalError("Closing remoteClient failed:"+e.getMessage());
            }
        }
        _remoteSocketChannel =null;
        synchronized (_numberOfConnectionsLock) {
            _numberOfConnections--;
        }

        // Tell the Observers that we closed the remote connection
        setChanged();
        notifyObservers(new ModelEvent("RemotePlayerServer: Connection closed"));
        log("Remote socket closed");
    }

    /**
     * Checks if server is running
     * @return true if server is running
     */
    public boolean isRunning() {
        return _serverThread!=null && _serverThread.isAlive();
    }

    /**
     * Checks if we have an active connection
     * @return true if we have an active connection
     */
    public boolean isConnected() {
        return _remoteConnection!=null;
    }

    /**
     * Cancels a remote request within a remote connection
     */
    public void cancelRemoteGameRequest() {
        if (_remoteConnection!=null) {
            _remoteConnection.cancelRemoteGameRequest();
        }
    }

    /**
     * Returns the current number of connections. Should either be 0 or 1.
     * @return returns the current number of connections. Should be 0 or 1
     */
    public int getNumberOfConnections() {
        return _numberOfConnections;
    }

    /**
     * Returns the current port configured for this service
     * @return port number
     */
    public int get_port() {
        return _port;
    }

    /**
     * Writes to log
     * @param level - log level of the message (see Level)
     * @param msg - msg to be written to log
     */
    public static void log(Level level, String msg) {
        RemotePlayerServerLogger.getLogger().log(level, msg);
        System.out.printf(level.toString() +
                " RemotePlayerServer: (Thread: "+Thread.currentThread().getName() +"): " + msg + '\n');
    }

    /**
     * Writes to log
     * @param msg - msg to be written to log
     */
    public static void log(String msg) {
        RemotePlayerServerLogger.getLogger().log(Level.INFO, msg);
        System.out.printf("INFO: " +
                "RemotePlayerServer: (Thread: "+Thread.currentThread().getName() +"): " + msg + '\n');
    }

}