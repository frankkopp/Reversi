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
import static java.lang.Integer.*;
import static java.lang.Math.*;

import java.text.MessageFormat;
import java.util.*;

/**
 * <p/>
 * The TreeSearchEngine_MTDf class implements the MTD(f) algorithm described by Aske Plaat (see author).
 * It is based on a AlphaBetaWithMemory algorithm.
 * </p>
 *
 * @author MTD(f): Aske Plaat, Dec 3, 1997, aske@acm.org (<a href="http://www.cs.vu.nl/~aske/mtdf.html">MTD(f)</a>)
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class TreeSearchEngine_MTDf_v6 implements Engine, TreeSearchEngineWatcher {

    // some constants
    private static final int   INITIAL_CACHE_CAPACITY   = 100000;
    private static final float CACHE_LOAD_FACTOR        = 0.75f;
    private static final float INITIAL_BRANCHING_FACTOR = 8.0f;
    
    // some optimization options
    private boolean _USE_PV          = true; // defines if the principal variation search shall be used
    private boolean _USE_NODE_CACHE  = true;
    private boolean _USE_BOARD_CACHE = true;
    private boolean _USE_QUIESCENCE  = true;

    // The current game this engine is used in
    private Game _game = null;

    // My color (Max Player)
    private ReversiColor _maxColor = ReversiColor.NONE;

    // The boardAnalyser is used to analyze the board and return a value
    private TreeSearchBoardAnalyser _boardAnalyser = null;

    // Used for the iterative search to determine the start and the max search depth for each iteration
    private int _currentSearchDepth  = 2;
    private int _iterativeStartDepth = 1;

    // some information about our nodes
    private int  _movesSize           = 0;
    private int  _curMoveNumber       = 0;
    private Move _curMove             = null;
    private Move _currentBestMove     = null;
    private int  _curSearchDepth      = 0;
    private int  _curExtraSearchDepth = 0;
    private int  _boardsChecked       = 0;
    private int  _boardsNonQuiet      = 0;
    private int  _cacheHits           = 0;
    private int  _cacheMisses         = 0;
    private int  _nodesChecked        = 0;

    // Time calculations
    private long  _startTime              = 0L;
    private float _branchingFactor        = INITIAL_BRANCHING_FACTOR;
    private int   _branchingSum           = 8;
    private int   _branchingFactorCounter = 1;

    // Time control
    private Timer   _timer                = null;
    private boolean _softTimeLimitReached = false;
    private boolean _hardTimeLimitReached = false;

    // cache for already analyzed boards
    private              boolean  _cacheEnabled;
    private              LruCache _boardCache   = null;
    private static final int      VALUE_UNKNOWN = MIN_VALUE;

    // A comparator to sort the move list after an iteration
    private static final Comparator<Move> _moveComparator = new MoveComparator();

    // The last calculated value as a starting point for the next move
    private int _lastValue = 0;

    /**
     * Constructor
     */
    public TreeSearchEngine_MTDf_v6() {
        _cacheEnabled = Boolean.valueOf(Reversi.getProperties().getProperty("engine.cacheEnabled"));
        _boardCache = new LruCache(INITIAL_CACHE_CAPACITY, CACHE_LOAD_FACTOR, true,
                parseInt(Reversi.getProperties().getProperty("engine.cacheSize")));
    }

    /**
     * Initializer
     */
    public void init(Player init_player) {
        _maxColor=init_player.getColor();
    }

    /**
     * Starts calculation and returns next move.<p/>
     *
     * @param curBoard
     * @return returns the "best" next move
     */
    public Move getNextMove(Board curBoard) {

        // Initialize timer
        _startTime = System.currentTimeMillis();

        // Timed game?
        boolean timedGame=_game.isTimedGame();

        // Approximate available time for this move
        long approxTime = 0;
        _softTimeLimitReached=false;
        _hardTimeLimitReached=false;
        if (_game.isTimedGame()) {
            approxTime = approxTime(curBoard);
            configureTimeControl(approxTime);
        }

        // Update maximal search depth so we can change the level for the engine each move
        int maxDepth = updateSearchDepth();

        // If we have only very little time left then limit the search depth
        if (timedGame) {
            if(approxTime < 100) {
                maxDepth = 2;
            }
            if(approxTime < 50) {
                maxDepth = 1;
            }
        }

        // Reset all the counters used for the TreeSearchEngineWatcher
        resetCounters();

        // If we do not have a timed game we immediately search with the maximum search depth
        if (!timedGame) {
            _iterativeStartDepth = maxDepth;
        }

        // Create new hashable board based on the current board (deep copy)
        TreeSearchBoard board = new TreeSearchBoardImpl_v14(curBoard);
        
        // Create and Initialize the BoardAnalyser if not already existing
        if (_boardAnalyser == null) {
            _boardAnalyser = new TreeSearchBoardAnalyserImpl_v14(board, _maxColor);
        }

        // Generate moves to check if we have more then 1 move
        List<Move> moves = board.getMoves();
        _movesSize = moves.size();

        // If we have only one move return this and mark it as the only move by setting its value to Integer.MIN_VALUE.
        if (_movesSize==1) {
            moves.get(0).setValue(MIN_VALUE);
            return moves.get(0);
        }

        // Do the search
        Move bestMove = search(moves, maxDepth, board);

        // stop the time keepers
        if (timedGame) {
            _timer.cancel();
        }

        return bestMove;
    }

    private Move search(List<Move> moves, int maxDepth, TreeSearchBoard parentBoard) {

        // Sort the move, good candidates first
        _boardAnalyser.sortMoves(parentBoard, moves);

        // Holds the best move so far
        _currentBestMove = new MoveImpl(moves.get(0));
        _currentBestMove.setValue(-MAX_VALUE);

        // Iterative deepening for time limited game.
        // If we do not have a timed game we immediately search with the maximum search depth.
        for (_currentSearchDepth = _iterativeStartDepth; _currentSearchDepth <= maxDepth; _currentSearchDepth++) {

            ListIterator<Move> movesIterator = moves.listIterator();
            while (movesIterator.hasNext()) {

                // Check for game paused
                _game.waitWhileGamePaused();

                // Store the current move for TreeSearchEngineWatcher
                _curMoveNumber = movesIterator.nextIndex()+1;

                final Move m = movesIterator.next();
                _curMove = m;
                TreeSearchBoard childBoard = genChild(parentBoard, m);

                // Do the actual search using either AlphaBeta or MTDf depending on the search depth.
                // MTDf is more efficient within deeper searches
                if (_currentSearchDepth < 4 || parentBoard.getNextMoveNumber() > 0.8*parentBoard.getMaxMoveNumber()) {
                    m.setValue(AlphaBetaWithMemory(childBoard, -MAX_VALUE, MAX_VALUE, _currentSearchDepth-1, 0));
                } else {
                    if (_game.isTimedGame()) {
                        // In a time game we always have a value for the current best move here
                        m.setValue(MTDf(childBoard, _currentBestMove.getValue(), _currentSearchDepth - 1));
                    } else {
                        // If the game is not timed we use the value of the last own move as a start value
                        m.setValue(MTDf(childBoard, _lastValue, _currentSearchDepth - 1));
                    }
                }

                // we have found a new best move
                if (m.getValue() > _currentBestMove.getValue()) {
                    _currentBestMove = m;
                }

                // If time is running out but still a bit left
                if (_softTimeLimitReached) {
                    break;
                }
            }

            // Resort the array with the results from the last iteration
            // this might give us a better sorting for AlphaBeta cut offs
            Collections.sort(moves, _moveComparator); // -- best first --

            // Check if game has been stopped or if time is up and return the best move so far
            if (_softTimeLimitReached || _game.isOverOrStopped()) {
                break;
            }

        }

        // save the value as a starting point for the next move
        _lastValue = _currentBestMove.getValue();

        return _currentBestMove;
    }

    /**
     * <b>MTDF</b>
     * <p/>
     * Author: Aske Plaat, Dec 3, 1997, aske@acm.org
     * <p/>
     * The algorithm works by calling AlphaBetaWithMemory a number of times with a search window of zero size.
     * The search works by zooming in on the minimax value. Each AlphaBeta call returns a bound on the minimax value.
     * The bounds are stored in upperbound and lowerbound, forming an interval around the true minimax value for that
     * search depth. Plus and minus INFINITY is shorthand for values outside the range of leaf values. When both the
     * upper and the lower bound collide, the minimax value is found.
     * <p/>
     * MTD(f) gets its efficiency from doing only zero-window alpha-beta searches, and using a "good" bound
     * (variable beta) to do those zero-window searches. Conventionally AlphaBeta is called with a wide search
     * window, as in AlphaBeta(root, -INFINITY, +INFINITY, depth), making sure that the return value lies between
     * the value of alpha and beta. In MTD(f) a window of zero size is used, so that on each call AlphaBeta will
     * either fail high or fail low, returning a lower bound or an upper bound on the minimax value, respectively.
     * Zero window calls cause more cutoffs, but return less information - only a bound on the minimax value.
     * To nevertheless find it, MTD(f) has to call AlphaBeta a number of times, converging towards it. The overhead
     * of re-exploring parts of the search tree in repeated calls to AlphaBeta disappears when using a version of
     * AlphaBeta that stores and retrieves the nodes it sees in memory. In order to work, MTD(f) needs a
     * "first guess" as to where the minimax value will turn out to be. The better than first guess is,
     * the more efficient the algorithm will be, on average, since the better it is, the less passes the repeat-until
     * loop will have to do to converge on the minimax value. If you feed MTD(f) the minimax value to start with,
     * it will only do two passes, the bare minimum: one to find an upper bound of value x, and one to find a
     * lower bound of the same value
     * <p/>
     * Typically, one would call MTD(f) in an iterative deepening framework. A natural choice for a first guess is
     * to use the value of the previous iteration, like in getNextMove().
     * <p/>
     * Pseudo code:<br/>
     * <pre>
     * function MTDF(root : node_type; f : integer; d : integer) : integer;
     *  g := f;
     *  upperbound := +INFINITY;
     *  lowerbound := -INFINITY;
     *  repeat
     *      if g == lowerbound then beta := g + 1 else beta := g;
     *      g := AlphaBetaWithMemory(root, beta - 1, beta, d);
     *      if g < beta then upperbound := g else lowerbound := g;
     *      until lowerbound >= upperbound;
     *  return g;
     * </pre>
     *
     * @param root . the root node
     * @param f    - the best move found so far (first guess)
     * @param d    - the search depth
     */
    private int MTDf(final TreeSearchBoard root, final int f, final int d) {
        int g = f;
        int upperbound = MAX_VALUE;
        int lowerbound = -MAX_VALUE;
        int beta;
        while (lowerbound < upperbound) {
            if (g == lowerbound) {
                beta = g + 1;
            } else {
                beta = g;
            }
            g = AlphaBetaWithMemory(root, beta - 1, beta, d, 0);
            if (g < beta) {
                upperbound = g;
            } else {
                lowerbound = g;
            }
            // Hard time limit reached? But we want at least one lowerbound value!
            if (_hardTimeLimitReached && lowerbound>-MAX_VALUE) {
                return lowerbound; // time is up for this move
            }
        }
        return g;
    }

    /**
     * AlphaBeta search with caching.
     * This implementation uses separate max and min node handling because in Reversi it might happen that
     * a player is passed. With a separate min and max handling this is easier to handle.
     *
     * @param parentBoard - node
     * @param alpha
     * @param beta
     * @param d - depth
     * @param extra - the current additional search depth due to non-quiet boards
     */
    private int AlphaBetaWithMemory(final TreeSearchBoard parentBoard, int alpha, int beta, int d, int extra) {

        // Check for game paused
        _game.waitWhileGamePaused();

        // best value so far
        int bestValue;

        // Store the current statistics for TreeSearchEngineWatcher
        _nodesChecked++;
        if (_currentSearchDepth-d > _curSearchDepth) {
            _curSearchDepth = _currentSearchDepth - d; // use only higher values
        }
        if (_currentSearchDepth-d+extra > _curExtraSearchDepth) {
            _curExtraSearchDepth = _currentSearchDepth - d + extra;
        }

        // Cache lookup
        BoardValue cachedBoardValue = retrieveCache(parentBoard.getHashKey());

        // we didn't find something useful(!) in cache so we must create a new boardValueN
        if (cachedBoardValue == null || cachedBoardValue.getDepth() < parentBoard.getLastMoveNumber()+d) {
            cachedBoardValue = new BoardValue(
                    parentBoard.getHashKey(), VALUE_UNKNOWN, 0, MAX_VALUE, -MAX_VALUE);
        }
        // we found something useful - if cache for nodes turned on then us it
        else if (_USE_NODE_CACHE) {
                // we found something in cache
                if (cachedBoardValue.getUpperbound() <= alpha
                        || cachedBoardValue.getUpperbound() == cachedBoardValue.getLowerbound()) {
                    return cachedBoardValue.getUpperbound();
                }
                if (cachedBoardValue.getLowerbound() >= beta) {
                    return cachedBoardValue.getLowerbound();
                }
                alpha = Math.max(alpha, cachedBoardValue.getLowerbound());
                beta  = Math.min(beta, cachedBoardValue.getUpperbound());
        }

        // Check if node is leave node and if board is quiet - increase depth by 1 if non-quiet
        if (_USE_QUIESCENCE && d==0 && !parentBoard.getNextPlayerColor().isNone()
                && _boardAnalyser.notQuiet(parentBoard) && extra<4) { // but only up to 3 plys
            d++; // non-quiet board -> search another level
            extra++; // just for statistics
            _boardsNonQuiet++; // just for statistics
        }

        // Processing node
        if (d == 0 || parentBoard.getNextPlayerColor().isNone()) { // n is LEAFNODE
            return leafNode(cachedBoardValue, parentBoard, d);
        }
        else if (parentBoard.getNextPlayerColor() == _maxColor) { // n is a MAXNODE
            bestValue = maxNode(parentBoard, alpha, beta, d, extra);
        }
        else if (parentBoard.getNextPlayerColor() == _maxColor.getInverseColor()) { // n is a MINNODE
            bestValue = minNode(parentBoard, alpha, beta, d, extra);
        } else {
            throw new RuntimeException("No next player!");
        }

        // Cache update
        if (_USE_NODE_CACHE) {
            updateCacheNode(parentBoard, cachedBoardValue, alpha, beta, bestValue, d);
        }

        return bestValue;
    }

    /**
     * Calculates the leaf nodes.
     * @param cachedBoardValue
     * @param parentBoard
     * @param d
     * @return value of the board in a null move
     */
    private int leafNode(BoardValue cachedBoardValue, TreeSearchBoard parentBoard, int d) {
        int value;
        if (cachedBoardValue.getExactValue() == VALUE_UNKNOWN) { // cache miss - evaluate the board and store in cache
            // Calculate heuristic value for the board
            value = _boardAnalyser.analyse(parentBoard);
            _boardsChecked++;
            if (_USE_BOARD_CACHE) {
                updateCacheLeaf(parentBoard, cachedBoardValue, value, d);
            }
        } else { // cache hit and value found
            value = cachedBoardValue.getExactValue();
        }
        return value;
    }

    /**
     * Calculates the best move for a max position (_maxPlayer's turn)
     * @param parentBoard
     * @param alpha
     * @param beta
     * @param d
     * @param extra
     * @return best move with value for the position
     */
    private int maxNode(TreeSearchBoard parentBoard, int alpha, int beta, int d, int extra) {
        Move currentBestMoveForNode;
        // Generate moves for current player
        List<Move> children = genChildren(parentBoard);
        ListIterator<Move> childrenIterator = children.listIterator();
        // Set the first move as the current best move
        currentBestMoveForNode = children.get(0);
        currentBestMoveForNode.setValue(-MAX_VALUE);
        // Loop though children
        while(currentBestMoveForNode.getValue() < beta && childrenIterator.hasNext()) {
            // generate next child out of the next move and a copy of our node
            Move currentMove = childrenIterator.next();
            TreeSearchBoard childBoard = genChild(parentBoard, currentMove);
            // Recursion
            if (_USE_PV && childrenIterator.previousIndex()>0) {
                currentMove.setValue(AlphaBetaWithMemory(childBoard, alpha, alpha+1, d-1, extra));
                 if (alpha < currentMove.getValue() && currentMove.getValue() < beta) { // Check for failure.
                     currentMove.setValue(AlphaBetaWithMemory(childBoard, currentMove.getValue(), beta, d-1, extra));
                 }
            }  else {
                currentMove.setValue(AlphaBetaWithMemory(childBoard, alpha, beta, d-1, extra));
            }
            // Did we find something new?
            if (currentMove.getValue() > currentBestMoveForNode.getValue()) {
                currentBestMoveForNode=currentMove; // new best move for max player
            }
            if (currentBestMoveForNode.getValue() > alpha) { // a = Math.max(a, currentBestMoveForNode.getValue());
                alpha = currentBestMoveForNode.getValue();
            }
            if (alpha >= beta) {
                break;
            }
            // Check if game has been stopped or the hard time limit has been reached
            // and return the best move so far
            if (_hardTimeLimitReached || _game.isOverOrStopped()) {
                break;
            }
        }
        return currentBestMoveForNode.getValue();
    }

    /**
     * * Calculates the best move for a min position (_minPlayer's turn)
     * @param parentBoard
     * @param alpha
     * @param beta
     * @param d
     * @param extra
     * @return best move with value for the position
     */
    private int minNode(TreeSearchBoard parentBoard, int alpha, int beta, int d, int extra) {
        Move currentBestMoveForNode;
        // Generate moves for current player
        List<Move> children = genChildren(parentBoard);
        ListIterator<Move> childrenIterator = children.listIterator();
        // Set the first move as the current best move
        currentBestMoveForNode = children.get(0);
        currentBestMoveForNode.setValue(MAX_VALUE);
        // Loop though children
        while(currentBestMoveForNode.getValue() > alpha && childrenIterator.hasNext()) {
            // generate next child out of the next move and a copy of our node
            Move currentMove = childrenIterator.next();
            TreeSearchBoard childBoard = genChild(parentBoard, currentMove);
            // Recursion
            if (_USE_PV && childrenIterator.previousIndex()>0) {
                currentMove.setValue(AlphaBetaWithMemory(childBoard, beta-1, beta, d-1, extra));
                if (alpha < currentMove.getValue() && currentMove.getValue() < beta) { // Check for failure.
                    currentMove.setValue(AlphaBetaWithMemory(childBoard, alpha, currentMove.getValue(), d-1, extra));
                }
            } else {
                currentMove.setValue(AlphaBetaWithMemory(childBoard, alpha, beta, d-1, extra));
            }
            // Did we find something new?
            if (currentMove.getValue() < currentBestMoveForNode.getValue()) {
                currentBestMoveForNode=currentMove; // we have a new best move for min player
            }
            if (currentBestMoveForNode.getValue() < beta) { //b = Math.min(b, currentBestMoveForNode.getValue());
                beta=currentBestMoveForNode.getValue();
            }
            if (alpha >= beta) {
                break;
            }
            // Check if game has been stopped or the hard time limit has been reached
            // and return the best move so far
            if (_hardTimeLimitReached || _game.isOverOrStopped()) {
                break;
            }
        }
        return currentBestMoveForNode.getValue();
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
        _boardAnalyser.sortMoves(n, children);
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
        TreeSearchBoard n2 = new TreeSearchBoardImpl_v14((TreeSearchBoardImpl_v14)n);
        try {
            n2.makeMove(c);  // generate new board
        } catch (IllegalMoveException e) {
            throw new RuntimeException("Illegal Move should not happen here",e);
        }
        return n2;
    }

    /**
     * Called to update the cache when a board was evaluated.
     * @param parentBoard
     * @param cachedBoardValue
     * @param value
     */
    private void updateCacheLeaf(TreeSearchBoard parentBoard, BoardValue cachedBoardValue, int value, int d) {
        cachedBoardValue.setExactValue(value);
        cachedBoardValue.setUpperbound(value);
        cachedBoardValue.setLowerbound(value);
        cachedBoardValue.setSearchDepth(parentBoard.getLastMoveNumber()+d);
        storeCache(cachedBoardValue);
    }

    /**
     * Called to update the cache when new lower- or upperbound have been found.
     *
     * @param parentBoard
     * @param cachedBoardValue
     * @param alpha
     * @param beta
     * @param value
     * @param d
     */
    private void updateCacheNode(TreeSearchBoard parentBoard, BoardValue cachedBoardValue,
                                 int alpha, int beta, int value, int d) {
        // Fail low result implies an upper bound
        if (value <= alpha) {
            cachedBoardValue.setUpperbound(value);
        }
        // Found an accurate minimax value - will not occur if called with zero window
        if (alpha < value && value < beta) {
            cachedBoardValue.setLowerbound(value);
            cachedBoardValue.setUpperbound(value);
        }
        // Fail high result implies a lower bound
        if (value >= beta) {
            cachedBoardValue.setLowerbound(value);
        }
        cachedBoardValue.setSearchDepth(parentBoard.getLastMoveNumber()+d);
        storeCache(cachedBoardValue);
    }

    /**
     * Checks if a given board is already evaluated.
     * @param hashKey - the hashkey of the board we look for its BoardValue
     * @return Returns a BoardValue of the given board found in the boardCache, null if not found
     */
    private BoardValue retrieveCache(String hashKey) {
        if (!_cacheEnabled) {
            return null;
        }
        final BoardValue boardValue = (BoardValue) _boardCache.get(hashKey);
        // -- check boardCache --
        if (boardValue != null) {   // hit
            _cacheHits++;
            return boardValue;
        } else {                    // miss
            _cacheMisses++;
            return null;
        }
    }

    /**
     * saves board evaluations for similar boards
     * @param value
     */
    private void storeCache(BoardValue value) {
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
     */
    private long approxTime(Board curBoard) {
        int movesLeft;
        long timeLeft;
        //noinspection NumericCastThatLosesPrecision
        movesLeft = (int) ceil(((curBoard.getMaxMoveNumber() - curBoard.getLastMoveNumber()) >> 1)); // >> equals division by 2
        if (_maxColor.isBlack()) {
            timeLeft = _game.getBlackTime() - _game.getBlackClock().getTime();
        } else {
            timeLeft = _game.getWhiteTime() - _game.getWhiteClock().getTime();
        }
        // Give some overhead time so that in games with very low available time we do not run out of time
        timeLeft-=500; // this should do
        // -- time left per remaining move --
        if (movesLeft==0) {
            return max(timeLeft, 0);
        } else {
            // double factor = 1.0d + Math.cos((2*Math.PI * (((double)curBoard.getNextMoveNumber()/(double)curBoard.getMaxMoveNumber())-(1.0d / 3.0d))));
            //System.out.print("MoveNumber: "+curBoard.getNextMoveNumber()+" t="+factor +" ");
            return max((timeLeft/movesLeft),0);
        }
    }

    /**
     * Configure and start time keepers
     * @param approxTime
     */
    private void configureTimeControl(long approxTime) {
        // standard limits
        float soft = 0.75f;
        float hard = 1.25f;
        // limits for very short available time
        if (approxTime < 100) {
            soft = 0.8f;
            hard = 0.9f;
        }
        // limits for higher available time
        if (approxTime > 1000) {
            soft = 1.0f;
            hard = 1.2f;
        }
        _timer = new Timer("TimeKeeper "+_maxColor.toString()+" "+" ApproxTime: " +approxTime + " Soft:"+soft+" Hard:"+hard);
        _timer.schedule(new TreeSearchEngine_MTDf_v6.TimeKeeper(1), (long) (approxTime * soft));
        _timer.schedule(new TreeSearchEngine_MTDf_v6.TimeKeeper(2), (long) (approxTime * hard));
    }

    /**
     * Update the current average branching factor:
     * @param newBranchingFactor
     **/
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
        _cacheHits = 0;
        _cacheMisses = 0;
        _boardsNonQuiet = 0;
        _curSearchDepth = 0;
        _curExtraSearchDepth = 0;
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
        return _currentBestMove;
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
        //noinspection NumericCastThatLosesPrecision,MagicNumber
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
        return _cacheHits;
    }

    /**
     * return the nubmer of cache misses so far
     * @return int
     */
    public int getCacheMisses() {
        return _cacheMisses;
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
     * This comparator is used to sort move according their current value.
     */
    private static class MoveComparator implements Comparator<Move> {
        public int compare(Move o1, Move o2) {
            final int o1Value = o1.getValue();
            final int o2Value = o2.getValue();
            if               (o1Value < o2Value) {
                return 1;
            } else          if (o1Value > o2Value) {
                return -1;
            } else {
                return 0;
            }
        }
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

    /**
     * This TimeKeeper class is used to implement a Timer that calls this Timekeeper when a time limit has been
     * reached. This TimeKeeper then sets the time limit reached flags.
     */
    private class TimeKeeper extends TimerTask {
        private final int _mode;
        private TimeKeeper(int mode) {
            _mode=mode;
        }
        /**
         * The action to be performed by this timer task.
         */
        @Override
		public void run() {
            switch(_mode) {
                case 1:
                    _softTimeLimitReached=true;
                    break;
                case 2:
                    _hardTimeLimitReached=true;
                    break;
                default:
                    throw new RuntimeException("TimeKeeper mode not set.");
            }
        }
    }

}
