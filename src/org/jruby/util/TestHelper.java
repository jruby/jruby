/*
 * TestHelper.java - No description
 * Created on 15. March 2002, 9:00
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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
package org.jruby.util;

/**
 * Helper class, used for testing calls to java from ruby code.
 *
 * @author  Benoit Cerrina
 * @version $Revision$
 **/
public class TestHelper {

    public String localVariable1;

    /**
     * used to test Java Arrays in Ruby.
     *  while we don't yet have a way to create them this can be used to test basic
     *  array functionalities
     */
    public static String[] createArray(int i) {
        return new String[i];
    }

    /**
     * used to test native exception handling.
     **/
    public static void throwException() {
        throw new RuntimeException("testException");
    }

    /**
     * @return object used to test casting
     */
    public static SomeInterface getInterfacedInstance() {
        return new SomeImplementation();
    }

    public static Object getLooslyCastedInstance() {
        return new SomeImplementation();
    }

    public static Object getNull() {
        return null;
    }

    public static interface SomeInterface {
        String doStuff();
    }

    private static class SomeImplementation implements SomeInterface {
        public String doStuff() {
            return "stuff done";
        }
    }
}