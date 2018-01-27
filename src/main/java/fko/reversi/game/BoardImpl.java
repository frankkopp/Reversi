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

package fko.reversi.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The board class has all the information regarding an actual current
 * Reversi board like stones the fields, who is next player, etc..<br/>
 * It also is responsible to generate regular moves according to the Reversi
 * rules and actually execute moves on the given board.
 * <p/>
 * This board returns a String hash key only based on the fields and the next player of the board.
 * <p/>
 * Outside this class fields are addressed starting from 1 to board dimension<br/>
 * Within this class fields are addressed starting with 0 to board dimension -1<br/>
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class BoardImpl implements Board {

    /**
     * 2-dimensional array [col][row] beginning with 0,0 bottom left corner.<br>
     * Due to performance reasons, subclasses are allowed to use this directly!<br/>
     * <b>Use carefully!</b>
     */
    protected ReversiColor[][] _fields;

    /**
     * board dimension (dim * dim)
     */
    private int _dim;
    private int _numberOfFields;

    /**
     * Memorizes the last move --
     */
    private Move _lastMove = null;

    /**
     * Memorizes the move path --
     */
    private List<Move> _moveHistory = null;

    /**
     * ReversiColor of the next player to move
     */
    private ReversiColor _nextPlayerColor;

    /**
     * is true when the last and the next player are the same
     */
    private boolean _hasPass = false;

    /**
     * How many moves has there been so far
     */
    private int _lastMoveNumber = 0;

    /**
     * How many moves can there be at all
     */
    private int _maxMoveNumber = 0;

    /**
     *  Number of pieces for black
     * (incrementally counted)
     */
    private int _piecesBlack = 2;

    /**
     *  Number of pieces for black
     * (incrementally counted)
     */
    private int _piecesWhite = 2;

    /**
     * array for faster toString operations
     */
    private static final char[] _fieldString = {'X', '-', 'O'};

    /**
     * To support a clockwise lookup around a field
     * This speeds up things a little
     */
    private static final int[][] clockwiseLookup = {
        {0, 1}, // 0
        {1, 1}, // 1 --> bottom right corner (-1,1)
        {1, 0}, // 2
        {1, -1}, // 3 --> bottom left corner (1,1)
        {0, -1}, // 4
        {-1, -1}, // 5 --> top left corner (1,-1)
        {-1, 0}, // 6
        {-1, 1}   // 7 --> top right corner (-1,-1)
    };

    // Cache for generated moves
    private boolean _cachedMoveListValid;
    private List<Move> _cachedMoveList;
    private final Object _cachedMoveListLock = new Object();

    /**
     * Incrementally updated unique representation of the board usable for hashKey, hashCode and toString()
     */
    private volatile StringBuilder _stringBoard = null;

    // field liberties
    private int[][] _liberties = null;

    /**
     * Creates a standard Reversi board with default dimensions (8x8).
     */
    public BoardImpl() {
        _dim = DEFAULT_DIM;
        _numberOfFields= _dim * _dim;
        _fields = new ReversiColor[_dim][_dim];
        _moveHistory = new ArrayList<Move>(_numberOfFields);
        _maxMoveNumber = _numberOfFields - 4;
        _nextPlayerColor = ReversiColor.BLACK;
        _hasPass=false;
        _cachedMoveListValid =false;
        _cachedMoveList =null;
        initBoard();
    }

    /**
     * Creates a Reversi board with the given dimension.
     * @param initialDimension
     */
    public BoardImpl(int initialDimension) {
        if ( initialDimension<4 || initialDimension%2!=0) {
            throw new IllegalArgumentException(
                    "Parameter newBoardDimension must be >= 4 and a multiple of 2. Was " + initialDimension);
        }
        _dim = initialDimension;
        _numberOfFields = _dim * _dim;
        _fields = new ReversiColor[_dim][_dim];
        _moveHistory = new ArrayList<Move>(_numberOfFields);
        _maxMoveNumber = _numberOfFields - 4;
        _nextPlayerColor = ReversiColor.BLACK;
        _hasPass=false;
        _cachedMoveListValid =false;
        _cachedMoveList =null;
        initBoard();
    }

    /**
     * Creates a new Reversi board as a exact deep copy of the given BordImpl board
     * @param  oldBoard
     */
    public BoardImpl(BoardImpl oldBoard) {
        _dim = oldBoard._dim;
        _numberOfFields = _dim*_dim;

        // -- copy fields --
        _fields = new ReversiColor[_dim][_dim];
        for (int col = 0; col < _dim; col++)
            // we can do an array copy here as we do know the implementation of the old board
        {
            System.arraycopy(oldBoard._fields[col], 0, _fields[col], 0, _dim);
        }

        // -- liberties  --
        _liberties = new int[_dim][_dim];
        for (int col = 0; col < _dim; col++) {
            System.arraycopy(oldBoard._liberties[col], 0, _liberties[col], 0, _dim);
        }

        _nextPlayerColor = oldBoard._nextPlayerColor;
        if (oldBoard._lastMove != null) {
            _lastMove = new MoveImpl(oldBoard._lastMove);
        }

        // -- copy stringBoard --
        _stringBoard = new StringBuilder(oldBoard._stringBoard);

        // -- copy lastMoves --
        _moveHistory = new ArrayList<Move>(_numberOfFields);
        for (Move a_moveHistory : oldBoard._moveHistory) {
            _moveHistory.add(new MoveImpl(a_moveHistory));
        }
        _lastMoveNumber = oldBoard._lastMoveNumber;
        _maxMoveNumber = oldBoard._maxMoveNumber;
        _piecesBlack = oldBoard._piecesBlack;
        _piecesWhite = oldBoard._piecesWhite;
        _hasPass=oldBoard._hasPass;

        // copy cached move list - should be faster than a recalculation of the move list
        if (oldBoard._cachedMoveListValid) {
            _cachedMoveList = oldBoard.getMoves(); // cache is checked and if cached a deep copy returned
            _cachedMoveListValid =true;
        } else {
            _cachedMoveListValid =false;
            _cachedMoveList =null;
        }

    }

    /**
     * Creates a new Reversi board as a exact deep copy of the given Board board.
     * @param oldBoard
     */
    public BoardImpl(Board oldBoard) {
        if (oldBoard ==null) {
            throw new NullPointerException("Parameter oldBoard may not be null");
        }

        _dim = oldBoard.getDim();
        _numberOfFields = _dim*_dim;

        // -- copy fields --
        _fields = new ReversiColor[_dim][_dim];
        for (int col = 0; col < _dim; col++) {
            for (int row = 0; row < _dim; row++)
            // we can't do an arraycopy here as we do not know the implemantation of the old board
            {
                _fields[col][row] = oldBoard.getField(col + 1, row + 1);
            }
        }

        _nextPlayerColor = oldBoard.getNextPlayerColor();
        if (oldBoard.getLastMove() != null) {
            _lastMove = new MoveImpl(oldBoard.getLastMove());
        }

        // -- create stringBoard --
        _stringBoard = new StringBuilder(oldBoard.toString());

        // -- copy lastMoves --
        _moveHistory = new ArrayList<Move>(_numberOfFields);
        for (Move move : oldBoard.getMoveHistory()) {
            //noinspection ObjectAllocationInLoop
            _moveHistory.add(new MoveImpl(move));
        }
        _lastMoveNumber = oldBoard.getLastMoveNumber();
        _maxMoveNumber = oldBoard.getMaxMoveNumber();
        _piecesBlack = oldBoard.getPiecesBlack();
        _piecesWhite = oldBoard.getPiecesWhite();
        _hasPass=oldBoard.hasPass();
        // we can't copy the cached move list as this is not public in Interface Board
        _cachedMoveListValid =false;
        _cachedMoveList =null;

        // this is as fast as copying
        initLiberties();
    }

    /**
     * Returns all legal moves for the next player.<br>
     * This method uses a cache for the moves generated. If the cache is valid a shallow copy of the
     * cached move list is returned. The move themselves are not copied as the moves from the MoveImpl-class
     * used are mostly immutable. Only the value of the move can be changed during the lifetime of such a move.
     * As this also can lead to trouble one should create copies of all moves in the list before using their values.
     *
     * @return returns an unordered ArrayList of possible moves
     */
    public synchronized List<Move> getMoves() {
        // If we already know that there are no more legal moves then return an empty list.
        if (_nextPlayerColor.isNone()) {
            return Collections.emptyList();
        }
        // check if the move list is already cached
        synchronized (_cachedMoveListLock) {
            if (_cachedMoveListValid) {
                // Make a shallow copy of our List so it can not be changed.
                // Be careful as the list items (Moves) still can be changed. But as our MoveImpl class is mostly
                // immutable the move (col, row) itself cannot be changed. Only the value can be changed. If this
                // makes trouble we must also copy the moves itself (deep copy).
                return new ArrayList<Move>(_cachedMoveList);
                /*
                // make a deep copy and return the list - should be faster then recalculating the list
                List<Move> temp = new ArrayList<Move>(_cachedMoveList.size()+1);
                for (Move m : _cachedMoveList) {
                    //noinspection ObjectAllocationInLoop
                    temp.add(new MoveImpl(m));
                    // reset the values of the move to UNKONWN
                    m.setValue(Move.VALUE_UNKNOWN);
                }
                return temp;
                */
            }
            else {
	            List<Move> result = generateMoves();
	            _cachedMoveList=result;
	            _cachedMoveListValid =true;
	            return result;
            }
        }
    }

    /**
     * Generates the list of possible moves the current board and caches the result.
     * @return list of possible moves
     */
    private List<Move> generateMoves() {
        // Find all possible moves
        List<Move> result = new ArrayList<Move>(20); // max expected number of possible moves
        for (int col = 0; col < _dim; col++) {
            outer_loop:
            for (int row = 0; row < _dim; row++) {
                if (_fields[col][row] != ReversiColor.EMPTY) {
                    continue;
                }
                // place stone clockwise around current field and check if stones can be flipped
                for (int i = 0; i < 8; i++) {
                    if (flip(_nextPlayerColor, col, row, clockwiseLookup[i][0], clockwiseLookup[i][1], false) > 0) {
                        result.add(new MoveImpl(col + 1, row + 1, _nextPlayerColor));
                        continue outer_loop;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Invalidates the cached move list. Should be called whenever the board itself is modified.
     */
    private void invalidateCachedMoveList() {
        // -- invalidate move list cache
        synchronized (_cachedMoveListLock) {
            _cachedMoveListValid=false;
            _cachedMoveList=null;
        }
    }

    /**
     * Checks if next player has a legal move
     * @return returns true when the next player has a legal move
     */
    public synchronized boolean hasLegalMoves() {
        // last possible move
        if (_lastMoveNumber == _maxMoveNumber) {
            return false;
        }
        // We already know that we do not have a legal move.
        if (_nextPlayerColor.isNone()) {
            return false;
        }
        // check if the move list is already cached
        synchronized (_cachedMoveListLock) {
            if (_cachedMoveListValid) {
                return !_cachedMoveList.isEmpty();
            }
        }
        // move list was not cached - call getMoves which creates a move list and caches it for later use
        return !getMoves().isEmpty();
    }

    /**
     * Makes move on board according to rules. It calls switchPlayerColor() at the and to change the next player.
     * @param move
     */
    public synchronized void makeMove(Move move) throws IllegalMoveException {
        // -- assert ---
        if (move==null) {
            throw new NullPointerException("Error: Parameter move in BoardImpl.makeMove() may not be null");
        }

        // -- legal move? ---
        if (!isLegalMove(move)) {
            throw new IllegalMoveException("Error: BoardImpl.makeMove() Tried to make illegal move: " + move);
        }

        int          col   = move.getCol() - 1;
        int          row   = move.getRow() - 1;
        ReversiColor color = move.getColor();

        invalidateCachedMoveList();

        // -- save last move ---
        this._lastMove = move;
        this._moveHistory.add(_lastMove);

        // -- set fields ---
        _fields[col][row] = color;

        // -- update piece counters --
        if      (color.isBlack()) {
            this._piecesBlack++;
        } else if (color.isWhite()) {
            this._piecesWhite++;
        }

        // update String representation of the board for this newly placed stone itself
        // the rest of the changed stones are handled in the overwritten flip() method itself
        updateStringBoard(col, row);

        // -- turn stones ---
        turn(move, true);

        // -- increase lastMoveNumber counter --
        _lastMoveNumber++;

        // -- determine next player --
        setNextPlayer();

        // -- updates the liberties of the fields
        updateLiberties(move);

    }

    /**
     * Determines the next player. This method assumes that the current player has just made a move.
     * It starts with switch the players and checking for possible moves.
     */
    private void setNextPlayer() {
        // First switch the player and check if we have a legal move
        _nextPlayerColor = _nextPlayerColor.getInverseColor();
        // now the move list cannot be correct anymore
        invalidateCachedMoveList();
        if (!hasLegalMoves()) {
            // we don't have a legal move so we switch back and check if we have a pass
            _nextPlayerColor = _nextPlayerColor.getInverseColor();
            // now the move list cannot be correct anymore
            invalidateCachedMoveList();
            if(hasLegalMoves()) {
                // We have a move so we have a pass - keep the next player
                _hasPass=true;
            } else {
                // We don't have a move at all --> no more moves --> game over
                _hasPass=false;
                _nextPlayerColor = ReversiColor.NONE;
            }
        } else {
            _hasPass=false;
        }
        _stringBoard.setCharAt(_numberOfFields +1,(_nextPlayerColor.toCharSymbol()));
    }

    /**
     * Checks for legal move
     * @param move
     * @return returns true if the given move is legal on this board
     */
    public synchronized boolean isLegalMove(Move move) {
        if (move == null) {
            return false;
        }

        final int          col   = move.getCol() - 1;
        final int          row   = move.getRow() - 1;
        final ReversiColor color = move.getColor();

        // -- stay within board
        if (!isWithinBoard(col, row)) {
            return false;
        }

        // -- only valid move if field is empty
        if (_fields[col][row] != ReversiColor.EMPTY) {
            return false;
        }

        // place stone clockwise around current field and check if stones can be flipped
        for (int i = 0; i < 8; i++) {
            if (this.flip(color, col, row, clockwiseLookup[i][0], clockwiseLookup[i][1], false) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Turns all stones according to rules for a given move.
     * If "commit=true" the board will be changed.
     * If "commit=false" the board will NOT be changed.
     * The number of turned stones is returned
     * @param move
     * @param commit
     * @return number of stones flipped
     */
    private int turn(Move move, boolean commit) {
        assert move != null : "Parameter move may not be null";

        final int          col   = move.getCol() - 1;
        final int          row   = move.getRow() - 1;
        final ReversiColor color = move.getColor();

        int tmpCount = 0;
        // place stone clockwise around current field and flip fields if fields can be flipped
        for (int i = 0; i < 8; i++) {
            tmpCount += flip(color, col, row, clockwiseLookup[i][0], clockwiseLookup[i][1], commit);
        }
        return tmpCount;
    }

    /**
     * Turns stones in a given direction. Direction is given by "colinc" and "rowinc".
     * If "commit=true" the board will be changed.
     * If "commit=false" the board will NOT be changed.
     * The number of turned stones is returned
     * @param color
     * @param col
     * @param row
     * @param colinc
     * @param rowinc
     * @param commit
     * @return The number of turned stones is returned
     */
    private int flip(ReversiColor color, int col, int row, int colinc, int rowinc, boolean commit) {
        int orgcol = col;
        int orgrow = row;
        int newcol = col + colinc;
        int newrow = row + rowinc;

        // No calls to other helper methods because of timing - inline everything

        // stay within board
        if (newcol < 0 || newcol >= _dim || newrow < 0 || newrow >= _dim) {
            return 0;
        }

        // ignore current or non-empty fields as well as own stones
        if (((colinc == 0) && (rowinc == 0)) || _fields[newcol][newrow] == color) {
            return 0;
        }

        int count = 0;

        // while within board check for oppenents stones
        // stay within board
        ReversiColor opponents_color = color.getInverseColor();
        while (newcol >= 0 && newcol < _dim && newrow >= 0 && newrow < _dim
                && _fields[newcol][newrow] == opponents_color) {
            newcol += colinc;
            newrow += rowinc;
            count++;
        }

        // if we are out of board then the opponents stones last until board border
        // check also if field is empty
        // stay within board
        if (newcol < 0 || newcol >= _dim || newrow < 0 || newrow >= _dim
                || _fields[newcol][newrow] == ReversiColor.EMPTY) {
            return 0;
        }

        // when we are here we have found one of our stones enclosing the opponents stones -> write fields
        if (commit) {
            while ((col != newcol) || (row != newrow)) {
                _fields[col][row] = color;
                if (!(col == orgcol && row == orgrow)) {
                    // inlined: updateStringBoard(col, row);
                    _stringBoard.setCharAt(row * _dim +col, _fields[col][row].toCharSymbol());
                    if (color == ReversiColor.BLACK) {
                        this._piecesBlack++;
                        this._piecesWhite--;
                    } else if (color == ReversiColor.WHITE) {
                        this._piecesBlack--;
                        this._piecesWhite++;
                    }
                }
                col += colinc;
                row += rowinc;
            }
        }

        return count;
    }

    /**
     * Create start setup of game board
     */
    private void initBoard() {
        // First set all emtpy
        for (int row = 0; row < _dim; row++) {
            for (int col = 0; col < _dim; col++) {
                _fields[col][row] = ReversiColor.EMPTY;
            }
        }
        // 4,4 = WHITE
        _fields[(_dim >> 1) - 1][(_dim >> 1) - 1] = ReversiColor.BLACK; // >> equals division by 2
        // 4,5 = BLACK
        _fields[(_dim >> 1) - 1][(_dim >> 1)] = ReversiColor.WHITE; // >> equals division by 2
        // 5,4 = BLACK
        _fields[(_dim >> 1)][(_dim >> 1) - 1] = ReversiColor.WHITE; // >> equals division by 2
        // 5,5 = WHITE
        _fields[(_dim >> 1)][(_dim >> 1)] = ReversiColor.BLACK; // >> equals division by 2
        // also initialize the StringBoard
        initStringBoard();
        // also initialize the liberties
        initLiberties();
    }

    /**
     * Returns true if the last player is the next player --> pass
     *
     * @return true - if last player is next player
     */
    public boolean hasPass() {
        return _hasPass;
    }

    /**
     * Return color of next player or ReversiColor.NONE if there are no more moves.
     * @return color of player for next move or none when there are no more moves.
     */
    public ReversiColor getNextPlayerColor() {
        return _nextPlayerColor;
    }

    /**
     * Return color of last player
     * @return color of player from last move (-1,0,1 -- BLACK, EMPTY, WHITE)
     */
    public synchronized ReversiColor getLastPlayerColor() {
        if (_lastMove != null) {
            return _lastMove.getColor();
        } else {
            return ReversiColor.EMPTY;
        }
    }

    /**
     * Return dimension of board
     * @return returns the dimension (number of rows/columns) of the board
     */
    public int getDim() {
        return _dim;
    }

    /**
     * Returns the number of fields on the board. Is equal to getDim()*getDim() but precalculated.
     */
    public int getNumberOfFields() {
        return _numberOfFields;
    }

    /**
     * Checks if col, row is still within the board.<br/>
     * <b>This is starting from 0 to dim-1 and not from 1 to dim!</b>
     *
     * @param col
     * @param row
     * @return returns true if coordinates are valid for the current board
     */
    public boolean isWithinBoard(int col, int row) {
        // stay within board
        return !(col < 0 || col >= this._dim || row < 0 || row >= this._dim);
    }

    /**
     * Return color of field row, col
     * @param row
     * @param col
     * @return color of given field (-1,0,1 -- BLACK, EMPTY, WHITE)
     */
    public synchronized ReversiColor getField(int col, int row) {
        return _fields[col - 1][row - 1];
    }

    /**
     * Returns a character to use for a String representation of the field.<br/>
     * It accepts Player.BLACK (X), Player.WHITE (O), Player.EMPTY (-) otherwise returns
     * an empty character.
     * @return char - one of 'X', '-', 'O' or ' '
     */
    public static char getFieldString(int color) {
        if (color < -1 || color > 1) {
            return ' ';
        }
        return _fieldString[color+1];
    }

    /**
     * Getter for lastMove
     * @return returns the last move mode on this board
     */
    public Move getLastMove() {
        return _lastMove;
    }

    /**
     * Is used from subclasses to set the last move on the board.
     * @param newLastMove
     */
    protected void setLastMove(Move newLastMove) {
        assert newLastMove!=null : "Parameter newLastMove may not be null";
        _lastMove = newLastMove;
    }

    /**
     * Getter for lastMoves
     * @return returns the move path for this board
     */
    public synchronized List<Move> getMoveHistory() {
        return Collections.unmodifiableList(_moveHistory);
    }

    /**
     * Is used by subclasses to add a move to the move history
     */
    protected synchronized void addToMoveHistory(Move m) {
        _moveHistory.add(m);
    }

    /**
     * Getter for the maximal possible number of moves
     * @return returns the maximal number of move on this board
     */
    public int getMaxMoveNumber() {
        return _maxMoveNumber;
    }

    /**
     * Returns the number of the next move
     * @return returns the number of the next move
     */
    public synchronized int getNextMoveNumber() {
        return _lastMoveNumber +1;
    }

    /**
     * Returns the number of move made so far
     * @return returns the number of moves made so far
     */
    public int getLastMoveNumber() {
        return _lastMoveNumber;
    }

    /**
     * Returns number of black stones
     * @return number of black stone
     */
    public int getPiecesBlack() {
        return this._piecesBlack;
    }

    /**
     * Returns number of white stones
     * @return number of white stones
     */
    public int getPiecesWhite() {
        return this._piecesWhite;
    }

    /**
     * Returns a pre-defined set of values to check all 8 directions of a field in a clockwise manner.<br/>
     * Should be used in a loop from 1 to 8:<br/>
     * <pre>
     * for (int i = 0; i < 8; i++) {
     *  int colInc = getClockwiseLookup((startCornerLookupIndex + i) % 8,0);
     *  int rowInc = getClockwiseLookup((startCornerLookupIndex + i) % 8,1);
     * ...
     * </pre>
     * Definition:<br/>
     * <pre>
     * private final static int[][] clockwiseLookup = {
     *   {0, 1}, // 0
     *   {1, 1}, // 1 --> bottom right corner (-1,1)
     *   {1, 0}, // 2
     *   {1, -1}, // 3 --> bottom left corner (1,1)
     *   {0, -1}, // 4
     *   {-1, -1}, // 5 --> top left corner (1,-1)
     *   {-1, 0}, // 6
     *   {-1, 1}   // 7 --> top right corner (-1,-1)
     *   };
     * </pre>
     * @param index
     * @param colOrRow
     * @return value
     */
    protected static int getClockwiseLookup(int index, int colOrRow) {
        assert index>=0 && index <8 : "Parameter index is not >=0 and <8. Was "+index;
        assert colOrRow == 0 || colOrRow == 1 : "Parameter colOrRow must be 0 or 1. Was "+colOrRow;
        return clockwiseLookup[index][colOrRow];
    }

    /**
     * Returns the differential of number of pieces
     * @param color
     * @return returns the differential of number of pieces
     */
    public synchronized int getPiecesDiff(ReversiColor color) {
        return color.toInt() * (_piecesWhite - _piecesBlack);
    }

    /**
     * Returns the differential of corners for a given color
     * @param color
     * @return returns the differential of corners for a given color
     */
    public synchronized int getCornerDiff(ReversiColor color) {
        return color.toInt() * (
            _fields[0][0].toInt() +
            _fields[0][_dim - 1].toInt() +
            _fields[_dim - 1][0].toInt() +
            _fields[_dim - 1][_dim - 1].toInt()
            );
    }

    /**
     * Returns the differential of X-squares
     * @param color
     * @return returns the differential of X-squares
     */
    public synchronized int getXsquaresDiff(ReversiColor color) {
        return color.toInt() * (
            _fields[1][1].toInt() +
            _fields[1][_dim - 2].toInt() +
            _fields[_dim - 2][1].toInt() +
            _fields[_dim - 2][_dim - 2].toInt()
            );
    }

    /**
     * Returns the differential of C-squares
     * @param color
     * @return returns the differential of C-squares
     */
    public synchronized int getCsquaresDiff(ReversiColor color) {
        //noinspection OverlyComplexArithmeticExpression
        return color.toInt() * (
            _fields[0][1].toInt() + _fields[1][0].toInt() +
            _fields[0][_dim - 2].toInt() + _fields[1][_dim - 1].toInt() +
            _fields[_dim - 2][_dim - 1].toInt() + _fields[_dim - 1][_dim - 2].toInt() +
            _fields[_dim - 1][1].toInt() + _fields[_dim - 2][0].toInt()
            );
    }

    /**
     * Returns the difference in the mobility for the current player
     *
     * @return int - difference of the mobility for the next player
     */
    public final synchronized int getMobilityDiff() {
        // -- first get mobility for nextPlayer
        int temp = getMoves().size();
        // -- switch player ...
        _nextPlayerColor = _nextPlayerColor.getInverseColor();
        // --- and get potential mobility for other player
        temp -= generateMoves().size(); // we cannot use getMoves as this is cached and we just switched color
        // -- now switch back ..
        _nextPlayerColor = _nextPlayerColor.getInverseColor();
        // return the result
        return temp;
    }

    /**
     * Getter for a hash key for the board. The hash key is calculated based on the fields of the board.
     * So to boards return equal hash keys when they have a similar field occupation.<br/>
     * This hash key is not meant to distinguish different boards. It is meant to be used for caches
     * of boards where similar game situations can be evaluated and cached. In this usage a board would be similar
     * to another when it has an identical field occupation or a transformation of it e.g. a transformation when
     * a board is turned by 90, 180 or 270 degrees.
     * Although the current implementation of this class does not consider these transformations this is subject
     * to change in future versions.
     *
     * @return returns a hash key for this board
     */
    public synchronized String getHashKey() {
        return _stringBoard.toString();
    }

    /**
     * Returns a unique hash code for this board computed in the base of the field occupation
     * and the next player color.<br/>
     * This implementation uses toString().hashCode();
     * @return hash code
     */
    @Override
	public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Checks if two boards are equal by comparing their fields, the next player and the move history.
     * @param o - the board to check for equality
     * @return true - if boards have the same field occupation and have the same move history.
     */
    @Override
	public synchronized boolean equals(Object o) {
        if (o==null || !(o instanceof BoardImpl)) {
            return false;
        }
        if (hashCode() != o.hashCode()) {
            return false;
        }
        return getMoveHistory().equals(((Board) o).getMoveHistory());
    }

    /**
     * Creates a string representation of the board.
     * The string will have dim*dim +2 characters (On a standard 8x8 board this would be 66)
     * An empty field will be represented by a "-", black as "X", white as "O".
     * The last character will determine who has the next move preceeded by a space
     *
     * @return returns a string representing the current board
     */
    @Override
	public synchronized String toString() {
        return _stringBoard.toString();
    }

    /**
      * update the string used for the hash key and toString()
      * @param col
      */
     private void updateStringBoard(int col, int row) {
        _stringBoard.setCharAt(row* _dim +col, _fields[col][row].toCharSymbol());
     }

    /**
     * initialize the string representation of the board
     */
    private void initStringBoard() {
        int dim = _dim;

        // -- first one row --
        StringBuilder tempRow = new StringBuilder(dim);
        for (int i=0; i<dim; i++) {
            tempRow = tempRow.append('-');
        }

        // -- then append all rows to one string
        _stringBoard = new StringBuilder(_numberOfFields + 2);

        for (int i=0; i<dim; i++) {
            _stringBoard = _stringBoard.append(tempRow);
        }

        _stringBoard.append(" X");

        // dim/2 is equal to >> 1
        _stringBoard.setCharAt((((dim >> 1) -1)*dim)+(dim >> 1)-1, 'X'); // =((dim/2-1)*dim)+(dim/2) - 1
        _stringBoard.setCharAt((((dim >> 1) -1)*dim)+(dim >> 1), 'O'); // =((dim/2-1)*dim)+(dim/2)
        _stringBoard.setCharAt(((dim >> 1) *dim)+(dim >> 1)-1, 'O'); // =((dim/2)*dim)+(dim/2) - 1
        _stringBoard.setCharAt(((dim >> 1) *dim)+(dim >> 1), 'X'); // =((dim/2)*dim)+(dim/2)
    }

    /**
     * Returns a String drawing the board to a console.
     */
    public String drawBoard() {

        StringBuilder boardString = new StringBuilder((_dim<<2+5)*_dim);

        // backwards as highest row is on top
        for (int row = _dim; row > 0; row--) {
            // upper border
            boardString.append("    -");   // 4 * space
            for (int col = _dim; col > 0; col--) {
                boardString.append("----"); // dim * -
            }
            boardString.append(Character.LINE_SEPARATOR);
            // row number
            if (row < 10) {
                boardString.append(' ').append(Integer.toString(row)).append(": |");
            } else {
                boardString.append(Integer.toString(row)).append(": |");
            }
            // col fields
            for (int col = 1; col <= _dim; col++) {
                ReversiColor color = getField(col, row);
                if (color.isEmpty()) {
                    boardString.append("   |");
                } else if (color.isBlack()) {
                    boardString.append(" X |");
                } else if (color.isWhite()) {
                    boardString.append(" O |");
                } else {
                    throw new RuntimeException("field value not allowed: field(" + row + ',' + col + ')');
                }
            }
            boardString.append(Character.LINE_SEPARATOR);
        }
        // lower border
        boardString.append("    -");   // 4 * space
        for (int col = _dim; col > 0; col--) {
            boardString.append("----"); // dim * -
        }
        boardString.append(Character.LINE_SEPARATOR);
        // col numbers
        boardString.append("     ");   // 4 * space
        for (int col = 1; col <= _dim; col++) {
            if (col < 10) {
                boardString.append(' ').append(col).append("  ");
            } else {
                boardString.append(' ').append(col).append(' ');
            }
        }
        boardString.append(Character.LINE_SEPARATOR);

        return boardString.toString();
    }

    /**
     * Initialize the liberties of the fields.
     */
    protected void initLiberties() {
        _liberties = new int[_dim][_dim];
        for (int col=0;col<_dim;col++) {
            for (int row=0;row<_dim;row++) {
                _liberties[col][row] = checkLiberties(col,row);
            }
        }
    }

    private int checkLiberties(int col, int row) {
        int c=8;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (!(x==0&&y==0)
                        && (
                        !isWithinBoard(col+x, row+y)
                        || _fields[col+x][row+y].isNotEmpty()) ) {
                    c--;
                }
            }
        }
        return c;
    }

    protected void updateLiberties(Move move) {
        int col = move.getCol()-1;
        int row = move.getRow()-1;
        for (int x = -1; x <= 1; x += 1) {
            for (int y = -1; y <= 1; y += 1) {
                if (!(x==0&&y==0) && isWithinBoard(col+x,row+y)) {
                    _liberties[col + x][row + y]--;
                }
            }
        }
    }

    /**
     * Returns the number of empty fiels next to the field (liberty).
     * @param col
     * @param row
     */
    public int getLiberties(int col, int row) {
        // !! internally we use 0 to dim-1
        return _liberties[col-1][row-1];
    }
}
