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
import fko.reversi.game.Move;
import fko.reversi.game.ReversiColor;
import fko.reversi.game.BoardImpl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Analyses a given board and returns an heuristic value for that board.
 */
public class TreeSearchBoardAnalyserImpl_v14 implements TreeSearchBoardAnalyser {

    private final Board _board;

    private final ReversiColor _maxPlayer;

    // -- contains value for each field on the board
    private int[][] _fieldValues;

    // -- contains a tupel for the weighting of different evaluatione regarding the current move number
    private int[][] _weightMatrix;

    // -- inner class moveComparator is used to sort moves --
    private final Comparator<Move> _moveComparator = new TreeSearchBoardAnalyserImpl_v14.moveComparator();

    //DEBUG
    public final static boolean SHOW_EVAL_FOR_DEBUG = false;

    /**
     * Constructor
     * @param board     the board
     * @param maxPlayer my player
     */
    public TreeSearchBoardAnalyserImpl_v14(Board board, ReversiColor maxPlayer) {
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
                                              x   c   m
                                              |   |   o
                  s       s   c               s   s   b
                  t   s   t   o               q   q   i  l
                  o   t   a   r       m       u   u   l  i
                  n   o   b   n       o       a   a   i  b
                  e   n   l   e       b   f   r   r   t  e
              m   v   e   e   r       i   o   e   e   y  r
              o   a   d   d   d   p   l   r   d   d   d  t  f  f  f  f  f  f
              v   l   i   i   i   a   i   c   i   i   i  i  r  r  r  r  r  r
              e   u   f   f   f   s   t   e   f   f   f  e  e  e  e  e  e  e
              #   e   f   f   f   s   y   d   f   f   f  s  e  e  e  e  e  e
*/
            // The value 10 should be a rough estimation of one captured field at the end of the game

            // Opening/Early game
            { 5, 10,  0, 10, 150, 10,  0, 10,  0,  0, 20, 20, 0, 0, 0, 0, 0, 0},
            {10, 10,  0, 10, 150, 10,  0, 10,  0,  0, 20, 20, 0, 0, 0, 0, 0, 0},
            {15, 10,  0, 10, 150, 10,  0, 10,  0,  0, 20, 20, 0, 0, 0, 0, 0, 0},
            // Middle Game
            {20, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 20, 0, 0, 0, 0, 0, 0},
            {25, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 20, 0, 0, 0, 0, 0, 0},
            {30, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 15, 0, 0, 0, 0, 0, 0},
            {35, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 10, 0, 0, 0, 0, 0, 0},
            {40, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 10, 0, 0, 0, 0, 0, 0},
            {45, 10,  0, 10, 150, 10,  0, 10,  0,  0, 10, 10, 0, 0, 0, 0, 0, 0},
            // End Game
            {50, 10,  0, 10, 150, 20,  0, 20,  0,  0,  0,  5, 0, 0, 0, 0, 0, 0},
            {55, 10,  2, 10, 150, 20,  0, 20,  0,  0,  0,  5, 0, 0, 0, 0, 0, 0},
            {59, 10,  5, 10, 150, 20,  0, 20,  0,  0,  0,  5, 0, 0, 0, 0, 0, 0},
            {60,  0, 10,  0,   0,  0,  0,  0,  0,  0,  0,  0, 0, 0, 0, 0, 0, 0},

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

		if (SHOW_EVAL_FOR_DEBUG) {
		    System.out.println("MaxPlayer="+_maxPlayer);
		    System.out.println("Current Board - LastMove: "+board.getLastMoveNumber()+ ". " +board.getLastMove()
		            +"\n NextMove: "+board.getNextMoveNumber()
		            +" Next: "+board.getNextPlayerColor()
		            +" Last: "+board.getLastPlayerColor());
		    System.out.println(((BoardImpl)board).drawBoard());
		}

        // No more moves! Just count the stones.
        if (board.getNextPlayerColor().isNone()) {
            int diff = board.getPiecesDiff(_maxPlayer);
            
		if (SHOW_EVAL_FOR_DEBUG) {
		    System.out.println("Eval: No more moves: "+diff+" (will be set to +-MAX_VALUE");
		}
            
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

        if (_weightMatrix[moveNumber][1] != 0 // field value
                || _weightMatrix[moveNumber][8] != 0 // x-squares
                || _weightMatrix[moveNumber][9] != 0) { // c- squares
            // Set the fieldValues according to the board
            setFieldValues(board);
            /*for (int y=board.getDim()-1;y>=0;y--) {
                for (int x=0;x<board.getDim();x++) {
                    System.out.print(" "+_fieldValues[x][y]);
                }
                System.out.println();
            }*/
        }

        // -- stonediff --
        if (_weightMatrix[moveNumber][2] != 0) {
            value += board.getPiecesDiff(_maxPlayer) * _weightMatrix[moveNumber][2];
            
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: StoneDiff: "+board.getPiecesDiff(_maxPlayer) * _weightMatrix[moveNumber][2]);
			    System.out.println(" Eval: "+value);
			}
        
        }
        // -- pass --
        if (_weightMatrix[moveNumber][5] > 0 && board.hasPass()) {
            // if maxPlayer==nextPlayer the product would be positiv, otherwise negativ
            value +=_maxPlayer.toInt()* board.getNextPlayerColor().toInt()  * _weightMatrix[moveNumber][5];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: Pass: "+board.getNextPlayerColor().toInt() *_maxPlayer.toInt() * _weightMatrix[moveNumber][5]);
			    System.out.println(" Eval: "+value);
			}
        }

