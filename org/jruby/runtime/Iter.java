/*
 * RubyIter.java - description
 * Created on 21.02.2002, 16:22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.runtime;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class Iter {
    public static final Iter ITER_NOT = new Iter("NOT");
    public static final Iter ITER_PRE = new Iter("PRE");
    public static final Iter ITER_CUR = new Iter("CUR");

    private String debug;

    private Iter(final String debug) {
        this.debug = debug;
    }

    public final boolean isNot() {
        return this == ITER_NOT;
    }

    public final boolean isPre() {
        return this == ITER_PRE;
    }

    public final boolean isCur() {
        return this == ITER_CUR;
    }

    public boolean isBlockGiven() {
        return ! isNot();
    }

    /**
     * @see Object#equals(Object)
     */
    public final boolean equals(final Object obj) {
        return this == obj;
    }

    /**
     * @see Object#hashCode()
     */
    public final int hashCode() {
        return debug.hashCode();
    }

    /**
     * @see Object#toString()
     */
    public final String toString() {
        return debug;
    }
}
