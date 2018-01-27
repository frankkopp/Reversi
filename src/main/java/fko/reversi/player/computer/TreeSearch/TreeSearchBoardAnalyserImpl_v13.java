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

import fko.reversi.game.Board;
import fko.reversi.game.ReversiColor;
import fko.reversi.game.Move;

import java.util.Comparator;
import java.util.List;
import java.util.Collections;

/**
 * Analyses a given board and returns an heuristic value for that board
 */
public class TreeSearchBoardAnalyserImpl_v13 implements TreeSearchBoardAnalyser {

    private final Board _board;

    private final ReversiColor _maxPlayer;

    // -- contains value for each field on the board
    private int _fieldValues[][];

    // -- contains a tupel for the weighting of different evaluatione regarding the current move number
    private int _weightMatrix[][];

    // -- inner class moveComparator is used to sort moves --
    private final Comparator _moveComparator = new TreeSearchBoardAnalyserImpl_v13.moveComparator();

    /**
     * Constructor
     * @param board
     */
    public TreeSearchBoardAnalyserImpl_v13(Board board, ReversiColor maxPlayer) {
        this._board = board;
        this._maxPlayer = maxPlayer;
        initFieldValues();
        initEvaluationWeights();
    }

    private void initEvaluationWeights() {
        _weightMatrix = new int[_board.getDim() * _board.getDim() - 3][1];

        // Here we define the tupels which are then copied to the Matrix
        // -- the tupels are created with a 8x8 field board in mind. bigger or smaller board would simply
        // -- use an according pattern
        int[][] weightList = new int[][]{
/*
                                    m
                                    o
                 s      s    c      b
                 t  s   t    o      i
                 o  t   a    r      l
                 n  o   b    n      i
                 e  n   l    e      t   f
              m  v  e   e    r      y   o
              o  a  d   d    d   p  d   r  f  f
              v  l  i   i    i   a  i   c  r  r
              e  u  f   f    f   s  f   e  e  e
              #  e  f   f    f   s  f   d  e  e
*/
            { 9, 1, 0, 50, 100,  0, 1, 20, 0, 0},
            {19, 1, 0, 50, 100, 20, 1, 20, 0, 0},
            {29, 1, 0, 50, 100, 20, 1, 20, 0, 0},
            {39, 1, 0, 50,  50, 20, 1, 20, 0, 0},
            {49, 1, 0, 50,  50, 20, 1, 20, 0, 0},
            {59, 0, 5, 50,  50, 20, 0, 20, 0, 0},
            {60, 0, 1,  0,   0,  0, 0, 20, 0, 0},

                /* backup
            { 9, 1, 0, 50, 100, 0, 1, 0, 0, 0},
            {19, 1, 0, 50, 100, 0, 1, 0, 0, 0},
            {29, 1, 0, 50, 100, 0, 1, 0, 0, 0},
            {39, 1, 0, 50,  50, 0, 1, 0, 0, 0},
            {49, 1, 0, 50,  50, 0, 0, 0, 0, 0},
            {59, 0, 5, 50,  50, 0, 0, 0, 0, 0},
            {60, 0, 1,  0,   0, 0, 0, 0, 0, 0},
            */
        };

        final int maxMoveNumber = (_board.getDim() * _board.getDim() - 4);
        int last = 0;
        for (int moveNumber = 1; moveNumber <= maxMoveNumber; moveNumber++) {
            float weightMove = weightList[last][0] / 60.0f;
            float curMove = (float) moveNumber / (float) maxMoveNumber;
            if (weightMove < curMove) {
                last++;
            }
            _weightMatrix[moveNumber] = weightList[last];
        }
    }