        // -- forced --
        if (_weightMatrix[moveNumber][7] > 0 && board.getMoves().size() < 2) {
            // to be forced mean to only have one move or non at all so it is negativ
            value -= board.getNextPlayerColor().toInt() *_maxPlayer.toInt() * _weightMatrix[moveNumber][7];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: Forced: "+(-board.getNextPlayerColor().toInt() *_maxPlayer.toInt() * _weightMatrix[moveNumber][7]));
			    System.out.println(" Eval: "+value);
			}
        }

        // -- stablediff --
        if (_weightMatrix[moveNumber][3] > 0) {
            value += (board.getStableFieldsApproxDiff(_maxPlayer) * _weightMatrix[moveNumber][3]);
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: StableDiff: "+(board.getStableFieldsApproxDiff(_maxPlayer) * _weightMatrix[moveNumber][3]));
			    System.out.println(" Eval: "+value);
			}
        }

        // -- corner diff --
        if (_weightMatrix[moveNumber][4] > 0) {
            value += board.getCornerDiff(_maxPlayer) * _weightMatrix[moveNumber][4];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: CornerDiff: "+board.getCornerDiff(_maxPlayer) * _weightMatrix[moveNumber][4]);
			    System.out.println(" Eval: "+value);
			}
        }

        // -- mobillity (for opponent) --
        if (_weightMatrix[moveNumber][6] > 0) {
            value += _maxPlayer.toInt() * board.getNextPlayerColor().toInt() * board.getMoves().size() * _weightMatrix[moveNumber][6];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: Mobility: "+ _maxPlayer.toInt() * board.getNextPlayerColor().toInt() * board.getMoves().size() * _weightMatrix[moveNumber][6]);
			    System.out.println(" Eval: "+value);
			}
        }

        // -- mobility diff (is always from the point of view of the next player
        if (_weightMatrix[moveNumber][10] != 0) {
            value += _maxPlayer.toInt() * board.getNextPlayerColor().toInt() *  board.getMobilityDiff() * _weightMatrix[moveNumber][10];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: Mobility-Diff: "+_maxPlayer.toInt() * board.getNextPlayerColor().toInt() *  board.getMobilityDiff() * _weightMatrix[moveNumber][10]);
			    System.out.println(" Eval: "+value);
			}
        }

        // -- x-square diff --
        if (_weightMatrix[moveNumber][8] != 0) {
            value -= board.getXsquaresDiff(_maxPlayer) * _weightMatrix[moveNumber][8];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: X-Square: "+(-board.getXsquaresDiff(_maxPlayer) * _weightMatrix[moveNumber][8]));
			    System.out.println(" Eval: "+value);
			}
        }

        // -- c-square diff --
        if (_weightMatrix[moveNumber][9] != 0) {
            value -= board.getCsquaresDiff(_maxPlayer) * _weightMatrix[moveNumber][9];
			if (SHOW_EVAL_FOR_DEBUG) {
			    System.out.print("Eval: C-Square: "+(-board.getCsquaresDiff(_maxPlayer) * _weightMatrix[moveNumber][9]));
			    System.out.println(" Eval: "+value);
			}
        }

        int fieldValues=0;
        int liberties=0;

        // -- we scan the whole board --
        // -- !! Very expensive so we do it only once and
        // -- !! add all evaluations which need to scan the board
        for (int x = 1; x <= dim; x++) {
            for (int y = 1; y <= dim; y++) {
                final ReversiColor color = board.getField(x, y);
                // -- ignore empty fields --
                if (color.isEmpty()) {
                    continue;
                }
                // --- is there a _maxPlayer stone ?? --
                if (color.equals(_maxPlayer)) {
                    // -- stonevalue --
                    if (_weightMatrix[moveNumber][1] != 0) {
                        fieldValues += _fieldValues[x - 1][y - 1];
                    }
                    // -- liberties (liberties around my stones are bad) --
                    if (_weightMatrix[moveNumber][11] != 0) {
                        liberties -= board.getLiberties(x, y);
                    }
                } else if (color.equals(_maxPlayer.getInverseColor())) {
                    // -- stonevalue --
                    if (_weightMatrix[moveNumber][1] != 0) {
                        fieldValues -= _fieldValues[x - 1][y - 1];
                    }
                    // -- liberties (liberties around the opponents stones are good
                    if (_weightMatrix[moveNumber][11] != 0) {
                        liberties += board.getLiberties(x, y);
                    }
                }
            }
        }

        // field values
        if (_weightMatrix[moveNumber][1] != 0) {
            value += fieldValues * _weightMatrix[moveNumber][1];
			if (SHOW_EVAL_FOR_DEBUG) {
			      System.out.print("Eval: Field Values: "+fieldValues*_weightMatrix[moveNumber][1]);
			      System.out.println(" Eval: "+value);
			}
        }

        // liberties
        if (_weightMatrix[moveNumber][11] != 0) {
            value += liberties * _weightMatrix[moveNumber][11];
			if (SHOW_EVAL_FOR_DEBUG) {
			        System.out.print("Eval: Liberties: "+liberties*_weightMatrix[moveNumber][11]);
			        System.out.println(" Eval: "+value);
			}
        }

		if (SHOW_EVAL_FOR_DEBUG) {
		    System.out.println("Final Eval: " + value + " \n");
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
        setFieldValues(board);
        // -- use forced moves, fieldValues to determine possible non-quiet moves --
        return Math.abs(_fieldValues[board.getLastMove().getCol() - 1][board.getLastMove().getRow() - 1]) >= 4
                || board.hasPass() // pass
                || board.getMoves().size() < 2; // forced move

    }

    /**
     * sort a list of moves -- the most promising first
     * @param board
     * @param moves
     */
    public void sortMoves(Board board, List<Move> moves) {
        if (board==null) {
            throw new NullPointerException("Board must not be null");
        }
        if (moves==null) {
            throw new NullPointerException("List of moves must not be null");
        }
        // If we have a board we can adapt the field values according to the situation
        setFieldValues(board);
        // -- sort list --
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
     * Sets the field values according to the current board
     * @param board
     */
    private void setFieldValues(Board board) {
        // reset the values
        initFieldValues();

        // -- m(ax) col and row --
        final int m = board.getDim() - 1;

        // If we have any corners we change the values for that corner region
        // left bottom corner
        if (board.getField(1,1).equals(_maxPlayer)) { // my corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[1][1] = 4;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[0][1] = 5;
            _fieldValues[1][0] = 5;
        } else if(board.getField(1,1).equals(_maxPlayer.getInverseColor())) { // opponents corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[1][1] = -1;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[0][1] = -1;
            _fieldValues[1][0] = -1;
        }
        // right bottom corner
        if (board.getField(m+1,1).equals(_maxPlayer)) { // my corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[m-1][1] = 4;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[m-1][0] = 5;
            _fieldValues[m][1]   = 5;
        } else if(board.getField(1,1).equals(_maxPlayer.getInverseColor())) { // opponents corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[m-1][1] = -1;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[m-1][0] = -1;
            _fieldValues[m][1]   = -1;
        }
        // left top corner
        if (board.getField(1,m+1).equals(_maxPlayer)) { // my corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[1][m-1] = 4;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[0][m-1] = 5;
            _fieldValues[1][m]   = 5;
        } else if(board.getField(1,1).equals(_maxPlayer.getInverseColor())) { // opponents corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[1][m-1] = -1;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[0][m-1] = -1;
            _fieldValues[1][m]   = -1;
        }
        // right top corner
        if (board.getField(m+1,m+1).equals(_maxPlayer)) { // my corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[m-1][m-1] = 4;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[m-1][m] = 5;
            _fieldValues[m][m-1] = 5;
        } else if(board.getField(1,1).equals(_maxPlayer.getInverseColor())) { // opponents corner
            // -- the X-fields (diagonally next to corners) get -10 --
            _fieldValues[m-1][m-1] = -1;
            // -- the C-fields (next to corners) get -3 --
            _fieldValues[m-1][m]  = -1;
            _fieldValues[m][m-1]  = -1;
        }

    }

    /**
     * initilaize the engine with a table for sorting move
     *
     */
    private void initFieldValues() {

        // For boards other the 8 we do a different initialization
        if (_board.getDim() == 8) {
            _fieldValues = new int[8][8];
            _fieldValues[0] = new int[] {+20, -4, +2, -2, -2, +2, -4,+20};
            _fieldValues[1] = new int[] { -4, -5, +1,  0,  0, +1, -5, -4};
            _fieldValues[2] = new int[] { +2, +1, +2, +1, +1, +2, +1, +2};
            _fieldValues[3] = new int[] { -2,  0, +1, +1, +1, +1,  0, -2};
            _fieldValues[4] = new int[] { -2,  0, +1, +1, +1, +1,  0, -2};
            _fieldValues[5] = new int[] { +2, +1, +2, +1, +1, +2, +1, +2};
            _fieldValues[6] = new int[] { -4, -5, +1,  0,  0, +1, -5, -4};
            _fieldValues[7] = new int[] {+20, -4, +2, -2, -2, +2, -4,+20};
        } else {
            initGeneralizedFieldValues();
        }
    }

    private void initGeneralizedFieldValues() {
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
        _fieldValues[m-1][0] = -3;
        _fieldValues[m][1] = -3;
        _fieldValues[0][m-1] = -3;
        _fieldValues[1][m] = -3;
        _fieldValues[m-1][m] = -3;
        _fieldValues[m][m-1] = -3;

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

    private class moveComparator implements Comparator<Move> {
        public int compare(Move oo1, Move oo2) {
            return _fieldValues[((Move) oo2).getCol() - 1][((Move) oo2).getRow() - 1]
                   - _fieldValues[((Move) oo1).getCol() - 1][((Move) oo1).getRow() - 1];
        }
    }
}
