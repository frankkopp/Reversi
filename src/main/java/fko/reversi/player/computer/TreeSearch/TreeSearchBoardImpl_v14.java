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
 * The TreeSearchBoardImpl extends the BoardImpl class ...
 *
 * Outside this class fields are adressed starting from 1 to board dimension
 * Within this class fields are addressed starting with 0 to board dimension -1
 *
 * This Board is not thread safe!
 *
 */
public class TreeSearchBoardImpl_v14 extends BoardImpl implements TreeSearchBoard {

    // Precomputed powers of 3
    // This is static as we only have to do it once for a given dimension
    // It is not final as the dimension can be different for different boards instances
    private static int[] POW3 = null;

    // Edge indices to speed up stable stone lookups and other lookups
    private int _topEdgeIndex = 0;
    private int _bottomEdgeIndex = 0;
    private int _leftEdgeIndex = 0;
    private int _rightEdgeIndex = 0;

    // stable fields
    private boolean[][] _stableFields;
    private int     _stableFieldsBlack = 0;
    private int     _stableFieldsWhite = 0;

    // flag if the current stable field set and counters are valid
    private boolean _valid = false;

    /**
     * default contructor
     */
    public TreeSearchBoardImpl_v14() {
        super();
        initPOW3();
        _stableFields = new boolean[getDim()][getDim()];
    }

    /**
     * contructor for different board dimensions
     * @param dim
     */
    public TreeSearchBoardImpl_v14(int dim) {
        super(dim);
        initPOW3();
        _stableFields = new boolean[dim][dim];
    }

    /**
     * copy contructor
     * @param old
     */
    public TreeSearchBoardImpl_v14(TreeSearchBoardImpl_v14 old) {
        super(old);
        initPOW3();
        // -- stable fields --
        _stableFields = new boolean[getDim()][getDim()];
        for (int col = 0; col < getDim(); col++) {
            System.arraycopy(old._stableFields[col], 0, _stableFields[col], 0, getDim());
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
    public TreeSearchBoardImpl_v14(Board old) {
        super(old);
        initPOW3();
        // -- stable fields --
        _stableFields = new boolean[getDim()][getDim()];
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
        _valid=false;
        super.makeMove(move);
    }

    /**
     * Returns an approximation of the difference of number of stable fields
     * @param color
     * @return returns the difference of number of stable fields
     */
    public synchronized int getStableFieldsApproxDiff(ReversiColor color) {
        if (!_valid) {
            markStableFields();
            _valid=true;
        }
        return color.toInt() * (_stableFieldsWhite - _stableFieldsBlack);
    }

    /**
     * Marks stable fields on the board. Does not find all possible stable fields.
     * There are certain constellations where this function does not recognize a field as stable.
     *
     */
    private void markStableFields() {

        // are there any stones on the corners?
        // if not there cannot be a stable stone at all
        if (_fields[0][0].isEmpty()
                && _fields[0][getDim()-1].isEmpty()
                && _fields[getDim()-1][0].isEmpty()
                && _fields[getDim()-1][getDim()-1].isEmpty()) {
            return;
        }

        // -- if the corners are not empty there are stable stones --

        // -- left bottom corner --
        if (_fields[0][0].isNotEmpty()) {
            setStableField(0, 0);
            // Check if the lines are full
            // Now start checking the rest of the board starting from
            // the corner
            recursiveStableFieldSearch(0, 0, 1, 1);
        }
        // -- right bottom corner --
        if (_fields[getDim()-1][0].isNotEmpty()) {
            setStableField(getDim()-1, 0);
            // Now start checking the rest of the board starting from
            // the corner
            recursiveStableFieldSearch(getDim()-1, 0, -1, 1);
        }
        // -- left top corner --
        if (_fields[0][getDim()-1].isNotEmpty()) {
            setStableField(0, getDim()-1);
            // Now start checking the rest of the board starting from
            // the corner
            recursiveStableFieldSearch(0, getDim()-1, 1, -1);
        }
        // -- right top corner --
        if (_fields[getDim()-1][getDim()-1].isNotEmpty()) {
            setStableField(getDim() - 1, getDim() - 1);
            // Now start checking the rest of the board starting from
            // the corner
            recursiveStableFieldSearch(getDim()-1, getDim()-1, -1, -1);
        }

    }

    /**
     * Marks all stable fields rekursivly from a given field
     * into given directions.
     *
     * @param col
     * @param row
     * @param colDir
     * @param rowDir
     */
    private void recursiveStableFieldSearch(int col, int row, int colDir, int rowDir) {

        // -- the field cannot be empty ---
        if (_fields[col][row].isEmpty()) {
            return;
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
            return;
        }

        // -- nothing from above so we must do the check on this field and
        // -- if the field is stable we continue recursivly
        if (checkStableField(col, row, colDir, rowDir)) {
            setStableField(col, row);
            if (isWithinBoard(col + colDir, row)) {
                recursiveStableFieldSearch(col + colDir, row, colDir, rowDir);
            }
            if (isWithinBoard(col, row + rowDir)) {
                recursiveStableFieldSearch(col, row + rowDir, colDir, rowDir);
            }
            if (isWithinBoard(col + colDir, row + rowDir)) {
                recursiveStableFieldSearch(col + colDir, row + rowDir, colDir, rowDir);
            }
        }
    }

    /**
     * Marks a field as stable and counts the _black and white stones
     * @param col
     * @param row
     */
    private void setStableField(int col, int row) {
        if (!_stableFields[col][row]) {
            if      (_fields[col][row].isBlack()) {
                _stableFieldsBlack++;
            } else if (_fields[col][row].isWhite()) {
                _stableFieldsWhite++;
            }
            _stableFields[col][row] = true;
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
        if (_stableFields[col][row]) {
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
        // --- either have an edge or an stable stone of same color as neighbour
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
            } else if (_stableFields[newCol][newRow] && _fields[newCol][newRow] == _fields[col][row]) {
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
            // -- right buttom corner
        } else if (col == getDim() - 1 && row == 0) {
            updateRightBottomCornerIndex();
            // -- left top corner
        } else if (col == 0 && row == getDim() - 1) {
            updateLeftTopCornerIndex();
            // -- right top corner
        } else if (col == getDim() - 1 && row == getDim() - 1) {
            updateRightTopCornerIndex();
        }
    }

    private void updateLeftEdgeIndex() {
        _leftEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            leftColumn(i);
        }
    }

    private void updateRightEdgeIndex() {
        _rightEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            rightColumn(i);
        }
    }

    private void updateBottomEdgeIndex() {
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- row bottom --
            rowBottom(i);
        }
    }