    /**
     * Analyses the board always as the BLACK player and returns the evaluation value.
     * @param board
     * @return int
     */
    public int analyse(TreeSearchBoard board) {

        // No more moves! Just count the stones.
        if (board.getNextPlayerColor().isNone()) {
            int diff = board.getPiecesDiff(_maxPlayer);
            if (diff>0) {
                return Integer.MAX_VALUE;
            } else if (diff<0) {
                return -Integer.MAX_VALUE;
            } else {
                return 0;
            }
        }

        // The game is not over - analyse the board  
        final int moveNumber = board.getLastMoveNumber();
        final int dim        = board.getDim();

        int value = 0;

        // -- stonediff --
        if (_weightMatrix[moveNumber][2] > 0) {
            value += board.getPiecesDiff(_maxPlayer) * _weightMatrix[moveNumber][2];
        }

        // -- pass --
        if (_weightMatrix[moveNumber][5] > 0 && board.hasPass()) {
            // if maxPlayer==nextPlayer the product would be positiv, otherwise negativ
            value += board.getNextPlayerColor().toInt() *_maxPlayer.toInt() * _weightMatrix[moveNumber][5];
        }

        // -- forced --
        if (_weightMatrix[moveNumber][7] > 0 && board.getMoves().size() < 2) {
            // to be forced mean to only have one move or non at all so it is negativ
            value -= board.getNextPlayerColor().toInt() *_maxPlayer.toInt() * _weightMatrix[moveNumber][7];
        }

        // -- stablediff --
        if (_weightMatrix[moveNumber][3] > 0) {
            value += (board.getStableFieldsApproxDiff(_maxPlayer) * _weightMatrix[moveNumber][3]);
        }

        // -- corner diff --
        if (_weightMatrix[moveNumber][4] > 0) {
            value += board.getCornerDiff(_maxPlayer) * _weightMatrix[moveNumber][4];
        }

        // -- mobillity --
        if (_weightMatrix[moveNumber][6] > 0) {
            // -- Now we do the mobility difference from the perspective of nextPlayer --
            // -- If the next player is maxPlayer everything is ok, if not we must negate the result
            value += board.getMobilityDiff() * _weightMatrix[moveNumber][6];
        }

        // -- we scan the whole board --
        // -- !! Very expensive so we do it only once and
        // -- !! add all evaluations which need to scan the board
        for (int i = 1; i <= dim; i++) {
            for (int j = 1; j <= dim; j++) {
                final ReversiColor color = board.getField(i, j);
                // -- ignore empty fields --
                if (color.isEmpty()) {
                    continue;
                }
                // --- is there a _maxPlayer stone ?? --
                if (color.equals(_maxPlayer)) {
                    // -- stonevalue --
                    if (_weightMatrix[moveNumber][1] > 0) {
                        value += _fieldValues[i - 1][j - 1] * _weightMatrix[moveNumber][1];
                    }
                } else if (color.equals(_maxPlayer.getInverseColor())) {
                    // -- stonevalue --
                    if (_weightMatrix[moveNumber][1] > 0) {
                        value -= _fieldValues[i - 1][j - 1] * _weightMatrix[moveNumber][1];
                    }
                }
            }
        }

        // -- evaluation is done from the view of the player to move next --
        return value;
    }

    /**
     * returns if board is relativly quiet so that we don't have to care
     * about the horizont problem
     * @param board
     * @return true if board is quiet
     */
    public boolean notQuiet(Board board) {
        // -- use fieldValues to determine possible non-quiet moves --
        return Math.abs(_fieldValues[board.getLastMove().getCol() - 1][board.getLastMove().getRow() - 1]) > 3;
    }

    /**
     * sort a list of moves -- the most promising first
     * @param board
     * @param moves
     */
    public void sortMoves(Board board, List<Move> moves) {
        // -- sort list --
        //noinspection unchecked
        Collections.sort(moves, _moveComparator);
    }

    /**
     * returns string representation of class
     */
    @Override
	public String toString() {
        return "Class TreeSearchBoardAnalyserImpl_v12";
    }

    /**
     * initilaize the engine with a table for sorting move
     *
     */
    private void initFieldValues() {

        // -- m(ax) col and row --
        final int m = _board.getDim() - 1;

        _fieldValues = new int[m + 1][m + 1];

        /*
        * on a 8x8 board it looks like this
        *
            20  -3  2  2  2  2 -3  20
            -3 -10 -2 -2 -2 -2 -10 -3
             2  -2  0  0  0  0 -2   2
             2  -2  0  0  0  0 -2   2
             2  -2  0  0  0  0 -2   2
             2  -2  0  0  0  0 -2   2
            -3 -10 -2 -2 -2 -2 -10 -3
            20  -3  2  2  2  2 -3  20
        *
        */

        // -- corners have a value of 20
        _fieldValues[0][0] = 20;
        _fieldValues[0][m] = 20;
        _fieldValues[m][0] = 20;
        _fieldValues[m][m] = 20;

        // -- the X-fields (diagonally next to corners) get -10 --
        _fieldValues[1][1] = -10;
        _fieldValues[1][m - 1] = -10;
        _fieldValues[m - 1][1] = -10;
        _fieldValues[m - 1][m - 1] = -10;

        // -- the C-fields (next to corners) get -3 --
        _fieldValues[0][1] = -3;
        _fieldValues[1][0] = -3;
        _fieldValues[m - 1][0] = -3;
        _fieldValues[m][1] = -3;
        _fieldValues[0][m - 1] = -3;
        _fieldValues[1][m] = -3;
        _fieldValues[m - 1][m] = -3;
        _fieldValues[m][m - 1] = -3;

        // -- edges get a 2 --
        for (int i = 0; i <= m; i += m) {
            for (int j = 2; j < m - 1; j++) {
                _fieldValues[i][j] = 2;
                _fieldValues[j][i] = 2;
            }
        }

        // -- fields next to edges get a -2 --
        for (int i = 1; i < m; i += m - 2) {
            for (int j = 2; j < m - 1; j++) {
                _fieldValues[i][j] = -2;
                _fieldValues[j][i] = -2;
            }
        }

    }

    private class moveComparator implements Comparator {
        public int compare(Object oo1, Object oo2) {
            return _fieldValues[((Move) oo2).getCol() - 1][((Move) oo2).getRow() - 1]
                   - _fieldValues[((Move) oo1).getCol() - 1][((Move) oo1).getRow() - 1];
        }
    }
}
