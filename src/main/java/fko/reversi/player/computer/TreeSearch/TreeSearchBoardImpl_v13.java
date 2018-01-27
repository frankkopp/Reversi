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

import fko.reversi.game.*;

/**
 * The HashableBoard_v10 extends the BoardImpl class with a stableFields array which is updated
 *
 * Outside this class fields are adressed starting from 1 to board dimension
 * Within this class fields are addressed starting with 0 to board dimension -1
 *
 * This Board is not thread safe!
 *
 */
public class TreeSearchBoardImpl_v13 extends BoardImpl implements TreeSearchBoard {

    /**
     * Precomputed powers of 3
     * This is static as we only have to do it once for a given dimension
     * It is not final as the dimension can be different for different boards instances
     */
    private static int[] POW3 = null;

    /**
     * Edge indices to to speed up stable stone lookups and other lookups
     */
    private int _topEdgeIndex = 0;
    private int _bottomEdgeIndex = 0;
    private int _leftEdgeIndex = 0;
    private int _rightEdgeIndex = 0;

    /**
     * stable fields
     */
    private boolean _stableFields[][];
    private int     _stableFieldsBlack = 0;
    private int     _stableFieldsWhite = 0;

    /**
     * default contructor
     */
    public TreeSearchBoardImpl_v13() {
        super();
        initPOW3();
        _stableFields = new boolean[getDim()][getDim()];
    }

    /**
     * contructor for different board dimensions
     * @param dim
     */
    public TreeSearchBoardImpl_v13(int dim) {
        super(dim);
        initPOW3();
        _stableFields = new boolean[dim][dim];
    }

    /**
     * copy contructor
     * @param old
     */
    public TreeSearchBoardImpl_v13(TreeSearchBoardImpl_v13 old) {
        super(old);
        initPOW3();

        // -- stable fields --
        _stableFields = new boolean[getDim()][getDim()];
        for (int col = 0; col < getDim(); col++) {
            System.arraycopy(old._stableFields[col], 0, this._stableFields[col], 0, getDim());
        }
        _stableFieldsBlack = old._stableFieldsBlack;
        _stableFieldsWhite = old._stableFieldsWhite;
        // -- copy indices --
        this._topEdgeIndex = old._topEdgeIndex;
        this._bottomEdgeIndex = old._bottomEdgeIndex;
        this._leftEdgeIndex = old._leftEdgeIndex;
        this._rightEdgeIndex = old._rightEdgeIndex;
    }

    /**
     * copy contructor for board
     * @param old
     */
    public TreeSearchBoardImpl_v13(Board old) {
        super(old);
        initPOW3();

        // -- stable fields --
        _stableFields = new boolean[getDim()][getDim()];
        markAllStableFields();

        // -- update edge index --
        for (int i = 0; i < getDim(); i++) {
            for (int j = 0; j < getDim(); j++) {
                updateEdgeIndices(i, j);
            }
        }
    }

    /**
     * makes move on board according to rules
     * @param move
     */
    @Override
	public synchronized void makeMove(Move move) throws IllegalMoveException {
        super.makeMove(move);
        // --update edge indices --
        updateEdgeIndices(move.getCol()-1, move.getRow()-1);
    }

    /**
     * updates an index of an edge based on the ternary value of an edge
     * @param col
     * @param row
     */
    private void updateEdgeIndices(int col, int row) {

        // -- check if edge
        if (!(col == 0 || col == getDim() - 1 || row == 0 || row == getDim() - 1)) {
            return;
        }

        // -- left edge not corner --
        if (col == 0 && row != 0 && row != getDim() - 1) {
            updateLeftEdgeIndex();
            // -- right edge not corner --
        } else if (col == getDim() - 1 && row != 0 && row != getDim() - 1) {
            updateRightEdgeIndex();
            // -- bottom edge not corner --
        } else if (row == 0 && col != 0 && col != getDim() - 1) {
            updateBottomEdgeIndex();
            // -- top edge not corner --
        } else if (row == getDim() - 1 && col != 0 && col != getDim() - 1) {
            updateTopEdgeIndex();
            // -- left bottom corner
        } else if (col == 0 && row == 0) {
            updateLeftBottomCornerIndex();
            // -- right top corner
        } else if (col == getDim() - 1 && row == getDim() - 1) {
            updateRightTopCornerIndex();
            // -- left top corner
        } else if (col == 0 && row == getDim() - 1) {
            updateLeftTopCornerIndex();
            // -- right buttom corner
        } else if (col == getDim() - 1 && row == 0) {
            updateRightBottomCornerIndex();
        }
    }

