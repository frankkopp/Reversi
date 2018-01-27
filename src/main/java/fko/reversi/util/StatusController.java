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

package fko.reversi.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/**
 * <p>This class is a helper to support classes which rely on certain states.</p>
 * <p>It defines certain methods to act on the status like wait for a certain state
 * or get and set the state.</p>
 * <p/>
 * <p>If this class shall check transitions from one state to the other it should be extended and the
 * checkTransition() method should be overwritten accordingly</p>
 * <p/>
 * <i>Example:</i><br/>
 * <code>
 * private static class PlayerStatusController extends StatusController {
 * private PlayerStatusController(int initialState, String name) {
 * super(initialState, name);
 * this.setTransitionCheck(true);
 * }
 * <p/>
 * protected synchronized boolean checkTransition(int sourceState, int targetState) {
 * if (sourceState == targetState) return true;
 * switch (sourceState) {
 * <p/>
 * // Define which states are allowed when currently in a certain state.
 * case Player.WAITING:
 * switch (targetState) {
 * case Player.THINKING:
 * return true;
 * case Player.STOPPED:
 * return true;
 * default                 :
 * return false;
 * }
 * ...
 * </code>
 * <p/>
 * <p>This class is thread safe</p>
 */
public class StatusController {

    // The current state
    private volatile int state;

    // The state which will interrupt any waiting methods
    private volatile int interruptState = -Integer.MAX_VALUE;

    // Defines if the transition from one state to the other shall be checked
    private volatile boolean transitionCheck = false;

    // A ReadWriteLock for better locking options than synchronize
    private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();
    private final Lock _readLock = _rwLock.readLock();
    private final Lock _writeLock = _rwLock.writeLock();
    private final Condition _stateChange = _writeLock.newCondition();

    /**
     * Default constructor
     * Defaults to StatusController(0,null)
     */
    public StatusController() {
        this(0);
    }

    /**
     * Constructor setting an initial status and a
     * name for this status object
     *
     * @param initialState
     */
    public StatusController(int initialState) {
        this.state = initialState;
    }

    /**
     * Returns the WriteLock of this object.
     *
     * @see ReentrantReadWriteLock
     */
    public Lock writeLock() {
        return _writeLock;
    }

    /**
     * Returns the ReadLock of this object.
     *
     * @see ReentrantReadWriteLock
     */
    public Lock readLock() {
        return _readLock;
    }

    /**
     * Getter for the current state for interupting the wait methods
     * Default is -Interger.MAX_VALUE
     *
     * @return interrupt state
     */
    public int getInterruptState() {
        try {
            _readLock.lock();
            return interruptState;
        } finally {
            _readLock.unlock();
        }
    }

    /**
     * Sets a state for interrupting the wait methods
     * Default is -Interger.MAX_VALUE
     *
     * @param newInterruptState
     */
    public void setInterruptState(int newInterruptState) {
        try {
            _writeLock.lock();
            this.interruptState = newInterruptState;
            _stateChange.signalAll();
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Getter for the actual status value of the status object
     *
     * @return state
     */
    public int getStatus() {
        try {
            _readLock.lock();
            return this.state;
        } finally {
            _readLock.unlock();
        }
    }

    /**
     * Setter for the actual status value of the status object.<br/>
     * Notifies all (this.notifyAll()) waiting objects.<br/>
     * If getTransitionCheck() is true the transition from the current state to the new state will be checked by
     * checkTransition().<br/>
     * If checkTransition() returns false an exception is thrown and a stack trace is printed to System.err.<br/>
     * If getTransitionFaultFatal() is true a fault transition will cause the application to terminate with exit(1).
     */
    public void setStatus(int newState) throws StateTransitionException {
        try {
            _writeLock.lock();
            final int oldState = this.state;
            if (transitionCheck) {
                if (!checkTransition(this.state, newState)) {
                    throw new StatusController.StateTransitionException("Transition fault! Tried to transist from " + oldState + " to " + newState);
                }
            }
            this.state = newState;
            _stateChange.signalAll();
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Test if status object is in a certain state
     *
     * @param status
     * @return true or false
     */
    public boolean inStatus(int status) {
        try {
            _readLock.lock();
            return (this.state == status);
        } finally {
            _readLock.unlock();
        }
    }

    /**
     * Waits until the status has hanged (oldState != currentState)
     */
    public void waitForStatusChange() {
        try {
            _writeLock.lock();
            final int oldState = state;
            while (oldState == state) {
                try {
                    _stateChange.await();
                } catch (InterruptedException e) {
                    if (interruptState == state) {
                        return;
                    }
                }
            }
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Calls this.wait()
     *
     * @throws InterruptedException
     */
    public void waitForStateSet() throws InterruptedException {
        try {
            _writeLock.lock();
            _stateChange.await();
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Waits for a certain state
     *
     * @param stateExpected
     */
    public void waitForState(int stateExpected) {
        try {
            _writeLock.lock();
            while (stateExpected != state && interruptState != state) {
                try {
                    _stateChange.await();
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    if (interruptState == state) //noinspection ReturnInsideFinallyBlock
                    {
                        return;
                    }
                }
            }
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * Waits in a certain state
     *
     * @param waitingState
     */
    public void waitWhileInState(int waitingState) {
        try {
            _writeLock.lock();
            while (waitingState == state) {
                try {
                    _stateChange.await();
                } catch (InterruptedException e) {
                    // ignore
                } finally {
                    if (interruptState == state) //noinspection ReturnInsideFinallyBlock
                    {
                        return;
                    }
                }
            }
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * If transition check is turned on every transition via <code>setState(int state)</code>
     * will be checked by calling the <code>checkTransition</code> method. <br>
     * The <code>checkTransition</code> method is meant to be overwritten be sublasses.
     * Depending on the transitionFaultFatal value the program exits with exit
     * code 1 or it just prints a message to System.err.
     *
     * @param bool
     */
    public void setTransitionCheck(boolean bool) {
        try {
            _writeLock.lock();
            this.transitionCheck = bool;
        } finally {
            _writeLock.unlock();
        }
    }

    /**
     * If transition check is turned on every transition via <code>setState(int state)</code>
     * will be checked by calling the <code>checkTransition</code> method. <br>
     * The <code>checkTransition</code> method is meant to be overwritten be sublasses.
     * Depending on the transitionFaultFatal value the program exits with exit
     * code 1 or it just prints a message to System.err.
     *
     * @return true or false
     */
    public boolean getTransitionCheck() {
        try {
            _readLock.lock();
            return this.transitionCheck;
        } finally {
            _readLock.unlock();
        }
    }

    /**
     * This method checks for valid transition. It returns true for a valid transition
     * and false for a illegal transition.
     * This method is meant to be overwritten be a subclass as it returns true always
     * in this implementation.
     *
     * @param sourceState
     * @param targetState
     * @return true when transition is allowed, false otherwise
     */
    protected boolean checkTransition(int sourceState, int targetState) {
        return true;
    }

    /**
     * The StateTransitionException class is thrown when a transition within the StatusController is illegal.
     */
    public static class StateTransitionException extends IllegalStateException {

		public StateTransitionException() {
            super();
        }

        public StateTransitionException(String message) {
            super(message);
        }

    }
}
