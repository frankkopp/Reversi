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

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;

/**
 * This class just provides some helper utilities and cannot be instanciated.
 */
public final class HelperTools {

    private final static Format digitFormat = new DecimalFormat("00");

    private HelperTools () {}

    /**
     * get a MByte String from a byte input
     * @param bytes
     * @return String
     */
    public static String getMBytes(long bytes) {
        double d = (Long.valueOf(bytes)).doubleValue() / (1024.0 * 1024.0);
        NumberFormat f = NumberFormat.getInstance();
        if (f instanceof DecimalFormat) {
            f.setMaximumFractionDigits(1);
        }
        return f.format(d);
    }

    /**
     * format a given time into 00:00:00
     * @param time
     * @return formatted string
     */
    public static String formatTime(long time) {
        StringBuilder sb = new StringBuilder(digitFormat.format((time / 1000 / 60 / 60)));
        sb.append(':');
        sb.append(digitFormat.format((time / 1000 / 60) % 60));
        sb.append(':');
        sb.append(digitFormat.format((time / 1000) % 60));
        return sb.toString();
    }
}