    private void updateRightBottomCornerIndex() {
        int colorValue;
        _rightEdgeIndex = 0;
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {

            // -- col right --
            rightColumn(i);

            // -- row bottom
            if (_fields[i][0].isBlack()) {
                colorValue = 1;
            } else if (_fields[i][0].isWhite()) {
                colorValue = 2;
            } else {
                colorValue = 0;
            }
            _bottomEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
        }
    }

    private void updateLeftTopCornerIndex() {
        _leftEdgeIndex = 0;
        _topEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col left --
            leftColumn(i);
            // -- row top --
            rowTop(i);
        }
    }

    private void leftColumn(int i) {
        int colorValue;
        if (_fields[0][i].isBlack()) {
            colorValue = 1;
        } else if (_fields[0][i].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _leftEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
    }

    private void rowTop(int i) {
        int colorValue;
        if (_fields[i][getDim()-1].isBlack()) {
            colorValue = 1;
        } else if (_fields[i][getDim()-1].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _topEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
    }

    private void updateRightTopCornerIndex() {
        _rightEdgeIndex = 0;
        _topEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right
            rightColumn(i);
            // -- row top --
            rowTop(i);
        }
    }

    private void updateLeftBottomCornerIndex() {
        int colorValue;
        _leftEdgeIndex = 0;
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {

            // -- col left --
            leftColumn(i);

            // -- row bottom --
            if (_fields[i][0].isBlack()) {
                colorValue = 1;
            } else if (_fields[i][0].isWhite()) {
                colorValue = 2;
            } else {
                colorValue = 0;
            }
            _bottomEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
        }
    }

    private void updateTopEdgeIndex() {
        _topEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            rowTop(i);
        }
    }

    private void updateBottomEdgeIndex() {
        int colorValue;
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            if (_fields[i][0].isBlack()) {
                colorValue = 1;
            } else if (_fields[i][0].isWhite()) {
                colorValue = 2;
            } else {
                colorValue = 0;
            }
            _bottomEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
        }
    }

    private void updateRightEdgeIndex() {
        _rightEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            rightColumn(i);
        }
    }

    private void rightColumn(int i) {
        int colorValue;
        if (_fields[getDim() - 1][i].isBlack()) {
            colorValue = 1;
        } else if (_fields[getDim() - 1][i].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _rightEdgeIndex += TreeSearchBoardImpl_v13.POW3[i] * colorValue;
    }

    private void updateLeftEdgeIndex() {
        _leftEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            leftColumn(i);
        }
    }

    /**
     * marks all stable fields on the board
     * highly optimized for performance
     * it does not scan all the fields. It alwaays starts from a stable corner field
     */
    private void markAllStableFields() {

        // -- reset counters --
        _stableFieldsBlack = 0;
        _stableFieldsWhite = 0;

        // are there any stones on the corners?
        // if not there cannot be a stable stone at all
        if (!(_fields[0][0] != ReversiColor.EMPTY
            || _fields[0][getDim() - 1] != ReversiColor.EMPTY
            || _fields[getDim() - 1][0] != ReversiColor.EMPTY
            || _fields[getDim() - 1][getDim() - 1] != ReversiColor.EMPTY)) {
            return;
        }

        // -- if the corners are not empty there are stable stones --

        // -- left bottom corner --
        if (_fields[0][0] != ReversiColor.EMPTY) {
            this._stableFields[0][0] = true;
            this.incStableFieldsCounters(_fields[0][0]);
            // -- starting from this corner mark all stable edge fields
            int[] result = markStableEdgeFields(0, 0, 1, 1);
            // -- now startGame checking the rest of the board starting from
            // -- the field diagonally next to the corner
            recursiveStableFieldSearch(1, 1, 1, 1, result[0], result[1]);
        }
        // -- right bottom corner --
        if (_fields[getDim() - 1][0] != ReversiColor.EMPTY) {
            this._stableFields[getDim() - 1][0] = true;
            this.incStableFieldsCounters(_fields[getDim() - 1][0]);
            // -- starting from this corner mark all stable edge fields
            int[] result = markStableEdgeFields(getDim() - 1, 0, -1, 1);
            // -- now startGame checking the rest of the board starting from
            // -- the field diagonally next to the corner
            recursiveStableFieldSearch(getDim() - 2, 1, -1, 1, result[0], result[1]);
        }
        // -- left top corner --
        if (_fields[0][getDim() - 1] != ReversiColor.EMPTY) {
            this._stableFields[0][getDim() - 1] = true;
            this.incStableFieldsCounters(_fields[0][getDim() - 1]);
            // -- starting from this corner mark all stable edge fields
            int[] result = markStableEdgeFields(0, getDim() - 1, 1, -1);
            // -- now startGame checking the rest of the board starting from
            // -- the field diagonally next to the corner
            recursiveStableFieldSearch(1, getDim() - 2, 1, -1, result[0], result[1]);
        }
        // -- right top corner --
        if (_fields[getDim() - 1][getDim() - 1] != ReversiColor.EMPTY) {
            this._stableFields[getDim() - 1][getDim() - 1] = true;
            this.incStableFieldsCounters(_fields[getDim() - 1][getDim() - 1]);
            // -- starting from this corner mark all stable edge fields
            int[] result = markStableEdgeFields(getDim() - 1, getDim() - 1, -1, -1);
            // -- now start checking the rest of the board starting from
            // -- the field diagonally next to the corner
            recursiveStableFieldSearch(getDim() - 2, getDim() - 2, -1, -1, result[0], result[1]);
        }

    }

    /**
     * returns the difference of number of stable fields
     * @param color
     * @return returns the difference of number of stable fields
     */
    public int getStableFieldsApproxDiff(ReversiColor color) {
        markAllStableFields();
        return color.toInt() * (_stableFieldsWhite - _stableFieldsBlack);
    }

    /**
     * If a corner is stable we can crawl along the edges until we find a field
     * which does not have the corner's color. All others are stable, too.
     * We do not check the corner itself and the opposite corner (only < dim-1).
     * We update the stableFields counter only from one direction (upwards and left to right)
     * We return the max column or row were we found stables stones at the edge
     *
     * @param col
     * @param row
     * @param colInc
     * @param rowInc
     * @return int []
     */
    private int[] markStableEdgeFields(int col, int row, int colInc, int rowInc) {

        int newCol = col;
        int newRow = row;

        // -- row ---
        // -- if color of neighbour field is the same then it must be stable
        while ((newRow + rowInc < getDim() - 1 && newRow + rowInc > 0)
            && _fields[col][newRow] == _fields[col][newRow += rowInc]) {

            // if a stone in this edge line is already stable
            // the rest in that direction of the same color must be stable too
            this._stableFields[col][newRow] = true;

            // -- up --
            // -- if the edge is full with one color we only count the
            // -- the stable fields in one direction (down)
            if (rowInc == 1) {
                // -- left edge full with one color -> then do not count --
                if (col == 0 && _leftEdgeIndex % 3280 != 0) {
                    this.incStableFieldsCounters(_fields[col][newRow]);
                    // -- right edge full with one color? --
                } else if (col == getDim() - 1 && _rightEdgeIndex % 3280 != 0) {
                    this.incStableFieldsCounters(_fields[col][newRow]);
                }
            } else {
                this.incStableFieldsCounters(_fields[col][newRow]);
            }
        }

        // -- col ---
        // -- if color of neighbour field is the same then it must be stable
        while ((newCol + colInc < getDim() - 1 && newCol + colInc > 0)
            && _fields[newCol][row] == _fields[newCol += colInc][row]) {
            // if a stone in this edge line is already stable
            // the rest in that direction must be stable too
            //if (this.stableFields[newCol][row]) break;
            this._stableFields[newCol][row] = true;

            // -- right --
            // -- if the edge is full with one color we only count the
            // -- the stable fields in one direction (left)
            if (colInc == 1) {
                // -- bottom edge full with one color? --
                if (row == 0 && _bottomEdgeIndex % 3280 != 0) {
                    this.incStableFieldsCounters(_fields[newCol][row]);
                    // -- top edge full with one color? --
                } else if (row == getDim() - 1 && _topEdgeIndex % 3280 != 0) {
                    this.incStableFieldsCounters(_fields[newCol][row]);
                }
            } else {
                this.incStableFieldsCounters(_fields[newCol][row]);
            }
        }

        // -- return the maximum x,y coordinate where we found a stable stone
        return new int[]{newCol - colInc, newRow - rowInc};

    }

    /**
     * Marks all stable fields rekursivly from a given field
     * into given directions.
     * The first call can use the information from markStableEdgeFields(...)
     *
     * @param col
     * @param row
     * @param colDir
     * @param rowDir
     * @param maxCol
     * @param maxRow
     */
    private boolean recursiveStableFieldSearch(int col, int row, int colDir, int rowDir, int maxCol, int maxRow) {

        // -- the field cannot be empty ---
        if (_fields[col][row].isEmpty()) {
            return false;
        }

        // -- the field should have the same color as the starting corner
        if (((colDir == 1 && rowDir == 1)
            && (_fields[col][row] != _fields[0][0]))
            || ((colDir == -1 && rowDir == 1)
            && (_fields[col][row] != _fields[getDim() - 1][0]))
            || ((colDir == 1 && rowDir == -1)
            && (_fields[col][row] != _fields[0][getDim() - 1]))
            || ((colDir == -1 && rowDir == -1)
            && (_fields[col][row] != _fields[getDim() - 1][getDim() - 1]))
        ) {
            return false;
        }

        // -- nothing from above so we must do the check on this field and
        // -- if the field is stable we continue recursivly
        if ((!(maxCol == 0 || maxRow == 0) && (maxCol * colDir) > (col * colDir) && (maxRow * rowDir) > (row * rowDir)) // -- if we have the maxCol and maxRow value we can use them --
            || checkStableField(col, row, colDir, rowDir)) {

            this._stableFields[col][row] = true;
            this.incStableFieldsCounters(_fields[col][row]);
            if (isWithinBoard(col + colDir, row)) {
                recursiveStableFieldSearch(col + colDir, row, colDir, rowDir, 0, 0);
            }
            if (isWithinBoard(col, row + rowDir)) {
                recursiveStableFieldSearch(col, row + rowDir, colDir, rowDir, 0, 0);
            }
            if (isWithinBoard(col + colDir, row + rowDir)) {
                recursiveStableFieldSearch(col + colDir, row + rowDir, colDir, rowDir, 0, 0);
            }
            return true;
        } else {
            return false;
        }

    }

    /**
     * checks if a field is stable
     * @param col
     * @param row
     * @return Boolean
     */
    private boolean checkStableField(int col, int row, int colDir, int rowDir) {

        // -- if already marked as stable
        if (this._stableFields[col][row]) {
            return true;
        }

        // 1 --> bottom right corner (-1,1)
        // 3 --> bottom left corner (1,1)
        // 5 --> top left corner (1,-1)
        // 7 --> top right corner (-1,-1)
        int startCornerLookupIndex = -1;
        if (colDir == 1 && rowDir == 1) {
            startCornerLookupIndex = 3; // bottom left corner
        } else if (colDir == -1 && rowDir == 1) {
            startCornerLookupIndex = 1; // bottom right corner
        } else if (colDir == 1 && rowDir == -1) {
            startCornerLookupIndex = 5; // top left corner
        } else if (colDir == -1 && rowDir == -1) {
            startCornerLookupIndex = 7; // top right corner
        }

        // --- check if a minimum of 4 directions in sequence
        // --- either have an edge or an stable stone as neighbour
        int counter = 0;
        boolean[] direction_field = new boolean[8];
        // --- all 8 directions have to be checked
        for (int i = 0; i < 8; i++) {
            int colInc = getClockwiseLookup((startCornerLookupIndex + i) % 8,0);
            int rowInc = getClockwiseLookup((startCornerLookupIndex + i) % 8,1);

            int newCol = col + colInc;
            int newRow = row + rowInc;

            // -- outside board? --
            if (!isWithinBoard(newCol, newRow)) {
                counter++;
                direction_field[i] = true;
                continue;
                // -- check stable neighbour of same color directly --
            } else if (this._stableFields[newCol][newRow] && _fields[newCol][newRow] == _fields[col][row]) {
                counter++;
                direction_field[i] = true;
                continue;
            } else {
                counter = 0;
                direction_field[i] = false;
            }

        }
        if (counter >= 4) {
            return true;
        }

        // -- as there must be 4 in sequence we have to look over the array limit ---
        counter = 0;
        for (int i = 0; i < 11; i++) {
            if (direction_field[(i % 8)]) {
                counter++;
            } else {
                counter = 0;
            }
            if (counter >= 4) {
                return true;
            }
        }
        return false;

    }

    /**
     * update counters for stableFields
     * @param color
     */
    private void incStableFieldsCounters(ReversiColor color) {
        if      (color.isBlack()) {
            _stableFieldsBlack++;
        } else if (color.isWhite()) {
            _stableFieldsWhite++;
        }
    }


    /**
     * inits the POW3 array
     */
    private void initPOW3() {
        // -- init power of 3 (saving a lot of time) --
        if (TreeSearchBoardImpl_v13.POW3 == null || TreeSearchBoardImpl_v13.POW3.length <= getDim()) {
            synchronized (this.getClass()) {
                TreeSearchBoardImpl_v13.POW3 = new int[getDim() + 1];
                for (int i = 0; i <= getDim(); i++) {
                    TreeSearchBoardImpl_v13.POW3[i] = (int) StrictMath.pow(3.0, i);
                }
            }
        }
    }

}
