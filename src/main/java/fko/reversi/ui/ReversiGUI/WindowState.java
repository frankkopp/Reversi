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

import java.io.*;

/**
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
 * <hr/>
 *
 *
 */
public class WindowState extends java.util.Properties {

    private final static String propertiesFile = '/' + "var/gui/reversi.guid";
    private final static String userDir = System.getProperty("user.dir");

    public WindowState() {
        super();
        String aUserDir = System.getProperty("user.dir");
        InputStream in = null;
        try {
            in = new FileInputStream(aUserDir + propertiesFile);
            this.load(in);
        } catch (FileNotFoundException e) {
            System.err.println("Properties file " + propertiesFile + " not found!");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Properties file " + propertiesFile + " could not be loaded!");
            e.printStackTrace();
        } finally {
            if (in!=null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public void save() {
        OutputStream out=null;
        try {
            out = new FileOutputStream(userDir + propertiesFile);
            this.store(out, " Window state file for Reversi by Frank Kopp");
        } catch (FileNotFoundException e) {
            System.err.println("Properties file " + propertiesFile + " could not be saved!");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Properties file " + propertiesFile + " could not be saved!");
            e.printStackTrace();
        } finally {
            if (out!=null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

}
