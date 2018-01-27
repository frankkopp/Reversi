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

package fko.reversi.ui.ReversiGUI;

import fko.reversi.Reversi;
import fko.reversi.game.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * The BoardPanel class displays the board of a given game.
 */
public class BoardPanel extends JPanel implements MouseListener {

    // -- back reference to _ui --
    private ReversiGUI _ui;

    // -- copy of the current board --
    private Board _curBoard = null;

    // -- colors --
    private Color _possibleMoveColor      = new Color(115, 215, 115);
    private Color _boardBorderColor       = Color.BLACK;
    private Color _boardGridColor         = Color.BLACK;
    private Color _boardBackgroundColor   = new Color(30, 140, 0);
    private Color _lastMoveColor          = new Color(0, 255, 0);
    private Color _blackGradientFromColor = Color.BLACK;
    private Color _blackGradientToColor   = Color.GRAY;
    private Color _whiteGradientFromColor = Color.GRAY;
    private Color _whiteGradientToColor   = Color.WHITE;

    /**
     * constructor
     */
    public BoardPanel(ReversiGUI reversiGUI) {
        super();

        this._ui = reversiGUI;

        // -- set border --
        this.setBorder(new BevelBorder(BevelBorder.LOWERED));
        // -- set background color --
        this.setBackground(Color.GRAY);
        // -- set mouse listener --
        this.addMouseListener(this);

        // -- colors from properties file --
        String[] colors;
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.possibleMoveColor")).split(":");
        _possibleMoveColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.boardBorderColor")).split(":");
        _boardBorderColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.boardGridColor")).split(":");
        _boardGridColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.boardBackgroundColor")).split(":");
        _boardBackgroundColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.lastMoveColor")).split(":");
        _lastMoveColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.blackGradientFromColor")).split(":");
        _blackGradientFromColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.blackGradientToColor")).split(":");
        _blackGradientToColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.whiteGradientFromColor")).split(":");
        _whiteGradientFromColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));
        colors = String.valueOf(Reversi.getProperties().getProperty("ui.whiteGradientToColor")).split(":");
        _whiteGradientToColor = new Color(Integer.valueOf(colors[0]), Integer.valueOf(colors[1]), Integer.valueOf(colors[2]));

    }

    /**
     * draw a board to the ui
     */
    public synchronized void drawBoard(Game game) {
        this._curBoard = game.getCurBoard();
        this.repaint();
    }

    /**
     * Overrides the JComponent paintComponent method to redraw the board
     * @param g
     */
    @Override
	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBoard(g);
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e) {
        if (Reversi.getPlayroom().getCurrentGame() == null
            || !Reversi.getPlayroom().getCurrentGame().isRunning()) {
            return;
        }
        if (e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        int x = e.getX();
        int y = e.getY();
        _ui.getReversiController().setPlayerMove(determineMove(x, y));
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e) {
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * draws the actual graphical board
     * @param g
     */
    private void drawBoard(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        Insets insets = this.getInsets();
        int currentWidth = getWidth() - insets.left - insets.right;
        // -- minimum of currentHeigh and currentWidth
        int size = Math.min(getHeight() - insets.top - insets.bottom, currentWidth) - 10;

        if (_curBoard == null) {
            _drawNullBoard(size, currentWidth, g2);
        } else {
            _drawCurrentBoard(size, currentWidth, g2);
        }
    }

    /**
     * This method draws the board with numbers, lines and stones
     *
     * @param size
     * @param currentWidth
     * @param g2
     */
    private void _drawCurrentBoard(int size, int currentWidth, Graphics2D g2) {

        int size1 = size;
        int dim = _curBoard.getDim();
        size1 -= (size1 % dim);

        int positionX = (currentWidth >> 1) - (size1 >> 1); // >> is equal to division by 2
        int positionY = 8;
        int distance = size1 / dim;

        // -- draw board background --
        g2.setColor(_boardBackgroundColor);
        g2.fill3DRect(positionX, positionY, size1, size1, true);
        g2.setColor(_boardBorderColor);
        g2.draw3DRect(positionX, positionY, size1, size1, true);

        // -- stones --
        GradientPaint stoneColor;
        float stoneSize = distance * 0.7f;
        Move lastMove = _curBoard.getLastMove();
        List<Move> moves = _curBoard.getMoves();
        
        // -- mark possible moves --
        if (_ui.is_showPossibleMoves()) {
            g2.setColor(_possibleMoveColor);
            for (Move curMove : moves) {
                g2.fillRect((curMove.getCol() - 1) * distance + positionX + 1,
                        (dim - curMove.getRow()) * distance + positionY + 1,
                        distance - 1,
                        distance - 1);
            }
        }

        // -- mark last move field --
        g2.setColor(_lastMoveColor);
        if (lastMove != null) {
            g2.fillRect((lastMove.getCol() - 1) * distance + positionX + 1,
                        (dim - lastMove.getRow()) * distance + positionY + 1,
                        distance - 1,
                        distance - 1);
        }

        drawLinesAndNumbers(g2, dim, positionX, positionY, distance, size1);

        // -- draw stones
        for (int col = 1; col <= dim; col++) {
            for (int row = dim; row > 0; row--) {
                if (!_curBoard.getField(col, row).isEmpty()) {
                    // >> equals division by 2
                    //noinspection OverlyComplexArithmeticExpression
                    float rowOffset = (dim - row + 1) * distance - (distance >> 1) - (stoneSize / 2) + positionY;
                    // >> equals division by 2
                    //noinspection OverlyComplexArithmeticExpression
                    float colOffset = col * distance - (distance >> 1) - (stoneSize / 2) + positionX;
                    if (_curBoard.getField(col, row).isBlack()) {
                        //noinspection ObjectAllocationInLoop
                        stoneColor = new GradientPaint(colOffset, rowOffset, _blackGradientToColor,
                                                  colOffset + stoneSize, rowOffset + stoneSize, _blackGradientFromColor);
                    } else if (_curBoard.getField(col, row).isWhite()) {
                        //noinspection ObjectAllocationInLoop
                        stoneColor = new GradientPaint(colOffset, rowOffset, _whiteGradientToColor,
                                                  colOffset + stoneSize, rowOffset + stoneSize, _whiteGradientFromColor);
                    } else {
                        throw new RuntimeException("Field has invalid color");
                    }
                    g2.setPaint(stoneColor);
                    //noinspection ObjectAllocationInLoop
                    g2.fill(new Ellipse2D.Double(colOffset, rowOffset, stoneSize, stoneSize));
                }
            }
        }
    }

    private void drawLinesAndNumbers(Graphics2D g2, int dim, int positionX, int positionY, int distance, int size) {
        // -- board lines and numbers --
        g2.setColor(_boardGridColor);
        g2.setFont(new Font("Arial", Font.PLAIN, size / 30));
        int offset = 2;
        for (int i = 1; i <= dim; i++) {
            g2.drawString(String.valueOf(i), positionX + ((i - 1) * distance) + offset, positionY + size - offset);
            g2.drawString(String.valueOf(i), positionX + offset, positionY + size - ((i - 1) * distance) - offset);
            g2.drawLine(positionX + (i * distance), positionY, positionX + (i * distance), positionY + size);
            g2.drawLine(positionX, positionY + (i * distance), positionX + size, positionY + (i * distance));
        }
        g2.setColor(_boardBorderColor);
        g2.draw3DRect(positionX, positionY, size, size, true);
    }

    /**
     * This method draws the board if we don't have a current board (null)
     *
     * @param size
     * @param currentWidth
     * @param g2
     */
    private void _drawNullBoard(int size, int currentWidth, Graphics2D g2) {

        int size1 = size;
        int dim = Reversi.getPlayroom().getBoardDimension();
        size1 -= (size1 % dim);

        // -- draw board background --
        int positionX = (currentWidth >> 1) - (size1 >> 1); // >> equals division by 2
        int positionY = 8;
        int distance = size1 / dim;
        g2.setColor(_boardBackgroundColor);
        g2.fill3DRect(positionX, positionY, size1, size1, true);

        drawLinesAndNumbers(g2, dim, positionX, positionY, distance, size1);

    }

    /**
     * determins the board coordinates of a user click
     * @param x
     * @param y
     * @return A Point object representing col and row on the board
     */
    private Point determineMove(int x, int y) {
        Insets insets = this.getInsets();
        int currentWidth = getWidth() - insets.left - insets.right;
        int currentHeight = getHeight() - insets.top - insets.bottom;
        int size = Math.min(currentHeight, currentWidth) - 10;
        int dim = _curBoard.getDim();
        size -= (size % dim);
        int positionX = (currentWidth >> 1) - (size >> 1); // >> equals division by 2
        int positionY = 8;
        int distance = size / dim;

        int col = 1 + (x - positionX) / distance;
        int row = 1 + dim - 1 - (y - positionY) / distance;

        return new Point(col, row);
    }
}