    private void updateTopEdgeIndex() {
        _topEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- row top --
            rowTop(i);
        }
    }

    private void updateLeftBottomCornerIndex() {
        _leftEdgeIndex = 0;
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col left --
            leftColumn(i);
            // -- row bottom --
            rowBottom(i);
        }
    }

    private void updateRightBottomCornerIndex() {
        _rightEdgeIndex = 0;
        _bottomEdgeIndex = 0;
        for (int i = 0; i < getDim(); i++) {
            // -- col right --
            rightColumn(i);
            // -- row bottom
            rowBottom(i);
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

    private void rowTop(int i) {
        int colorValue;
        if      (_fields[i][getDim()-1].isBlack()) {
            colorValue = 1;
        } else if (_fields[i][getDim()-1].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _topEdgeIndex += POW3[i] * colorValue;
    }

    private void rowBottom(int i) {
        int colorValue;
        if      (_fields[i][0].isBlack()) {
            colorValue = 1;
        } else if (_fields[i][0].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _bottomEdgeIndex += POW3[i] * colorValue;
    }

    private void leftColumn(int i) {
        int colorValue;
        if      (_fields[0][i].isBlack()) {
            colorValue = 1;
        } else if (_fields[0][i].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _leftEdgeIndex += POW3[i] * colorValue;
    }

    private void rightColumn(int i) {
        int colorValue;
        if      (_fields[getDim()-1][i].isBlack()) {
            colorValue = 1;
        } else if (_fields[getDim()-1][i].isWhite()) {
            colorValue = 2;
        } else {
            colorValue = 0;
        }
        _rightEdgeIndex += POW3[i] * colorValue;
    }

    /**
     * inits the POW3 array
     */
    private void initPOW3() {
        // -- init power of 3 (saving a lot of time) --
        if (POW3 == null || POW3.length <= getDim()) {
            synchronized (this.getClass()) {
                POW3 = new int[getDim() + 1];
                for (int i = 0; i <= getDim(); i++) {
                    //noinspection NumericCastThatLosesPrecision
                    POW3[i] = (int) StrictMath.pow(3.0, i);
                }
            }
        }
    }

}
