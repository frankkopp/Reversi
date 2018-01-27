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

package fko.reversi.player.computer.TreeSearch;

import fko.reversi.Reversi;
import fko.reversi.game.*;
import fko.reversi.player.Player;
import fko.reversi.player.computer.Engine;
import fko.reversi.util.LruCache;
import fko.reversi.util.ReversiLogFormatter;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import static java.lang.Integer.*;
import static java.lang.Math.ceil;

/**
 * <p/>
 * The TreeSearchEngine_MTDf_v3 class implements the MTD(f) algorithm described by Aske Plaat (see author).
 * It is based on a AlphaBetaWithMemory algorithm.
 * </p>
 *
 * @author MTD(f): Aske Plaat, Dec 3, 1997, aske@acm.org (<a href="http://www.cs.vu.nl/~aske/mtdf.html">MTD(f)</a>)
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class TreeSearchEngine_AlphaBetaWithMemory_v2 implements Engine, TreeSearchEngineWatcher {

    /**
     * A log for easier debugging
     */
    private final Logger LOG;

    /**
     * An inittial size for our cache
     */
    private static final int INITIAL_CACHE_CAPACITY = 100000;

    /**
     * The current game this engine is used in
     */
    private Game _game = null;

    /**
     * my color (Max Player)
     */
    private ReversiColor _maxColor = ReversiColor.NONE;

    /**
     * the boardAnalyser is used to analyse the board and return a value
     */
    private TreeSearchBoardAnalyser _boardAnalyser = null;

    /**
     * Used for the iterative search to determine the start and the max search depth for each iteration
     */
    private int _iterativeMaxDepth = 2;
    private int _iterativeStartDepth = 1;
    /**
     * some information about our nodes
     */
    private int _movesSize = 0;
    private int _curMoveNumber = 0;
    private Move _curMove = null;
    private Move _maxValueMove = null;
    private int _curSearchDepth = 0;
    private int _curExtraSearchDepth = 0;
    private int _boardsChecked = 0;
    private int _boardsNonQuiet = 0;
    private int _nodesFoundInCache = 0;
    private int _nodesNotFoundInCache = 0;
    private int _nodesChecked = 0;

    /**
     * time calculations
     */
    private long  _startTime = 0L;
    private float _branchingFactor = 8.0f;
    private int   _branchingSum = 8;
    private int   _branchingFactorCounter = 1;

    /**
     * cache for already analysed boards
     */
    private boolean _cacheEnabled;
    private LruCache _boardCache = null;
    private static final int VALUE_UNKNOWN = MIN_VALUE;

    /**
     * The last calculated value as a starting point for the next move
     */
    private int lastValue=0;

    // DEBUG only
    private boolean _BOUNDS_CACHE=true;

    /**
     * Contructor
     */
    public TreeSearchEngine_AlphaBetaWithMemory_v2() {
        _cacheEnabled = Boolean.valueOf(Reversi.getProperties().getProperty("engine.cacheEnabled"));
        _boardCache = new LruCache(TreeSearchEngine_AlphaBetaWithMemory_v2.INITIAL_CACHE_CAPACITY, 0.75f, true,
                parseInt(Reversi.getProperties().getProperty("engine.cacheSize")));

        LOG=Logger.getAnonymousLogger();//getLogger(Logger.GLOBAL_LOGGER_NAME);
        LOG.setUseParentHandlers(false);
        Handler h = new StreamHandler(new PrintStream(System.out,  true), new ReversiLogFormatter());
        LOG.addHandler(h);
        if (Reversi.isDebug()) {
            h.setLevel(Level.ALL);
            LOG.setLevel(Level.ALL);
        } else {
            h.setLevel(Level.OFF);
            LOG.setLevel(Level.OFF);
        }
        LOG.config("Start logging to console!");
    }

    /**
     * Inititializer - not used in this implementation
     */
    public void init(Player init_player) {
        LOG.entering(this.getClass().getName(), "init", init_player);
        _maxColor=init_player.getColor();
    }

    /**
     * Starts calculation and returns next move.<p/>
     *
     * @param curBoard
     * @return returns the "best" next move
     */
    public Move getNextMove(Board curBoard) {

        LOG.entering(this.getClass().getName(), "getNextMove", curBoard+" Move number: "+curBoard.getNextMoveNumber());

        // Initizalize timer
        _startTime = System.currentTimeMillis();

        // Create new hashable board based on the current board (deep copy)
        TreeSearchBoard board = new TreeSearchBoardImpl_v13(curBoard);

        // Set my color (MAX player)
        _maxColor = board.getNextPlayerColor();

        // Update maximal search depth so we can change the level for the engine each move
        int maxDepth = updateSearchDepth();

        // Approximate available time for this move
        long approxTime = 0;
        if (_game.isTimedGame()) {
            approxTime = approxTime(curBoard);
        }

        // Create and Initialize the BoardAnalyser if not already existing
        if (_boardAnalyser == null) {
            _boardAnalyser = new TreeSearchBoardAnalyserImpl_v13(board, _maxColor);
        }

        // Reset all the counters used for the TreeSearchEngineWatcher
        resetCounters();

        // If we do not have a timed game we immediately search with the maximum search depth
        if (!_game.isTimedGame()) {
            _iterativeStartDepth = maxDepth;
        }

        // Generate moves to check if we have more then 1 move
        List<Move> moves = board.getMoves();

        // If we have only one move return this and mark it as the only move by setting its value to Integer.MIN_VALUE.
        if (moves.size()==1) {
            moves.get(0).setValue(MIN_VALUE);
            return moves.get(0);
        }

        /********************************************************************
         * AlphaBeta search
         */

        // We need a good first guess (value) and a move in case we don't find anything better
        _boardAnalyser.sortMoves(null, moves);
        moves.get(0).setValue(lastValue);
        Move value = new MoveImpl(moves.get(0));

        // Iterative deepening for time limited game
        for (_iterativeMaxDepth = _iterativeStartDepth; _iterativeMaxDepth <= maxDepth; _iterativeMaxDepth++) {

            // Calculate how much time the iteration has needed
            final long startTimeThisIteration = System.currentTimeMillis();

            value = AlphaBetaWithMemory(board, -MAX_VALUE, MAX_VALUE, _iterativeMaxDepth, 0);

            // Check time available for another iteration
            if (_game.isTimedGame()
                    && outOfTime(_startTime, approxTime, System.currentTimeMillis()-startTimeThisIteration)) {
                break;
            }
        }

        /*
        * AlphaBeta search
        ********************************************************************/

        LOG.exiting(this.getClass().getName(), "getNextMove", value);
        LOG.exiting(this.getClass().getName(), "getNextMove", "Nodes: "+_nodesChecked+" Boards: "+_boardsChecked);
        LOG.getHandlers()[0].flush();

/**********************
 * DEBUG
 *
        Move debugValue =new MoveImpl((Move) moves.get(0));

_boardCache.clear();
_BOUNDS_CACHE=true;
st2.start();

        // Iterative deepening for time limited game
        for (_iterativeMaxDepth = _iterativeStartDepth; _iterativeMaxDepth <= maxDepth; _iterativeMaxDepth++) {

            // Calculate how much time the iteration has needed
            final long startTimeThisIteration = System.currentTimeMillis();

            debugValue = AlphaBetaWithMemory(board, -MAX_VALUE, MAX_VALUE, _iterativeMaxDepth, 0);

            // Check time available for another iteration
            if (_game.isTimedGame()
                    && !checkTime(_startTime, approxTime, System.currentTimeMillis()-startTimeThisIteration)) {
                break;
            }
        }

st2.stop();
_BOUNDS_CACHE=false;
_boardCache.clear();

        LOG.exiting(this.getClass().getName(), "getNextMove", debugValue);
        LOG.exiting(this.getClass().getName(), "getNextMove", "Nodes: "+_nodesChecked+" Boards: "+_boardsChecked);
        LOG.getHandlers()[0].flush();

System.out.println("Move Number: "+board.getNextMoveNumber()+" First(Move: "+value+" Time: "+st1.toString()+") / "
                  +"Second(Move: "+debugValue + " Time: "+st2.toString()+") ("
                  +((double)st2.getTime()/(double)st1.getTime())*100.0d +"%)");

        if (! (value.equals(debugValue) && value.getValue()==debugValue.getValue()))
            System.err.println("First("+value+") != Second("+debugValue + ')');
            //throw new RuntimeException("First("+firstguess+") != Second("+secondguess+ ')');
/*
 * DEBUG
**************************/

        // save the value as a starting point for the next move
        lastValue = value.getValue();
        return value; // firstguess;
    }

    /**
     * AlphaBeta search with caching.
     *
     * @param parentBoard - node
     * @param alpha
     * @param beta
     * @param d - depth
     * @param extra - the current additional search depth due to non-quiet boards
     */
    private Move AlphaBetaWithMemory(final TreeSearchBoard parentBoard, int alpha, int beta, int d, int extra) {

        // we don't have a current best move for this node yet
        Move currentBestMoveForNode = null;

        // Store the current statistics for TreeSearchEngineWatcher
        _nodesChecked++;
        _curSearchDepth = _iterativeMaxDepth-d;
        _curExtraSearchDepth = _iterativeMaxDepth-d+extra;

        LOG.entering(this.getClass().getName(), "AlphaBetaWithMemory", ' ' +spacer(d)+parentBoard.toString()
                + "depth: "+d+" extra: "+extra+" alpha: "+alpha+" beta: "+beta);

        // Cache lookup
        BoardValue cachedBoardValue = retrieveCache(parentBoard.getHashKey());

        // DEBUG
        Move vergleich = null;

        if (cachedBoardValue == null) { // we didn't find something in cache so we must create a new boardValueN
            cachedBoardValue = new TreeSearchEngine_AlphaBetaWithMemory_v2.BoardValue(
                    parentBoard.getHashKey(), TreeSearchEngine_AlphaBetaWithMemory_v2.VALUE_UNKNOWN, 0, MAX_VALUE, -MAX_VALUE);
            LOG.info("Not in cache: "+cachedBoardValue.toString());
        }
        else if (_BOUNDS_CACHE && d >= 0 && cachedBoardValue.getDepth() >= parentBoard.getLastMoveNumber()+d) { // can we use this due to depth?
            // we found something in cache
            LOG.info("From cache: "+cachedBoardValue.toString());
            if (cachedBoardValue.getUpperbound() <= alpha
                    || cachedBoardValue.getUpperbound() == cachedBoardValue.getLowerbound()) {
                return new MoveImpl(0,0,ReversiColor.NONE,cachedBoardValue.getUpperbound());
            }
            if (cachedBoardValue.getLowerbound() >= beta) {
                return new MoveImpl(0,0,ReversiColor.NONE,cachedBoardValue.getLowerbound());
            }
            alpha = Math.max(alpha, cachedBoardValue.getLowerbound());
            beta  = Math.min(beta, cachedBoardValue.getUpperbound());
        }

        // Check if node is leave node and if board is quiet - increase depth by 1 if non-quiet
        // DEBUG - turned off
        if (false && d==0 && !parentBoard.getNextPlayerColor().isNone() && _boardAnalyser.notQuiet(parentBoard)){
            d++; // non-quiet board -> search another level
            extra++; // just for statistics
            _boardsNonQuiet++; // just for statistics
        }

        // Processing node
        if (d == 0 || parentBoard.getNextPlayerColor().isNone()) { // n is LEAFNODE
            LOG.info(" LEAF : "+spacer(d)+parentBoard +" Move: "+currentBestMoveForNode);
            if (cachedBoardValue.getExactValue()== VALUE_UNKNOWN) { // cache miss - evaluate the board and store in cache
                currentBestMoveForNode = new MoveImpl(0,0,ReversiColor.NONE,_boardAnalyser.analyse(parentBoard));
                cachedBoardValue.setExactValue(currentBestMoveForNode.getValue());
                cachedBoardValue.setUpperbound(currentBestMoveForNode.getValue());
                cachedBoardValue.setLowerbound(currentBestMoveForNode.getValue());
                storeCache(cachedBoardValue);
                _boardsChecked++;
            } else { // cache hit and value stored
                currentBestMoveForNode = new MoveImpl(0,0,ReversiColor.NONE,cachedBoardValue.getExactValue());
            }
        }
        else if (parentBoard.getNextPlayerColor() == _maxColor) { // n is a MAXNODE
            int a = alpha; // save original alpha value
            // Generate moves for current player
            List<Move> children = genChildren(parentBoard);
            ListIterator<Move> childrenIterator = children.listIterator();
            // Set the first move as the current best move
            currentBestMoveForNode = children.get(0);
            currentBestMoveForNode.setValue(-MAX_VALUE);
            // Loop though children
            int i=0;
            while(currentBestMoveForNode.getValue() < beta && childrenIterator.hasNext()) {
                // generate next child out of the next move and a copy of our node
                Move currentMove = childrenIterator.next();
                TreeSearchBoard childBoard = TreeSearchEngine_AlphaBetaWithMemory_v2.genChild(parentBoard, currentMove);
                // Statistics
                if (d==_iterativeMaxDepth) {
                    // Store the current move for TreeSearchEngineWatcher
                    _curMove = currentMove;
                    _curMoveNumber = ++i;
                    _movesSize = children.size();
                }
                // recursive call
                LOG.info(" MAX  : "+spacer(d)+parentBoard +" Move: "+currentMove);
                Move temp = AlphaBetaWithMemory(childBoard, a, beta, d-1, extra);
                if (temp.getValue() > currentBestMoveForNode.getValue()) {
                    currentMove.setValue(temp.getValue());
                    currentBestMoveForNode=currentMove; // new best move for max player
                    if (d==_iterativeMaxDepth) {
                        _maxValueMove = currentBestMoveForNode; // just statistics
                    }
                }
                a = Math.max(a, currentBestMoveForNode.getValue());
            }
        }
        else if (parentBoard.getNextPlayerColor() == _maxColor.getInverseColor()) { // n is a MINNODE
            int b = beta; // save original beta value
            // Generate moves for current player
            List<Move> children = genChildren(parentBoard);
            ListIterator<Move> childrenIterator = children.listIterator();
            // Set the first move as the current best move
            currentBestMoveForNode = children.get(0);
            currentBestMoveForNode.setValue(MAX_VALUE);
            // Loop though children
            int i=0;
            while(currentBestMoveForNode.getValue() > alpha && childrenIterator.hasNext()) {
                // generate next child out of the next move and a copy of our node
                Move currentMove = childrenIterator.next();
                TreeSearchBoard childBoard = TreeSearchEngine_AlphaBetaWithMemory_v2.genChild(parentBoard, currentMove);
                // Statistics
                if (d==_iterativeMaxDepth) {
                    // Store the current move for TreeSearchEngineWatcher
                    _curMove = currentMove;
                    _curMoveNumber = ++i;
                    _movesSize = children.size();
                }
                // recursive call
                LOG.info(" MIN  : "+spacer(d)+parentBoard +" Move: "+currentMove);
                Move temp = AlphaBetaWithMemory(childBoard, alpha, b, d-1, extra);
                if (temp.getValue() < currentBestMoveForNode.getValue()) {
                    currentMove.setValue(temp.getValue());
                    currentBestMoveForNode=currentMove; // we have a new best move for min player
                    if (d==_iterativeMaxDepth) {
                        _maxValueMove = currentBestMoveForNode; // just statistics
                    }
                }
                b = Math.min(b, currentBestMoveForNode.getValue());
            }
        } else {
            throw new RuntimeException("No next player!");
        }

        // Cache update
        if (_BOUNDS_CACHE) {
            // Fail low result implies an upper bound
            if (currentBestMoveForNode.getValue() <= alpha) {
                cachedBoardValue.setUpperbound(currentBestMoveForNode.getValue());
            }
            // Found an accurate minimax value - will not occur if called with zero window
            if (alpha < currentBestMoveForNode.getValue() && currentBestMoveForNode.getValue() < beta) {
                cachedBoardValue.setLowerbound(currentBestMoveForNode.getValue());
                cachedBoardValue.setUpperbound(currentBestMoveForNode.getValue());
            }
            // Fail high result implies a lower bound
            if (currentBestMoveForNode.getValue() >= beta) {
                cachedBoardValue.setLowerbound(currentBestMoveForNode.getValue());
            }
            cachedBoardValue.setSearchDepth(parentBoard.getLastMoveNumber()+d);
            storeCache(cachedBoardValue);
        }

        LOG.exiting(this.getClass().getName(), "AlphaBetaWithMemory", spacer(d)+currentBestMoveForNode);

        return currentBestMoveForNode;
    }

    /**
     * Generates a list of moves from a given board and also sorts these moves using
     * _boardAnalyser.sortMoves.
     * The branching factor is also updated.
     * @param n
     * @return List of moves for the next player of the given board
     */
    private List<Move> genChildren(TreeSearchBoard n) {
        List<Move> children = n.getMoves();
        // Pruning in AlphaBeta works best when moves are sorted with the most promising first
        _boardAnalyser.sortMoves(null, children);
        updateBranchingFactor(children.size()); // Support time estimation
        return children;
    }

    /**
     * Generates a new copy of the given board a makes the given move.
     * @param n
     * @param c
     * @return new board with the move made
     */
    private static TreeSearchBoard genChild(TreeSearchBoard n, Move c) {
        TreeSearchBoard n2 = new TreeSearchBoardImpl_v13(n);
        try {
            n2.makeMove(c);  // generate new board
        } catch (IllegalMoveException e) {
            throw new RuntimeException("Illegal Move should not happen here",e);
        }
        return n2;
    }

    /**
     * Checks if a given board is already evaluated.
     * @param hashKey - the hashkey of the board we look for its BoardValue
     * @return Returns a BoardValue of the given board found in the boardCache, null if not found
     */
    private TreeSearchEngine_AlphaBetaWithMemory_v2.BoardValue retrieveCache(String hashKey) {
        if (!_cacheEnabled) {
            return null;
        }
        final TreeSearchEngine_AlphaBetaWithMemory_v2.BoardValue boardValue = (TreeSearchEngine_AlphaBetaWithMemory_v2.BoardValue) _boardCache.get(hashKey);
        // -- check boardCache --
        if (boardValue != null) {   // hit
            _nodesFoundInCache++;
            return boardValue;
        } else {                    // miss
            _nodesNotFoundInCache++;
            return null;
        }
    }

    /**
     * saves board evaluations for similar boards
     * @param value
     */
    private void storeCache(TreeSearchEngine_AlphaBetaWithMemory_v2.BoardValue value) {
        if (_cacheEnabled) {
            _boardCache.put(value.getHashKey(), value);
        }

    }

    /**
     * Called to update the current search depth for the player.
     * @return current search depth
     */
    private int updateSearchDepth() {
        int maxDepth;
        if (_maxColor.isBlack()) {
            maxDepth = Reversi.getPlayroom().getCurrentEngineLevelBlack();
        } else if (_maxColor.isWhite()) {
            maxDepth = Reversi.getPlayroom().getCurrentEngineLevelWhite();
        } else {
            throw new RuntimeException("Invalid next player color. Was " + _maxColor);
        }
        return maxDepth;
    }

    /**
     * Approximates the time available for the next move.
     * TODO: Improve this!
     */
    private long approxTime(Board curBoard) {
        int movesLeft;
        long timeLeft;

        //noinspection NumericCastThatLosesPrecision
        movesLeft = (int) ceil(((curBoard.getMaxMoveNumber() - curBoard.getLastMoveNumber()) >> 1)); // >> equals division by 2

        // -- we need very little time at the end of the game so ignore these moves --
        if (movesLeft > 11) {
            movesLeft -= 11;
        }

        if (_maxColor.isBlack()) {
            timeLeft = _game.getBlackTime() - _game.getBlackClock().getTime();
        } else {
            timeLeft = _game.getWhiteTime() - _game.getWhiteClock().getTime();
        }
        // -- time left per remaining move --
        if (movesLeft==0) {
            return timeLeft;
        } else {
            return (timeLeft / movesLeft);
        }
    }


    /**
     * calculates if we can do another iteration
     * TODO: Improve this!
     * @return true for another iteration or false otherwise
     */
    private boolean outOfTime(long startTime, long approxTime, long timeLastIteration) {
        //noinspection NumericCastThatLosesPrecision
        return approxTime - (System.currentTimeMillis() - startTime)
                < (long) (timeLastIteration * _branchingFactor);
    }

    /**
     * Update the current average branching factor:
     * @param newBranchingFactor
     */
    private void updateBranchingFactor(int newBranchingFactor) {
        _branchingSum += newBranchingFactor;
        _branchingFactorCounter++;
        _branchingFactor = (float)_branchingSum / (float)_branchingFactorCounter;
    }

    /**
     * Resets the counter used for the TreeSearchEngineWatcher
     */
    private void resetCounters() {
        // -- reset counters --
        _boardsChecked = 0;
        _nodesChecked = 0;
        _nodesFoundInCache = 0;
        _nodesNotFoundInCache = 0;
        _boardsNonQuiet = 0;
    }

    /**
     * returns a string representation of the class
     */
    @Override
	public String toString() {
        return "Class TreeSearchEingine_v12";
    }

    /**
     * return the number of possible moves for the current move
     * @return int
     */
    public int numberOfPossibleMoves() {
        return _movesSize;
    }

    /**
     * Sets the current game.
     * @param game
     */
    public void setGame(Game game) {
        this._game = game;
    }

    /**
     * returns the current move in calculation
     * @return Move
     */
    public Move getCurMove() {
        return _curMove;
    }


    /**
     * return the current move number
     * @return int
     */
    public int getCurMoveNumber() {
        return _curMoveNumber;
    }

    /**
     * return the current best move
     * @return Move
     */
    public Move getMaxValueMove() {
        return _maxValueMove;
    }

    /**
     * returns the current depth in the search tree (without non-quite extra depth)
     * @return int
     */
    public int getCurSearchDepth() {
        return _curSearchDepth;
    }

    /**
     * returns the current depth in the search tree (with non-quite extra depth)
     * @return int
     */
    public int getCurExtraSearchDepth() {
        return _curExtraSearchDepth;
    }

    /**
     * return the number of nodes checked so far
     * @return int
     */
    public int getNodesChecked() {
        return _nodesChecked;
    }

    /**
     * returns the number of nodes per second for the current calculation
     * @return int
     */
    public int getCurNodesPerSecond() {
        //noinspection NumericCastThatLosesPrecision
        return (int) (1000.0F * ((float) _nodesChecked / ((float) (System.currentTimeMillis() - _startTime))));
    }

    /**
     * returns the used time for the current move
     * @return long
     */
    public long getCurUsedTime() {
        return System.currentTimeMillis() - _startTime;
    }

    /**
     * return the number of boards analysed so far
     * @return int
     */
    public int getBoardsChecked() {
        return _boardsChecked;
    }

    /**
     * return the number of non-quiet boards found so far
     * @return int
     */
    public int getBoardsNonQuiet() {
        return _boardsNonQuiet;
    }

    /**
     * return the number of cache hits so far
     * @return int
     */
    public int getCacheHits() {
        return _nodesFoundInCache;
    }

    /**
     * return the nubmer of cache misses so far
     * @return int
     */
    public int getCacheMisses() {
        return _nodesNotFoundInCache;
    }

    /**
     * return the current cache size
     * @return int
     */
    public int getCurCacheSize() {
        if (!_cacheEnabled) {
            return 0;
        }
        return _boardCache.size();
    }

    /**
     * return the current number of boards in cache
     * @return int
     */
    public int getCurCachedBoards() {
        if (!_cacheEnabled) {
            return 0;
        }
        return _boardCache.getMaxEntries();
    }

    /**
     * To support a nicer log output this method returns spaces according to the remaining
     * search depth
     * @param d
     * @return a String with spaces
     */
    private String spacer(int d) {
        StringBuilder sb = new StringBuilder(_iterativeMaxDepth *5);
        for (int i=0;i<_iterativeMaxDepth-d;i++) {
            sb.append("  >  ");
        }
        return sb.toString();
    }

    /**
     * A class to store a evaluated board in a cache.
     */
    private static class BoardValue {

        private String _hashKey;    // used to store the hash key of the board - the hashkey could be used to recreate the board
        private Move   _bestMove;   // the last move on the board - also used to store the current value
        private int    _searchDepth; // used to find out if upper - and lowerbound are valid
        private int    _value;      // the exact value for the current board
        private int    _upperbound; // _upper and lowerbound are used for subtrees
        private int    _lowerbound; // they can only be used when queried from the same or a lower depth

        private BoardValue(String hashKey, int value, int searchDepth, int upperbound, int lowerbound) {
            _hashKey = hashKey;
            _bestMove = null;
            _searchDepth = searchDepth;
            _value = value;
            _upperbound = upperbound;
            _lowerbound = lowerbound;
        }

        public void   setBestMove   (Move bestMove  ) {_bestMove    = bestMove;  }
        public void   setSearchDepth(int  depth     ) {_searchDepth = depth;  }
        public void   setExactValue (int  value     ) {_value = value; }
        public void   setUpperbound (int  upperbound) {_upperbound  = upperbound;  }
        public void   setLowerbound (int  lowerbound) {_lowerbound  = lowerbound;  }
        public Move   getBestMove   (               ) {return _bestMove; }
        public int    getDepth      (               ) {return _searchDepth; }
        public int    getExactValue (               ) {return _value; }
        public int    getUpperbound (               ) {return _upperbound; }
        public int    getLowerbound (               ) {return _lowerbound; }
        public String getHashKey    (               ) {return _hashKey; }

        @Override
		public String toString() {
            return MessageFormat.format("{0}: bestMove={1} searchDepth={2} value={3} upperbound={4} lowerbound={5}"
                    , _hashKey, _bestMove, _searchDepth, _value, _upperbound, _lowerbound);
        }
    }

}
