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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

 /**
 * An abstract dialog class.
 */
public abstract class AbstractDialog extends JDialog {

    protected AbstractDialog(Frame owner, String title, boolean modal) throws HeadlessException {
        super(owner, title, modal);
    }

    /**
     * Validate the user interface after a change
     * @param c The component to validate.
     */
    public static void validateAll(Component c) {
        if (c != null) {
            synchronized (c.getTreeLock()) {
                for (Container p = c.getParent(); ((p != null) && (!p.isValid()));
                     p = p.getParent()) {
                    c = p;
                    c.validate();
                }
                c.validate();
            }
        }
    }

    /**
     * Centers a component on the screen.
     */
    public static void centerComponent(Component component) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension componentSize = component.getSize();
        // -- >> 1 is equal to division by 2 (/2)
        int x = (screenSize.width - componentSize.width) >> 1;
        int y = (screenSize.height - componentSize.height) >> 1;
        component.setLocation(x, y);
    }

    /**
     * Dispose a dialog instance on request.
     */
    protected class Disposer implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }


}
