/*
 * Utils.java - description
 * Created on 22.03.2002, 17:42:05
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
package org.jruby.internal.util;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public final class Utils {
    public static int getHashcode(Object obj) {
        return obj != null ? obj.hashCode() : 0;
    }

    public static boolean isEquals(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj2 != null ? false : true;
        } else {
            return obj2 != null ? obj1.equals(obj2) : false;
        }
    }

    public static String toString(Object obj) {
        return obj != null ? obj.toString() : "null";
    }
}