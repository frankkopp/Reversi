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

package fko.reversi.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>This class is an implementation of a LRUCache extending LinkedHashMap.
 * Compared to a "normal" HashMap this class sets a maximum number of entries
 * by deleting the least recent used object.</p>
 *
 * <strong>This class is not thread safe (synchronized)</strong>
 *
 * @see LinkedHashMap
 *
 * @author Frank Kopp (frank@familie-kopp.de)
 */
public class LruCache extends LinkedHashMap {

    // -- default value for the maximum number of entries
    private int _maxEntries;

    // -- through "removeEldestEntry" this class takes care that we do not run out of memory due to
    // -- too many entries in the cache
    private long _minFreeMemLeft;

    private static final long MAXMEM = Runtime.getRuntime().maxMemory();

    /**
     * Constructs an empty LruCache instance with the specified initial capacity, load factor and ordering mode.<br/>
     * This class also has a method to set the minimum free memory left for the VM. If less memory than this minimum
     * is available the LruCache will purge old entries every time a new entry is added.
     * @param initialCapacity - the initial capacity
     * @param loadFactor - the load factor
     * @param accessOrder  - the ordering mode - true for access-order, false for insertion-order
     * @param maxEntries - the maximal number of entries this LruCache will hold
     * @see LinkedHashMap
     */
    public LruCache(int initialCapacity, float loadFactor, boolean accessOrder, int maxEntries) {
	super(initialCapacity, loadFactor, accessOrder);
	_maxEntries = maxEntries;
	setMinFreeMemLeft(25);
    }

    /**
     * Sets the minimum available memory in percent of the virtual machines
     * @param percent amount of minimum memory left (not used by cache)
     */
    public void setMinFreeMemLeft(int percent) {
	_minFreeMemLeft = (long) (MAXMEM * (percent / 100.0f));
    }

    /**
     * Sets the maximal number of objects this LruCache will hold.
     * @param maxEntries Maximal number of cache entries
     */
    public void setMaxEntries(int maxEntries) {
	_maxEntries = maxEntries;
    }

    /**
     * Gets the maximal number this LruCache will hold.
     * @return MAX_ENTRIES
     */
    public int getMaxEntries() {
	return _maxEntries;
    }

    /**
     * Implements removeEldestEntry from LinkedHashMap to determine if the eldest entry
     * shall be removed (LRU)
     * @param eldest Removes the eldest entry. Will be called everytime an entry is added
     * @return true or false
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
	return (
		size() > _maxEntries ||
		(MAXMEM-Runtime.getRuntime().totalMemory()+Runtime.getRuntime().freeMemory()) < _minFreeMemLeft
		);
    }

}
