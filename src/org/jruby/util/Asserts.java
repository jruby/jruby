/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.util;

/** Debugging util class to terminate the application if the assertion fails.
 *
 * The methods can be turned off in final releases of code by 
 * setting ENABLE_ASSERT to false.
 * 
 * @author jpetersen
 */
public final class Asserts {
    private static final boolean ENABLE_ASSERTS = true;

    /** If the assertion fails (i.e. the expression is not true), an error 
     * message is logged and the application is terminated.
     */
    public static void isTrue(boolean condition) {
        if (ENABLE_ASSERTS && !condition) {
            throw new AssertError("assertTrue failed.");
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (ENABLE_ASSERTS && ! condition) {
            throw new AssertError(message);
        }
    }

    /** If the assertion fails (i.e. the method is invoked), an error 
     * message is logged and the application is terminated.
     */    
    public static void notReached() {
        if (ENABLE_ASSERTS) {
            throw new AssertError("assertNotReached failed.");
        }
    }

    /** If the assertion fails (i.e. the method is invoked), the
     * message is logged and the application is terminated.
     */
    public static void notReached(String message) {
        if (ENABLE_ASSERTS) {
            throw new AssertError(message);
        }
	}
    
    /** If the assertion fails (i.e. the object is null), an error 
     * message is logged and the application is terminated.
     */
    public static void notNull(Object object, String message) {
        if (ENABLE_ASSERTS && object == null) {
            throw new AssertError(message);
        }
    }

    /** If the assertion fails (i.e. the object is null), an error 
     * message is logged and the application is terminated.
     */
    public static void notNull(Object object) {
        if (ENABLE_ASSERTS && object == null) {
            throw new AssertError("object mustn't be null.");
        }
    }
}